package org.goplanit.demands.discrete.jitter;

import org.goplanit.demands.discrete.person.Person;
import org.goplanit.demands.discrete.person.PersonUtils;
import org.goplanit.demands.discrete.tour.ParticipantTour;
import org.goplanit.demands.discrete.tour.Tour;
import org.goplanit.utils.time.LocalTimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.logging.Logger;

/**
 * Applies temporal jitter to a person's schedule, see {@link ScheduleJitterType} for the available strategies.
 * <p>
 * Demand read from a time binned source gives every element in a bin an identical time, which shows up as a sharp
 * peak at each bin boundary. Jitter spreads those elements out again without changing the aggregate profile.
 * </p>
 * <p>
 * <b>Time points.</b> Sub-bin jitter moves times, not schedule elements, see {@code JitterPoint}. A trip has one time
 * point, its start. A tour has one, its end. A tour's start is the start of its first trip, so it follows that trip
 * and is not drawn. Before the jitter, all time points in one bin carry the bin's start. The bin is split into one
 * sub-bin per time point, and each is drawn in its own sub-bin, in schedule order. So the bin fills evenly, and the
 * draws cannot put a schedule out of order.
 * </p>
 * <p>
 * <b>Shared tours.</b> A tour shared by several participants, such as a parent taking a child to school, is one tour
 * in the model. Each participant sees it through their own participation, so its times are the same for all of them.
 * Those times must fit into every participant's day, and each participant may have other time points in the same
 * bins. These rules keep every participant's day in order:
 * </p>
 * <ol>
 * <li>Each shared tour is drawn once, before any person, see {@code SharedTourJitterUtils}. In a person's draw its time
 * points are fixed. They keep their place in the order but never move, as a move would change the tour for every
 * participant. The person's own time points give way around them.</li>
 * <li>Shared tours are drawn in the order they take place. So when a shared tour is drawn, the shared tours before it
 * on its participants' schedules already have their final times.</li>
 * <li>A shared tour's time points in a bin are drawn only in the part of the bin that every participant leaves them,
 * see {@code ParticipantSlotBoundsInSharedTour}. That part leaves each participant room for their other time points
 * in the bin, before and after the tour.</li>
 * <li>When the participants leave no common part, the end of an earlier shared tour of one of them moves earlier
 * within its bin to make room. When that cannot make enough room, the tour's time points in the bin all take one
 * time. That keeps the order, but gives them no spread. Both cases are logged once all shared tours are drawn, see
 * {@code SharedTourRoomAdjustments}.</li>
 * <li>A shared tour given a minimum duration gets it in its own draw, within its draw range. So the minimum takes no
 * room left for its participants' later time points.</li>
 * </ol>
 * <p>
 * For example, a parent and child share a short tour that leaves and returns within the bin from 12:00 to 12:15, so
 * its two trips and its end all carry 12:00. The parent then leaves on a tour of their own, also at 12:00. So the
 * parent has four time points in the bin, and the child three. For the parent, the shared tour gets the first three
 * of four parts of the bin, so its draw range is 12:00 to 12:11:15. When the parent is drawn later, their own trip is
 * drawn over the whole bin. If it lands before the shared tour's end, it moves up to that end.
 * </p>
 * <p>
 * A day can still end up out of order only when its shared tours already overlap in the input. Such persons are
 * returned to the caller, see {@link #applySubBinJitter(Iterable, Iterable, int, int, int, Set, int)}.
 * </p>
 * <p>
 * All times are compared as seconds from the start of the allowed window, not from midnight, because a window can run
 * past midnight. In a window from 03:00 to 03:00 the next day, 01:00 is late in the day, not early.
 * </p>
 *
 * @author markr
 */
public class ScheduleJitterExecutor {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(ScheduleJitterExecutor.class.getCanonicalName());

  /**
   * Spread a person's schedule within the time bins it occupies, see
   * {@link #applySubBinJitter(Person, int, int, int, Set, int)}
   *
   * @param person the person whose schedule to jitter
   * @param spreadWidthSeconds width to spread the time points in a bin over
   * @param minAllowedTimeSeconds the lower boundary in seconds
   * @param maxAllowedTimeSeconds the upper boundary in seconds
   * @param minTourDuration the minimum duration and the tours to give it
   * @return true when the resulting schedule is in chronological order, false when it could not be made so
   */
  private static boolean applySubBinJitter(
      Person person, int spreadWidthSeconds, int minAllowedTimeSeconds, int maxAllowedTimeSeconds,
      MinTourDuration minTourDuration) {

    if (spreadWidthSeconds <= 0 || person == null || person.getSchedule() == null
        || person.getSchedule().isEmpty()) {
      return true;
    }

    var schedule = person.getSchedule();
    var jitterPoints = new ArrayList<JitterPoint>(schedule.sizeUnrolled(false) + 1);
    JitterPoint.collect(schedule, jitterPoints, false);
    if (jitterPoints.isEmpty()) {
      return true;
    }

    final var windowStartTime = LocalTimeUtils.ofSecondOfDayWrapped(minAllowedTimeSeconds);
    final long windowDurationSeconds = maxAllowedTimeSeconds - minAllowedTimeSeconds;
    var rng = new SplittableRandom(PersonUtils.generatePersonSeed(person));

    SubBinDrawUtils.drawSubBins(jitterPoints, spreadWidthSeconds, windowStartTime, windowDurationSeconds, rng,
        null /* a person's own points, drawn over the whole bin */);
    SubBinDrawUtils.enforceNonDecreasingOrder(jitterPoints, windowStartTime);

    // tour starts are not drawn, they follow the first trip that has now been placed
    schedule.syncTourStartTimesToFirstElement();

    SubBinDrawUtils.enforceMinTourDuration(jitterPoints, minTourDuration, windowStartTime, windowDurationSeconds);

    return SubBinDrawUtils.isNonDecreasing(jitterPoints, windowStartTime);
  }

  /**
   * Spread the schedules of the provided persons within the time bins they occupy. Tours shared by several
   * participants are drawn first, once each and in the order they take place, as the class documentation explains.
   * Then each person is drawn, see {@link #applySubBinJitter(Person, int, int, int, Set, int)}. Drawing a shared tour
   * may move the end of an earlier shared tour of one of its participants earlier. The shared tours whose end moved,
   * and those whose time points in a bin were kept in order at one time, are logged once all shared tours are drawn
   *
   * @param tours all tours, so the shared ones among them can be drawn up front
   * @param persons whose schedules to jitter
   * @param spreadWidthSeconds width to spread the time points in a bin over, being the bin length scaled by any
   *                           configured ratio
   * @param minAllowedTimeSeconds the lower boundary in seconds
   * @param maxAllowedTimeSeconds the upper boundary in seconds
   * @param minDurationTours tours to give a minimum duration, may be null
   * @param minDurationSeconds minimum duration to give the tours provided
   * @return persons whose schedule could not be brought into chronological order, empty when all of them could. It
   *         is left to the caller to decide what such a schedule is worth, nothing is discarded here
   */
  public static List<Person> applySubBinJitter(
      Iterable<Tour> tours, Iterable<Person> persons, int spreadWidthSeconds, int minAllowedTimeSeconds,
      int maxAllowedTimeSeconds, Set<Tour> minDurationTours, int minDurationSeconds) {

    if (spreadWidthSeconds <= 0) {
      return List.of();
    }

    final var windowStartTime = LocalTimeUtils.ofSecondOfDayWrapped(minAllowedTimeSeconds);
    var minTourDuration = new MinTourDuration(minDurationTours, minDurationSeconds);
    var roomAdjustments = new SharedTourRoomAdjustments();
    SharedTourJitterUtils.collectSharedToursInOrder(tours, persons, windowStartTime).forEach(
        tour -> SharedTourJitterUtils.applySharedTourSubBinJitter(tour, spreadWidthSeconds, minAllowedTimeSeconds,
            maxAllowedTimeSeconds, minTourDuration, roomAdjustments));
    roomAdjustments.log();

    var nonChronologicalPersons = new ArrayList<Person>();
    persons.forEach(person -> {
      if (!applySubBinJitter(
          person,
          spreadWidthSeconds,
          minAllowedTimeSeconds,
          maxAllowedTimeSeconds,
          minTourDuration)) {
        nonChronologicalPersons.add(person);
      }
    });
    return nonChronologicalPersons;
  }

  /**
   * Spread a person's schedule within the time bins it occupies. Elements sharing a bin carry an identical time,
   * so each bin is split into as many sub-bins as it holds time points and each point is drawn within its own
   * sub-bin, in schedule order. This keeps the aggregate spread across the bin uniform, exactly as
   * a rigid per person offset does, while removing the collisions a rigid offset preserves.
   * <p>
   * Ordering cannot be inverted by the draws: consecutive sub-bins are disjoint and assigned in schedule order,
   * and distinct bins are disjoint and ordered among themselves.
   * </p>
   * <p>
   * Times belonging to a tour shared by several participants are expected to have been drawn already, see
   * {@link #applySubBinJitter(Iterable, Iterable, int, int, int, Set, int)}, and are left untouched here
   * </p>
   *
   * @param person the person whose schedule to jitter
   * @param spreadWidthSeconds width to spread the time points in a bin over, being the bin length scaled by any
   *                           configured ratio
   * @param minAllowedTimeSeconds the lower boundary in seconds
   * @param maxAllowedTimeSeconds the upper boundary in seconds
   * @param minDurationTours tours to give a minimum duration, may be null
   * @param minDurationSeconds minimum duration to give the tours provided
   * @return true when the resulting schedule is in chronological order, false when it could not be made so
   */
  public static boolean applySubBinJitter(
      Person person, int spreadWidthSeconds, int minAllowedTimeSeconds, int maxAllowedTimeSeconds,
      Set<Tour> minDurationTours, int minDurationSeconds) {
    return applySubBinJitter(person, spreadWidthSeconds, minAllowedTimeSeconds, maxAllowedTimeSeconds,
        new MinTourDuration(minDurationTours, minDurationSeconds));
  }

  /**
   * Draw a single offset for the person and shift their entire schedule rigidly by it, deterministically and
   * clamped within the allowed period boundaries.
   *
   * @param person the person whose schedule to jitter
   * @param maxDeviationSeconds the maximum deviation allowed (+/- maxDeviationSeconds)
   * @param symmetric if true, shifts by [-max, +max]; if false, shifts strictly forward by [0, max]
   * @param minAllowedTimeSeconds the lower boundary in seconds
   * @param maxAllowedTimeSeconds the upper boundary in seconds
   */
  public static void applyRigidPersonJitter(
      Person person,
      int maxDeviationSeconds,
      boolean symmetric,
      int minAllowedTimeSeconds,
      int maxAllowedTimeSeconds) {

    if (maxDeviationSeconds <= 0 || person == null || person.getSchedule() == null
        || person.getSchedule().isEmpty()) {
      return;
    }

    var schedule = person.getSchedule();

    // Establish the schedule extent relative to the start of the allowed window rather than to midnight, so that
    // schedules legitimately running past midnight are not mistaken for schedules starting before the window
    var first = schedule.getFirst();
    if (first == null || first.getStartTime() == null) {
      LOGGER.severe(String.format(
          "Schedule of person (%s) has no valid first element start time, unable to apply jitter",
          person.getIdsAsString()));
      return;
    }
    var last = schedule.getLast(false /* not flattened, because we're after end time of last tour */);
    if (!(last instanceof ParticipantTour) || ((ParticipantTour) last).getEndTime() == null) {
      LOGGER.severe(String.format("Schedule of person (%s) has invalid top level last tour, unable to apply jitter",
          person.getIdsAsString()));
      return;
    }

    final var windowStartTime = LocalTimeUtils.ofSecondOfDayWrapped(minAllowedTimeSeconds);
    final long windowDurationSeconds = maxAllowedTimeSeconds - minAllowedTimeSeconds;
    long earliestSeconds = LocalTimeUtils.secondsFromWrapAroundDayAnchor(windowStartTime, first.getStartTime());

    // The two end points settle the span. In a window of at most a day an end time after the start cannot have
    // wrapped, since a wrap would exceed a day and then not fit the window at all, and an end time before the
    // start must have wrapped exactly once
    final var firstStartTime = first.getStartTime();
    final var lastEndTime = ((ParticipantTour) last).getEndTime();
    final long availableElapsedSeconds = windowDurationSeconds - earliestSeconds;
    long scheduleSpanSeconds;
    if (!lastEndTime.equals(firstStartTime)) {
      scheduleSpanSeconds = LocalTimeUtils.secondsFromWrapAroundDayAnchor(firstStartTime, lastEndTime);
    } else if (LocalTimeUtils.SECONDS_IN_DAY <= availableElapsedSeconds) {
      // an end time equal to the start is either a zero span or exactly one day, and here both readings are
      // valid because a day still fits from the schedule's start onwards. Adopt the day, being the reading that
      // cannot push the schedule out of the window, but flag it since the choice is not decidable
      LOGGER.warning(String.format(
          "Schedule of person (%s) both starts and ends at %s, which is ambiguous: it spans either nothing at " +
              "all or exactly one day. Assuming one day, which leaves no room to apply jitter",
          person.getIdsAsString(), firstStartTime));
      scheduleSpanSeconds = LocalTimeUtils.SECONDS_IN_DAY;
    } else {
      // not ambiguous, a day does not fit from the schedule's start onwards, so a wrapping schedule would leave
      // the window entirely and a zero span is the only valid reading
      scheduleSpanSeconds = 0;
    }
    long latestSeconds = earliestSeconds + scheduleSpanSeconds;
    if (latestSeconds > windowDurationSeconds) {
      LOGGER.severe(String.format(
          "Schedule of person (%s) extends beyond the allowed time window (by %d seconds), unable to apply " +
              "jitter. Its elements may not be in chronological order",
          person.getIdsAsString(), latestSeconds - windowDurationSeconds));
      return;
    }

    // Restrict the draw to offsets that keep the entire schedule inside the window and draw within that
    // restriction, rather than drawing first and truncating: truncation piles mass exactly on the window
    // boundaries, reintroducing a spike at the two places jitter exists to avoid
    long lowerOffsetBound = Math.max(symmetric ? -maxDeviationSeconds : 0, -earliestSeconds);
    long upperOffsetBound = Math.min(maxDeviationSeconds, windowDurationSeconds - latestSeconds);
    if (upperOffsetBound <= lowerOffsetBound) {
      return;
    }

    SplittableRandom rng = new SplittableRandom(PersonUtils.generatePersonSeed(person));
    int offsetSeconds = (int) Math.round(
        lowerOffsetBound + rng.nextDouble() * (upperOffsetBound - lowerOffsetBound));
    if (offsetSeconds == 0) {
      return;
    }

    schedule.shift(offsetSeconds);
  }
}

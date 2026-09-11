package org.goplanit.demands.discrete;

import org.goplanit.demands.discrete.person.Person;
import org.goplanit.demands.discrete.person.PersonUtils;
import org.goplanit.demands.discrete.tour.ActivitySchedule;
import org.goplanit.demands.discrete.tour.ParticipantTour;
import org.goplanit.demands.discrete.tour.ScheduleElement;
import org.goplanit.demands.discrete.tour.Tour;
import org.goplanit.demands.discrete.trip.Trip;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.time.LocalTimeUtils;

import java.time.LocalTime;
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
 */
public class ScheduleJitter {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(ScheduleJitter.class.getCanonicalName());

  /**
   * A single time within a schedule that jitter may move, paired with how to write it back. A schedule element
   * carries at most two such times, its start and, for a tour, its end.
   */
  private static final class JitterPoint {

    /** element the time belongs to */
    private final ScheduleElement element;

    /** when true the point is the element's tour end time, otherwise its start time */
    private final boolean tourEndTime;

    /** when true this time belongs to a tour shared by several participants and is drawn once, elsewhere. It still
     * takes part in the ordering of the schedule it appears in, but is never moved by that schedule's draw */
    private final boolean fixed;

    /** end of the bin this point was drawn in, as seconds from the window anchor */
    private long binEndElapsedSeconds;

    private JitterPoint(ScheduleElement element, boolean tourEndTime, boolean fixed) {
      this.element = element;
      this.tourEndTime = tourEndTime;
      this.fixed = fixed;
    }

    private LocalTime getTime() {
      return tourEndTime ? ((ParticipantTour) element).getEndTime() : element.getStartTime();
    }

    private void setTime(LocalTime time) {
      if (element instanceof ParticipantTour) {
        var tour = ((ParticipantTour) element).getTour();
        if (tourEndTime) {
          tour.setEndTime(time);
        } else {
          tour.setStartTime(time);
        }
      } else if (element instanceof Trip) {
        ((Trip) element).setStartTime(time);
      } else {
        throw new PlanItRunTimeException("Unsupported schedule element type (%s) encountered when applying jitter",
            element.getClass().getCanonicalName());
      }
    }
  }

  /**
   * Collect the times of a schedule that are points to be drawn, in schedule order.
   * <p>
   * A tour's start is the same instant as its first trip's departure, so a tour holding nested elements contributes
   * no point of its own and instead follows that trip afterwards, see
   * {@link ActivitySchedule#syncTourStartTimesToFirstElement()}. Its end is the arrival back at the anchor, a
   * distinct and strictly later instant than the last trip's start, so it is a point in its own right. It is
   * collected after recursing, so that it can never claim a sub-bin ahead of the elements it contains.
   * </p>
   *
   * <p>
   * Everything within a tour shared by several participants - its trips as much as its end - is drawn once for the
   * tour itself rather than per participant, see
   * {@link #applySharedTourSubBinJitter(Tour, int, int, int)}. Such points are still collected here, because they
   * take part in the order of the schedule they appear in and bound the points around them, but they are marked as
   * fixed so no participant's draw moves them
   * </p>
   *
   * @param schedule to collect the points of
   * @param pointsToPopulate to add the encountered points to, in order
   * @param withinSharedTour when true the points collected sit within a tour shared by several participants
   */
  private static void collectJitterPoints(
      ActivitySchedule schedule, List<JitterPoint> pointsToPopulate, boolean withinSharedTour) {
    for (ScheduleElement element : schedule) {
      if (!(element instanceof ParticipantTour) && !(element instanceof Trip)) {
        throw new PlanItRunTimeException(
            "Unsupported schedule element type (%s) encountered when collecting jitter points",
            element.getClass().getCanonicalName());
      }

      boolean hasNestedSchedule = element.hasSchedule();

      boolean sharedTour = withinSharedTour ||
          (element instanceof ParticipantTour && ((ParticipantTour) element).getTour().hasMultipleParticipants());

      if (!hasNestedSchedule && element.getStartTime() != null) {
        pointsToPopulate.add(new JitterPoint(element, false, sharedTour));
      }

      if (hasNestedSchedule) {
        collectJitterPoints(element.getSchedule(), pointsToPopulate, sharedTour);
      }

      if (element instanceof ParticipantTour && ((ParticipantTour) element).getEndTime() != null) {
        pointsToPopulate.add(new JitterPoint(element, true, sharedTour));
      }
    }
  }

  /**
   * Give each of the provided tours a minimum duration by moving its end time later, leaving the trips it contains
   * where they are. The end is never moved past the end of its own bin, nor past the point that follows it, so the
   * minimum is a preference that is met only as far as there is room for it.
   *
   * @param jitterPoints all points of the schedule, in order, as drawn
   * @param minDurationTours tours to give a minimum duration
   * @param minDurationSeconds minimum duration to give them
   * @param windowStartSeconds start of the allowed window in seconds
   * @param windowStartTime start of the allowed window as a time of day
   * @param windowDurationSeconds length of the allowed window
   */
  private static void enforceMinTourDuration(
      List<JitterPoint> jitterPoints, Set<Tour> minDurationTours, int minDurationSeconds, int windowStartSeconds,
      LocalTime windowStartTime, long windowDurationSeconds) {

    for (int index = 0; index < jitterPoints.size(); ++index) {
      var point = jitterPoints.get(index);
      if (!point.tourEndTime) {
        continue;
      }
      /* the point belongs to a participation, the minimum duration is a property of the tour behind it */
      var tour = ((ParticipantTour) point.element).getTour();
      if (!minDurationTours.contains(tour)) {
        continue;
      }

      long startElapsedSeconds =
          LocalTimeUtils.secondsFromWrapAroundDayAnchor(windowStartTime, tour.getStartTime());
      long endElapsedSeconds = LocalTimeUtils.secondsFromWrapAroundDayAnchor(windowStartTime, point.getTime());

      long latestAllowedSeconds = index + 1 < jitterPoints.size()
          ? LocalTimeUtils.secondsFromWrapAroundDayAnchor(windowStartTime, jitterPoints.get(index + 1).getTime())
          : windowDurationSeconds;
      latestAllowedSeconds = Math.min(latestAllowedSeconds, point.binEndElapsedSeconds);

      long newEndElapsedSeconds = Math.min(startElapsedSeconds + minDurationSeconds, latestAllowedSeconds);
      if (newEndElapsedSeconds > endElapsedSeconds) {
        point.setTime(LocalTimeUtils.ofSecondOfDayWrapped(windowStartSeconds + newEndElapsedSeconds));
      }
    }
  }

  /**
   * Draw the provided points within their bins. Points carrying an identical time form a run that shares one bin and
   * is split into as many sub-bins as it holds points, each point being drawn within its own sub-bin in order.
   * Points marked as fixed keep their time, they were drawn as part of the tour they belong to, but they still
   * occupy their place in the order
   *
   * @param jitterPoints points to draw, in schedule order
   * @param spreadWidthSeconds width to spread a run over
   * @param windowStartTime start of the allowed window as a time of day
   * @param windowDurationSeconds length of the allowed window
   * @param rng to draw with
   */
  private static void drawSubBins(
      List<JitterPoint> jitterPoints, int spreadWidthSeconds, LocalTime windowStartTime,
      long windowDurationSeconds, SplittableRandom rng) {

    int runStartIndex = 0;
    while (runStartIndex < jitterPoints.size()) {

      // a run is a maximal stretch of points carrying the same time, i.e. the points sharing one bin
      final var runTime = jitterPoints.get(runStartIndex).getTime();
      int runEndIndex = runStartIndex;
      while (runEndIndex + 1 < jitterPoints.size()
          && runTime.equals(jitterPoints.get(runEndIndex + 1).getTime())) {
        ++runEndIndex;
      }
      final int runSize = runEndIndex - runStartIndex + 1;

      // near the window end there may be less room than a full width, in which case spread over what remains
      // rather than clamping the draws, which would pile them onto the boundary
      long runElapsedSeconds = LocalTimeUtils.secondsFromWrapAroundDayAnchor(windowStartTime, runTime);
      long remainingSeconds = windowDurationSeconds - runElapsedSeconds;
      double availableSeconds = Math.min(spreadWidthSeconds, Math.max(remainingSeconds, 0));

      // remember where the bin ends, nothing may be moved beyond it afterwards
      for (int index = 0; index < runSize; ++index) {
        jitterPoints.get(runStartIndex + index).binEndElapsedSeconds =
            runElapsedSeconds + (long) availableSeconds;
      }

      if (availableSeconds > 0) {
        final double subBinSeconds = availableSeconds / runSize;
        for (int index = 0; index < runSize; ++index) {
          // draw regardless, so the sequence of draws does not depend on which points happen to be fixed
          long offsetSeconds = Math.round((index + rng.nextDouble()) * subBinSeconds);
          var point = jitterPoints.get(runStartIndex + index);
          if (!point.fixed) {
            point.setTime(runTime.plusSeconds(offsetSeconds));
          }
        }
      }

      runStartIndex = runEndIndex + 1;
    }
  }

  /**
   * Ensure the points do not run backwards in time. Draws within a single run cannot invert, but a point belonging
   * to a shared tour was drawn elsewhere and no longer carries its bin's time, so a point drawn after it can land
   * before it. Any such point is moved up to its predecessor. Where the bin leaves no room the two end up equal,
   * which is in order rather than inverted
   *
   * @param jitterPoints points in schedule order
   * @param windowStartSeconds start of the allowed window in seconds
   * @param windowStartTime start of the allowed window as a time of day
   */
  private static void enforceNonDecreasingOrder(
      List<JitterPoint> jitterPoints, int windowStartSeconds, LocalTime windowStartTime) {

    long previousElapsedSeconds = -1;
    for (var point : jitterPoints) {
      long elapsedSeconds = LocalTimeUtils.secondsFromWrapAroundDayAnchor(windowStartTime, point.getTime());
      if (elapsedSeconds < previousElapsedSeconds) {
        if (point.fixed) {
          /* authoritative, it was drawn as part of its own tour, leave it and continue from here */
          previousElapsedSeconds = elapsedSeconds;
          continue;
        }
        point.setTime(LocalTimeUtils.ofSecondOfDayWrapped(windowStartSeconds + previousElapsedSeconds));
        elapsedSeconds = previousElapsedSeconds;
      }
      previousElapsedSeconds = elapsedSeconds;
    }
  }

  /**
   * Spread the schedules of the provided persons within the time bins they occupy. Tours shared by several
   * participants are common to all of them and are therefore drawn once, before any person is drawn, so that every
   * participant sees the same final times and those times bound the draws around them
   *
   * @param tours all tours, so the shared ones among them can be drawn up front
   * @param persons whose schedules to jitter
   * @param spreadWidthSeconds width to spread a run over, being the bin length scaled by any configured ratio
   * @param minAllowedTimeSeconds the lower boundary in seconds
   * @param maxAllowedTimeSeconds the upper boundary in seconds
   * @param minDurationTours tours to give a minimum duration, may be null
   * @param minDurationSeconds minimum duration to give the tours provided
   */
  public static void applySubBinJitter(
      Iterable<Tour> tours, Iterable<Person> persons, int spreadWidthSeconds, int minAllowedTimeSeconds,
      int maxAllowedTimeSeconds, Set<Tour> minDurationTours, int minDurationSeconds) {

    if (spreadWidthSeconds <= 0) {
      return;
    }

    tours.forEach(tour -> applySharedTourSubBinJitter(
        tour, spreadWidthSeconds, minAllowedTimeSeconds, maxAllowedTimeSeconds));
    persons.forEach(person -> applySubBinJitter(
        person, spreadWidthSeconds, minAllowedTimeSeconds, maxAllowedTimeSeconds, minDurationTours,
        minDurationSeconds));
  }

  /**
   * Draw the times within a tour that is shared by more than one participant. Everything inside such a tour, its
   * trips as much as its end, is common to all participants and is therefore drawn once here rather than once per
   * participant, seeded by the primary participant so the result stays reproducible
   *
   * @param tour to draw, ignored when it has a single participant
   * @param spreadWidthSeconds width to spread a run over
   * @param minAllowedTimeSeconds the lower boundary in seconds
   * @param maxAllowedTimeSeconds the upper boundary in seconds
   */
  private static void applySharedTourSubBinJitter(
      Tour tour, int spreadWidthSeconds, int minAllowedTimeSeconds, int maxAllowedTimeSeconds) {

    if (spreadWidthSeconds <= 0 || tour == null || !tour.hasMultipleParticipants() || !tour.hasSchedule()) {
      return;
    }
    var primaryParticipant = tour.getPrimaryParticipant();
    if (primaryParticipant == null) {
      LOGGER.severe(String.format(
          "Tour (%s) shared by multiple participants has no primary participant, unable to apply jitter",
          tour.getIdsAsString()));
      return;
    }

    /* the tour's own points, which are not fixed here, this being the place they are drawn */
    var jitterPoints = new ArrayList<JitterPoint>(tour.getSchedule().sizeUnrolled(false) + 1);
    collectJitterPoints(tour.getSchedule(), jitterPoints, false);
    var primaryParticipation = tour.getParticipantTours().stream().filter(
        pt -> pt.isPrimary()).findFirst().orElse(null);
    if (primaryParticipation != null && primaryParticipation.getEndTime() != null) {
      jitterPoints.add(new JitterPoint(primaryParticipation, true, false));
    }
    if (jitterPoints.isEmpty()) {
      return;
    }

    final var windowStartTime = LocalTimeUtils.ofSecondOfDayWrapped(minAllowedTimeSeconds);
    final long windowDurationSeconds = maxAllowedTimeSeconds - minAllowedTimeSeconds;
    var rng = new SplittableRandom(PersonUtils.generatePersonSeed(primaryParticipant));

    drawSubBins(jitterPoints, spreadWidthSeconds, windowStartTime, windowDurationSeconds, rng);
    enforceNonDecreasingOrder(jitterPoints, minAllowedTimeSeconds, windowStartTime);

    // the tour's start follows the first trip that has now been placed
    tour.getSchedule().syncTourStartTimesToFirstElement();
    var firstElementStartTime = tour.getSchedule().getFirst() != null
        ? tour.getSchedule().getFirst().getStartTime() : null;
    if (firstElementStartTime != null) {
      tour.setStartTime(firstElementStartTime);
    }
  }

  /**
   * Spread a person's schedule within the time bins it occupies. Elements sharing a bin carry an identical time,
   * so each maximal run of equal times is split into as many sub-bins as it holds points and each point is drawn
   * within its own sub-bin, in schedule order. This keeps the aggregate spread across the bin uniform, exactly as
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
   * @param spreadWidthSeconds width to spread a run over, being the bin length scaled by any configured ratio
   * @param minAllowedTimeSeconds the lower boundary in seconds
   * @param maxAllowedTimeSeconds the upper boundary in seconds
   * @param minDurationTours tours to give a minimum duration, may be null
   * @param minDurationSeconds minimum duration to give the tours provided
   */
  public static void applySubBinJitter(
      Person person, int spreadWidthSeconds, int minAllowedTimeSeconds, int maxAllowedTimeSeconds,
      Set<Tour> minDurationTours, int minDurationSeconds) {

    if (spreadWidthSeconds <= 0 || person == null || person.getSchedule() == null
        || person.getSchedule().isEmpty()) {
      return;
    }

    var schedule = person.getSchedule();
    var jitterPoints = new ArrayList<JitterPoint>(schedule.sizeUnrolled(false) + 1);
    collectJitterPoints(schedule, jitterPoints, false);
    if (jitterPoints.isEmpty()) {
      return;
    }

    final var windowStartTime = LocalTimeUtils.ofSecondOfDayWrapped(minAllowedTimeSeconds);
    final long windowDurationSeconds = maxAllowedTimeSeconds - minAllowedTimeSeconds;
    var rng = new SplittableRandom(PersonUtils.generatePersonSeed(person));

    drawSubBins(jitterPoints, spreadWidthSeconds, windowStartTime, windowDurationSeconds, rng);
    enforceNonDecreasingOrder(jitterPoints, minAllowedTimeSeconds, windowStartTime);

    // tour starts are not drawn, they follow the first trip that has now been placed
    schedule.syncTourStartTimesToFirstElement();

    if (minDurationSeconds > 0 && minDurationTours != null && !minDurationTours.isEmpty()) {
      enforceMinTourDuration(
          jitterPoints, minDurationTours, minDurationSeconds, minAllowedTimeSeconds, windowStartTime,
          windowDurationSeconds);
    }
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

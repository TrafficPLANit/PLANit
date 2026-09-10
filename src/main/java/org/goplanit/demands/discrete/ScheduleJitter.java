package org.goplanit.demands.discrete;

import org.goplanit.demands.discrete.person.Person;
import org.goplanit.demands.discrete.person.PersonUtils;
import org.goplanit.demands.discrete.tour.ActivitySchedule;
import org.goplanit.demands.discrete.tour.ScheduleElement;
import org.goplanit.demands.discrete.tour.Tour;
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

    /** end of the bin this point was drawn in, as seconds from the window anchor */
    private long binEndElapsedSeconds;

    private JitterPoint(ScheduleElement element, boolean tourEndTime) {
      this.element = element;
      this.tourEndTime = tourEndTime;
    }

    private LocalTime getTime() {
      return tourEndTime ? ((Tour) element).getEndTime() : element.getStartTime();
    }

    private void setTime(LocalTime time) {
      if (tourEndTime) {
        ((Tour) element).setEndTime(time);
      } else {
        element.setStartTime(time);
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
   * @param schedule to collect the points of
   * @param pointsToPopulate to add the encountered points to, in order
   */
  private static void collectJitterPoints(ActivitySchedule schedule, List<JitterPoint> pointsToPopulate) {
    for (ScheduleElement element : schedule) {
      boolean hasNestedSchedule = element.hasSchedule() && element.getSchedule() != null;

      if (!hasNestedSchedule && element.getStartTime() != null) {
        pointsToPopulate.add(new JitterPoint(element, false));
      }

      if (hasNestedSchedule) {
        collectJitterPoints(element.getSchedule(), pointsToPopulate);
      }

      if (element instanceof Tour && ((Tour) element).getEndTime() != null) {
        pointsToPopulate.add(new JitterPoint(element, true));
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
      if (!point.tourEndTime || !minDurationTours.contains(point.element)) {
        continue;
      }

      long startElapsedSeconds =
          LocalTimeUtils.secondsFromWrapAroundDayAnchor(windowStartTime, point.element.getStartTime());
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
   * Spread a person's schedule within the time bins it occupies. Elements sharing a bin carry an identical time,
   * so each maximal run of equal times is split into as many sub-bins as it holds points and each point is drawn
   * within its own sub-bin, in schedule order. This keeps the aggregate spread across the bin uniform, exactly as
   * a rigid per person offset does, while removing the collisions a rigid offset preserves.
   * <p>
   * Ordering cannot be inverted by the draws: consecutive sub-bins are disjoint and assigned in schedule order,
   * and distinct bins are disjoint and ordered among themselves.
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
    collectJitterPoints(schedule, jitterPoints);
    if (jitterPoints.isEmpty()) {
      return;
    }

    final var windowStartTime = LocalTimeUtils.ofSecondOfDayWrapped(minAllowedTimeSeconds);
    final long windowDurationSeconds = maxAllowedTimeSeconds - minAllowedTimeSeconds;
    var rng = new SplittableRandom(PersonUtils.generatePersonSeed(person));

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
          long offsetSeconds = Math.round((index + rng.nextDouble()) * subBinSeconds);
          jitterPoints.get(runStartIndex + index).setTime(runTime.plusSeconds(offsetSeconds));
        }
      }

      runStartIndex = runEndIndex + 1;
    }

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
    if (!(last instanceof Tour) || ((Tour) last).getEndTime() == null) {
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
    final var lastEndTime = ((Tour) last).getEndTime();
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

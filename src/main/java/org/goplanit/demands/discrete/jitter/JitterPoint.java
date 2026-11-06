package org.goplanit.demands.discrete.jitter;

import org.goplanit.demands.discrete.tour.ActivitySchedule;
import org.goplanit.demands.discrete.tour.ParticipantTour;
import org.goplanit.demands.discrete.tour.ScheduleElement;
import org.goplanit.demands.discrete.trip.Trip;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.time.LocalTimeUtils;

import java.time.LocalTime;
import java.util.List;

/**
 * Used only by the schedule jitter, see {@link ScheduleJitterExecutor}: a single time within a schedule that jitter
 * may move, paired with how to write it back. A schedule element carries at most two such times, its start and, for a
 * tour, its end.
 * <p>
 * A person's points are all the times on their schedule in order, those of their shared tours included. A shared
 * tour's trips are the same elements for every participant, but each participant reaches the tour's end through
 * their own participation. So the same end appears on a different element for each of them, see
 * {@link #isSameTimeAs(JitterPoint)}.
 * </p>
 *
 * @author markr
 */
final class JitterPoint {

  /** element the time belongs to */
  private final ScheduleElement element;

  /** when true the point is the element's tour end time, otherwise its start time */
  private final boolean tourEndTime;

  /** when true this time belongs to a tour shared by several participants and is drawn once, elsewhere. It still
   * takes part in the ordering of the schedule it appears in, but is never moved by that schedule's draw. The time
   * is the tour's, so moving it for one participant would move it for all, and could put another participant's day
   * out of order */
  private final boolean fixed;

  /** end of the draw range this point was drawn in, as seconds from the window start. A minimum tour duration may
   * move a tour's end later, but never past this, so it takes no room the bin left for the points after it */
  private long drawRangeEndElapsedSeconds;

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
   * tour itself rather than per participant, see {@link SharedTourJitterUtils#applySharedTourSubBinJitter}. Such points
   * are still collected here, because they take part in the order of the schedule they appear in and bound the points
   * around them, but they are marked as fixed so no participant's draw moves them
   * </p>
   *
   * @param schedule to collect the points of
   * @param pointsToPopulate to add the encountered points to, in order
   * @param withinSharedTour when true the points collected sit within a tour shared by several participants
   */
  public static void collect(
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
        collect(element.getSchedule(), pointsToPopulate, sharedTour);
      }

      if (element instanceof ParticipantTour && ((ParticipantTour) element).getEndTime() != null) {
        pointsToPopulate.add(new JitterPoint(element, true, sharedTour));
      }
    }
  }

  /**
   * Constructor
   *
   * @param element the time belongs to
   * @param tourEndTime when true the point is the element's tour end time, otherwise its start time
   * @param fixed when true the time belongs to a tour shared by several participants, drawn elsewhere
   */
  public JitterPoint(ScheduleElement element, boolean tourEndTime, boolean fixed) {
    this.element = element;
    this.tourEndTime = tourEndTime;
    this.fixed = fixed;
  }

  /**
   * Element the time belongs to
   *
   * @return the element
   */
  public ScheduleElement getElement() {
    return element;
  }

  /**
   * Whether the point is the element's tour end time rather than its start time
   *
   * @return true when it is the tour end time, false otherwise
   */
  public boolean isTourEndTime() {
    return tourEndTime;
  }

  /**
   * Whether the time belongs to a tour shared by several participants, drawn once elsewhere, so the draw of the
   * schedule it appears in never moves it
   *
   * @return true when it is fixed, false otherwise
   */
  public boolean isFixed() {
    return fixed;
  }

  /**
   * End of the draw range this point was drawn in
   *
   * @return the end as seconds from the window start
   */
  public long getDrawRangeEndElapsedSeconds() {
    return drawRangeEndElapsedSeconds;
  }

  /**
   * Set the end of the draw range this point was drawn in
   *
   * @param drawRangeEndElapsedSeconds the end as seconds from the window start
   */
  public void setDrawRangeEndElapsedSeconds(long drawRangeEndElapsedSeconds) {
    this.drawRangeEndElapsedSeconds = drawRangeEndElapsedSeconds;
  }

  /**
   * The time of this point
   *
   * @return the element's start time, or its tour's end time
   */
  public LocalTime getTime() {
    return tourEndTime ? ((ParticipantTour) element).getEndTime() : element.getStartTime();
  }

  /**
   * Write the given time back to where this point's time is held: the start of a trip, or the start or end of a tour
   *
   * @param time to set
   */
  public void setTime(LocalTime time) {
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

  /**
   * The time of this point as seconds from the start of the allowed window, so a time past midnight in a window
   * running past midnight counts as late, not early
   *
   * @param windowStartTime start of the allowed window as a time of day
   * @return the time as seconds from the window start
   */
  public long getElapsedSeconds(LocalTime windowStartTime) {
    return LocalTimeUtils.secondsFromWrapAroundDayAnchor(windowStartTime, getTime());
  }

  /**
   * Set the time of this point, given as seconds from the start of the allowed window
   *
   * @param elapsedSeconds the time as seconds from the window start
   * @param windowStartTime start of the allowed window as a time of day
   */
  public void setElapsedSeconds(long elapsedSeconds, LocalTime windowStartTime) {
    setTime(LocalTimeUtils.ofSecondOfDayWrapped(windowStartTime.toSecondOfDay() + elapsedSeconds));
  }

  /**
   * Whether this point is the same time in the model as the other: the start of the same element, or the end of
   * the same tour, which each participant of a shared tour sees through their own participation
   *
   * @param other point to compare with
   * @return true when both are the same time, false otherwise
   */
  public boolean isSameTimeAs(JitterPoint other) {
    if (tourEndTime != other.tourEndTime) {
      return false;
    }
    if (!tourEndTime) {
      return element == other.element;
    }
    return ((ParticipantTour) element).getTour() == ((ParticipantTour) other.element).getTour();
  }
}

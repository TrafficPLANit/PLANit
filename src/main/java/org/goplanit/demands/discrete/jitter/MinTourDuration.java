package org.goplanit.demands.discrete.jitter;

import org.goplanit.demands.discrete.tour.Tour;
import org.goplanit.utils.time.LocalTimeUtils;

import java.time.LocalTime;
import java.util.Set;

/**
 * Used only by the schedule jitter, see {@link ScheduleJitterExecutor}: the minimum duration given to some tours, and
 * which tours they are. A tour starting and ending in one bin would otherwise often be drawn with almost no duration.
 * The minimum is met only as far as there is room for it.
 *
 * @author markr
 */
final class MinTourDuration {

  /** tours given the minimum duration, may be null */
  private final Set<Tour> tours;

  /** the minimum duration in seconds */
  private final int durationSeconds;

  /**
   * Constructor
   *
   * @param tours given the minimum duration, may be null
   * @param durationSeconds the minimum duration in seconds, none when 0 or less
   */
  public MinTourDuration(Set<Tour> tours, int durationSeconds) {
    this.tours = tours;
    this.durationSeconds = durationSeconds;
  }

  /**
   * Whether any tour is given a minimum duration
   *
   * @return true when there is, false otherwise
   */
  public boolean appliesToAnyTour() {
    return durationSeconds > 0 && tours != null && !tours.isEmpty();
  }

  /**
   * The earliest end the minimum duration asks of a tour: its start plus the minimum
   *
   * @param tour to find the end for
   * @param windowStartTime start of the allowed window as a time of day
   * @return the end as seconds from the window start, null when the tour is not given a minimum duration
   */
  public Long findEndSeconds(Tour tour, LocalTime windowStartTime) {
    if (durationSeconds <= 0 || tours == null || !tours.contains(tour)) {
      return null;
    }
    return LocalTimeUtils.secondsFromWrapAroundDayAnchor(windowStartTime, tour.getStartTime()) + durationSeconds;
  }
}

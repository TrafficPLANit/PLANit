package org.goplanit.service.routed;

import java.time.LocalTime;

import org.goplanit.utils.network.layer.service.ServiceLegSegment;

/**
 * The schedule with on or more departures for a routed service as well as the relative timings of each leg for each departure. Each leg timing is in an ordered position, meaning
 * that the first timing represents the first leg of the routed service and the last leg the final leg etc.
 * 
 * @author markr
 *
 */
public interface RoutedTripSchedule extends RoutedTrip {

  /**
   * Access to the departures of this schedule
   * 
   * @return departures
   */
  public abstract RoutedTripDepartures getDepartures();

  /**
   * Clear all leg timings from the trip
   */
  public abstract void clearRelativeLegTimings();

  /**
   * Add a new leg's timing to the end of the already registered leg timings.
   * 
   * @param parentLegSegment (directed leg) to add to the trip's route
   * @param duration         duration of the leg segment
   * @param dwellTime        at the destination of the leg segment
   * @return the added timing
   */
  public abstract RelativeLegTiming addRelativeLegSegmentTiming(final ServiceLegSegment parentLegSegment, final LocalTime duration, final LocalTime dwellTime);

  /**
   * Collect a leg timing based on its index
   * 
   * @param index to collect
   * @return the relative leg timing found
   */
  public abstract RelativeLegTiming getRelativeLegTiming(int index);

  /**
   * Collect the number of registered leg timings
   * 
   * @return number of relative leg timings registered
   */
  public abstract int getRelativeLegTimingsSize();

}

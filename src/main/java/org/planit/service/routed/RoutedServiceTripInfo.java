package org.planit.service.routed;

/**
 * Interface to reflect one or more similar routed service trips by providing information on their route legs and schedule/frequencies.
 * 
 * @author markr
 *
 */
public interface RoutedServiceTripInfo extends Cloneable {

  /**
   * Be able to clone a RoutedServicetripInfo instance
   * 
   * @return shallow copy
   */
  public abstract RoutedServiceTripInfo clone();

  /**
   * Trips for a service can be frequency or schedule based, or have both. Via this method we collect only the frequency based trips
   * 
   * @return frequency based trips of the service
   */
  public abstract RoutedTripsFrequency getFrequencyBasedTrips();

  /**
   * Trips for a service can be frequency or schedule based, or have both. Via this method we collect only the schedule based trips
   * 
   * @return schedule based trips of the service
   */
  public abstract RoutedTripsSchedule getScheduleBasedTrips();

  /**
   * Verify if any frequency based trips exists
   * 
   * @return true when present, false otherwise
   */
  public default boolean hasFrequencyBasedTrips() {
    return getFrequencyBasedTrips() != null && !getFrequencyBasedTrips().isEmpty();
  }

  /**
   * Verify if any schedule based trips exists
   * 
   * @return True when present, false otherwise
   */
  public default boolean hasScheduleBasedTrips() {
    return getFrequencyBasedTrips() != null && !getFrequencyBasedTrips().isEmpty();
  }

  /**
   * Remove all registered frequency and schedule based trips
   */
  public abstract void reset();

}

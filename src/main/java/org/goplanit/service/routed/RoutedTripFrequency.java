package org.goplanit.service.routed;

import org.goplanit.utils.network.layer.service.ServiceLegSegment;

/**
 * Interface for frequency based trips of a RoutedService. The route is defined based on legs on the parent ServiceNetwork the RoutedService - and therefore the trip - resides on.
 * Legs are ordered such that the first leg represents the starting point and the last the end point. It is expected that the legs combined are contiguous and imply the direction
 * of the route by their ordering.
 * 
 * @author markr
 *
 */
public interface RoutedTripFrequency extends RoutedTrip, Iterable<ServiceLegSegment> {

  /**
   * Clear all legs from the trip
   */
  public abstract void clearLegs();

  /**
   * Collect the number of registered leg segments
   * 
   * @return number of registered leg segments
   */
  public abstract int getNumberOfLegSegments();

  /**
   * Add a new leg segment (directed leg) to the end of the already registered legs.
   * 
   * @param leg segment to add to the trip's route
   */
  public abstract void addLegSegment(ServiceLegSegment legSegment);

  /**
   * Get a leg segment in a particular position of the routed trip
   * 
   * @param index to collect segment for
   */
  public abstract ServiceLegSegment getLegSegment(int index);

  /**
   * Collect frequency per hour for this trip
   * 
   * 
   * @return frequencyPerHour
   */
  public abstract double getFrequencyPerHour();

  /**
   * Set the frequency per hour
   * 
   * @param frequencyPerHour to use
   */
  public abstract void setFrequencyPerHour(double frequencyPerHour);

  /**
   * Verify if a valid frequency is defined, must be positive to be valid
   * 
   * @return true when a positive frequency is defined, false otherwise
   */
  public default boolean hasValidFrequency() {
    return getFrequencyPerHour() > 0;
  }

  /**
   * Verify if there are leg segments registered
   * 
   * @return true when leg segments are registered, false otherwise
   */
  public default boolean hasLegSegments() {
    return getNumberOfLegSegments() > 0;
  }

  /**
   * Get first leg segment of the routed trip
   * 
   * @param first leg segment
   */
  public default ServiceLegSegment getFirstLegSegment() {
    return getLegSegment(0);
  }

  /**
   * Get last leg segment of the routed trip
   * 
   * @param last leg segment, null if none present
   */
  public default ServiceLegSegment getLastLegSegment() {
    if (!hasLegSegments()) {
      return null;
    }
    return getLegSegment(getNumberOfLegSegments() - 1);
  }

}

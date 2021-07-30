package org.planit.service.routed;

import org.planit.utils.network.layer.service.ServiceLeg;

/**
 * Interface for frequency based trips of a RoutedService. The route is defined based on legs on the parent ServiceNetwork the RoutedService - and therefore the trip - resides on.
 * Legs are ordered such that the first leg represents the starting point and the last the end point. It is expected that the legs combined are contiguous and imply the direction
 * of the route by their ordering.
 * 
 * @author markr
 *
 */
public interface RoutedTripFrequency extends RoutedTrip {

  /**
   * Clear all legs from the trip
   */
  public abstract void clearLegs();

  /**
   * Add a new leg to the end of the already registered legs.
   * 
   * @param leg to add to the trip's route
   */
  public abstract void addLeg(ServiceLeg leg);

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

}

package org.goplanit.service.routed;

/**
 * Interface for wrapper container class around RoutedTrip instances that define a frequency based schedule.
 * 
 * @author markr
 *
 */
public interface RoutedTripsFrequency extends RoutedTrips<RoutedTripFrequency> {

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract RoutedTripsFrequency clone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract RoutedTripFrequencyFactory getFactory();

}

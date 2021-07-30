package org.planit.service.routed;

/**
 * Interface for wrapper container class around RoutedTrip instances that define an explicit schedule.
 * 
 * @author markr
 *
 */
public interface RoutedTripsSchedule extends RoutedTrips<RoutedTripSchedule> {

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract RoutedTripsSchedule clone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract RoutedTripScheduleFactory getFactory();

}

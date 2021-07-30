package org.planit.service.routed;

import org.planit.utils.id.IdGroupingToken;

/**
 * Implementation of a RoutedServiceTripInfo interface
 * 
 * @author markr
 */
public class RoutedServiceTripInfoImpl implements RoutedServiceTripInfo {

  /** container for frequency based trips of this service */
  private final RoutedTripsFrequency frequencyBasedTrips;

  /** container for schedule based trips of this service */
  private final RoutedTripsSchedule scheduleBasedTrips;

  /**
   * Constructor
   * 
   * @param tokenId to use for id generation
   */
  public RoutedServiceTripInfoImpl(final IdGroupingToken tokenId) {
    this.frequencyBasedTrips = new RoutedTripsFrequencyImpl(tokenId);
    this.scheduleBasedTrips = new RoutedTripsScheduleImpl(tokenId);
  }

  /**
   * Copy constructor
   * 
   * @param routedServiceTripInfoImpl to copy
   */
  public RoutedServiceTripInfoImpl(RoutedServiceTripInfoImpl routedServiceTripInfoImpl) {
    this.frequencyBasedTrips = routedServiceTripInfoImpl.frequencyBasedTrips.clone();
    this.scheduleBasedTrips = routedServiceTripInfoImpl.scheduleBasedTrips.clone();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedServiceTripInfoImpl clone() {
    return new RoutedServiceTripInfoImpl(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripsFrequency getFrequencyBasedTrips() {
    return frequencyBasedTrips;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripsSchedule getScheduleBasedTrips() {
    return scheduleBasedTrips;
  }

}

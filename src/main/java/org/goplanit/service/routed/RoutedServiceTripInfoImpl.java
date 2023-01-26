package org.goplanit.service.routed;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.service.routed.RoutedServiceTripInfo;
import org.goplanit.utils.service.routed.RoutedTripsFrequency;
import org.goplanit.utils.service.routed.RoutedTripsSchedule;

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
    super();
    this.frequencyBasedTrips = new RoutedTripsFrequencyImpl(tokenId);
    this.scheduleBasedTrips = new RoutedTripsScheduleImpl(tokenId);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public RoutedServiceTripInfoImpl(RoutedServiceTripInfoImpl other, boolean deepCopy) {
    super();

    // container wrappers so require clone always
    this.frequencyBasedTrips  = deepCopy ? other.frequencyBasedTrips.deepClone()  : other.frequencyBasedTrips.clone();
    this.scheduleBasedTrips   = deepCopy ? other.scheduleBasedTrips.deepClone()   : other.scheduleBasedTrips.clone();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedServiceTripInfoImpl clone() {
    return new RoutedServiceTripInfoImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedServiceTripInfoImpl deepClone() {
    return new RoutedServiceTripInfoImpl(this, true);
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

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    this.frequencyBasedTrips.reset();
    this.scheduleBasedTrips.reset();
  }

}

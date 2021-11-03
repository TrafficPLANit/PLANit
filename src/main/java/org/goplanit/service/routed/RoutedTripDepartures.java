package org.goplanit.service.routed;

import org.goplanit.utils.id.ManagedIdEntities;

/**
 * A container class for departures registered on a schedule based routed trip
 * 
 * @author markr
 *
 */
public interface RoutedTripDepartures extends ManagedIdEntities<RoutedTripDeparture> {

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract RoutedTripDepartures clone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract RoutedTripDepartureFactory getFactory();
}

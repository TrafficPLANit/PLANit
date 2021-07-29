package org.planit.service.routed;

import org.planit.utils.id.ManagedIdEntities;

/**
 * Interface for wrapper container class around RoutedServiceTripInfo instances.
 * 
 * @author markr
 *
 */
public interface RoutedServiceTrips extends ManagedIdEntities<RoutedServiceTripInfo> {

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract RoutedServiceTrips clone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract RoutedServiceTripInfoFactory getFactory();

}

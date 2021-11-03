package org.goplanit.service.routed;

import org.goplanit.utils.id.ManagedIdEntities;

/**
 * Base class for routed trips container for some derived type of RoutedTrip (either schedule or frequency based for example).
 * 
 * @author markr
 */
public interface RoutedTrips<T extends RoutedTrip> extends ManagedIdEntities<T> {

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract RoutedTripFactory<T> getFactory();

}

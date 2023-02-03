package org.goplanit.service.routed;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.service.routed.RoutedTrip;
import org.goplanit.utils.service.routed.RoutedTrips;

/**
 * Base class for toued trips of some derived type (either schedule or frequency based for example).
 * 
 * @author markr
 */
public abstract class RoutedTripsImpl<T extends RoutedTrip> extends ManagedIdEntitiesImpl<T> implements RoutedTrips<T> {

  /** factory for this container class */
  protected RoutedTripFactoryImpl<T> factory;

  /**
   * The factory to use. To be set once by super class immediately after construction of the instance
   * 
   * @param factory to use
   */
  protected void setFactory(final RoutedTripFactoryImpl<T> factory) {
    this.factory = factory;
  }

  /**
   * Constructor
   * 
   * @param tokenId to use
   */
  protected RoutedTripsImpl(final IdGroupingToken tokenId) {
    super(T::getId, RoutedTrip.ROUTED_TRIP_ID_CLASS);
  }

  /**
   * Copy constructor, incomplete, requires derived class to explicitly set factory
   *
   * @param routedTripsBase to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected RoutedTripsImpl(RoutedTripsImpl<T> routedTripsBase, boolean deepCopy) {
    super(routedTripsBase, deepCopy);
    this.factory = null; // reset so it is clear it needs to be set by concrete implementing class afterwards
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripFactoryImpl<T> getFactory() {
    return factory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract RoutedTripsImpl shallowClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract RoutedTripsImpl deepClone();
}

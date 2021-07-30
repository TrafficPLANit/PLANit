package org.planit.service.routed;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.ManagedIdEntitiesImpl;

/**
 * Base class for toued trips of some derived type (either schedule or frequency based for example).
 * 
 * @author markr
 */
public abstract class RoutedTripsImpl<T extends RoutedTrip> extends ManagedIdEntitiesImpl<T> implements RoutedTrips<T> {

  /** factory for this container class */
  protected RoutedTripFactory<T> factory;

  /**
   * The factory to use. To be set once by super class immediately after construction of the instance
   * 
   * @param factory to use
   */
  protected void setFactory(final RoutedTripFactory<T> factory) {
    this.factory = factory;
  }

  /**
   * Constructor
   * 
   * @param tokenId           to use
   * @param routedTripFactory to use, ensure that the container on this factory is the class instance deriving from this class
   */
  protected RoutedTripsImpl(final IdGroupingToken tokenId) {
    super(T::getId, RoutedTrip.ROUTED_TRIP_ID_CLASS);
  }

  /**
   * Copy constructor
   * 
   * @param routedServiceTripsImpl to copy
   */
  public RoutedTripsImpl(RoutedTripsImpl<T> routedTripsBase) {
    super(routedTripsBase);
    this.factory = routedTripsBase.factory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripFactory<T> getFactory() {
    return factory;
  }

}

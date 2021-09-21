package org.planit.service.routed;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.ManagedIdEntityFactoryImpl;

/**
 * Factory for creating routed trips of type T
 * 
 * @param <T> type of routed trip
 * 
 * @author markr
 */
public abstract class RoutedTripFactory<T extends RoutedTrip> extends ManagedIdEntityFactoryImpl<T> {

  /** container to use */
  protected final RoutedTrips<T> routedTrips;

  /**
   * Create a newly created instance without registering on the container
   * 
   * @return created routed trip instance
   */
  protected abstract T createNew();

  /**
   * Constructor
   * 
   * @param tokenId     to use
   * @param routedTrips to use
   */
  protected RoutedTripFactory(final IdGroupingToken tokenId, final RoutedTrips<T> routedTrips) {
    super(tokenId);
    this.routedTrips = routedTrips;
  }

  /**
   * Register a newly created instance on the underlying container
   * 
   * @return created instance
   */
  public T registerNew() {
    T newRoutedTrip = createNew();
    routedTrips.register(newRoutedTrip);
    return newRoutedTrip;
  }

}

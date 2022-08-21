package org.goplanit.service.routed;

import java.time.LocalTime;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;
import org.goplanit.utils.time.ExtendedLocalTime;

/**
 * Factory for creating routed trip departure instances (on container)
 * 
 * @author markr
 */
public class RoutedTripDepartureFactory extends ManagedIdEntityFactoryImpl<RoutedTripDeparture> {

  /** container to use */
  protected final RoutedTripDepartures routedTripDepartures;

  /**
   * Create a newly created instance without registering on the container
   * 
   * @param departureTime the departure time
   * @return created routed services layer
   */
  protected RoutedTripDeparture createNew(ExtendedLocalTime departureTime) {
    return new RoutedTripDeparture(getIdGroupingToken(), departureTime);
  }

  /**
   * Constructor
   * 
   * @param tokenId              to use
   * @param routedTripDepartures to use
   */
  protected RoutedTripDepartureFactory(final IdGroupingToken tokenId, final RoutedTripDepartures routedTripDepartures) {
    super(tokenId);
    this.routedTripDepartures = routedTripDepartures;
  }

  /**
   * Register a newly created instance on the underlying container
   * 
   * @param departureTime the departure time (which is allowed to be beyond midnight of that day)
   * @return created instance
   */
  public RoutedTripDeparture registerNew(final ExtendedLocalTime departureTime) {
    RoutedTripDeparture newDeparture = createNew(departureTime);
    routedTripDepartures.register(newDeparture);
    return newDeparture;
  }

}

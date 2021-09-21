package org.planit.service.routed;

import java.time.LocalTime;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.ManagedIdEntityFactoryImpl;

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
  protected RoutedTripDeparture createNew(LocalTime departureTime) {
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
   * @param departureTime the departure time
   * @return created instance
   */
  public RoutedTripDeparture registerNew(final LocalTime departureTime) {
    RoutedTripDeparture newDeparture = createNew(departureTime);
    routedTripDepartures.register(newDeparture);
    return newDeparture;
  }

}

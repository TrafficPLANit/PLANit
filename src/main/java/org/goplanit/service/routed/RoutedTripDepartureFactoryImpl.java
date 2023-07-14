package org.goplanit.service.routed;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;
import org.goplanit.utils.service.routed.RoutedTripDeparture;
import org.goplanit.utils.service.routed.RoutedTripDepartureFactory;
import org.goplanit.utils.service.routed.RoutedTripDepartures;
import org.goplanit.utils.time.ExtendedLocalTime;

/**
 * Factory for creating routed trip departure instances (on container)
 * 
 * @author markr
 */
public class RoutedTripDepartureFactoryImpl extends ManagedIdEntityFactoryImpl<RoutedTripDeparture> implements RoutedTripDepartureFactory {

  /** container to use */
  protected final RoutedTripDepartures routedTripDepartures;

  /**
   * Create a newly created instance without registering on the container
   * 
   * @param departureTime the departure time
   * @return created routed services layer
   */
  protected RoutedTripDepartureImpl createNew(ExtendedLocalTime departureTime) {
    return new RoutedTripDepartureImpl(getIdGroupingToken(), departureTime);
  }

  /**
   * Constructor
   * 
   * @param tokenId              to use
   * @param routedTripDepartures to use
   */
  protected RoutedTripDepartureFactoryImpl(final IdGroupingToken tokenId, final RoutedTripDepartures routedTripDepartures) {
    super(tokenId);
    this.routedTripDepartures = routedTripDepartures;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripDepartureImpl registerNew(final ExtendedLocalTime departureTime) {
    RoutedTripDepartureImpl newDeparture = createNew(departureTime);
    routedTripDepartures.register(newDeparture);
    return newDeparture;
  }

}

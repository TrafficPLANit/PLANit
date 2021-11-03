package org.goplanit.service.routed;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;

public class RoutedTripDeparturesImpl extends ManagedIdEntitiesImpl<RoutedTripDeparture> implements RoutedTripDepartures {

  /** The factory to use */
  RoutedTripDepartureFactory factory;

  /**
   * Constructor
   * 
   * @param tokenId to use
   */
  protected RoutedTripDeparturesImpl(final IdGroupingToken tokenId) {
    super(RoutedTripDeparture::getId, RoutedTripDeparture.ROUTED_TRIP_DEPARTURE_ID_CLASS);
    this.factory = new RoutedTripDepartureFactory(tokenId, this);
  }

  /**
   * Copy constructor
   * 
   * @param routedServiceTripsImpl to copy
   */
  public RoutedTripDeparturesImpl(RoutedTripDeparturesImpl routedTripDepartures) {
    super(routedTripDepartures);
    this.factory = routedTripDepartures.factory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripDepartureFactory getFactory() {
    return factory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripDeparturesImpl clone() {
    return new RoutedTripDeparturesImpl(this);
  }
}

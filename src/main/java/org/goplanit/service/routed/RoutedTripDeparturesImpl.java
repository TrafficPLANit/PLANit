package org.goplanit.service.routed;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.service.routed.RoutedTripDeparture;
import org.goplanit.utils.service.routed.RoutedTripDepartureFactory;
import org.goplanit.utils.service.routed.RoutedTripDepartures;

/**
 * Class that manages all routed trip departures
 * 
 * @author markr
 *
 */
public class RoutedTripDeparturesImpl extends ManagedIdEntitiesImpl<RoutedTripDeparture> implements RoutedTripDepartures {

  /** The factory to use */
  RoutedTripDepartureFactoryImpl factory;

  /**
   * Constructor
   * 
   * @param tokenId to use
   */
  protected RoutedTripDeparturesImpl(final IdGroupingToken tokenId) {
    super(RoutedTripDeparture::getId, RoutedTripDeparture.ROUTED_TRIP_DEPARTURE_ID_CLASS);
    this.factory = new RoutedTripDepartureFactoryImpl(tokenId, this);
  }

  /**
   * Copy constructor
   * 
   * @param routedTripDepartures to copy
   */
  public RoutedTripDeparturesImpl(RoutedTripDeparturesImpl routedTripDepartures) {
    super(routedTripDepartures);
    this.factory = routedTripDepartures.factory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripDepartureFactoryImpl getFactory() {
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

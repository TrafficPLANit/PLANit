package org.goplanit.service.routed;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.service.routed.RoutedTripDeparture;
import org.goplanit.utils.service.routed.RoutedTripDepartureFactory;
import org.goplanit.utils.service.routed.RoutedTripDepartures;

import java.time.LocalTime;

/**
 * Class that manages all routed trip departures for a given routed trip schedule
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
   * Copy constructor, also creates new factory with this as its underlying container
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public RoutedTripDeparturesImpl(RoutedTripDeparturesImpl other, boolean deepCopy) {
    super(other, deepCopy);
    this.factory =
            new RoutedTripDepartureFactoryImpl(other.factory.getIdGroupingToken(), this);
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
  public void allDepartLaterBy(LocalTime departureTimeIncrease) {
    forEach( routedTripDeparture -> routedTripDeparture.departLater(departureTimeIncrease));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void allDepartEarlierBy(LocalTime departureTimeIncrease) {
    forEach( routedTripDeparture -> routedTripDeparture.departEarlier(departureTimeIncrease));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripDeparturesImpl clone() {
    return new RoutedTripDeparturesImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripDeparturesImpl deepClone() {
    return new RoutedTripDeparturesImpl(this, true);
  }
}

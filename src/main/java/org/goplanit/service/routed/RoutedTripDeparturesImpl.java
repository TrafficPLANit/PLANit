package org.goplanit.service.routed;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.service.routed.RoutedServicesLayer;
import org.goplanit.utils.service.routed.RoutedTripDeparture;
import org.goplanit.utils.service.routed.RoutedTripDepartures;

import java.time.LocalTime;
import java.util.function.BiConsumer;

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
   * @param mapper to use for tracking mapping between original and copied entity (may be null)
   */
  public RoutedTripDeparturesImpl(RoutedTripDeparturesImpl other, boolean deepCopy, BiConsumer<RoutedTripDeparture, RoutedTripDeparture> mapper) {
    super(other, deepCopy, mapper);
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
  public RoutedTripDeparturesImpl shallowClone() {
    return new RoutedTripDeparturesImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripDeparturesImpl deepClone() {
    return new RoutedTripDeparturesImpl(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripDeparturesImpl deepCloneWithMapping(BiConsumer<RoutedTripDeparture, RoutedTripDeparture> mapper) {
    return new RoutedTripDeparturesImpl(this, true, mapper);
  }

  /** String representation of departures
   *
   * @return departures of this instance in string form
   */
  @Override
  public String toString(){
    var sb = new StringBuilder("Departures: ");
    forEach( d -> sb.append(d).append(':'));
    return sb.toString();
  }

}

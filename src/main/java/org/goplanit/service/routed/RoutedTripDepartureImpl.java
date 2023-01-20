package org.goplanit.service.routed;

import java.time.LocalTime;

import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedId;
import org.goplanit.utils.service.routed.RoutedTripDeparture;
import org.goplanit.utils.service.routed.RoutedTripDepartures;
import org.goplanit.utils.time.ExtendedLocalTime;

/**
 * A representation of a departure within a routed trip
 * 
 * @author markr
 *
 */
public class RoutedTripDepartureImpl extends ExternalIdAbleImpl implements RoutedTripDeparture {

  /** departure time of this instance */
  private final ExtendedLocalTime departureTime;

  /**
   * Generate id for instances of this class based on the token and class identifier
   * 
   * @param tokenId to use
   * @return generated id
   */
  protected static long generateId(IdGroupingToken tokenId) {
    return IdGenerator.generateId(tokenId, ROUTED_TRIP_DEPARTURE_ID_CLASS);
  }

  /**
   * Constructor
   * 
   * @param tokenId       to use for id generation
   * @param departureTime to use
   */
  public RoutedTripDepartureImpl(final IdGroupingToken tokenId, ExtendedLocalTime departureTime) {
    super(generateId(tokenId));
    this.departureTime = departureTime;
  }

  /**
   * Copy constructor
   * 
   * @param routedTripDeparture to copy
   */
  public RoutedTripDepartureImpl(RoutedTripDepartureImpl routedTripDeparture) {
    super(routedTripDeparture);
    this.departureTime = routedTripDeparture.departureTime;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<RoutedTripDeparture> getIdClass() {
    return ROUTED_TRIP_DEPARTURE_ID_CLASS;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    long newId = generateId(tokenId);
    setId(newId);
    return newId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripDepartureImpl clone() {
    return new RoutedTripDepartureImpl(this);
  }
}

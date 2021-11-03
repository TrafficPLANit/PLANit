package org.goplanit.service.routed;

import java.time.LocalTime;

import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedId;

/**
 * A representation of a departure within a routed trip
 * 
 * @author markr
 *
 */
public class RoutedTripDeparture extends ExternalIdAbleImpl implements ManagedId {

  /** id class for generating ids */
  public static final Class<RoutedTripDeparture> ROUTED_TRIP_DEPARTURE_ID_CLASS = RoutedTripDeparture.class;

  /** departure time of this instance */
  private final LocalTime departureTime;

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
  public RoutedTripDeparture(final IdGroupingToken tokenId, LocalTime departureTime) {
    super(generateId(tokenId));
    this.departureTime = departureTime;
  }

  /**
   * Copy constructor
   * 
   * @param routedTripDeparture to copy
   */
  public RoutedTripDeparture(RoutedTripDeparture routedTripDeparture) {
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
  public RoutedTripDeparture clone() {
    return new RoutedTripDeparture(this);
  }
}

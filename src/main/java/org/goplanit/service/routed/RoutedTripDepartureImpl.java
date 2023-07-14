package org.goplanit.service.routed;

import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.service.routed.RoutedTripDeparture;
import org.goplanit.utils.time.ExtendedLocalTime;

import java.time.LocalTime;
import java.util.logging.Logger;

/**
 * An representation of a departure within a routed trip
 * 
 * @author markr
 *
 */
public class RoutedTripDepartureImpl extends ExternalIdAbleImpl implements RoutedTripDeparture {

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(RoutedTripDepartureImpl.class.getCanonicalName());

  /** Departure time of this instance */
  private ExtendedLocalTime departureTime;

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
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public RoutedTripDepartureImpl(RoutedTripDepartureImpl routedTripDeparture, boolean deepCopy /* no impact yet */) {
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
  public ExtendedLocalTime getDepartureTime() {
    return departureTime;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripDepartureImpl shallowClone() {
    return new RoutedTripDepartureImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripDepartureImpl deepClone() {
    return new RoutedTripDepartureImpl(this, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void departLater(LocalTime departureTimeIncrease) {
    if(!ExtendedLocalTime.isNanosValid(departureTime.toNanoOfExtendedDay() + departureTimeIncrease.toNanoOfDay())){
      LOGGER.warning(String.format("Unable to depart later by % when existing departure is at %s", departureTimeIncrease, this));
    }
    departureTime = departureTime.plus(ExtendedLocalTime.of(departureTimeIncrease));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void departEarlier(LocalTime departureTimeDecrease) {
    if(!ExtendedLocalTime.isNanosValid(departureTime.toNanoOfExtendedDay() - departureTimeDecrease.toNanoOfDay())){
      LOGGER.warning(String.format("Unable to depart earlier by % when existing departure is at %s", departureTimeDecrease, this));
    }
    departureTime = departureTime.minus(ExtendedLocalTime.of(departureTimeDecrease));
  }

  public String toString(){
    return String.format("Departure (id: %d xmlId: %s): %s", getId(), getXmlId(), getDepartureTime());
  }
}

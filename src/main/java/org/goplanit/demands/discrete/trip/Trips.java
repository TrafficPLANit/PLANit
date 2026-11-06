package org.goplanit.demands.discrete.trip;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;

import java.util.function.BiConsumer;

/**
 * Class to register and store trips
 *
 * @author garym, markr
 */
public final class Trips extends ManagedIdEntitiesImpl<Trip> {

  /** factory to create instances on this container */
  private final TripsFactory factory;

  /**
   * Constructor
   *
   * @param tokenId  to use for id generation
   */
  public Trips(final IdGroupingToken tokenId) {
    super(Trip::getId, Trip.TRIP_ID_CLASS);
    this.factory = new TripsFactory(tokenId, this);
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param mapper to apply in case of deep copy to each original to copy combination (when provided, may be null)
   */
  public Trips(Trips other, boolean deepCopy, BiConsumer<Trip, Trip> mapper) {
    super(other, deepCopy, mapper);
    this.factory = new TripsFactory(other.getFactory().getIdGroupingToken(), this);
  }


  /**
   * Retrieve by its xml Id
   * <p>
   * This method is not efficient, since it loops through all the registered entries in order to find the
   * required one.
   *
   * @param xmlId the XML Id of the entity
   * @return the retrieved entity, or null if nothing was found
   */
  public Trip getByXmlId(final String xmlId) {
    return firstMatch(trip -> xmlId.equals(trip.getXmlId()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TripsFactory getFactory() {
    return this.factory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Trips shallowClone() {
    return new Trips(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  public Trips deepClone() {
    return new Trips(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Trips deepCloneWithMapping(BiConsumer<Trip, Trip> mapper) {
    return new Trips(this, true, mapper);
  }
}

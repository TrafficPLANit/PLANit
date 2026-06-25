package org.goplanit.demands.discrete.trip;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactory;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;

/**
 * Factory class for trip instances to be registered on its parent container passed in to constructor
 */
public class TripsFactory extends ManagedIdEntityFactoryImpl<Trip>
    implements ManagedIdEntityFactory<Trip> {

  /** container to use */
  protected final Trips trips;

  /**
   * Create a newly created instance without registering on the container
   *
   * @return created time period
   */
  protected Trip createNew() {
    return new Trip(getIdGroupingToken());
  }

  /**
   * Constructor
   *
   * @param tokenId    to use
   * @param trips to use
   */
  protected TripsFactory(final IdGroupingToken tokenId, final Trips trips) {
    super(tokenId);
    this.trips = trips;
  }

  /**
   * register a new entry on the container and return it
   *
   * @return created instance
   */
  public Trip registerNew() {
    var newInstance = new Trip(getIdGroupingToken());
    trips.register(newInstance);
    return newInstance;
  }

}

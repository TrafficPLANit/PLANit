package org.goplanit.demands.discrete.trip;

import org.goplanit.demands.discrete.tour.ActivitySchedule;
import org.goplanit.demands.discrete.tour.Tour;
import org.goplanit.demands.discrete.tour.TourImpl;
import org.goplanit.demands.discrete.util.DirectionBound;
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
    return new TripImpl(getIdGroupingToken());
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
    var newInstance = new TripImpl(getIdGroupingToken());
    trips.register(newInstance);
    return newInstance;
  }

  /**
   * register a new entry on the container and return it
   *
   * @param parentTour tour the trip belongs to
   * @param direction direction of trip within tour
   * @param addToTourSchedule when true we add the trip to the tour's schedule
   * @return created instance
   */
  public Trip registerNew(Tour parentTour, DirectionBound direction, boolean addToTourSchedule) {
    var newInstance = new TripImpl(getIdGroupingToken());
    newInstance.setDirection(direction);
    newInstance.setTour(parentTour);
    if(addToTourSchedule){
      if(!parentTour.hasSchedule()){
        parentTour.setSchedule(new ActivitySchedule());
      }
      parentTour.getSchedule().add(newInstance);
    }
    trips.register(newInstance);
    return newInstance;
  }

}

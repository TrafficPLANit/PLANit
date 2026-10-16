package org.goplanit.demands.discrete.trip;

import org.goplanit.demands.discrete.tour.ActivitySchedule;
import org.goplanit.demands.discrete.tour.Tour;
import org.goplanit.demands.discrete.tour.TourImpl;
import org.goplanit.demands.discrete.util.DirectionBound;
import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.zoning.OdZone;

import java.time.LocalTime;
import java.util.logging.Logger;

/**
 * Represents a trip.
 * 
 * @author markr
 *
 */
public class TripImpl extends ExternalIdAbleImpl implements Trip {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(TripImpl.class.getCanonicalName());

  /** Tour the trip belongs to (can be a sub-tour), for now must be present as origin/destination is stored on tour */
  private Tour tour;

  /** purpose of the tour. "smaller" than purpose of a tour as it encompasses single leg */
  private String purpose;

  /** mode of the trip */
  private Mode mode;

  /** start time of the trip */
  private LocalTime startTime;

  /** trip direction, default is always outbound, but if part of a tour, it may be inbound */
  private DirectionBound direction = DirectionBound.OUTBOUND;

  /** trip origin, which in case of multi-trip trips for a single tour direction is needed to uniquely determine
   * the O/D of the trip, can be left null if this is single trip direction of tour */
  private OdZone origin;

  /** trip destination, which in case of multi-trip trips for a single tour direction is needed to uniquely determine
   * the O/D of the trip, can be left null if this is single trip direction of tour  */
  private OdZone destination;

  /**
   * Generate id for instances of this class based on the token and class identifier
   *
   * @param tokenId to use
   * @return generated id
   */
  protected static long generateId(IdGroupingToken tokenId) {
    return IdGenerator.generateId(tokenId, TripImpl.TRIP_ID_CLASS);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<? extends Trip> getIdClass() {
    return TRIP_ID_CLASS;
  }

  /**
   * Constructor
   *
   * @param groupId          contiguous id generation within this group for instances of this class
   */
  public TripImpl(IdGroupingToken groupId) {
    super(IdGenerator.generateId(groupId, TRIP_ID_CLASS));
  }

  /**
   * Copy constructor
   *
   * @param trip to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public TripImpl(TripImpl trip, boolean deepCopy /* no impact yet */) {
    super(trip);
    this.tour = trip.tour;
    this.direction = trip.direction;
    this.purpose = trip.purpose;
    this.mode = trip.mode;
  }

  /**
   * Purpose
   * @return purpose
   */
  @Override
  public String getPurpose() {
    return purpose;
  }

  /**
   * Purpose
   * @param purpose to set
   */
  @Override
  public void setPurpose(String purpose) {
    this.purpose = purpose;
  }

  /**
   * mode
   * @return mode
   */
  @Override
  public Mode getMode() {
    return mode;
  }

  /**
   * mode
   * @param mode to set
   */
  @Override
  public void setMode(Mode mode) {
    this.mode = mode;
  }

  /**
   * Access to tour
   * @return tour
   */
  @Override
  public Tour getTour() {
    return tour;
  }

  /**
   * set tour
   * @param  tour to use
   */
  @Override
  public void setTour(Tour tour) {
    this.tour = tour;
  }

  /**
   * Directionality
   * @return directionality
   */
  @Override
  public DirectionBound getDirection() {
    return direction;
  }

  /**
   * Set directionality
   * @param direction to use
   */
  @Override
  public void setDirection(DirectionBound direction) {
    this.direction = direction;
  }

  /**
   * access to departure time
   * @return dep time
   */
  @Override
  public LocalTime getStartTime() {
    return startTime;
  }

  /**
   * set departure time
   * @param startTime to use
   */
  @Override
  public void setStartTime(LocalTime startTime) {
    this.startTime = startTime;
  }

  /**
   * A trip has no internal schedule as it is always a "leaf" element of a schedule
   */
  @Override
  public ActivitySchedule getSchedule() {
    return null;
  }

  /**
   * Outbound mode is the same as the trip's mode
   * @return trip mode
   */
  @Override
  public Mode getOutboundMode() {
    return getMode();
  }

  @Override
  public Mode getInboundMode() {
    return getMode();
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
  public TripImpl shallowClone() {
    return new TripImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TripImpl deepClone() {
    return new TripImpl(this, true);
  }

  /**
   * Output this object as a String
   * 
   * @return String containing the value of this
   */
  @Override
  public String toString() {
    return getIdsAsString();
  }

  /**
   * Get the value of origin.
   *
   * @param useTourIfNotSet if true derive from tour based on direction logic, only valid in single trip tour leg
   * @return value of origin
   */
  @Override
  public OdZone getOrigin(boolean useTourIfNotSet) {
    if(origin != null || !useTourIfNotSet) {
      return origin;
    }else if(getTour()!=null){
      return getDirection().equals(DirectionBound.OUTBOUND) ? getTour().getOrigin() : getTour().getDestination();
    }
    return null;
  }

  /**
   * Set the value of origin.
   *
   * @param origin value of origin
   */
  @Override
  public void setOrigin(OdZone origin) {
    this.origin = origin;
  }

  /**
   * Get the value of destination.
   *
   * @param useTourIfNotSet if true derive from tour based on direction logic, only valid in single trip tour leg
   * @return value of destination
   */
  @Override
  public OdZone getDestination(boolean useTourIfNotSet) {
    if(destination != null || !useTourIfNotSet) {
      return destination;
    }else if(getTour()!=null){
      return getDirection().equals(DirectionBound.OUTBOUND) ? getTour().getDestination() : getTour().getOrigin();
    }
    return null;
  }

  /**
   * Set the value of destination.
   *
   * @param destination value of destination
   */
  @Override
  public void setDestination(OdZone destination) {
    this.destination = destination;
  }
}

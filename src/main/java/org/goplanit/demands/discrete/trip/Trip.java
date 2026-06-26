package org.goplanit.demands.discrete.trip;

import org.goplanit.demands.discrete.tour.ScheduleElement;
import org.goplanit.demands.discrete.tour.Tour;
import org.goplanit.demands.discrete.tour.Schedule;
import org.goplanit.demands.discrete.util.DirectionBound;
import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedId;
import org.goplanit.utils.mode.Mode;

import java.time.LocalTime;
import java.util.logging.Logger;

/**
 * Represents a trip.
 * 
 * @author markr
 *
 */
public class Trip extends ExternalIdAbleImpl implements ManagedId, ScheduleElement {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(Trip.class.getCanonicalName());

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

  /**
   * Generate id for instances of this class based on the token and class identifier
   *
   * @param tokenId to use
   * @return generated id
   */
  protected static long generateId(IdGroupingToken tokenId) {
    return IdGenerator.generateId(tokenId, Trip.TRIP_ID_CLASS);
  }

  /** id class for generating ids */
  public static final Class<Trip> TRIP_ID_CLASS = Trip.class;

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
  public Trip(IdGroupingToken groupId) {
    super(IdGenerator.generateId(groupId, TRIP_ID_CLASS));
  }

  /**
   * Copy constructor
   *
   * @param trip to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public Trip(Trip trip, boolean deepCopy /* no impact yet */) {
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
  public String getPurpose() {
    return purpose;
  }

  /**
   * Purpose
   * @param purpose to set
   */
  public void setPurpose(String purpose) {
    this.purpose = purpose;
  }

  /**
   * mode
   * @return mode
   */
  public Mode getMode() {
    return mode;
  }

  /**
   * mode
   * @param mode to set
   */
  public void setMode(Mode mode) {
    this.mode = mode;
  }

  /**
   * Access to tour
   * @return tour
   */
  public Tour getTour() {
    return tour;
  }

  /**
   * set tour
   * @param  tour to use
   */
  public void setTour(Tour tour) {
    this.tour = tour;
  }

  /**
   * Directionality
   * @return directionality
   */
  public DirectionBound getDirection() {
    return direction;
  }

  /**
   * Set directionality
   * @param direction to use
   */
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
  public void setStartTime(LocalTime startTime) {
    this.startTime = startTime;
  }

  /**
   * A trip has no internal schedule as it is always a "leaf" element of a schedule
   */
  @Override
  public Schedule getSchedule() {
    return null;
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
  public Trip shallowClone() {
    return new Trip(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Trip deepClone() {
    return new Trip(this, true);
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

}

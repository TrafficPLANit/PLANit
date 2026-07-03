package org.goplanit.demands.discrete.tour;

import org.goplanit.demands.discrete.person.Person;
import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedId;
import org.goplanit.utils.zoning.OdZone;

import java.time.LocalTime;
import java.util.logging.Logger;

/**
 * Represents a tour.
 * 
 * @author markr
 *
 */
public class Tour extends ExternalIdAbleImpl implements ManagedId, ScheduleElement {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(Tour.class.getCanonicalName());

  /** Person the tour belongs to */
  private Person person;

  /** origin zone of the tour */
  private OdZone origin;

  /** destination zone of the tour */
  private OdZone destination;

  /** if this is a sub tour performed at destination of its parent, we set the parent here */
  private Tour parentTour;

  /** purpose of the tour. "bigger" than purpose of a trip as it encompasses the entire tour */
  private String purpose;

  /** tour schedule contains order tour schedule elements which can either be trips, and/or sub tours. When it is a
   * sub tour, the trips or sub tours within the sub tour are ordered as well and assumed to be carried out before the
   * next element on the schedule at the level where the sub tour was scheduled
   */
  private ActivitySchedule schedule;

  /** start time of tour, i.e., moment of departure of outbound leg */
  private LocalTime startTime;

  /** end time of tour, i.e., moment of arrival back at start of inbound leg */
  private LocalTime endTime;

  /**
   * Generate id for instances of this class based on the token and class identifier
   *
   * @param tokenId to use
   * @return generated id
   */
  protected static long generateId(IdGroupingToken tokenId) {
    return IdGenerator.generateId(tokenId, Tour.TOUR_ID_CLASS);
  }

  /** id class for generating ids */
  public static final Class<Tour> TOUR_ID_CLASS = Tour.class;

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<? extends Tour> getIdClass() {
    return TOUR_ID_CLASS;
  }

  /**
   * Constructor
   *
   * @param groupId          contiguous id generation within this group for instances of this class
   */
  public Tour(IdGroupingToken groupId) {
    super(IdGenerator.generateId(groupId, TOUR_ID_CLASS));
  }

  /**
   * Copy constructor
   *
   * @param tour to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public Tour(Tour tour, boolean deepCopy /* no impact yet */) {
    super(tour);
    this.person = tour.person;
    this.origin = tour.origin;
    this.destination = tour.destination;
    this.purpose = tour.purpose;
    this.parentTour = tour.parentTour;
    this.startTime = tour.startTime;
    this.endTime = tour.endTime;
    this.schedule = deepCopy ? tour.schedule.deepClone() : tour.schedule.shallowClone();
  }

  /**
   * Access to person
   * @return person
   */
  public Person getPerson() {
    return person;
  }

  /**
   * person
   * @param person to set
   */
  public void setPerson(Person person) {
    this.person = person;
  }

  /**
   * Access to origin
   * @return origin
   */
  public OdZone getOrigin() {
    return origin;
  }

  /**
   * origin
   * @param origin to set
   */
  public void setOrigin(OdZone origin) {
    this.origin = origin;
  }

  /**
   * Access to destination
   * @return destination
   */
  public OdZone getDestination() {
    return destination;
  }

  /**
   * destination
   * @param destination to set
   */
  public void setDestination(OdZone destination) {
    this.destination = destination;
  }

  /**
   * origin and destination
   *
   * @param origin to set
   * @param destination to set
   */
  public void setOriginDestination(OdZone origin, OdZone destination) {
    setOrigin(origin);
    setDestination(destination);
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
  public void setPurpose(String purpose) {
    this.purpose = purpose;
  }

  /**
   * Access to parent tour (if any)
   * @return parent
   */
  public Tour getParentTour(){
    return this.parentTour;
  }

  /**
   * check presence
   * @return true when present
   */
  public boolean hasParentTour(){
    return getParentTour()!=null;
  }

  /**
   * parent tour
   * @param parent to set
   */
  public void setParentTour(Tour parent){
    this.parentTour = parent;
  }

  /**
   * Set start time of the tour, i.e., departure time from the origin
   * @param startTime to set
   */
  public void setStartTime(LocalTime startTime) {
    this.startTime = startTime;
  }

  /**
   * Set end time of tour, i.e., arrival time back at the origin
   * @param endTime to set
   */
  public void setEndTime(LocalTime endTime) {
    this.endTime = endTime;
  }

  /**
   * Set start end time
   * @param startTime to set
   * @param endTime to set
   */
  public void setStartEndTime(LocalTime startTime, LocalTime endTime) {
    setStartTime(startTime);
    setEndTime(endTime);
  }

  /**
   * departure time
   * @return departureTime
   */
  @Override
  public LocalTime getStartTime() {
    return this.startTime;
  }

  /**
   * arrivalTime time
   * @return arrivalTime
   */
  public LocalTime getEndTime() {
    return this.endTime;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ActivitySchedule getSchedule() {
    return schedule;
  }

  /**
   * set the schedule
   * @param schedule to use
   */
  public void setSchedule(ActivitySchedule schedule) {
    this.schedule = schedule;
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
  public Tour shallowClone() {
    return new Tour(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Tour deepClone() {
    return new Tour(this, true);
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

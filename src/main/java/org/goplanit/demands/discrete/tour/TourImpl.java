package org.goplanit.demands.discrete.tour;

import org.goplanit.demands.discrete.person.Person;
import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedId;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.zoning.OdZone;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Represents a tour.
 * 
 * @author markr
 *
 */
public class TourImpl extends ExternalIdAbleImpl implements Tour {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(TourImpl.class.getCanonicalName());

  /** the persons participating in this tour and their role, primary participant first */
  private final List<ParticipantTour> participantTours;

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
  public TourImpl(IdGroupingToken groupId) {
    super(IdGenerator.generateId(groupId, TOUR_ID_CLASS));
    this.participantTours = new ArrayList<>(1); // a single participant is the norm
    setSchedule(new ActivitySchedule()); // each tour has a schedule
  }

  /**
   * Copy constructor. The deep copy flag governs the schedule, i.e. whether the trips and sub tours are shared with
   * the original or cloned along. Participations are always recreated regardless: a participation binds a person to
   * one specific tour and holds a reference back to it, so sharing them would leave this copy with participations
   * that claim to belong to the original tour
   *
   * @param tour to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public TourImpl(TourImpl tour, boolean deepCopy) {
    super(tour);
    this.participantTours = new ArrayList<>(tour.participantTours.size());
    tour.participantTours.forEach(pt -> addParticipant(pt.getPerson(), pt.getRole()));
    this.origin = tour.origin;
    this.destination = tour.destination;
    this.purpose = tour.purpose;
    this.parentTour = tour.parentTour;
    this.startTime = tour.startTime;
    this.endTime = tour.endTime;
    this.schedule = deepCopy ? tour.schedule.deepClone() : tour.schedule.shallowClone();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<ParticipantTour> getParticipantTours() {
    return participantTours;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ParticipantTour addParticipant(Person person, TourParticipantRole role) {
    var participantTour = new ParticipantTourImpl(this, person, role);
    if(role != null && role.isPrimary()){
      /* keep the primary participant first, which getPrimaryParticipant and the writers rely on */
      participantTours.add(0, participantTour);
    }else{
      participantTours.add(participantTour);
    }
    return participantTour;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeParticipant(ParticipantTour participantTour) {
    return participantTours.remove(participantTour);
  }

  /**
   * Access to origin
   * @return origin
   */
  @Override
  public OdZone getOrigin() {
    return origin;
  }

  /**
   * origin
   * @param origin to set
   */
  @Override
  public void setOrigin(OdZone origin) {
    this.origin = origin;
  }

  /**
   * Access to destination
   * @return destination
   */
  @Override
  public OdZone getDestination() {
    return destination;
  }

  /**
   * destination
   * @param destination to set
   */
  @Override
  public void setDestination(OdZone destination) {
    this.destination = destination;
  }

  /**
   * origin and destination
   *
   * @param origin to set
   * @param destination to set
   */
  @Override
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
  @Override
  public void setPurpose(String purpose) {
    this.purpose = purpose;
  }

  /**
   * Access to parent tour (if any)
   * @return parent
   */
  @Override
  public Tour getParentTour(){
    return this.parentTour;
  }

  /**
   * parent tour
   * @param parent to set
   */
  @Override
  public void setParentTour(Tour parent){
    this.parentTour = parent;
  }

  /**
   * Set start time of the tour, i.e., departure time from the origin
   * @param startTime to set
   */
  @Override
  public void setStartTime(LocalTime startTime) {
    this.startTime = startTime;
  }

  /**
   * Set end time of tour, i.e., arrival time back at the origin
   * @param endTime to set
   */
  @Override
  public void setEndTime(LocalTime endTime) {
    this.endTime = endTime;
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
  @Override
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
   * {@inheritDoc}
   */
  @Override
  public void setSchedule(ActivitySchedule schedule) {
    this.schedule = schedule;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mode getOutboundMode() {
    if(!hasSchedule()){
      return null; // tour is expected to have a schedule
    }
    var firstEntry = getSchedule().getFirst();
    if(firstEntry.hasSchedule()){
      return firstEntry.getSchedule().getOutboundMode();
    }
    return firstEntry.getOutboundMode();
  }
  /**
   * {@inheritDoc}
   */
  @Override
  public Mode getInboundMode() {
    if(!hasSchedule()){
      return null;
    }
    var firstEntry = getSchedule().getFirst();
    if(firstEntry.hasSchedule()){
      return firstEntry.getSchedule().getInboundMode();
    }
    return firstEntry.getInboundMode();
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
  public TourImpl shallowClone() {
    return new TourImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TourImpl deepClone() {
    return new TourImpl(this, true);
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

package org.goplanit.demands.discrete.tour;

import org.goplanit.demands.discrete.person.Person;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactory;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.zoning.OdZone;

import java.time.LocalTime;
import java.util.logging.Logger;

/**
 * Factory class for tour instances to be registered on its parent container passed in to constructor
 */
public class ToursFactory extends ManagedIdEntityFactoryImpl<Tour>
    implements ManagedIdEntityFactory<Tour> {

  private static final Logger LOGGER = Logger.getLogger(ToursFactory.class.getCanonicalName());

  /** container to use */
  protected final Tours tours;

  /**
   * Create a newly created instance without registering on the container
   *
   * @return created time period
   */
  protected Tour createNew() {
    return new TourImpl(getIdGroupingToken());
  }

  /**
   * Constructor
   *
   * @param tokenId    to use
   * @param tours to use
   */
  protected ToursFactory(final IdGroupingToken tokenId, final Tours tours) {
    super(tokenId);
    this.tours = tours;
  }

  /**
   * register a new entry on the container and return it
   *
   * @return created instance
   */
  public Tour registerNew() {
    var newInstance = new TourImpl(getIdGroupingToken());
    tours.register(newInstance);
    return newInstance;
  }

  /**
   * register a new entry on the container and return it, with the given person as its primary participant
   *
   * @param person person making the tour
   * @param registerOnSchedule when true we register the participation on the person's schedule
   * @return created instance
   */
  public Tour registerNew(Person person, boolean registerOnSchedule) {
    return registerNew(person, TourParticipantRole.PRIMARY, registerOnSchedule);
  }

  /**
   * register a new entry on the container and return it, with the given person participating in the given role. No
   * ordering is implied: a tour may be created with a non-primary participant first and gain its primary
   * participant later
   *
   * @param person person participating in the tour
   * @param role the person takes on the tour
   * @param registerOnSchedule when true we register the participation on the person's schedule
   * @return created instance
   */
  public Tour registerNew(Person person, TourParticipantRole role, boolean registerOnSchedule) {
    var newInstance = new TourImpl(getIdGroupingToken());
    var participation = newInstance.addParticipant(person, role);
    if(registerOnSchedule){
      person.getSchedule().add(participation);
    }
    tours.register(newInstance);
    return newInstance;
  }

  /**
   * register a new entry on the container and return it
   *
   * @param person person making the tour
   * @param origin origin zone
   * @param destination destination zone
   * @param startTime start time
   * @param endTime end time
   * @param registerOnSchedule when true we register this tour on the person's schedule
   * @return created instance
   */
  public Tour registerNew(
      Person person,
      OdZone origin,
      OdZone destination,
      LocalTime startTime,
      LocalTime endTime,
      boolean registerOnSchedule) {
    var newInstance = registerNew(person, registerOnSchedule);
    newInstance.setOriginDestination(origin, destination);
    newInstance.setStartEndTime(startTime, endTime);
    return newInstance;
  }

  /**
   * register a new entry on the container and return it as a sub tour of the given parent. A sub tour is performed
   * by the person whose tour it sits within, so it adopts that person as its own primary participant
   *
   * @param parentTour parent tour of this tour, whose participant is adopted
   * @param registerOnSchedule when true we register the participation on the parent tour's schedule
   * @return created instance
   */
  public Tour registerNew(Tour parentTour, boolean registerOnSchedule) {
    var newInstance = new TourImpl(getIdGroupingToken());
    newInstance.setParentTour(parentTour);
    var currParent = parentTour;
    while(currParent.hasParentTour()){
      if(currParent.getPrimaryParticipant()!=null){
        newInstance.setParentTour(currParent);
      }
      currParent = currParent.getParentTour();
    }

    /* adopt the participant of the tour this one sits within. Fall back on any participant when no primary is
     * registered (yet), so no ordering of participant registration is assumed */
    var parentParticipation = newInstance.getParentTour().getPrimaryParticipation() != null
        ? newInstance.getParentTour().getPrimaryParticipation()
        : newInstance.getParentTour().getParticipantTours().stream().findFirst().orElse(null);
    PlanItRunTimeException.throwIfNull(parentParticipation,
        "Unable to create sub tour for parent tour (%s) that has no participants",
        newInstance.getParentTour().getIdsAsString());
    var participation = newInstance.addParticipant(parentParticipation.getPerson(), TourParticipantRole.PRIMARY);

    if(registerOnSchedule){
      if(!parentTour.hasSchedule()){
        parentTour.setSchedule(new ActivitySchedule());
      }
      parentTour.getSchedule().add(participation);
    }

    tours.register(newInstance);
    return newInstance;
  }

  /**
   * register a new entry on the container and return it
   *
   * @param parentTour parent tour this tour is embedded within
   * @param origin origin zone
   * @param destination destination zone
   * @param startTime start time
   * @param endTime end time
   * @param registerOnSchedule when true we register this tour on the person's schedule
   * @return created instance
   */
  public Tour registerNew(
      Tour parentTour,
      OdZone origin,
      OdZone destination,
      LocalTime startTime,
      LocalTime endTime,
      boolean registerOnSchedule) {
    var newInstance = registerNew(parentTour, registerOnSchedule);
    newInstance.setOriginDestination(origin, destination);
    newInstance.setStartEndTime(startTime, endTime);
    return newInstance;
  }

}

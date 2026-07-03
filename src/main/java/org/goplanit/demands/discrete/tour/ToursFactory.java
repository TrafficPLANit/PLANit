package org.goplanit.demands.discrete.tour;

import org.goplanit.demands.discrete.person.Person;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactory;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;
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
    return new Tour(getIdGroupingToken());
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
    var newInstance = new Tour(getIdGroupingToken());
    tours.register(newInstance);
    return newInstance;
  }

  /**
   * register a new entry on the container and return it
   *
   * @param person person making the tour
   * @param registerOnSchedule when true we register this tour on the person's schedule
   * @return created instance
   */
  public Tour registerNew(Person person, boolean registerOnSchedule) {
    var newInstance = new Tour(getIdGroupingToken());
    newInstance.setPerson(person);
    if(registerOnSchedule){
      person.getSchedule().add(newInstance);
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
   * register a new entry on the container and return it
   *
   * @param parentTour parent tour of this tour, will automatically deduce person from parent (if present)
   * @param registerOnSchedule when true we register this tour on the parent tour's schedule
   * @return created instance
   */
  public Tour registerNew(Tour parentTour, boolean registerOnSchedule) {
    var newInstance = new Tour(getIdGroupingToken());
    newInstance.setParentTour(parentTour);
    var currParent = parentTour;
    while(currParent.hasParentTour()){
      if(currParent.getPerson()!=null){
        newInstance.setParentTour(currParent);
      }
      currParent = currParent.getParentTour();
    }

    if(registerOnSchedule){
      if(!parentTour.hasSchedule()){
        parentTour.setSchedule(new ActivitySchedule());
      }
      parentTour.getSchedule().add(newInstance);
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

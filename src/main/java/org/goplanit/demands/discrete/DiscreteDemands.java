package org.goplanit.demands.discrete;

import org.goplanit.component.PlanitComponent;
import org.goplanit.demands.TimePeriods;
import org.goplanit.demands.discrete.household.Household;
import org.goplanit.demands.discrete.household.Households;
import org.goplanit.demands.discrete.person.Person;
import org.goplanit.demands.discrete.person.Persons;
import org.goplanit.demands.discrete.tour.ScheduleElement;
import org.goplanit.demands.discrete.tour.Tour;
import org.goplanit.demands.discrete.tour.TourImpl;
import org.goplanit.demands.discrete.tour.Tours;
import org.goplanit.demands.discrete.trip.Trip;
import org.goplanit.demands.discrete.trip.TripImpl;
import org.goplanit.demands.discrete.trip.Trips;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdDeepCopyMapper;
import org.goplanit.utils.time.TimePeriod;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Container class for all discrete demands (agents). For now this is only used for conversion support rather than
 * directly as part of the more macroscopic oriented assignment in PLANit, but the idea is it will also allow for
 * conversion from discrete to continuous or macroscopic demands that are used in PLANit already through the normal
 * demands setup
 * <p>
 * In future we can contemplate supporting this directly also for assignment
 * </p>
 *
 * @author markr
 *
 */
public class DiscreteDemands extends PlanitComponent<DiscreteDemands> implements Serializable {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(DiscreteDemands.class.getCanonicalName());

  /** create a schedule element mapping from tour and trip mappings
   *
   * @param tourToTourMapping to use
   * @param tripToTripMapping to use
   * @param removeMissingMappings setting
   * @return function mapping
   */
  private static Function<ScheduleElement, ScheduleElement> constructScheduleElementMapping(
      Function<Tour,Tour> tourToTourMapping,
      Function<Trip,Trip> tripToTripMapping,
      boolean removeMissingMappings){

    Function<ScheduleElement, ScheduleElement> scheduleToScheduleElementMapping = (s -> {
      ScheduleElement mapped = null;
      if(s instanceof TourImpl){
        mapped = tourToTourMapping.apply((Tour) s);
      }else if(s instanceof TripImpl){
        mapped = tripToTripMapping.apply((Trip) s);
      }
      if(removeMissingMappings){
        return mapped;
      }else{
        throw new PlanItRunTimeException("Unsupported element found %s", mapped);
      }
    });
    return scheduleToScheduleElementMapping;
  }

  /**
   * Update the household type of all persons based on the mapping provided (if any)
   * @param hhToHhMapping should contain original household as currently used on person and then
   *                      the value is the new household to replace it
   * @param removeMissingMappings when true if there is no mapping, the household is nullified, otherwise they are
   *                              left in-tact
   */
  private void updatePersonHouseholds( Function<Household,Household> hhToHhMapping, boolean removeMissingMappings) {
    for(var person : this.persons){
      var household = person.getHousehold();
      var newHousehold = hhToHhMapping.apply(household);
      if (newHousehold != null || removeMissingMappings) {
        person.setHousehold(newHousehold);
      }
    }
  }

  /**
   * Update the person of all tours based on the mapping provided (if any)
   * @param pToPMapping old to new person mapping
   * @param removeMissingMappings when true if there is no mapping, the household is nullified, otherwise they are
   *                              left in-tact
   */
  private void updateTourPersons(Function<Person,Person> pToPMapping, boolean removeMissingMappings) {
    for(var tour : this.tours){
      var person = tour.getPerson();
      var newPerson = pToPMapping.apply(person);
      if (newPerson != null || removeMissingMappings) {
        tour.setPerson(newPerson);
      }
    }
  }

  /**
   * Update the schedule of all persons based on the mapping provided (if any)
   * @param tourToTourMapping old to new tour mapping
   * @param tripToTripMapping old to new trip mapping
   * @param removeMissingMappings when true if there is no mapping, the schedule is cleared, otherwise they are
   *                              left in-tact
   */
  private void updatePersonSchedules(
      Function<Tour,Tour> tourToTourMapping,
      Function<Trip,Trip> tripToTripMapping,
      boolean removeMissingMappings) {
    for(var person : this.persons){
      var schedule = person.getSchedule();
      person.setSchedule(schedule.deepCloneWithMapping(
          constructScheduleElementMapping(tourToTourMapping, tripToTripMapping, removeMissingMappings)));
    }
  }

  /**
   * Update the parent tour of all tours based on the mapping provided (if any)
   * @param tourToTourMapping old to new tour mapping
   * @param removeMissingMappings when true if there is no mapping, the household is nullified, otherwise they are
   *                              left in-tact
   */
  private void updateTourParents(Function<Tour,Tour> tourToTourMapping, boolean removeMissingMappings) {
    for(var tour : this.tours){
      var parent = tour.getParentTour();
      var newParent = tourToTourMapping.apply(parent);
      if (newParent != null || removeMissingMappings) {
        tour.setParentTour(newParent);
      }
    }
  }

  /**
   * Update the tour schedule of all tours based on the mapping provided (if any)
   * @param tourToTourMapping old to new tour mapping
   * @param tripToTripMapping old to new trip mapping
   * @param removeMissingMappings when true if there is no mapping, the household is nullified, otherwise they are
   *                              left in-tact
   */
  private void updateTourSchedules(
      Function<Tour,Tour> tourToTourMapping,
      Function<Trip,Trip> tripToTripMapping,
      boolean removeMissingMappings) {

    for(var tour : this.tours){
      // this is the flat list of all tours, so we do not need to traverse the hierarchy of schedules, instead we can
      // map and clone each tours' schedule individually
      if(tour.hasSchedule()){
        var schedule = tour.getSchedule();
        tour.setSchedule(schedule.deepCloneWithMapping(
            constructScheduleElementMapping(tourToTourMapping, tripToTripMapping, removeMissingMappings)));
      }
    }
  }


  /**
   * Update the tour of all trips based on the mapping provided (if any)
   * @param tourToTourMapping old to new person mapping
   * @param removeMissingMappings when true if there is no mapping, the household is nullified, otherwise they are
   *                              left in-tact
   */
  private void updateTripTours(Function<Tour,Tour> tourToTourMapping, boolean removeMissingMappings) {
    for(var trip : this.trips){
      var tour = trip.getTour();
      var newTour = tourToTourMapping.apply(tour);
      if (newTour != null || removeMissingMappings) {
        trip.setTour(newTour);
      }
    }
  }

  // Protected

  /** time periods */
  protected final TimePeriods timePeriods;

  /**  the households */
  protected final Households households;

  /**  the persons */
  protected final Persons persons;

  /**  the tours */
  protected final Tours tours;

  /**  the trips */
  protected final Trips trips;

  /** modifier */
  protected DiscreteDemandsModifier discreteDemandsModifier;

  /**
   * Constructor
   *
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public DiscreteDemands(IdGroupingToken groupId) {
    super(groupId, DiscreteDemands.class);
    this.timePeriods = new TimePeriods(groupId);
    this.households = new Households(groupId);
    this.persons = new Persons(groupId);
    this.tours = new Tours(groupId);
    this.trips = new Trips(groupId);

    this.discreteDemandsModifier = new DiscreteDemandsModifier(this);
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public DiscreteDemands(DiscreteDemands other, boolean deepCopy) {
    super(other, deepCopy);
    this.discreteDemandsModifier = new DiscreteDemandsModifier(this);

    if(deepCopy) {
      var timePeriodMapper = new ManagedIdDeepCopyMapper<TimePeriod>();
      var householdMapper = new ManagedIdDeepCopyMapper<Household>();
      var personMapper = new ManagedIdDeepCopyMapper<Person>();
      var tourMapper = new ManagedIdDeepCopyMapper<Tour>();
      var tripMapper = new ManagedIdDeepCopyMapper<Trip>();

      this.timePeriods = other.timePeriods.deepCloneWithMapping(timePeriodMapper);
      this.households = other.households.deepCloneWithMapping(householdMapper);
      this.persons = other.persons.deepCloneWithMapping(personMapper);
      this.tours = other.tours.deepCloneWithMapping(tourMapper);
      this.trips = other.trips.deepCloneWithMapping(tripMapper);

      /* DISCRETE DEMANDS */
      // note zones of households/tours are not remapped because zoning is not part of discrete demands
      // if zoning is also deep cloned --> manual update is required for those references
      //todo: move to container clones above where possible as it is cleaner!!
      updatePersonHouseholds(householdMapper::getMapping, true);
      updateTourPersons(personMapper::getMapping, true);
      updatePersonSchedules(tourMapper::getMapping, tripMapper::getMapping, true);
      updateTourParents(tourMapper::getMapping, true);
      updateTourSchedules(tourMapper::getMapping, tripMapper::getMapping, true);
      updateTripTours(tourMapper::getMapping, true);

    }else{
      this.timePeriods    = other.timePeriods.shallowClone();
      this.households    = other.households.shallowClone();
      this.persons    = other.persons.shallowClone();
      this.tours    = other.tours.shallowClone();
      this.trips    = other.trips.shallowClone();

    }

  }


  /**
   * Access to the time periods
   * @return time periods container
   */
  public TimePeriods getTimePeriods(){
    return timePeriods;
  }

  /**
   * Access to the households
   * @return households container
   */
  public Households getHouseholds(){
    return households;
  }

  /**
   * Access to the persons
   * @return container
   */
  public Persons getPersons(){
    return persons;
  }

  /**
   * Access to the tours
   * @return container
   */
  public Tours getTours(){
    return tours;
  }

  /**
   * Access to the trips
   * @return container
   */
  public Trips getTrips(){
    return trips;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DiscreteDemands shallowClone() {
    return new DiscreteDemands(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DiscreteDemands deepClone() {
    return new DiscreteDemands(this, true);
  }

  /**
   * Log general information on this demands to the user
   *
   * @param prefix to use
   */
  public void logInfo(String prefix) {
    LOGGER.info(String.format("%s#time periods: %d", prefix, timePeriods.size()));
    LOGGER.info(String.format("%s#households: %d", prefix, households.size()));
    LOGGER.info(String.format("%s#persons: %d", prefix, persons.size()));
    LOGGER.info(String.format("%s#tours: %d", prefix, tours.size()));
    LOGGER.info(String.format("%s#trips: %d", prefix, trips.size()));
  }

  /**
   * reset all demands contents
   */
  public void reset() {
    timePeriods.clear();
    households.clear();
    persons.clear();
    tours.clear();
    trips.clear();
  }

  /*
  * {@inheritDoc}
  */
  @Override
  public Map<String, String> collectSettingsAsKeyValueMap() {
    //todo
    return null;
  }

  /**
   * access to modifier features
   *
   * @return discreteDemandsModifier
   */
  public DiscreteDemandsModifier getDiscreteDemandsModifier(){
    return discreteDemandsModifier;
  }
}

package org.goplanit.algorithms.sampling.demands.table;

import org.goplanit.demands.discrete.household.Household;
import org.goplanit.demands.discrete.person.Person;
import org.goplanit.demands.discrete.tour.ParticipantTour;
import org.goplanit.demands.discrete.tour.Tour;
import org.goplanit.demands.discrete.trip.Trip;
import org.goplanit.utils.zoning.OdZone;

import java.util.ArrayList;
import java.util.List;

/**
 * One sampling unit of a discrete demand, while the demand is counted into a {@link DiscreteDemandsDownSamplingTable}:
 * what a down-sampling keeps or drops whole, with everything the down-sampling table counts for it
 * <p>
 * A sampling unit is a household with its persons, tours and trips; households whose persons share a tour are one
 * sampling unit together, and a person without a household is one of their own. For example, households h3 and h4,
 * whose persons share a visit, are one sampling unit of two households, their persons, the visit and its trips. A
 * demand keeps households, persons, tours and trips in containers of their own, and a household does not know its
 * persons, so one sampling unit is gathered here, in one place, for the down-sampling table to count. All terms are
 * listed in {@link org.goplanit.algorithms.sampling.demands}.
 * </p>
 *
 * @author markr
 */
final class DownSamplingUnit {

  /** its households, in the demand's order, most units having one */
  private final List<Household> households = new ArrayList<>(1);

  /** the persons of each of its households, in the order of the households */
  private final List<List<Person>> personsOfHouseholds = new ArrayList<>(1);

  /** its persons without a household, in the demand's order, most units having none */
  private final List<Person> personsWithoutHousehold = new ArrayList<>(0);

  /** the tours whose first participant is one of its persons, in the demand's order */
  private final List<Tour> tours = new ArrayList<>();

  /** the trips on its tours, in the demand's order */
  private final List<Trip> trips = new ArrayList<>();

  /**
   * Add a household with its persons, after those added before
   *
   * @param household to add
   * @param persons of the household
   */
  public void addHousehold(Household household, List<Person> persons) {
    households.add(household);
    personsOfHouseholds.add(persons);
  }

  /**
   * Add a person without a household, after those added before
   *
   * @param person to add
   */
  public void addPersonWithoutHousehold(Person person) {
    personsWithoutHousehold.add(person);
  }

  /**
   * Add a tour one of its persons takes part in first
   *
   * @param tour to add
   */
  public void addTour(Tour tour) {
    tours.add(tour);
  }

  /**
   * Add a trip on one of its tours
   *
   * @param trip to add
   */
  public void addTrip(Trip trip) {
    trips.add(trip);
  }

  /**
   * Its households
   *
   * @return the households, in the demand's order
   */
  public List<Household> getHouseholds() {
    return households;
  }

  /**
   * The persons of one of its households
   *
   * @param household of the sampling unit
   * @return the persons
   */
  public List<Person> getPersonsOf(Household household) {
    return personsOfHouseholds.get(households.indexOf(household));
  }

  /**
   * Its persons without a household
   *
   * @return the persons, in the demand's order
   */
  public List<Person> getPersonsWithoutHousehold() {
    return personsWithoutHousehold;
  }

  /**
   * The tours whose first participant is one of its persons
   *
   * @return the tours, in the demand's order
   */
  public List<Tour> getTours() {
    return tours;
  }

  /**
   * The trips on its tours
   *
   * @return the trips, in the demand's order
   */
  public List<Trip> getTrips() {
    return trips;
  }

  /**
   * Its zone: that of its first household, or for a unit of persons without a household the origin of the first tour
   * on its first person's schedule
   *
   * @return the zone, null when it has none
   */
  public OdZone findZone() {
    if (!households.isEmpty()) {
      return households.get(0).getZone();
    }
    var firstTour = personsWithoutHousehold.get(0).getSchedule().getFirstOfType(ParticipantTour.class);
    return firstTour == null ? null : firstTour.getTour().getOrigin();
  }
}

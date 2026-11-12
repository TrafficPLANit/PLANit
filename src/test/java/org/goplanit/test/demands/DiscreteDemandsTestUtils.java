package org.goplanit.test.demands;

import org.goplanit.demands.discrete.DiscreteDemands;
import org.goplanit.demands.discrete.household.Household;
import org.goplanit.demands.discrete.person.EmploymentStatus;
import org.goplanit.demands.discrete.person.Person;
import org.goplanit.demands.discrete.person.Sex;
import org.goplanit.demands.discrete.person.StudyStatus;
import org.goplanit.demands.discrete.tour.ParticipantTour;
import org.goplanit.demands.discrete.tour.Tour;
import org.goplanit.demands.discrete.tour.TourParticipantRole;
import org.goplanit.demands.discrete.trip.Trip;
import org.goplanit.demands.discrete.util.DirectionBound;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.zoning.OdZone;

import java.time.LocalTime;

/**
 * Builds discrete demands in tests
 */
public final class DiscreteDemandsTestUtils {

  /**
   * Register a household
   *
   * @param demands to register it on
   * @param xmlId of the household, null for none
   * @param zone it lives in
   * @param cars it has, null when not known
   * @param yearlyIncome it has, null when not known
   * @return the household
   */
  public static Household registerHousehold(
      DiscreteDemands demands, String xmlId, OdZone zone, Integer cars, Double yearlyIncome) {
    var household = demands.getHouseholds().getFactory().registerNew();
    household.setXmlId(xmlId);
    household.setZone(zone);
    household.setNumberOfCars(cars);
    household.setYearlyIncome(yearlyIncome);
    return household;
  }

  /**
   * Register a person
   *
   * @param demands to register them on
   * @param household they live in, null for none
   * @param xmlId of the person
   * @param age in years, null when not known
   * @param employment when known, else null
   * @param study when known, else null
   * @param carLicence when known, else null
   * @param sex when known, else null
   * @return the person
   */
  public static Person registerPerson(DiscreteDemands demands, Household household, String xmlId, Integer age,
      EmploymentStatus employment, StudyStatus study, Boolean carLicence, Sex sex) {
    var person = demands.getPersons().getFactory().registerNew(household);
    person.setXmlId(xmlId);
    person.setAge(age);
    person.setEmploymentStatus(employment);
    person.setStudyStatus(study);
    person.setCarLicence(carLicence);
    person.setSex(sex);
    return person;
  }

  /**
   * Register a tour of a person from 06:00 to 22:00, placed on their schedule
   *
   * @param demands to register it on
   * @param person making it
   * @param origin of the tour
   * @param destination of the tour
   * @param purpose of the tour
   * @return the tour
   */
  public static Tour registerTour(
      DiscreteDemands demands, Person person, OdZone origin, OdZone destination, String purpose) {
    var tour = demands.getTours().getFactory().registerNew(
        person, origin, destination, LocalTime.of(6, 0), LocalTime.of(22, 0), true);
    tour.setPurpose(purpose);
    return tour;
  }

  /**
   * Register a trip on a tour's schedule, between the tour's zones: from its origin outbound, back to it inbound
   *
   * @param demands to register it on
   * @param tour it is on
   * @param direction of the trip
   * @param mode of the trip
   * @param departure time of the trip
   * @return the trip
   */
  public static Trip registerTrip(
      DiscreteDemands demands, Tour tour, DirectionBound direction, Mode mode, LocalTime departure) {
    var trip = demands.getTrips().getFactory().registerNew(tour, direction, true);
    trip.setMode(mode);
    trip.setStartTime(departure);
    return trip;
  }

  /**
   * Add an accompanying participant to an existing tour, without knowing whether they drive, and place that
   * participation on their schedule
   *
   * @param tour to add the participant to
   * @param person to add
   * @return the participation
   */
  public static ParticipantTour addAccompanyingParticipant(Tour tour, Person person) {
    return addAccompanyingParticipant(tour, person, null);
  }

  /**
   * Add an accompanying participant to an existing tour and place that participation on their schedule
   *
   * @param tour to add the participant to
   * @param person to add
   * @param driver whether the person drives the tour's trips by a mode that needs a driver, null when not known
   * @return the participation
   */
  public static ParticipantTour addAccompanyingParticipant(Tour tour, Person person, Boolean driver) {
    var participation = tour.addParticipant(person, TourParticipantRole.ACCOMPANYING, driver);
    person.getSchedule().add(participation);
    return participation;
  }
}

package org.goplanit.test.algorithms.sampling.utils;

import org.goplanit.demands.discrete.DiscreteDemands;
import org.goplanit.demands.discrete.person.EmploymentStatus;
import org.goplanit.demands.discrete.person.Person;
import org.goplanit.demands.discrete.person.Sex;
import org.goplanit.demands.discrete.person.StudyStatus;
import org.goplanit.demands.discrete.util.DirectionBound;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.HashUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.zoning.Zoning;
import org.goplanit.zoning.zonetozone.OdSkimMatrix;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.TreeSet;

import static org.goplanit.test.demands.DiscreteDemandsTestUtils.addAccompanyingParticipant;
import static org.goplanit.test.demands.DiscreteDemandsTestUtils.registerHousehold;
import static org.goplanit.test.demands.DiscreteDemandsTestUtils.registerPerson;
import static org.goplanit.test.demands.DiscreteDemandsTestUtils.registerTour;
import static org.goplanit.test.demands.DiscreteDemandsTestUtils.registerTourThereAndBack;
import static org.goplanit.test.demands.DiscreteDemandsTestUtils.registerTrip;

/**
 * Synthetic demands for the down-sampling tests, of up to 100,000 households, with their zoning, modes and distances
 * <p>
 * The zones are a grid of 20 by 20, 1 km apart, taken in their order along the Hilbert curve as the down-sampling
 * table takes them. Along that order the first zone holds 37 households, the next ten 4 each, and the next three 13, 7
 * and 4, the scenarios of the households per zone; every other household lives in one of the other zones, drawn at
 * random. A household has one to four persons, the first an adult, and each adult makes up to two tours, two in five
 * by car where the household has one, a sixth by bus and the rest on foot, a quarter of the car tours with a second
 * person of the household riding along, so about 30% of person trips are driven by car. Tours go to a zone drawn at
 * random and leave at hours spread over the day with peaks in the morning and the afternoon. A trip's distance is the
 * straight line between the zones, 0.5 km within one. Each household is drawn from its own seed, so household 7 is the
 * same in whatever demand it is added to.
 * </p>
 * <p>
 * A demand with mixed sampling units also has every hundredth household from 301 on taking the household before along
 * on a visit by car, joining the two, and every five hundredth from 250 on a person without a household driving to
 * work.
 * </p>
 *
 * @author markr
 */
public final class DownSamplingTestDemands {

  /** seed every household is drawn from, with its index */
  private static final long DEMAND_SEED = 2026;

  /** zones along each side of the grid */
  private static final int GRID_SIDE = 20;

  /** distance of a trip within a zone */
  private static final double INTRAZONAL_KM = 0.5;

  /** households of the first zones along the curve: one of 37, ten of 4, and three of 13, 7 and 4 */
  private static final int[] SCENARIO_ZONE_SIZES = {37, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 13, 7, 4};

  /** share of tours by car, in a household with a car */
  private static final double CAR_SHARE = 0.4;

  /** share of tours by bus */
  private static final double BUS_SHARE = 0.17;

  /** share of car tours with a second person of the household riding along */
  private static final double JOINT_SHARE = 0.25;

  /** with mixed sampling units, every this many households a person without a household is added */
  private static final int PERSON_ALONE_EVERY = 500;

  /**
   * A departure time: a third or so in the morning peak from 07:00, a quarter in the afternoon peak from 15:00, the
   * rest at any hour from 06:00 to 21:00
   *
   * @param random to draw from
   * @return the time
   */
  private static LocalTime drawDeparture(SplittableRandom random) {
    double draw = random.nextDouble();
    int hour = draw < 0.35 ? 7 + random.nextInt(3) : draw < 0.6 ? 15 + random.nextInt(4) : 6 + random.nextInt(16);
    return LocalTime.of(hour, random.nextInt(60));
  }

  /** id token of the demands */
  private final IdGroupingToken token;

  /** the modes */
  private final Mode car;

  /** the modes */
  private final Mode bus;

  /** the modes */
  private final Mode walk;

  /** zoning of the grid of zones */
  private final Zoning zoning;

  /** the zones in their order along the Hilbert curve */
  private final List<OdZone> curveOrder;

  /** the distance skims, one complete straight line skim shared by every mode */
  private final Map<Mode, OdSkimMatrix> distances;

  /**
   * The zone household of the given index lives in: the scenario zones first, along the curve, then one drawn at random
   * from the others
   *
   * @param index of the household
   * @param random of the household
   * @return the zone
   */
  private OdZone findZoneOfHousehold(int index, SplittableRandom random) {
    int first = 0;
    for (int position = 0; position < SCENARIO_ZONE_SIZES.length; ++position) {
      first += SCENARIO_ZONE_SIZES[position];
      if (index < first) {
        return curveOrder.get(position);
      }
    }
    int otherZones = curveOrder.size() - SCENARIO_ZONE_SIZES.length;
    return curveOrder.get(SCENARIO_ZONE_SIZES.length + random.nextInt(otherZones));
  }

  /**
   * Register a tour of a person to a zone drawn at random, with a trip there and one back
   *
   * @param demands to register it on
   * @param persons of the person's household, the person among them
   * @param person making the tour
   * @param random of the household
   */
  private void registerRandomTour(DiscreteDemands demands, List<Person> persons, Person person,
      SplittableRandom random) {
    var household = person.getHousehold();
    double modeDraw = random.nextDouble();
    var mode = household.getNumberOfCars() > 0 && modeDraw < CAR_SHARE ?
        car : modeDraw < CAR_SHARE + BUS_SHARE ? bus : walk;
    var destination = curveOrder.get(random.nextInt(curveOrder.size()));
    var purpose = PURPOSES.get(random.nextInt(PURPOSES.size()));
    var tour = registerTour(demands, person, household.getZone(), destination, purpose);
    if (mode == car && persons.size() > 1 && random.nextDouble() < JOINT_SHARE) {
      addAccompanyingParticipant(tour, persons.get(persons.get(0) == person ? 1 : 0));
    }
    var outbound = drawDeparture(random);
    var inbound = outbound.plusHours(1 + random.nextInt(8));
    registerTrip(demands, tour, DirectionBound.OUTBOUND, mode, outbound);
    registerTrip(
        demands, tour, DirectionBound.INBOUND, mode, inbound.isBefore(outbound) ? LocalTime.of(23, 30) : inbound);
  }

  /** purposes of the tours */
  public static final List<String> PURPOSES = List.of("work", "school", "shopping", "leisure");

  /** with mixed sampling units, every this many households one takes the household before along on a visit */
  public static final int SHARED_TOUR_EVERY = 100;

  /**
   * The XML ids of the households and the persons without a household a demand holds
   *
   * @param demands to collect from
   * @return the XML ids, sorted
   */
  public static Set<String> collectXmlIdsOfSamplingUnitMembers(DiscreteDemands demands) {
    var xmlIds = new TreeSet<String>();
    demands.getHouseholds().forEach(household -> xmlIds.add(household.getXmlId()));
    demands.getPersons().stream().filter(person -> person.getHousehold() == null)
        .forEach(person -> xmlIds.add(person.getXmlId()));
    return xmlIds;
  }

  /**
   * The grid of zones in their order along the curve, the modes and the lookup
   *
   * @param token to create the network, zoning and demands with
   */
  public DownSamplingTestDemands(IdGroupingToken token) {
    this.token = token;
    var grid = new DownSamplingTestGrid(token, GRID_SIDE, 0, (x, y) -> x + "_" + y,
        (origin, destination) -> origin == destination ? INTRAZONAL_KM :
            origin.getCentroid().getPosition().distance(destination.getCentroid().getPosition()) / 1000);
    car = grid.getMode(PredefinedModeType.CAR);
    bus = grid.getMode(PredefinedModeType.BUS);
    walk = grid.getMode(PredefinedModeType.PEDESTRIAN);
    zoning = grid.getZoning();
    curveOrder = grid.getCurveOrder();
    distances = grid.getDistances();
  }

  /**
   * A demand of the given number of households, each drawn from its own seed
   *
   * @param households in the demand
   * @param mixedUnits true to add households sharing a tour and persons without a household
   * @return the demand
   */
  public DiscreteDemands createDemand(int households, boolean mixedUnits) {
    var demands = new DiscreteDemands(token);
    Person firstPersonOfHouseholdBefore = null;
    for (int index = 0; index < households; ++index) {
      var random = new SplittableRandom(HashUtils.createCombinedSeed(DEMAND_SEED, index));
      var zone = findZoneOfHousehold(index, random);
      int cars = random.nextDouble() < 0.2 ? 0 : random.nextDouble() < 0.6 ? 1 : 2;
      var household = registerHousehold(demands, "h" + index, zone, cars, 20000.0 + random.nextInt(180000));
      int size = 1 + random.nextInt(4);
      var persons = new ArrayList<Person>(size);
      for (int member = 0; member < size; ++member) {
        int age = member == 0 ? 25 + random.nextInt(40) : random.nextInt(85);
        persons.add(registerPerson(demands, household, "h" + index + "p" + member, age,
            EmploymentStatus.values()[random.nextInt(EmploymentStatus.values().length)],
            StudyStatus.values()[random.nextInt(StudyStatus.values().length)], age >= 17 && random.nextDouble() < 0.8,
            random.nextBoolean() ? Sex.FEMALE : Sex.MALE));
      }
      for (var person : persons) {
        int tours = person.getAge() >= 18 ? random.nextInt(3) : 0;
        for (int tour = 0; tour < tours; ++tour) {
          registerRandomTour(demands, persons, person, random);
        }
      }

      if (mixedUnits && index > 2 * SHARED_TOUR_EVERY && index % SHARED_TOUR_EVERY == 1
          && firstPersonOfHouseholdBefore != null) {
        registerTourThereAndBack(demands, List.of(persons.get(0), firstPersonOfHouseholdBefore), zone,
            firstPersonOfHouseholdBefore.getHousehold().getZone(), "visit", car, LocalTime.of(11, 0),
            LocalTime.of(14, 0));
      }
      if (mixedUnits && index % PERSON_ALONE_EVERY == PERSON_ALONE_EVERY / 2) {
        var alone = registerPerson(demands, null, "p" + index, 30, EmploymentStatus.FULL_TIME,
            StudyStatus.NOT_STUDYING, true, Sex.FEMALE);
        registerTourThereAndBack(demands, List.of(alone), zone, curveOrder.get(random.nextInt(curveOrder.size())),
            "work", car, LocalTime.of(7, 30), LocalTime.of(17, 0));
      }
      firstPersonOfHouseholdBefore = persons.get(0);
    }
    return demands;
  }

  /**
   * The zone position along the curve of each household
   *
   * @param demands holding the households
   * @return the position by XML id
   */
  public Map<String, Integer> collectPositionByHousehold(DiscreteDemands demands) {
    var positionOfZone = new IdentityHashMap<OdZone, Integer>();
    for (int position = 0; position < curveOrder.size(); ++position) {
      positionOfZone.put(curveOrder.get(position), position);
    }
    var positions = new HashMap<String, Integer>();
    demands.getHouseholds().forEach(household -> positions.put(household.getXmlId(),
        positionOfZone.get(household.getZone())));
    return positions;
  }

  /**
   * The car
   *
   * @return the mode
   */
  public Mode getCar() {
    return car;
  }

  /**
   * The zoning of the grid
   *
   * @return the zoning
   */
  public Zoning getZoning() {
    return zoning;
  }

  /**
   * The zones in their order along the Hilbert curve
   *
   * @return the zones
   */
  public List<OdZone> getCurveOrder() {
    return curveOrder;
  }

  /**
   * The distance skims, one complete straight line skim shared by every mode
   *
   * @return the skims by mode
   */
  public Map<Mode, OdSkimMatrix> getDistances() {
    return distances;
  }
}

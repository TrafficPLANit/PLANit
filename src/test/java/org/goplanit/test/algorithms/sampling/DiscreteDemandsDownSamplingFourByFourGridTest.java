package org.goplanit.test.algorithms.sampling;

import org.goplanit.algorithms.sampling.demands.DiscreteDemandsDownSamplingAlgorithmFactory;
import org.goplanit.algorithms.sampling.demands.DiscreteDemandsDownSamplingSettings;
import org.goplanit.algorithms.sampling.demands.enums.DiscreteDemandsDownSamplingAlgorithmType;
import org.goplanit.demands.discrete.DiscreteDemands;
import org.goplanit.demands.discrete.household.Household;
import org.goplanit.demands.discrete.person.EmploymentStatus;
import org.goplanit.demands.discrete.person.Person;
import org.goplanit.demands.discrete.person.Sex;
import org.goplanit.logging.Logging;
import org.goplanit.test.algorithms.sampling.utils.DownSamplingTestGrid;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.zoning.OdZone;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.goplanit.algorithms.sampling.demands.enums.DownSamplingBalanceGroup.CAR_DRIVEN_TRIPS_BY_DEPARTURE_PERIOD;
import static org.goplanit.algorithms.sampling.demands.enums.DownSamplingBalanceGroup.PERSONS_PER_ZONE;
import static org.goplanit.algorithms.sampling.demands.enums.DownSamplingBalanceGroup.PERSON_KILOMETRES_BY_MODE_GROUP;
import static org.goplanit.algorithms.sampling.demands.enums.DownSamplingBalanceGroup.PERSON_TRIPS_BY_MODE;
import static org.goplanit.demands.discrete.person.EmploymentStatus.FULL_TIME;
import static org.goplanit.demands.discrete.person.EmploymentStatus.NOT_EMPLOYED;
import static org.goplanit.demands.discrete.person.EmploymentStatus.PART_TIME;
import static org.goplanit.demands.discrete.person.Sex.FEMALE;
import static org.goplanit.demands.discrete.person.Sex.MALE;
import static org.goplanit.test.algorithms.sampling.utils.DownSamplingTestDemands.collectXmlIdsOfSamplingUnitMembers;
import static org.goplanit.test.demands.DiscreteDemandsTestUtils.createDemandsWithDayStart;
import static org.goplanit.test.demands.DiscreteDemandsTestUtils.registerHousehold;
import static org.goplanit.test.demands.DiscreteDemandsTestUtils.registerPerson;
import static org.goplanit.test.demands.DiscreteDemandsTestUtils.registerTourThereAndBack;
import static org.goplanit.utils.mode.PredefinedModeType.BUS;
import static org.goplanit.utils.mode.PredefinedModeType.CAR;
import static org.goplanit.utils.mode.PredefinedModeType.PEDESTRIAN;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Down-sampling a quarter of a small demand at the balanced quality, small enough to follow every step in a debugger:
 * its sampling units, the zones' order along the Hilbert curve and the sampling units kept at seed 42.
 * <p>
 * Sixteen zones of 1 km, A top left to P bottom right, each placed at its centre. On the left the zones, on the right
 * the number of households living in each:
 * </p>
 * <pre>
 *          zones          households
 *   y=3   A  B  C  D      1  2  2  1
 *   y=2   E  F  G  H      3  5  4  3
 *   y=1   I  J  K  L      2  4  5  2
 *   y=0   M  N  O  P      2  3  3  2
 *  x =    0  1  2  3      0  1  2  3
 * </pre>
 * <p>
 * The 44 households, h1 to h44, are numbered zone by zone in the order A to P and hold persons p1 to p94; p95 and p96
 * have no household. There are 22 tours of a trip there and a trip back, a trip's distance being the grid distance
 * between zone centres, 0.52 km within a zone. Commutes leave at 07:30 or 08:15 and come back at 17:30, shopping
 * leaves at 10:30 and comes back at 12:30. Four tours go by bus, two on foot and sixteen by car, the first participant
 * driving. Three car tours have two persons: h9 and h32 each go shopping with both their persons, and p50 of h25 in J
 * takes p61 of h29 in K to P, out at 19:30 and back at 01:30, the next morning, which makes h25 and h29 one sampling
 * unit. p95 drives to work from G to P at 07:45 and back at 17:15; p96 has no tours and so no zone. The day starts at
 * 03:00.
 * </p>
 * <p>
 * The draw balances persons per zone, person trips by mode, person kilometres by mode group and car driven trips by
 * departure period, with 4 destination areas.
 * </p>
 *
 * @author markr
 */
public class DiscreteDemandsDownSamplingFourByFourGridTest {

  /** departure of the early commutes */
  private static final LocalTime COMMUTE = LocalTime.of(7, 30);

  /** departure of the late commutes */
  private static final LocalTime LATE_COMMUTE = LocalTime.of(8, 15);

  /** return of every commute */
  private static final LocalTime COMMUTE_BACK = LocalTime.of(17, 30);

  /** departure of the shopping tours */
  private static final LocalTime SHOPPING = LocalTime.of(10, 30);

  /** return of the shopping tours */
  private static final LocalTime SHOPPING_BACK = LocalTime.of(12, 30);

  /** the logger, giving the log output PLANit's format */
  private static Logger LOGGER = null;

  /**
   * Register the next household, h1 onwards in the order registered
   *
   * @param demands to register it on
   * @param zone it lives in
   * @param cars number of cars
   * @param incomeThousands yearly income in thousands
   * @return the household
   */
  private static Household household(DiscreteDemands demands, OdZone zone, int cars, int incomeThousands) {
    return registerHousehold(
        demands, "h" + (demands.getHouseholds().size() + 1), zone, cars, incomeThousands * 1000.0);
  }

  /**
   * Register the next person of a household, p1 onwards in the order registered, whose study status and car licence
   * are not known
   *
   * @param demands to register them on
   * @param household they live in
   * @param age in years
   * @param sex of the person
   * @param employment status of the person
   * @return the person
   */
  private static Person person(
      DiscreteDemands demands, Household household, int age, Sex sex, EmploymentStatus employment) {
    return registerPerson(
        demands, household, "p" + (demands.getPersons().size() + 1), age, employment, null, null, sex);
  }

  /**
   * Register a tour from the zone of its first participant's household, with a trip there and a trip back
   *
   * @param demands to register it on
   * @param purpose of the tour
   * @param destination of the tour
   * @param mode of both trips
   * @param departure of the trip there
   * @param returnDeparture of the trip back
   * @param participants making it, the first driving
   */
  private static void tour(DiscreteDemands demands, String purpose, OdZone destination, Mode mode,
      LocalTime departure, LocalTime returnDeparture, Person... participants) {
    registerTourThereAndBack(demands, List.of(participants), participants[0].getHousehold().getZone(), destination,
        purpose, mode, departure, returnDeparture);
  }

  /** id token of the test */
  private final IdGroupingToken token = IdGroupingToken.collectGlobalToken();

  /** the grid of zones A to P, with the modes and distances */
  private DownSamplingTestGrid grid;

  /** the zones by name */
  private Map<String, OdZone> zoneByName;

  /** the settings of the draw */
  private DiscreteDemandsDownSamplingSettings settings;

  /**
   * The demand: each household with its persons and then its tours, zone by zone, then the shared visit, then p95 and
   * p96
   *
   * @return the demand
   */
  private DiscreteDemands createDemand() {
    var demands = createDemandsWithDayStart(token, LocalTime.of(3, 0));
    var car = grid.getMode(CAR);
    var bus = grid.getMode(BUS);
    var walk = grid.getMode(PEDESTRIAN);

    var h1 = household(demands, zoneByName.get("A"), 0, 38);
    person(demands, h1, 72, FEMALE, NOT_EMPLOYED);

    var h2 = household(demands, zoneByName.get("B"), 1, 120);
    var p2 = person(demands, h2, 35, FEMALE, FULL_TIME);
    person(demands, h2, 33, MALE, PART_TIME);
    tour(demands, "work", zoneByName.get("G"), car, COMMUTE, COMMUTE_BACK, p2);
    var h3 = household(demands, zoneByName.get("B"), 0, 64);
    var p4 = person(demands, h3, 28, MALE, FULL_TIME);
    tour(demands, "work", zoneByName.get("K"), bus, LATE_COMMUTE, COMMUTE_BACK, p4);

    var h4 = household(demands, zoneByName.get("C"), 2, 185);
    var p5 = person(demands, h4, 41, FEMALE, FULL_TIME);
    person(demands, h4, 43, MALE, FULL_TIME);
    person(demands, h4, 12, FEMALE, NOT_EMPLOYED);
    person(demands, h4, 9, MALE, NOT_EMPLOYED);
    tour(demands, "work", zoneByName.get("G"), car, LATE_COMMUTE, COMMUTE_BACK, p5);
    var h5 = household(demands, zoneByName.get("C"), 1, 52);
    var p9 = person(demands, h5, 67, MALE, NOT_EMPLOYED);
    tour(demands, "shopping", zoneByName.get("D"), car, SHOPPING, SHOPPING_BACK, p9);

    var h6 = household(demands, zoneByName.get("D"), 2, 210);
    person(demands, h6, 58, MALE, FULL_TIME);
    person(demands, h6, 55, FEMALE, NOT_EMPLOYED);

    var h7 = household(demands, zoneByName.get("E"), 0, 41);
    var p12 = person(demands, h7, 24, FEMALE, FULL_TIME);
    tour(demands, "work", zoneByName.get("J"), bus, LATE_COMMUTE, COMMUTE_BACK, p12);
    var h8 = household(demands, zoneByName.get("E"), 1, 98);
    person(demands, h8, 36, MALE, FULL_TIME);
    person(demands, h8, 34, FEMALE, PART_TIME);
    person(demands, h8, 3, MALE, NOT_EMPLOYED);
    var h9 = household(demands, zoneByName.get("E"), 1, 45);
    var p16 = person(demands, h9, 70, MALE, NOT_EMPLOYED);
    var p17 = person(demands, h9, 68, FEMALE, NOT_EMPLOYED);
    tour(demands, "shopping", zoneByName.get("F"), car, SHOPPING, SHOPPING_BACK, p16, p17);

    var h10 = household(demands, zoneByName.get("F"), 0, 72);
    person(demands, h10, 31, FEMALE, FULL_TIME);
    var h11 = household(demands, zoneByName.get("F"), 2, 160);
    var p19 = person(demands, h11, 45, MALE, FULL_TIME);
    person(demands, h11, 44, FEMALE, PART_TIME);
    person(demands, h11, 16, FEMALE, NOT_EMPLOYED);
    person(demands, h11, 14, MALE, NOT_EMPLOYED);
    person(demands, h11, 7, FEMALE, NOT_EMPLOYED);
    tour(demands, "work", zoneByName.get("K"), car, COMMUTE, COMMUTE_BACK, p19);
    var h12 = household(demands, zoneByName.get("F"), 1, 140);
    var p24 = person(demands, h12, 29, FEMALE, FULL_TIME);
    person(demands, h12, 30, MALE, FULL_TIME);
    tour(demands, "work", zoneByName.get("O"), car, COMMUTE, COMMUTE_BACK, p24);
    var h13 = household(demands, zoneByName.get("F"), 0, 30);
    person(demands, h13, 52, MALE, NOT_EMPLOYED);
    var h14 = household(demands, zoneByName.get("F"), 1, 88);
    person(demands, h14, 61, FEMALE, PART_TIME);
    person(demands, h14, 63, MALE, NOT_EMPLOYED);

    var h15 = household(demands, zoneByName.get("G"), 1, 79);
    person(demands, h15, 26, MALE, FULL_TIME);
    var h16 = household(demands, zoneByName.get("G"), 2, 132);
    var p30 = person(demands, h16, 39, FEMALE, FULL_TIME);
    person(demands, h16, 40, MALE, PART_TIME);
    person(demands, h16, 10, MALE, NOT_EMPLOYED);
    person(demands, h16, 6, FEMALE, NOT_EMPLOYED);
    tour(demands, "work", zoneByName.get("J"), car, COMMUTE, COMMUTE_BACK, p30);
    var h17 = household(demands, zoneByName.get("G"), 0, 115);
    var p34 = person(demands, h17, 33, FEMALE, FULL_TIME);
    person(demands, h17, 35, FEMALE, FULL_TIME);
    tour(demands, "work", zoneByName.get("N"), bus, LATE_COMMUTE, COMMUTE_BACK, p34);
    var h18 = household(demands, zoneByName.get("G"), 1, 76);
    person(demands, h18, 47, FEMALE, FULL_TIME);
    person(demands, h18, 15, MALE, NOT_EMPLOYED);
    person(demands, h18, 12, FEMALE, NOT_EMPLOYED);

    var h19 = household(demands, zoneByName.get("H"), 2, 230);
    var p39 = person(demands, h19, 50, MALE, FULL_TIME);
    person(demands, h19, 48, FEMALE, FULL_TIME);
    tour(demands, "work", zoneByName.get("G"), car, COMMUTE, COMMUTE_BACK, p39);
    var h20 = household(demands, zoneByName.get("H"), 0, 28);
    person(demands, h20, 81, FEMALE, NOT_EMPLOYED);
    var h21 = household(demands, zoneByName.get("H"), 1, 95);
    person(demands, h21, 37, MALE, FULL_TIME);

    var h22 = household(demands, zoneByName.get("I"), 1, 92);
    person(demands, h22, 42, FEMALE, PART_TIME);
    person(demands, h22, 44, MALE, FULL_TIME);
    var p45 = person(demands, h22, 11, FEMALE, NOT_EMPLOYED);
    person(demands, h22, 8, MALE, NOT_EMPLOYED);
    tour(demands, "school", zoneByName.get("I"), walk, LocalTime.of(8, 15), LocalTime.of(15, 15), p45);
    var h23 = household(demands, zoneByName.get("I"), 0, 39);
    person(demands, h23, 23, FEMALE, FULL_TIME);

    var h24 = household(demands, zoneByName.get("J"), 1, 61);
    person(demands, h24, 64, MALE, NOT_EMPLOYED);
    person(demands, h24, 66, FEMALE, NOT_EMPLOYED);
    var h25 = household(demands, zoneByName.get("J"), 1, 128);
    var p50 = person(demands, h25, 38, MALE, FULL_TIME);
    person(demands, h25, 36, FEMALE, FULL_TIME);
    person(demands, h25, 4, FEMALE, NOT_EMPLOYED);
    var h26 = household(demands, zoneByName.get("J"), 0, 70);
    var p53 = person(demands, h26, 45, FEMALE, FULL_TIME);
    tour(demands, "work", zoneByName.get("K"), walk, LATE_COMMUTE, COMMUTE_BACK, p53);
    var h27 = household(demands, zoneByName.get("J"), 2, 102);
    person(demands, h27, 40, FEMALE, FULL_TIME);
    person(demands, h27, 41, MALE, NOT_EMPLOYED);
    person(demands, h27, 13, MALE, NOT_EMPLOYED);
    person(demands, h27, 11, FEMALE, NOT_EMPLOYED);
    person(demands, h27, 9, MALE, NOT_EMPLOYED);

    var h28 = household(demands, zoneByName.get("K"), 1, 83);
    person(demands, h28, 29, MALE, FULL_TIME);
    var h29 = household(demands, zoneByName.get("K"), 1, 118);
    person(demands, h29, 57, FEMALE, FULL_TIME);
    var p61 = person(demands, h29, 59, MALE, PART_TIME);
    var h30 = household(demands, zoneByName.get("K"), 2, 150);
    person(demands, h30, 35, FEMALE, FULL_TIME);
    var p63 = person(demands, h30, 37, MALE, FULL_TIME);
    person(demands, h30, 6, MALE, NOT_EMPLOYED);
    person(demands, h30, 3, FEMALE, NOT_EMPLOYED);
    tour(demands, "work", zoneByName.get("P"), car, LATE_COMMUTE, COMMUTE_BACK, p63);
    var h31 = household(demands, zoneByName.get("K"), 0, 34);
    person(demands, h31, 73, MALE, NOT_EMPLOYED);
    var h32 = household(demands, zoneByName.get("K"), 1, 108);
    var p67 = person(demands, h32, 27, FEMALE, FULL_TIME);
    var p68 = person(demands, h32, 28, MALE, FULL_TIME);
    tour(demands, "shopping", zoneByName.get("O"), car, SHOPPING, SHOPPING_BACK, p67, p68);

    var h33 = household(demands, zoneByName.get("L"), 1, 87);
    person(demands, h33, 44, MALE, FULL_TIME);
    person(demands, h33, 42, FEMALE, NOT_EMPLOYED);
    person(demands, h33, 17, MALE, NOT_EMPLOYED);
    var h34 = household(demands, zoneByName.get("L"), 0, 48);
    person(demands, h34, 30, FEMALE, PART_TIME);

    var h35 = household(demands, zoneByName.get("M"), 1, 42);
    person(demands, h35, 76, MALE, NOT_EMPLOYED);
    person(demands, h35, 74, FEMALE, NOT_EMPLOYED);
    var h36 = household(demands, zoneByName.get("M"), 1, 67);
    var p75 = person(demands, h36, 32, MALE, FULL_TIME);
    tour(demands, "work", zoneByName.get("G"), car, COMMUTE, COMMUTE_BACK, p75);

    var h37 = household(demands, zoneByName.get("N"), 2, 145);
    var p76 = person(demands, h37, 37, FEMALE, FULL_TIME);
    person(demands, h37, 39, MALE, FULL_TIME);
    person(demands, h37, 8, FEMALE, NOT_EMPLOYED);
    person(demands, h37, 5, MALE, NOT_EMPLOYED);
    tour(demands, "work", zoneByName.get("K"), car, COMMUTE, COMMUTE_BACK, p76);
    var h38 = household(demands, zoneByName.get("N"), 1, 81);
    person(demands, h38, 55, FEMALE, FULL_TIME);
    var h39 = household(demands, zoneByName.get("N"), 0, 62);
    var p81 = person(demands, h39, 25, MALE, FULL_TIME);
    person(demands, h39, 24, FEMALE, PART_TIME);
    tour(demands, "work", zoneByName.get("F"), bus, LATE_COMMUTE, COMMUTE_BACK, p81);

    var h40 = household(demands, zoneByName.get("O"), 1, 40);
    person(demands, h40, 66, MALE, NOT_EMPLOYED);
    var h41 = household(demands, zoneByName.get("O"), 2, 170);
    var p84 = person(demands, h41, 43, MALE, FULL_TIME);
    person(demands, h41, 41, FEMALE, PART_TIME);
    person(demands, h41, 15, FEMALE, NOT_EMPLOYED);
    person(demands, h41, 13, MALE, NOT_EMPLOYED);
    person(demands, h41, 10, FEMALE, NOT_EMPLOYED);
    tour(demands, "work", zoneByName.get("G"), car, COMMUTE, COMMUTE_BACK, p84);
    var h42 = household(demands, zoneByName.get("O"), 1, 112);
    person(demands, h42, 31, FEMALE, FULL_TIME);
    person(demands, h42, 32, MALE, FULL_TIME);

    var h43 = household(demands, zoneByName.get("P"), 1, 90);
    var p91 = person(demands, h43, 48, MALE, FULL_TIME);
    tour(demands, "work", zoneByName.get("K"), car, COMMUTE, COMMUTE_BACK, p91);
    var h44 = household(demands, zoneByName.get("P"), 1, 104);
    person(demands, h44, 34, FEMALE, FULL_TIME);
    person(demands, h44, 36, MALE, FULL_TIME);
    person(demands, h44, 2, MALE, NOT_EMPLOYED);

    tour(demands, "visit", zoneByName.get("P"), car, LocalTime.of(19, 30), LocalTime.of(1, 30), p50, p61);

    var p95 = registerPerson(demands, null, "p95", 34, FULL_TIME, null, null, MALE);
    registerTourThereAndBack(demands, List.of(p95), zoneByName.get("G"), zoneByName.get("P"), "work",
        car, LocalTime.of(7, 45), LocalTime.of(17, 15));
    registerPerson(demands, null, "p96", 81, NOT_EMPLOYED, null, null, FEMALE);
    return demands;
  }

  /**
   * Create the logger once for all tests of the class
   *
   * @throws Exception when the log file cannot be opened
   */
  @BeforeAll
  public static void setUpLogger() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(DiscreteDemandsDownSamplingFourByFourGridTest.class);
    }
  }

  /** Close the logger after all tests of the class */
  @AfterAll
  public static void tearDownLogger() {
    Logging.closeLogger(LOGGER);
  }

  /** The grid of zones A to P, its zones by name, and the settings of the draw, before each test */
  @BeforeEach
  public void setUp() {
    IdGenerator.reset();
    var mapRows = new String[]{"MNOP", "IJKL", "EFGH", "ABCD"};
    grid = new DownSamplingTestGrid(token, 4, 500, (x, y) -> String.valueOf(mapRows[y].charAt(x)),
        (origin, destination) -> {
          var from = origin.getCentroid().getPosition().getCoordinate();
          var to = destination.getCentroid().getPosition().getCoordinate();
          return origin == destination ? 0.52 : (Math.abs(from.x - to.x) + Math.abs(from.y - to.y)) / 1000;
        });
    zoneByName = grid.getZoning().getOdZones().createIndex(OdZone::getXmlId);

    settings = new DiscreteDemandsDownSamplingSettings();
    settings.setAlgorithmType(DiscreteDemandsDownSamplingAlgorithmType.BALANCED);
    settings.setDownSampleRate(0.25);
    settings.setSeed(42);
    settings.setBalanceOrder(
        PERSONS_PER_ZONE, PERSON_TRIPS_BY_MODE, PERSON_KILOMETRES_BY_MODE_GROUP, CAR_DRIVEN_TRIPS_BY_DEPARTURE_PERIOD);
    settings.setNumberOfDestinationAreas(4);
  }

  /** Reset the ids after each test */
  @AfterEach
  public void tearDown() {
    IdGenerator.reset();
  }

  /**
   * The zones lie along the Hilbert curve as M, N, J, I, E, A, B, F, G, C, D, H, L, K, O, P, and the demand makes 45
   * sampling units: 44 households less one for h25 and h29 sharing a tour, and p95 and p96
   */
  @Test
  public void testZoneOrderAndSamplingUnits() {
    assertEquals(List.of("M", "N", "J", "I", "E", "A", "B", "F", "G", "C", "D", "H", "L", "K", "O", "P"),
        grid.getCurveOrder().stream().map(OdZone::getXmlId).collect(Collectors.toList()));
    var table = DiscreteDemandsDownSamplingAlgorithmFactory.create(settings)
        .createDownSamplingTable(createDemand(), grid.getZoning(), grid.getDistances());
    assertEquals(45, table.size());
  }

  /**
   * At seed 42 the zones M, I, A, D and L, and p96 without a zone, keep no sampling unit and every other zone keeps
   * one: h39 in N, h27 in J, h8 in E, h3 in B, h14 in F, p95 in G, h5 in C, h19 in H, h32 in K, h40 in O and h44 in P
   */
  @Test
  public void testBalancedQuarterAtSeed42() {
    var demands = createDemand();
    DiscreteDemandsDownSamplingAlgorithmFactory.create(settings)
        .downSample(demands, grid.getZoning(), grid.getDistances(), false);
    assertEquals(Set.of("h39", "h27", "h8", "h3", "h14", "p95", "h5", "h19", "h32", "h40", "h44"),
        collectXmlIdsOfSamplingUnitMembers(demands));
  }
}

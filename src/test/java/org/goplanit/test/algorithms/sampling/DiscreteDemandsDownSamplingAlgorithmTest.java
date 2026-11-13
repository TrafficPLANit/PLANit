package org.goplanit.test.algorithms.sampling;

import org.goplanit.algorithms.parallel.GenericBatchExecutorService;
import org.goplanit.algorithms.sampling.demands.BestOfSeveralBalancedDownSamplingAlgorithm;
import org.goplanit.algorithms.sampling.demands.DiscreteDemandsDownSamplingAlgorithm;
import org.goplanit.algorithms.sampling.demands.DiscreteDemandsDownSamplingAlgorithmFactory;
import org.goplanit.algorithms.sampling.demands.enums.DiscreteDemandsDownSamplingAlgorithmType;
import org.goplanit.algorithms.sampling.demands.result.DiscreteDemandsDownSamplingResult;
import org.goplanit.algorithms.sampling.demands.DiscreteDemandsDownSamplingSettings;
import org.goplanit.algorithms.sampling.demands.enums.DownSamplingBalanceGroup;
import org.goplanit.demands.discrete.DiscreteDemands;
import org.goplanit.demands.discrete.household.Household;
import org.goplanit.demands.discrete.person.Person;
import org.goplanit.demands.discrete.trip.TripUtils;
import org.goplanit.logging.Logging;
import org.goplanit.test.algorithms.sampling.utils.DownSamplingTestDemands;
import org.goplanit.utils.id.IdAbleUtils;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.unit.Unit;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.zoning.Zoning;
import org.goplanit.zoning.zonetozone.OdSkimMatrix;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.goplanit.test.algorithms.sampling.utils.DownSamplingTestDemands.SHARED_TOUR_EVERY;
import static org.goplanit.test.algorithms.sampling.utils.DownSamplingTestDemands.collectXmlIdsOfSamplingUnitMembers;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Down-sampling a discrete demand at each quality, by the scenarios of the core/demand-down-sampling spec, on the
 * synthetic demands of {@link DownSamplingTestDemands}, of up to 100,000 households
 *
 * @author markr
 */
public class DiscreteDemandsDownSamplingAlgorithmTest {

  /** the logger, giving the log output PLANit's format */
  private static Logger LOGGER = null;

  /** index of the car driven trips in the values of a household */
  private static final int CAR_DRIVEN_TRIPS = 0;

  /** index of the car driven trips departing from 08:00 up to 10:00 in the values of a household */
  private static final int CAR_DRIVEN_TRIPS_8_TO_10 = 1;

  /** index of the car driven metres in the values of a household */
  private static final int CAR_DRIVEN_METRES = 2;

  /**
   * How far the sample's total of a value is from its target, over the spread a random draw of households gives
   * <p>
   * Each household's chance is its zone's households kept over its households. The target is the sum of chance times
   * value, and the spread the square root of the sum of chance times one less the chance times the value squared.
   * </p>
   *
   * @param positionByHousehold zone position along the curve of each household, by XML id
   * @param valuesByHousehold values of each household, by XML id
   * @param kept XML ids of the households kept
   * @param value index of the value
   * @return the difference from the target over the spread
   */
  private static double findErrorOverRandomSpread(Map<String, Integer> positionByHousehold,
      Map<String, long[]> valuesByHousehold, Set<String> kept, int value) {
    var households = new HashMap<Integer, Integer>();
    var keptHouseholds = new HashMap<Integer, Integer>();
    positionByHousehold.forEach((xmlId, position) -> {
      households.merge(position, 1, Integer::sum);
      keptHouseholds.merge(position, kept.contains(xmlId) ? 1 : 0, Integer::sum);
    });
    double target = 0;
    double sample = 0;
    double variance = 0;
    for (var entry : positionByHousehold.entrySet()) {
      double chance = keptHouseholds.get(entry.getValue()) / (double) households.get(entry.getValue());
      double householdValue = valuesByHousehold.get(entry.getKey())[value];
      target += chance * householdValue;
      variance += chance * (1 - chance) * householdValue * householdValue;
      sample += kept.contains(entry.getKey()) ? householdValue : 0;
    }
    return Math.abs(sample - target) / Math.sqrt(variance);
  }

  /** id token of the test */
  private final IdGroupingToken token = IdGroupingToken.collectGlobalToken();

  /** the synthetic demands, with their zoning, modes and lookup */
  private DownSamplingTestDemands synthetic;

  /** the car */
  private Mode car;

  /** zoning of the grid of zones */
  private Zoning zoning;

  /** the zones in their order along the Hilbert curve */
  private List<OdZone> curveOrder;

  /** the lookup, one straight line skim for every mode */
  private Map<Mode, OdSkimMatrix> distances;

  /** the settings, at their defaults until a test changes them */
  private DiscreteDemandsDownSamplingSettings settings;

  /**
   * The car driven values of each household, counted as the spec says: each trip by car once for the person assumed to
   * drive it, with its distance in whole metres
   *
   * @param demands holding the households
   * @return the car driven trips, those departing from 08:00 up to 10:00, and the metres, by XML id
   */
  private Map<String, long[]> collectCarDrivenValues(DiscreteDemands demands) {
    var values = new HashMap<String, long[]>();
    demands.getHouseholds().forEach(household -> values.put(household.getXmlId(), new long[3]));
    for (var trip : demands.getTrips()) {
      if (trip.getMode() != car) {
        continue;
      }
      for (var participation : trip.getTour().getParticipantTours()) {
        Household household = participation.getPerson().getHousehold();
        if (!participation.isAssumedAsDriver() || household == null) {
          continue;
        }
        var householdValues = values.get(household.getXmlId());
        ++householdValues[CAR_DRIVEN_TRIPS];
        var departure = trip.getStartTime();
        if (!departure.isBefore(LocalTime.of(8, 0)) && departure.isBefore(LocalTime.of(10, 0))) {
          ++householdValues[CAR_DRIVEN_TRIPS_8_TO_10];
        }
        householdValues[CAR_DRIVEN_METRES] += Math.round(TripUtils.getDistance(trip, distances, Unit.METER));
      }
    }
    return values;
  }

  /**
   * Verify each zone kept its households times the rate rounded down or up, and the scenario zones as the spec says:
   * the zone of 37 households 3 or 4, the ten of 4 next to each other exactly 4 together and each 0 or 1, and those of
   * 13, 7 and 4 two or three together
   *
   * @param positionByHousehold zone position along the curve of each household before, by XML id
   * @param kept XML ids of the households kept
   */
  private void assertZonesKeepTheirShare(Map<String, Integer> positionByHousehold, Set<String> kept) {
    var households = new int[curveOrder.size()];
    var keptHouseholds = new int[curveOrder.size()];
    positionByHousehold.forEach((xmlId, position) -> {
      ++households[position];
      keptHouseholds[position] += kept.contains(xmlId) ? 1 : 0;
    });
    double rate = settings.getDownSampleRate();
    for (int position = 0; position < curveOrder.size(); ++position) {
      double expected = households[position] * rate;
      assertTrue(keptHouseholds[position] >= Math.floor(expected - 1e-9)
          && keptHouseholds[position] <= Math.ceil(expected + 1e-9));
    }
    assertTrue(keptHouseholds[0] == 3 || keptHouseholds[0] == 4);
    int runOfTen = 0;
    for (int position = 1; position <= 10; ++position) {
      assertTrue(keptHouseholds[position] <= 1);
      runOfTen += keptHouseholds[position];
    }
    assertEquals(4, runOfTen);
    int runOfThree = keptHouseholds[11] + keptHouseholds[12] + keptHouseholds[13];
    assertTrue(runOfThree == 2 || runOfThree == 3);
  }

  /**
   * The algorithm of the given type with the test's settings
   *
   * @param type of the algorithm
   * @return the algorithm
   */
  private DiscreteDemandsDownSamplingAlgorithm createAlgorithm(DiscreteDemandsDownSamplingAlgorithmType type) {
    settings.setAlgorithmType(type);
    return DiscreteDemandsDownSamplingAlgorithmFactory.create(settings);
  }

  /**
   * Create the logger once for all tests of the class
   *
   * @throws Exception when the log file cannot be opened
   */
  @BeforeAll
  public static void setUpLogger() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(DiscreteDemandsDownSamplingAlgorithmTest.class);
    }
  }

  /** Close the logger after all tests of the class */
  @AfterAll
  public static void tearDownLogger() {
    Logging.closeLogger(LOGGER);
  }

  /** The synthetic demands' grid of zones, modes and lookup, and the settings, before each test */
  @BeforeEach
  public void setUp() {
    IdGenerator.reset();
    synthetic = new DownSamplingTestDemands(token);
    car = synthetic.getCar();
    zoning = synthetic.getZoning();
    curveOrder = synthetic.getCurveOrder();
    distances = synthetic.getDistances();
    settings = new DiscreteDemandsDownSamplingSettings();
  }

  /** Reset the ids after each test */
  @AfterEach
  public void tearDown() {
    IdGenerator.reset();
  }

  /** The factory creates the algorithm of the type the settings ask for */
  @Test
  public void testFactoryCreatesTheTypeAsked() {
    for (var type : DiscreteDemandsDownSamplingAlgorithmType.values()) {
      assertEquals(type, createAlgorithm(type).getType());
    }
  }

  /**
   * Best of three keeps, of the balanced draws at the seeds of draws 0, 1 and 2, the one with the lowest score: the
   * same households and persons without a household, with the same score. The three scores differ, so the choice is
   * a real one
   */
  @Test
  public void testBestOfThree() {
    var table = createAlgorithm(DiscreteDemandsDownSamplingAlgorithmType.BALANCED)
        .createDownSamplingTable(synthetic.createDemand(10_000, true), zoning, distances);
    var draws = new ArrayList<DiscreteDemandsDownSamplingResult>();
    for (int draw = 0; draw < 3; ++draw) {
      var drawSettings = settings.shallowClone();
      drawSettings.setSeed(BestOfSeveralBalancedDownSamplingAlgorithm.createSeedOfDraw(settings.getSeed(), draw));
      draws.add(DiscreteDemandsDownSamplingAlgorithmFactory.create(drawSettings).drawFrom(table));
    }
    var scores = draws.stream().mapToDouble(draw -> draw.getStatistics().getScore()).sorted().toArray();
    assertTrue(scores[0] < scores[1] && scores[1] < scores[2]);
    var best = draws.stream().min(Comparator.comparingDouble(draw -> draw.getStatistics().getScore())).orElseThrow();

    settings.setNumberOfDraws(3);
    var result = createAlgorithm(DiscreteDemandsDownSamplingAlgorithmType.BALANCED_BEST_OF_SEVERAL).drawFrom(table);
    assertEquals(best.getHouseholdIdsKept(), result.getHouseholdIdsKept());
    assertEquals(best.getPersonIdsWithoutHouseholdKept(), result.getPersonIdsWithoutHouseholdKept());
    assertEquals(scores[0], result.getStatistics().getScore(), 1e-12);
  }

  /**
   * At the default quality, 100,000 households down-sampled to 0.1 keep 9,999 to 10,001; every zone its share, the
   * scenario zones as the spec says; and the car driven trips, those departing between 08:00 and 10:00, and the car
   * driven kilometres each come within a tenth of the spread a random draw of households gives
   */
  @Test
  public void testBalancedTenPercent() {
    var demands = synthetic.createDemand(100_000, false);
    var positionByHousehold = synthetic.collectPositionByHousehold(demands);
    var carDriven = collectCarDrivenValues(demands);
    long carDrivenTrips = carDriven.values().stream().mapToLong(values -> values[CAR_DRIVEN_TRIPS]).sum();
    long personTrips = demands.getTrips().stream().mapToLong(trip -> trip.getTour().getParticipantTours().size()).sum();
    assertEquals(0.3, carDrivenTrips / (double) personTrips, 0.03);

    DiscreteDemandsDownSamplingAlgorithmFactory.create(settings).downSample(demands, zoning, distances, false);
    var kept = collectXmlIdsOfSamplingUnitMembers(demands);
    assertTrue(kept.size() >= 9_999 && kept.size() <= 10_001);
    assertZonesKeepTheirShare(positionByHousehold, kept);
    for (int value : new int[]{CAR_DRIVEN_TRIPS, CAR_DRIVEN_TRIPS_8_TO_10, CAR_DRIVEN_METRES}) {
      assertTrue(findErrorOverRandomSpread(positionByHousehold, carDriven, kept, value) < 0.1);
    }
  }

  /**
   * Households at random, 1,000 draws of 0.1 from the same 100,000 households with different seeds: 10,000 kept on
   * average, to within 10, and the number kept varying from draw to draw. The table is filled once and drawn from by
   * each
   */
  @Test
  public void testHouseholdsAtRandom() {
    var demands = synthetic.createDemand(100_000, false);
    var table = createAlgorithm(DiscreteDemandsDownSamplingAlgorithmType.HOUSEHOLDS_AT_RANDOM)
        .createDownSamplingTable(demands, zoning, distances);

    long total = 0;
    int fewest = Integer.MAX_VALUE;
    int most = 0;
    for (long seed = 0; seed < 1000; ++seed) {
      settings.setSeed(seed);
      int kept = DiscreteDemandsDownSamplingAlgorithmFactory.create(settings).drawFrom(table)
          .getHouseholdIdsKept().cardinality();
      total += kept;
      fewest = Math.min(fewest, kept);
      most = Math.max(most, kept);
    }
    assertEquals(10_000, total / 1000.0, 10);
    assertTrue(fewest < most);
  }

  /** By zone, 10,000 households down-sampled to 0.1: each zone keeps its share, and 999 to 1,001 are kept */
  @Test
  public void testByZone() {
    var demands = synthetic.createDemand(10_000, false);
    var positionByHousehold = synthetic.collectPositionByHousehold(demands);
    createAlgorithm(DiscreteDemandsDownSamplingAlgorithmType.BY_ZONE).downSample(demands, zoning, distances, false);
    var kept = collectXmlIdsOfSamplingUnitMembers(demands);
    assertTrue(kept.size() >= 999 && kept.size() <= 1_001);
    assertZonesKeepTheirShare(positionByHousehold, kept);
  }

  /**
   * Balanced on persons per zone and person trips by mode only, the car driven trips still come within a tenth of the
   * spread a random draw of households gives
   */
  @Test
  public void testGroupsLeftOut() {
    var demands = synthetic.createDemand(10_000, false);
    var positionByHousehold = synthetic.collectPositionByHousehold(demands);
    var carDriven = collectCarDrivenValues(demands);
    settings.setBalanceOrder(DownSamplingBalanceGroup.PERSONS_PER_ZONE, DownSamplingBalanceGroup.PERSON_TRIPS_BY_MODE);
    DiscreteDemandsDownSamplingAlgorithmFactory.create(settings).downSample(demands, zoning, distances, false);
    var kept = collectXmlIdsOfSamplingUnitMembers(demands);
    assertZonesKeepTheirShare(positionByHousehold, kept);
    assertTrue(findErrorOverRandomSpread(positionByHousehold, carDriven, kept, CAR_DRIVEN_TRIPS) < 0.1);
  }

  /**
   * A household kept keeps all its persons, and no tour or trip is left with a person or tour removed; two households
   * sharing a visit are kept or removed together; a person without a household is kept or removed on their own
   */
  @Test
  public void testWholeHouseholdsAndJointTours() {
    var demands = synthetic.createDemand(10_000, true);
    var personsBefore = new HashMap<String, Integer>();
    demands.getPersons().createGroupByIndex(Person::getHousehold).forEach(
        (household, persons) -> personsBefore.put(household.getXmlId(), persons.size()));
    int personsAlone = (int) demands.getPersons().stream().filter(person -> person.getHousehold() == null).count();

    DiscreteDemandsDownSamplingAlgorithmFactory.create(settings).downSample(demands, zoning, distances, false);
    var kept = collectXmlIdsOfSamplingUnitMembers(demands);
    var personsByHousehold = demands.getPersons().createGroupByIndex(Person::getHousehold);
    int personsAloneKept = 0;
    for (var person : demands.getPersons()) {
      var household = person.getHousehold();
      assertTrue(household == null || demands.getHouseholds().get(household.getId()) == household);
      personsAloneKept += household == null ? 1 : 0;
    }
    for (var household : demands.getHouseholds()) {
      assertEquals(personsBefore.get(household.getXmlId()), personsByHousehold.get(household).size());
    }
    assertEquals(demands.getHouseholds().stream().mapToInt(household -> personsBefore.get(household.getXmlId())).sum()
        + personsAloneKept, demands.getPersons().size());
    assertTrue(personsAloneKept > 0 && personsAloneKept < personsAlone);

    for (var tour : demands.getTours()) {
      tour.getParticipantTours().forEach(participation ->
          assertTrue(demands.getPersons().get(participation.getPerson().getId()) == participation.getPerson()));
    }
    for (var trip : demands.getTrips()) {
      assertTrue(demands.getTours().get(trip.getTour().getId()) == trip.getTour());
    }

    int pairsKept = 0;
    for (int index = 2 * SHARED_TOUR_EVERY + 1; index < 10_000; index += SHARED_TOUR_EVERY) {
      assertEquals(kept.contains("h" + (index - 1)), kept.contains("h" + index));
      pairsKept += kept.contains("h" + index) ? 1 : 0;
    }
    assertTrue(pairsKept > 0);
  }

  /** At a rate of 1 every household, person, tour and trip is still there */
  @Test
  public void testRateOne() {
    var demands = synthetic.createDemand(10_000, true);
    var counts = List.of(demands.getHouseholds().size(), demands.getPersons().size(), demands.getTours().size(),
        demands.getTrips().size());
    settings.setDownSampleRate(1);
    DiscreteDemandsDownSamplingAlgorithmFactory.create(settings).downSample(demands, zoning, distances, false);
    assertEquals(counts, List.of(demands.getHouseholds().size(), demands.getPersons().size(),
        demands.getTours().size(), demands.getTrips().size()));
  }

  /**
   * The same demand down-sampled twice with the same settings and seed keeps the same households, once on one thread
   * and once on up to eight; and the same table drawn from twice gives the same sampling units
   */
  @Test
  public void testSameSampleTwiceAndOnAnyNumberOfThreads() {
    var once = synthetic.createDemand(10_000, true);
    settings.setNumberOfThreads(1);
    DiscreteDemandsDownSamplingAlgorithmFactory.create(settings).downSample(once, zoning, distances, false);

    var twice = synthetic.createDemand(10_000, true);
    settings.setNumberOfThreads(Math.min(8, GenericBatchExecutorService.getDefaultNumberOfThreads()));
    DiscreteDemandsDownSamplingAlgorithmFactory.create(settings).downSample(twice, zoning, distances, false);
    assertEquals(collectXmlIdsOfSamplingUnitMembers(once), collectXmlIdsOfSamplingUnitMembers(twice));

    var algorithm = DiscreteDemandsDownSamplingAlgorithmFactory.create(settings);
    var table = algorithm.createDownSamplingTable(synthetic.createDemand(10_000, true), zoning, distances);
    assertEquals(algorithm.drawFrom(table).getHouseholdIdsKept(), algorithm.drawFrom(table).getHouseholdIdsKept());
  }

  /**
   * 10 households in one zone down-sampled to 0.5: without asking, the 5 left keep their ids; asking for the ids to be
   * made contiguous, the 5 left are numbered 0 to 4 in the order they had, with their XML ids, and so are the persons,
   * tours and trips left
   */
  @Test
  public void testIdsKeptOrMadeContiguous() {
    settings.setDownSampleRate(0.5);
    var demands = synthetic.createDemand(10, false);
    var idByXmlId = new HashMap<String, Long>();
    demands.getHouseholds().forEach(household -> idByXmlId.put(household.getXmlId(), household.getId()));
    DiscreteDemandsDownSamplingAlgorithmFactory.create(settings).downSample(demands, zoning, distances, false);
    assertEquals(5, demands.getHouseholds().size());
    demands.getHouseholds().forEach(household -> assertEquals(idByXmlId.get(household.getXmlId()), household.getId()));

    var contiguous = synthetic.createDemand(10, false);
    var xmlIdsInIdOrder = new ArrayList<String>();
    contiguous.getHouseholds().forEach(household -> xmlIdsInIdOrder.add(household.getXmlId()));
    DiscreteDemandsDownSamplingAlgorithmFactory.create(settings).downSample(contiguous, zoning, distances, true);
    assertEquals(LongStream.range(0, 5).boxed().collect(Collectors.toList()),
        IdAbleUtils.collectSortedIds(contiguous.getHouseholds().toCollection()));
    var kept = collectXmlIdsOfSamplingUnitMembers(contiguous);
    var keptInIdOrder = new ArrayList<String>();
    contiguous.getHouseholds().forEach(household -> keptInIdOrder.add(household.getXmlId()));
    assertEquals(xmlIdsInIdOrder.stream().filter(kept::contains).collect(Collectors.toList()), keptInIdOrder);
    for (var entities : List.of(contiguous.getPersons().toCollection(), contiguous.getTours().toCollection(),
        contiguous.getTrips().toCollection())) {
      assertEquals(LongStream.range(0, entities.size()).boxed().collect(Collectors.toList()),
          IdAbleUtils.collectSortedIds(entities));
    }
  }

  /**
   * A draw from the table names the households and persons without a household that down-sampling the demand in place
   * keeps: the same seed, the same sampling units, by their ids in the demand
   */
  @Test
  public void testDrawNamesWhatDownSamplingKeeps() {
    var inPlace = synthetic.createDemand(10_000, true);
    DiscreteDemandsDownSamplingAlgorithmFactory.create(settings).downSample(inPlace, zoning, distances, false);

    var demands = synthetic.createDemand(10_000, true);
    var algorithm = DiscreteDemandsDownSamplingAlgorithmFactory.create(settings);
    var result = algorithm.drawFrom(algorithm.createDownSamplingTable(demands, zoning, distances));
    var named = new TreeSet<String>();
    result.getHouseholdIdsKept().stream().forEach(id -> named.add(demands.getHouseholds().get(id).getXmlId()));
    result.getPersonIdsWithoutHouseholdKept().stream().forEach(id -> named.add(demands.getPersons().get(id).getXmlId()));
    assertEquals(collectXmlIdsOfSamplingUnitMembers(inPlace), named);
  }

  /**
   * Origin areas are off by default: their group has no categories. Allowed three at a rate of 0.5, the zones are
   * binned along the curve into three areas of about equal car driven trips by origin, a third each give or take a
   * zone's worth, together every car driven trip of the full demand
   */
  @Test
  public void testOriginAreasCutByCarDrivenTrips() {
    var group = DownSamplingBalanceGroup.CAR_DRIVEN_TRIPS_BY_ORIGIN_AREA;
    var demands = synthetic.createDemand(10_000, false);
    settings.setDownSampleRate(0.5);
    var off = createAlgorithm(DiscreteDemandsDownSamplingAlgorithmType.BALANCED)
        .createDownSamplingTable(demands, zoning, distances);
    assertTrue(off.getCategories(group).isEmpty());

    settings.setMaximumNumberOfOriginAreas(3);
    var table = createAlgorithm(DiscreteDemandsDownSamplingAlgorithmType.BALANCED)
        .createDownSamplingTable(demands, zoning, distances);
    assertEquals(List.of("1", "2", "3"), table.getCategories(group));
    var modes = table.getCategories(DownSamplingBalanceGroup.PERSON_TRIPS_BY_MODE);
    long carDriven = table.getFullTotal(
        DownSamplingBalanceGroup.PERSON_TRIPS_BY_MODE, modes.indexOf(car.getXmlId() + " driven"));
    long areasTogether = 0;
    for (int area = 0; area < 3; ++area) {
      assertEquals(carDriven / 3.0, table.getFullTotal(group, area), 0.15 * carDriven);
      areasTogether += table.getFullTotal(group, area);
    }
    assertEquals(carDriven, areasTogether);
  }

  /**
   * Allowed a thousand origin areas, there are only as many as keep, per area, 4 times the totals balanced over all
   * sampling units in sampling units kept: of 10,000 at a rate of 0.1, 1,000 kept, a couple of areas
   */
  @Test
  public void testOriginAreasAtMostTheFloorAllows() {
    settings.setMaximumNumberOfOriginAreas(1_000);
    var table = createAlgorithm(DiscreteDemandsDownSamplingAlgorithmType.BALANCED)
        .createDownSamplingTable(synthetic.createDemand(10_000, false), zoning, distances);
    int totals = settings.getBalanceOrder().stream()
        .filter(group -> group != DownSamplingBalanceGroup.PERSONS_PER_ZONE
            && group != DownSamplingBalanceGroup.CAR_DRIVEN_TRIPS_BY_ORIGIN_AREA)
        .mapToInt(group -> table.getCategories(group).size()).sum();
    int expected = (int) (settings.getDownSampleRate() * table.size() / (4.0 * totals));
    assertTrue(expected >= 2 && expected < 1_000);
    assertEquals(expected, table.getCategories(DownSamplingBalanceGroup.CAR_DRIVEN_TRIPS_BY_ORIGIN_AREA).size());
  }

  /** Balanced with three origin areas at a rate of 0.5, each area's car driven trips come within a tenth of the spread
   * a random draw gives */
  @Test
  public void testOriginAreasBalanced() {
    settings.setDownSampleRate(0.5);
    settings.setMaximumNumberOfOriginAreas(3);
    var statistics = DiscreteDemandsDownSamplingAlgorithmFactory.create(settings)
        .downSample(synthetic.createDemand(10_000, false), zoning, distances, false);
    var areas = statistics.getTotals(DownSamplingBalanceGroup.CAR_DRIVEN_TRIPS_BY_ORIGIN_AREA);
    assertEquals(3, areas.size());
    for (var area : areas) {
      assertTrue(Math.abs(area.getRatioToRandomSpread()) < 0.1);
    }
  }
}

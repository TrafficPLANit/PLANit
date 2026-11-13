package org.goplanit.test.algorithms.sampling;

import org.goplanit.algorithms.sampling.demands.DiscreteDemandsDownSamplingAlgorithmFactory;
import org.goplanit.algorithms.sampling.demands.DiscreteDemandsDownSamplingSettings;
import org.goplanit.algorithms.sampling.demands.result.DiscreteDemandsDownSamplingStatistics;
import org.goplanit.algorithms.sampling.demands.enums.DownSamplingBalanceGroup;
import org.goplanit.algorithms.sampling.demands.enums.DownSamplingCheckedTable;
import org.goplanit.algorithms.sampling.demands.result.DownSamplingTotal;
import org.goplanit.algorithms.sampling.demands.result.DownSamplingTotalsSummary;
import org.goplanit.algorithms.sampling.demands.enums.DownSamplingZoneMeasure;
import org.goplanit.demands.discrete.DiscreteDemands;
import org.goplanit.logging.Logging;
import org.goplanit.test.algorithms.sampling.utils.DownSamplingTestDemands;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The statistics of a down-sampling, by the scenarios of the core/demand-down-sampling-statistics spec: a total against
 * its target and the random spread, a group's error, the score over the checked tables, every group reported, the
 * zones, and the CSV, on the synthetic demands of {@link DownSamplingTestDemands}
 *
 * @author markr
 */
public class DiscreteDemandsDownSamplingStatisticsTest {

  /** the logger, giving the log output PLANit's format */
  private static Logger LOGGER = null;

  /** id token of the test */
  private final IdGroupingToken token = IdGroupingToken.collectGlobalToken();

  /** the synthetic demands, with their zoning, modes and lookup */
  private DownSamplingTestDemands synthetic;

  /** the settings, at their defaults until a test changes them */
  private DiscreteDemandsDownSamplingSettings settings;

  /**
   * Down-sample a demand held whole with the test's settings
   *
   * @param demands to down-sample
   * @return the statistics
   */
  private DiscreteDemandsDownSamplingStatistics downSample(DiscreteDemands demands) {
    return DiscreteDemandsDownSamplingAlgorithmFactory.create(settings)
        .downSample(demands, synthetic.getZoning(), synthetic.getDistances(), false);
  }

  /**
   * Create the logger once for all tests of the class
   *
   * @throws Exception when the log file cannot be opened
   */
  @BeforeAll
  public static void setUpLogger() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(DiscreteDemandsDownSamplingStatisticsTest.class);
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
    settings = new DiscreteDemandsDownSamplingSettings();
  }

  /** Reset the ids after each test */
  @AfterEach
  public void tearDown() {
    IdGenerator.reset();
  }

  /** 120 above a target of 1,000, with a random spread of 400: 12% of the target, and a ratio of 0.3 */
  @Test
  public void testRatioToRandomSpread() {
    var total = new DownSamplingTotal("car driven", 10_000, 1_000, 1_120, 400);
    assertEquals(120, total.getDifference(), 1e-9);
    assertEquals(0.12, total.getShareOfTarget(), 1e-9);
    assertEquals(0.3, total.getRatioToRandomSpread(), 1e-9);
  }

  /**
   * Targets 100 and 300 against samples of 110 and 290: an error of 0.05; with random spreads of 20 and 40, ratios of
   * 0.5 and 0.25 and a random error of the root of the mean of 400 and 1,600 over the mean target of 200
   */
  @Test
  public void testErrorOfAGroup() {
    var summary = new DownSamplingTotalsSummary(List.of(
        new DownSamplingTotal("a", 1_000, 100, 110, 20), new DownSamplingTotal("b", 3_000, 300, 290, 40)));
    assertEquals(2, summary.getNumberOfTotals());
    assertEquals(0.05, summary.getError(), 1e-9);
    assertEquals(Math.sqrt((400 + 1_600) / 2.0) / 200, summary.getRandomError(), 1e-9);
    assertEquals(0.1, summary.getLargestShareOfTarget(), 1e-9);
    assertEquals(0.375, summary.getMeanRatioToRandomSpread(), 1e-9);
    assertEquals(10, summary.getLargestDifference(), 1e-9);
    assertEquals(1.0, summary.getShareWithin(10), 1e-9);
    assertEquals(0.0, summary.getShareWithin(9), 1e-9);
  }

  /**
   * Five checked tables whose errors are half, the same as, a quarter of, three quarters of and half of what a random
   * draw would give score 0.6
   */
  @Test
  public void testScore() {
    var tables = IntStream.of(50, 100, 25, 75, 50)
        .mapToObj(difference -> new DownSamplingTotalsSummary(List.of(
            new DownSamplingTotal("cell", 1_000, 100, 100 + difference, 100))))
        .collect(Collectors.toList());
    assertEquals(0.6, DiscreteDemandsDownSamplingStatistics.calculateScore(tables), 1e-9);
  }

  /**
   * Balanced on persons per zone and person trips by mode only, every group and checked table is still reported, the
   * origin areas allowed: tours by purpose with one total per purpose, sorted by label, each target about a tenth of
   * the full demand's
   */
  @Test
  public void testGroupNotBalancedStillReported() {
    settings.setBalanceOrder(DownSamplingBalanceGroup.PERSONS_PER_ZONE, DownSamplingBalanceGroup.PERSON_TRIPS_BY_MODE);
    settings.setMaximumNumberOfOriginAreas(3);
    var statistics = downSample(synthetic.createDemand(10_000, false));
    for (var group : DownSamplingBalanceGroup.values()) {
      assertTrue(statistics.getSummary(group).getNumberOfTotals() > 0);
    }
    for (var table : DownSamplingCheckedTable.values()) {
      assertTrue(statistics.getSummary(table).getNumberOfTotals() > 0);
    }

    var purposes = statistics.getTotals(DownSamplingBalanceGroup.TOURS_BY_PURPOSE);
    assertEquals(DownSamplingTestDemands.PURPOSES.stream().sorted().collect(Collectors.toList()),
        purposes.stream().map(DownSamplingTotal::getCategory).collect(Collectors.toList()));
    for (var purpose : purposes) {
      assertEquals(0.1, purpose.getTarget() / purpose.getFull(), 0.01);
      assertTrue(purpose.getSample() > 0);
    }
  }

  /**
   * At the balanced quality each zone keeps exactly the households its chance aims at, its persons are reported within
   * one and within two of their target, and car driven trips by origin zone are reported next to the error a random
   * draw within each zone would give
   */
  @Test
  public void testZones() {
    var demands = synthetic.createDemand(10_000, false);
    int zonesWithHouseholds = new HashSet<>(synthetic.collectPositionByHousehold(demands).values()).size();
    var statistics = downSample(demands);

    assertEquals(zonesWithHouseholds, statistics.getZones(DownSamplingZoneMeasure.HOUSEHOLDS).size());
    assertEquals(0, statistics.getSummary(DownSamplingZoneMeasure.HOUSEHOLDS).getLargestDifference(), 1e-6);

    var persons = statistics.getSummary(DownSamplingZoneMeasure.PERSONS);
    double withinOne = persons.getShareWithin(1);
    double withinTwo = persons.getShareWithin(2);
    assertTrue(withinOne > 0.5 && withinOne <= withinTwo && withinTwo <= 1);

    var carDriven = statistics.getSummary(DownSamplingZoneMeasure.CAR_DRIVEN_TRIPS_BY_ORIGIN_ZONE);
    assertTrue(carDriven.getNumberOfTotals() > 0);
    assertTrue(carDriven.getRandomError() > 0);
    assertTrue(carDriven.getError() >= 0);
  }

  /**
   * With a statistics file named, the file has a header row, then a row per total of every group and per cell of every
   * checked table
   *
   * @param folder to write the file to
   * @throws IOException when the file cannot be read back
   */
  @Test
  public void testCsv(@TempDir Path folder) throws IOException {
    var file = folder.resolve("down_sampling_statistics.csv");
    settings.setStatisticsFile(file);
    var statistics = downSample(synthetic.createDemand(1_000, false));

    var lines = Files.readAllLines(file);
    assertEquals("group,category,full,target,sample,difference,ratio_to_random_spread", lines.get(0));
    int rows = 0;
    for (var group : DownSamplingBalanceGroup.values()) {
      rows += statistics.getTotals(group).size();
    }
    for (var table : DownSamplingCheckedTable.values()) {
      rows += statistics.getCells(table).size();
    }
    assertEquals(rows + 1, lines.size());
  }

  /**
   * Person kilometres are reported in kilometres: the full demand's kilometres over its person trips give a mean trip
   * between the 0.5 km of a trip within a zone and the 28 km across the grid
   */
  @Test
  public void testPersonKilometresInKilometres() {
    var statistics = downSample(synthetic.createDemand(1_000, false));
    double kilometres = statistics.getTotals(DownSamplingBalanceGroup.PERSON_KILOMETRES_BY_MODE_GROUP).stream()
        .mapToDouble(DownSamplingTotal::getFull).sum();
    double personTrips = statistics.getTotals(DownSamplingBalanceGroup.PERSON_TRIPS_BY_DISTANCE_BAND_AND_MODE_GROUP)
        .stream().mapToDouble(DownSamplingTotal::getFull).sum();
    double meanKm = kilometres / personTrips;
    assertTrue(meanKm >= 0.5 && meanKm <= 28);
  }
}

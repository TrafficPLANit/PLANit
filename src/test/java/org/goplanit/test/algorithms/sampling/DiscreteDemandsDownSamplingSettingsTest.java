package org.goplanit.test.algorithms.sampling;

import org.goplanit.algorithms.parallel.GenericBatchExecutorService;
import org.goplanit.algorithms.sampling.demands.enums.DiscreteDemandsDownSamplingAlgorithmType;
import org.goplanit.algorithms.sampling.demands.DiscreteDemandsDownSamplingSettings;
import org.goplanit.algorithms.sampling.demands.enums.DownSamplingBalanceGroup;
import org.goplanit.demands.TimePeriods;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.binning.BinningConfiguration;
import org.goplanit.utils.time.LocalTimeUtils;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The settings of down-sampling a discrete demand: their defaults, the departure periods, distance bands and bands of
 * whole numbers they make, the values refused, and a copy apart from the original
 *
 * @author markr
 */
public class DiscreteDemandsDownSamplingSettingsTest {

  /** the default departure period boundaries */
  private static final List<LocalTime> DEFAULT_PERIODS = List.of(LocalTime.of(6, 0), LocalTime.of(8, 0),
      LocalTime.of(10, 0), LocalTime.of(15, 0), LocalTime.of(17, 0), LocalTime.of(19, 0));

  /** the default distance band boundaries in km */
  private static final double[] DEFAULT_BANDS_KM = {1, 2, 5, 10, 20, 40};

  /** the labels of the default age bands */
  private static final List<String> DEFAULT_AGE_BANDS =
      List.of("0-4", "5-11", "12-17", "18-24", "25-44", "45-64", "65-79", ">=80");

  /** the labels of the default household size bands */
  private static final List<String> DEFAULT_HOUSEHOLD_SIZE_BANDS = List.of("0", "1", "2", "3", "4", ">=5");

  /** the labels of the default bands of cars and of workers per household */
  private static final List<String> DEFAULT_PER_HOUSEHOLD_BANDS = List.of("0", "1", "2", ">=3");

  /** the day start of an ActivitySim day */
  private static final LocalTime THREE_AM = LocalTime.of(3, 0);

  /**
   * The departure period a trip at the given time falls in, for a day starting at the given time
   *
   * @param periods the departure periods of that day
   * @param dayStart start of the day
   * @param time of the trip
   * @return index of its period
   */
  private static int periodOf(BinningConfiguration<Integer> periods, LocalTime dayStart, LocalTime time) {
    return periods.binIndexOf((int) LocalTimeUtils.secondsFromWrapAroundDayAnchor(dayStart, time));
  }

  /** Every setting starts at its default */
  @Test
  public void testDefaults() {
    var settings = new DiscreteDemandsDownSamplingSettings();
    assertEquals(DiscreteDemandsDownSamplingAlgorithmType.BALANCED, settings.getAlgorithmType());
    assertEquals(0.1, settings.getDownSampleRate());
    assertEquals(List.of(DownSamplingBalanceGroup.values()), settings.getBalanceOrder());
    assertEquals(DEFAULT_PERIODS, settings.getDeparturePeriodBoundaries());
    assertArrayEquals(DEFAULT_BANDS_KM, settings.getDistanceBandBoundariesKm());
    assertEquals(DEFAULT_AGE_BANDS, settings.getAgeBands().getLabels());
    assertEquals(DEFAULT_HOUSEHOLD_SIZE_BANDS, settings.getHouseholdSizeBands().getLabels());
    assertEquals(DEFAULT_PER_HOUSEHOLD_BANDS, settings.getCarsPerHouseholdBands().getLabels());
    assertEquals(DEFAULT_PER_HOUSEHOLD_BANDS, settings.getWorkersPerHouseholdBands().getLabels());
    assertEquals(5, settings.getNumberOfIncomeBands());
    assertEquals(12, settings.getNumberOfDestinationAreas());
    assertEquals(0, settings.getMaximumNumberOfOriginAreas());
    assertEquals(16, settings.getMinimumDrivingAge());
    assertEquals(3, settings.getNumberOfDraws());
    assertEquals(42, settings.getSeed());
    assertNull(settings.getStatisticsFile());
    assertEquals(GenericBatchExecutorService.getDefaultNumberOfThreads(), settings.getNumberOfThreads());
  }

  /**
   * Bands of whole numbers start at 0, each boundary starting the next band, and the last holds every number above:
   * age band boundaries 18 and 65 make 0-17, 18-64 and 65 and over, so 17 is in the first and 100 in the last. No
   * boundaries make one band of every number
   */
  @Test
  public void testCountBandsFromBoundaries() {
    var settings = new DiscreteDemandsDownSamplingSettings();
    settings.setAgeBandBoundaries(18, 65);
    var ages = settings.getAgeBands();
    assertEquals(List.of("0-17", "18-64", ">=65"), ages.getLabels());
    assertEquals(0, ages.binIndexOf(17));
    assertEquals(1, ages.binIndexOf(18));
    assertEquals(2, ages.binIndexOf(100));

    settings.setHouseholdSizeBandBoundaries();
    assertEquals(List.of(">=0"), settings.getHouseholdSizeBands().getLabels());
    settings.setCarsPerHouseholdBandBoundaries(1);
    assertEquals(List.of("0", ">=1"), settings.getCarsPerHouseholdBands().getLabels());
    settings.setWorkersPerHouseholdBandBoundaries(2);
    assertEquals(List.of("0-1", ">=2"), settings.getWorkersPerHouseholdBands().getLabels());
    settings.setNumberOfIncomeBands(10);
    assertEquals(10, settings.getNumberOfIncomeBands());
  }

  /**
   * For a day starting at midnight a trip at 07:45 is in the period from 06:00 to 08:00, the second of seven; one
   * before 06:00 in the first, one at or after 19:00 in the last. For a day starting at 03:00 the first period starts
   * at 03:00 and the last runs from 19:00 to 03:00, so a trip at 01:30 shares it with one at 20:00. A trip of 3 km is
   * in the band from 2 to 5 km, the third of seven; one of 40 km or more in the last
   */
  @Test
  public void testPeriodsAndBands() {
    var settings = new DiscreteDemandsDownSamplingSettings();
    var periods = settings.createDeparturePeriods(LocalTime.MIDNIGHT);
    assertEquals(7, periods.size());
    assertEquals(1, periodOf(periods, LocalTime.MIDNIGHT, LocalTime.of(7, 45)));
    assertEquals(0, periodOf(periods, LocalTime.MIDNIGHT, LocalTime.of(5, 59)));
    assertEquals(0, periodOf(periods, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT));
    assertEquals(6, periodOf(periods, LocalTime.MIDNIGHT, LocalTime.of(19, 0)));
    assertEquals(6, periodOf(periods, LocalTime.MIDNIGHT, LocalTime.MAX));

    var activitySimPeriods = settings.createDeparturePeriods(THREE_AM);
    assertEquals(7, activitySimPeriods.size());
    assertEquals(0, periodOf(activitySimPeriods, THREE_AM, THREE_AM));
    assertEquals(0, periodOf(activitySimPeriods, THREE_AM, LocalTime.of(5, 59)));
    assertEquals(1, periodOf(activitySimPeriods, THREE_AM, LocalTime.of(7, 45)));
    assertEquals(6, periodOf(activitySimPeriods, THREE_AM, LocalTime.of(20, 0)));
    assertEquals(6, periodOf(activitySimPeriods, THREE_AM, LocalTime.of(1, 30)));
    assertEquals(6, periodOf(activitySimPeriods, THREE_AM, LocalTime.of(2, 59, 59)));

    var bands = settings.getDistanceBands();
    assertEquals(7, bands.size());
    assertEquals(2, bands.binIndexOf(3.0));
    assertEquals(0, bands.binIndexOf(0.0));
    assertEquals(6, bands.binIndexOf(40.0));
    assertEquals(6, bands.binIndexOf(300.0));
  }

  /**
   * Departure period boundaries may be given in any order and are kept sorted on the clock. Midnight is a boundary
   * like any other, and a boundary on the day start adds nothing, the first period starting there anyway
   */
  @Test
  public void testDepartureBoundariesInAnyOrder() {
    var settings = new DiscreteDemandsDownSamplingSettings();
    settings.setDeparturePeriodBoundaries(LocalTime.of(9, 0), LocalTime.of(7, 0));
    assertEquals(List.of(LocalTime.of(7, 0), LocalTime.of(9, 0)), settings.getDeparturePeriodBoundaries());
    var periods = settings.createDeparturePeriods(LocalTime.MIDNIGHT);
    assertEquals(3, periods.size());
    assertEquals(1, periodOf(periods, LocalTime.MIDNIGHT, LocalTime.of(8, 0)));

    settings.setDeparturePeriodBoundaries(THREE_AM, LocalTime.of(7, 0));
    assertEquals(2, settings.createDeparturePeriods(THREE_AM).size());

    settings.setDeparturePeriodBoundaries(LocalTime.MIDNIGHT, LocalTime.of(7, 0));
    var nightSplit = settings.createDeparturePeriods(THREE_AM);
    assertEquals(3, nightSplit.size());
    assertEquals(1, periodOf(nightSplit, THREE_AM, LocalTime.of(23, 0)));
    assertEquals(2, periodOf(nightSplit, THREE_AM, LocalTime.of(1, 0)));
  }

  /**
   * Each time period's start and end become a boundary, a time where one ends and the next starts counted once, and a
   * period running past midnight ending on the clock of the next morning
   */
  @Test
  public void testDepartureBoundariesFromTimePeriods() {
    var settings = new DiscreteDemandsDownSamplingSettings();
    var peaks = new TimePeriods(IdGroupingToken.collectGlobalToken());
    peaks.getFactory().registerNew(LocalTime.of(16, 0).toSecondOfDay(), 7200);
    peaks.getFactory().registerNew(LocalTime.of(7, 0).toSecondOfDay(), 7200);
    settings.setDeparturePeriodBoundaries(peaks);
    assertEquals(List.of(LocalTime.of(7, 0), LocalTime.of(9, 0), LocalTime.of(16, 0), LocalTime.of(18, 0)),
        settings.getDeparturePeriodBoundaries());
    var periods = settings.createDeparturePeriods(LocalTime.MIDNIGHT);
    assertEquals(1, periodOf(periods, LocalTime.MIDNIGHT, LocalTime.of(8, 30)));
    assertEquals(2, periodOf(periods, LocalTime.MIDNIGHT, LocalTime.of(12, 0)));

    var adjoining = new TimePeriods(IdGroupingToken.collectGlobalToken());
    adjoining.getFactory().registerNew(LocalTime.of(7, 0).toSecondOfDay(), 7200);
    adjoining.getFactory().registerNew(LocalTime.of(9, 0).toSecondOfDay(), 7200);
    adjoining.getFactory().registerNew(LocalTime.of(22, 0).toSecondOfDay(), 4 * 3600);
    settings.setDeparturePeriodBoundaries(adjoining);
    assertEquals(List.of(LocalTime.of(2, 0), LocalTime.of(7, 0), LocalTime.of(9, 0), LocalTime.of(11, 0),
        LocalTime.of(22, 0)), settings.getDeparturePeriodBoundaries());
  }

  /** Each value that cannot be used is refused, and the setting keeps the value it had */
  @Test
  public void testRefusals() {
    var settings = new DiscreteDemandsDownSamplingSettings();

    assertThrows(IllegalArgumentException.class, () -> settings.setAlgorithmType(null));
    assertEquals(DiscreteDemandsDownSamplingAlgorithmType.BALANCED, settings.getAlgorithmType());
    for (double rate : new double[]{0, -0.1, 1.5}) {
      assertThrows(IllegalArgumentException.class, () -> settings.setDownSampleRate(rate));
      assertEquals(0.1, settings.getDownSampleRate());
    }
    assertThrows(IllegalArgumentException.class, () -> settings.setBalanceOrder(
        DownSamplingBalanceGroup.PERSON_TRIPS_BY_MODE, DownSamplingBalanceGroup.PERSON_TRIPS_BY_MODE));
    assertThrows(IllegalArgumentException.class, () -> settings.setBalanceOrder(
        DownSamplingBalanceGroup.PERSON_TRIPS_BY_MODE, null));
    assertEquals(List.of(DownSamplingBalanceGroup.values()), settings.getBalanceOrder());
    assertThrows(IllegalArgumentException.class,
        () -> settings.setDeparturePeriodBoundaries(LocalTime.of(7, 0), LocalTime.of(7, 0)));
    assertThrows(IllegalArgumentException.class,
        () -> settings.setDeparturePeriodBoundaries(LocalTime.of(7, 0), null));
    assertEquals(DEFAULT_PERIODS, settings.getDeparturePeriodBoundaries());
    assertThrows(IllegalArgumentException.class, () -> settings.setDistanceBandBoundariesKm(2, 1));
    assertThrows(IllegalArgumentException.class, () -> settings.setDistanceBandBoundariesKm(0, 1));
    assertArrayEquals(DEFAULT_BANDS_KM, settings.getDistanceBandBoundariesKm());
    assertEquals(7, settings.getDistanceBands().size());
    for (Consumer<int[]> setBoundaries : List.<Consumer<int[]>>of(settings::setAgeBandBoundaries,
        settings::setHouseholdSizeBandBoundaries, settings::setCarsPerHouseholdBandBoundaries,
        settings::setWorkersPerHouseholdBandBoundaries)) {
      assertThrows(IllegalArgumentException.class, () -> setBoundaries.accept(new int[]{18, 18}));
      assertThrows(IllegalArgumentException.class, () -> setBoundaries.accept(new int[]{0, 5}));
      assertThrows(IllegalArgumentException.class, () -> setBoundaries.accept(null));
    }
    assertEquals(DEFAULT_AGE_BANDS, settings.getAgeBands().getLabels());
    assertEquals(DEFAULT_HOUSEHOLD_SIZE_BANDS, settings.getHouseholdSizeBands().getLabels());
    assertEquals(DEFAULT_PER_HOUSEHOLD_BANDS, settings.getCarsPerHouseholdBands().getLabels());
    assertEquals(DEFAULT_PER_HOUSEHOLD_BANDS, settings.getWorkersPerHouseholdBands().getLabels());
    assertThrows(IllegalArgumentException.class, () -> settings.setNumberOfIncomeBands(0));
    assertEquals(5, settings.getNumberOfIncomeBands());
    assertThrows(IllegalArgumentException.class, () -> settings.setNumberOfDestinationAreas(0));
    assertEquals(12, settings.getNumberOfDestinationAreas());
    assertThrows(IllegalArgumentException.class, () -> settings.setMaximumNumberOfOriginAreas(-1));
    assertEquals(0, settings.getMaximumNumberOfOriginAreas());
    assertThrows(IllegalArgumentException.class, () -> settings.setNumberOfDraws(0));
    assertEquals(3, settings.getNumberOfDraws());
    assertThrows(IllegalArgumentException.class, () -> settings.setMinimumDrivingAge(-1));
    assertEquals(16, settings.getMinimumDrivingAge());
    int defaultThreads = GenericBatchExecutorService.getDefaultNumberOfThreads();
    assertThrows(IllegalArgumentException.class, () -> settings.setNumberOfThreads(0));
    assertThrows(IllegalArgumentException.class, () -> settings.setNumberOfThreads(defaultThreads + 1));
    assertEquals(defaultThreads, settings.getNumberOfThreads());

    /* the bounds themselves are allowed */
    settings.setDownSampleRate(1);
    assertEquals(1.0, settings.getDownSampleRate());
    settings.setMinimumDrivingAge(0);
    assertEquals(0, settings.getMinimumDrivingAge());
    settings.setNumberOfThreads(1);
    assertEquals(1, settings.getNumberOfThreads());
  }

  /** A copy can be changed without changing the original */
  @Test
  public void testShallowCloneApart() {
    var settings = new DiscreteDemandsDownSamplingSettings();
    var copy = settings.shallowClone();
    copy.setAlgorithmType(DiscreteDemandsDownSamplingAlgorithmType.BY_ZONE);
    copy.setDownSampleRate(0.5);
    copy.setBalanceOrder(DownSamplingBalanceGroup.PERSONS_PER_ZONE);
    copy.setDeparturePeriodBoundaries(LocalTime.of(7, 0));
    copy.setDistanceBandBoundariesKm(3);
    copy.setAgeBandBoundaries(18);
    copy.setHouseholdSizeBandBoundaries(1);
    copy.setCarsPerHouseholdBandBoundaries(1);
    copy.setWorkersPerHouseholdBandBoundaries(1);
    copy.setNumberOfIncomeBands(10);
    copy.setNumberOfDestinationAreas(4);
    copy.setMaximumNumberOfOriginAreas(100);
    copy.setMinimumDrivingAge(17);
    copy.setNumberOfDraws(5);
    copy.setSeed(7);
    copy.setStatisticsFile(Path.of("statistics.csv"));
    copy.setNumberOfThreads(1);

    assertEquals(DiscreteDemandsDownSamplingAlgorithmType.BALANCED, settings.getAlgorithmType());
    assertEquals(0.1, settings.getDownSampleRate());
    assertEquals(List.of(DownSamplingBalanceGroup.values()), settings.getBalanceOrder());
    assertEquals(DEFAULT_PERIODS, settings.getDeparturePeriodBoundaries());
    assertArrayEquals(DEFAULT_BANDS_KM, settings.getDistanceBandBoundariesKm());
    assertEquals(DEFAULT_AGE_BANDS, settings.getAgeBands().getLabels());
    assertEquals(DEFAULT_HOUSEHOLD_SIZE_BANDS, settings.getHouseholdSizeBands().getLabels());
    assertEquals(DEFAULT_PER_HOUSEHOLD_BANDS, settings.getCarsPerHouseholdBands().getLabels());
    assertEquals(DEFAULT_PER_HOUSEHOLD_BANDS, settings.getWorkersPerHouseholdBands().getLabels());
    assertEquals(5, settings.getNumberOfIncomeBands());
    assertEquals(2, copy.getAgeBands().size());
    assertEquals(2, copy.getHouseholdSizeBands().size());
    assertEquals(2, copy.getCarsPerHouseholdBands().size());
    assertEquals(2, copy.getWorkersPerHouseholdBands().size());
    assertEquals(10, copy.getNumberOfIncomeBands());
    assertEquals(12, settings.getNumberOfDestinationAreas());
    assertEquals(0, settings.getMaximumNumberOfOriginAreas());
    assertEquals(100, copy.getMaximumNumberOfOriginAreas());
    assertEquals(16, settings.getMinimumDrivingAge());
    assertEquals(3, settings.getNumberOfDraws());
    assertEquals(42, settings.getSeed());
    assertNull(settings.getStatisticsFile());
    assertEquals(GenericBatchExecutorService.getDefaultNumberOfThreads(), settings.getNumberOfThreads());
    assertEquals(7, settings.createDeparturePeriods(LocalTime.MIDNIGHT).size());
    assertEquals(7, settings.getDistanceBands().size());
    assertEquals(2, copy.createDeparturePeriods(LocalTime.MIDNIGHT).size());
    assertEquals(2, copy.getDistanceBands().size());
    assertEquals(List.of(DownSamplingBalanceGroup.PERSONS_PER_ZONE), copy.getBalanceOrder());
  }
}

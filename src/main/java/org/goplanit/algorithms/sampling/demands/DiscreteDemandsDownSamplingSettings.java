package org.goplanit.algorithms.sampling.demands;

import org.goplanit.algorithms.parallel.GenericBatchExecutorService;
import org.goplanit.algorithms.sampling.demands.enums.DiscreteDemandsDownSamplingAlgorithmType;
import org.goplanit.algorithms.sampling.demands.enums.DownSamplingBalanceGroup;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.misc.binning.BinBuilder;
import org.goplanit.utils.misc.binning.BinningConfiguration;
import org.goplanit.utils.time.LocalTimeUtils;
import org.goplanit.utils.time.TimePeriod;

import java.nio.file.Path;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Settings for down-sampling a discrete demand: the quality, the down-sample rate, the groups of totals balanced and
 * their order, the departure periods, distance bands and other bands those groups count in, and what the draw and its
 * statistics use. Each setter refuses a value that cannot be used with an {@link IllegalArgumentException} and keeps
 * the value it had
 * <p>
 * Terms used here, all listed in {@link org.goplanit.algorithms.sampling.demands}: the <b>quality</b> is how the
 * sampling units are chosen, from households at random to the best of several balanced draws; the <b>down-sample
 * rate</b> is the share of households kept; a <b>group of totals</b> is one way of counting the demand, such as person
 * trips by mode, that the balanced qualities match; a <b>draw</b> is one sample from one seed.
 * </p>
 * <p>
 * Every choice of the down-sampling is in one place, each with a default, so a caller sets only what it wants
 * different, in most runs only the rate. The settings hold the user's choices only: what comes from the demand, such
 * as the time its day starts, is given when the down-sampling runs.
 * </p>
 *
 * @author markr
 */
public class DiscreteDemandsDownSamplingSettings {

  /**
   * Distance bands from their boundaries, the first band starting at 0 km and the last holding everything further
   *
   * @param boundariesKm where one band ends and the next starts, in km
   * @return the bands
   * @throws PlanItRunTimeException when the boundaries are not above 0 km and strictly increasing
   */
  private static BinningConfiguration<Double> createDistanceBands(double[] boundariesKm) {
    var lowerBounds = new double[boundariesKm.length + 1];
    System.arraycopy(boundariesKm, 0, lowerBounds, 1, boundariesKm.length);
    return BinBuilder.ofInclusiveDoubleLowerBounds(lowerBounds);
  }

  /**
   * Bands of whole numbers, such as ages or the cars of a household, from their boundaries: the first band starts at 0,
   * each boundary is the first number of the next band, and the last band holds every number from its boundary up.
   * Boundaries 18 and 65 make the bands 0-17, 18-64 and 65 and over
   *
   * @param name of the bands, for a refusal
   * @param boundaries where one band ends and the next starts, above 0 and strictly increasing, none making one band
   * @return the bands, labelled such as 0-17 and &gt;=65
   * @throws IllegalArgumentException when the boundaries are null, not above 0 or not strictly increasing
   */
  private static BinningConfiguration<Integer> createCountBands(String name, int[] boundaries) {
    if (boundaries == null) {
      throw new IllegalArgumentException(String.format("%s boundaries must be given, were null", name));
    }
    var inclusiveUpperBounds = new int[boundaries.length + 1];
    for (int index = 0; index < boundaries.length; ++index) {
      inclusiveUpperBounds[index] = boundaries[index] - 1;
    }
    inclusiveUpperBounds[boundaries.length] = Integer.MAX_VALUE;
    try {
      return BinBuilder.ofInclusiveIntegerUpperBounds(0, inclusiveUpperBounds);
    } catch (PlanItRunTimeException e) {
      throw new IllegalArgumentException(String.format(
          "%s boundaries must be above 0 and strictly increasing, were %s", name, Arrays.toString(boundaries)), e);
    }
  }

  /** the quality of the down-sampling */
  private DiscreteDemandsDownSamplingAlgorithmType algorithmType = DEFAULT_ALGORITHM_TYPE;

  /** the share of households kept, on top of any sampling the demand already had */
  private double downSampleRate = DEFAULT_DOWN_SAMPLE_RATE;

  /** the groups of totals balanced, the most important first */
  private List<DownSamplingBalanceGroup> balanceOrder = DEFAULT_BALANCE_ORDER;

  /** clock times the departure periods change at, sorted on the clock */
  private List<LocalTime> departurePeriodBoundaries = DEFAULT_DEPARTURE_PERIOD_BOUNDARIES;

  /** where one distance band ends and the next starts, in km, strictly increasing */
  private double[] distanceBandBoundariesKm =
      DEFAULT_DISTANCE_BAND_BOUNDARIES_KM.stream().mapToDouble(Double::doubleValue).toArray();

  /** the distance bands of the boundaries, the first starting at 0 km */
  private BinningConfiguration<Double> distanceBands = createDistanceBands(distanceBandBoundariesKm);

  /** the age bands of the persons, in years, the first starting at 0 */
  private BinningConfiguration<Integer> ageBands =
      createCountBands("Age band", DEFAULT_AGE_BAND_BOUNDARIES.stream().mapToInt(Integer::intValue).toArray());

  /** the size bands of the households, in persons, the first starting at 0 */
  private BinningConfiguration<Integer> householdSizeBands = createCountBands("Household size band",
      DEFAULT_HOUSEHOLD_SIZE_BAND_BOUNDARIES.stream().mapToInt(Integer::intValue).toArray());

  /** the bands of the cars of a household, the first starting at 0 */
  private BinningConfiguration<Integer> carsPerHouseholdBands = createCountBands("Cars per household band",
      DEFAULT_CARS_PER_HOUSEHOLD_BAND_BOUNDARIES.stream().mapToInt(Integer::intValue).toArray());

  /** the bands of the workers of a household, the first starting at 0 */
  private BinningConfiguration<Integer> workersPerHouseholdBands = createCountBands("Workers per household band",
      DEFAULT_WORKERS_PER_HOUSEHOLD_BAND_BOUNDARIES.stream().mapToInt(Integer::intValue).toArray());

  /** number of income bands, each holding about as many of the known incomes */
  private int numberOfIncomeBands = DEFAULT_NUMBER_OF_INCOME_BANDS;

  /** number of areas the tour destinations are counted in */
  private int numberOfDestinationAreas = DEFAULT_NUMBER_OF_DESTINATION_AREAS;

  /** most areas the car driven trips are counted in by origin, 0 for none */
  private int maximumNumberOfOriginAreas = DEFAULT_MAXIMUM_NUMBER_OF_ORIGIN_AREAS;

  /** age from which a person not known to lack a licence may drive */
  private int minimumDrivingAge = DEFAULT_MINIMUM_DRIVING_AGE;

  /** number of balanced draws the best of several picks from */
  private int numberOfDraws = DEFAULT_NUMBER_OF_DRAWS;

  /** seed of the draw */
  private long seed = DEFAULT_SEED;

  /** file the statistics are written to, null when they are only logged */
  private Path statisticsFile;

  /** threads the draw runs on */
  private int numberOfThreads = GenericBatchExecutorService.getDefaultNumberOfThreads();

  /**
   * Copy constructor
   *
   * @param other to copy
   */
  private DiscreteDemandsDownSamplingSettings(DiscreteDemandsDownSamplingSettings other) {
    this.algorithmType = other.algorithmType;
    this.downSampleRate = other.downSampleRate;
    this.balanceOrder = other.balanceOrder;
    this.departurePeriodBoundaries = other.departurePeriodBoundaries;
    this.distanceBandBoundariesKm = other.distanceBandBoundariesKm;
    this.distanceBands = other.distanceBands;
    this.ageBands = other.ageBands;
    this.householdSizeBands = other.householdSizeBands;
    this.carsPerHouseholdBands = other.carsPerHouseholdBands;
    this.workersPerHouseholdBands = other.workersPerHouseholdBands;
    this.numberOfIncomeBands = other.numberOfIncomeBands;
    this.numberOfDestinationAreas = other.numberOfDestinationAreas;
    this.maximumNumberOfOriginAreas = other.maximumNumberOfOriginAreas;
    this.minimumDrivingAge = other.minimumDrivingAge;
    this.numberOfDraws = other.numberOfDraws;
    this.seed = other.seed;
    this.statisticsFile = other.statisticsFile;
    this.numberOfThreads = other.numberOfThreads;
  }

  /** the best match within a few minutes */
  public static final DiscreteDemandsDownSamplingAlgorithmType DEFAULT_ALGORITHM_TYPE =
      DiscreteDemandsDownSamplingAlgorithmType.BALANCED;

  /** one household in ten, the sample transport models most often run */
  public static final double DEFAULT_DOWN_SAMPLE_RATE = 0.1;

  /**
   * every group, in the order they are declared: trips first, as they are what loads the network, by mode, how far,
   * when and where to; then tours; then households and persons, which reach a model run only through the trips they
   * already make
   */
  public static final List<DownSamplingBalanceGroup> DEFAULT_BALANCE_ORDER = List.of(DownSamplingBalanceGroup.values());

  /**
   * two-hour periods through the morning and evening peaks, where congestion builds, and coarse ones elsewhere, all on
   * the hour so that keeping a time within its half hour never moves a trip across one
   */
  public static final List<LocalTime> DEFAULT_DEPARTURE_PERIOD_BOUNDARIES = List.of(LocalTime.of(6, 0),
      LocalTime.of(8, 0), LocalTime.of(10, 0), LocalTime.of(15, 0), LocalTime.of(17, 0), LocalTime.of(19, 0));

  /** short walks and cycles apart from local car trips, and the long commutes in two bands of their own */
  public static final List<Double> DEFAULT_DISTANCE_BAND_BOUNDARIES_KM = List.of(1.0, 2.0, 5.0, 10.0, 20.0, 40.0);

  /**
   * under school age, primary and secondary school ages, young adults, the working ages in two bands, and the young old
   * and the old: 0-4, 5-11, 12-17, 18-24, 25-44, 45-64, 65-79 and 80 and over
   */
  public static final List<Integer> DEFAULT_AGE_BAND_BOUNDARIES = List.of(5, 12, 18, 25, 45, 65, 80);

  /** each size of household up to 4 persons a band of its own, and 5 or more together, as larger ones are few */
  public static final List<Integer> DEFAULT_HOUSEHOLD_SIZE_BAND_BOUNDARIES = List.of(1, 2, 3, 4, 5);

  /** 0, 1 and 2 cars each a band of their own, and 3 or more together, as households with more are few */
  public static final List<Integer> DEFAULT_CARS_PER_HOUSEHOLD_BAND_BOUNDARIES = List.of(1, 2, 3);

  /** 0, 1 and 2 workers each a band of their own, and 3 or more together, as households with more are few */
  public static final List<Integer> DEFAULT_WORKERS_PER_HOUSEHOLD_BAND_BOUNDARIES = List.of(1, 2, 3);

  /** the fifths of the known incomes */
  public static final int DEFAULT_NUMBER_OF_INCOME_BANDS = 5;

  /** enough to tell the regions of a metropolitan area apart, few enough to stay cheap */
  public static final int DEFAULT_NUMBER_OF_DESTINATION_AREAS = 12;

  /** none, so the car driven trips are matched over all zones only, as without origin areas */
  public static final int DEFAULT_MAXIMUM_NUMBER_OF_ORIGIN_AREAS = 0;

  /** the age from which a learner may drive in most places */
  public static final int DEFAULT_MINIMUM_DRIVING_AGE = 16;

  /** the best of three, within minutes */
  public static final int DEFAULT_NUMBER_OF_DRAWS = 3;

  /** a fixed seed, so a run gives the same sample unless asked otherwise */
  public static final long DEFAULT_SEED = 42;

  /** Settings with every default */
  public DiscreteDemandsDownSamplingSettings() {
  }

  /**
   * A copy that can be changed without changing these settings
   *
   * @return the copy
   */
  public DiscreteDemandsDownSamplingSettings shallowClone() {
    return new DiscreteDemandsDownSamplingSettings(this);
  }

  /**
   * The quality of the down-sampling
   *
   * @return the quality
   */
  public DiscreteDemandsDownSamplingAlgorithmType getAlgorithmType() {
    return algorithmType;
  }

  /**
   * Set the quality of the down-sampling
   *
   * @param algorithmType to use, not null
   */
  public void setAlgorithmType(DiscreteDemandsDownSamplingAlgorithmType algorithmType) {
    if (algorithmType == null) {
      throw new IllegalArgumentException("Down-sampling algorithm type must be given, was null");
    }
    this.algorithmType = algorithmType;
  }

  /**
   * The down-sample rate: the share of households kept, on top of any sampling the demand already had. 0.1 keeps one
   * household in ten, so a demand that is itself a 10% sample keeps 1% of the population
   *
   * @return the rate
   */
  public double getDownSampleRate() {
    return downSampleRate;
  }

  /**
   * Set the down-sample rate: the share of households kept, on top of any sampling the demand already had
   *
   * @param downSampleRate above 0 and at most 1, 1 leaving the demand as it is
   */
  public void setDownSampleRate(double downSampleRate) {
    if (!(downSampleRate > 0 && downSampleRate <= 1)) {
      throw new IllegalArgumentException(String.format(
          "Down-sample rate must be above 0 and at most 1, was %f", downSampleRate));
    }
    this.downSampleRate = downSampleRate;
  }

  /**
   * The groups of totals balanced, the most important first
   *
   * @return the groups, unmodifiable
   */
  public List<DownSamplingBalanceGroup> getBalanceOrder() {
    return balanceOrder;
  }

  /**
   * Set the groups of totals balanced, the most important first. A group left out is not balanced, though the
   * statistics still report it
   *
   * @param groups each named once, none null, none at all balancing nothing
   */
  public void setBalanceOrder(DownSamplingBalanceGroup... groups) {
    var named = EnumSet.noneOf(DownSamplingBalanceGroup.class);
    for (var group : groups == null ? new DownSamplingBalanceGroup[]{null} : groups) {
      if (group == null || !named.add(group)) {
        throw new IllegalArgumentException(String.format(
            "Balance order must name each group once, was %s", Arrays.toString(groups)));
      }
    }
    this.balanceOrder = List.of(groups);
  }

  /**
   * The clock times the departure periods change at
   *
   * @return the boundaries sorted on the clock, unmodifiable
   */
  public List<LocalTime> getDeparturePeriodBoundaries() {
    return departurePeriodBoundaries;
  }

  /**
   * Set the clock times the departure periods change at, in any order. The periods run from the demand's day start,
   * see {@link #createDeparturePeriods(LocalTime)}, so midnight is a boundary like any other
   *
   * @param boundaries each given once, none null, none at all making one period of the whole day
   */
  public void setDeparturePeriodBoundaries(LocalTime... boundaries) {
    var sortedBoundaries = new TreeSet<LocalTime>();
    for (var boundary : boundaries == null ? new LocalTime[]{null} : boundaries) {
      if (boundary == null || !sortedBoundaries.add(boundary)) {
        throw new IllegalArgumentException(String.format(
            "Departure period boundaries must each be given once, were %s", Arrays.toString(boundaries)));
      }
    }
    this.departurePeriodBoundaries = List.copyOf(sortedBoundaries);
  }

  /**
   * Set the departure period boundaries from time periods: each period's start and end become a boundary, a time
   * where one period ends and the next starts counted once, so each time period is a departure period of its own
   * <p>
   * A demand matched on its departure periods keeps its trips per period, so periods that are those of the
   * assignment keep each assignment period's load. A gap between two time periods is a departure period too, as the
   * periods cover the whole day.
   * </p>
   *
   * @param timePeriods to take the boundaries from, not null, a period past midnight ending on the next morning's
   *                    clock
   */
  public void setDeparturePeriodBoundaries(Iterable<? extends TimePeriod> timePeriods) {
    if (timePeriods == null) {
      throw new IllegalArgumentException("Time periods for the departure period boundaries must be given, was null");
    }
    var sortedBoundaries = new TreeSet<LocalTime>();
    for (var timePeriod : timePeriods) {
      sortedBoundaries.add(LocalTimeUtils.ofSecondOfDayWrapped(timePeriod.getStartTimeSeconds()));
      sortedBoundaries.add(LocalTimeUtils.ofSecondOfDayWrapped(timePeriod.getEndTimeSeconds()));
    }
    this.departurePeriodBoundaries = List.copyOf(sortedBoundaries);
  }

  /**
   * The departure periods of a day starting at the given time, binned by the seconds since the day start, so a trip's
   * period is the bin of {@code LocalTimeUtils#secondsFromWrapAroundDayAnchor(dayStart, trip.getStartTime())}
   * <p>
   * Times are kept on the clock, and a demand's day need not start at midnight: a day from 03:00 to 03:00 holds its
   * trips at 01:30 as the next morning. Counting from the day start keeps them at the end of the day, so with a day
   * starting at 03:00 the night from 19:00 to 03:00 is one period rather than split at midnight. A boundary on the day
   * start adds nothing, the first period starting there anyway.
   * </p>
   *
   * @param dayStart the time of day the demand's day starts at
   * @return the periods, the first from the day start to the first boundary after it, the last from the last boundary
   *         to the day's end, each labelled by its clock times, such as 19:00:00-03:00:00
   */
  public BinningConfiguration<Integer> createDeparturePeriods(LocalTime dayStart) {
    var lowerBounds = IntStream.concat(IntStream.of(0), departurePeriodBoundaries.stream()
        .mapToInt(boundary -> (int) LocalTimeUtils.secondsFromWrapAroundDayAnchor(dayStart, boundary))
        .filter(seconds -> seconds > 0)).sorted().toArray();
    return BinningConfiguration.of(BinBuilder.ofInclusiveIntegerLowerBounds(lowerBounds).getBins().stream()
        .map(period -> period.withLabel(LocalTimeUtils.formatHhMmSs(
            dayStart.plusSeconds(period.getLowerBound()).toSecondOfDay()) + "-" + LocalTimeUtils.formatHhMmSs(
            (period.getUpperBound() == Integer.MAX_VALUE ? dayStart : dayStart.plusSeconds(period.getUpperBound()))
                .toSecondOfDay())))
        .collect(Collectors.toList()));
  }

  /**
   * Where one distance band ends and the next starts
   *
   * @return a copy of the boundaries in km, strictly increasing
   */
  public double[] getDistanceBandBoundariesKm() {
    return distanceBandBoundariesKm.clone();
  }

  /**
   * Set where one distance band ends and the next starts. The first band starts at 0 km, the last holds everything
   * further
   *
   * @param boundariesKm in km, above 0 and strictly increasing, none at all making one band of every distance
   */
  public void setDistanceBandBoundariesKm(double... boundariesKm) {
    if (boundariesKm == null) {
      throw new IllegalArgumentException("Distance band boundaries must be given, were null");
    }
    var boundaries = boundariesKm.clone();
    try {
      this.distanceBands = createDistanceBands(boundaries);
    } catch (PlanItRunTimeException e) {
      throw new IllegalArgumentException(String.format(
          "Distance band boundaries must be above 0 km and strictly increasing, were %s",
          Arrays.toString(boundariesKm)), e);
    }
    this.distanceBandBoundariesKm = boundaries;
  }

  /**
   * The distance bands, binned by km, the first starting at 0 km and the last holding everything further
   *
   * @return the bands
   */
  public BinningConfiguration<Double> getDistanceBands() {
    return distanceBands;
  }

  /**
   * The age bands of the persons, binned by years, the first starting at 0 and the last holding every age above
   *
   * @return the bands, labelled such as 0-4 and &gt;=80
   */
  public BinningConfiguration<Integer> getAgeBands() {
    return ageBands;
  }

  /**
   * Set where one age band ends and the next starts: the first band starts at 0 years, each boundary is the first age
   * of the next band, and the last band holds every age from its boundary up. 18 and 65 make 0-17, 18-64 and 65 and
   * over
   *
   * @param boundaries ages in years, above 0 and strictly increasing, none at all making one band of every age
   */
  public void setAgeBandBoundaries(int... boundaries) {
    this.ageBands = createCountBands("Age band", boundaries);
  }

  /**
   * The size bands of the households, binned by persons, the first starting at 0 and the last holding every size above
   *
   * @return the bands, labelled such as 0 and &gt;=5
   */
  public BinningConfiguration<Integer> getHouseholdSizeBands() {
    return householdSizeBands;
  }

  /**
   * Set where one household size band ends and the next starts: the first band starts at 0 persons, each boundary is
   * the first size of the next band, and the last band holds every size from its boundary up. 1 and 3 make 0, 1-2 and
   * 3 or more persons
   *
   * @param boundaries numbers of persons, above 0 and strictly increasing, none at all making one band of every size
   */
  public void setHouseholdSizeBandBoundaries(int... boundaries) {
    this.householdSizeBands = createCountBands("Household size band", boundaries);
  }

  /**
   * The bands of the cars of a household, the first starting at 0 cars and the last holding every number above
   *
   * @return the bands, labelled such as 0 and &gt;=3
   */
  public BinningConfiguration<Integer> getCarsPerHouseholdBands() {
    return carsPerHouseholdBands;
  }

  /**
   * Set where one band of the cars of a household ends and the next starts: the first band starts at 0 cars, each
   * boundary is the first number of the next band, and the last band holds every number from its boundary up. 1 makes
   * no car and 1 or more cars
   *
   * @param boundaries numbers of cars, above 0 and strictly increasing, none at all making one band of every number
   */
  public void setCarsPerHouseholdBandBoundaries(int... boundaries) {
    this.carsPerHouseholdBands = createCountBands("Cars per household band", boundaries);
  }

  /**
   * The bands of the workers of a household, persons known to work full or part time, the first starting at 0 and the
   * last holding every number above
   *
   * @return the bands, labelled such as 0 and &gt;=3
   */
  public BinningConfiguration<Integer> getWorkersPerHouseholdBands() {
    return workersPerHouseholdBands;
  }

  /**
   * Set where one band of the workers of a household ends and the next starts: the first band starts at 0 workers, each
   * boundary is the first number of the next band, and the last band holds every number from its boundary up. 2 makes
   * 0-1 and 2 or more workers
   *
   * @param boundaries numbers of workers, above 0 and strictly increasing, none at all making one band of every number
   */
  public void setWorkersPerHouseholdBandBoundaries(int... boundaries) {
    this.workersPerHouseholdBands = createCountBands("Workers per household band", boundaries);
  }

  /**
   * The number of income bands, each holding about as many of the full demand's known incomes
   *
   * @return the number
   */
  public int getNumberOfIncomeBands() {
    return numberOfIncomeBands;
  }

  /**
   * Set the number of income bands, each holding about as many of the full demand's known incomes: 5 makes the fifths,
   * 10 the tenths
   *
   * @param numberOfIncomeBands 1 or more
   */
  public void setNumberOfIncomeBands(int numberOfIncomeBands) {
    if (numberOfIncomeBands < 1) {
      throw new IllegalArgumentException(String.format(
          "Number of income bands must be 1 or more, was %d", numberOfIncomeBands));
    }
    this.numberOfIncomeBands = numberOfIncomeBands;
  }

  /**
   * The number of areas the tour destinations are counted in
   *
   * @return the number
   */
  public int getNumberOfDestinationAreas() {
    return numberOfDestinationAreas;
  }

  /**
   * Set the number of areas the tour destinations are counted in
   *
   * @param numberOfDestinationAreas 1 or more
   */
  public void setNumberOfDestinationAreas(int numberOfDestinationAreas) {
    if (numberOfDestinationAreas < 1) {
      throw new IllegalArgumentException(String.format(
          "Number of destination areas must be 1 or more, was %d", numberOfDestinationAreas));
    }
    this.numberOfDestinationAreas = numberOfDestinationAreas;
  }

  /**
   * The most areas the car driven trips are counted in by the zone they leave from
   *
   * @return the number, 0 for none
   */
  public int getMaximumNumberOfOriginAreas() {
    return maximumNumberOfOriginAreas;
  }

  /**
   * Set the most areas the car driven trips are counted in by the zone they leave from. There are fewer when the
   * households kept are too few for so many, each area needing 4 sampling units kept per total balanced over all
   * zones, and none below 2, as one area would only repeat the car driven trips of all zones
   *
   * @param maximumNumberOfOriginAreas 0 or more, 0 for none
   */
  public void setMaximumNumberOfOriginAreas(int maximumNumberOfOriginAreas) {
    if (maximumNumberOfOriginAreas < 0) {
      throw new IllegalArgumentException(String.format(
          "Maximum number of origin areas must be 0 or more, was %d", maximumNumberOfOriginAreas));
    }
    this.maximumNumberOfOriginAreas = maximumNumberOfOriginAreas;
  }

  /**
   * The age from which a person not known to lack a licence may drive, for the households by car availability
   *
   * @return the age in years
   */
  public int getMinimumDrivingAge() {
    return minimumDrivingAge;
  }

  /**
   * Set the age from which a person not known to lack a licence may drive, for the households by car availability
   *
   * @param minimumDrivingAge in years, 0 or more
   */
  public void setMinimumDrivingAge(int minimumDrivingAge) {
    if (minimumDrivingAge < 0) {
      throw new IllegalArgumentException(String.format(
          "Minimum driving age must be 0 or more, was %d", minimumDrivingAge));
    }
    this.minimumDrivingAge = minimumDrivingAge;
  }

  /**
   * The number of balanced draws the best of several picks from
   *
   * @return the number
   */
  public int getNumberOfDraws() {
    return numberOfDraws;
  }

  /**
   * Set the number of balanced draws the best of several picks from
   *
   * @param numberOfDraws 1 or more
   */
  public void setNumberOfDraws(int numberOfDraws) {
    if (numberOfDraws < 1) {
      throw new IllegalArgumentException(String.format("Number of draws must be 1 or more, was %d", numberOfDraws));
    }
    this.numberOfDraws = numberOfDraws;
  }

  /**
   * The seed of the draw. The same demand, settings and seed always give the same sample
   *
   * @return the seed
   */
  public long getSeed() {
    return seed;
  }

  /**
   * Set the seed of the draw
   *
   * @param seed any value
   */
  public void setSeed(long seed) {
    this.seed = seed;
  }

  /**
   * The file the statistics are written to
   *
   * @return the file, null when the statistics are only logged
   */
  public Path getStatisticsFile() {
    return statisticsFile;
  }

  /**
   * Set the file the statistics are written to, besides being logged
   *
   * @param statisticsFile to write to, null to only log them
   */
  public void setStatisticsFile(Path statisticsFile) {
    this.statisticsFile = statisticsFile;
  }

  /**
   * The number of threads the draw runs on
   *
   * @return the number
   */
  public int getNumberOfThreads() {
    return numberOfThreads;
  }

  /**
   * Set the number of threads the draw runs on. The sample is the same on any number
   *
   * @param numberOfThreads from 1 up to {@link GenericBatchExecutorService#getDefaultNumberOfThreads()}
   */
  public void setNumberOfThreads(int numberOfThreads) {
    GenericBatchExecutorService.verifyNumberOfThreads(numberOfThreads);
    this.numberOfThreads = numberOfThreads;
  }
}

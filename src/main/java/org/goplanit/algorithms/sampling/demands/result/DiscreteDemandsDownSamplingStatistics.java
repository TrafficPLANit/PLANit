package org.goplanit.algorithms.sampling.demands.result;

import org.goplanit.algorithms.sampling.demands.enums.DownSamplingBalanceGroup;
import org.goplanit.algorithms.sampling.demands.enums.DownSamplingCheckedTable;
import org.goplanit.algorithms.sampling.demands.enums.DownSamplingZoneMeasure;
import org.goplanit.algorithms.sampling.demands.table.DiscreteDemandsDownSamplingTable;
import org.goplanit.utils.csv.SimpleCsvWriter;
import org.goplanit.utils.misc.StringUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * How well a down-sampled demand stands for its full demand: every total of every group of totals, every cell of the
 * checked tables and every zone's households, persons and car driven trips by origin, each against its target and the
 * spread a random draw of households would give, with a summary of each and the score that picks the best of several
 * draws
 * <p>
 * A user judges a sample before running a model on it. A total's target is what the draw aimed at: each sampling
 * unit's chance in the draw times its value, summed, so the zones' deliberate rounding of their numbers kept is not
 * counted as error. The random spread is that of a draw of households at random at the rate, the square root of the
 * sum of the rate times one less the rate times each sampling unit's value squared, so a ratio of the difference to it
 * well below 1 says the sample is better than a random draw would mostly be. A group balanced on is expected to sit far
 * below 1; a checked table, never balanced, shows whether matching the groups pulled what they do not hold off.
 * </p>
 * <p>
 * Terms used here, all listed in {@link org.goplanit.algorithms.sampling.demands}: a <b>group of totals</b> is one way
 * of counting the demand, such as person trips by mode; a <b>checked table</b> is a finer count, such as person trips
 * by mode and hour; the <b>error</b> of a group is how far its totals are off together, see
 * {@link DownSamplingTotalsSummary}; the <b>score</b> is the checked tables' error over the error a random draw of
 * households would give, lower being better.
 * </p>
 * <p>
 * The statistics are computed from the down-sampling table and the draw alone, never the demand's objects, so several
 * draws from one down-sampling table each have their own.
 * </p>
 *
 * @author markr
 */
public class DiscreteDemandsDownSamplingStatistics {

  /**
   * The sums over the sampling units that make the totals of a set of columns: the full value, the target, the sample
   * and the square of the random spread
   */
  private static final class Sums {

    /** per column, the full demand's total */
    private final double[] full;

    /** per column, the sum of each sampling unit's chance times its value */
    private final double[] target;

    /** per column, the sum of the values of the sampling units kept */
    private final double[] sample;

    /** per column, the sum of the rate times one less the rate times each value squared */
    private final double[] spreadSquared;

    /**
     * Constructor
     *
     * @param columns number of columns
     */
    private Sums(int columns) {
      full = new double[columns];
      target = new double[columns];
      sample = new double[columns];
      spreadSquared = new double[columns];
    }

    /**
     * Add a sampling unit's values
     *
     * @param values of the sampling unit, by column
     * @param chance of the sampling unit in the draw
     * @param randomShare the rate times one less the rate
     * @param kept true when the draw kept the sampling unit
     */
    private void add(double[] values, double chance, double randomShare, boolean kept) {
      for (int column = 0; column < values.length; ++column) {
        double value = values[column];
        if (value != 0) {
          full[column] += value;
          target[column] += chance * value;
          spreadSquared[column] += randomShare * value * value;
          if (kept) {
            sample[column] += value;
          }
        }
      }
    }

    /**
     * The totals of a run of columns, in the unit of what they count
     *
     * @param firstColumn of the run
     * @param labels of the columns of the run, in order
     * @param unitsPerValue how much of that unit one value added is, such as a thousandth of a kilometre for a metre
     * @return the totals
     */
    private List<DownSamplingTotal> createTotals(int firstColumn, List<String> labels, double unitsPerValue) {
      var totals = new ArrayList<DownSamplingTotal>(labels.size());
      for (int index = 0; index < labels.size(); ++index) {
        int column = firstColumn + index;
        totals.add(new DownSamplingTotal(labels.get(index), unitsPerValue * full[column],
            unitsPerValue * target[column], unitsPerValue * sample[column],
            unitsPerValue * Math.sqrt(spreadSquared[column])));
      }
      return totals;
    }
  }

  /** the logger */
  private static final Logger LOGGER =
      Logger.getLogger(DiscreteDemandsDownSamplingStatistics.class.getCanonicalName());

  /** the columns of the statistics file */
  private static final String[] CSV_HEADERS =
      {"group", "category", "full", "target", "sample", "difference", "ratio_to_random_spread"};

  /**
   * The totals of the zones whose full value is above 0, labelled by the zone's XML id or else its id
   *
   * @param sums of the zones, by zone position along the curve
   * @param table the zones come from
   * @return the totals
   */
  private static List<DownSamplingTotal> createZoneTotals(Sums sums, DiscreteDemandsDownSamplingTable table) {
    var zones = table.getZonesAlongCurve();
    var labels = zones.stream()
        .map(zone -> zone.getXmlId() != null ? zone.getXmlId() : String.valueOf(zone.getId()))
        .collect(Collectors.toList());
    return sums.createTotals(0, labels, 1).stream().filter(total -> total.getFull() > 0).collect(Collectors.toList());
  }

  /** the totals of each group */
  private final Map<DownSamplingBalanceGroup, List<DownSamplingTotal>> totalsByGroup =
      new EnumMap<>(DownSamplingBalanceGroup.class);

  /** the cells of each checked table */
  private final Map<DownSamplingCheckedTable, List<DownSamplingTotal>> cellsByCheckedTable =
      new EnumMap<>(DownSamplingCheckedTable.class);

  /** the zones of each measure, those with a full value above 0 */
  private final Map<DownSamplingZoneMeasure, List<DownSamplingTotal>> zonesByMeasure =
      new EnumMap<>(DownSamplingZoneMeasure.class);

  /**
   * The score of a draw over its checked tables: the mean of each checked table's error divided by the error a random
   * draw would give, so 1 is as good as a draw of households at random and 0 a perfect match. A checked table without a
   * random error, having no cells, is left out
   *
   * @param checkedTables the summaries of the checked tables
   * @return the score, 0 when no checked table has a random error
   */
  public static double calculateScore(Collection<DownSamplingTotalsSummary> checkedTables) {
    return checkedTables.stream().filter(summary -> summary.getRandomError() > 0)
        .mapToDouble(summary -> summary.getError() / summary.getRandomError()).average().orElse(0);
  }

  /**
   * Compute the statistics of a draw, in one pass over the down-sampling table's sampling units
   * <p>
   * Each sampling unit adds its values, its chance times its values, the rate times one less the rate times its values
   * squared, and, when kept, its values again, to the sums of each column of the groups, each cell of the checked
   * tables and each zone. A household of 3 persons in a zone kept with chance 0.1 adds 3 to the zone's full persons,
   * 0.3 to its target and 0.1 times 0.9 times 9 to its random spread squared, and 3 to its sample when kept. The
   * totals then follow from the sums alone.
   * </p>
   *
   * @param table drawn from
   * @param draw the sampling units kept and each stratum's chance
   * @param rate the down-sample rate, for the random spread
   */
  public DiscreteDemandsDownSamplingStatistics(
      DiscreteDemandsDownSamplingTable table, DownSamplingDraw draw, double rate) {
    double randomShare = rate * (1 - rate);
    var groupSums = new Sums(table.getNumberOfColumns());
    var row = new double[table.getNumberOfColumns()];
    var checkedTables = DownSamplingCheckedTable.values();
    var checkedSums = new Sums[checkedTables.length];
    var cells = new double[checkedTables.length][];
    for (var checkedTable : checkedTables) {
      int numberOfCells = table.getCategories(checkedTable).size();
      checkedSums[checkedTable.ordinal()] = new Sums(numberOfCells);
      cells[checkedTable.ordinal()] = new double[numberOfCells];
    }
    int numberOfZones = table.getZonesAlongCurve().size();
    var householdSums = new Sums(numberOfZones);
    var personSums = new Sums(numberOfZones);
    var carDrivenSums = new Sums(numberOfZones);
    var householdsByZone = new double[numberOfZones];
    var personsByZone = new double[numberOfZones];
    var carDrivenByZone = new double[numberOfZones];

    for (int unit = 0; unit < table.size(); ++unit) {
      int stratum = table.getStratum(unit);
      double chance = draw.getChance(stratum);
      boolean kept = draw.getKept().get(unit);
      table.collectValues(unit, row);
      groupSums.add(row, chance, randomShare, kept);
      for (var checkedTable : checkedTables) {
        table.collectValues(unit, checkedTable, cells[checkedTable.ordinal()]);
        checkedSums[checkedTable.ordinal()].add(cells[checkedTable.ordinal()], chance, randomShare, kept);
      }
      if (stratum < numberOfZones) {
        householdsByZone[stratum] = table.getHouseholdIds(unit).length;
        personsByZone[stratum] = table.getPersons(unit);
        householdSums.add(householdsByZone, chance, randomShare, kept);
        personSums.add(personsByZone, chance, randomShare, kept);
        householdsByZone[stratum] = 0;
        personsByZone[stratum] = 0;
      }
      table.collectCarDrivenTripsByOrigin(unit, carDrivenByZone);
      carDrivenSums.add(carDrivenByZone, chance, randomShare, kept);
    }

    for (var group : DownSamplingBalanceGroup.values()) {
      totalsByGroup.put(group, groupSums.createTotals(
          table.getFirstColumn(group), table.getCategories(group), table.getGroupUnitsPerStoredValue(group)));
    }
    for (var checkedTable : checkedTables) {
      cellsByCheckedTable.put(checkedTable,
          checkedSums[checkedTable.ordinal()].createTotals(0, table.getCategories(checkedTable), 1));
    }
    zonesByMeasure.put(DownSamplingZoneMeasure.HOUSEHOLDS, createZoneTotals(householdSums, table));
    zonesByMeasure.put(DownSamplingZoneMeasure.PERSONS, createZoneTotals(personSums, table));
    zonesByMeasure.put(DownSamplingZoneMeasure.CAR_DRIVEN_TRIPS_BY_ORIGIN_ZONE, createZoneTotals(carDrivenSums, table));
  }

  /**
   * The totals of a group, balanced on or not, in the order of its categories
   *
   * @param group of the totals
   * @return the totals, unmodifiable
   */
  public List<DownSamplingTotal> getTotals(DownSamplingBalanceGroup group) {
    return List.copyOf(totalsByGroup.get(group));
  }

  /**
   * The summary of a group's totals
   *
   * @param group to summarise
   * @return the summary
   */
  public DownSamplingTotalsSummary getSummary(DownSamplingBalanceGroup group) {
    return new DownSamplingTotalsSummary(totalsByGroup.get(group));
  }

  /**
   * The cells of a checked table, in the order of its cells
   *
   * @param checkedTable of the cells
   * @return the cells, unmodifiable
   */
  public List<DownSamplingTotal> getCells(DownSamplingCheckedTable checkedTable) {
    return List.copyOf(cellsByCheckedTable.get(checkedTable));
  }

  /**
   * The summary of a checked table's cells
   *
   * @param checkedTable to summarise
   * @return the summary
   */
  public DownSamplingTotalsSummary getSummary(DownSamplingCheckedTable checkedTable) {
    return new DownSamplingTotalsSummary(cellsByCheckedTable.get(checkedTable));
  }

  /**
   * The zones of a measure whose full value is above 0, in their order along the curve
   *
   * @param measure of the zones
   * @return the zones' totals, unmodifiable
   */
  public List<DownSamplingTotal> getZones(DownSamplingZoneMeasure measure) {
    return List.copyOf(zonesByMeasure.get(measure));
  }

  /**
   * The summary of a measure's zones
   *
   * @param measure to summarise
   * @return the summary
   */
  public DownSamplingTotalsSummary getSummary(DownSamplingZoneMeasure measure) {
    return new DownSamplingTotalsSummary(zonesByMeasure.get(measure));
  }

  /**
   * The score of the draw over its checked tables, see {@link #calculateScore(Collection)}
   *
   * @return the score
   */
  public double getScore() {
    var checkedTables = new ArrayList<DownSamplingTotalsSummary>();
    for (var checkedTable : DownSamplingCheckedTable.values()) {
      checkedTables.add(getSummary(checkedTable));
    }
    return calculateScore(checkedTables);
  }

  /**
   * Log the statistics: a line per group with totals and per checked table, one for the zones, and the score. A group
   * without totals, such as the origin areas when there are none, adds no line
   */
  public void log() {
    for (var group : DownSamplingBalanceGroup.values()) {
      var summary = getSummary(group);
      if (summary.getNumberOfTotals() > 0) {
        LOGGER.info(summary.createLogLine(StringUtils.toSentence(group)));
      }
    }
    for (var checkedTable : DownSamplingCheckedTable.values()) {
      LOGGER.info(getSummary(checkedTable).createLogLine(StringUtils.toSentence(checkedTable) + ", checked"));
    }
    var households = getSummary(DownSamplingZoneMeasure.HOUSEHOLDS);
    var persons = getSummary(DownSamplingZoneMeasure.PERSONS);
    var carDriven = getSummary(DownSamplingZoneMeasure.CAR_DRIVEN_TRIPS_BY_ORIGIN_ZONE);
    LOGGER.info(String.format("Zones: households error %.4f against %.4f for households at random; persons error %.4f"
            + " against %.4f, %.0f%% of zones within one and %.0f%% within two; car driven trips by origin zone error"
            + " %.4f against %.4f", households.getError(), households.getRandomError(), persons.getError(),
        persons.getRandomError(), 100 * persons.getShareWithin(1), 100 * persons.getShareWithin(2),
        carDriven.getError(), carDriven.getRandomError()));
    LOGGER.info(String.format(
        "Score over the checked tables %.3f, 1 being as good as households at random", getScore()));
  }

  /**
   * Write every total of every group and every cell of every checked table to a CSV file, a row each
   *
   * @param file to write, replaced when it exists
   */
  public void writeCsv(Path file) {
    try (var writer = SimpleCsvWriter.create(file, CSV_HEADERS)) {
      for (var group : DownSamplingBalanceGroup.values()) {
        totalsByGroup.get(group).forEach(total -> writer.writeRow(group.name(), total.getCategory(), total.getFull(),
            total.getTarget(), total.getSample(), total.getDifference(), total.getRatioToRandomSpread()));
      }
      for (var checkedTable : DownSamplingCheckedTable.values()) {
        cellsByCheckedTable.get(checkedTable).forEach(total -> writer.writeRow(checkedTable.name(),
            total.getCategory(), total.getFull(), total.getTarget(), total.getSample(), total.getDifference(),
            total.getRatioToRandomSpread()));
      }
    }
  }
}

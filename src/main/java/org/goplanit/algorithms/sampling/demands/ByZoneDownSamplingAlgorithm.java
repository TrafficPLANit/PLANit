package org.goplanit.algorithms.sampling.demands;

import org.goplanit.algorithms.sampling.BalancedSampler;
import org.goplanit.algorithms.sampling.BalancedSamplingUnits;
import org.goplanit.algorithms.sampling.demands.enums.DiscreteDemandsDownSamplingAlgorithmType;
import org.goplanit.algorithms.sampling.demands.enums.DownSamplingBalanceGroup;
import org.goplanit.algorithms.sampling.demands.result.DownSamplingDraw;
import org.goplanit.algorithms.sampling.demands.table.DiscreteDemandsDownSamplingTable;
import org.goplanit.utils.math.RoundUtils;
import org.goplanit.utils.misc.HashUtils;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.SplittableRandom;
import java.util.stream.IntStream;

/**
 * Down-sampling at the quality by zone: each zone keeps its sampling units times the down-sample rate, rounded down or
 * up, and those it keeps are drawn at random among its own
 * <p>
 * Terms used here, all listed in {@link org.goplanit.algorithms.sampling.demands}: the <b>down-sample rate</b> is the
 * share of households kept, 0.1 keeping one in ten; a <b>sampling unit</b> is a household with its persons, tours and
 * trips, kept or dropped whole; its <b>chance</b> is how likely it is to be kept; the zones are the <b>strata</b>, each
 * keeping a fixed number of its sampling units.
 * </p>
 * <p>
 * Drawn at random over the whole demand, a zone of 37 households at 10% keeps anywhere from none to eight or so, and a
 * cluster of small zones can keep none at all. Here the zones are rounded in their order along a Hilbert curve from
 * one shared start, so each keeps its expected number rounded down or up, and any run of zones next to each other
 * keeps its expected total to within one. Within a zone, each sampling unit's chance is the zone's number kept over its
 * sampling units, which over the random start is exactly the rate. The draw itself is that of {@link BalancedSampler},
 * the zones as strata, here with no totals besides each zone's number, which the balanced quality adds, see
 * {@link #getGroupsBalanced()}.
 * </p>
 *
 * @author markr
 */
public class ByZoneDownSamplingAlgorithm extends DiscreteDemandsDownSamplingAlgorithmBase {

  /**
   * part of the seed of the zones' rounding after the draw's seed, setting it apart from the sampler's seeds, which
   * take a stratum, 0 or more, or a run, 1, there, and from the seeds of the draws of best of several, -2
   */
  private static final long ROUNDING_SEED_PART = -1;

  /**
   * The number of sampling units each stratum keeps: its sampling units times the rate, rounded along the strata in
   * their order from a start drawn at random, see {@link RoundUtils#roundSystematically(double[], double, double)}
   * <p>
   * The strata are the zones in their order along the curve, then the sampling units without a zone. At 10%, zones of
   * 13, 7 and 4 sampling units expect 1.3, 0.7 and 0.4; from a start of 0.5 the running total goes 1.8, 2.5 and 2.9,
   * passing 1 in the first zone and 2 in the second, so they keep 1, 1 and 0.
   * </p>
   *
   * @param unitsByStratum the sampling units of each stratum
   * @param seed of the draw
   * @return the number each stratum keeps
   */
  private int[] calculateNumbersKeptByStratum(int[] unitsByStratum, long seed) {
    double rate = getSettings().getDownSampleRate();
    var expected = Arrays.stream(unitsByStratum).mapToDouble(units -> units * rate).toArray();
    double start = new SplittableRandom(HashUtils.createCombinedSeed(seed, ROUNDING_SEED_PART)).nextDouble();
    return Arrays.stream(RoundUtils.roundSystematically(expected, 1, start))
        .mapToInt(kept -> (int) Math.round(kept)).toArray();
  }

  /**
   * What the sampler draws from: per sampling unit in the order given, its chance, its stratum, and its values for
   * the groups balanced, see {@link #getGroupsBalanced()}. The categories of each group are totals over all strata,
   * the groups in their order of importance; persons per zone is a total per stratum instead, the zone's persons
   * <p>
   * The down-sampling table keeps only the categories each sampling unit has, a few dozen of the hundred or more. The
   * sampler moves the chances of a few sampling units at a time while keeping every total, so it takes each one's value
   * for every total kept, here read from the down-sampling table's row of the sampling unit. Persons per zone is a
   * total per stratum because the sampler gives those up before any total over all strata, see
   * {@link BalancedDownSamplingAlgorithm}.
   * </p>
   *
   * @param table to draw from
   * @param order of the sampling units, see {@link DiscreteDemandsDownSamplingTable#createUnitOrder()}
   * @param chanceByStratum the chance of each stratum's sampling units
   * @return the sampling units, in the order given
   */
  private BalancedSamplingUnits createSamplingUnits(
      DiscreteDemandsDownSamplingTable table, int[] order, double[] chanceByStratum) {
    var groups = getGroupsBalanced();
    boolean personsPerZone = groups.contains(DownSamplingBalanceGroup.PERSONS_PER_ZONE);
    int[] columns = groups.stream().filter(group -> group != DownSamplingBalanceGroup.PERSONS_PER_ZONE)
        .flatMapToInt(group -> IntStream.range(0, table.getCategories(group).size())
            .map(category -> table.getFirstColumn(group) + category))
        .toArray();

    var units = new BalancedSamplingUnits(columns.length, personsPerZone ? 1 : 0, table.size());
    var row = new double[table.getNumberOfColumns()];
    var unitValues = new double[columns.length];
    var unitStratumValues = new double[personsPerZone ? 1 : 0];
    for (int unit : order) {
      if (columns.length > 0) {
        table.collectValues(unit, row);
        for (int total = 0; total < columns.length; ++total) {
          unitValues[total] = row[columns[total]];
        }
      }
      if (personsPerZone) {
        unitStratumValues[0] = table.getPersons(unit);
      }
      int stratum = table.getStratum(unit);
      units.addUnit(chanceByStratum[stratum], stratum, unitValues, unitStratumValues);
    }
    return units;
  }

  /**
   * Constructor
   *
   * @param settings to use, owned by the algorithm
   */
  protected ByZoneDownSamplingAlgorithm(DiscreteDemandsDownSamplingSettings settings) {
    super(settings);
  }

  /**
   * The groups of totals the draw keeps, the most important first
   *
   * @return none at this quality
   */
  protected List<DownSamplingBalanceGroup> getGroupsBalanced() {
    return List.of();
  }

  /**
   * Draw the sampling units kept at a seed: each stratum's number, see
   * {@link #calculateNumbersKeptByStratum(int[], long)}, drawn by {@link BalancedSampler}
   * over the sampling units in the down-sampling table's order, keeping the totals of {@link #getGroupsBalanced()}
   * as closely as whole sampling units allow
   *
   * @param table to choose from, not changed
   * @param seed of the draw; the same down-sampling table and seed give the same sampling units
   * @return the sampling units kept, by row of the down-sampling table, each stratum's drawn with its number kept over
   *         its sampling units as their chance
   */
  protected DownSamplingDraw chooseUnitsKept(DiscreteDemandsDownSamplingTable table, long seed) {
    int[] order = table.createUnitOrder();
    var unitsByStratum = new int[table.getNumberOfStrata()];
    for (int unit = 0; unit < table.size(); ++unit) {
      ++unitsByStratum[table.getStratum(unit)];
    }
    var numbersKept = calculateNumbersKeptByStratum(unitsByStratum, seed);
    var chanceByStratum = IntStream.range(0, unitsByStratum.length).mapToDouble(stratum ->
        unitsByStratum[stratum] == 0 ? 0 : numbersKept[stratum] / (double) unitsByStratum[stratum]).toArray();
    var units = createSamplingUnits(table, order, chanceByStratum);
    var keptInOrder = new BalancedSampler(getSettings().getNumberOfThreads()).draw(units, seed);
    var kept = new BitSet(table.size());
    keptInOrder.stream().forEach(position -> kept.set(order[position]));
    return new DownSamplingDraw(kept, chanceByStratum);
  }

  /**
   * Draw the sampling units kept at the settings' seed, see
   * {@link #chooseUnitsKept(DiscreteDemandsDownSamplingTable, long)}
   *
   * @param table to choose from, not changed
   * @return the sampling units kept, by row of the down-sampling table, and the chance each stratum's were
   *         drawn with
   */
  @Override
  protected DownSamplingDraw chooseUnitsKept(DiscreteDemandsDownSamplingTable table) {
    return chooseUnitsKept(table, getSettings().getSeed());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DiscreteDemandsDownSamplingAlgorithmType getType() {
    return DiscreteDemandsDownSamplingAlgorithmType.BY_ZONE;
  }
}

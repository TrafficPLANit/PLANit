package org.goplanit.algorithms.sampling;

import org.goplanit.algorithms.parallel.GenericBatchExecutorService;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.HashUtils;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.SplittableRandom;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Draws a balanced sample: a part of a population whose totals match the whole's, scaled down. Each unit, such as a
 * household, is kept with its own chance, such as 10%. Each stratum, a group of units such as a zone, keeps a fixed
 * number of them. Totals over the kept units, such as their car trips, come as close as the units allow to the whole's
 * totals times the chances, a tenth of all car trips at 10%. Where not all of them can be met, the least important
 * total is given up first.
 * <p>
 * A sample drawn at random keeps each unit with its chance, but its totals wander: a 10% sample of households keeps
 * 10% of the car trips only give or take the random spread, and with a hundred totals some are always far off. A
 * balanced sample keeps the chances and holds the totals too: only samples whose totals match can come out, and each
 * unit is still kept with exactly its chance. With p totals and n units kept, a total is then off by about p / n of
 * itself, against about 1 / &radic;n at random (Tillé 2011). It suits any draw of a part that must look like the
 * whole: a travel demand down-sampled with zones as strata and trips by mode as totals, or a survey sample of
 * addresses with districts as strata and dwellings by type as totals.
 * </p>
 * <p>
 * Terms used here, all listed in {@link org.goplanit.algorithms.sampling}: the <b>cube method</b> pictures every
 * possible sample as a corner of a cube with an axis per unit, the chances a point inside it; its <b>flight</b> moves
 * the chances a step at a time, each step keeping every total and deciding at least one unit, kept or dropped; its
 * <b>landing</b> gives up the least important total whenever no step is left. {@link CubeFlight} draws the picture
 * with two households.
 * </p>
 * <p>
 * It is the cube method of Deville and Tillé (2004), in its fast form (Chauvet and Tillé 2006), stratified as Chauvet
 * (2009), with only the strata of a step's units in the step as Jauslin, Eustache and Tillé (2021). Every unit starts
 * at its chance. A step takes a few undecided units and moves their chances together, some up and some down, along a
 * direction that leaves every total and every stratum's number of units unchanged, until one of them reaches 0,
 * dropped, or 1, kept. Which way it moves is drawn so that each unit's expected chance stays what it was, see
 * {@link CubeFlight}. A draw has four passes:
 * </p>
 * <ol>
 * <li>Within each stratum, the strata in parallel: the steps take the stratum's units only, and keep its number of
 * units, its totals per stratum and every total over all units. A stratum ends with at most as many units undecided as
 * it has totals to keep. This is the first phase of Chauvet (2009).</li>
 * <li>Within runs of strata side by side, the runs in parallel: as the next pass, over one run of strata at a time,
 * each run holding a few thousand of the units the strata leave, a fixed number for each row a step keeps.</li>
 * <li>Over all strata: the steps take what the runs leave undecided, stratum after stratum in the order of their
 * first units, and keep the totals over all units and the numbers of units and totals per stratum of the strata a
 * step's units are in. This is the second phase of Chauvet (2009), with the strata brought in as in Algorithm 2 of
 * Jauslin et al. (2021).</li>
 * <li>Landing: when no direction is left, the least important total is given up and the steps go on with the others.
 * The totals per stratum go first, from the end of their list, then the totals over all units, from the end of theirs.
 * The numbers of units per stratum are never given up; they are whole, so each stratum ends with exactly its number.
 * This is the landing by suppression of variables of Deville and Tillé (2004), in an order of preference as Tillé
 * (2011) describes, here the order of the totals.</li>
 * </ol>
 * <p>
 * What differs from those papers:
 * </p>
 * <ul>
 * <li>The totals are of the values as they are: the kept units' values add up to the sum over all units of chance
 * times value. The papers balance each value divided by its unit's chance on the values' plain total, which is the
 * same draw with every value multiplied by its unit's chance; with equal chances the two are the same.</li>
 * <li>Totals per stratum, such as the persons of the households in a zone, besides each stratum's number of units,
 * which is all the papers keep per stratum. The landing gives them up before any total over all units. A stratum's
 * target is seldom a whole number, a tenth of a zone's 2,343 persons being 234.3, so most strata end with a few units
 * no step can move while their totals per stratum are kept. Given up first, those units are settled while every total
 * over all units is still kept; given up last, every total over all units would take the rounding of all of them.</li>
 * <li>The pass within runs of strata is an addition. The pass over all strata runs on one thread, a step at a time,
 * and with thousands of strata it would take most of a draw; within runs, in parallel, it leaves the pass over all
 * strata a few units per run instead of a few per stratum. Each pass keeps every unit's chance, so the sample is drawn
 * as without it.</li>
 * <li>Totals over all units implied by the count and the totals before them, see
 * {@link BalancedSamplingUnits#collectImpliedTotals()}, are left out of the steps, as they are kept whenever the others
 * are.</li>
 * <li>The second pass takes the units stratum by stratum, the strata in the order of their first units, where Jauslin
 * et al. (2021) take the units in the order of the population. A caller that adds the strata in a spatial order thus
 * has what one stratum leaves settled with its neighbours.</li>
 * <li>The landing runs the same steps with fewer totals, not a separate algorithm, so it keeps each unit's chance
 * too.</li>
 * </ul>
 * <p>
 * The first pass draws per stratum from a random sequence seeded by the seed and the stratum, the second per run from
 * one seeded by the seed and the run's first stratum, the third pass and the landing from one seeded by the seed. The
 * runs follow from the units alone, so the same units and seed give the same sample on any number of threads.
 * </p>
 *
 * @see <a href="https://doi.org/10.1093/biomet/91.4.893">Deville, J.-C. and Tillé, Y. (2004). Efficient balanced
 * sampling: the cube method. Biometrika 91(4), 893-912</a>
 * @see <a href="https://doi.org/10.1007/s00180-006-0250-2">Chauvet, G. and Tillé, Y. (2006). A fast algorithm for
 * balanced sampling. Computational Statistics 21(1), 53-62</a>
 * @see "Chauvet, G. (2009). Stratified balanced sampling. Survey Methodology 35(1), 115-119"
 * @see "Hasler, C. and Tillé, Y. (2014). Fast balanced sampling for highly stratified population. Computational
 * Statistics and Data Analysis 74, 81-94"
 * @see <a href="https://doi.org/10.1007/s42081-021-00134-y">Jauslin, R., Eustache, E. and Tillé, Y. (2021). Enhanced
 * cube implementation for highly stratified population. Japanese Journal of Statistics and Data Science 4, 783-795</a>
 * @see <a href="https://www150.statcan.gc.ca/n1/pub/12-001-x/2011002/article/11609-eng.pdf">Tillé, Y. (2011). Ten
 * years of balanced sampling with the cube method: an appraisal. Survey Methodology 37(2), 215-226</a>
 *
 * @author markr
 */
public class BalancedSampler {

  /**
   * The units grouped by stratum, the strata in the order of their first units and a stratum's units in the order
   * added
   */
  private static final class UnitsByStratum {

    /** a stratum's chances add up to a whole number when this close to it */
    private static final double WHOLE_SUM_EPSILON = Precision.EPSILON_6;

    /** the units, stratum after stratum */
    private final int[] order;

    /** per stratum, the position of its first unit in the order, and after the last stratum the number of units */
    private final int[] starts;

    /** per stratum, the stratum */
    private final int[] strata;

    /**
     * Constructor
     *
     * @param order the units, stratum after stratum
     * @param starts per stratum the position of its first unit, then the number of units
     * @param strata per stratum, the stratum
     */
    private UnitsByStratum(int[] order, int[] starts, int[] strata) {
      this.order = order;
      this.starts = starts;
      this.strata = strata;
    }

    /**
     * Group the units by stratum, and check that each stratum's chances add up to a whole number
     * <p>
     * The steps need a stratum's units next to each other: the first pass takes one stratum's units as a run of the
     * order, and the second knows a stratum has all its units in once the next unit is of another one. Units added
     * stratum after stratum keep their order; units of a stratum added apart are brought together where its first one
     * was. A stratum can only keep a fixed number of units when its chances add up to a whole number, such as 30 units
     * of 0.1 keeping 3, so any other sum is refused here, before anything is drawn.
     * </p>
     *
     * @param units to group
     * @return the units grouped by stratum
     * @throws IllegalArgumentException when a stratum's chances do not add up to a whole number
     */
    private static UnitsByStratum of(BalancedSamplingUnits units) {
      final int size = units.size();
      var positionOfStratum = new HashMap<Integer, Integer>();
      var strata = new ArrayList<Integer>();
      var positionOfUnit = new int[size];
      for (int unit = 0; unit < size; ++unit) {
        positionOfUnit[unit] = positionOfStratum.computeIfAbsent(units.getStratum(unit), stratum -> {
          strata.add(stratum);
          return strata.size() - 1;
        });
      }

      var starts = new int[strata.size() + 1];
      var chanceSums = new double[strata.size()];
      for (int unit = 0; unit < size; ++unit) {
        ++starts[positionOfUnit[unit] + 1];
        chanceSums[positionOfUnit[unit]] += units.getChance(unit);
      }
      for (int position = 0; position < strata.size(); ++position) {
        starts[position + 1] += starts[position];
        if (!Precision.equal(chanceSums[position], Math.rint(chanceSums[position]), WHOLE_SUM_EPSILON)) {
          throw new IllegalArgumentException(String.format(
              "Chances of the units of stratum %d add up to %s, not a whole number, so it cannot keep a fixed number "
                  + "of units", strata.get(position), chanceSums[position]));
        }
      }

      var order = new int[size];
      var nextPosition = new int[strata.size()];
      System.arraycopy(starts, 0, nextPosition, 0, strata.size());
      for (int unit = 0; unit < size; ++unit) {
        order[nextPosition[positionOfUnit[unit]]++] = unit;
      }
      return new UnitsByStratum(order, starts, strata.stream().mapToInt(Integer::intValue).toArray());
    }
  }

  /** strata handed to a thread at a time in the first pass */
  private static final int STRATA_PER_BATCH = 8;

  /** runs of strata handed to a thread at a time in the second pass, one, as runs are few and each is long */
  private static final int RUNS_PER_BATCH = 1;

  /** a run of strata in the second pass holds at least this many undecided units for each row a step keeps */
  private static final int RUN_UNITS_PER_ROW = 32;

  /** part of a run's seed after the draw's seed, setting it apart from the seeds of the strata in the first pass */
  private static final long RUN_SEED_PART = 1;

  /** number of threads the first pass runs on */
  private final int threads;

  /**
   * Move the chances within each of the given ranges of the order, the ranges in parallel, each range drawing from a
   * random sequence of its own seed and keeping every total over all units and per stratum. On one thread the ranges
   * run in the calling thread
   *
   * @param units to sample
   * @param chances current chance of each unit, moved in place
   * @param independentTotals the totals over all units the steps write rows for
   * @param order of the units, the ranges in it next to each other
   * @param rangeStarts per range the position in the order of its first unit, then the end of the last range
   * @param rangeSeeds per range the seed of its random sequence
   * @param rangesPerBatch number of ranges handed to a thread at a time
   * @param ranges what the ranges are, for messages, such as "the strata"
   */
  private void flyWithinRanges(BalancedSamplingUnits units, double[] chances, int[] independentTotals, int[] order,
      int[] rangeStarts, long[] rangeSeeds, int rangesPerBatch, String ranges) {
    final int totals = units.getNumberOfTotals();
    final int stratumTotals = units.getNumberOfStratumTotals();
    BiFunction<Integer, CubeFlight, Integer> flyRange = (range, flight) -> {
      flight.fly(order, rangeStarts[range], rangeStarts[range + 1], totals, stratumTotals,
          new SplittableRandom(rangeSeeds[range]));
      return range;
    };
    List<Integer> rangeIndices = IntStream.range(0, rangeSeeds.length).boxed().collect(Collectors.toList());

    if (threads == 1) {
      var flight = new CubeFlight(units, chances, independentTotals);
      rangeIndices.forEach(range -> flyRange.apply(range, flight));
      return;
    }
    var executor = new GenericBatchExecutorService<Integer, Integer, CubeFlight>(
        rangeIndices, (thread, threadId) -> new CubeFlight(units, chances, independentTotals), flyRange, null);
    executor.executeOrThrowRunTimeException(
        threads, rangesPerBatch, String.format("Balanced sampling while drawing within %s", ranges));
  }

  /**
   * The first pass: move the chances within each stratum, the strata in parallel, each drawing from a random sequence
   * seeded by the seed and the stratum
   *
   * @param units to sample
   * @param chances current chance of each unit, moved in place
   * @param independentTotals the totals over all units the steps write rows for
   * @param byStratum the units grouped by stratum
   * @param seed of the draw
   */
  private void flyWithinEachStratum(BalancedSamplingUnits units, double[] chances, int[] independentTotals,
      UnitsByStratum byStratum, long seed) {
    final long[] seeds = new long[byStratum.strata.length];
    for (int position = 0; position < seeds.length; ++position) {
      seeds[position] = HashUtils.createCombinedSeed(seed, byStratum.strata[position]);
    }
    flyWithinRanges(units, chances, independentTotals, byStratum.order, byStratum.starts, seeds, STRATA_PER_BATCH,
        "the strata");
  }

  /**
   * The second pass: split the undecided units, in their order, into runs of strata side by side, and move the chances
   * within each run, the runs in parallel, each drawing from a random sequence seeded by the seed and the run's first
   * stratum. A run ends at the end of the first stratum that brings it to at least {@link #RUN_UNITS_PER_ROW} units for
   * each row a step keeps; with one run only, the pass over all strata does the same, so nothing is done here
   * <p>
   * The pass over all strata runs on one thread, a step at a time, and with thousands of strata that would be most of
   * a draw. Each run leaves about as many undecided units as a step keeps rows, so after this pass it has a few per run
   * to settle instead of a few per stratum. The runs follow from the units alone, never from the number of threads, so
   * the sample does not change with it. For example, with 118 totals over all units, 20 of them implied and left out,
   * and one per zone, a run holds at least 32 times 100 units: the 316,899 units 3,370 zones left made 103 runs, which
   * left the pass over all zones about 10,000.
   * </p>
   *
   * @param units to sample
   * @param chances current chance of each unit, moved in place
   * @param independentTotals the totals over all units the steps write rows for
   * @param order of the units, the undecided ones at the front, a stratum's units next to each other
   * @param undecided number of undecided units at the front of the order
   * @param seed of the draw
   */
  private void flyWithinRunsOfStrata(BalancedSamplingUnits units, double[] chances, int[] independentTotals,
      int[] order, int undecided, long seed) {
    final int runUnits = RUN_UNITS_PER_ROW * (independentTotals.length + units.getNumberOfStratumTotals() + 1);
    final var runStarts = new ArrayList<Integer>();
    runStarts.add(0);
    for (int position = 1; position < undecided; ++position) {
      if (position - runStarts.get(runStarts.size() - 1) >= runUnits
          && units.getStratum(order[position]) != units.getStratum(order[position - 1])) {
        runStarts.add(position);
      }
    }
    if (runStarts.size() < 2) {
      return;
    }
    final long[] seeds = new long[runStarts.size()];
    for (int run = 0; run < seeds.length; ++run) {
      seeds[run] = HashUtils.createCombinedSeed(seed, RUN_SEED_PART, units.getStratum(order[runStarts.get(run)]));
    }
    runStarts.add(undecided);
    flyWithinRanges(units, chances, independentTotals, order, runStarts.stream().mapToInt(Integer::intValue).toArray(),
        seeds, RUNS_PER_BATCH, "runs of strata");
  }

  /**
   * Constructor
   *
   * @param threads number of threads to draw on, at least 1: the strata, such as zones, are shared out over them. The
   *                same units and seed give the same sample on any number of threads
   * @throws IllegalArgumentException when the number of threads is below 1
   */
  public BalancedSampler(int threads) {
    if (threads < 1) {
      throw new IllegalArgumentException(String.format("Number of threads %d is below 1", threads));
    }
    this.threads = threads;
  }

  /**
   * Draw a balanced sample of the units: within each stratum, then within runs of strata, then over all strata, then
   * landing by giving up the totals from the least important, each stratum keeping exactly its number of units
   * throughout
   * <p>
   * For example, households with a chance of 10% each, zones as strata, car trips as a total over all units and persons
   * as a total per stratum: each zone keeps exactly a tenth of its households, the kept households hold a tenth of all
   * car trips as near as they allow, and each zone's kept persons come as close to a tenth of its persons as remains
   * possible after that.
   * </p>
   * <p>
   * The first pass does most of the work, a stratum at a time and so in parallel, and leaves only a few units per
   * stratum. The second settles those within runs of strata side by side, also in parallel, and the third settles what
   * the runs leave across all strata, still keeping every total. The landing then runs the same steps once per total
   * given up: with 118 totals over all units and one per stratum it can run 119 times, each over the few units still
   * undecided, until only the numbers per stratum are kept, which always leaves a direction while a stratum has two
   * undecided units. For example, households with a count of car trips listed before their kilometres keep the car
   * trips exactly whenever the kilometres can be given up to do so.
   * </p>
   *
   * @param units to draw from
   * @param seed of the draw; the same units and seed give the same sample
   * @return the units kept, by their index in the order added, so bit 5 set means the sixth household added is kept
   * @throws IllegalArgumentException when a stratum's chances do not add up to a whole number, such as a zone of 13
   *                                  households at 10%, which would keep 1.3
   */
  public BitSet draw(BalancedSamplingUnits units, long seed) {
    final var byStratum = UnitsByStratum.of(units);
    final var implied = units.collectImpliedTotals();
    final int[] independentTotals =
        IntStream.range(0, units.getNumberOfTotals()).filter(total -> !implied.get(total)).toArray();
    final var chances = new double[units.size()];
    for (int unit = 0; unit < chances.length; ++unit) {
      chances[unit] = units.getChance(unit);
    }

    flyWithinEachStratum(units, chances, independentTotals, byStratum, seed);
    int undecided = CubeFlight.moveUndecidedForward(byStratum.order, 0, byStratum.order.length, 0, chances);
    flyWithinRunsOfStrata(units, chances, independentTotals, byStratum.order, undecided, seed);
    undecided = CubeFlight.moveUndecidedForward(byStratum.order, 0, undecided, 0, chances);

    var flight = new CubeFlight(units, chances, independentTotals);
    var random = new SplittableRandom(seed);
    int totals = units.getNumberOfTotals();
    int stratumTotals = units.getNumberOfStratumTotals();
    while (undecided > 0) {
      flight.fly(byStratum.order, 0, undecided, totals, stratumTotals, random);
      undecided = CubeFlight.moveUndecidedForward(byStratum.order, 0, undecided, 0, chances);
      if (stratumTotals > 0) {
        --stratumTotals;
      } else if (totals > 0) {
        --totals;
      } else {
        break;
      }
    }

    /* only rounding errors leave a unit undecided here, a stratum's last one, which goes to the nearer of 0 and 1 */
    var kept = new BitSet(chances.length);
    for (int unit = 0; unit < chances.length; ++unit) {
      if (chances[unit] >= 0.5) {
        kept.set(unit);
      }
    }
    return kept;
  }
}

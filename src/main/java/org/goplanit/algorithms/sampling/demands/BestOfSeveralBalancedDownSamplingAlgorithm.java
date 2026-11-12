package org.goplanit.algorithms.sampling.demands;

import org.goplanit.algorithms.sampling.demands.enums.DiscreteDemandsDownSamplingAlgorithmType;
import org.goplanit.algorithms.sampling.demands.result.DiscreteDemandsDownSamplingStatistics;
import org.goplanit.algorithms.sampling.demands.result.DownSamplingDraw;
import org.goplanit.algorithms.sampling.demands.table.DiscreteDemandsDownSamplingTable;
import org.goplanit.utils.misc.HashUtils;

import java.util.Arrays;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Down-sampling at the quality balanced, best of several: the balanced draw made several times, each at a seed of its
 * own, keeping the draw of the lowest score, see {@link DiscreteDemandsDownSamplingSettings#getNumberOfDraws()} and
 * {@link DiscreteDemandsDownSamplingStatistics#getScore()}
 * <p>
 * Terms used here, all listed in {@link org.goplanit.algorithms.sampling.demands}: a <b>draw</b> is one sample from
 * one seed; a <b>checked table</b> is a finer count, such as person trips by mode and hour, measured but never
 * balanced; the <b>score</b> is the checked tables' error over the error a draw of households at random would give,
 * lower being better.
 * </p>
 * <p>
 * The balanced draw keeps the totals of its groups, but the checked tables, such as trips between areas or tours by
 * purpose and mode, only as far as those totals carry them, and how far differs from draw to draw. Keeping the draw
 * that matches the checked tables best gives a sample closer to the full demand where nothing balances it, for as
 * many times the time of one draw as there are draws. It favours, a little, households that make well balanced
 * samples, so their chance is a little above the rate; with a few draws the effect is small (Fuller 2009).
 * </p>
 * <p>
 * The first draw is at the settings' seed, so it is the balanced draw itself, and the sample never scores worse than
 * the balanced quality at the same seed. The draw kept is logged with its seed, so it can be drawn again at the
 * balanced quality alone.
 * </p>
 *
 * @author markr
 */
public class BestOfSeveralBalancedDownSamplingAlgorithm extends BalancedDownSamplingAlgorithm {

  /** the logger */
  private static final Logger LOGGER =
      Logger.getLogger(BestOfSeveralBalancedDownSamplingAlgorithm.class.getCanonicalName());

  /**
   * part of the seed of each draw after the first, after the settings' seed, setting it apart from every seed a draw
   * takes within: the zones' rounding, -1, and the sampler's, a zone id in the table, 0 or more, or a run, 1, there
   */
  private static final long DRAW_SEED_PART = -2;

  /**
   * Constructor
   *
   * @param settings to use, owned by the algorithm
   */
  protected BestOfSeveralBalancedDownSamplingAlgorithm(DiscreteDemandsDownSamplingSettings settings) {
    super(settings);
  }

  /**
   * Draw the sampling units kept: the balanced draw at the seed of each of the settings' number of draws, see
   * {@link #createSeedOfDraw(long, int)}, keeping the one of the lowest score, the first of them when two score the
   * same
   *
   * @param table to choose from, not changed
   * @return the sampling units kept, by row of the down-sampling table, and the chance each zone's were
   *         drawn with
   */
  @Override
  protected DownSamplingDraw chooseUnitsKept(DiscreteDemandsDownSamplingTable table) {
    var scores = new double[getSettings().getNumberOfDraws()];
    DownSamplingDraw best = null;
    int bestDraw = 0;
    for (int draw = 0; draw < scores.length; ++draw) {
      var candidate = chooseUnitsKept(table, createSeedOfDraw(getSettings().getSeed(), draw));
      scores[draw] = new DiscreteDemandsDownSamplingStatistics(
          table, candidate, getSettings().getDownSampleRate()).getScore();
      if (best == null || scores[draw] < scores[bestDraw]) {
        best = candidate;
        bestDraw = draw;
      }
    }
    LOGGER.info(String.format("Best of %d balanced draws: draw %d kept, at seed %d, scoring %.3f; the draws scored %s",
        scores.length, bestDraw, createSeedOfDraw(getSettings().getSeed(), bestDraw), scores[bestDraw],
        Arrays.stream(scores).mapToObj(score -> String.format("%.3f", score)).collect(Collectors.joining(", "))));
    return best;
  }

  /**
   * The seed of a draw of the best of several: the seed itself for the first, draw 0, and one made from the seed and
   * the draw number for each next. The balanced quality at a draw's seed makes that draw
   *
   * @param seed of the settings
   * @param draw number, 0 for the first
   * @return the seed of the draw
   */
  public static long createSeedOfDraw(long seed, int draw) {
    return draw == 0 ? seed : HashUtils.createCombinedSeed(seed, DRAW_SEED_PART, draw);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DiscreteDemandsDownSamplingAlgorithmType getType() {
    return DiscreteDemandsDownSamplingAlgorithmType.BALANCED_BEST_OF_SEVERAL;
  }
}

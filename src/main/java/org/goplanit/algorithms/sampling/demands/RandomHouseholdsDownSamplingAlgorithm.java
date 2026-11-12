package org.goplanit.algorithms.sampling.demands;

import org.goplanit.algorithms.sampling.demands.enums.DiscreteDemandsDownSamplingAlgorithmType;
import org.goplanit.algorithms.sampling.demands.result.DownSamplingDraw;
import org.goplanit.algorithms.sampling.demands.table.DiscreteDemandsDownSamplingTable;
import org.goplanit.utils.math.RoundUtils;

import java.util.Arrays;
import java.util.BitSet;
import java.util.SplittableRandom;

/**
 * Down-sampling at the quality of households at random: each sampling unit kept with the down-sample rate as its
 * chance, on its own, whatever is kept of the others
 * <p>
 * Terms used here, all listed in {@link org.goplanit.algorithms.sampling.demands}: a <b>sampling unit</b> is a
 * household with its persons, tours and trips, kept or dropped whole; its <b>chance</b> is how likely it is to be
 * kept, here the <b>down-sample rate</b>, the share of households kept, such as 0.1 keeping one in ten.
 * </p>
 * <p>
 * The sample holds the rate's share of the households only give or take the random spread, and so does each zone and
 * each total, as with MATSim's own down-sampling, though here households and the tours they share stay whole. It is
 * the baseline the other qualities are measured against.
 * </p>
 *
 * @author markr
 */
public class RandomHouseholdsDownSamplingAlgorithm extends DiscreteDemandsDownSamplingAlgorithmBase {

  /**
   * Constructor
   *
   * @param settings to use, owned by the algorithm
   */
  protected RandomHouseholdsDownSamplingAlgorithm(DiscreteDemandsDownSamplingSettings settings) {
    super(settings);
  }

  /**
   * Keep each sampling unit when its rate rounds up to 1 at random, see
   * {@link RoundUtils#roundStochastically(double, double, java.util.function.DoubleSupplier)}, one number drawn per row
   * of the down-sampling table in the order given, so the same demand and seed always keep the same households
   *
   * @param table to choose from, not changed
   * @return the sampling units kept, by row of the down-sampling table, each stratum's drawn with the rate as its
   *         chance
   */
  @Override
  protected DownSamplingDraw chooseUnitsKept(DiscreteDemandsDownSamplingTable table) {
    var random = new SplittableRandom(getSettings().getSeed());
    double rate = getSettings().getDownSampleRate();
    var kept = new BitSet(table.size());
    for (int unit = 0; unit < table.size(); ++unit) {
      if (RoundUtils.roundStochastically(rate, 1, random::nextDouble) > 0) {
        kept.set(unit);
      }
    }
    var chanceByStratum = new double[table.getNumberOfStrata()];
    Arrays.fill(chanceByStratum, rate);
    return new DownSamplingDraw(kept, chanceByStratum);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DiscreteDemandsDownSamplingAlgorithmType getType() {
    return DiscreteDemandsDownSamplingAlgorithmType.HOUSEHOLDS_AT_RANDOM;
  }
}

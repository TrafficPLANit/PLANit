package org.goplanit.algorithms.sampling.demands.result;

/**
 * One total of a down-sampling's statistics, a cell of a checked table or a zone's value: the full demand's, the
 * sample's target, the sample's own, and the spread a random draw of households would give
 * <p>
 * Terms used here, all listed in {@link org.goplanit.algorithms.sampling.demands}: the <b>target</b> is what the sample
 * aims at, each sampling unit's chance times its value summed over all of them, at 10% about a tenth of the full
 * demand's; the <b>random spread</b> is how far a draw of households at random at the rate would typically be off.
 * </p>
 * <p>
 * The difference from the target says how far the sample is off; divided by the random spread it says how that
 * compares with a plain random draw of households at the same rate. A sample 120 car trips above a target of 1,000,
 * where a random draw spreads by 400, is 12% off and at 0.3 of the random spread, so better than a random draw would
 * mostly be.
 * </p>
 *
 * @author markr
 */
public class DownSamplingTotal {

  /** label of the category */
  private final String category;

  /** the full demand's total */
  private final double full;

  /** what the sample aims at: each sampling unit's chance in the draw times its value, summed */
  private final double target;

  /** the sample's total */
  private final double sample;

  /** the spread a random draw of households at the rate would give */
  private final double randomSpread;

  /**
   * Constructor
   *
   * @param category label of the category
   * @param full the full demand's total
   * @param target what the sample aims at
   * @param sample the sample's total
   * @param randomSpread the spread a random draw of households at the rate would give
   */
  public DownSamplingTotal(String category, double full, double target, double sample, double randomSpread) {
    this.category = category;
    this.full = full;
    this.target = target;
    this.sample = sample;
    this.randomSpread = randomSpread;
  }

  /**
   * The label of the category
   *
   * @return the label
   */
  public String getCategory() {
    return category;
  }

  /**
   * The full demand's total
   *
   * @return the total
   */
  public double getFull() {
    return full;
  }

  /**
   * What the sample aims at
   *
   * @return the target
   */
  public double getTarget() {
    return target;
  }

  /**
   * The sample's total
   *
   * @return the total
   */
  public double getSample() {
    return sample;
  }

  /**
   * The spread a random draw of households at the rate would give
   *
   * @return the spread
   */
  public double getRandomSpread() {
    return randomSpread;
  }

  /**
   * How far the sample is above its target, below it when negative
   *
   * @return the sample less the target
   */
  public double getDifference() {
    return sample - target;
  }

  /**
   * The difference as a share of the target
   *
   * @return the share, 0 when the target is 0
   */
  public double getShareOfTarget() {
    return target == 0 ? 0 : getDifference() / target;
  }

  /**
   * The difference divided by the random spread
   *
   * @return the ratio, 0 when the spread is 0
   */
  public double getRatioToRandomSpread() {
    return randomSpread == 0 ? 0 : getDifference() / randomSpread;
  }
}

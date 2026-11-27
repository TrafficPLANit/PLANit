package org.goplanit.algorithms.sampling.demands.result;

import org.goplanit.utils.math.Precision;

import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * A summary of the totals of a group, the cells of a checked table or the zones of a measure: how many, how far off at
 * worst, and the error of them all against the error a random draw of households would give
 * <p>
 * The error is the standardised root mean square error: the root of the mean squared difference over the totals,
 * divided by the mean of their targets, so groups of large and small totals compare. Targets of 100 and 300 met by 110
 * and 290 differ by 10 each, so the error is 10 over the mean target of 200, 0.05. The random error is the same with
 * each total's random spread in place of its difference, the error a random draw would give on average.
 * </p>
 * <p>
 * Terms used here, all listed in {@link org.goplanit.algorithms.sampling.demands}: a total's <b>target</b> is what
 * the sample aims at, at 10% about a tenth of the full demand's; its <b>random spread</b> is how far a draw of
 * households at random at the rate would typically be off.
 * </p>
 *
 * @author markr
 */
public class DownSamplingTotalsSummary {

  /** the totals */
  private final List<DownSamplingTotal> totals;

  /**
   * The mean of a value over the totals
   *
   * @param value of a total
   * @return the mean, 0 without totals
   */
  private double calculateMean(ToDoubleFunction<DownSamplingTotal> value) {
    return totals.stream().mapToDouble(value).average().orElse(0);
  }

  /**
   * A root mean square over the totals divided by their mean target
   *
   * @param value of a total, squared before the mean is taken
   * @return the standardised root mean square, 0 when the mean target is 0
   */
  private double calculateStandardisedRootMeanSquare(ToDoubleFunction<DownSamplingTotal> value) {
    double meanTarget = calculateMean(DownSamplingTotal::getTarget);
    if (meanTarget == 0) {
      return 0;
    }
    return Math.sqrt(calculateMean(total -> value.applyAsDouble(total) * value.applyAsDouble(total))) / meanTarget;
  }

  /**
   * Constructor
   *
   * @param totals to summarise
   */
  public DownSamplingTotalsSummary(List<DownSamplingTotal> totals) {
    this.totals = List.copyOf(totals);
  }

  /**
   * Number of totals
   *
   * @return the number
   */
  public int getNumberOfTotals() {
    return totals.size();
  }

  /**
   * The largest difference as a share of its target, whatever its sign
   *
   * @return the share, 0 without totals
   */
  public double getLargestShareOfTarget() {
    return totals.stream().mapToDouble(total -> Math.abs(total.getShareOfTarget())).max().orElse(0);
  }

  /**
   * The mean of the ratios to the random spread, ignoring their sign
   *
   * @return the mean, 0 without totals
   */
  public double getMeanRatioToRandomSpread() {
    return calculateMean(total -> Math.abs(total.getRatioToRandomSpread()));
  }

  /**
   * The standardised root mean square error of the totals
   *
   * @return the error, 0 when the mean target is 0
   */
  public double getError() {
    return calculateStandardisedRootMeanSquare(DownSamplingTotal::getDifference);
  }

  /**
   * The error a random draw of households would give on average, from each total's random spread
   *
   * @return the error, 0 when the mean target is 0
   */
  public double getRandomError() {
    return calculateStandardisedRootMeanSquare(DownSamplingTotal::getRandomSpread);
  }

  /**
   * The largest difference, whatever its sign
   *
   * @return the difference, 0 without totals
   */
  public double getLargestDifference() {
    return totals.stream().mapToDouble(total -> Math.abs(total.getDifference())).max().orElse(0);
  }

  /**
   * The share of totals whose difference is at most the given one, whatever its sign
   *
   * @param difference allowed, such as one household
   * @return the share, 0 without totals
   */
  public double getShareWithin(double difference) {
    return totals.isEmpty() ? 0 :
        totals.stream().filter(total -> Math.abs(total.getDifference()) <= difference + Precision.EPSILON_9).count()
            / (double) totals.size();
  }

  /**
   * A line of the log summing these totals up, such as "Person trips by mode: 10 totals, largest difference 0.04%, mean
   * ratio to random 0.020, error 0.0003 against 0.0061 for households at random"
   *
   * @param name of what is summarised, starting with a capital
   * @return the line
   */
  public String createLogLine(String name) {
    return String.format("%s: %d totals, largest difference %.2f%%, mean ratio to random %.3f, error %.4f against %.4f"
            + " for households at random", name, getNumberOfTotals(), 100 * getLargestShareOfTarget(),
        getMeanRatioToRandomSpread(), getError(), getRandomError());
  }
}

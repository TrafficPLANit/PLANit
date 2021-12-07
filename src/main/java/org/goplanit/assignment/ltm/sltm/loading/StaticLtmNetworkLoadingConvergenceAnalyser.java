package org.goplanit.assignment.ltm.sltm.loading;

import java.util.ArrayList;
import java.util.logging.Logger;

import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.LoggingUtils;

/**
 * Analyser that based on various inputs such as the iteration and previous gaps determines if the loading is converging or not using its configurable criteria
 * 
 * @author markr
 *
 */
public class StaticLtmNetworkLoadingConvergenceAnalyser {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(StaticLtmNetworkLoadingConvergenceAnalyser.class.getCanonicalName());

  /**
   * Tracked iteration gaps
   */
  private final ArrayList<Double> gapsByIteration;

  /**
   * iteration offset to use in applying iteration based thresholds, e.g. when iteration=10, but offset is 8, then we are working with an actual iteration of 2.
   */
  private int iterationOffset = DEFAULT_ITERATION_OFFSET;

  // SETTINGS

  /**
   * only after exceeding this threshold convergence can be classified as non-improving
   */
  private int minIterationThreshold = DEFAULT_MIN_ITERATION_THRESHOLD;

  /**
   * only before exceeding this threshold convergence can be classified as improving
   */
  private int maxIterationThreshold = DEFAULT_MAX_ITERATION_THRESHOLD;

  /**
   * Whenever a change in gap is detected worse than this value, we consider the network non-convergent conditional on the min iteration threshold to be satisfied
   */
  private final double maxSingleIterationWorseningGapThreshold = DEFAULT_MAX_SINGLE_WORSENING_THRESHOLD;

  /** There should be some improvement on average, once it turns negative on average we have a problem */
  private final double minAverageImprovingGapThreshold = DEFAULT_MIN_AVERAGE_IMPROVEMENT_THRESHOLD;

  /**
   * Default value to use
   */
  public static int DEFAULT_ITERATION_OFFSET = 0;

  /**
   * Default value to use
   */
  public static int DEFAULT_MIN_ITERATION_THRESHOLD = 2;

  /**
   * Default value to use
   */
  public static int DEFAULT_MAX_ITERATION_THRESHOLD = 5;

  /**
   * Default value to use
   */
  public static double DEFAULT_MIN_AVERAGE_IMPROVEMENT_THRESHOLD = 0.0;

  /**
   * Default value to use
   */
  public static double DEFAULT_MAX_SINGLE_WORSENING_THRESHOLD = 0.1;

  /**
   * Default constructor
   */
  public StaticLtmNetworkLoadingConvergenceAnalyser() {
    this.gapsByIteration = new ArrayList<Double>();
  }

  /**
   * Verify if the convergence is improving or not given the settings and data provided to the analyser
   * 
   * @return true is potentially improving, false it is not
   */
  public boolean isImproving() {
    /* do not flag as non-improving before exceeding minimum iteration threshold */
    if (minIterationThreshold > (getRegisteredIterations() - iterationOffset)) {
      return true;
    }
    /* do not flag as improving after exceeding maximum iteration threshold */
    if (maxIterationThreshold < (getRegisteredIterations() - iterationOffset)) {
      return false;
    }

    /* check if single iteration gap worsening is not exceeded */
    if (gapsByIteration.size() > 2) {
      double latestGap = gapsByIteration.get(getRegisteredIterations());
      double nextLatestGap = gapsByIteration.get(getRegisteredIterations() - 1);
      if (Precision.greaterEqual(latestGap - nextLatestGap, maxSingleIterationWorseningGapThreshold)) {
        return false;
      }
    }

    /* check if average gap over iterations is not improving by the minimum requirement */
    double gapDelta = gapsByIteration.get(iterationOffset) - gapsByIteration.get(getRegisteredIterations());
    double averagePerIterationGapImprovement = gapDelta / (getRegisteredIterations() - iterationOffset);
    if (Precision.smallerEqual(averagePerIterationGapImprovement, minAverageImprovingGapThreshold)) {
      return false;
    }

    return true;

  }

  /**
   * Register the found gap for the iteration
   * 
   * @param gap to log
   */
  public void registerIterationGap(double gap) {
    gapsByIteration.add(gap);
  }

  // GETTERS-SETTERS

  /**
   * Number of registered iterations
   * 
   * @return iterations
   */
  public int getRegisteredIterations() {
    return Math.max(0, gapsByIteration.size() - 1);
  }

  /**
   * Currently set iteration offset beyond which the analyser is applied
   * 
   * @return iteration offset index
   */
  public int getIterationOffset() {
    return iterationOffset;
  }

  /**
   * Set the offset to use
   * 
   * @param iterationOffset to use
   */
  public void setIterationOffset(int iterationOffset) {
    this.iterationOffset = iterationOffset;
  }

  /**
   * Get the minimum iteration threshold to use before we can classify as non-improving
   * 
   * @return current threshold
   */
  public int getMinIterationThreshold() {
    return minIterationThreshold;
  }

  /**
   * Set the minimum iteration threshold to use before we can classify as non-improving
   * 
   * @param minIterationThreshold to set
   */
  public void setMinIterationThreshold(int minIterationThreshold) {
    this.minIterationThreshold = minIterationThreshold;
  }

  /**
   * Log gaps since iteration provided
   * 
   * @param runId              to log for
   * @param referenceIteration starting iteration to log until most recent
   */
  public void logGapsSince(long runId, int referenceIteration) {
    LOGGER.info(
        String.format("%sGaps for iteration %d-%d: %s", LoggingUtils.createRunIdPrefix(runId), referenceIteration, getRegisteredIterations(), this.gapsByIteration.toString()));
  }

  /**
   * Reset to initial state
   */
  public void reset() {
    this.gapsByIteration.clear();
    this.iterationOffset = DEFAULT_ITERATION_OFFSET;
  }
}

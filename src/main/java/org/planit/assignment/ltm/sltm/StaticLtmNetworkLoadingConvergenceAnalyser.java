package org.planit.assignment.ltm.sltm;

import java.util.ArrayList;

import org.planit.utils.math.Precision;

/**
 * Analyser that based on various inputs such as the iteration and previous gaps determines if the loading is converging or not using its configurable criteria
 * 
 * @author markr
 *
 */
public class StaticLtmNetworkLoadingConvergenceAnalyser {

  private ArrayList<Double> gapsByIteration;

  // SETTINGS

  /**
   * only after exceeding this threshold convergence can be classified as non-improving
   */
  private int minIterationThreshold = 2;

  /**
   * only before exceeding this threshold convergence can be classified as improving
   */
  private int maxIterationThreshold = 5;

  /**
   * Whenever a change in gap is detected worse than this value, we consider the network non-convergent conditional on the min iteration threshold to be satisfied
   */
  private final double maxSingleIterationWorseningGapThreshold = 0.1;

  private final double minAverageImprovingGapThreshold = 0.1;

  /**
   * iteration offset to use in applying iteration based thresholds, e.g. when iteration=10, but offset is 8, then we are working with an actual iteration of 2.
   */
  private int iterationOffset = 0;

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
      if (Precision.isGreaterEqual(latestGap - nextLatestGap, maxSingleIterationWorseningGapThreshold)) {
        return false;
      }
    }

    /* check if average gap over iterations is not improving by the minimum requirement */
    double gapDelta = gapsByIteration.get(iterationOffset) - gapsByIteration.get(getRegisteredIterations());
    double averagePerIterationGapImprovement = gapDelta / (getRegisteredIterations() - iterationOffset);
    if (Precision.isSmallerEqual(minAverageImprovingGapThreshold, averagePerIterationGapImprovement)) {
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
}

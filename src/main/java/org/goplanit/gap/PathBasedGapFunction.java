package org.goplanit.gap;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;

import java.util.logging.Logger;

/**
 * Gap function generalised from the SUE gap function in Bliemer et al. (2014) who sum each origin-destination path based cost multiplied by its
 * assigned path flow and divide it by the least cost path multiplied by the total o-d flow.
 * 
 * @author markr
 *
 */
public class PathBasedGapFunction extends GapFunction {

  /** Generated UID */
  private static final long serialVersionUID = 7202275902172315983L;

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(PathBasedGapFunction.class.getCanonicalName());

  /**
   * Constructor
   *
   * @param idToken       to use for the generation of its id
   * @param stopCriterion StopCriterion object being used
   */
  public PathBasedGapFunction(final IdGroupingToken idToken, final StopCriterion stopCriterion) {
    super(idToken, stopCriterion);
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public PathBasedGapFunction(PathBasedGapFunction other, boolean deepCopy) {
    super(other, deepCopy);
    this.scaledMeasuredPathCostGap = other.scaledMeasuredPathCostGap;
    this.gap = other.gap;
    this.scaledMinimumPathCosts = other.scaledMinimumPathCosts;
  }

  /**
   * Current system cost as it stands
   */
  protected double scaledMeasuredPathCostGap = 0;

  /**
   * Represents the total minimum cost possible
   */
  protected double scaledMinimumPathCosts = 0;

  /**
   * Gap
   */
  protected double gap = INITIAL_GAP;

  /** initial gap to use */
  public static double INITIAL_GAP = Double.POSITIVE_INFINITY;

  /**
   * Return the (demand) scaled measured cost absolute gap
   * 
   * @return the measured cost
   */
  public double getScaledMeasuredPathCostAbsoluteGap() {
    return scaledMeasuredPathCostGap;
  }

  /**
   * Increase measured cost gap which is diff between min and measured path cost for group, this difference is muliplied by
   * the assigned path demand. This adds to the numerator of the gap
   * 
   * @param measuredPathCost cost of a single path
   * @param pathDemand assigned which is sued as multiplication factor
   */
  public void increaseAbsolutePathGap(double measuredPathCost, double pathDemand, double minimumPathCost) {
    scaledMeasuredPathCostGap += (measuredPathCost - minimumPathCost) * pathDemand;
  }

  /**
   * Increase minimum path cost by providing minimum cost and the multiplication factor (typically demand). This scaled minimum
   * cost contributes to the denominator of the gap
   * 
   * @param minimumPathCost cost of a cheapest path across a grouping of paths, usually per origin-destination
   * @param groupedDemand the total that goes with the grouping, usually the total Origin-destination demand
   */
  public void increaseMinimumPathCosts(double minimumPathCost, double groupedDemand) {
    scaledMinimumPathCosts += minimumPathCost * groupedDemand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    this.scaledMeasuredPathCostGap = 0;
    this.scaledMinimumPathCosts = 0;
    this.gap = INITIAL_GAP;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double computeGap() {
    if (scaledMeasuredPathCostGap == 0 && scaledMinimumPathCosts == 0) {
      return 0;
    }

    if (!Precision.positive(scaledMeasuredPathCostGap)) {
      LOGGER.severe(String.format("Measured network cost (%.2f) needs to be positive to compute gap, this is not the case", scaledMeasuredPathCostGap));
      return -1;
    }

    if(!Precision.positive(scaledMinimumPathCosts)){
      LOGGER.severe(String.format("Minimum network cost (%.2f) needs to be positive to compute gap, this is not the case", scaledMinimumPathCosts));
    }

    /* regular non-zero measured cost */
    gap = scaledMeasuredPathCostGap / scaledMinimumPathCosts;
    return gap;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getGap() {
    return gap;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PathBasedGapFunction shallowClone() {
    return new PathBasedGapFunction(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PathBasedGapFunction deepClone() {
    return new PathBasedGapFunction(this, true);
  }

}
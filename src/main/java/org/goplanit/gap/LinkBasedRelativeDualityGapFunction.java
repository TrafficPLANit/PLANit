package org.goplanit.gap;

import java.util.Map;
import java.util.logging.Logger;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;

/**
 * Gap function based on the work of Bovy and Jansen (1983) who take the different between the current system travel time and the system travel time if all flow were to be assigned
 * to the shortest paths, divided by the system travel time
 * 
 * @author markr
 *
 */
public class LinkBasedRelativeDualityGapFunction extends GapFunction {

  /** Generated UID */
  private static final long serialVersionUID = 7202275902172315983L;

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(LinkBasedRelativeDualityGapFunction.class.getCanonicalName());

  /**
   * Constructor
   * 
   * @param idToken       to use for the generation of its id
   * @param stopCriterion StopCriterion object being used
   */
  public LinkBasedRelativeDualityGapFunction(final IdGroupingToken idToken, final StopCriterion stopCriterion) {
    super(idToken, stopCriterion);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   */
  public LinkBasedRelativeDualityGapFunction(LinkBasedRelativeDualityGapFunction other) {
    super(other);
    this.measuredNetworkCost = other.measuredNetworkCost;
    this.gap = other.gap;
    this.minimumNetworkCost = other.minimumNetworkCost;
  }

  /**
   * Current system cost as it stands
   */
  protected double measuredNetworkCost = 0;

  /**
   * Represents the total minimum cost possible
   */
  protected double minimumNetworkCost = 0;

  /**
   * Gap
   */
  protected double gap = INITIAL_GAP;

  /** initial gap to use */
  public static double INITIAL_GAP = Double.POSITIVE_INFINITY;

  /**
   * Return the measured cost
   * 
   * @return the measured cost
   */
  public double getMeasuredNetworkCost() {
    return measuredNetworkCost;
  }

  /**
   * Increase system cost, i.e. compute it exogenously
   * 
   * @param value increase
   */
  public void increaseMeasuredCost(double value) {
    measuredNetworkCost += value;
  }

  /**
   * Increase convexity bound cost, i.e. compute it exogenously
   * 
   * @param value the increase
   */
  public void increaseConvexityBound(double value) {
    minimumNetworkCost += value;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    this.measuredNetworkCost = 0;
    this.minimumNetworkCost = 0;
    this.gap = INITIAL_GAP;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double computeGap() {
    if (Precision.smaller(measuredNetworkCost, minimumNetworkCost)) {
      LOGGER.severe(String.format("Minimum network cost (%.2f) exceeds measured network cost (%.2f), this should not happen", minimumNetworkCost, measuredNetworkCost));
    }

    /* special case, both might be zero for example - unlikely but technically this is considered converged */
    double absoluteGap = measuredNetworkCost - minimumNetworkCost;
    if (absoluteGap == 0) {
      gap = absoluteGap;
      return gap;
    }

    if (!Precision.positive(measuredNetworkCost)) {
      LOGGER.severe(String.format("Measured network cost (%.2f) needs to be positive to compute gap, this is not the case", measuredNetworkCost));
      return -1;
    }

    /* regular non-zero measured cost */
    gap = absoluteGap / measuredNetworkCost;
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
  public LinkBasedRelativeDualityGapFunction clone() {
    return new LinkBasedRelativeDualityGapFunction(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> collectSettingsAsKeyValueMap() {
    return null;
  }

}

package org.planit.gap;

import org.planit.utils.id.IdGroupingToken;

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
  protected double gap = Double.POSITIVE_INFINITY;

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
  public void increaseMeasuredNetworkCost(double value) {
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
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double computeGap() {
    gap = (measuredNetworkCost - minimumNetworkCost) / measuredNetworkCost;
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

}

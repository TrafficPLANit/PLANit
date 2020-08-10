package org.planit.gap;

/**
 * Gap function based on the work of Bovy and Jansen (1983) who take the different between the current system travel time and the system travel time if all flow were to be assigned
 * to the shortest paths, divided by the system travel time
 * 
 * @author markr
 *
 */
public class LinkBasedRelativeDualityGapFunction extends GapFunction {

  /**
   * Constructor
   * 
   * @param stopCriterion StopCriterion object being used
   */
  public LinkBasedRelativeDualityGapFunction(StopCriterion stopCriterion) {
    super(stopCriterion);
  }

  /**
   * Current system cost as it stands
   */
  protected double measuredNetworkCost = 0;

  /**
   * Represents the total cost if all flow were to be diverted to the shortest paths for all origin-destination pairs
   */
  protected double minimumNetworkCost = 0;

  /**
   * Gap
   */
  protected double gap = Double.POSITIVE_INFINITY;

  /**
   * Compute the gap
   * 
   * @return the gap for the current iteration
   */
  public double computeGap() {
    gap = (measuredNetworkCost - minimumNetworkCost) / measuredNetworkCost;
    return gap;
  }

  /**
   * Return the actual system travel time
   * 
   * @return the actual system travel time
   */
  public double getMeasuredNetworkCost() {
    return measuredNetworkCost;
  }

  /**
   * Increase system cost, i.e. compute it exogenously
   * 
   * @param increaseValue increase in actualSystemTravelTime for this iteration
   */
  public void increaseMeasuredNetworkCost(double increaseValue) {
    measuredNetworkCost += increaseValue;
  }

  /**
   * Increase convexity bound cost, i.e. compute it exogenously
   * 
   * @param increaseMinimumSystemCost the increase in minimum system cost
   */
  public void increaseConvexityBound(double increaseMinimumSystemCost) {
    minimumNetworkCost += increaseMinimumSystemCost;
  }

  /**
   * Reset system travel time and convexity bound to zero
   */
  public void reset() {
    this.measuredNetworkCost = 0;
    this.minimumNetworkCost = 0;
  }

  /**
   * Return the gap for the current iteration
   * 
   * @return the gap for the current iteration
   */
  @Override
  public double getGap() {
    return gap;
  }

}

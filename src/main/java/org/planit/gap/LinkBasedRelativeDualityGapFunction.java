package org.planit.gap;

/**
 * Gap function based on the work of Bovy and Jansen (1983) who take the
 * different between the current system travel time and the system travel time
 * if all flow were to be assigned to the shortest paths, divided by the system
 * travel time
 * 
 * @author markr
 *
 */
public class LinkBasedRelativeDualityGapFunction extends GapFunction {

  /**
   * Constructor
   * 
   * @param stopCriterion
   *          StopCriterion object being used
   */
  public LinkBasedRelativeDualityGapFunction(StopCriterion stopCriterion) {
    super(stopCriterion);
  }

  /**
   * Current system travel time as it stands
   */
  protected double actualSystemTravelTime = 0;

  /**
   * Represents the total travel time if all flow were to be diverted to the
   * shortest paths for all origin-destination pairs
   */
  protected double minimumSystemTravelTime = 0;

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
    gap = (actualSystemTravelTime - minimumSystemTravelTime) / actualSystemTravelTime;
    return gap;
  }

  /**
   * Return the actual system travel time
   * 
   * @return the actual system travel time
   */
  public double getActualSystemTravelTime() {
    return actualSystemTravelTime;
  }

  /**
   * Increase system travel time, i.e. compute it exogenously
   * 
   * @param increaseValue
   *          increase in actualSystemTravelTime for this iteration
   */
  public void increaseActualSystemTravelTime(double increaseValue) {
    actualSystemTravelTime += increaseValue;
  }

  /**
   * Increase convexity bound travel time, i.e. compute it exogenously
   * 
   * @param increaseMinimumSystemTravelTime
   *          the increase in minimum system travel time
   */
  public void increaseConvexityBound(double increaseMinimumSystemTravelTime) {
    minimumSystemTravelTime += increaseMinimumSystemTravelTime;
  }

  /**
   * Reset system travel time and convexity bound to zero
   */
  public void reset() {
    this.actualSystemTravelTime = 0;
    this.minimumSystemTravelTime = 0;
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

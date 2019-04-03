package org.planit.gap;

/**
 * Gap function based on the work of Bovy and Jansen (1983) who take the different between the current system
 * travel time and the system travel time if all flow were to be assigned to the shortest paths, divided by the 
 * system travel time
 * @author markr
 *
 */
public class LinkBasedRelativeDualityGapFunction extends GapFunction {
	
	/** constructor
	 * @param stopCriterion
	 */
	public LinkBasedRelativeDualityGapFunction(StopCriterion stopCriterion) {
		super(stopCriterion);
	}

	/**
	 * Current system travel time as it stands
	 */
	protected double actualSystemTravelTime = 0;
	
	/**
	 * represents the total travel time if all flow were to be diverted to the shortest paths for all origin-destination pairs
	 */
	protected double minimumSystemTravelTime = 0;
	
	/**
	 * gap
	 */
	protected double gap = Double.POSITIVE_INFINITY;
	
	
	/** Compute the gap
	 * 
	 * @see org.planit.gap.GapFunction#computeGap()
	 */
	public double computeGap(){
		gap = (actualSystemTravelTime - minimumSystemTravelTime) / actualSystemTravelTime;
		return gap; 
	}
	
	public double getActualSystemTravelTime() {
		return actualSystemTravelTime;
	}
	
	/** increase system travel time, i.e. compute it exogenously 
	 * @param increaseValue
	 */
	public void increaseActualSystemTravelTime(double increaseValue) {
		actualSystemTravelTime += increaseValue;
	}	
	
	/** increase convexity bound travel time, i.e. compute it exogenously 
	 * @param minimumSystemTravelTime
	 */
	public void increaseConvexityBound(double increaseMinimumSystemTravelTime) {
		minimumSystemTravelTime += increaseMinimumSystemTravelTime;
	}
	
	/**
	 * reset system travel time and convexity bound to zero
	 */
	public void reset() {
		this.actualSystemTravelTime = 0;
		this.minimumSystemTravelTime = 0;
	}

	@Override
	public double getGap() {
		return gap;
	}


}

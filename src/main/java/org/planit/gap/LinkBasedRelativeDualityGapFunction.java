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
	@Override
	public double computeGap(){
		gap = (actualSystemTravelTime - minimumSystemTravelTime) / actualSystemTravelTime;
//		System.out.println("actualSystemTravelTime = " + actualSystemTravelTime + " minimumSystemTravelTime = " + minimumSystemTravelTime);
		return gap; 
	}
	
	/** Set system travel time directly
	 * @param systemTravelTime
	 */
	public void setSystemTravelTime(double actualSystemTravelTime) {
		this.actualSystemTravelTime = actualSystemTravelTime;
	}
	
	public double getSystemTravelTime() {
		return actualSystemTravelTime;
	}
	
	/** increase system travel time, i.e. compute it exogenously 
	 * @param increaseValue
	 */
	public void increaseSystemTravelTime(double increaseValue) {
		actualSystemTravelTime += increaseValue;
	}	
	
	/** Endogenously compute the system travel time based on network segment costs and flows
	 * @param networkSegmentCosts
	 * @param networkSegmentFlows
	 * @param numberOfNetworkSegments
	 */
//	public void updateSystemTravelTime(double[] networkSegmentCosts, double[] networkSegmentFlows, int numberOfNetworkSegments) {
//		for(int index=0;index<numberOfNetworkSegments;++index) {
//			actualSystemTravelTime += networkSegmentCosts[index]*networkSegmentFlows[index];
//		}
//	}	
	
	/** Set convexity bound travel time directly
	 * @param systemTravelTime
	 */	
	public void setConvexityBound(double minimumSystemTravelTime) {
		this.minimumSystemTravelTime = minimumSystemTravelTime;
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

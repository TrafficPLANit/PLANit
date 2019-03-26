package org.planit.gap;

/**
 * StopCrtierion class. In its base form we only provide an epsilon value. However by deriving from this class
 * additional citeria can be added
 * @author markr
 *
 */
public class StopCriterion {
	
	/**
	 * Default Epsilon in case it is not set by user
	 */
	public final double DEFAULT_EPSILON = 0.01;

	/**
	 * Chosen epsilon for stop criterion
	 */
	protected final double epsilon;
	
	/** constructor
	 * @param epsilon
	 */
	public StopCriterion(double epsilon){
		this.epsilon = epsilon;
	}
	
	/**
	 * Default constructor
	 */
	public StopCriterion() {
		this.epsilon = DEFAULT_EPSILON;
	}

	/** check if converged based on the gap and the internal information
	 * @param gap
	 * @param iterationIndex
	 * @return true, if gap is smaller than criterion, false otherwise
	 */
	public boolean hasConverged(double gap, int iterationIndex) {
		return (Math.abs(gap) < epsilon);
	}
}

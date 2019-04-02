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
	public final double DEFAULT_EPSILON = 0.00;
	public final int MAX_ITERATIONS = 500;
	private int maxIterations;
	

	/**
	 * Chosen epsilon for stop criterion
	 */
	private double epsilon;
	
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
		this.maxIterations = MAX_ITERATIONS;
	}

	
	/** check if converged based on the gap and the internal information
	 * @param gap
	 * @param iterationIndex
	 * @return true, if gap is smaller than criterion, false otherwise
	 */
	public boolean hasConverged(double gap, int iterationIndex) {
		if (iterationIndex == maxIterations)
			return true;
		return (Math.abs(gap) < epsilon);
	}

	public int getMaxIterations() {
		return maxIterations;
	}

	public void setMaxIterations(int maxIterations) {
		this.maxIterations = maxIterations;
	}

	public double getEpsilon() {
		return epsilon;
	}

	public void setEpsilon(double epsilon) {
		this.epsilon = epsilon;
	}
}

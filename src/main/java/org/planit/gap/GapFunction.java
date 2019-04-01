package org.planit.gap;

/**
 * Interface for computing gap functions
 * @author markr
 *
 */
public abstract class GapFunction {
	
	/**
	 * the stopCriterion to use
	 */
	protected final StopCriterion stopCriterion;
	
	/** Constructor
	 * @param stopCriterion
	 */
	public GapFunction(StopCriterion stopCriterion) {
		this.stopCriterion = stopCriterion;
	}

	/** Compute the gap
	 * 
	 * @return gap
	 */
	public abstract double computeGap();
	
	/** collect the computed gap
	 * @return
	 */
	public abstract double getGap();
	
	/** Verify if converged
	 * @param iterationIndex
	 * @return true, if stopcriterion is met, false otherwise
	 */
	public boolean hasConverged(int iterationIndex) {
		return stopCriterion.hasConverged(getGap(), iterationIndex);
	}
	
}

package org.planit.sdinteraction.smoothing;

import org.planit.trafficassignment.TrafficAssignmentComponent;

/** Smoothing class to smooth data, such as path flows or other types of flows or traffic data between iterations
 * @author markr
 *
 */
public abstract class Smoothing extends TrafficAssignmentComponent<Smoothing>{

	/** Determine the stepsize for the passed in iteraction
	 * @param iterationIndex
	 */
	public abstract void update(int iterationIndex);
	
	/** Apply smoothing based on the current step size
	 * @param previousValue
	 * @param proposedValue
	 * @return smoothedValue
	 */
	public abstract double applySmoothing(double previousValue, double proposedValue);

	/** Apply smoothing based on the current step size
	 * @param previousValues
	 * @param proposedValues
	 * @param numberOfValues
	 * @return smoothedValues
	 */
	public abstract double[] applySmoothing(double[] previousValues, double[] proposedValues, int numberOfValues);
	
	public Smoothing() {
		super();
	}
	
}

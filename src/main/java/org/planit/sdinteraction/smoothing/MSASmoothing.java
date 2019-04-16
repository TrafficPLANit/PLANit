package org.planit.sdinteraction.smoothing;

import java.util.logging.Logger;

/**
 * MSA smoothing object
 * 
 * @author gman6028
 *
 */
public class MSASmoothing extends Smoothing {

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(MSASmoothing.class.getName());
        
 /**
  * Step size
  */
	protected double stepSize = 1;
	
	public MSASmoothing() {
		super();
	}
	
	/** update stepSize to 1/iterationIndex
	 * @see org.planit.sdinteraction.smoothing.Smoothing#update(int)
	 */
	@Override
	public void update(int iterationIndex) {
		this.stepSize = 1.0/(iterationIndex + 1);
	}
	
	/* (non-Javadoc)
	 * @see org.planit.sdinteraction.smoothing.Smoothing#applySmoothing(double, double)
	 */
	@Override
	public double applySmoothing(double previousValue, double proposedValue) {
		return (1-stepSize)*previousValue + stepSize*proposedValue;
	}
	
	/* (non-Javadoc)
	 * @see org.planit.sdinteraction.smoothing.Smoothing#applySmoothing(double[], double[], int)
	 */
	@Override 
	public double[] applySmoothing(double[] previousValues, double[] proposedValues, int numberOfValues) {
		double[] smoothedValues = new double[numberOfValues];
		for (int i = 0; i < numberOfValues; ++i) {
			smoothedValues[i] = (1-stepSize)*previousValues[i] + stepSize*proposedValues[i]; 
		}
		return smoothedValues;
	}
	
}

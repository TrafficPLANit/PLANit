package org.planit.sdinteraction.smoothing;

import java.util.logging.Logger;
import org.planit.trafficassignment.TrafficAssignmentComponent;

/**
 * Smoothing class to smooth data, such as path flows or other types of flows or
 * traffic data between iterations
 * 
 * @author markr
 *
 */
public abstract class Smoothing extends TrafficAssignmentComponent<Smoothing> {

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(Smoothing.class.getName());
    
    /**
     * Base constructor
     */
    public Smoothing() {
        super();
    }    

    /**
     * Determine the stepsize for the passed in iteraction
     * 
     * @param iterationIndex
     *            index of current iteration
     */
    public abstract void update(int iterationIndex);

    /**
     * Apply smoothing based on the current step size
     * 
     * @param previousValue
     *            previous value
     * @param proposedValue
     *            proposed value
     * @return smoothedValue smoothed value
     */
    public abstract double applySmoothing(double previousValue, double proposedValue);

    /**
     * Apply smoothing based on the current step size
     * 
     * @param previousValues
     *            array of previous values
     * @param proposedValues
     *            array of proposed values
     * @param numberOfValues
     *            number of proposed values
     * @return smoothedValues array of smoothed values
     */
    public abstract double[] applySmoothing(double[] previousValues, double[] proposedValues, int numberOfValues);

}

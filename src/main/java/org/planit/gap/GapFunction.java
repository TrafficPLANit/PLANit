package org.planit.gap;

import java.util.logging.Logger;

/**
 * Interface for computing gap functions
 * 
 * @author markr
 *
 */
public abstract class GapFunction {

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(GapFunction.class.getName());

    /**
     * The stopCriterion to use
     */
    protected final StopCriterion stopCriterion;

    /**
     * Constructor
     * 
     * @param stopCriterion
     *            the StopCriterion object to be used
     */
    public GapFunction(StopCriterion stopCriterion) {
        this.stopCriterion = stopCriterion;
    }

    /**
     * Returns the gap for the current iteration
     * 
     * @return gap for current iteration
     */
    public abstract double getGap();
    
    /**
     * Reset the gap function
     */
    public abstract void reset();    

    /**
     * Verify if algorithm has converged
     * 
     * @param iterationIndex
     *            the index of the current iteration
     * @return true if stopping criterion has been met, false otherwise
     */
    public boolean hasConverged(int iterationIndex) {
        return stopCriterion.hasConverged(getGap(), iterationIndex);
    }


    /**
     * Return the StopCriterion object
     * 
     * @return StopCriterion object being used
     */
    public StopCriterion getStopCriterion() {
        return stopCriterion;
    }

}

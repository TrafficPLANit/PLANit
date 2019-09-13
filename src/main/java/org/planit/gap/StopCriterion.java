package org.planit.gap;

/**
 * StopCriterion class. In its base form we only provide an epsilon value.
 * However by deriving from this class additional citeria can be added
 * 
 * @author markr
 *
 */
public class StopCriterion {

    /**
     * Default Epsilon in case it is not set by user
     */
    public static final double DEFAULT_EPSILON = 0.001;

    /**
     * Default maximum number of iterations in case it is not set by user
     */
    public static final int DEFAULT_MAX_ITERATIONS = 500;

    /**
     * Chosen maximum number of iterations
     */
    private int maxIterations = DEFAULT_MAX_ITERATIONS;
    
    /**
     * Chosen epsilon for stop criterion
     */
    private double epsilon = DEFAULT_EPSILON;

    /**
     * Check if converged based on the gap and the internal information
     * 
     * @param gap
     *            gap for the current iteration
     * @param iterationIndex
     *            index of current iteration
     * @return true if gap is smaller than criterion, false otherwise
     */
    public boolean hasConverged(double gap, int iterationIndex) {
        if (iterationIndex == maxIterations)
            return true;
        return (Math.abs(gap) < epsilon);
    }

    /**
     * Return the maximum allowable number of iterations
     * 
     * @return the maximum allowable number of iterations
     */
    public int getMaxIterations() {
        return maxIterations;
    }

    /**
     * Set the maximum allowable number of iterations
     * 
     * @param maxIterations
     *            the maximum allowable number of iterations
     */
    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    /**
     * Return the epsilon of this stopping criterion
     * 
     * @return the epsilon of this stopping criterion
     */
    public double getEpsilon() {
        return epsilon;
    }

    /**
     * Set the epsilon of this stopping criterion
     * 
     * @param epsilon
     *            the epsilon of this stopping criterion
     */
    public void setEpsilon(double epsilon) {
        this.epsilon = epsilon;
    }
}

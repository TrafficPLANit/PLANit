package org.planit.data;

/**
 * General simulation data that only are available during simulation
 * 
 */
public abstract class SimulationData {

    /**
     * simulation variable
     */
    private boolean converged = false; // general

    /**
     * Iteration index, tracking the iteration during execution
     */
    private int iterationIndex = 0; // general

    /**
     * Increment iteration index by one
     */
    public void incrementIterationIndex() {
        ++this.iterationIndex;
    }

    // getters - setters

    /**
     * Test whether the traffic assignment has converged
     * 
     * @return true if the assignment has converged, false otherwise
     */
    public boolean isConverged() {
        return converged;
    }

    /**
     * Set whether the traffic assignment has converged
     * 
     * @param converged
     *            boolean, true if assignment
     */
    public void setConverged(boolean converged) {
        this.converged = converged;
    }

    public int getIterationIndex() {
        return iterationIndex;
    }

    public void setIterationIndex(int iterationIndex) {
        this.iterationIndex = iterationIndex;
    }

}
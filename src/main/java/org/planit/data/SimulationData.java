package org.planit.data;

/**
 * General simulation data that only are available during simulation
 * 
 */
public abstract class SimulationData {

  /**
   * Iteration index, tracking the iteration during execution
   */
  private int iterationIndex = 0; // general

  /**
   * Increment iteration index by one
   */
  public void incrementIterationIndex() {
    iterationIndex++;
  }

  // getters - setters

  /**
   * Returns the current iteration index
   * 
   * @return the current iteration index
   */
  public int getIterationIndex() {
    return iterationIndex;
  }

  /**
   * Set the current iteration index
   * 
   * @param iterationIndex the current iteration index
   */
  public void setIterationIndex(int iterationIndex) {
    this.iterationIndex = iterationIndex;
  }

}

package org.goplanit.assignment;

/**
 * General simulation data that only are available during simulation
 * 
 */
public class SimulationData {

  /**
   * Iteration index, tracking the iteration during execution
   */
  private int iterationIndex;

  /**
   * Constructor
   */
  public SimulationData() {
    this.reset();
  }

  /**
   * Copy constructor
   * 
   * @param simulationData to copy
   */
  protected SimulationData(final SimulationData simulationData) {
    super();
    this.iterationIndex = simulationData.iterationIndex;
  }

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

  /**
   * {@inheritDoc}
   */
  public SimulationData shallowClone() {
    return new SimulationData(this);
  }

  /**
   * reset to initial state
   */
  public void reset() {
    this.iterationIndex = 0;
  }

  /**
   * Verify if we're in the first iteration (not considering initial solution), so we're checking against the iteration
   * index being 1.
   *
   * @return true when iteration==1, false otherwise
   */
  public boolean isFirstIteration() {
    return iterationIndex == 1;
  }
}

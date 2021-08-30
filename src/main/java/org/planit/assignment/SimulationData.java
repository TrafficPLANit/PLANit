package org.planit.assignment;

/**
 * General simulation data that only are available during simulation
 * 
 */
public class SimulationData implements Cloneable {

  /**
   * Iteration index, tracking the iteration during execution
   */
  private int iterationIndex;

  /**
   * Constructor
   */
  public SimulationData() {
    this.iterationIndex = 0;
  }

  /**
   * Copy constructor
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
  @Override
  public SimulationData clone() {
    return new SimulationData(this);
  }

}

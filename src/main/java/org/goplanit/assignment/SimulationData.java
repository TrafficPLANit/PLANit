package org.goplanit.assignment;

import org.goplanit.utils.time.RunTimesTracker;

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
   * Provide support to track run times
   */
  private RunTimesTracker runTimesTracker = RunTimesTracker.create();

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
   * @param deepCopy flag
   */
  protected SimulationData(final SimulationData simulationData, boolean deepCopy) {
    super();
    this.iterationIndex = simulationData.iterationIndex;
    if(!deepCopy) {
      this.runTimesTracker = simulationData.runTimesTracker.shallowClone();
    }else{
      this.runTimesTracker = simulationData.runTimesTracker.deepClone();
    }
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
   * Access to run times tracker
   */
  public RunTimesTracker getRunTimesTracker(){
    return runTimesTracker;
  }

  /**
   * perform shallow clone
   */
  public SimulationData shallowClone() {
    return new SimulationData(this, false);
  }

  /**
   * perform deep clone
   */
  public SimulationData deepClone() {
    return new SimulationData(this, true);
  }

  /**
   * reset to initial state
   */
  public void reset() {
    this.iterationIndex = 0;
    runTimesTracker.reset();
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

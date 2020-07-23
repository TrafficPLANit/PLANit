package org.planit.output.configuration;

/**
 * Class containing the general output configuration and the type specific configurations for some traffic assignment
 * 
 * @author markr
 *
 */
public class OutputConfiguration {

  /**
   * Default for persisting final iteration
   */
  private static final boolean PERSIST_ONLY_FINAL_ITERATION = true;

  /**
   * Default for persisting zero flows
   */
  private static final boolean PERSIST_ZERO_FLOW = false;

  /**
   * persisting final iteration only or not
   */
  protected boolean persistOnlyFinalIteration = PERSIST_ONLY_FINAL_ITERATION;

  /**
   * persisting zero flows or not
   */
  protected boolean persistZeroFlow = PERSIST_ZERO_FLOW;

  /**
   * Base constructor
   */
  public OutputConfiguration() {
  }

  // getters - setters

  /**
   * Set whether only the final iteration will be recorded (default is true)
   * 
   * @param persistOnlyFinalIteration true if only the final iteration will be recorded
   */
  public void setPersistOnlyFinalIteration(boolean persistOnlyFinalIteration) {
    this.persistOnlyFinalIteration = persistOnlyFinalIteration;
  }

  /**
   * Returns whether only the final iteration will be recorded (default is true)
   * 
   * @return true if only the final iteration will be recorded, false otherwise
   */
  public boolean isPersistOnlyFinalIteration() {
    return persistOnlyFinalIteration;
  }

  /**
   * Set whether links and paths with zero flow should be record (default is false)
   * 
   * @param persistZeroFlow if true links and paths with zero flow are recorded
   */
  public void setPersistZeroFlow(boolean persistZeroFlow) {
    this.persistZeroFlow = persistZeroFlow;
  }

  /**
   * Verify if we are persisting zero flow or not (default is false)
   * 
   * @return true when persisting zero flows, false otherwise
   */
  public boolean isPersistZeroFlow() {
    return persistZeroFlow;
  }

}
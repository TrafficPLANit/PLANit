package org.planit.output.configuration;

import org.planit.output.OutputManager;
import org.planit.output.enums.OutputType;

/**
 * Class containing the general output configuration and the type specific
 * configurations for some traffic assignment
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
   * The output manager
   */
  private OutputManager outputManager;

  /**
   * persisting final iteration only or not
   */
  protected boolean persistOnlyFinalIteration = PERSIST_ONLY_FINAL_ITERATION;
  
  /**
   * Base constructor
   * 
   * @param outputManager the manager for this configuration
   */
  public OutputConfiguration(OutputManager outputManager) {
    this.outputManager = outputManager;
  }

  // getters - setters

  /**
   * Set whether only the final iteration will be recorded
   * 
   * @param persistOnlyFinalIteration true if only the final iteration will be recorded
   */
  public void setPersistOnlyFinalIteration(boolean persistOnlyFinalIteration) {
    this.persistOnlyFinalIteration = persistOnlyFinalIteration;
  }

  /**
   * Returns whether only the final iteration will be recorded
   * 
   * @return true if only the final iteration will be recorded, false otherwise
   */
  public boolean isPersistOnlyFinalIteration() {
    return persistOnlyFinalIteration;
  }

  /**
   * Retrieve the output type configuration for a specified output type
   * 
   * @param outputType the specified output type
   * @return the output type configuration
   */
  public OutputTypeConfiguration getOutputTypeConfiguration(OutputType outputType) {
    return outputManager.getOutputTypeConfiguration(outputType);
  }

  /**
   * Returns whether a specified output type has been activated
   * 
   * @param outputType the specified output type
   * @return true if the specified output type has been activated, false otherwise
   */
  public boolean isOutputTypeActive(OutputType outputType) {
    return outputManager.isOutputTypeActive(outputType);
  }

  /**
   * Set whether links and paths with zero flow should be record
   * 
   * @param recordZeroFlow if true links and paths with zero flow are recorded
   */
  public void setRecordZeroFlow(boolean recordZeroFlow) {
    outputManager.setRecordZeroFlow(recordZeroFlow);
  }

}
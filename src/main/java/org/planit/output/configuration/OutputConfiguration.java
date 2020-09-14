package org.planit.output.configuration;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.planit.output.enums.OutputType;
import org.planit.utils.exceptions.PlanItException;

/**
 * Class containing the general output configuration and the type specific configurations for some traffic assignment
 * 
 * @author markr
 *
 */
public class OutputConfiguration {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(OutputConfiguration.class.getCanonicalName());

  /**
   * persisting final iteration only or not
   */
  protected boolean persistOnlyFinalIteration = PERSIST_ONLY_FINAL_ITERATION;

  /**
   * persisting zero flows or not
   */
  protected boolean persistZeroFlow = PERSIST_ZERO_FLOW;

  /**
   * output configurations per output type
   */
  protected final Map<OutputType, OutputTypeConfiguration> outputTypeConfigurations;

  /**
   * Default for persisting final iteration
   */
  public static final boolean PERSIST_ONLY_FINAL_ITERATION = true;

  /**
   * Default for persisting zero flows
   */
  public static final boolean PERSIST_ZERO_FLOW = false;

  /**
   * Base constructor
   */
  public OutputConfiguration() {
    outputTypeConfigurations = new HashMap<OutputType, OutputTypeConfiguration>();
  }

  /**
   * Collect the activated output types
   * 
   * @return activated output types
   */
  public Set<OutputType> getActivatedOutputTypes() {
    return outputTypeConfigurations.keySet();
  }

  /**
   * Collect all output type configuration activated on this instance
   * 
   * @return activated output type configruation
   */
  public Collection<OutputTypeConfiguration> getActivatedOutputTypeConfigurations() {
    return outputTypeConfigurations.values();
  }

  /**
   * Collect the output type configuration for the given type if it is activated
   * 
   * @param outputType to collect
   * @return output type configuration when active, otherwise null is returned
   */
  public OutputTypeConfiguration getOutputTypeConfiguration(OutputType outputType) {
    return outputTypeConfigurations.get(outputType);
  }

  /**
   * remove the output type from the list of active types, its configuration is also nullified, this type will no longer be persisted
   * 
   * @param outputType to deregister
   */
  public void deregisterOutputTypeConfiguration(OutputType outputType) {
    outputTypeConfigurations.remove(outputType);
  }

  /**
   * Verify ig the output type is activated or not
   * 
   * @param outputType to verify
   * @return true when active, false otherwise
   */
  public boolean isOutputTypeActive(OutputType outputType) {
    return outputTypeConfigurations.containsKey(outputType);
  }

  /**
   * Factory method to create an output configuration for a given type
   * 
   * @param outputType the output type to register the configuration for
   * @return outputTypeconfiguration the output type configuration that has been newly registered
   * @throws PlanItException thrown if there is an error
   */
  public OutputTypeConfiguration createAndRegisterOutputTypeConfiguration(OutputType outputType) throws PlanItException {
    OutputTypeConfiguration createdOutputTypeConfiguration = null;
    switch (outputType) {
    case LINK:
      createdOutputTypeConfiguration = new LinkOutputTypeConfiguration();
      break;
    case OD:
      createdOutputTypeConfiguration = new ODOutputTypeConfiguration();
      break;
    case PATH:
      createdOutputTypeConfiguration = new PathOutputTypeConfiguration();
      break;
    default:
      LOGGER.warning(outputType.value() + " has not been defined yet.");
    }

    if (createdOutputTypeConfiguration != null) {
      outputTypeConfigurations.put(outputType, createdOutputTypeConfiguration);
    }
    return createdOutputTypeConfiguration;
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
package org.goplanit.output;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.output.adapter.OutputAdapter;
import org.goplanit.output.adapter.OutputTypeAdapter;
import org.goplanit.output.configuration.OutputConfiguration;
import org.goplanit.output.configuration.OutputTypeConfiguration;
import org.goplanit.output.enums.OutputType;
import org.goplanit.output.formatter.OutputFormatter;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.time.TimePeriod;

/**
 * Base class for output writers containing basic functionality regarding all things output
 * 
 * @author markr
 *
 */
public class OutputManager {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(OutputManager.class.getCanonicalName());

  /**
   * The overall output configuration instance
   */
  private OutputConfiguration outputConfiguration;

  /**
   * Registered output formatters
   */
  private List<OutputFormatter> outputFormatters;

  /**
   * Output Adapter object
   */
  private OutputAdapter outputAdapter;

  /**
   * Base constructor of Output writer
   * 
   */
  public OutputManager() {
    outputFormatters = new ArrayList<OutputFormatter>();
    outputConfiguration = new OutputConfiguration();
  }

  /**
   * Allows the output manager to initialize itself and any of its registered output formatters to prepare before the simulation starts
   * 
   * @param runId traffic assignment run id
   * @throws PlanItException thrown if there is an error
   */
  public void initialiseBeforeSimulation(long runId) throws PlanItException {
    for (OutputFormatter outputFormatter : outputFormatters) {
      outputFormatter.initialiseBeforeSimulation(outputConfiguration, runId);
    }
  }

  /**
   * Allows the output manager to finalise itself and any of its registered output formatters to after the simulation ended
   * 
   * @throws PlanItException thrown if there is an error
   */
  public void finaliseAfterSimulation() throws PlanItException {
    for (OutputFormatter outputFormatter : outputFormatters) {
      outputFormatter.finaliseAfterSimulation(outputConfiguration, outputAdapter);
    }
  }

  /**
   * Persist the output data for all registered output types
   * 
   * @param timePeriod the current time period whose results are being saved
   * @param modes      Set of modes for the current assignment
   * @param converged  true if the assignment has converged
   * @throws PlanItException thrown if there is an error
   */
  public void persistOutputData(final TimePeriod timePeriod, final Set<Mode> modes, final boolean converged) throws PlanItException {
    for (OutputType outputType : outputConfiguration.getActivatedOutputTypes()) {
      OutputTypeConfiguration outputTypeConfiguration = outputConfiguration.getOutputTypeConfiguration(outputType);
      if (converged || !outputConfiguration.isPersistOnlyFinalIteration()) {
        for (OutputFormatter outputFormatter : outputFormatters) {
          if (converged || outputFormatter.canHandleMultipleIterations()) {
            outputFormatter.persist(timePeriod, modes, outputConfiguration, outputTypeConfiguration, outputAdapter);
          }
        }
      }
    }
  }

  /**
   * Verify if anything is actually persisted when {@link #persistOutputData(TimePeriod, Set, boolean)} is invoked given the underlying configuration
   * 
   * @param timePeriod to verify for
   * @param modes      to verify for
   * @param converged  true if the assignment has converged
   * @return true when anything is persisted, false otherwise
   */
  public boolean isAnyOutputPersisted(final TimePeriod timePeriod, final Set<Mode> modes, final boolean converged) {
    if (converged || !outputConfiguration.isPersistOnlyFinalIteration()) {
      for (OutputFormatter outputFormatter : outputFormatters) {
        if (converged || outputFormatter.canHandleMultipleIterations()) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Factory method to create an output configuration for a given type
   * 
   * @param outputType the output type to register the configuration for
   * @return outputTypeconfiguration the output type configuration that has been newly registered
   */
  public OutputTypeConfiguration createAndRegisterOutputTypeConfiguration(OutputType outputType){
    return outputConfiguration.createAndRegisterOutputTypeConfiguration(outputType);
  }

  /**
   * Remove the output type configuration for a specified output type
   * 
   * @param outputType the output type whose configuration is to be deregistered
   */
  public void deregisterOutputTypeConfiguration(OutputType outputType) {
    outputConfiguration.deregisterOutputTypeConfiguration(outputType);
  }

  /**
   * Register the OutputTypeAdapter for a given output type
   * 
   * @param outputTypeAdapter the OutputTypeAdapte to be registered
   */
  public void registerOutputTypeAdapter(OutputTypeAdapter outputTypeAdapter) {
    if (outputAdapter == null) {
      LOGGER.warning(String.format("Output adapter not available to register type on, ignored registration of %s instance", outputTypeAdapter.getClass().getCanonicalName()));
      return;
    }
    if (outputTypeAdapter == null) {
      LOGGER.warning("Output type adapter that is registered is null, ignored");
      return;
    }
    outputAdapter.registerOutputTypeAdapter(outputTypeAdapter.getOutputType(), outputTypeAdapter);
  }

  /**
   * Deregister the output adapter for a specified output type (if it exists)
   * 
   * @param outputType the output type whose adapter is to be deregistered
   */
  public void deregisterOutputTypeAdapter(OutputType outputType) {
    if (outputAdapter != null) {
      outputAdapter.deregisterOutputTypeAdapter(outputType);
    }
  }

  // getters - setters

  /**
   * Get the OutputConfiguration object
   * 
   * @return the OutputConfiguration object being used
   */
  public OutputConfiguration getOutputConfiguration() {
    return outputConfiguration;
  }

  /**
   * Register the output formatter on the output manager.
   * 
   * Whenever something is persisted it will be delegated to the registered formatters
   * 
   * @param outputFormatter OutputFormatter to be registered
   */
  public void registerOutputFormatter(OutputFormatter outputFormatter) {
    outputFormatters.add(outputFormatter);
  }

  /**
   * Remove an output formatter which has previously been registered
   * 
   * @param outputFormatter output formatter to be removed
   */
  public void unregisterOutputFormatter(OutputFormatter outputFormatter) {
    outputFormatters.remove(outputFormatter);
  }

  /**
   * Returns the list of currently registered OutputFormatter objects for a specified output type
   * 
   * @return List of registered OutputFormatter objects
   */
  public List<OutputFormatter> getOutputFormatters() {
    return outputFormatters;
  }

  /**
   * Verify if the given output type is already activated or not
   * 
   * @param outputType OutputType object to be checked
   * @return true if active false otherwise
   */
  public boolean isOutputTypeActive(OutputType outputType) {
    return outputConfiguration.isOutputTypeActive(outputType);
  }

  /**
   * Collect the output type configuration for the given type
   * 
   * @param outputType OutputType to collect the output type configuration for
   * @return output type configuration registered, if not registered null is returned and a warning is logged
   */
  public OutputTypeConfiguration getOutputTypeConfiguration(OutputType outputType) {
    return outputConfiguration.getOutputTypeConfiguration(outputType);
  }

  /**
   * Returns a List of registered output type configuration objects
   * 
   * @return a List of registered output type configuration objects
   */
  public Collection<OutputTypeConfiguration> getRegisteredOutputTypeConfigurations() {
    return outputConfiguration.getActivatedOutputTypeConfigurations();
  }

  /**
   * Returns a List of registered output types
   * 
   * @return a List of registered output types
   */
  public Set<OutputType> getRegisteredOutputTypes() {
    return outputConfiguration.getActivatedOutputTypes();
  }

  /**
   * Based on the passed in assignment, create the necessary output adapters
   * 
   * @param trafficAssignment we are creating adapters for
   */
  public void initialiseOutputAdapters(TrafficAssignment trafficAssignment) {
    /* main assignment adapter */
    this.outputAdapter = new OutputAdapter(trafficAssignment);
    /* sub type output adapters */
    for (OutputTypeConfiguration otc : getRegisteredOutputTypeConfigurations()) {
      final OutputTypeAdapter outputTypeAdapter = trafficAssignment.createOutputTypeAdapter(otc.getOutputType());
      registerOutputTypeAdapter(outputTypeAdapter);
    }
  }

  /**
   * Remove all registered formatters and adapters, only retain the configuration
   */
  public void reset() {
    this.outputAdapter.reset();
    this.outputFormatters.clear();
  }

}

package org.planit.output;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.planit.exceptions.PlanItException;
import org.planit.output.adapter.OutputAdapter;
import org.planit.output.configuration.OutputConfiguration;
import org.planit.output.configuration.OutputTypeConfiguration;
import org.planit.output.formatter.OutputFormatter;
import org.planit.time.TimePeriod;
import org.planit.userclass.Mode;

/**
 * Base class for output writers containing basic functionality regarding all
 * things output
 * 
 * @author markr
 *
 */
public class OutputManager {

	private static final Logger LOGGER = Logger.getLogger(OutputManager.class.getName());

	/**
	 * The overall output configuration instance
	 */
	protected OutputConfiguration outputConfiguration;

	/**
	 * registered output formatters
	 */
	protected List<OutputFormatter> outputFormatters;

	/**
	 * Base constructor of Output writer
	 */
	public OutputManager() {
		outputFormatters = new ArrayList<OutputFormatter>();
		outputConfiguration = new OutputConfiguration();
	}
	
	public void setOutputConfiguration(OutputConfiguration outputConfiguration) {
		this.outputConfiguration = outputConfiguration;
	}

	/**
	 * Persist the output data for a given output type pending the configuration
	 * choices made
	 * 
	 * @param timePeriod     the current time period whose results are being saved
	 * @param modes          Set of modes for the current assignment
	 * @param outputType     the current output type
	 * @throws PlanItException thrown if there is an error
	 */
	public void persistOutputData(TimePeriod timePeriod, Set<Mode> modes, OutputType outputType) throws PlanItException {
		if (outputConfiguration.containsOutputTypeConfiguration(outputType)) {
			OutputTypeConfiguration outputTypeConfiguration = outputConfiguration
					.getOutputTypeConfiguration(outputType);
			OutputAdapter outputAdapter = outputTypeConfiguration.getOutputAdapter();
			if ((outputAdapter.isConverged()) || (!outputConfiguration.isPersistOnlyFinalIteration())) {
				for (OutputFormatter outputFormatter : outputFormatters) {
					outputFormatter.persist(timePeriod, modes, outputTypeConfiguration, outputType);
				}
			}
		}
	}

	/**
	 * Factory method to create an output configuration of a given type
	 * 
	 * @param outputType    the output type to register the configuration for
	 * @param outputAdapter the adapter that allows access to the data to persist
	 *                      for the given output type
	 * @return outputConfiguration that has been created
	 * @throws PlanItException thrown if there is an error creating the output type configuration
	 */
	public OutputTypeConfiguration createAndRegisterOutputTypeConfiguration(OutputType outputType,
			OutputAdapter outputAdapter) throws PlanItException {
		return outputConfiguration.createAndRegisterOutputTypeConfiguration(outputType, outputAdapter);
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
	 * Whenever something is persisted it will be delegated to the registered
	 * formatters
	 * 
	 * @param outputFormatter OutputFormatter to be registered
	 */
	public void registerOutputFormatter(OutputFormatter outputFormatter) {
		outputFormatters.add(outputFormatter);
	}

	/**
	 * Returns the list of currently registered OutputFormatter objects
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
		return outputConfiguration.containsOutputTypeConfiguration(outputType);
	}
	
}

package org.planit.output;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.planit.exceptions.PlanItException;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegmentImpl;
import org.planit.output.adapter.OutputAdapter;
import org.planit.output.adapter.OutputTypeAdapter;
import org.planit.output.configuration.LinkOutputTypeConfiguration;
import org.planit.output.configuration.OriginDestinationOutputTypeConfiguration;
import org.planit.output.configuration.OutputConfiguration;
import org.planit.output.configuration.OutputTypeConfiguration;
import org.planit.output.configuration.PathOutputTypeConfiguration;
import org.planit.output.enums.OutputType;
import org.planit.output.formatter.OutputFormatter;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.utils.network.physical.Mode;

/**
 * Base class for output writers containing basic functionality regarding all
 * things output
 * 
 * @author markr
 *
 */
public class OutputManager {
  
  /** the logger */
  private static final Logger LOGGER =  Logger.getLogger(MacroscopicLinkSegmentImpl.class.getCanonicalName());  

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
     * output configurations per output type
     */
    private Map<OutputType, OutputTypeConfiguration> outputTypeConfigurations;

	/**
	 * Base constructor of Output writer
	 * 
	 * @param trafficAssignment the traffic assignment this output manager is managing for
	 */
	public OutputManager(TrafficAssignment trafficAssignment) {
		outputFormatters = new ArrayList<OutputFormatter>();
		outputConfiguration = new OutputConfiguration(this);
		outputTypeConfigurations = new HashMap<OutputType, OutputTypeConfiguration>();
		outputAdapter = new OutputAdapter(trafficAssignment);
	}
	
	/**
	 * Allows the output manager to initialize itself and any of its registered output formatters to prepare before the simulation starts
	 * 
	 *@param runId traffic assignment run id
	 * @throws PlanItException thrown if there is an error
	 */
    public void initialiseBeforeSimulation(long runId) throws PlanItException {
        for (OutputFormatter outputFormatter : outputFormatters) {
             outputFormatter.initialiseBeforeSimulation(outputTypeConfigurations, runId);
        }
    }	
    
    /**
     * Allows the output manager to finalise itself and any of its registered output formatters to after the simulation ended
     * @throws PlanItException thrown if there is an error
     */    
    public void finaliseAfterSimulation() throws PlanItException {
        for (OutputFormatter outputFormatter : outputFormatters) {
            outputFormatter.finaliseAfterSimulation(outputTypeConfigurations);
        }       
    }

	/**
	 * Persist the output data for all registered output types
	 * 
	 * @param timePeriod the current time period whose results are being saved
	 * @param modes      Set of modes for the current assignment
	 * @param converged true if the assignment has converged
	 * @throws PlanItException thrown if there is an error
	 */
	public void persistOutputData(TimePeriod timePeriod, Set<Mode> modes, boolean converged) throws PlanItException {
		for (OutputType outputType : outputTypeConfigurations.keySet()) {
			OutputTypeConfiguration outputTypeConfiguration = outputTypeConfigurations.get(outputType);
			for (OutputFormatter outputFormatter : outputFormatters) {
				if (converged || !outputConfiguration.isPersistOnlyFinalIteration()) {
					if (converged || outputFormatter.canHandleMultipleIterations()) {
						outputFormatter.persist(timePeriod, modes, outputTypeConfiguration, outputAdapter);
					}
				}
			}
		}
	}
	
    /**
     * Factory method to create an output configuration and adapter for a given type
     * 
     * @param outputType  the output type to register the configuration for
     * @param trafficAssignment traffic assignment we are creating this configuration for
     * @return outputTypeconfiguration the output type configuration that has been newly registered
     * @throws PlanItException thrown if there is an error
     */
	public OutputTypeConfiguration createAndRegisterOutputTypeConfiguration(OutputType outputType, TrafficAssignment trafficAssignment) throws PlanItException {
	    OutputTypeConfiguration createdOutputTypeConfiguration = null;
        switch (outputType) {
        case LINK: 
            createdOutputTypeConfiguration = new LinkOutputTypeConfiguration(trafficAssignment);
        break;
        case OD:
            createdOutputTypeConfiguration = new OriginDestinationOutputTypeConfiguration(trafficAssignment);
        break;
        case PATH:
            createdOutputTypeConfiguration = new PathOutputTypeConfiguration(trafficAssignment);
        break;
        default: LOGGER.warning(outputType.value() + " has not been defined yet.");
        }
        
        if(createdOutputTypeConfiguration != null) {
            outputTypeConfigurations.put(outputType, createdOutputTypeConfiguration);   
        }
        return createdOutputTypeConfiguration;
	}
	
	/**
	 * Remove the output type configuration for a specified output type
	 * 
	 * @param outputType the output type whose configuration is to be deregistered
	 */
	public void deregisterOutputTypeConfiguration(OutputType outputType) {
	  outputTypeConfigurations.remove(outputType);
	}
	
	/**
	 * Register the OutputTypeAdapter for a given output type
	 * 
     * @param outputType  the output type to register the output type adapter for
	 * @param outputTypeAdapter the OutputTypeAdapte to be registered
	 */
	public void registerOutputTypeAdapter(OutputType outputType, OutputTypeAdapter outputTypeAdapter) {
		outputAdapter.registerOutputTypeAdapter(outputType, outputTypeAdapter);
	}
	
	/**
	 * Deregister the output adapter for a specified output type
	 * 
	 * @param outputType the output type whose adapter is to be deregistered
	 */
	public void deregisterOutputTypeAdapter(OutputType outputType) {
	  outputAdapter.deregisterOutputTypeAdapter(outputType);
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
        return outputTypeConfigurations.containsKey(outputType);
    }

    /**
     * Collect the output type configuration for the given type
     * 
     * @param outputType
     *            OutputType to collect the output type configuration for
     * @return output type configuration registered, if not registered null is
     *         returned and a warning is logged
     */
    public OutputTypeConfiguration getOutputTypeConfiguration(OutputType outputType) {
        return outputTypeConfigurations.get(outputType);
    }
    
    /**
     * Returns a List of registered output type configuration objects
     * 
     * @return a List of registered output type configuration objects
     */
    public List<OutputTypeConfiguration> getRegisteredOutputTypeConfigurations() {
    	return new ArrayList<OutputTypeConfiguration>(outputTypeConfigurations.values());
    	
    }
    
    /**
     * Returns a List of registered output types
     * 
     * @return a List of registered output types
     */
    public List<OutputType> getRegisteredOutputTypes() {
    	return new ArrayList<OutputType>(outputTypeConfigurations.keySet());
    }

}
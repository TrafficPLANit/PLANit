package org.planit.output.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.planit.exceptions.PlanItException;
import org.planit.logging.PlanItLogger;
import org.planit.output.OutputType;
import org.planit.output.adapter.OutputAdapter;
import org.planit.trafficassignment.TrafficAssignment;

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
     * persisting final iteration only or not
     */
    protected boolean persistOnlyFinalIteration = PERSIST_ONLY_FINAL_ITERATION;

    /**
     * output configurations per output type
     */
    protected final TreeMap<OutputType, OutputTypeConfiguration> outputTypeConfigurations;

    /**
     * Base constructor
     */
    public OutputConfiguration() {
        this.outputTypeConfigurations = new TreeMap<OutputType, OutputTypeConfiguration>();
    }

    /**
     * Factory method to create an output configuration of a given type
     * 
     * @param outputType  the output type to register the configuration for
     * @return outputConfiguration that has been created
     * @throws PlanItException thrown if there is an error
     */
    public void createAndRegisterOutputTypeConfiguration(OutputType outputType, TrafficAssignment trafficAssignment) throws PlanItException {
    	OutputAdapter outputAdapter = trafficAssignment.createOutputAdapter(outputType);
        OutputTypeConfiguration outputTypeConfiguration = null;
        switch (outputType) {
        case LINK: outputTypeConfiguration = new LinkOutputTypeConfiguration(outputAdapter);
        break;
        case OD: outputTypeConfiguration = new OriginDestinationOutputTypeConfiguration(outputAdapter);
        break;
        default: PlanItLogger.warning(outputType.value() + " has not been defined yet.");
       }
        outputTypeConfigurations.put(outputType, outputTypeConfiguration);
    }

    /**
     * Verify if output type configuration for the given output type exists
     * 
     * @param outputType the specified output type
     * @return true if an output type configuration exists for the specified output type, false otherwise
     */
    public boolean containsOutputTypeConfiguration(OutputType outputType) {
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
        if (!containsOutputTypeConfiguration(outputType)) {
            PlanItLogger.severe("Requesting output type configuration for " + outputType.toString()
                    + " which has not been registered, returning null ");
        }
        return outputTypeConfigurations.get(outputType);
    }
    
    // getters - setters

    public void setPersistOnlyFinalIteration(boolean persistOnlyFinalIteration) {
        this.persistOnlyFinalIteration = persistOnlyFinalIteration;
    }

    public boolean isPersistOnlyFinalIteration() {
        return persistOnlyFinalIteration;
    }
    
    public List<OutputTypeConfiguration> getRegisteredOutputTypeConfigurations() {
    	return new ArrayList<OutputTypeConfiguration>(outputTypeConfigurations.values());
    	
    }
    
    public List<OutputType> getRegisteredOutputTypes() {
    	return new ArrayList<OutputType>(outputTypeConfigurations.keySet());
    }
    
}

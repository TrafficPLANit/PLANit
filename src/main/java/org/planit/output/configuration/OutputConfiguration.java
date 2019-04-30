package org.planit.output.configuration;

import java.util.TreeMap;
import java.util.logging.Logger;

import org.planit.demand.Demands;
import org.planit.output.OutputType;
import org.planit.output.adapter.OutputAdapter;

/**
 * Class containing the general output configuration and the type specific configurations for some traffic assignment 
 * @author markr
 *
 */
public class OutputConfiguration {
    
    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(Demands.class.getName());    
    
    /**
     * Default for persisting final iteration
     */
    private final boolean PERSIST_ONLY_FINAL_ITERATION = true;
    
    /**
     * persisting final iteration only or not
     */
    protected boolean persistOnlyFinalIteration = PERSIST_ONLY_FINAL_ITERATION;    
    
    /**
     * output configurations per output type
     */
    protected final TreeMap<OutputType,OutputTypeConfiguration> outputTypeConfigurations;    
    
    /**
     * Base constructor
     */
    public OutputConfiguration() {
        this.outputTypeConfigurations = new TreeMap<OutputType, OutputTypeConfiguration>();
    }

/** 
 * Factory method to create an output configuration of a given type
 * 
 * @param outputType            the output type to register the configuration for
 * @param outputAdapter       the adapter that allows access to the data to persist for the given output type
 * @return                              outputConfiguration that has been created
 */    
    public OutputTypeConfiguration createAndRegisterOutputTypeConfiguration(OutputType outputType, OutputAdapter outputAdapter){
        OutputTypeConfiguration outputTypeConfiguration = null;
        if (outputType.equals(OutputType.LINK)) {
            outputTypeConfiguration = new LinkOutputTypeConfiguration(outputAdapter);
        } else {
            // no other dedicated output type configurations yet exist
            //TODO: create relevant type specific loggers with relevant properties to configure
            outputTypeConfiguration = new DummyOutputTypeConfiguration(outputAdapter); 
        }
        outputTypeConfigurations.put(outputType, outputTypeConfiguration);
        return outputTypeConfiguration;
    }
    
/** 
 * Verify if output type configuration for the given output type exists
 * 
 * @param outputType                                       specified output type
 * @return outputTypeConfiguration exists       true if an output type configuration exists for the specified output type, false otherwise
 */
    public boolean containsOutputTypeConfiguration(OutputType outputType) {
        return outputTypeConfigurations.containsKey(outputType);
    }
    
/** 
 * Collect the output type configuration for the given type
 * 
 * @param outputType         OutputType to collect the output type configuration for
 * @return                            output type configuration registered, if not registered null is returned and a warning is logged
 */
    public OutputTypeConfiguration getOutputTypeConfiguration(OutputType outputType) {
        if (!containsOutputTypeConfiguration(outputType)) {
            LOGGER.severe("Requesting output type configuration for "+outputType.toString() + " which has not been registered, returning null ");
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
    
}

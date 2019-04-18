package org.planit.output;

import java.util.ArrayList;
import org.planit.output.adapter.OutputAdapter;
import org.planit.output.configuration.OutputConfiguration;
import org.planit.output.configuration.OutputTypeConfiguration;
import org.planit.output.formatter.OutputFormatter;
import org.planit.trafficassignment.TrafficAssignment;

/**
 * Base class for output writers containing basic functionality regarding all things output
 * 
 * @author markr
 *
 */
public class OutputManager {
    
    /**
     * the traffic assignment this output manager manages
     */
    protected TrafficAssignment assignment;
    
    /**
     * The overall output configuration instance
     */
    protected OutputConfiguration outputConfiguration;
    
    /**
     * registered output formatters
     */
    protected ArrayList<OutputFormatter> outputFormatters;
                         
    /**
     * Base constructor of Output writer
     */
    public OutputManager(TrafficAssignment assignment) {
        this.assignment = assignment;
        this.outputFormatters= new ArrayList<OutputFormatter>();
    }
    
    /** Persist the output data for a given output type pending the configuration choices made
     * @param outputType
     */
    public void persistOutputData(OutputType outputType) {
        if(outputConfiguration.containsOutputTypeConfiguration(outputType)) {
            OutputTypeConfiguration outputTypeConfiguration = outputConfiguration.getOutputTypeConfiguration(outputType);
            for(OutputFormatter outputFormatter :  outputFormatters) {
                outputFormatter.persist(outputTypeConfiguration);   
            }
        }        
    }

    /** Factory method to create an output configuration of a given type
     * @param outputType    the output type to register the configuration for
     * @param outputAdapter the adapter that allows access to the data to persist for the given output type
     * @return outputConfiguration that has been created
     */
    public OutputTypeConfiguration createAndRegisterOutputTypeConfiguration(OutputType outputType, OutputAdapter outputAdapter) {
        return outputConfiguration.createAndRegisterOutputTypeConfiguration(outputType, outputAdapter);
    }
    
    // getters - setters

    public OutputConfiguration getOutputConfiguration() {
        return outputConfiguration;
    }

    /** Register the output formatter on the output manager. whenever something is persisted it will be delegated to the registered
     *  formatters
     * @param outputFormatter
     */
    public void registerOutputFormatter(OutputFormatter outputFormatter) {
        outputFormatters.add(outputFormatter);
    }
}

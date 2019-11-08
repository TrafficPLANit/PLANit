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
     * persisting final iteration only or not
     */
    protected boolean persistOnlyFinalIteration = PERSIST_ONLY_FINAL_ITERATION;
    
    private OutputManager outputManager;

    /**
     * Base constructor
     */
    public OutputConfiguration(OutputManager outputManager) {
    	this.outputManager = outputManager;
    }

    // getters - setters

    public void setPersistOnlyFinalIteration(boolean persistOnlyFinalIteration) {
        this.persistOnlyFinalIteration = persistOnlyFinalIteration;
    }

    public boolean isPersistOnlyFinalIteration() {
        return persistOnlyFinalIteration;
    }
    
    public OutputTypeConfiguration getOutputTypeConfiguration(OutputType outputType) {
    	return outputManager.getOutputTypeConfiguration(outputType);
    }
    
}
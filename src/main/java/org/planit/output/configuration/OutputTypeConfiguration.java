package org.planit.output.configuration;

import org.planit.output.OutputType;
import org.planit.output.adapter.OutputAdapter;
import org.planit.output.property.OutputProperty;

/**
 * Configuration for a specific output type including the adapter allowing
 * access to the underlying raw data
 * 
 * @author markr
 *
 */
public abstract class OutputTypeConfiguration {

    /**
     * The output adapter for the output type which provides access the data when
     * needed to the one utilizing this configuration for persistence reasons
     */
    protected final OutputAdapter outputAdapter;
    
    /**
     * The output type being used with the current instance - this must be set in each concrete class which extends OutputTypeConfiguration
     */
    protected OutputType outputType;

    /**
     * OutputTypeconfiguration constructor
     * 
     * @param outputAdapter   to access data for output persistence
     */
     public OutputTypeConfiguration(OutputAdapter outputAdapter) {
        this.outputAdapter = outputAdapter;
    }

    /**
     * Collect the output adapter granting access to selected parts of the traffic
     * assignment model for retrieving persistence data
     * 
     * @return registered output adapter
     */
    public OutputAdapter getOutputAdapter() {
        return outputAdapter;
    }

	public OutputType getOutputType() {
		return outputType;
	}
	
	public abstract OutputProperty [] getOutputKeyProperties();
	
	public abstract OutputProperty [] getOutputValueProperties();

}

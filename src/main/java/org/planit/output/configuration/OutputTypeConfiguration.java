package org.planit.output.configuration;

import java.util.logging.Logger;

import org.planit.output.OutputType;
import org.planit.output.adapter.OutputAdapter;

/**
 * Configuration for a specific output type including the adapter allowing
 * access to the underlying raw data
 * 
 * @author markr
 *
 */
public abstract class OutputTypeConfiguration {

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(OutputTypeConfiguration.class.getName());

    /**
     * The output adapter for the output type which provides access the data when
     * needed to the one utilizing this configuration for persistence reasons
     */
    protected final OutputAdapter outputAdapter;
    
    /**
     * The output type being used with the current instance
     */
    protected OutputType outputType;

    /**
     * OutputTypeconfiguration constructor
     * 
     * @param outputAdapter   to access data for output persistence
     * @param outputType        OutputType being used with this configuration
     */
    public OutputTypeConfiguration(OutputAdapter outputAdapter, OutputType outputType) {
        this.outputAdapter = outputAdapter;
        this.outputType = outputType;
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
}

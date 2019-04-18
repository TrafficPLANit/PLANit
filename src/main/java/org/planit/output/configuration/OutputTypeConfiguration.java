package org.planit.output.configuration;

import java.util.logging.Logger;
import org.planit.output.adapter.OutputAdapter;

/**
 * Configuration for a specific output type includeing the adapter allowing access to the underlying raw data
 * @author markr
 *
 */
public class OutputTypeConfiguration {
    
    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(OutputTypeConfiguration.class.getName());    

    /**
     * Default for persisting link id
     */
    private final boolean EXCLUDE_LINK_ID = false;
    
    /**
     * excluding link id from persistence
     */
    protected boolean excludeLinkId = EXCLUDE_LINK_ID; 
    
    /**
     * The output adapter for thie output type which provides access the data when needed to the one
     * utilising this configuration for persistence reasons
     */
    protected final OutputAdapter outputAdapter;
    
    /**
     * OutputTypeconfiguration constructor
     * @param outputAdapter to access data for output persistence
     */
    public OutputTypeConfiguration(OutputAdapter outputAdapter) {
        this.outputAdapter = outputAdapter;
    }
    
    // getters - setters
    
    public boolean isExcludeLinkId() {
        return excludeLinkId;
    }

    public void setExcludeLinkId(boolean excludeLinkId) {
        this.excludeLinkId = excludeLinkId;
    }
    
    /** Collect the output adapter granting access to selected parts of the traffic assignment model for retrieving persistence data
     * @return registered output adapter
     */
    public OutputAdapter getOutputAdapter() {
        return outputAdapter;
    }
    
}

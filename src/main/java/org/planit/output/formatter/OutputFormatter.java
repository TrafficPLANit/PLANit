package org.planit.output.formatter;

import org.planit.output.configuration.OutputTypeConfiguration;

/**
 * Interface for peristing output data in a particular format
 * 
 * @author markr
 *
 */
public interface OutputFormatter {

    
    /** collect the id of the formatter
     * @return id
     */
    public long getId();
    
    /** Persist the output data based on the passed in configuration and adapter (contained in the configuration)
     * @param outputTypeConfiguration
     */
    public void persist(OutputTypeConfiguration outputTypeConfiguration);    
}

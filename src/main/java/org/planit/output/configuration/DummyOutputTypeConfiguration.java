package org.planit.output.configuration;

import org.planit.output.adapter.OutputAdapter;

/**
 * Dummy class to prevent error during creation of new output type configuration.  This will need to be removed later.
 * 
 * @author gman6028
 *
 */
public class DummyOutputTypeConfiguration extends OutputTypeConfiguration {

    /** Base constructor
     * 
     * @param outputAdapter the output adapter to be used
     */
    public DummyOutputTypeConfiguration(OutputAdapter outputAdapter) {
        super(outputAdapter);
     }

}

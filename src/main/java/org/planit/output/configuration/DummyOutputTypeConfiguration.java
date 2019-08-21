package org.planit.output.configuration;

import org.planit.output.OutputType;
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
     * @param outputType the output type to be used with this configuration
     */
    public DummyOutputTypeConfiguration(OutputAdapter outputAdapter, OutputType outputType) {
        super(outputAdapter, outputType);
     }

}

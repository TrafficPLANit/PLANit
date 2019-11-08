package org.planit.output.adapter;

import org.planit.output.enums.OutputType;

/**
 * Top-level interface for all output type adapters
 * 
 * @author gman6028
 *
 */
public interface OutputTypeAdapter {
	
    /**
     * Return the output type corresponding to this output adapter
     * 
     * @return the output type corresponding to this output adapter
     */
	public OutputType getOutputType();
	
}
package org.planit.output.adapter;

import org.planit.output.enums.OutputType;
import org.planit.trafficassignment.TrafficAssignment;

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
	
	/**
	 * Return the TrafficAssignment used by this output adapter
	 * 
	 * @return the TrafficAssignment used by this output adapter
	 */
	public TrafficAssignment getTrafficAssignment();

}

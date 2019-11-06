package org.planit.output.adapter;

import org.planit.output.enums.OutputType;
import org.planit.trafficassignment.TrafficAssignment;

/**
 * Top-level abstract class which defines the common methods required by all output type adapters
 * 
 * @author gman6028
 *
 */
public abstract class OutputTypeAdapterImpl implements OutputTypeAdapter {

    /**
     * the traffic assignment this output adapter is drawing from
     */
    protected TrafficAssignment trafficAssignment;
    
    /**
     * The OutputType this OutputTypeAdapter is used for 
     */
    protected OutputType outputType;
    
    /**
     * Constructor
     * 
     * @param trafficAssignment TrafficAssignment object which this adapter wraps
     */
    public OutputTypeAdapterImpl(OutputType outputType, TrafficAssignment trafficAssignment) {
    	this.outputType = outputType;
        this.trafficAssignment = trafficAssignment;
    }
    
    /**
     * Return the output type corresponding to this output adapter
     * 
     * @return the output type corresponding to this output adapter
     */
    public OutputType getOutputType() {
    	return outputType;
    }
    
    public TrafficAssignment getTrafficAssignment() {
    	return trafficAssignment;
    }
}

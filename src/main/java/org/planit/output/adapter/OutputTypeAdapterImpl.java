package org.planit.output.adapter;

import org.planit.exceptions.PlanItException;
import org.planit.output.enums.OutputType;
import org.planit.output.enums.SubOutputTypeEnum;
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
     * @param outputType the OutputType this adapter corresponds to
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
    
    /**
     * Default implementation assumes that regular iteration index is used, which in most cases it true, only when for example
     * costs are trailing one iteration behind in case they are only revealed in the next iteration this method should be overridden
     */
    @Override
    public int getIterationIndexForSubOutputType(SubOutputTypeEnum outputTypeEnum) throws PlanItException {
        return trafficAssignment.getSimulationData().getIterationIndex();
    }    
    
}
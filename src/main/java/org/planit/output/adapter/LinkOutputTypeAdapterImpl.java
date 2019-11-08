package org.planit.output.adapter;

import java.util.List;

import org.planit.network.physical.LinkSegment;
import org.planit.output.enums.OutputType;
import org.planit.trafficassignment.TrafficAssignment;

public abstract class LinkOutputTypeAdapterImpl extends OutputTypeAdapterImpl implements LinkOutputTypeAdapter {

    /**
     * Constructor
     * 
     * @param outputType the OutputType this adapter corresponds to
     * @param trafficAssignment TrafficAssignment object which this adapter wraps
     */
    public LinkOutputTypeAdapterImpl(OutputType outputType, TrafficAssignment trafficAssignment) {
    	super(outputType, trafficAssignment);
    }
    
    /**
     * Return a List of link segments for this assignment
     * 
     * @return a List of link segments for this assignment
     */
	@Override
    public List<LinkSegment> getLinkSegments() {
    	return trafficAssignment.getTransportNetwork().linkSegments.toList();
    }
 
}
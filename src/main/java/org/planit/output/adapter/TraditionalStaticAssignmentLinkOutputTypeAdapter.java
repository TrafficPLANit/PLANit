package org.planit.output.adapter;

import java.util.List;

import org.planit.data.TraditionalStaticAssignmentSimulationData;
import org.planit.network.physical.LinkSegment;
import org.planit.output.enums.OutputType;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.userclass.Mode;

/**
 * Adapter providing access to the data of the TraditionalStaticAssignment class
 * relevant for link outputs without exposing the internals of the traffic
 * assignment class itself
 * 
 * @author markr
 *
 */
public class TraditionalStaticAssignmentLinkOutputTypeAdapter extends OutputTypeAdapterImpl implements LinkOutputTypeAdapter {

	/**
	 * Constructor
	 * 
	 * @param outputType the output type for the current persistence
	 * @param trafficAssignment the traffic assignment used to provide the data
	 */
	public TraditionalStaticAssignmentLinkOutputTypeAdapter(OutputType outputType, TrafficAssignment trafficAssignment) {
		super(outputType, trafficAssignment);
	}
		
	/**
	 * Returns true if there is a flow through the current specified link segment for the specified mode
	 * 
	 * @param linkSegment specified link segment
	 * @param mode specified mode
	 * @return true is there is flow through this link segment, false if the flow is zero
	 */
	@Override
	public boolean isFlowPositive(LinkSegment linkSegment, Mode mode) {
		TraditionalStaticAssignmentSimulationData simulationData = (TraditionalStaticAssignmentSimulationData) trafficAssignment.getSimulationData();
		return (simulationData.getModalNetworkSegmentFlows(mode)[(int) linkSegment.getId()] > 0.0);
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

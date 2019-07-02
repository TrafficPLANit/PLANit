package org.planit.output.adapter;

import java.util.logging.Logger;

import org.planit.data.TraditionalStaticAssignmentSimulationData;
import org.planit.exceptions.PlanItException;
import org.planit.network.physical.Node;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.output.property.BaseOutputProperty;
import org.planit.trafficassignment.TraditionalStaticAssignment;
import org.planit.userclass.Mode;

/**
 * Adapter providing access to the data of the TraditionalStaticAssignment class
 * relevant for link outputs without exposing the internals of the traffic
 * assignment class itself
 * 
 * @author markr
 *
 */
public class TraditionalStaticAssignmentLinkOutputAdapter extends LinkOutputAdapter {

	private static final Logger LOGGER = Logger.getLogger(TraditionalStaticAssignmentLinkOutputAdapter.class.getName());

	/**
	 * Constructor
	 *
	 * @param trafficAssignment TraditionalStaticAssignment object which this
	 *                          adapter wraps
	 */
	public TraditionalStaticAssignmentLinkOutputAdapter(TraditionalStaticAssignment trafficAssignment) {
		super(trafficAssignment);
	}

	/**
	 * Return the network segment costs for a specified mode
	 * 
	 * @param mode specified mode
	 * @return array storing the network segment costs for the specified mode
	 * @throws PlanItException thrown if there is an error
	 */
	public double[] getModalNetworkSegmentCosts(Mode mode) throws PlanItException {
		return ((TraditionalStaticAssignment) trafficAssignment).getModalNetworkSegmentCosts(mode);
	}

	/**
	 * Returns whether the current assignment has converged
	 * 
	 * @return true if the current assignment has converged, false otherwise
	 */
	@Override
	public boolean isConverged() {
		return ((TraditionalStaticAssignment) trafficAssignment).getSimulationData().isConverged();
	}
	
	/**
	 * Returns the current iteration index of the simulation
	 * 
	 * @return index of the current iteration
	 */
	public int getIterationIndex() {
		TraditionalStaticAssignmentSimulationData simulationData = getSimulationData();
		return simulationData.getIterationIndex();
	}

	/**
	 * Returns the output object corresponding to the current output property for a specified link segmen and mode
	 * 
	 * @param outputProperty current output property
	 * @param linkSegment specified link segment
	 * @param mode specified mode
	 * @return Object containing the output which is to be sent to the output 
	 */
	public Object getPropertyValue(BaseOutputProperty outputProperty, MacroscopicLinkSegment linkSegment, Mode mode) {
		switch (outputProperty.getOutputProperty()) {
		case DENSITY:
			return getDensityPropertyValue(linkSegment);
		case LINK_ID:
			return getLinkIdPropertyValue(linkSegment);
		case MODE_ID:
			return getModeIdPropertyValue(mode);
		case SPEED:
			return getSpeedPropertyValue(linkSegment, mode);
		case FLOW:
			return getFlowPropertyValue(linkSegment, mode);
		case LENGTH:
			return getLengthPropertyValue(linkSegment);
		case UPSTREAM_NODE_EXTERNAL_ID:
			return getUpstreamNodeIdPropertyValue(linkSegment);
		case DOWNSTREAM_NODE_EXTERNAL_ID:
			return getDownstreamNodeIdPropertyValue(linkSegment);
		case TRAVEL_TIME:
			return getTravelTimePropertyValue(linkSegment, mode);
		default:
			return null;
		}
	}

	private Object getDensityPropertyValue(MacroscopicLinkSegment linkSegment) {
		return linkSegment.getLinkSegmentType().getMaximumDensityPerLane();
	}

	private Object getLinkIdPropertyValue(MacroscopicLinkSegment linkSegment) {
		return linkSegment.getId();
	}

	private Object getModeIdPropertyValue(Mode mode) {
		return mode.getExternalId();
	}

	private Object getSpeedPropertyValue(MacroscopicLinkSegment linkSegment, Mode mode) {
		int id = (int) linkSegment.getId();
		TraditionalStaticAssignmentSimulationData simulationData = getSimulationData();
		double[] modalNetworkSegmentCosts = simulationData.getModalNetworkSegmentCosts(mode);
		double travelTime = modalNetworkSegmentCosts[id];
		double length = linkSegment.getParentLink().getLength();
		double speed = length / travelTime;
		return speed;
	}

	private Object getFlowPropertyValue(MacroscopicLinkSegment linkSegment, Mode mode) {
		int id = (int) linkSegment.getId();
		TraditionalStaticAssignmentSimulationData simulationData = getSimulationData();
		double[] modalNetworkSegmentFlows = simulationData.getModalNetworkSegmentFlows(mode);
		double flow = modalNetworkSegmentFlows[id];
		return flow;
	}

	private Object getLengthPropertyValue(MacroscopicLinkSegment linkSegment) {
		return linkSegment.getParentLink().getLength();
	}

	private Object getUpstreamNodeIdPropertyValue(MacroscopicLinkSegment linkSegment) {
		Node startNode = (Node) linkSegment.getUpstreamVertex();
		return startNode.getExternalId();
	}

	private Object getDownstreamNodeIdPropertyValue(MacroscopicLinkSegment linkSegment) {
		Node endNode = (Node) linkSegment.getDownstreamVertex();
		return endNode.getExternalId();
	}

	private Object getTravelTimePropertyValue(MacroscopicLinkSegment linkSegment, Mode mode) {
		int id = (int) linkSegment.getId();
		TraditionalStaticAssignmentSimulationData simulationData = getSimulationData();
		double[] modalNetworkSegmentCosts = simulationData.getModalNetworkSegmentCosts(mode);
		double travelTime = modalNetworkSegmentCosts[id];
		return travelTime;
	}
	
	private TraditionalStaticAssignmentSimulationData getSimulationData() {
		TraditionalStaticAssignment traditionalStaticAssignment = (TraditionalStaticAssignment) trafficAssignment;
		TraditionalStaticAssignmentSimulationData simulationData = (TraditionalStaticAssignmentSimulationData) traditionalStaticAssignment
				.getSimulationData();
		return simulationData;
	}
	
	/**
	 * Returns true if there is a flow through the current specified link segment for the specified mode
	 * 
	 * @param linkSegment specified link segment
	 * @param mode specified mode
	 * @return true is there is flow through this link segment, false if the flow is zero
	 */
	public boolean isFlowPositive(MacroscopicLinkSegment linkSegment, Mode mode) {
		TraditionalStaticAssignmentSimulationData simulationData = getSimulationData();
		return (simulationData.getModalNetworkSegmentFlows(mode)[(int) linkSegment.getId()] > 0.0);
	}

}

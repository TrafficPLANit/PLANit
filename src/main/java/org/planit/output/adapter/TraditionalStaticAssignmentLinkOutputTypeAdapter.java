package org.planit.output.adapter;

import org.opengis.geometry.DirectPosition;
import org.planit.data.TraditionalStaticAssignmentSimulationData;
import org.planit.network.physical.LinkSegment;
import org.planit.network.physical.Node;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.output.formatter.OutputFormatter;
import org.planit.output.property.OutputProperty;
import org.planit.time.TimePeriod;
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
public class TraditionalStaticAssignmentLinkOutputTypeAdapter extends OutputTypeAdapter {

	/**
	 * Constructor
	 *
	 * @param trafficAssignment TrafficAssignment object which this adapter wraps
	 */
	public TraditionalStaticAssignmentLinkOutputTypeAdapter(TrafficAssignment trafficAssignment) {
		super(trafficAssignment);
	}
	
	/**
	 * Get the number of lanes to be presented as an output property
	 * 
	 * @param linkSegment the current link segment
	 * @return the number of lanes 
	 */
	private Object getNumberOfLanesPropertyValue(MacroscopicLinkSegment linkSegment) {
		return linkSegment.getNumberOfLanes();
	}
	
	/**
	 * Get the geometry location of the downstream node to be presented as an output property
	 * 
	 * @param linkSegment the current link segment
	 * @return the location of the downstream node
	 */
	private Object getDownstreamNodeLocationPropertyValue(MacroscopicLinkSegment linkSegment) {
		DirectPosition centrePoint = linkSegment.getDownstreamVertex().getCentrePointGeometry();
		if (centrePoint == null) {
			return OutputFormatter.NOT_SPECIFIED;
		}
		double[] coordinates = linkSegment.getDownstreamVertex().getCentrePointGeometry().getCoordinate();
		return coordinates[0] + "-" + coordinates[1];
	}
	
	/**
	 * Get the geometry location of the upstream node to be presented as an output property
	 * 
	 * @param linkSegment the current link segment
	 * @return the location of the upstream node
	 */
	private Object getUpstreamNodeLocationPropertyValue(MacroscopicLinkSegment linkSegment) {
		DirectPosition centrePoint = linkSegment.getUpstreamVertex().getCentrePointGeometry();
		if (centrePoint == null) {
			return OutputFormatter.NOT_SPECIFIED;
		}
		double[] coordinates = linkSegment.getUpstreamVertex().getCentrePointGeometry().getCoordinate();
		return coordinates[0] + "-" + coordinates[1];
	}
	
	/**
	 * Get the capacity per lane to be presented as an output property
	 * 
	 * @param linkSegment the current link segment
	 * @return the capacity per lane
	 */
	private Object getCapacityPerLanePropertyValue(MacroscopicLinkSegment linkSegment) {
		return linkSegment.getLinkSegmentType().getCapacityPerLane();
	}

	/**
	 * Get the density to be presented as an output property
	 * 
	 * @param linkSegment the current link segment
	 * @return the lane density
	 */
	private Object getDensityPropertyValue(MacroscopicLinkSegment linkSegment) {
		return linkSegment.getLinkSegmentType().getMaximumDensityPerLane();
	}
	
	/**
	 * Get the link segment id to be presented as an output property
	 * 
	 * @param linkSegment the current link segment
	 * @return the link segment id
	 */
	private Object getLinkSegmentIdPropertyValue(MacroscopicLinkSegment linkSegment) {
		return linkSegment.getId();
	}

	/**
	 * Get the link segment external Id to be presented as an output property
	 * 
	 * @param linkSegment the current link segment
	 * @return the link segment external Id
	 */
	private Object getLinkSegmentExternalIdPropertyValue(MacroscopicLinkSegment linkSegment) {
		return linkSegment.getParentLink().getExternalId();
	}

	/**
	 * Get the mode external Id to be presented as an output property
	 * 
	 * @param mode the current mode
	 * @return the mode external Id
	 */
	private Object getModeExternalIdPropertyValue(Mode mode) {
		return mode.getExternalId();
	}
	
	/**
	 * Get the mode Id to be presented as an output property
	 * 
	 * @param mode the current mode
	 * @return the mode id
	 */
	private Object getModeIdPropertyValue(Mode mode) {
		return mode.getId();
	}
	
	/**
	 * Get the link speed for a specified link segment and mode to be presented as an output property
	 * 
	 * @param linkSegment the current link segment
	 * @param mode the current mode
	 * @return the link segment speed
	 */
	private Object getCalculatedSpeedPropertyValue(MacroscopicLinkSegment linkSegment, Mode mode) {
		int id = (int) linkSegment.getId();
		TraditionalStaticAssignmentSimulationData simulationData = (TraditionalStaticAssignmentSimulationData) trafficAssignment.getSimulationData();
		double[] modalNetworkSegmentCosts = simulationData.getModalNetworkSegmentCosts(mode);
		double travelTime = modalNetworkSegmentCosts[id];
		double length = linkSegment.getParentLink().getLength();
		return length / travelTime;
	}
	
	/**
	 * Get the traffic flow for a specified link segment and mode to be presented as an output property
	 * 
	 * @param linkSegment the current link segment
	 * @param mode the current mode
	 * @return the traffic flow through the link segment
	 */
	private Object getFlowPropertyValue(MacroscopicLinkSegment linkSegment, Mode mode) {
		int id = (int) linkSegment.getId();
		TraditionalStaticAssignmentSimulationData simulationData = (TraditionalStaticAssignmentSimulationData) trafficAssignment.getSimulationData();
		double[] modalNetworkSegmentFlows = simulationData.getModalNetworkSegmentFlows(mode);
		return modalNetworkSegmentFlows[id];
	}
	
	/**
	 * Get the maximum speed for a specified link segment and mode
	 * 
	 * @param linkSegment the specified link segment
	 * @param mode the specified mode
	 * @return the link maximum speed
	 */
	private Object getMaximumSpeedPropertyValue(MacroscopicLinkSegment linkSegment, Mode mode) {
		return linkSegment.getMaximumSpeed(mode.getExternalId());
	}

	/**
	 * Get the link length to be presented as an output property
	 * 
	 * @param linkSegment the current link segment
	 * @return the link segment length
	 */
	private Object getLengthPropertyValue(MacroscopicLinkSegment linkSegment) {
		return linkSegment.getParentLink().getLength();
	}

	/**
	 * Get the external Id of the upstream node to be presented as an output property
	 * 
	 * @param linkSegment the current link segment
	 * @return the upstream node external Id
	 */
	private Object getUpstreamNodeExternalIdPropertyValue(MacroscopicLinkSegment linkSegment) {
		Node startNode = (Node) linkSegment.getUpstreamVertex();
		return startNode.getExternalId();
	}
	
	/**
	 * Get the Id of the upstream node to be presented as an output property
	 * 
	 * @param linkSegment the current link segment
	 * @return the upstream node external Id
	 */
	private Object getUpstreamNodeIdPropertyValue(MacroscopicLinkSegment linkSegment) {
		Node startNode = (Node) linkSegment.getUpstreamVertex();
		return startNode.getId();
	}
	
	/**
	 * Get the external Id of the downstream node to be presented as an output property
	 * 
	 * @param linkSegment the current link segment
	 * @return the downstream node external Id
	 */
	private Object getDownstreamNodeExternalIdPropertyValue(MacroscopicLinkSegment linkSegment) {
		Node endNode = (Node) linkSegment.getDownstreamVertex();
		return endNode.getExternalId();
	}

	/**
	 * Get the Id of the downstream node to be presented as an output property
	 * 
	 * @param linkSegment the current link segment
	 * @return the downstream node external Id
	 */
	private Object getDownstreamNodeIdPropertyValue(MacroscopicLinkSegment linkSegment) {
		Node endNode = (Node) linkSegment.getDownstreamVertex();
		return endNode.getId();
	}
	
	/**
	 * Get the link cost to be presented as an output property
	 * 
	 * @param linkSegment the current link segment
	 * @param mode the current mode
	 * @param timeUnitMultiplier multiplier to convert time units into hours, minutes or seconds
	 * @return the link segment cost
	 */
	private Object getCostPropertyValue(MacroscopicLinkSegment linkSegment, Mode mode, double timeUnitMultiplier) {
		int id = (int) linkSegment.getId();
		TraditionalStaticAssignmentSimulationData simulationData = (TraditionalStaticAssignmentSimulationData) trafficAssignment.getSimulationData();
		double[] modalNetworkSegmentCosts = simulationData.getModalNetworkSegmentCosts(mode);
		return modalNetworkSegmentCosts[id] * timeUnitMultiplier;
	}
	
	/**
	 * Returns the output object corresponding to the current output property for a specified link segment and mode
	 * 
	 * @param outputProperty current output property
	 * @param linkSegment specified link segment
	 * @param mode specified mode
	 * @param timeUnitMultiplier multiplier to convert time into hours, minutes or seconds
	 * @return Object containing the output which is to be sent to the output 
	 */
	public Object getLinkPropertyValue(OutputProperty outputProperty, LinkSegment linkSegment, Mode mode, TimePeriod timePeriod, double timeUnitMultiplier) {
		MacroscopicLinkSegment macroscopicLinkSegment  = (MacroscopicLinkSegment) linkSegment;
		switch (outputProperty) {
		case DENSITY:
			return getDensityPropertyValue(macroscopicLinkSegment);
		case LINK_SEGMENT_ID:
			return getLinkSegmentIdPropertyValue(macroscopicLinkSegment);
		case LINK_SEGMENT_EXTERNAL_ID:
			return getLinkSegmentExternalIdPropertyValue(macroscopicLinkSegment);
		case MODE_ID:
			return getModeIdPropertyValue(mode);
		case MODE_EXTERNAL_ID:
			return getModeExternalIdPropertyValue(mode);
		case CALCULATED_SPEED:
			return getCalculatedSpeedPropertyValue(macroscopicLinkSegment, mode);
		case MAXIMUM_SPEED:
			return getMaximumSpeedPropertyValue(macroscopicLinkSegment, mode);
		case FLOW:
			return getFlowPropertyValue(macroscopicLinkSegment, mode);
		case LENGTH:
			return getLengthPropertyValue(macroscopicLinkSegment);
		case UPSTREAM_NODE_EXTERNAL_ID:
			return getUpstreamNodeExternalIdPropertyValue(macroscopicLinkSegment);
		case UPSTREAM_NODE_ID:
			return getUpstreamNodeIdPropertyValue(macroscopicLinkSegment);
		case DOWNSTREAM_NODE_EXTERNAL_ID:
			return getDownstreamNodeExternalIdPropertyValue(macroscopicLinkSegment);
		case DOWNSTREAM_NODE_ID:
			return getDownstreamNodeIdPropertyValue(macroscopicLinkSegment);
		case COST:
			return getCostPropertyValue(macroscopicLinkSegment, mode, timeUnitMultiplier);
		case CAPACITY_PER_LANE:
			return getCapacityPerLanePropertyValue(macroscopicLinkSegment);
		case DOWNSTREAM_NODE_LOCATION:
			return getDownstreamNodeLocationPropertyValue(macroscopicLinkSegment);
		case UPSTREAM_NODE_LOCATION:
			return getUpstreamNodeLocationPropertyValue(macroscopicLinkSegment);
		case NUMBER_OF_LANES:
			return getNumberOfLanesPropertyValue(macroscopicLinkSegment);
		case RUN_ID:
			return trafficAssignment.getId();
		case ITERATION_INDEX: 
			return trafficAssignment.getSimulationData().getIterationIndex();
		case TIME_PERIOD_ID:
			return timePeriod.getId();
		case TIME_PERIOD_EXTERNAL_ID:
			return timePeriod.getExternalId();
		default:
			return OutputFormatter.NOT_SPECIFIED;
		}
	}

	/**
	 * Returns true if there is a flow through the current specified link segment for the specified mode
	 * 
	 * @param linkSegment specified link segment
	 * @param mode specified mode
	 * @return true is there is flow through this link segment, false if the flow is zero
	 */
	public boolean isFlowPositive(MacroscopicLinkSegment linkSegment, Mode mode) {
		TraditionalStaticAssignmentSimulationData simulationData = (TraditionalStaticAssignmentSimulationData) trafficAssignment.getSimulationData();
		return (simulationData.getModalNetworkSegmentFlows(mode)[(int) linkSegment.getId()] > 0.0);
	}

}

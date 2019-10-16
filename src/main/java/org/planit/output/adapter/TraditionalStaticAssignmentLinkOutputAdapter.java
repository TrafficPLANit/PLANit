package org.planit.output.adapter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import org.opengis.geometry.DirectPosition;
import org.planit.data.TraditionalStaticAssignmentSimulationData;
import org.planit.network.physical.LinkSegment;
import org.planit.network.physical.Node;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.network.transport.TransportNetwork;
import org.planit.output.formatter.OutputFormatter;
import org.planit.output.property.OutputProperty;
import org.planit.time.TimePeriod;
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
public class TraditionalStaticAssignmentLinkOutputAdapter extends OutputAdapter {

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
		TraditionalStaticAssignmentSimulationData simulationData = getSimulationData();
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
		TraditionalStaticAssignmentSimulationData simulationData = getSimulationData();
		double[] modalNetworkSegmentFlows = simulationData.getModalNetworkSegmentFlows(mode);
		return modalNetworkSegmentFlows[id];
	}
	
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
	private Object getUpstreamNodeIdPropertyValue(MacroscopicLinkSegment linkSegment) {
		Node startNode = (Node) linkSegment.getUpstreamVertex();
		return startNode.getExternalId();
	}
	
	/**
	 * Get the external Id of the downstream node to be presented as an output property
	 * 
	 * @param linkSegment the current link segment
	 * @return the downstream node external Id
	 */
	private Object getDownstreamNodeIdPropertyValue(MacroscopicLinkSegment linkSegment) {
		Node endNode = (Node) linkSegment.getDownstreamVertex();
		return endNode.getExternalId();
	}
	
	/**
	 * Get the link cost to be presented as an output property
	 * 
	 * @param linkSegment the current link segment
	 * @param mode the current mode
	 * @return the link segment cost
	 */
	private Object getCostPropertyValue(MacroscopicLinkSegment linkSegment, Mode mode) {
		int id = (int) linkSegment.getId();
		TraditionalStaticAssignmentSimulationData simulationData = getSimulationData();
		double[] modalNetworkSegmentCosts = simulationData.getModalNetworkSegmentCosts(mode);
		return modalNetworkSegmentCosts[id];
	}
	
	/**
	 * Returns an output object for all link segments
	 * 
	 * @param transportNetwork current network
	 * @param getValue lambda function which returns the desired object for each link segment
	 * @return List of the required output object
	 */
	private List<Object> getObjectForAllLinkSegments(TransportNetwork transportNetwork, Function<MacroscopicLinkSegment, Object> getValue) {
		List<Object> values = new ArrayList<Object>();
		Iterator<LinkSegment> linkSegmentIter = transportNetwork.linkSegments.iterator();
		while (linkSegmentIter.hasNext()) {
			MacroscopicLinkSegment linkSegment = (MacroscopicLinkSegment) linkSegmentIter.next();
			values.add(getValue.apply(linkSegment));
		}
		return values;
	}
	
	/**
	 * Get the simulation data for the current iteration
	 * 
	 * @return the simulation data for the current iteration
	 */
	@Override
	public TraditionalStaticAssignmentSimulationData getSimulationData() {
		TraditionalStaticAssignment traditionalStaticAssignment = (TraditionalStaticAssignment) trafficAssignment;
		TraditionalStaticAssignmentSimulationData simulationData = 
				(TraditionalStaticAssignmentSimulationData) traditionalStaticAssignment.getSimulationData();
		return simulationData;
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
	 * Returns the output object corresponding to the current output property for a specified link segment and mode
	 * 
	 * @param outputProperty current output property
	 * @param linkSegment specified link segment
	 * @param mode specified mode
	 * @return Object containing the output which is to be sent to the output 
	 */
	public Object getLinkPropertyValue(OutputProperty outputProperty, LinkSegment linkSegment, Mode mode, TimePeriod timePeriod) {
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
			return getUpstreamNodeIdPropertyValue(macroscopicLinkSegment);
		case DOWNSTREAM_NODE_EXTERNAL_ID:
			return getDownstreamNodeIdPropertyValue(macroscopicLinkSegment);
		case COST:
			return getCostPropertyValue(macroscopicLinkSegment, mode);
		case CAPACITY_PER_LANE:
			return getCapacityPerLanePropertyValue(macroscopicLinkSegment);
		case DOWNSTREAM_NODE_LOCATION:
			return getDownstreamNodeLocationPropertyValue(macroscopicLinkSegment);
		case UPSTREAM_NODE_LOCATION:
			return getUpstreamNodeLocationPropertyValue(macroscopicLinkSegment);
		case NUMBER_OF_LANES:
			return getNumberOfLanesPropertyValue(macroscopicLinkSegment);
		case RUN_ID:
			return getTrafficAssignmentId();
		case ITERATION_INDEX: 
			return getSimulationData().getIterationIndex();
		case TIME_PERIOD_ID:
			return timePeriod.getId();
		default:
			return null;
		}
	}
	
	public List<Object> getDensityForAllLinkSegments(TransportNetwork transportNetwork) {
		return getObjectForAllLinkSegments(transportNetwork, (linkSegment) -> {return getDensityPropertyValue(linkSegment);});
	}

	public List<Object> getLinkSegmentIdForAllLinkSegments(TransportNetwork transportNetwork) {
		return getObjectForAllLinkSegments(transportNetwork, (linkSegment) -> {return getLinkSegmentIdPropertyValue(linkSegment);});
	}
	
	public List<Object> getLinkSegmentExternalIdForAllLinkSegments(TransportNetwork transportNetwork) {
		return getObjectForAllLinkSegments(transportNetwork, (linkSegment) -> {return getLinkSegmentExternalIdPropertyValue(linkSegment);});
	}
	
	public List<Object> getModeExternalIdForAllLinkSegments(TransportNetwork transportNetwork, Mode mode) {
		return getObjectForAllLinkSegments(transportNetwork, (linkSegment) -> {return getModeExternalIdPropertyValue(mode);});
	}

	public List<Object> getModeIdForAllLinkSegments(TransportNetwork transportNetwork, Mode mode) {
		return getObjectForAllLinkSegments(transportNetwork, (linkSegment) -> {return getModeIdPropertyValue(mode);});
	}

	public List<Object> getCalculatedSpeedForAllLinkSegments(TransportNetwork transportNetwork, Mode mode) {
		return getObjectForAllLinkSegments(transportNetwork, (linkSegment) -> {return getCalculatedSpeedPropertyValue(linkSegment, mode);});
	}
	
	public List<Object> getMaximumSpeedForAllLinkSegments(TransportNetwork transportNetwork, Mode mode) {
		return getObjectForAllLinkSegments(transportNetwork, (linkSegment) -> {return getMaximumSpeedPropertyValue(linkSegment, mode);});
	}
	
	public List<Object> getFlowForAllLinkSegments(TransportNetwork transportNetwork, Mode mode) {
		return getObjectForAllLinkSegments(transportNetwork, (linkSegment) -> {return getFlowPropertyValue(linkSegment, mode);});
	}

	public List<Object> getUpstreamNodeIdForAllLinkSegments(TransportNetwork transportNetwork) {
		return getObjectForAllLinkSegments(transportNetwork, (linkSegment) -> {return getUpstreamNodeIdPropertyValue(linkSegment);});		
	}
	
	public List<Object> getLengthForAllLinkSegments(TransportNetwork transportNetwork) {
		return getObjectForAllLinkSegments(transportNetwork, (linkSegment) -> {return getLengthPropertyValue(linkSegment);});
	}
	
	public List<Object> getCostForAllLinkSegments(TransportNetwork transportNetwork, Mode mode) {
		return getObjectForAllLinkSegments(transportNetwork, (linkSegment) -> {return getCostPropertyValue(linkSegment, mode);});		
	}

	public List<Object> getDowntreamNodeIdForAllLinkSegments(TransportNetwork transportNetwork) {
		return getObjectForAllLinkSegments(transportNetwork, (linkSegment) -> {return getDownstreamNodeIdPropertyValue(linkSegment);});		
	}
	
	public List<Object> getCapacityPerLaneForAllLinkSegments(TransportNetwork transportNetwork) {
		return getObjectForAllLinkSegments(transportNetwork, (linkSegment) -> {return getCapacityPerLanePropertyValue(linkSegment);});		
	}
	
	public List<Object>  getDownstreamNodeLocationForAllLinkSegments(TransportNetwork transportNetwork) {
		return getObjectForAllLinkSegments(transportNetwork, (linkSegment) -> {return getDownstreamNodeLocationPropertyValue(linkSegment);});
	}
	
	public List<Object>  getUpstreamNodeLocationForAllLinkSegments(TransportNetwork transportNetwork) {
		return getObjectForAllLinkSegments(transportNetwork, (linkSegment) -> {return getUpstreamNodeLocationPropertyValue(linkSegment);});
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

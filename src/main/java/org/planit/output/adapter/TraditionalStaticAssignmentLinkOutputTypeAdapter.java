package org.planit.output.adapter;

import org.opengis.geometry.DirectPosition;
import org.planit.data.TraditionalStaticAssignmentSimulationData;
import org.planit.exceptions.PlanItException;
import org.planit.network.physical.LinkSegment;
import org.planit.network.physical.Node;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.output.enums.OutputType;
import org.planit.output.formatter.OutputFormatter;
import org.planit.output.property.BaseOutputProperty;
import org.planit.output.property.IterationIndexOutputProperty;
import org.planit.output.property.ModeExternalIdOutputProperty;
import org.planit.output.property.ModeIdOutputProperty;
import org.planit.output.property.OutputProperty;
import org.planit.output.property.RunIdOutputProperty;
import org.planit.output.property.TimePeriodExternalIdOutputProperty;
import org.planit.output.property.TimePeriodIdOutputProperty;
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
public class TraditionalStaticAssignmentLinkOutputTypeAdapter extends LinkOutputTypeAdapterImpl {

	/**
	 * Returns the value of the calculated speed
	 * 
	 * @param linkSegment LinkSegment containing data which may be required
	 * @param mode        current mode
	 * @return the calculated speed across the link
	 * @throws PlanItException thrown if there is an error
	 */
	private double getCalculatedSpeed(LinkSegment linkSegment, Mode mode) throws PlanItException {
		int id = (int) linkSegment.getId();
		TraditionalStaticAssignmentSimulationData simulationData = (TraditionalStaticAssignmentSimulationData) trafficAssignment
				.getSimulationData();
		double[] modalNetworkSegmentCosts = simulationData.getModalNetworkSegmentCosts(mode);
		double travelTime = modalNetworkSegmentCosts[id];
		double length = linkSegment.getParentLink().getLength();
		//double travelTime = trafficAssignment.getPhysicalCost().getSegmentCost(mode, linkSegment);
		return length / travelTime;
	}

	/**
	 * Returns the value of the capacity per lane
	 * 
	 * @param linkSegment LinkSegment containing data which may be required
	 * @return the capacity per lane across this link segment
	 * @throws PlanItException thrown if there is an error
	 */
	private double getCapacityPerLane(LinkSegment linkSegment) throws PlanItException {
		if (!(linkSegment instanceof MacroscopicLinkSegment)) {
			throw new PlanItException(
					"Tried to calculate capacity per link across an object which is not a MacroscopicLinkSegment.");
		}
		MacroscopicLinkSegment macroscopicLinkSegment = (MacroscopicLinkSegment) linkSegment;
		return macroscopicLinkSegment.getLinkSegmentType().getCapacityPerLane();
	}

	/**
	 * Returns the flow density of the current link
	 * 
	 * @param linkSegment LinkSegment containing data which may be required
	 * @return the flow density of the current link
	 * @throws PlanItException thrown if there is an error
	 */
	private double getFlowDensity(LinkSegment linkSegment) throws PlanItException {
		if (!(linkSegment instanceof MacroscopicLinkSegment)) {
			throw new PlanItException(
					"Tried to density per lane across an object which is not a MacroscopicLinkSegment.");
		}
		MacroscopicLinkSegment macroscopicLinkSegment = (MacroscopicLinkSegment) linkSegment;
		return macroscopicLinkSegment.getLinkSegmentType().getMaximumDensityPerLane();
	}

	/**
	 * Returns the external Id of the downstream node
	 * 
	 * @param linkSegment LinkSegment object containing the required data
	 * @return he external Id of the downstream node
	 * @throws PlanItException thrown if there is an error
	 */
	private long getDownstreamNodeExternalId(LinkSegment linkSegment) throws PlanItException {
		return ((Node) linkSegment.getDownstreamVertex()).getExternalId();
	}

	/**
	 * Returns the flow through the current link segment
	 * 
	 * @param linkSegment LinkSegment object containing the required data
	 * @param mode        current mode
	 * @return the flow through the current link segment
	 * @throws PlanItException thrown if there is an error
	 */
	private double getFlow(LinkSegment linkSegment, Mode mode) throws PlanItException {
		int id = (int) linkSegment.getId();
		TraditionalStaticAssignmentSimulationData simulationData = (TraditionalStaticAssignmentSimulationData) trafficAssignment
				.getSimulationData();
		double[] modalNetworkSegmentFlows = simulationData.getModalNetworkSegmentFlows(mode);
		return modalNetworkSegmentFlows[id];
	}

	/**
	 * Returns the Id of the downstream node
	 * 
	 * @param linkSegment LinkSegment object containing the required data
	 * @return the Id of the downstream node
	 * @throws PlanItException thrown if there is an error
	 */
	private long getDownstreamNodeId(LinkSegment linkSegment) throws PlanItException {
		return ((Node) linkSegment.getDownstreamVertex()).getId();
	}

	/**
	 * Returns the location of the downstream node
	 * 
	 * @param object LinkSegment object containing the required data
	 * @return the location of the downstream node
	 * @throws PlanItException thrown if there is an error
	 */
	private Object getDownstreamNodeLocation(LinkSegment linkSegment) throws PlanItException {
		DirectPosition centrePoint = linkSegment.getDownstreamVertex().getCentrePointGeometry();
		if (centrePoint == null) {
			return OutputFormatter.NOT_SPECIFIED;
		}
		double[] coordinates = linkSegment.getDownstreamVertex().getCentrePointGeometry().getCoordinate();
		return coordinates[0] + "-" + coordinates[1];
	}

	/**
	 * Returns the length of the current link segment
	 * 
	 * @param linkSegment LinkSegment object containing the required data
	 * @return the length of the current link segment
	 * @throws PlanItException thrown if there is an error
	 */
	private double getLength(LinkSegment linkSegment) throws PlanItException {
		return linkSegment.getParentLink().getLength();
	}

	/**
	 * Returns the travel cost (time) through the current link segment
	 * 
	 * @param linkSegment        LinkSegment object containing the required data
	 * @param mode               current mode
	 * @param timeUnitMultiplier multiplier to convert time durations to hours,
	 *                           minutes or seconds
	 * @return the travel cost (time) through the current link segment
	 * @throws PlanItException thrown if there is an error
	 */
	private double getLinkCost(LinkSegment linkSegment, Mode mode, double timeUnitMultiplier) throws PlanItException {
		int id = (int) linkSegment.getId();
		TraditionalStaticAssignmentSimulationData simulationData = (TraditionalStaticAssignmentSimulationData) trafficAssignment.getSimulationData();
		double[] modalNetworkSegmentCosts = simulationData.getModalNetworkSegmentCosts(mode);
		return modalNetworkSegmentCosts[id] * timeUnitMultiplier;
		//return trafficAssignment.getPhysicalCost().getSegmentCost(mode, linkSegment) * timeUnitMultiplier;
	}

	/**
	 * Returns the external Id of the current link segment
	 * 
	 * @param linkSegment LinkSegment object containing the required data
	 * @return the external Id of the current link segment
	 * @throws PlanItException thrown if there is an error
	 */
	private long getLinkSegmentExternalId(LinkSegment linkSegment) throws PlanItException {
		return linkSegment.getParentLink().getExternalId();
	}

	/**
	 * Returns the Id of the current link segment
	 * 
	 * @param linkSegment LinkSegment object containing the required data
	 * @return the Id of the current link segment
	 * @throws PlanItException thrown if there is an error
	 */
	private long getLinkSegmentId(LinkSegment linkSegment) throws PlanItException {
		return linkSegment.getId();
	}

	/**
	 * Returns the maximum speed through the current link segment
	 * 
	 * @param linkSegment MacroscopicLinkSegment object containing the required data
	 * @param mode        current mode
	 * @return the maximum speed through the current link segment
	 * @throws PlanItException thrown if there is an error
	 */
	private double getMaximumSpeed(LinkSegment linkSegment, Mode mode) throws PlanItException {
		if (!(linkSegment instanceof MacroscopicLinkSegment)) {
			throw new PlanItException(
					"Tried to read maximum speed of an object which is not a MacroscopicLinkSegment.");
		}
		MacroscopicLinkSegment macroscopicLinkSegment = (MacroscopicLinkSegment) linkSegment;
		return macroscopicLinkSegment.getMaximumSpeed(mode.getExternalId());
	}

	/**
	 * Returns the number of lanes of the current link
	 * 
	 * @param linkSegment LinkSegment object containing the required data
	 * @return the number of lanes of the current link
	 * @throws PlanItException thrown if there is an error
	 */
	private int getNumberOfLanes(LinkSegment linkSegment) throws PlanItException {
		return linkSegment.getNumberOfLanes();
	}

	/**
	 * Returns the external Id of the upstream node
	 * 
	 * @param linkSegment LinkSegment object containing the required data
	 * @return the external Id of the upstream node
	 * @throws PlanItException thrown if there is an error
	 */
	private long getUpstreamNodeExternalId(LinkSegment linkSegment) throws PlanItException {
		return ((Node) linkSegment.getUpstreamVertex()).getExternalId();
	}

	/**
	 * Returns the location of the upstream node
	 * 
	 * @param linkSegment LinkSegment object containing the required data
	 * @return the location of the upstream node
	 * @throws PlanItException thrown if there is an error
	 */
	private Object getUpstreamNodeLocation(LinkSegment linkSegment) throws PlanItException {
		DirectPosition centrePoint = linkSegment.getUpstreamVertex().getCentrePointGeometry();
		if (centrePoint == null) {
			return OutputFormatter.NOT_SPECIFIED;
		}
		double[] coordinates = linkSegment.getUpstreamVertex().getCentrePointGeometry().getCoordinate();
		return coordinates[0] + "-" + coordinates[1];
	}

	/**
	 * Returns the Id of the upstream node
	 * 
	 * @param linkSegment LinkSegment object containing the required data
	 * @return the Id of the upstream node
	 * @throws PlanItException thrown if there is an error
	 */
	private long getUpstreamNodeId(LinkSegment linkSegment) throws PlanItException {
		return ((Node) linkSegment.getUpstreamVertex()).getId();
	}

	/**
	 * Constructor
	 * 
	 * @param outputType        the output type for the current persistence
	 * @param trafficAssignment the traffic assignment used to provide the data
	 */
	public TraditionalStaticAssignmentLinkOutputTypeAdapter(OutputType outputType,
			TrafficAssignment trafficAssignment) {
		super(outputType, trafficAssignment);
	}

	/**
	 * Returns true if there is a flow through the current specified link segment
	 * for the specified mode
	 * 
	 * @param linkSegment specified link segment
	 * @param mode        specified mode
	 * @return true if there is flow through this link segment, false if the flow is
	 *         zero
	 */
	@Override
	public boolean isFlowPositive(LinkSegment linkSegment, Mode mode) {
		TraditionalStaticAssignmentSimulationData simulationData = (TraditionalStaticAssignmentSimulationData) trafficAssignment
				.getSimulationData();
		return (simulationData.getModalNetworkSegmentFlows(mode)[(int) linkSegment.getId()] > 0.0);
	}

    /**
     * Return the value of a specified output property of a link segment
     * 
     * @param outputProperty the specified output property
     * @param linkSegment the specified link segment
     * @param mode the current mode
     * @param timePeriod the current time period
     * @param timeUnitMultiplier the multiplier for time units 
     * @return the value of the specified output property (or an Exception if an error occurs)
     */
	@Override
	public Object getLinkOutputPropertyValue(OutputProperty outputProperty, LinkSegment linkSegment, Mode mode,
			TimePeriod timePeriod, double timeUnitMultiplier) {
		try {
			switch (outputProperty) {
			case CALCULATED_SPEED:
				return getCalculatedSpeed(linkSegment, mode);
			case CAPACITY_PER_LANE:
				return getCapacityPerLane(linkSegment);
			case DENSITY:
				return getFlowDensity(linkSegment);
			case DOWNSTREAM_NODE_EXTERNAL_ID:
				return getDownstreamNodeExternalId(linkSegment);
			case DOWNSTREAM_NODE_ID:
				return getDownstreamNodeId(linkSegment);
			case DOWNSTREAM_NODE_LOCATION:
				return getDownstreamNodeLocation(linkSegment);
			case FLOW:
				return getFlow(linkSegment, mode);
			case ITERATION_INDEX:
				return IterationIndexOutputProperty.getIterationIndex(trafficAssignment);
			case LENGTH:
				return getLength(linkSegment);
			case LINK_COST:
				return getLinkCost(linkSegment, mode, timeUnitMultiplier);
			case LINK_SEGMENT_EXTERNAL_ID:
				return getLinkSegmentExternalId(linkSegment);
			case LINK_SEGMENT_ID:
				return getLinkSegmentId(linkSegment);
			case MAXIMUM_SPEED:
				return getMaximumSpeed(linkSegment, mode);
			case MODE_EXTERNAL_ID:
				return ModeExternalIdOutputProperty.getModeExternalId(mode);
			case MODE_ID:
				return ModeIdOutputProperty.getModeId(mode);
			case NUMBER_OF_LANES:
				return getNumberOfLanes(linkSegment);
			case RUN_ID:
				return RunIdOutputProperty.getRunId(trafficAssignment);
			case TIME_PERIOD_EXTERNAL_ID:
				return TimePeriodExternalIdOutputProperty.getTimePeriodExternalId(timePeriod);
			case TIME_PERIOD_ID:
				return TimePeriodIdOutputProperty.getTimePeriodId(timePeriod);
			case TOTAL_COST_TO_END_NODE:
				return OutputFormatter.NOT_SPECIFIED;
			case UPSTREAM_NODE_EXTERNAL_ID:
				return getUpstreamNodeExternalId(linkSegment);
			case UPSTREAM_NODE_ID:
				return getUpstreamNodeId(linkSegment);
			case UPSTREAM_NODE_LOCATION:
				return getUpstreamNodeLocation(linkSegment);
			default:
				return new PlanItException("Tried to find link property of "
						+ BaseOutputProperty.convertToBaseOutputProperty(outputProperty).getName()
						+ " which is not applicable for links.");
			}
		} catch (PlanItException e) {
			return e;
		}
	}

}
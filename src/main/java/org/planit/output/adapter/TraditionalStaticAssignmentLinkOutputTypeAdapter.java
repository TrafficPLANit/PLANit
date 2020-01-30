package org.planit.output.adapter;

import org.planit.data.TraditionalStaticAssignmentSimulationData;
import org.planit.exceptions.PlanItException;
import org.planit.network.physical.LinkSegment;
import org.planit.output.enums.OutputType;
import org.planit.output.property.BaseOutputProperty;
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
		double[] modalNetworkSegmentCosts = simulationData.getModalLinkSegmentCosts(mode);
		double travelTime = modalNetworkSegmentCosts[id];
		double length = linkSegment.getParentLink().getLength();
		return length / travelTime;
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
		double[] modalNetworkSegmentCosts = simulationData.getModalLinkSegmentCosts(mode);
		return modalNetworkSegmentCosts[id] * timeUnitMultiplier;
	}

	/**
	 * Returns the VC ratio for the link over all modes
	 * 
	 * @param linkSegment  LinkSegment object containing the required data
	 * @return VC ratio for the link
	 * @throws PlanItException thrown if there is an error
	 */
	private double getVCRatio(LinkSegment linkSegment) throws PlanItException {
		double totalFlow = 0.0;
		for (Mode mode : Mode.getAllModes()) {
			totalFlow += getFlow(linkSegment, mode);
		}
		double capacityPerLane = getCapacityPerLane(linkSegment);
		return totalFlow/(linkSegment.getNumberOfLanes() * capacityPerLane);
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
			Object obj = getCommonPropertyValue(outputProperty, mode, timePeriod);
			if (obj != null) {
				return obj;
			}
			obj = super.getLinkOutputPropertyValue(outputProperty, linkSegment, mode, timePeriod, timeUnitMultiplier) ;
			if (obj != null) {
				return obj;
			}
			switch (outputProperty) {
			case CALCULATED_SPEED:
				return getCalculatedSpeed(linkSegment, mode);
			case FLOW:
				return getFlow(linkSegment, mode);
			case LINK_COST:
				return getLinkCost(linkSegment, mode, timeUnitMultiplier);
			case VC_RATIO:
				return getVCRatio(linkSegment);
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
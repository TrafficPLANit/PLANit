package org.planit.output.adapter;

import java.util.Collection;

import org.planit.data.TraditionalStaticAssignmentSimulationData;
import org.planit.exceptions.PlanItException;
import org.planit.output.enums.OutputType;
import org.planit.output.property.BaseOutputProperty;
import org.planit.output.property.OutputProperty;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Mode;

/**
 * Adapter providing access to the data of the TraditionalStaticAssignment class
 * relevant for link outputs without exposing the internals of the traffic
 * assignment class itself
 *
 * @author markr
 *
 */
public class TraditionalStaticAssignmentLinkOutputTypeAdapter extends LinkOutputTypeAdapterImpl {

	/** the modes available on this assignment */
	protected final Collection<Mode> modes;

	/**
	 * Returns the value of the calculated speed
	 *
	 * @param linkSegment LinkSegment containing data which may be required
	 * @param mode        current mode
	 * @return the calculated speed across the link
	 * @throws PlanItException thrown if there is an error
	 */
	private double getCalculatedSpeed(final LinkSegment linkSegment, final Mode mode) throws PlanItException {
		final int id = (int) linkSegment.getId();
		final TraditionalStaticAssignmentSimulationData simulationData = (TraditionalStaticAssignmentSimulationData) trafficAssignment
				.getSimulationData();
		final double[] modalNetworkSegmentCosts = simulationData.getModalLinkSegmentCosts(mode);
		final double travelTime = modalNetworkSegmentCosts[id];
		final double length = linkSegment.getParentLink().getLength();
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
	private double getFlow(final LinkSegment linkSegment, final Mode mode) throws PlanItException {
		final int id = (int) linkSegment.getId();
		final TraditionalStaticAssignmentSimulationData simulationData = (TraditionalStaticAssignmentSimulationData) trafficAssignment
				.getSimulationData();
		final double[] modalNetworkSegmentFlows = simulationData.getModalNetworkSegmentFlows(mode);
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
	private double getLinkCost(final LinkSegment linkSegment, final Mode mode, final double timeUnitMultiplier) throws PlanItException {
		final int id = (int) linkSegment.getId();
		final TraditionalStaticAssignmentSimulationData simulationData = (TraditionalStaticAssignmentSimulationData) trafficAssignment.getSimulationData();
		final double[] modalNetworkSegmentCosts = simulationData.getModalLinkSegmentCosts(mode);
		return modalNetworkSegmentCosts[id] * timeUnitMultiplier;
	}

  /**
   * Returns the flow multiplied by travel cost (time) through the current link segment
   *
   * @param linkSegment        LinkSegment object containing the required data
   * @param mode               current mode
   * @param timeUnitMultiplier multiplier to convert time durations to hours,
   *                           minutes or seconds
   * @return the travel cost (time) through the current link segment
   * @throws PlanItException thrown if there is an error
   */
	private double getCostTimesFlow(final LinkSegment linkSegment, final Mode mode, final double timeUnitMultiplier) throws PlanItException {
	  return getLinkCost(linkSegment, mode, timeUnitMultiplier) * getFlow(linkSegment, mode);
	}

	/**
	 * Returns the VC ratio for the link over all modes
	 *
	 * @param linkSegment  LinkSegment object containing the required data
	 * @param modes to collect the overall v/c ratio for
	 * @return VC ratio for the link
	 * @throws PlanItException thrown if there is an error
	 */
	private double getVCRatio(final LinkSegment linkSegment) throws PlanItException {
		double totalFlow = 0.0;
		for (final Mode mode : modes) {
			totalFlow += getFlow(linkSegment, mode);
		}
		final double capacityPerLane = getCapacityPerLane(linkSegment);
		return totalFlow/(linkSegment.getNumberOfLanes() * capacityPerLane);
	}

	/**
	 * Constructor
	 *
	 * @param outputType        the output type for the current persistence
	 * @param trafficAssignment the traffic assignment used to provide the data
	 */
	public TraditionalStaticAssignmentLinkOutputTypeAdapter(
			final OutputType outputType, final TrafficAssignment trafficAssignment, final Collection<Mode> modes) {
		super(outputType, trafficAssignment);
		this.modes = modes;
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
	public boolean isFlowPositive(final LinkSegment linkSegment, final Mode mode) {
		final TraditionalStaticAssignmentSimulationData simulationData = (TraditionalStaticAssignmentSimulationData) trafficAssignment
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
	public Object getLinkOutputPropertyValue(
			final OutputProperty outputProperty,
			final LinkSegment linkSegment,
			final Mode mode,
			final TimePeriod timePeriod,
			final double timeUnitMultiplier) {
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
			case COST_TIMES_FLOW:
			  return getCostTimesFlow(linkSegment, mode, timeUnitMultiplier);
			default:
				return new PlanItException("Tried to find link property of "
						+ BaseOutputProperty.convertToBaseOutputProperty(outputProperty).getName()
						+ " which is not applicable for links.");
			}
		} catch (final PlanItException e) {
			return e;
		}
	}
}
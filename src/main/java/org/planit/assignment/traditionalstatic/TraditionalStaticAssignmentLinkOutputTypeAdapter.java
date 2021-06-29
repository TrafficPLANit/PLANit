package org.planit.assignment.traditionalstatic;

import java.util.Optional;

import org.planit.assignment.TrafficAssignment;
import org.planit.output.adapter.LinkOutputTypeAdapter;
import org.planit.output.adapter.MacroscopicLinkOutputTypeAdapterImpl;
import org.planit.output.enums.OutputType;
import org.planit.output.property.BaseOutputProperty;
import org.planit.output.property.OutputProperty;
import org.planit.utils.time.TimePeriod;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegment;

/**
 * Adapter providing access to the data of the TraditionalStaticAssignment class relevant for link outputs without exposing the internals of the traffic assignment class itself
 *
 * @author markr
 *
 */
public class TraditionalStaticAssignmentLinkOutputTypeAdapter extends MacroscopicLinkOutputTypeAdapterImpl<MacroscopicLinkSegment> implements LinkOutputTypeAdapter<MacroscopicLinkSegment> {

  /**
   * track parent assignment as its actual class
   */
  protected TraditionalStaticAssignment theAssignment;

  /**
   * Returns the value of the calculated speed
   *
   * @param linkSegment LinkSegment containing data which may be required
   * @param mode        current mode
   * @return the calculated speed across the link
   * @throws PlanItException thrown if there is an error
   */
  private Optional<Double> getCalculatedSpeed(final MacroscopicLinkSegment linkSegment, final Mode mode) throws PlanItException {
    final int id = (int) linkSegment.getId();
    final double[] modalNetworkSegmentCosts = theAssignment.getIterationData().getModalLinkSegmentCosts(mode);
    final double travelTime = modalNetworkSegmentCosts[id];
    final double length = linkSegment.getParentLink().getLengthKm();
    return Optional.of(length / travelTime);
  }

  /**
   * Returns the flow through the current link segment
   *
   * @param linkSegment LinkSegment object containing the required data
   * @param mode        current mode
   * @return the flow through the current link segment
   * @throws PlanItException thrown if there is an error
   */
  private Optional<Double> getFlow(final MacroscopicLinkSegment linkSegment, final Mode mode) throws PlanItException {
    final int id = (int) linkSegment.getId();
    final double[] modalNetworkSegmentFlows = theAssignment.getIterationData().getModeSpecificData().get(mode).getCurrentSegmentFlows();
    return Optional.of(modalNetworkSegmentFlows[id]);
  }

  /**
   * Returns the travel cost (time) through the current link segment
   *
   * @param linkSegment        LinkSegment object containing the required data
   * @param mode               current mode
   * @param timeUnitMultiplier multiplier to convert time durations to hours, minutes or seconds
   * @return the travel cost (time) through the current link segment
   * @throws PlanItException thrown if there is an error
   */
  private Optional<Double> getLinkCost(final MacroscopicLinkSegment linkSegment, final Mode mode, final double timeUnitMultiplier) throws PlanItException {
    final int id = (int) linkSegment.getId();
    final double[] modalNetworkSegmentCosts = theAssignment.getIterationData().getModalLinkSegmentCosts(mode);
    return Optional.of(modalNetworkSegmentCosts[id] * timeUnitMultiplier);
  }

  /**
   * Returns the flow multiplied by travel cost (time) through the current link segment
   *
   * @param linkSegment        LinkSegment object containing the required data
   * @param mode               current mode
   * @param timeUnitMultiplier multiplier to convert time durations to hours, minutes or seconds
   * @return the travel cost (time) through the current link segment
   * @throws PlanItException thrown if there is an error
   */
  private Optional<Double> getCostTimesFlow(final MacroscopicLinkSegment linkSegment, final Mode mode, final double timeUnitMultiplier) throws PlanItException {
    return Optional.of(getLinkCost(linkSegment, mode, timeUnitMultiplier).get() * getFlow(linkSegment, mode).get());
  }

  /**
   * Returns the VC ratio for the link over all modes
   *
   * @param linkSegment LinkSegment object containing the required data
   * @return VC ratio for the link
   * @throws PlanItException thrown if there is an error
   */
  private Optional<Double> getVCRatio(final MacroscopicLinkSegment linkSegment) throws PlanItException {
    double totalFlow = 0.0;
    for (final Mode mode : trafficAssignment.getTransportNetwork().getInfrastructureNetwork().modes) {
      totalFlow += getFlow(linkSegment, mode).get();
    }
    final double capacityPerLane = getCapacityPerLane(linkSegment).get();
    return Optional.of(totalFlow / (linkSegment.getNumberOfLanes() * capacityPerLane));
  }

  /**
   * Constructor
   *
   * @param outputType        the output type for the current persistence
   * @param trafficAssignment the traffic assignment used to provide the data
   */
  public TraditionalStaticAssignmentLinkOutputTypeAdapter(final OutputType outputType, final TrafficAssignment trafficAssignment) {
    super(outputType, trafficAssignment);
    this.theAssignment = (TraditionalStaticAssignment) this.trafficAssignment;
  }

  /**
   * Returns true if there is a flow through the current specified link segment for the specified mode
   *
   * @param linkSegment specified link segment
   * @param mode        specified mode
   * @return true if there is flow through this link segment, false if the flow is zero
   */
  @Override
  public Optional<Boolean> isFlowPositive(final MacroscopicLinkSegment linkSegment, final Mode mode) {
    return Optional.of(theAssignment.getIterationData().getModeSpecificData().get(mode).getCurrentSegmentFlows()[(int) linkSegment.getId()] > 0.0);
  }

  /**
   * Return the value of a specified output property of a link segment
   *
   * @param outputProperty     the specified output property
   * @param linkSegment        the specified link segment
   * @param mode               the current mode
   * @param timePeriod         the current time period
   * @param timeUnitMultiplier the multiplier for time units
   * @return the value of the specified output property (or an Exception if an error occurs)
   */
  @Override
  public Optional<?> getLinkSegmentOutputPropertyValue(final OutputProperty outputProperty, final MacroscopicLinkSegment linkSegment, final Mode mode, final TimePeriod timePeriod,
      final double timeUnitMultiplier) {
    try {
      Optional<?> value = getOutputTypeIndependentPropertyValue(outputProperty, mode, timePeriod);
      if (value.isPresent()) {
        return value;
      }
      
      value = super.getLinkSegmentOutputPropertyValue(outputProperty, linkSegment, mode, timePeriod, timeUnitMultiplier);
      if (value.isPresent()) {
        return value;
      }
      
      switch (outputProperty) {
      case CALCULATED_SPEED:
        return getCalculatedSpeed(linkSegment, mode);
      case FLOW:
        return getFlow(linkSegment, mode);
      case LINK_SEGMENT_COST:
        return getLinkCost(linkSegment, mode, timeUnitMultiplier);
      case VC_RATIO:
        return getVCRatio(linkSegment);
      case COST_TIMES_FLOW:
        return getCostTimesFlow(linkSegment, mode, timeUnitMultiplier);
      default:
        return Optional.of(String.format(
            "Tried to find link property of %s which is not applicable for links", BaseOutputProperty.convertToBaseOutputProperty(outputProperty).getName() ));
      }
    } catch (final PlanItException e) {
      return Optional.of(e.getMessage());
    }
  }

}

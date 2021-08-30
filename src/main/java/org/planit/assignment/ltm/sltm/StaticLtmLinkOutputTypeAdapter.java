package org.planit.assignment.ltm.sltm;

import java.util.Optional;

import org.planit.assignment.TrafficAssignment;
import org.planit.output.adapter.MacroscopicLinkOutputTypeAdapterImpl;
import org.planit.output.enums.OutputType;
import org.planit.output.property.BaseOutputProperty;
import org.planit.output.property.OutputProperty;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.time.TimePeriod;

/**
 * Adapter providing access to the data of the StaticLtm class relevant for link outputs without exposing the internals of the traffic assignment class itself
 *
 * @author markr
 *
 */
public class StaticLtmLinkOutputTypeAdapter extends MacroscopicLinkOutputTypeAdapterImpl {

  /**
   * {@inheritDoc}
   */
  @Override
  protected StaticLtm getAssignment() {
    return (StaticLtm) super.getAssignment();
  }

  /**
   * Returns the value of the calculated speed based on the link segment travel time and length of the link segment
   *
   * @param linkSegment to use
   * @param mode        current mode
   * @return the calculated speed across the link segment
   */
  private Optional<Double> getCalculatedSpeed(final MacroscopicLinkSegment linkSegment, final Mode mode) {
    final int id = (int) linkSegment.getId();
    final double[] modalLinkSegmentsTravelTimeHour = getAssignment().getIterationData().getLinkSegmentTravelTimeHour(mode);
    final double travelTimeHour = modalLinkSegmentsTravelTimeHour[id];
    final double length = linkSegment.getParentLink().getLengthKm();
    return Optional.of(length / travelTimeHour);
  }

  /**
   * Returns the inflow in pcu per hour through the current link segment
   *
   * @param linkSegment to use
   * @param mode        current mode
   * @return the inflow through the current link segment
   */
  private Optional<Double> getInFlow(final MacroscopicLinkSegment linkSegment, final Mode mode) {
    return Optional.of(getAssignment().getIterationData().getNetworkLoading().getCurrentInflowsPcuH()[(int) linkSegment.getId()]);
  }

  /**
   * Returns the outflow in pcu per hour through the current link segment
   *
   * @param linkSegment to use
   * @param mode        current mode
   * @return the outflow through the current link segment
   */
  private Optional<Double> getOutFlow(final MacroscopicLinkSegment linkSegment, final Mode mode) {
    return Optional.of(getAssignment().getIterationData().getNetworkLoading().getCurrentOutflowsPcuH()[(int) linkSegment.getId()]);
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
    final double[] modalNetworkSegmentCosts = getAssignment().getIterationData().getModalLinkSegmentCosts(mode);
    return Optional.of(modalNetworkSegmentCosts[id] * timeUnitMultiplier);
  }

  /**
   * Returns the (out)flow multiplied by travel cost (time) through the current link segment
   *
   * @param linkSegment        LinkSegment object containing the required data
   * @param mode               current mode
   * @param timeUnitMultiplier multiplier to convert time durations to hours, minutes or seconds
   * @return the travel cost (time) through the current link segment
   * @throws PlanItException thrown if there is an error
   */
  private Optional<Double> getCostTimesFlow(final MacroscopicLinkSegment linkSegment, final Mode mode, final double timeUnitMultiplier) throws PlanItException {
    return Optional.of(getLinkCost(linkSegment, mode, timeUnitMultiplier).get() * getOutFlow(linkSegment, mode).get());
  }

  /**
   * Returns the Vc ratio for the link over all modes. HEre we use the inflow rate as it is a better indicator of the busyness. Generally though the Vc ratio is quite meaningless,
   * especially in a capacity constrained asssignment such as sLTM.
   *
   * @param linkSegment LinkSegment object containing the required data
   * @return VC ratio for the link segment
   * @throws PlanItException thrown if there is an error
   */
  private Optional<Double> getVcRatio(final MacroscopicLinkSegment linkSegment) throws PlanItException {
    double totalFlow = 0.0;
    for (final Mode mode : getAssignment().getIterationData().getNetworkLoading().getSupportedModes()) {
      totalFlow += getInFlow(linkSegment, mode).get();
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
  public StaticLtmLinkOutputTypeAdapter(final OutputType outputType, final TrafficAssignment trafficAssignment) {
    super(outputType, trafficAssignment);
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
    return Optional.of(getOutFlow(linkSegment, mode).get() > 0.0);
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
        // not ideal, but if someone uses flow property, we provide the outflow rate
        return getOutFlow(linkSegment, mode);
      case OUTFLOW:
        return getOutFlow(linkSegment, mode);
      case INFLOW:
        return getInFlow(linkSegment, mode);
      case LINK_SEGMENT_COST:
        return getLinkCost(linkSegment, mode, timeUnitMultiplier);
      case VC_RATIO:
        return getVcRatio(linkSegment);
      case COST_TIMES_FLOW:
        return getCostTimesFlow(linkSegment, mode, timeUnitMultiplier);
      default:
        return Optional
            .of(String.format("Tried to find link property of %s which is not applicable for links", BaseOutputProperty.convertToBaseOutputProperty(outputProperty).getName()));
      }
    } catch (final PlanItException e) {
      return Optional.of(e.getMessage());
    }
  }

}

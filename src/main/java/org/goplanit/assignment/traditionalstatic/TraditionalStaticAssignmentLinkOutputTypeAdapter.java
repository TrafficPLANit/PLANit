package org.goplanit.assignment.traditionalstatic;

import java.util.Optional;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.output.adapter.MacroscopicLinkOutputTypeAdapterImpl;
import org.goplanit.output.enums.OutputType;
import org.goplanit.output.property.OutputProperty;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.time.TimePeriod;

/**
 * Adapter providing access to the data of the TraditionalStaticAssignment class relevant for link outputs without exposing the internals of the traffic assignment class itself
 *
 * @author markr
 *
 */
public class TraditionalStaticAssignmentLinkOutputTypeAdapter extends MacroscopicLinkOutputTypeAdapterImpl {

  /**
   * {@inheritDoc}
   */
  @Override
  protected TraditionalStaticAssignment getAssignment() {
    return (TraditionalStaticAssignment) super.getAssignment();
  }

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
    final double[] modalNetworkSegmentCosts = getAssignment().getIterationData().getModalLinkSegmentCosts(mode);
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
    final double[] modalNetworkSegmentFlows = getAssignment().getIterationData().getModeSpecificData().get(mode).getCurrentSegmentFlows();
    return Optional.of(modalNetworkSegmentFlows[id]);
  }

  /**
   * Returns the travel cost (time) through the current link segment
   *
   * @param linkSegment LinkSegment object containing the required data
   * @param mode        current mode
   * @return the travel cost (time) through the current link segment
   * @throws PlanItException thrown if there is an error
   */
  private Optional<Double> getLinkCostTravelTime(final MacroscopicLinkSegment linkSegment, final Mode mode) throws PlanItException {
    final int id = (int) linkSegment.getId();
    final double[] modalNetworkSegmentCosts = getAssignment().getIterationData().getModalLinkSegmentCosts(mode);
    return Optional.of(modalNetworkSegmentCosts[id]);
  }

  /**
   * Returns the flow multiplied by travel cost (time) through the current link segment
   *
   * @param linkSegment LinkSegment object containing the required data
   * @param mode        current mode
   * @return the travel cost (time) through the current link segment
   * @throws PlanItException thrown if there is an error
   */
  private Optional<Double> getCostTimesFlow(final MacroscopicLinkSegment linkSegment, final Mode mode) throws PlanItException {
    return Optional.of(getLinkCostTravelTime(linkSegment, mode).get() * getFlow(linkSegment, mode).get());
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
    for (var mode : getAssignment().getTransportNetwork().getInfrastructureNetwork().getModes()) {
      totalFlow += getFlow(linkSegment, mode).get();
    }
    final double capacityPerLane = getCapacityPerLanePcuHour(linkSegment).get();
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
    return Optional.of(getAssignment().getIterationData().getModeSpecificData().get(mode).getCurrentSegmentFlows()[(int) linkSegment.getId()] > 0.0);
  }

  /**
   * Return the value of a specified output property of a link segment
   *
   * @param outputProperty the specified output property
   * @param linkSegment    the specified link segment
   * @param mode           the current mode
   * @param timePeriod     the current time period
   * @return the value of the specified output property (or an Exception if an error occurs)
   */
  @Override
  public Optional<?> getLinkSegmentOutputPropertyValue(final OutputProperty outputProperty, final MacroscopicLinkSegment linkSegment, final Mode mode,
      final TimePeriod timePeriod) {

    Optional<?> value = Optional.empty();
    try {
      value = getOutputTypeIndependentPropertyValue(outputProperty, mode, timePeriod);
      if (value.isPresent()) {
        return value;
      }

      value = super.getLinkSegmentOutputPropertyValue(outputProperty, linkSegment, mode, timePeriod);
      if (value.isPresent()) {
        return value;
      }

      switch (outputProperty.getOutputPropertyType()) {
      case CALCULATED_SPEED:
        value = getCalculatedSpeed(linkSegment, mode);
        break;
      case FLOW:
        value = getFlow(linkSegment, mode);
        break;
      case INFLOW:
        value = getFlow(linkSegment, mode);
        break;
      case OUTFLOW:
        value = getFlow(linkSegment, mode);
        break;
      case LINK_SEGMENT_COST:
        value = getLinkCostTravelTime(linkSegment, mode);
        break;
      case VC_RATIO:
        value = getVCRatio(linkSegment);
        break;
      case COST_TIMES_FLOW:
        value = getCostTimesFlow(linkSegment, mode);
        break;
      default:
        throw new PlanItException("Tried to find link property of %s which is not applicable for links", outputProperty.getName());
      }

      if (outputProperty.supportsUnitOverride() && outputProperty.isUnitOverride()) {
        value = createConvertedUnitsValue(outputProperty, value);
      }
    } catch (final PlanItException e) {
      value = Optional.of(e.getMessage());
    }
    return value;
  }

}

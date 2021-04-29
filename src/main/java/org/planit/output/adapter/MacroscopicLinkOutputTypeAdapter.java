package org.planit.output.adapter;

import java.util.Optional;

import org.planit.output.property.OutputProperty;
import org.planit.utils.time.TimePeriod;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.Vertex;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegment;

/**
 * Interface defining the methods required for a link output adapter
 * 
 * @author gman6028, markr
 *
 */
public interface MacroscopicLinkOutputTypeAdapter<LS extends MacroscopicLinkSegment> extends LinkOutputTypeAdapter<LS> {
  
  /**
   * Returns the value of the capacity per lane
   * 
   * @param linkSegment LinkSegment containing data which may be required
   * @return the capacity per lane across this link segment
   * @throws PlanItException thrown if there is an error
   */
  public default Optional<Double> getCapacityPerLane(LS linkSegment) throws PlanItException {
    return Optional.of(linkSegment.getLinkSegmentType().getCapacityPerLane());
  }

  /**
   * Return the link segment type of the current link segment
   * 
   * @param linkSegment the current link segment
   * @return the link segment type
   * @throws PlanItException thrown if there is an error
   */
  public default Optional<String> getLinkType(LS linkSegment) throws PlanItException {
    return Optional.of(linkSegment.getLinkSegmentType().getName());
  }

  /**
   * Returns the flow density of the current link
   * 
   * @param linkSegment LinkSegment containing data which may be required
   * @return the flow density of the current link
   * @throws PlanItException thrown if there is an error
   */
  public default  Optional<Double> getMaximumDensity(LS linkSegment) throws PlanItException {
    return Optional.of(linkSegment.getLinkSegmentType().getMaximumDensityPerLane());
  }

  /**
   * Returns the external Id of the downstream node
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return he external Id of the downstream node
   * @throws PlanItException thrown if there is an error
   */
  public default  Optional<String> getDownstreamNodeExternalId(LS linkSegment) throws PlanItException {
    return Optional.of(((Vertex) linkSegment.getDownstreamVertex()).getExternalId());
  }

  /**
   * Returns the maximum speed through the current link segment
   * 
   * @param linkSegment MacroscopicLinkSegment object containing the required data
   * @param mode        current mode
   * @return the maximum speed through the current link segment
   * @throws PlanItException thrown if there is an error
   */
  public default Optional<Double> getMaximumSpeed(LS linkSegment, Mode mode) throws PlanItException {
    return Optional.of(linkSegment.getModelledSpeedLimitKmH(mode));
  }

  /**
   * collect the infrastructure layer id this mode resides on
   * 
   * @param mode to collect layer id for
   * @return infrastructure layer id, null if not found
   */
  public abstract Optional<Long> getInfrastructureLayerIdForMode(Mode mode);

  /**
   * Returns true if there is a flow through the current specified link segment for the specified mode
   * 
   * @param linkSegment specified link segment
   * @param mode        specified mode
   * @return true is there is flow through this link segment, false if the flow is zero
   */
  public abstract Optional<Boolean> isFlowPositive(LS linkSegment, Mode mode);

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
  public abstract Optional<?> getLinkSegmentOutputPropertyValue(OutputProperty outputProperty, LS linkSegment, Mode mode, TimePeriod timePeriod, double timeUnitMultiplier);
}
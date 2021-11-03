package org.goplanit.output.adapter;

import java.util.Optional;

import org.goplanit.output.property.OutputProperty;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.time.TimePeriod;

/**
 * Interface defining the methods required for a macroscopic link (segment) output adapter
 * 
 * @author gman6028, markr
 *
 */
public interface MacroscopicLinkOutputTypeAdapter extends UntypedLinkOutputTypeAdapter<MacroscopicLinkSegment> {

  /**
   * Returns the value of the capacity per lane
   * 
   * @param linkSegment LinkSegment containing data which may be required
   * @return the capacity per lane across this link segment
   * @throws PlanItException thrown if there is an error
   */
  public default Optional<Double> getCapacityPerLanePcuHour(MacroscopicLinkSegment linkSegment) throws PlanItException {
    return Optional.of(linkSegment.getLinkSegmentType().getExplicitCapacityPerLaneOrDefault());
  }

  /**
   * Return the link segment type name of the current link segment
   * 
   * @param linkSegment the current link segment
   * @return the link segment type name
   * @throws PlanItException thrown if there is an error
   */
  public default Optional<String> getLinkSegmentTypeName(MacroscopicLinkSegment linkSegment) throws PlanItException {
    return Optional.of(linkSegment.getLinkSegmentType().getName());
  }

  /**
   * Return the link segment type id of the current link segment
   * 
   * @param linkSegment the current link segment
   * @return the link segment type id
   * @throws PlanItException thrown if there is an error
   */
  public default Optional<Long> getLinkSegmentTypeId(MacroscopicLinkSegment linkSegment) throws PlanItException {
    return Optional.of(linkSegment.getLinkSegmentType().getId());
  }

  /**
   * Return the link segment type xml id of the current link segment
   * 
   * @param linkSegment the current link segment
   * @return the link segment type xml id
   * @throws PlanItException thrown if there is an error
   */
  public default Optional<String> getLinkSegmentTypeXmlId(MacroscopicLinkSegment linkSegment) throws PlanItException {
    return Optional.of(linkSegment.getLinkSegmentType().getXmlId());
  }

  /**
   * Returns the flow density of the current link
   * 
   * @param linkSegment LinkSegment containing data which may be required
   * @return the flow density of the current link
   * @throws PlanItException thrown if there is an error
   */
  public default Optional<Double> getMaximumDensity(MacroscopicLinkSegment linkSegment) throws PlanItException {
    return Optional.of(linkSegment.getLinkSegmentType().getExplicitMaximumDensityPerLaneOrDefault());
  }

  /**
   * Returns the external Id of the downstream node
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return he external Id of the downstream node
   * @throws PlanItException thrown if there is an error
   */
  public default Optional<String> getDownstreamNodeExternalId(MacroscopicLinkSegment linkSegment) throws PlanItException {
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
  public default Optional<Double> getMaximumSpeed(MacroscopicLinkSegment linkSegment, Mode mode) throws PlanItException {
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
  public abstract Optional<Boolean> isFlowPositive(MacroscopicLinkSegment linkSegment, Mode mode);

  /**
   * Return the value of a specified output property of a link segment
   * 
   * @param outputProperty the specified output property
   * @param linkSegment    the specified link segment
   * @param mode           the current mode
   * @param timePeriod     the current time period
   * @return the value of the specified output property (or an Exception if an error occurs)
   */
  public abstract Optional<?> getLinkSegmentOutputPropertyValue(OutputProperty outputProperty, MacroscopicLinkSegment linkSegment, Mode mode, TimePeriod timePeriod);
}

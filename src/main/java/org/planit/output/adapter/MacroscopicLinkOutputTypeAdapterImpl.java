package org.planit.output.adapter;

import java.util.Optional;
import java.util.logging.Logger;

import org.planit.assignment.TrafficAssignment;
import org.planit.network.layer.macroscopic.MacroscopicPhysicalLayer;
import org.planit.output.enums.OutputType;
import org.planit.output.property.OutputProperty;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.layer.TransportLayer;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.layer.physical.LinkSegments;
import org.planit.utils.time.TimePeriod;

/**
 * Top-level abstract class which defines the common methods required by Link output type adapters
 * 
 * @author gman6028, markr
 *
 */
public abstract class MacroscopicLinkOutputTypeAdapterImpl<LS extends MacroscopicLinkSegment> extends LinkOutputTypeAdapterImpl<LS>
    implements MacroscopicLinkOutputTypeAdapter<LS> {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(MacroscopicLinkOutputTypeAdapterImpl.class.getCanonicalName());

  /**
   * Constructor
   * 
   * @param outputType        the OutputType this adapter corresponds to
   * @param trafficAssignment TrafficAssignment object which this adapter wraps
   */
  public MacroscopicLinkOutputTypeAdapterImpl(OutputType outputType, TrafficAssignment trafficAssignment) {
    super(outputType, trafficAssignment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<Long> getInfrastructureLayerIdForMode(Mode mode) {
    TransportLayer networkLayer = this.trafficAssignment.getTransportNetwork().getInfrastructureNetwork().getLayerByMode(mode);
    return Optional.of(networkLayer != null ? networkLayer.getId() : null);
  }

  /**
   * Provide access to the macroscopic link segments
   * 
   * @param infrastructureLayerId to use
   */
  @SuppressWarnings("unchecked")
  @Override
  public LinkSegments<LS> getPhysicalLinkSegments(long infrastructureLayerId) {
    TransportLayer networkLayer = this.trafficAssignment.getTransportNetwork().getInfrastructureNetwork().transportLayers.get(infrastructureLayerId);
    if (networkLayer instanceof MacroscopicPhysicalLayer) {
      return (LinkSegments<LS>) ((MacroscopicPhysicalLayer) networkLayer).linkSegments;
    }
    LOGGER.warning(String.format("cannot collect macroscopic physical link segments from infrastructure layer %s, as it is not a macroscopic physical network layer",
        networkLayer.getXmlId()));
    return null;
  }

  /**
   * Return the value of a specified output property of a link segment
   * 
   * The DENSITY case should never be called for TraditionalStaticAssignment.
   * 
   * @param outputProperty     the specified output property
   * @param linkSegment        the specified link segment
   * @param mode               the current mode
   * @param timePeriod         the current time period
   * @param timeUnitMultiplier the multiplier for time units
   * @return the value of the specified output property (or an Exception message if an error occurs)
   */
  @Override
  public Optional<?> getLinkSegmentOutputPropertyValue(OutputProperty outputProperty, LS linkSegment, Mode mode, TimePeriod timePeriod, double timeUnitMultiplier) {
    try {
      Optional<?> value = getOutputTypeIndependentPropertyValue(outputProperty, mode, timePeriod);
      if (value.isPresent()) {
        return value;
      }

      value = getLinkSegmentOutputPropertyValue(outputProperty, linkSegment);
      if (value.isPresent()) {
        return value;
      }

      /* specific to macroscopic link segment */
      switch (outputProperty) {
      case CAPACITY_PER_LANE:
        return getCapacityPerLane(linkSegment);
      case MAXIMUM_DENSITY:
        return getMaximumDensity(linkSegment);
      case MAXIMUM_SPEED:
        return getMaximumSpeed(linkSegment, mode);
      case LINK_SEGMENT_TYPE_NAME:
        return getLinkSegmentTypeName(linkSegment);
      case LINK_SEGMENT_TYPE_ID:
        return getLinkSegmentTypeId(linkSegment);
      case LINK_SEGMENT_TYPE_XML_ID:
        return getLinkSegmentTypeXmlId(linkSegment);
      default:
        return Optional.empty();
      }
    } catch (PlanItException e) {
      return Optional.of(e.getMessage());
    }
  }

}

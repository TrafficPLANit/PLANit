package org.planit.output.adapter;

import java.util.Optional;
import java.util.logging.Logger;

import org.planit.assignment.TrafficAssignment;
import org.planit.output.enums.OutputType;
import org.planit.output.property.OutputProperty;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.layer.MacroscopicNetworkLayer;
import org.planit.utils.network.layer.TransportLayer;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegments;
import org.planit.utils.time.TimePeriod;

/**
 * Top-level abstract class which defines the common methods required by macroscopic link output type adapters. Specifically designed for adoption of networks with macroscopic link
 * segments.
 * 
 * @author gman6028, markr
 *
 */
public abstract class MacroscopicLinkOutputTypeAdapterImpl extends UntypedLinkOutputTypeAdapterImpl<MacroscopicLinkSegment> implements MacroscopicLinkOutputTypeAdapter {

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
    TransportLayer networkLayer = getAssignment().getTransportNetwork().getInfrastructureNetwork().getLayerByMode(mode);
    return Optional.of(networkLayer != null ? networkLayer.getId() : null);
  }

  /**
   * Provide access to the macroscopic link segments
   * 
   * @param infrastructureLayerId to use
   */
  @Override
  public MacroscopicLinkSegments getPhysicalLinkSegments(long infrastructureLayerId) {
    TransportLayer networkLayer = getAssignment().getTransportNetwork().getInfrastructureNetwork().getTransportLayers().get(infrastructureLayerId);
    if (networkLayer instanceof MacroscopicNetworkLayer) {
      return ((MacroscopicNetworkLayer) networkLayer).getLinkSegments();
    }
    LOGGER.warning(String.format("Cannot collect macroscopic physical link segments from infrastructure layer %s, as it is not a macroscopic physical network layer",
        networkLayer.getXmlId()));
    return null;
  }

  /**
   * Return the value of a specified output property of a link segment
   * 
   * The DENSITY case should never be called for TraditionalStaticAssignment.
   * 
   * @param outputProperty the specified output property
   * @param linkSegment    the specified link segment
   * @param mode           the current mode
   * @param timePeriod     the current time period
   * @return the value of the specified output property (or an Exception message if an error occurs)
   */
  @Override
  public Optional<?> getLinkSegmentOutputPropertyValue(OutputProperty outputProperty, MacroscopicLinkSegment linkSegment, Mode mode, TimePeriod timePeriod) {
    try {
      Optional<?> value = super.getOutputTypeIndependentPropertyValue(outputProperty, mode, timePeriod);
      if (value.isPresent()) {
        return value;
      }

      value = super.getLinkSegmentOutputPropertyValue(outputProperty, linkSegment);
      if (value.isPresent()) {
        return value;
      }

      /* specific to macroscopic link segment */
      switch (outputProperty.getOutputPropertyType()) {
      case CAPACITY_PER_LANE:
        return getCapacityPerLanePcuHour(linkSegment);
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

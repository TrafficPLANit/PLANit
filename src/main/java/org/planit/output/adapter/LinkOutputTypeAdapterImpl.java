package org.planit.output.adapter;

import java.util.Optional;
import java.util.logging.Logger;

import org.planit.assignment.TrafficAssignment;
import org.planit.network.InfrastructureLayer;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.output.enums.OutputType;
import org.planit.output.property.OutputProperty;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.LinkSegments;

/**
 * Top-level abstract class which defines the common methods required by Link output type adapters
 * 
 * @author gman6028
 *
 */
public abstract class LinkOutputTypeAdapterImpl<LS extends LinkSegment> extends OutputTypeAdapterImpl implements LinkOutputTypeAdapter<LS> {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(LinkOutputTypeAdapterImpl.class.getCanonicalName());

  /**
   * Constructor
   * 
   * @param outputType        the OutputType this adapter corresponds to
   * @param trafficAssignment TrafficAssignment object which this adapter wraps
   */
  public LinkOutputTypeAdapterImpl(OutputType outputType, TrafficAssignment trafficAssignment) {
    super(outputType, trafficAssignment);
  }


  /**
   * Provide access to the macroscopic link segments
   * 
   * @param infrastructureLayerId to use
   */
  @SuppressWarnings("unchecked")
  @Override
  public LinkSegments<LS> getPhysicalLinkSegments(long infrastructureLayerId) {
    InfrastructureLayer networkLayer = this.trafficAssignment.getTransportNetwork().getInfrastructureNetwork().infrastructureLayers.get(infrastructureLayerId);
    if (networkLayer instanceof PhysicalNetwork) {
      return ((PhysicalNetwork<?,?,LS>) networkLayer).linkSegments;
    }
    LOGGER.warning(String.format("cannot collect physical link segments from infrastructure layer %s, as it is not a physical network layer", networkLayer.getXmlId()));
    return null;
  }
 

  /**
   * Return the value of a specified output property of a link segment
   * 
   * 
   * @param outputProperty     the specified output property
   * @param linkSegment        the specified link segment
   * @return the value of the specified output property (or an Exception message if an error occurs)
   */
  @Override
  public Optional<?> getLinkSegmentOutputPropertyValue(OutputProperty outputProperty, LS linkSegment) {
    try {

      switch (outputProperty) {
      case DOWNSTREAM_NODE_EXTERNAL_ID:
        return getDownstreamNodeExternalId(linkSegment);
      case DOWNSTREAM_NODE_XML_ID:
        return getDownstreamNodeXmlId(linkSegment);
      case DOWNSTREAM_NODE_ID:
        return getDownstreamNodeId(linkSegment);
      case DOWNSTREAM_NODE_LOCATION:
        return getDownstreamNodeLocation(linkSegment);
      case LENGTH:
        return getLength(linkSegment);
      case LINK_SEGMENT_EXTERNAL_ID:
        return getLinkSegmentExternalId(linkSegment);
      case LINK_SEGMENT_XML_ID:
        return getLinkSegmentXmlId(linkSegment);
      case LINK_SEGMENT_ID:
        return getLinkSegmentId(linkSegment);
      case NUMBER_OF_LANES:
        return getNumberOfLanes(linkSegment);
      case UPSTREAM_NODE_EXTERNAL_ID:
        return getUpstreamNodeExternalId(linkSegment);
      case UPSTREAM_NODE_XML_ID:
        return getUpstreamNodeXmlId(linkSegment);
      case UPSTREAM_NODE_ID:
        return getUpstreamNodeId(linkSegment);
      case UPSTREAM_NODE_LOCATION:
        return getUpstreamNodeLocation(linkSegment);
      default:
        return Optional.empty();
      }
    } catch (PlanItException e) {
      return Optional.of(e.getMessage());
    }
  }

}

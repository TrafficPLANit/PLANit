package org.planit.output.adapter;

import java.util.Optional;
import java.util.logging.Logger;

import org.planit.assignment.TrafficAssignment;
import org.planit.output.enums.OutputType;
import org.planit.output.property.OutputProperty;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.network.layer.physical.LinkSegment;

/**
 * Top-level abstract class which defines the common methods required by Link output type adapters
 * 
 * @author gman6028
 *
 */
public abstract class UntypedLinkOutputTypeAdapterImpl<LS extends LinkSegment> extends OutputTypeAdapterImpl implements UntypedLinkOutputTypeAdapter<LS> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(UntypedLinkOutputTypeAdapterImpl.class.getCanonicalName());

  /**
   * Constructor
   * 
   * @param outputType        the OutputType this adapter corresponds to
   * @param trafficAssignment TrafficAssignment object which this adapter wraps
   */
  public UntypedLinkOutputTypeAdapterImpl(OutputType outputType, TrafficAssignment trafficAssignment) {
    super(outputType, trafficAssignment);
  }

  /**
   * Return the value of a specified output property of a link segment
   * 
   * 
   * @param outputProperty the specified output property
   * @param linkSegment    the specified link segment
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

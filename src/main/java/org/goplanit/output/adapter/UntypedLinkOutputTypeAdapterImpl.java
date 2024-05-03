package org.goplanit.output.adapter;

import java.util.Optional;
import java.util.logging.Logger;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.output.enums.OutputType;
import org.goplanit.output.property.OutputProperty;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.network.layer.physical.LinkSegment;

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

  /** in case a link (segment) has no geometry of its own, we can choose to construct it by creating a line between the two node
   * locations if possible. This is switched on by default */
  protected static final boolean CONSTRUCT_LINK_SEGMENT_GEOMETRY_FROM_NODES_IF_UNAVAILABLE = true;

  /** in case a link (segment)'s geometry does not run in the direction of travel, force it to be via this flag in the output */
  protected static final boolean FORCE_TRAVEL_DIRECTION = true;

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
    Optional<?> result = Optional.empty();
    try {
      switch (outputProperty.getOutputPropertyType()) {
      case DOWNSTREAM_NODE_EXTERNAL_ID:
        result = getDownstreamNodeExternalId(linkSegment);
        break;
      case DOWNSTREAM_NODE_XML_ID:
        result = getDownstreamNodeXmlId(linkSegment);
        break;
      case DOWNSTREAM_NODE_ID:
        result = getDownstreamNodeId(linkSegment);
        break;
      case DOWNSTREAM_NODE_GEOMETRY:
        result = getDownstreamNodeGeometry(linkSegment);
        break;
      case LENGTH:
        result = getLength(linkSegment);
        break;
      case LINK_SEGMENT_EXTERNAL_ID:
        result = getLinkSegmentExternalId(linkSegment);
        break;
      case LINK_SEGMENT_XML_ID:
        result = getLinkSegmentXmlId(linkSegment);
        break;
      case LINK_SEGMENT_GEOMETRY:
        result = getLinkSegmentGeometry(
                linkSegment, CONSTRUCT_LINK_SEGMENT_GEOMETRY_FROM_NODES_IF_UNAVAILABLE, FORCE_TRAVEL_DIRECTION);
        break;
      case LINK_SEGMENT_ID:
        result = getLinkSegmentId(linkSegment);
        break;
      case NUMBER_OF_LANES:
        result = getNumberOfLanes(linkSegment);
        break;
      case UPSTREAM_NODE_EXTERNAL_ID:
        result = getUpstreamNodeExternalId(linkSegment);
        break;
      case UPSTREAM_NODE_XML_ID:
        result = getUpstreamNodeXmlId(linkSegment);
        break;
      case UPSTREAM_NODE_ID:
        result = getUpstreamNodeId(linkSegment);
        break;
      case UPSTREAM_NODE_GEOMETRY:
        result = getUpstreamNodeGeometry(linkSegment);
        break;
      default:
      }

      if (outputProperty.supportsUnitOverride() && outputProperty.isUnitOverride()) {
        result = createConvertedUnitsValue(outputProperty, result);
      }
    } catch (PlanItException e) {
      result = Optional.of(e.getMessage());
    }
    return result;
  }

}

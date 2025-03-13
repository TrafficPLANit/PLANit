package org.goplanit.output.adapter;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.output.enums.OutputType;
import org.goplanit.output.property.OutputProperty;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.directed.EdgeSegment;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * Top-level abstract class which defines the common methods required by edge output type adapters
 * 
 * @author gman6028, markr
 *
 */
public abstract class UntypedEdgeOutputTypeAdapterImpl<ES extends EdgeSegment>
    extends OutputTypeAdapterImpl implements UntypedEdgeOutputTypeAdapter<ES> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(UntypedEdgeOutputTypeAdapterImpl.class.getCanonicalName());

  /** in case an edge (segment) has no geometry of its own, we can choose to construct it by creating a line between
   * the two node locations if possible. This is switched on by default */
  protected static final boolean CONSTRUCT_SEGMENT_GEOMETRY_FROM_NODES_IF_UNAVAILABLE = true;

  /** in case a link (segment)'s geometry does not run in the direction of travel, force it to be via this flag in
   * the output */
  protected static final boolean FORCE_TRAVEL_DIRECTION = true;

  /**
   * Constructor
   *
   * @param outputType        the OutputType this adapter corresponds to
   * @param trafficAssignment TrafficAssignment object which this adapter wraps
   */
  public UntypedEdgeOutputTypeAdapterImpl(OutputType outputType, TrafficAssignment trafficAssignment) {
    super(outputType, trafficAssignment);
  }

  /**
   * Return the value of a specified output property of a edge segment
   * 
   * 
   * @param outputProperty the specified output property
   * @param edgeSegment    the specified edge segment
   * @return the value of the specified output property (or an Exception message if an error occurs)
   */
  @Override
  public Optional<?> getEdgeSegmentOutputPropertyValue(OutputProperty outputProperty, ES edgeSegment) {
    Optional<?> result = Optional.empty();
    try {
      switch (outputProperty.getOutputPropertyType()) {
      case DOWNSTREAM_NODE_EXTERNAL_ID:
        result = getDownstreamNodeExternalId(edgeSegment);
        break;
      case DOWNSTREAM_NODE_XML_ID:
        result = getDownstreamNodeXmlId(edgeSegment);
        break;
      case DOWNSTREAM_NODE_ID:
        result = getDownstreamNodeId(edgeSegment);
        break;
      case DOWNSTREAM_NODE_GEOMETRY:
        result = getDownstreamNodeGeometry(edgeSegment);
        break;
      case LENGTH:
        result = getLength(edgeSegment);
        break;
      case LINK_SEGMENT_ID:
        result = getSegmentId(edgeSegment);
        break;
      case LINK_SEGMENT_EXTERNAL_ID:
        result = getSegmentExternalId(edgeSegment);
        break;
      case LINK_SEGMENT_XML_ID:
        result = getSegmentXmlId(edgeSegment);
        break;
      case LINK_SEGMENT_GEOMETRY:
        result = getGeometry(
                edgeSegment, CONSTRUCT_SEGMENT_GEOMETRY_FROM_NODES_IF_UNAVAILABLE, FORCE_TRAVEL_DIRECTION);
        break;
      case UPSTREAM_NODE_EXTERNAL_ID:
        result = getUpstreamNodeExternalId(edgeSegment);
        break;
      case UPSTREAM_NODE_XML_ID:
        result = getUpstreamNodeXmlId(edgeSegment);
        break;
      case UPSTREAM_NODE_ID:
        result = getUpstreamNodeId(edgeSegment);
        break;
      case UPSTREAM_NODE_GEOMETRY:
        result = getUpstreamNodeGeometry(edgeSegment);
        break;
      case LINK_ID:
        result = getParentLinkId(edgeSegment);
        break;
      case LINK_EXTERNAL_ID:
        result = getParentLinkExternalId(edgeSegment);
        break;
      case LINK_XML_ID:
        result = getParentLinkXmlId(edgeSegment);
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

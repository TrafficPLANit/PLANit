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
      case DOWNSTREAM_NODE_LOCATION:
        result = getDownstreamNodeLocation(linkSegment);
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
      case UPSTREAM_NODE_LOCATION:
        result = getUpstreamNodeLocation(linkSegment);
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

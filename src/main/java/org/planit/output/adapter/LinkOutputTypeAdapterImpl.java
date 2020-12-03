package org.planit.output.adapter;

import java.util.logging.Logger;

import org.planit.assignment.TrafficAssignment;
import org.planit.output.enums.OutputType;
import org.planit.output.formatter.OutputFormatter;
import org.planit.output.property.OutputProperty;
import org.planit.time.TimePeriod;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.Vertex;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.LinkSegments;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegment;

import org.locationtech.jts.geom.Point;

/**
 * Top-level abstract class which defines the common methods required by Link output type adapters
 * 
 * @author gman6028
 *
 */
public abstract class LinkOutputTypeAdapterImpl extends OutputTypeAdapterImpl implements LinkOutputTypeAdapter {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(LinkOutputTypeAdapterImpl.class.getCanonicalName());

  /**
   * collect location as string representation from vertex
   * 
   * @param vertex
   * @return node location
   */
  private String getVertexLocationAsString(Vertex vertex) {
    Point position = vertex.getPosition();
    if (position == null) {
      return OutputFormatter.NOT_SPECIFIED;
    } else {
      return position.getCoordinate().x + "-" + position.getCoordinate().y;
    }
  }

  /**
   * Returns the value of the capacity per lane
   * 
   * @param linkSegment LinkSegment containing data which may be required
   * @return the capacity per lane across this link segment
   * @throws PlanItException thrown if there is an error
   */
  protected double getCapacityPerLane(LinkSegment linkSegment) throws PlanItException {
    PlanItException.throwIf(!(linkSegment instanceof MacroscopicLinkSegment), "Tried to calculate capacity per link across an object which is not a MacroscopicLinkSegment");

    MacroscopicLinkSegment macroscopicLinkSegment = (MacroscopicLinkSegment) linkSegment;
    return macroscopicLinkSegment.getLinkSegmentType().getCapacityPerLane();
  }

  /**
   * Return the link segment type of the current link segment
   * 
   * @param linkSegment the current link segment
   * @return the link segment type
   * @throws PlanItException thrown if there is an error
   */
  protected String getLinkType(LinkSegment linkSegment) throws PlanItException {
    PlanItException.throwIf(!(linkSegment instanceof MacroscopicLinkSegment), "Tried to find the Link Type of an object which is not a MacroscopicLinkSegment");

    MacroscopicLinkSegment macroscopicLinkSegment = (MacroscopicLinkSegment) linkSegment;
    return macroscopicLinkSegment.getLinkSegmentType().getName();
  }

  /**
   * Returns the maximum density of the current link
   * 
   * @param linkSegment LinkSegment containing data which may be required
   * @return the flow density of the current link
   * @throws PlanItException thrown if there is an error
   */
  /**
   * Returns the flow density of the current link
   * 
   * @param linkSegment LinkSegment containing data which may be required
   * @return the flow density of the current link
   * @throws PlanItException thrown if there is an error
   */
  protected double getMaximumDensity(LinkSegment linkSegment) throws PlanItException {
    PlanItException.throwIf(!(linkSegment instanceof MacroscopicLinkSegment), "Tried to density per lane across an object which is not a MacroscopicLinkSegment");

    MacroscopicLinkSegment macroscopicLinkSegment = (MacroscopicLinkSegment) linkSegment;
    return macroscopicLinkSegment.getLinkSegmentType().getMaximumDensityPerLane();
  }

  /**
   * Returns the external Id of the downstream node
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return he external Id of the downstream node
   * @throws PlanItException thrown if there is an error
   */
  protected String getDownstreamNodeExternalId(LinkSegment linkSegment) throws PlanItException {
    return ((Node) linkSegment.getDownstreamVertex()).getExternalId();
  }
  
  /**
   * Returns the xml Id of the downstream node
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return the xml Id of the downstream node
   * @throws PlanItException thrown if there is an error
   */
  protected String getDownstreamNodeXmlId(LinkSegment linkSegment) throws PlanItException {
    return ((Node) linkSegment.getDownstreamVertex()).getXmlId();
  }  

  /**
   * Returns the Id of the downstream node
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return the Id of the downstream node
   * @throws PlanItException thrown if there is an error
   */
  protected long getDownstreamNodeId(LinkSegment linkSegment) throws PlanItException {
    return ((Node) linkSegment.getDownstreamVertex()).getId();
  }

  /**
   * Returns the location of the downstream node
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return the location of the downstream node
   * @throws PlanItException thrown if the location could not be retrieved
   */
  protected Object getDownstreamNodeLocation(LinkSegment linkSegment) throws PlanItException {
    Vertex downstreamVertex = linkSegment.getDownstreamVertex();
    return getVertexLocationAsString(downstreamVertex);
  }

  /**
   * Returns the length of the current link segment
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return the length of the current link segment
   * @throws PlanItException thrown if there is an error
   */
  protected double getLength(LinkSegment linkSegment) throws PlanItException {
    return linkSegment.getParentLink().getLengthKm();
  }

  /**
   * Returns the external Id of the current link segment
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return the external Id of the current link segment
   * @throws PlanItException thrown if there is an error
   */
  protected String getLinkSegmentExternalId(LinkSegment linkSegment) throws PlanItException {
    return linkSegment.getExternalId();
  }
  
  /**
   * Returns the Xml Id of the current link segment
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return the Xml Id of the current link segment
   * @throws PlanItException thrown if there is an error
   */
  protected String getLinkSegmentXmlId(LinkSegment linkSegment) throws PlanItException {
    return linkSegment.getXmlId();
  }  

  /**
   * Returns the Id of the current link segment
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return the Id of the current link segment
   * @throws PlanItException thrown if there is an error
   */
  protected long getLinkSegmentId(LinkSegment linkSegment) throws PlanItException {
    return linkSegment.getId();
  }

  /**
   * Returns the maximum speed through the current link segment
   * 
   * @param linkSegment MacroscopicLinkSegment object containing the required data
   * @param mode        current mode
   * @return the maximum speed through the current link segment
   * @throws PlanItException thrown if there is an error
   */
  protected double getMaximumSpeed(LinkSegment linkSegment, Mode mode) throws PlanItException {
    PlanItException.throwIf(!(linkSegment instanceof MacroscopicLinkSegment), "Tried to read maximum speed of an object which is not a MacroscopicLinkSegment");
    return ((MacroscopicLinkSegment) linkSegment).getModelledSpeedLimitKmH(mode);
  }

  /**
   * Returns the number of lanes of the current link
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return the number of lanes of the current link
   * @throws PlanItException thrown if there is an error
   */
  protected int getNumberOfLanes(LinkSegment linkSegment) throws PlanItException {
    return linkSegment.getNumberOfLanes();
  }

  /**
   * Returns the external Id of the upstream node
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return the external Id of the upstream node
   * @throws PlanItException thrown if there is an error
   */
  protected String getUpstreamNodeExternalId(LinkSegment linkSegment) throws PlanItException {
    return ((Node) linkSegment.getUpstreamVertex()).getExternalId();
  }
  
  /**
   * Returns the Xml Id of the upstream node
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return the xml Id of the upstream node
   * @throws PlanItException thrown if there is an error
   */
  protected String getUpstreamNodeXmlId(LinkSegment linkSegment) throws PlanItException {
    return ((Node) linkSegment.getUpstreamVertex()).getXmlId();
  }  

  /**
   * Returns the location of the upstream node
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return the location of the upstream node
   * @throws PlanItException thrown if there is an error
   */
  protected Object getUpstreamNodeLocation(LinkSegment linkSegment) throws PlanItException {
    Vertex upstreamVertex = linkSegment.getUpstreamVertex();
    return getVertexLocationAsString(upstreamVertex);
  }

  /**
   * Returns the Id of the upstream node
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return the Id of the upstream node
   * @throws PlanItException thrown if there is an error
   */
  protected long getUpstreamNodeId(LinkSegment linkSegment) throws PlanItException {
    return ((Node) linkSegment.getUpstreamVertex()).getId();
  }

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
   * Provide access to the available link segments
   */
  @Override
  public LinkSegments<? extends LinkSegment> getPhysicalLinkSegments() {
    return trafficAssignment.getTransportNetwork().getPhysicalNetwork().linkSegments;
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
   * @return the value of the specified output property (or an Exception if an error occurs)
   */
  @Override
  public Object getLinkOutputPropertyValue(OutputProperty outputProperty, LinkSegment linkSegment, Mode mode, TimePeriod timePeriod, double timeUnitMultiplier) {
    try {
      Object obj = getOutputTypeIndependentPropertyValue(outputProperty, mode, timePeriod);
      if (obj != null) {
        return obj;
      }
      switch (outputProperty) {
      case CAPACITY_PER_LANE:
        return getCapacityPerLane(linkSegment);
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
      case MAXIMUM_DENSITY:
        return getMaximumDensity(linkSegment);
      case MAXIMUM_SPEED:
        return getMaximumSpeed(linkSegment, mode);
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
      case LINK_TYPE:
        return getLinkType(linkSegment);
      default:
        return null;
      }
    } catch (PlanItException e) {
      return e;
    }
  }

}

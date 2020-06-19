package org.planit.output.adapter;

import java.util.logging.Logger;

import org.opengis.geometry.DirectPosition;
import org.planit.exceptions.PlanItException;
import org.planit.graph.VertexImpl;
import org.planit.network.physical.PhysicalNetwork.LinkSegments;
import org.planit.output.enums.OutputType;
import org.planit.output.formatter.OutputFormatter;
import org.planit.output.property.OutputProperty;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Mode;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegment;

/**
 * Top-level abstract class which defines the common methods required by Link output type adapters
 * 
 * @author gman6028
 *
 */
public abstract class LinkOutputTypeAdapterImpl extends OutputTypeAdapterImpl implements LinkOutputTypeAdapter {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(LinkOutputTypeAdapterImpl.class.getCanonicalName());

  /**
   * Returns the value of the capacity per lane
   * 
   * @param linkSegment LinkSegment containing data which may be required
   * @return the capacity per lane across this link segment
   * @throws PlanItException thrown if there is an error
   */
  protected double getCapacityPerLane(LinkSegment linkSegment) throws PlanItException {
    if (!(linkSegment instanceof MacroscopicLinkSegment)) {
      throw new PlanItException(
          "Tried to calculate capacity per link across an object which is not a MacroscopicLinkSegment.");
    }
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
    if (!(linkSegment instanceof MacroscopicLinkSegment)) {
      throw new PlanItException("Tried to find the Link Type of an object which is not a MacroscopicLinkSegment.");
    }
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
    if (!(linkSegment instanceof MacroscopicLinkSegment)) {
      throw new PlanItException("Tried to density per lane across an object which is not a MacroscopicLinkSegment.");
    }
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
  protected Object getDownstreamNodeExternalId(LinkSegment linkSegment) throws PlanItException {
    return ((Node) linkSegment.getDownstreamVertex()).getExternalId();
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
    if (!(linkSegment.getDownstreamVertex() instanceof VertexImpl)) {
      String errorMessage = "Downstream node location not available";
      throw new PlanItException(errorMessage);
    }
    VertexImpl downstreamVertex = (VertexImpl) linkSegment.getDownstreamVertex();
    DirectPosition centrePoint = downstreamVertex.getCentrePointGeometry();
    if (centrePoint == null) {
      return OutputFormatter.NOT_SPECIFIED;
    } else {
      double[] coordinates = centrePoint.getCoordinate();
      return coordinates[0] + "-" + coordinates[1];
    }
  }

  /**
   * Returns the length of the current link segment
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return the length of the current link segment
   * @throws PlanItException thrown if there is an error
   */
  protected double getLength(LinkSegment linkSegment) throws PlanItException {
    return linkSegment.getParentLink().getLength();
  }

  /**
   * Returns the external Id of the current link segment
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return the external Id of the current link segment
   * @throws PlanItException thrown if there is an error
   */
  protected Object getLinkSegmentExternalId(LinkSegment linkSegment) throws PlanItException {
    return linkSegment.getExternalId();
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
   * @param mode current mode
   * @return the maximum speed through the current link segment
   * @throws PlanItException thrown if there is an error
   */
  protected double getMaximumSpeed(LinkSegment linkSegment, Mode mode) throws PlanItException {
    if (!(linkSegment instanceof MacroscopicLinkSegment)) {
      throw new PlanItException("Tried to read maximum speed of an object which is not a MacroscopicLinkSegment.");
    }
    MacroscopicLinkSegment macroscopicLinkSegment = (MacroscopicLinkSegment) linkSegment;
    if (!macroscopicLinkSegment.isModeAllowedThroughLink(mode)) {
      return 0.0;
    }
    return macroscopicLinkSegment.getMaximumSpeed(mode);
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
  protected Object getUpstreamNodeExternalId(LinkSegment linkSegment) throws PlanItException {
    return ((Node) linkSegment.getUpstreamVertex()).getExternalId();
  }

  /**
   * Returns the location of the upstream node
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return the location of the upstream node
   * @throws PlanItException thrown if there is an error
   */
  protected Object getUpstreamNodeLocation(LinkSegment linkSegment) throws PlanItException {
    if (!(linkSegment.getDownstreamVertex() instanceof VertexImpl)) {
      String errorMessage = "Upstream node location not available";
      throw new PlanItException(errorMessage);
    }
    VertexImpl upstreamVertex = (VertexImpl) linkSegment.getUpstreamVertex();
    DirectPosition centrePoint = upstreamVertex.getCentrePointGeometry();
    if (centrePoint == null) {
      return OutputFormatter.NOT_SPECIFIED;
    } else {
      double[] coordinates = centrePoint.getCoordinate();
      return coordinates[0] + "-" + coordinates[1];
    }
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
   * @param outputType the OutputType this adapter corresponds to
   * @param trafficAssignment TrafficAssignment object which this adapter wraps
   */
  public LinkOutputTypeAdapterImpl(OutputType outputType, TrafficAssignment trafficAssignment) {
    super(outputType, trafficAssignment);
  }

  /**
   * Provide access to the available link segments
   */
  @Override
  public LinkSegments getPhysicalLinkSegments() {
    return trafficAssignment.getTransportNetwork().getPhysicalNetwork().linkSegments;
  }

  /**
   * Return the value of a specified output property of a link segment
   * 
   * The DENSITY case should never be called for TraditionalStaticAssignment.
   * 
   * @param outputProperty the specified output property
   * @param linkSegment the specified link segment
   * @param mode the current mode
   * @param timePeriod the current time period
   * @param timeUnitMultiplier the multiplier for time units
   * @return the value of the specified output property (or an Exception if an error occurs)
   */
  @Override
  public Object getLinkOutputPropertyValue(OutputProperty outputProperty, LinkSegment linkSegment, Mode mode,
      TimePeriod timePeriod, double timeUnitMultiplier) {
    try {
      Object obj = getCommonPropertyValue(outputProperty, mode, timePeriod);
      if (obj != null) {
        return obj;
      }
      switch (outputProperty) {
        case CAPACITY_PER_LANE:
          return getCapacityPerLane(linkSegment);
        case DOWNSTREAM_NODE_EXTERNAL_ID:
          return getDownstreamNodeExternalId(linkSegment);
        case DOWNSTREAM_NODE_ID:
          return getDownstreamNodeId(linkSegment);
        case DOWNSTREAM_NODE_LOCATION:
          return getDownstreamNodeLocation(linkSegment);
        case LENGTH:
          return getLength(linkSegment);
        case LINK_SEGMENT_EXTERNAL_ID:
          return getLinkSegmentExternalId(linkSegment);
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

package org.planit.output.adapter;

import java.util.Optional;

import org.locationtech.jts.geom.Point;
import org.planit.output.formatter.OutputFormatter;
import org.planit.output.property.OutputProperty;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.GraphEntities;
import org.planit.utils.graph.Vertex;
import org.planit.utils.network.layer.physical.LinkSegment;

/**
 * Interface defining the methods required for a link output adapter
 * 
 * @author gman6028, markr
 *
 */
public interface UntypedLinkOutputTypeAdapter<T extends LinkSegment> extends OutputTypeAdapter {

  /**
   * collect location as string representation from vertex
   * 
   * @param vertex to extract location for
   * @return node location
   */
  public static Optional<String> getVertexLocationAsString(Vertex vertex) {
    Point position = vertex.getPosition();
    if (position == null) {
      return Optional.of(OutputFormatter.NOT_SPECIFIED);
    } else {
      return Optional.of(position.getCoordinate().x + "-" + position.getCoordinate().y);
    }
  }

  /**
   * Returns the external Id of the downstream node
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return the external Id of the downstream node
   * @throws PlanItException thrown if there is an error
   */
  public default Optional<String> getDownstreamNodeExternalId(T linkSegment) throws PlanItException {
    return Optional.of(linkSegment.getDownstreamVertex().getExternalId());
  }

  /**
   * Returns the XML Id of the downstream node
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return the XML Id of the downstream node
   * @throws PlanItException thrown if there is an error
   */
  public default Optional<String> getDownstreamNodeXmlId(T linkSegment) throws PlanItException {
    return Optional.of(linkSegment.getDownstreamVertex().getXmlId());
  }

  /**
   * Returns the Id of the downstream node
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return the Id of the downstream node
   * @throws PlanItException thrown if there is an error
   */
  public default Optional<Long> getDownstreamNodeId(T linkSegment) throws PlanItException {
    return Optional.of(linkSegment.getDownstreamVertex().getId());
  }

  /**
   * Returns the location of the downstream node
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return the location of the downstream node
   * @throws PlanItException thrown if the location could not be retrieved
   */
  public default Optional<String> getDownstreamNodeLocation(T linkSegment) throws PlanItException {
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
  public default Optional<Double> getLength(T linkSegment) throws PlanItException {
    return Optional.of(linkSegment.getParentLink().getLengthKm());
  }

  /**
   * Returns the external Id of the current link segment
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return the external Id of the current link segment
   * @throws PlanItException thrown if there is an error
   */
  public default Optional<String> getLinkSegmentExternalId(T linkSegment) throws PlanItException {
    return Optional.of(linkSegment.getExternalId());
  }

  /**
   * Returns the XML Id of the current link segment
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return the XML Id of the current link segment
   * @throws PlanItException thrown if there is an error
   */
  public default Optional<String> getLinkSegmentXmlId(T linkSegment) throws PlanItException {
    return Optional.of(linkSegment.getXmlId());
  }

  /**
   * Returns the Id of the current link segment
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return the Id of the current link segment
   * @throws PlanItException thrown if there is an error
   */
  public default Optional<Long> getLinkSegmentId(T linkSegment) throws PlanItException {
    return Optional.of(linkSegment.getId());
  }

  /**
   * Returns the number of lanes of the current link
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return the number of lanes of the current link
   * @throws PlanItException thrown if there is an error
   */
  public default Optional<Integer> getNumberOfLanes(T linkSegment) throws PlanItException {
    return Optional.of(linkSegment.getNumberOfLanes());
  }

  /**
   * Returns the external Id of the upstream node
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return the external Id of the upstream node
   * @throws PlanItException thrown if there is an error
   */
  public default Optional<String> getUpstreamNodeExternalId(T linkSegment) throws PlanItException {
    return Optional.of(linkSegment.getUpstreamVertex().getExternalId());
  }

  /**
   * Returns the XML Id of the upstream node
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return the XML Id of the upstream node
   * @throws PlanItException thrown if there is an error
   */
  public default Optional<String> getUpstreamNodeXmlId(T linkSegment) throws PlanItException {
    return Optional.of(linkSegment.getUpstreamVertex().getXmlId());
  }

  /**
   * Returns the location of the upstream node
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return the location of the upstream node
   * @throws PlanItException thrown if there is an error
   */
  public default Optional<String> getUpstreamNodeLocation(T linkSegment) throws PlanItException {
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
  public default Optional<Long> getUpstreamNodeId(T linkSegment) throws PlanItException {
    return Optional.of(linkSegment.getUpstreamVertex().getId());
  }

  /**
   * Return the Link segments for this assignment
   * 
   * @param infrastructureLayerId to collect link segments for
   * @return a List of link segments for this assignment
   */
  public abstract GraphEntities<T> getPhysicalLinkSegments(long infrastructureLayerId);

  /**
   * Return the value of a specified output property of a link segment
   * 
   * @param outputProperty the specified output property
   * @param linkSegment    the specified link segment
   * @return the value of the specified output property (or an Exception if an error occurs)
   */
  public abstract Optional<?> getLinkSegmentOutputPropertyValue(OutputProperty outputProperty, T linkSegment);
}

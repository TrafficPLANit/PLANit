package org.goplanit.output.adapter;

import java.util.Optional;

import org.goplanit.output.property.OutputProperty;
import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.EdgeUtils;
import org.goplanit.utils.graph.GraphEntities;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.locationtech.jts.geom.Geometry;

/**
 * Interface defining the methods required for a link output adapter
 * 
 * @author gman6028, markr
 *
 */
public interface UntypedLinkOutputTypeAdapter<T extends LinkSegment> extends OutputTypeAdapter {

  /**
   * collect geometry from vertex
   * 
   * @param vertex to extract geometry from
   * @return the geometry
   */
  public static Optional<?> getVertexGeometry(Vertex vertex) {
    if(vertex == null){
      return Optional.of(PROPERTY_NOT_AVAILABLE);
    }
    var position = vertex.getPosition();
    return position != null ? Optional.of(position) : Optional.of(PROPERTY_NOT_AVAILABLE);
  }

  /**
   * collect geometry from edge
   *
   * @param edge                                    to extract geometry from
   * @param constructGeometryFromNodesIfUnavailable   when true and no internal geometry is available, attempt to construct from node locations
   * @param constructInAbDirection when true and constructGeometryFromNodesIfUnavailable==true then we construct the geometry in AB direction otherwise in BA direction
   * @return the geometry
   */
  public static Optional<?> getEdgeGeometry(Edge edge, boolean constructGeometryFromNodesIfUnavailable, boolean constructInAbDirection) {
    if(edge == null){
      return Optional.of(PROPERTY_NOT_AVAILABLE);
    }
    var geometry = edge.getGeometry();
    if(geometry == null && constructGeometryFromNodesIfUnavailable){
      geometry = EdgeUtils.createLineStringFromVertexLocations(edge, constructInAbDirection);
    }
    return geometry != null ? Optional.of( geometry) : Optional.of(PROPERTY_NOT_AVAILABLE);
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
   */
  public default Optional<?> getDownstreamNodeGeometry(T linkSegment) {
    if(linkSegment == null){
      return Optional.of(PROPERTY_NOT_AVAILABLE);
    }
    Vertex downstreamVertex = linkSegment.getDownstreamVertex();
    return getVertexGeometry(linkSegment.getDownstreamVertex());
  }

  /**
   * Returns the length of the current link segment
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return the length of the current link segment
   */
  public default Optional<Double> getLength(T linkSegment) {
    return Optional.of(linkSegment.getParentLink().getLengthKm());
  }

  /**
   * Returns the external Id of the current link segment
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return the external Id of the current link segment
   */
  public default Optional<String> getLinkSegmentExternalId(T linkSegment) {
    return Optional.of(linkSegment.getExternalId());
  }

  /**
   * Returns the XML Id of the current link segment
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return the XML Id of the current link segment
   */
  public default Optional<String> getLinkSegmentXmlId(T linkSegment) {
    return Optional.of(linkSegment.getXmlId());
  }

  /**
   * Returns the Id of the current link segment
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return the Id of the current link segment
   */
  public default Optional<Long> getLinkSegmentId(T linkSegment){
    return Optional.of(linkSegment.getId());
  }

  /**
   * Returns the location of the link segment
   *
   * @param linkSegment                               linkSegment object containing the required data
   * @param constructGeometryFromNodesIfUnavailable   when true and no internal geometry is available, attempt to construct from node locations
   * @param forceSegmentDirection when true, we force the geometry to be provided in the travel direction of the segment, when false keep
   * @return the geometry
   */
  public default Optional<?> getLinkSegmentGeometry(T linkSegment, boolean constructGeometryFromNodesIfUnavailable, boolean forceSegmentDirection) {
    if(linkSegment == null){
      Optional.of(PROPERTY_NOT_AVAILABLE);
    }
    var collectedEdgeGeometry = getEdgeGeometry(linkSegment.getParentLink(), constructGeometryFromNodesIfUnavailable, linkSegment.isDirectionAb());

    /* force geometry to be in travel direction of segment if configrued as such */
    boolean mayNeedReversal = linkSegment.getParentLink().hasGeometry(); // only when geometry is not constructed from nodes we may need to reverse
    boolean collectedEdgeGeometryValid = collectedEdgeGeometry.isPresent() && (collectedEdgeGeometry.get() instanceof Geometry);
    if(mayNeedReversal && forceSegmentDirection && collectedEdgeGeometryValid){
      boolean reverseGeometry = linkSegment.getParentLink().isGeometryInAbDirection() != linkSegment.isDirectionAb();
      if(reverseGeometry){
        collectedEdgeGeometry = Optional.of(((Geometry)collectedEdgeGeometry.get()).reverse());
      }
    }
    return collectedEdgeGeometry;
  }

  /**
   * Returns the number of lanes of the current link
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return the number of lanes of the current link
   */
  public default Optional<Integer> getNumberOfLanes(T linkSegment){
    return Optional.of(linkSegment.getNumberOfLanes());
  }

  /**
   * Returns the external Id of the upstream node
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return the external Id of the upstream node
   */
  public default Optional<String> getUpstreamNodeExternalId(T linkSegment){
    return Optional.of(linkSegment.getUpstreamVertex().getExternalId());
  }

  /**
   * Returns the XML Id of the upstream node
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return the XML Id of the upstream node
   */
  public default Optional<String> getUpstreamNodeXmlId(T linkSegment){
    return Optional.of(linkSegment.getUpstreamVertex().getXmlId());
  }

  /**
   * Returns the location of the upstream node
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return the location of the upstream node
   */
  public default Optional<?> getUpstreamNodeGeometry(T linkSegment) {
    if(linkSegment == null){
      Optional.of(PROPERTY_NOT_AVAILABLE);
    }
    Vertex upstreamVertex = linkSegment.getUpstreamVertex();
    return getVertexGeometry(upstreamVertex);
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

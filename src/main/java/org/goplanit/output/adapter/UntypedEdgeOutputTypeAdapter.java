package org.goplanit.output.adapter;

import org.goplanit.output.property.OutputProperty;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.graph.EdgeUtils;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.locationtech.jts.geom.Geometry;

import java.util.Optional;

/**
 * Interface defining the methods required for an edge  output adapter agnostic to how these edge (segments) are
 * containerised or stored in a larger setting, e.g., network or bush.
 *
 * todo: split in edge segment and link segment version now that we need the distinction
 * 
 * @author gman6028, markr
 * @param <T> type of edge segment
 */
public interface UntypedEdgeOutputTypeAdapter<T extends EdgeSegment> extends OutputTypeAdapter {

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
   * @param constructGeometryFromNodesIfUnavailable   when true and no internal geometry is available, attempt to
   *                                                  construct from node locations
   * @param constructInAbDirection when true and constructGeometryFromNodesIfUnavailable==true then we construct
   *                               the geometry in AB direction otherwise in BA direction
   * @return the geometry
   */
  public static Optional<?> getEdgeGeometry(
      Edge edge, boolean constructGeometryFromNodesIfUnavailable, boolean constructInAbDirection) {
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
   * @param edgeSegment edgeSegment object containing the required data
   * @return the external Id of the downstream node
   * @throws PlanItException thrown if there is an error
   */
  public default Optional<String> getDownstreamNodeExternalId(T edgeSegment) throws PlanItException {
    return Optional.of(edgeSegment.getDownstreamVertex().hasExternalId() ?
        edgeSegment.getDownstreamVertex().getExternalId() : "");
  }

  /**
   * Returns the XML Id of the downstream node
   * 
   * @param edgeSegment edgeSegment object containing the required data
   * @return the XML Id of the downstream node
   * @throws PlanItException thrown if there is an error
   */
  public default Optional<String> getDownstreamNodeXmlId(T edgeSegment) throws PlanItException {
    return Optional.of(edgeSegment.getDownstreamVertex().getXmlId());
  }

  /**
   * Returns the Id of the downstream node
   * 
   * @param edgeSegment edgeSegment object containing the required data
   * @return the Id of the downstream node
   * @throws PlanItException thrown if there is an error
   */
  public default Optional<Long> getDownstreamNodeId(T edgeSegment) throws PlanItException {
    return Optional.of(edgeSegment.getDownstreamVertex().getId());
  }

  /**
   * Returns the location of the downstream node
   * 
   * @param edgeSegment edgeSegment object containing the required data
   * @return the location of the downstream node
   */
  public default Optional<?> getDownstreamNodeGeometry(T edgeSegment) {
    if(edgeSegment == null){
      return Optional.of(PROPERTY_NOT_AVAILABLE);
    }
    Vertex downstreamVertex = edgeSegment.getDownstreamVertex();
    return getVertexGeometry(downstreamVertex);
  }

  /**
   * Returns the length of the current edge segment
   * 
   * @param edgeSegment edgeSegment object containing the required data
   * @return the length of the current link segment
   */
  public default Optional<Double> getLength(T edgeSegment) {
    return Optional.of(edgeSegment.getParent().getLengthKm());
  }

  /**
   * Returns the external Id of the current edge segment
   * 
   * @param edgeSegment edgeSegment object containing the required data
   * @return the external Id of the current edge segment
   */
  public default Optional<String> getSegmentExternalId(T edgeSegment) {
    return Optional.of(edgeSegment.hasExternalId() ? edgeSegment.getExternalId() : "");
  }

  /**
   * Returns the XML Id of the current edge segment
   * 
   * @param edgeSegment edgeSegment object containing the required data
   * @return the XML Id of the current edge segment
   */
  public default Optional<String> getSegmentXmlId(T edgeSegment) {
    return Optional.of(edgeSegment.hasXmlId() ? edgeSegment.getXmlId() : "");
  }

  /**
   * Returns the Id of the current edge segment's parent
   * 
   * @param edgeSegment edgeSegment object containing the required data
   * @return the Id of the current edge segment's parent
   */
  public default Optional<Long> getParentLinkId(T edgeSegment){
    if(edgeSegment.getParent() == null){
      return Optional.empty();
    }
    return Optional.of(edgeSegment.getParent().getId());
  }

  /**
   * Returns the external Id of the current edge segment's parent
   *
   * @param edgeSegment edgeSegment object containing the required data
   * @return the external Id of the current edge segment's parent
   */
  public default Optional<String> getParentLinkExternalId(T edgeSegment) {
    if(edgeSegment.getParent() == null){
      return Optional.empty();
    }
    return Optional.of(edgeSegment.getParent().hasExternalId() ?
        edgeSegment.getParent().getExternalId() : "");
  }

  /**
   * Returns the XML Id of the current edge segment's parent
   *
   * @param edgeSegment edgeSegment object containing the required data
   * @return the XML Id of the current edge segment's parent
   */
  public default Optional<String> getParentLinkXmlId(T edgeSegment) {
    if(edgeSegment.getParent() == null){
      return Optional.empty();
    }
    return Optional.of(edgeSegment.getParent().getXmlId());
  }

  /**
   * Returns the Id of the current edge segment
   *
   * @param edgeSegment edgeSegment object containing the required data
   * @return the Id of the current edge segment
   */
  public default Optional<Long> getSegmentId(T edgeSegment){
    return Optional.of(edgeSegment.getId());
  }

  /**
   * Returns the location of the edge segment
   *
   * @param edgeSegment                               edgeSegment object containing the required data
   * @param constructGeometryFromNodesIfUnavailable   when true and no internal geometry is available, attempt to
   *                                                  construct from node locations
   * @param forceSegmentDirection when true, we force the geometry to be provided in the travel direction of the
   *                              segment, when false keep
   * @return the geometry
   */
  public default Optional<?> getGeometry(
      T edgeSegment, boolean constructGeometryFromNodesIfUnavailable, boolean forceSegmentDirection) {
    if(edgeSegment == null){
      Optional.of(PROPERTY_NOT_AVAILABLE);
    }
    var collectedEdgeGeometry =
        getEdgeGeometry(edgeSegment.getParent(), constructGeometryFromNodesIfUnavailable, edgeSegment.isDirectionAb());

    /* force geometry to be in travel direction of segment if configured as such */
    // only when geometry is not constructed from nodes we may need to reverse
    boolean mayNeedReversal = edgeSegment.getParent().hasGeometry();
    boolean collectedEdgeGeometryValid = collectedEdgeGeometry.isPresent() &&
        (collectedEdgeGeometry.get() instanceof Geometry);
    if(mayNeedReversal && forceSegmentDirection && collectedEdgeGeometryValid){
      boolean reverseGeometry = edgeSegment.getParent().isGeometryInAbDirection() != edgeSegment.isDirectionAb();
      if(reverseGeometry){
        collectedEdgeGeometry = Optional.of(((Geometry)collectedEdgeGeometry.get()).reverse());
      }
    }
    return collectedEdgeGeometry;
  }

  /**
   * Returns the external Id of the upstream node
   * 
   * @param edgeSegment edgeSegment object containing the required data
   * @return the external Id of the upstream node
   */
  public default Optional<String> getUpstreamNodeExternalId(T edgeSegment){
    return Optional.of(edgeSegment.getUpstreamVertex().hasExternalId() ?
        edgeSegment.getUpstreamVertex().getExternalId() : "");
  }

  /**
   * Returns the XML Id of the upstream node
   * 
   * @param edgeSegment edgeSegment object containing the required data
   * @return the XML Id of the upstream node
   */
  public default Optional<String> getUpstreamNodeXmlId(T edgeSegment){
    return Optional.of(edgeSegment.getUpstreamVertex().getXmlId());
  }

  /**
   * Returns the location of the upstream node
   * 
   * @param edgeSegment edgeSegment object containing the required data
   * @return the location of the upstream node
   */
  public default Optional<?> getUpstreamNodeGeometry(T edgeSegment) {
    if(edgeSegment == null){
      Optional.of(PROPERTY_NOT_AVAILABLE);
    }
    Vertex upstreamVertex = edgeSegment.getUpstreamVertex();
    return getVertexGeometry(upstreamVertex);
  }

  /**
   * Returns the Id of the upstream node
   * 
   * @param edgeSegment edgeSegment object containing the required data
   * @return the Id of the upstream node
   * @throws PlanItException thrown if there is an error
   */
  public default Optional<Long> getUpstreamNodeId(T edgeSegment) throws PlanItException {
    return Optional.of(edgeSegment.getUpstreamVertex().getId());
  }

  /**
   * Return the value of a specified output property of a edge segment
   * 
   * @param outputProperty the specified output property
   * @param edgeSegment    the specified link segment
   * @return the value of the specified output property (or an Exception if an error occurs)
   */
  public abstract Optional<?> getEdgeSegmentOutputPropertyValue(OutputProperty outputProperty, T edgeSegment);
}

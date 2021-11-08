package org.goplanit.graph.directed;

import org.goplanit.graph.GraphEntityFactoryImpl;
import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegmentFactory;
import org.goplanit.utils.graph.directed.EdgeSegments;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * Factory for creating edge segments on edge segments container
 * 
 * @author markr
 */
public class EdgeSegmentFactoryImpl extends GraphEntityFactoryImpl<EdgeSegment> implements EdgeSegmentFactory {

  /**
   * Constructor
   * 
   * @param groupId      to use
   * @param edgeSegments to use
   */
  protected EdgeSegmentFactoryImpl(final IdGroupingToken groupId, EdgeSegments edgeSegments) {
    super(groupId, edgeSegments);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegment create(final DirectedEdge parentEdge, final boolean directionAB) {
    final EdgeSegment edgeSegment = new EdgeSegmentImpl(getIdGroupingToken(), directionAB);
    edgeSegment.setParent(parentEdge);
    return edgeSegment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegment registerNew(DirectedEdge parentEdge, boolean directionAb, boolean registerOnVertexAndEdge) {
    final EdgeSegment edgeSegment = new EdgeSegmentImpl(getIdGroupingToken(), parentEdge, directionAb);
    getGraphEntities().register(edgeSegment);

    if (registerOnVertexAndEdge) {
      parentEdge.registerEdgeSegment(edgeSegment, directionAb);
      if (parentEdge.getVertexA() instanceof DirectedVertex) {
        ((DirectedVertex) parentEdge.getVertexA()).addEdgeSegment(edgeSegment);
        ((DirectedVertex) parentEdge.getVertexB()).addEdgeSegment(edgeSegment);
      }
    }
    return edgeSegment;
  }

}

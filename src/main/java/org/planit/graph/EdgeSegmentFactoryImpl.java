package org.planit.graph;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.DirectedEdge;
import org.planit.utils.graph.DirectedVertex;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.EdgeSegmentFactory;
import org.planit.utils.graph.EdgeSegments;
import org.planit.utils.id.IdGroupingToken;

/**
 * Factory for creating edge segments on edge segments container
 * 
 * @author markr
 */
public class EdgeSegmentFactoryImpl extends GraphEntityFactoryImpl<EdgeSegment> implements EdgeSegmentFactory {

  /**
   * Constructor
   * 
   * @param groupId       to use
   * @param directedEdges to use
   */
  protected EdgeSegmentFactoryImpl(final IdGroupingToken groupId, EdgeSegments edgeSegments) {
    super(groupId, edgeSegments);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegment create(final DirectedEdge parentEdge, final boolean directionAB) throws PlanItException {
    final EdgeSegment edgeSegment = new EdgeSegmentImpl(getIdGroupingToken(), directionAB);
    edgeSegment.setParent(parentEdge);
    return edgeSegment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegment registerNew(DirectedEdge parentEdge, boolean directionAb, boolean registerOnVertexAndEdge) throws PlanItException {
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

package org.planit.graph;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.DirectedEdge;
import org.planit.utils.graph.DirectedGraphBuilder;
import org.planit.utils.graph.DirectedVertex;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.EdgeSegmentFactory;
import org.planit.utils.graph.EdgeSegments;

public class EdgeSegmentFactoryImpl<ES extends EdgeSegment> implements EdgeSegmentFactory<ES> {

  private final DirectedGraphBuilder<?, ?, ES> directedGraphBuilder;
  private final EdgeSegments<ES> edgeSegments;

  protected EdgeSegmentFactoryImpl(final DirectedGraphBuilder<?, ?, ES> directedGraphBuilder, EdgeSegments<ES> edgeSegments) {
    this.directedGraphBuilder = directedGraphBuilder;
    this.edgeSegments = edgeSegments;
  }

  /**
   * {@inheritDoc}
   */
  public ES create(final DirectedEdge parentEdge, final boolean directionAB) throws PlanItException {
    final ES edgeSegment = directedGraphBuilder.createEdgeSegment(directionAB);
    edgeSegment.setParentEdge(parentEdge);
    return edgeSegment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ES registerNew(DirectedEdge parentEdge, boolean directionAb, boolean registerOnNodeAndLink) throws PlanItException {
    ES edgeSegment = create(parentEdge, directionAb);
    edgeSegments.register(parentEdge, edgeSegment, directionAb);
    if (registerOnNodeAndLink) {
      parentEdge.registerEdgeSegment(edgeSegment, directionAb);
      if (parentEdge.getVertexA() instanceof DirectedVertex) {
        ((DirectedVertex) parentEdge.getVertexA()).addEdgeSegment(edgeSegment);
        ((DirectedVertex) parentEdge.getVertexB()).addEdgeSegment(edgeSegment);
      }
    }
    return edgeSegment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ES registerUniqueCopyOf(ES edgeSegmentToCopy, DirectedEdge newParentEdge) {
    final ES copy = directedGraphBuilder.createUniqueCopyOf(edgeSegmentToCopy, null /* cannot set new parent edge directly due to generics */);
    copy.setParentEdge(newParentEdge);
    edgeSegments.register(copy);
    return copy;
  }

}

package org.planit.graph;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.DirectedEdge;
import org.planit.utils.graph.DirectedGraph;
import org.planit.utils.graph.DirectedVertex;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.EdgeSegments;
import org.planit.utils.id.IdGroupingToken;

/**
 * 
 * A graph implementation consisting of vertices and edges
 * 
 * @author markr
 *
 */
public class DirectedGraphImpl<V extends DirectedVertex, E extends DirectedEdge, ES extends EdgeSegment> extends GraphImpl<V, E> implements DirectedGraph<V, E, ES> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(DirectedGraphImpl.class.getCanonicalName());

  // Protected

  /**
   * class instance containing all edge segments
   */
  protected final EdgeSegments<E, ES> edgeSegments;

  /**
   * DirectedGraph Constructor
   *
   * @param groupToken     contiguous id generation within this group for instances of this class
   * @param networkBuilder the builder to be used to create this network
   */
  public DirectedGraphImpl(final IdGroupingToken groupToken, final DirectedGraphBuilder<V, E, ES> graphBuilder) {
    super(groupToken, graphBuilder);
    this.edgeSegments = new EdgeSegmentsImpl<E, ES>(graphBuilder);
  }

  // Getters - Setters

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegments<E, ES> getEdgeSegments() {
    return edgeSegments;
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public void removeSubGraph(Set<? extends V> subNetworkToRemove) {

    /* remove the edge segment portion of the directed subgraph from the actual directed graph */
    for (DirectedVertex directedVertex : subNetworkToRemove) {
      Set<EdgeSegment> entryEdgeSegments = new HashSet<EdgeSegment>(directedVertex.getEntryEdgeSegments());
      Set<EdgeSegment> exitEdgeSegments = new HashSet<EdgeSegment>(directedVertex.getExitEdgeSegments());

      /* remove vertex' edge segments from graph */
      entryEdgeSegments.forEach(edgeSegment -> getEdgeSegments().remove((ES) edgeSegment));
      exitEdgeSegments.forEach(edgeSegment -> getEdgeSegments().remove((ES) edgeSegment));

      /* remove directed vertex from edge segments */
      entryEdgeSegments.forEach(edgeSegment -> edgeSegment.remove(directedVertex));
      exitEdgeSegments.forEach(edgeSegment -> edgeSegment.remove(directedVertex));

      /* remove edge from edge segments */
      entryEdgeSegments.forEach(edgeSegment -> edgeSegment.removeParentEdge());
      exitEdgeSegments.forEach(edgeSegment -> edgeSegment.removeParentEdge());

      /* remove edge segments from vertex */
      entryEdgeSegments.forEach(edgeSegment -> directedVertex.removeEdgeSegment(edgeSegment));
      exitEdgeSegments.forEach(edgeSegment -> directedVertex.removeEdgeSegment(edgeSegment));
    }

    /* ensure no id gaps remain after the removal of internal entities */
    if (graphBuilder instanceof DirectedGraphBuilder<?, ?, ?>) {
      ((DirectedGraphBuilder<?, E, ES>) graphBuilder).recreateIds(getEdgeSegments());
    } else {
      LOGGER.severe(
          "expected the EdgeSegments implementation to be compatible with directed graph builder, this is not the case: unable to correctly remove subnetwork and update ids");
    }

    /* do the same for vertices and edges */
    super.removeSubGraph(subNetworkToRemove);
  }

  /**
   * Identical to the {@code }GraphImpl implementation except that we now also account for the edge segments present on the edge. Copies of the original edge segments are placed on
   * vertexToBreakAt->VertexB, while the original ones are retained at VertexA->vertexToBreakAt
   * 
   * @param edgesToBreak    edges to break
   * @param vertexToBreakAt the vertex to break at
   */
  @Override
  public void breakEdgesAt(List<? extends E> edgesToBreak, V vertexToBreakAt) throws PlanItException {
    // PREP for edge segments

    // delegate regular breaking of edge
    super.breakEdgesAt(edgesToBreak, vertexToBreakAt);

    // FINALISE for (copied) edge segments
  }

}

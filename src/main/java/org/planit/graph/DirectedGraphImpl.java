package org.planit.graph;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
  private static final Logger LOGGER = Logger.getLogger(DirectedGraphImpl.class.getCanonicalName());

  // Protected

  /**
   * class instance containing all edge segments
   */
  protected final EdgeSegments<ES> edgeSegments;

  /**
   * DirectedGraph Constructor
   *
   * @param groupToken     contiguous id generation within this group for instances of this class
   * @param networkBuilder the builder to be used to create this network
   */
  public DirectedGraphImpl(final IdGroupingToken groupToken, final DirectedGraphBuilder<V, E, ES> graphBuilder) {
    super(groupToken, graphBuilder);
    this.edgeSegments = new EdgeSegmentsImpl<ES>(graphBuilder);
  }

  // Getters - Setters

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegments<ES> getEdgeSegments() {
    return edgeSegments;
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public void removeSubGraph(Set<? extends V> subNetworkToRemove, boolean recreateIds) {

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

    /* do the same for vertices and edges */
    super.removeSubGraph(subNetworkToRemove, recreateIds);
  }

  /**
   * Identical to {@link GraphImpl.recreateIds()} except that now the ids of the edge segments are also recreated on top of the vertices and edges
   */
  @SuppressWarnings("unchecked")
  @Override
  public void recreateIds() {
    super.recreateIds();

    /* ensure no id gaps remain after the removal of internal entities */
    if (graphBuilder instanceof DirectedGraphBuilder<?, ?, ?>) {
      ((DirectedGraphBuilder<?, E, ES>) graphBuilder).recreateIds(getEdgeSegments());
    } else {
      LOGGER.severe(
          "expected the EdgeSegments implementation to be compatible with directed graph builder, this is not the case: unable to correctly remove subnetwork and update ids");
    }
  }

  /**
   * Identical to the {@code }GraphImpl implementation except that we now also account for the edge segments present on the edge. Copies of the original edge segments are placed on
   * vertexToBreakAt->VertexB, while the original ones are retained at VertexA->vertexToBreakAt
   * 
   * @param edgesToBreak    edges to break
   * @param vertexToBreakAt the vertex to break at
   * @return affected edges of breaking the passed in edges, includes the newly created edges and modified existing edges
   */
  @SuppressWarnings("unchecked")
  @Override
  public Map<Long, Set<E>> breakEdgesAt(List<? extends E> edgesToBreak, V vertexToBreakAt) throws PlanItException {

    /* delegate regular breaking of edges */
    Map<Long, Set<E>> brokenEdgesByOriginalEdgeId = super.breakEdgesAt(edgesToBreak, vertexToBreakAt);

    /* edge segments have only been shallow copied since undirected graph is unaware of them */
    /* break edge segments here using the already updated vertex/edge information in affected edges */
    Set<EdgeSegment> identifiedEdgeSegmentOnEdge = new HashSet<EdgeSegment>();
    for (Entry<Long, Set<E>> entry : brokenEdgesByOriginalEdgeId.entrySet()) {
      for (E brokenEdge : entry.getValue()) {

        /* attach edge segment A-> B to the right vertices/edges, and make a unique copy if needed */
        if (brokenEdge.hasEdgeSegmentAb()) {
          EdgeSegment edgeSegmentAb = brokenEdge.getEdgeSegmentAb();

          if (identifiedEdgeSegmentOnEdge.contains(edgeSegmentAb)) {
            /* edge segment shallow copy present from breaking link in super implementation, replace by register a unique copy of edge segment on this edge */
            edgeSegmentAb = this.edgeSegments.registerUniqueCopyOf((ES) edgeSegmentAb, brokenEdge);
          } else {
            identifiedEdgeSegmentOnEdge.add(edgeSegmentAb);
          }

          /* update parent edge <-> edge segment */
          brokenEdge.replace(brokenEdge.getEdgeSegmentAb(), edgeSegmentAb);
          edgeSegmentAb.setParentEdge(brokenEdge);

          /* update segment's vertices */
          edgeSegmentAb.setUpstreamVertex((DirectedVertex) brokenEdge.getVertexA());
          edgeSegmentAb.setDownstreamVertex((DirectedVertex) brokenEdge.getVertexB());

          /* update vertices' segments */
          edgeSegmentAb.getUpstreamVertex().replace(brokenEdge.getEdgeSegmentAb(), edgeSegmentAb, true);
          edgeSegmentAb.getDownstreamVertex().replace(brokenEdge.getEdgeSegmentAb(), edgeSegmentAb, true);

          /* useful for debugging */
          // edgeSegmentAb.validate();
        }

        /* do the same for edge segment B-> A */
        if (brokenEdge.hasEdgeSegmentBa()) {
          EdgeSegment edgeSegmentBa = brokenEdge.getEdgeSegmentBa();

          if (identifiedEdgeSegmentOnEdge.contains(edgeSegmentBa)) {
            /* edge segment shallow copy present from breaking link in super implementation, replace by register a unique copy of edge segment on this edge */
            edgeSegmentBa = this.edgeSegments.registerUniqueCopyOf((ES) edgeSegmentBa, brokenEdge);
          } else {
            identifiedEdgeSegmentOnEdge.add(edgeSegmentBa);
          }
          /* update parent edge <-> edge segment */
          brokenEdge.replace(brokenEdge.getEdgeSegmentBa(), edgeSegmentBa);
          edgeSegmentBa.setParentEdge(brokenEdge);

          /* update segment's vertices */
          edgeSegmentBa.setUpstreamVertex((DirectedVertex) brokenEdge.getVertexB());
          edgeSegmentBa.setDownstreamVertex((DirectedVertex) brokenEdge.getVertexA());

          /* update vertices' segments */
          edgeSegmentBa.getUpstreamVertex().replace(brokenEdge.getEdgeSegmentBa(), edgeSegmentBa, true);
          edgeSegmentBa.getDownstreamVertex().replace(brokenEdge.getEdgeSegmentBa(), edgeSegmentBa, true);

          /* useful for debugging */
          // edgeSegmentBa.validate();
        }
      }
    }

    return brokenEdgesByOriginalEdgeId;
  }

}

package org.planit.graph.modifier;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.graph.DirectedGraphBuilder;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.DirectedEdge;
import org.planit.utils.graph.DirectedGraph;
import org.planit.utils.graph.DirectedVertex;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.modifier.BreakEdgeListener;
import org.planit.utils.graph.modifier.BreakEdgeSegmentListener;
import org.planit.utils.graph.modifier.DirectedGraphModifier;
import org.planit.utils.graph.modifier.RemoveDirectedSubGraphListener;
import org.planit.utils.graph.modifier.RemoveSubGraphListener;

/**
 * implementation of a directed graph modifier that supports making changes to a directed graph
 * 
 * @author markr
 *
 * @param <V>  vertex type
 * @param <E>  edge type
 * @param <ES> edge segment type
 */
public class DirectedGraphModifierImpl<V extends DirectedVertex, E extends DirectedEdge, ES extends EdgeSegment> extends GraphModifierImpl<V, E>
    implements DirectedGraphModifier<V, E, ES> {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(DirectedGraphModifierImpl.class.getCanonicalName());

  /**
   * Constructor
   * 
   * @param theDirectedGraph        to use
   * @param theDirectedGraphBuilder to use
   */
  public DirectedGraphModifierImpl(final DirectedGraph<V, E, ES> theDirectedGraph, final DirectedGraphBuilder<V, E, ES> theDirectedGraphBuilder) {
    super(theDirectedGraph, theDirectedGraphBuilder);
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public void removeSubGraph(Set<? extends V> subNetworkToRemove, boolean recreateIds) {
    DirectedGraph<V, E, ES> theDirectedGraph = (DirectedGraph<V, E, ES>) theGraph;

    /* remove the edge segment portion of the directed subgraph from the actual directed graph */
    for (DirectedVertex directedVertex : subNetworkToRemove) {
      Set<EdgeSegment> entryEdgeSegments = new HashSet<EdgeSegment>(directedVertex.getEntryEdgeSegments());
      Set<EdgeSegment> exitEdgeSegments = new HashSet<EdgeSegment>(directedVertex.getExitEdgeSegments());

      /* remove vertex' edge segments from graph */
      entryEdgeSegments.forEach(edgeSegment -> theDirectedGraph.getEdgeSegments().remove((ES) edgeSegment));
      exitEdgeSegments.forEach(edgeSegment -> theDirectedGraph.getEdgeSegments().remove((ES) edgeSegment));

      /* remove directed vertex from edge segments */
      entryEdgeSegments.forEach(edgeSegment -> edgeSegment.remove(directedVertex));
      exitEdgeSegments.forEach(edgeSegment -> edgeSegment.remove(directedVertex));

      /* remove edge from edge segments */
      entryEdgeSegments.forEach(edgeSegment -> edgeSegment.removeParentEdge());
      exitEdgeSegments.forEach(edgeSegment -> edgeSegment.removeParentEdge());

      /* remove edge segments from vertex */
      entryEdgeSegments.forEach(edgeSegment -> directedVertex.removeEdgeSegment(edgeSegment));
      exitEdgeSegments.forEach(edgeSegment -> directedVertex.removeEdgeSegment(edgeSegment));

      /* remove edge segments from graph */
      for (EdgeSegment edgeSegment : entryEdgeSegments) {
        theDirectedGraph.getEdgeSegments().remove((ES) edgeSegment);
        if (!registeredRemoveSubGraphListeners.isEmpty()) {
          for (RemoveSubGraphListener<V, E> listener : registeredRemoveSubGraphListeners) {
            ((RemoveDirectedSubGraphListener<V, E, ES>) listener).onRemoveSubGraphEdgeSegment((ES) edgeSegment);
          }
        }
      }
      for (EdgeSegment edgeSegment : exitEdgeSegments) {
        theDirectedGraph.getEdgeSegments().remove((ES) edgeSegment);
        if (!registeredRemoveSubGraphListeners.isEmpty()) {
          for (RemoveSubGraphListener<V, E> listener : registeredRemoveSubGraphListeners) {
            ((RemoveDirectedSubGraphListener<V, E, ES>) listener).onRemoveSubGraphEdgeSegment((ES) edgeSegment);
          }
        }
      }
    }

    /* do the same for vertices and edges */
    super.removeSubGraph(subNetworkToRemove, recreateIds);
  }

  /**
   * Identical to GraphImpl.recreateIds() except that now the ids of the edge segments are also recreated on top of the vertices and edges
   */
  @SuppressWarnings("unchecked")
  @Override
  public void recreateIds() {
    super.recreateIds();

    /* ensure no id gaps remain after the removal of internal entities */
    if (theGraphBuilder instanceof DirectedGraphBuilder<?, ?, ?>) {
      DirectedGraph<V, E, ES> theDirectedGraph = (DirectedGraph<V, E, ES>) theGraph;
      ((DirectedGraphBuilder<?, E, ES>) theGraphBuilder).recreateIds(theDirectedGraph.getEdgeSegments());
    } else {
      LOGGER.severe(
          "expected the EdgeSegments implementation to be compatible with directed graph builder, this is not the case: unable to correctly remove subnetwork and update ids");
    }
  }

  /**
   * Identical to the {@code GraphImpl} implementation except that we now also account for the edge segments present on the edge. Copies of the original edge segments are placed on
   * (vertexToBreakAt,vertexB), while the original ones are retained at (vertexA,vertexToBreakAt)
   * 
   * @param edgesToBreak    edges to break
   * @param vertexToBreakAt the vertex to break at
   * @param crs             required to update edge lengths
   * @return affected edges of breaking the passed in edges, includes the newly created edges and modified existing edges
   */
  @SuppressWarnings("unchecked")
  @Override
  public Map<Long, Set<E>> breakEdgesAt(List<? extends E> edgesToBreak, V vertexToBreakAt, CoordinateReferenceSystem crs) throws PlanItException {

    DirectedGraph<V, E, ES> theDirectedGraph = (DirectedGraph<V, E, ES>) theGraph;

    /* delegate regular breaking of edges */
    Map<Long, Set<E>> brokenEdgesByOriginalEdgeId = super.breakEdgesAt(edgesToBreak, vertexToBreakAt, crs);

    /* edge segments have only been shallow copied since undirected graph is unaware of them */
    /* break edge segments here using the already updated vertex/edge information in affected edges */
    Set<EdgeSegment> identifiedEdgeSegmentOnEdge = new HashSet<EdgeSegment>();
    for (Entry<Long, Set<E>> entry : brokenEdgesByOriginalEdgeId.entrySet()) {
      for (E brokenEdge : entry.getValue()) {

        /* attach edge segment A-> B to the right vertices/edges, and make a unique copy if needed */
        if (brokenEdge.hasEdgeSegmentAb()) {
          ES oldEdgeSegmentAb = (ES) brokenEdge.getEdgeSegmentAb();
          ES newEdgeSegmentAb = (ES) oldEdgeSegmentAb;

          if (identifiedEdgeSegmentOnEdge.contains(oldEdgeSegmentAb)) {
            /* edge segment shallow copy present from breaking link in super implementation, replace by register a unique copy of edge segment on this edge */
            newEdgeSegmentAb = theDirectedGraph.getEdgeSegments().registerUniqueCopyOf((ES) oldEdgeSegmentAb, brokenEdge);
          } else {
            /* reuse the old first */
            identifiedEdgeSegmentOnEdge.add(newEdgeSegmentAb);
          }

          /* update parent edge <-> edge segment */
          brokenEdge.replace(oldEdgeSegmentAb, newEdgeSegmentAb);
          newEdgeSegmentAb.setParentEdge(brokenEdge);

          /* update segment's vertices */
          newEdgeSegmentAb.setUpstreamVertex((DirectedVertex) brokenEdge.getVertexA());
          newEdgeSegmentAb.setDownstreamVertex((DirectedVertex) brokenEdge.getVertexB());

          /* update vertices' segments */
          newEdgeSegmentAb.getUpstreamVertex().replaceExitSegment(oldEdgeSegmentAb, newEdgeSegmentAb, true);
          newEdgeSegmentAb.getDownstreamVertex().replaceEntrySegment(oldEdgeSegmentAb, newEdgeSegmentAb, true);

          if (!registeredBreakEdgeListeners.isEmpty()) {
            for (BreakEdgeListener<V, E> listener : registeredBreakEdgeListeners) {
              if (listener instanceof BreakEdgeSegmentListener<?, ?, ?>) {
                ((BreakEdgeSegmentListener<V, E, ES>) listener).onBreakEdgeSegment(vertexToBreakAt, brokenEdge, newEdgeSegmentAb);
              }
            }
          }

          /* useful for debugging */
          // edgeSegmentAb.validate();
        }

        /* do the same for edge segment B-> A */
        if (brokenEdge.hasEdgeSegmentBa()) {
          ES oldEdgeSegmentBa = (ES) brokenEdge.getEdgeSegmentBa();
          ES newEdgeSegmentBa = (ES) oldEdgeSegmentBa;

          if (identifiedEdgeSegmentOnEdge.contains(oldEdgeSegmentBa)) {
            /* edge segment shallow copy present from breaking link in super implementation, replace by register a unique copy of edge segment on this edge */
            newEdgeSegmentBa = theDirectedGraph.getEdgeSegments().registerUniqueCopyOf((ES) oldEdgeSegmentBa, brokenEdge);
          } else {
            identifiedEdgeSegmentOnEdge.add(newEdgeSegmentBa);
          }
          /* update parent edge <-> edge segment */
          brokenEdge.replace(oldEdgeSegmentBa, newEdgeSegmentBa);
          newEdgeSegmentBa.setParentEdge(brokenEdge);

          /* update segment's vertices */
          newEdgeSegmentBa.setUpstreamVertex((DirectedVertex) brokenEdge.getVertexB());
          newEdgeSegmentBa.setDownstreamVertex((DirectedVertex) brokenEdge.getVertexA());

          /* update vertices' segments */
          newEdgeSegmentBa.getUpstreamVertex().replaceExitSegment(oldEdgeSegmentBa, newEdgeSegmentBa, true);
          newEdgeSegmentBa.getDownstreamVertex().replaceEntrySegment(oldEdgeSegmentBa, newEdgeSegmentBa, true);

          if (!registeredBreakEdgeListeners.isEmpty()) {
            for (BreakEdgeListener<V, E> listener : registeredBreakEdgeListeners) {
              if (listener instanceof BreakEdgeSegmentListener<?, ?, ?>) {
                ((BreakEdgeSegmentListener<V, E, ES>) listener).onBreakEdgeSegment(vertexToBreakAt, brokenEdge, newEdgeSegmentBa);
              }
            }
          }

          /* useful for debugging */
          // edgeSegmentBa.validate();
        }
      }

    }

    return brokenEdgesByOriginalEdgeId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void registerRemoveSubGraphListener(RemoveDirectedSubGraphListener<V, E, ES> subGraphRemovalListener) {
    super.registerRemoveSubGraphListener((RemoveSubGraphListener<V, E>) subGraphRemovalListener);
  }

}

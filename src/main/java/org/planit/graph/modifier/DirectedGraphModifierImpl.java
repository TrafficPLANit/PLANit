package org.planit.graph.modifier;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.graph.modifier.event.BreakEdgeSegmentEvent;
import org.planit.graph.modifier.event.RemoveSubGraphEdgeSegmentEvent;
import org.planit.utils.event.Event;
import org.planit.utils.event.EventListener;
import org.planit.utils.event.EventProducerImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.UntypedDirectedGraph;
import org.planit.utils.graph.directed.DirectedEdge;
import org.planit.utils.graph.directed.DirectedVertex;
import org.planit.utils.graph.modifier.DirectedGraphModifier;
import org.planit.utils.graph.modifier.event.DirectedGraphModificationEvent;
import org.planit.utils.graph.modifier.event.DirectedGraphModifierListener;
import org.planit.utils.graph.modifier.event.GraphModifierEventType;
import org.planit.utils.graph.modifier.event.GraphModifierListener;

/**
 * Implementation of a directed graph modifier that supports making changes to any untyped directed graph. The benefit of using the untyped directed graph is that it does not rely
 * on knowing the specific typed containers used for vertices, edges, edge segments which in turn signals that no information on the underlying factories is required.
 * 
 * @author markr
 *
 */
public class DirectedGraphModifierImpl extends EventProducerImpl implements DirectedGraphModifier {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(DirectedGraphModifierImpl.class.getCanonicalName());

  /**
   * Reuse for non-directed modifications aspects while being able to override signatures and generic types for directed graph aspects
   */
  private final GraphModifierImpl graphModifier;

  /**
   * Depending on whether these are directed or undirected evens call the appropriate notification method
   */
  @Override
  protected void fireEvent(EventListener eventListener, Event event) {
    if (event.getType() instanceof DirectedGraphModifierListener) {
      DirectedGraphModifierListener.class.cast(eventListener).onDirectedGraphModificationEvent(DirectedGraphModificationEvent.class.cast(event));
    } else {
      graphModifier.fireEvent(eventListener, event);
    }
  }

  /**
   * Access to directed graph we are modifying
   * 
   * @return directed graph
   */
  protected UntypedDirectedGraph<?, ?, ?> getUntypedDirectedGraph() {
    return (UntypedDirectedGraph<?, ?, ?>) graphModifier.theGraph;
  }

  /**
   * Constructor.
   * 
   * @param theDirectedGraph to use
   */
  public DirectedGraphModifierImpl(final UntypedDirectedGraph<?, ?, ?> theDirectedGraph) {
    this.graphModifier = new GraphModifierImpl(theDirectedGraph);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeSubGraph(Set<? extends DirectedVertex> subGraphToRemove, boolean recreateIds) {
    UntypedDirectedGraph<?, ?, ?> directedGraph = getUntypedDirectedGraph();
    /* remove the edge segment portion of the directed subgraph from the actual directed graph */
    for (DirectedVertex directedVertex : subGraphToRemove) {

      Set<EdgeSegment> entryEdgeSegments = new HashSet<EdgeSegment>(directedVertex.getEntryEdgeSegments());
      Set<EdgeSegment> exitEdgeSegments = new HashSet<EdgeSegment>(directedVertex.getExitEdgeSegments());

      /* remove vertex' edge segments from graph */
      entryEdgeSegments.forEach(edgeSegment -> directedGraph.getEdgeSegments().remove(edgeSegment.getId()));
      exitEdgeSegments.forEach(edgeSegment -> directedGraph.getEdgeSegments().remove(edgeSegment.getId()));

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
        directedGraph.getEdgeSegments().remove(edgeSegment.getId());
        if (hasListener(RemoveSubGraphEdgeSegmentEvent.EVENT_TYPE)) {
          fireEvent(new RemoveSubGraphEdgeSegmentEvent(this, edgeSegment));
        }
      }
      for (EdgeSegment edgeSegment : exitEdgeSegments) {
        directedGraph.getEdgeSegments().remove(edgeSegment.getId());
        if (hasListener(RemoveSubGraphEdgeSegmentEvent.EVENT_TYPE)) {
          fireEvent(new RemoveSubGraphEdgeSegmentEvent(this, edgeSegment));
        }
      }
    }

    /* do the same for vertices and edges */
    graphModifier.removeSubGraph(subGraphToRemove, recreateIds);
  }

  /**
   * Recreate ids of edges, vertices, and edge segments
   */
  @Override
  public void recreateIds() {
    graphModifier.recreateIds();
    getUntypedDirectedGraph().getEdgeSegments().recreateIds();
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
  @Override
  public <Ex extends DirectedEdge> Map<Long, Set<Ex>> breakEdgesAt(List<Ex> edgesToBreak, DirectedVertex vertexToBreakAt, CoordinateReferenceSystem crs) throws PlanItException {

    /* delegate regular breaking of edges */
    Map<Long, Set<Ex>> brokenEdgesByOriginalEdgeId = graphModifier.breakEdgesAt(edgesToBreak, vertexToBreakAt, crs);

    /* edge segments have only been shallow copied since undirected graph is unaware of them */
    /* break edge segments here using the already updated vertex/edge information in affected edges */
    Set<EdgeSegment> identifiedEdgeSegmentOnEdge = new HashSet<EdgeSegment>();
    for (Entry<Long, Set<Ex>> entry : brokenEdgesByOriginalEdgeId.entrySet()) {
      for (Ex brokenEdge : entry.getValue()) {
        /* attach edge segment A-> B to the right vertices/edges, and make a unique copy if needed */
        if (brokenEdge.hasEdgeSegmentAb()) {
          EdgeSegment oldEdgeSegmentAb = brokenEdge.getEdgeSegmentAb();
          EdgeSegment newEdgeSegmentAb = oldEdgeSegmentAb;

          if (identifiedEdgeSegmentOnEdge.contains(oldEdgeSegmentAb)) {
            /* edge segment shallow copy present from breaking link in super implementation, replace by register a unique copy of edge segment on this edge */
            newEdgeSegmentAb = getUntypedDirectedGraph().getEdgeSegments().getFactory().registerUniqueCopyOf(oldEdgeSegmentAb);
            newEdgeSegmentAb.setParent(brokenEdge);
          } else {
            /* reuse the old first */
            identifiedEdgeSegmentOnEdge.add(newEdgeSegmentAb);
          }

          /* update parent edge <-> edge segment */
          brokenEdge.replace(oldEdgeSegmentAb, newEdgeSegmentAb);
          newEdgeSegmentAb.setParent(brokenEdge);

          /* update segment's vertices */
          newEdgeSegmentAb.setUpstreamVertex((DirectedVertex) brokenEdge.getVertexA());
          newEdgeSegmentAb.setDownstreamVertex((DirectedVertex) brokenEdge.getVertexB());

          /* update vertices' segments */
          newEdgeSegmentAb.getUpstreamVertex().replaceExitSegment(oldEdgeSegmentAb, newEdgeSegmentAb, true);
          newEdgeSegmentAb.getDownstreamVertex().replaceEntrySegment(oldEdgeSegmentAb, newEdgeSegmentAb, true);

          if (graphModifier.hasListener(BreakEdgeSegmentEvent.EVENT_TYPE)) {
            fireEvent(new BreakEdgeSegmentEvent(this, vertexToBreakAt, newEdgeSegmentAb));
          }

          /* useful for debugging */
          // edgeSegmentAb.validate();
        }

        /* do the same for edge segment B-> A */
        if (brokenEdge.hasEdgeSegmentBa()) {
          EdgeSegment oldEdgeSegmentBa = brokenEdge.getEdgeSegmentBa();
          EdgeSegment newEdgeSegmentBa = oldEdgeSegmentBa;

          if (identifiedEdgeSegmentOnEdge.contains(oldEdgeSegmentBa)) {
            /* edge segment shallow copy present from breaking link in super implementation, replace by register a unique copy of edge segment on this edge */
            newEdgeSegmentBa = getUntypedDirectedGraph().getEdgeSegments().getFactory().registerUniqueCopyOf(oldEdgeSegmentBa);
            newEdgeSegmentBa.setParent(brokenEdge);
          } else {
            identifiedEdgeSegmentOnEdge.add(newEdgeSegmentBa);
          }
          /* update parent edge <-> edge segment */
          brokenEdge.replace(oldEdgeSegmentBa, newEdgeSegmentBa);
          newEdgeSegmentBa.setParent(brokenEdge);

          /* update segment's vertices */
          newEdgeSegmentBa.setUpstreamVertex((DirectedVertex) brokenEdge.getVertexB());
          newEdgeSegmentBa.setDownstreamVertex((DirectedVertex) brokenEdge.getVertexA());

          /* update vertices' segments */
          newEdgeSegmentBa.getUpstreamVertex().replaceExitSegment(oldEdgeSegmentBa, newEdgeSegmentBa, true);
          newEdgeSegmentBa.getDownstreamVertex().replaceEntrySegment(oldEdgeSegmentBa, newEdgeSegmentBa, true);

          if (graphModifier.hasListener(BreakEdgeSegmentEvent.EVENT_TYPE)) {
            fireEvent(new BreakEdgeSegmentEvent(this, vertexToBreakAt, newEdgeSegmentBa));
          }

          /* useful for debugging */
          // edgeSegmentBa.validate();
        }
      }

    }

    return brokenEdgesByOriginalEdgeId;
  }

  /* DELEGATED METHOD CALLS */

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeDanglingSubGraphs(Integer belowSize, Integer aboveSize, boolean alwaysKeepLargest) throws PlanItException {
    graphModifier.removeDanglingSubGraphs(belowSize, aboveSize, alwaysKeepLargest);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeSubGraphOf(DirectedVertex referenceVertex, boolean recreateIds) throws PlanItException {
    graphModifier.removeSubGraphOf(referenceVertex, recreateIds);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    graphModifier.reset();
    removeAllListeners();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(GraphModifierListener listener) {
    if (listener instanceof DirectedGraphModifierListener) {
      super.addListener(listener);
    } else {
      graphModifier.addListener(listener);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(GraphModifierListener listener, GraphModifierEventType eventType) {
    if (listener instanceof DirectedGraphModifierListener) {
      super.addListener(listener, eventType);
    } else {
      graphModifier.addListener(listener, eventType);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(GraphModifierListener listener, GraphModifierEventType eventType) {
    if (listener instanceof DirectedGraphModifierListener) {
      super.removeListener(listener, eventType);
    } else {
      graphModifier.removeListener(listener, eventType);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(GraphModifierListener listener) {
    if (listener instanceof DirectedGraphModifierListener) {
      super.removeListener(listener);
    } else {
      graphModifier.removeListener(listener);
    }
  }

}

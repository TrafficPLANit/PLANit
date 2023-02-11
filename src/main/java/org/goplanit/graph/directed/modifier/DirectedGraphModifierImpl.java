package org.goplanit.graph.directed.modifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.goplanit.graph.modifier.GraphModifierImpl;
import org.goplanit.graph.directed.modifier.event.BreakEdgeSegmentEvent;
import org.goplanit.graph.directed.modifier.event.RecreatedDirectedGraphEntitiesManagedIdsEvent;
import org.goplanit.graph.directed.modifier.event.RemoveSubGraphEdgeSegmentEvent;
import org.goplanit.utils.event.Event;
import org.goplanit.utils.event.EventListener;
import org.goplanit.utils.event.EventProducerImpl;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.graph.GraphEntities;
import org.goplanit.utils.graph.UntypedDirectedGraph;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.graph.modifier.DirectedGraphModifier;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModifierEventType;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModifierListener;
import org.goplanit.utils.graph.modifier.event.GraphModifierEventType;
import org.goplanit.utils.graph.modifier.event.GraphModifierListener;
import org.goplanit.utils.id.ManagedIdEntities;
import org.goplanit.utils.misc.Pair;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

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
    if (event.getType() instanceof DirectedGraphModifierEventType) {
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
  public UntypedDirectedGraph<?, ?, ?> getUntypedDirectedGraph() {
    return (UntypedDirectedGraph<?, ?, ?>) graphModifier.getGraph();
  }

  /**
   * For each broken edge, its underlying edge segments are simply copies of the original edge. These require updating as well. the two original segments are reused on one of the
   * two sections of the broken edge, whereas two new segments are created for the other part
   * 
   * @param <Ex>     type of directed edge
   * @param aToBreak first brokenLinkSection
   * @param breakToB second brokenLinkSection
   */
  @SuppressWarnings("unchecked")
  private <Ex extends DirectedEdge> void updateBrokenEdgeItsEdgeSegments(Ex aToBreak, Ex breakToB) {
    DirectedVertex vertexAtBreak = aToBreak.getVertexB();

    List<EdgeSegment> identifiedEdgeSegmentOnEdge = new ArrayList<>(2);
    for (Ex brokenEdge : List.of(aToBreak, breakToB)) {
      /* attach edge segment A-> B to the right vertices/edges, and make a unique copy if needed */
      if (brokenEdge.hasEdgeSegmentAb()) {
        EdgeSegment oldEdgeSegmentAb = brokenEdge.getEdgeSegmentAb();
        EdgeSegment newEdgeSegmentAb = oldEdgeSegmentAb;

        if (identifiedEdgeSegmentOnEdge.contains(oldEdgeSegmentAb)) {
          /* edge segment shallow copy present from breaking link in super implementation, replace by register a unique copy of edge segment on this edge */
          newEdgeSegmentAb = getUntypedDirectedGraph().getEdgeSegments().getFactory().createUniqueDeepCopyOf(oldEdgeSegmentAb);
          ((GraphEntities<EdgeSegment>) getUntypedDirectedGraph().getEdgeSegments()).register(newEdgeSegmentAb);
          newEdgeSegmentAb.setParent(brokenEdge);
        } else {
          /* reuse the old first */
          identifiedEdgeSegmentOnEdge.add(newEdgeSegmentAb);
        }

        /* update parent edge <-> edge segment */
        brokenEdge.replace(oldEdgeSegmentAb, newEdgeSegmentAb);
        newEdgeSegmentAb.setParent(brokenEdge);

        if (hasListener(BreakEdgeSegmentEvent.EVENT_TYPE)) {
          fireEvent(new BreakEdgeSegmentEvent(this, vertexAtBreak, newEdgeSegmentAb));
        }

        /* useful for debugging */
        // newEdgeSegmentAb.validate();
      }

      /* do the same for edge segment B-> A */
      if (brokenEdge.hasEdgeSegmentBa()) {
        EdgeSegment oldEdgeSegmentBa = brokenEdge.getEdgeSegmentBa();
        EdgeSegment newEdgeSegmentBa = oldEdgeSegmentBa;

        if (identifiedEdgeSegmentOnEdge.contains(oldEdgeSegmentBa)) {
          /* edge segment shallow copy present from breaking link in super implementation, replace by register a unique copy of edge segment on this edge */
          newEdgeSegmentBa = getUntypedDirectedGraph().getEdgeSegments().getFactory().createUniqueDeepCopyOf(oldEdgeSegmentBa);
          ((GraphEntities<EdgeSegment>) getUntypedDirectedGraph().getEdgeSegments()).register(newEdgeSegmentBa);
          newEdgeSegmentBa.setParent(brokenEdge);
        } else {
          identifiedEdgeSegmentOnEdge.add(newEdgeSegmentBa);
        }
        /* update parent edge <-> edge segment */
        brokenEdge.replace(oldEdgeSegmentBa, newEdgeSegmentBa);
        newEdgeSegmentBa.setParent(brokenEdge);

        if (hasListener(BreakEdgeSegmentEvent.EVENT_TYPE)) {
          fireEvent(new BreakEdgeSegmentEvent(this, vertexAtBreak, newEdgeSegmentBa));
        }

        /* useful for debugging */
        // newEdgeSegmentBa.validate();
      }
    }
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
  public void removeVertex(DirectedVertex vertex) {
    this.graphModifier.removeVertex(vertex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeEdge(DirectedEdge edge) {
    edge.removeEdgeSegments();
    this.graphModifier.removeEdge(edge);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeEdgeSegment(EdgeSegment edgeSegment) {
    edgeSegment.removeParentEdge();
    getUntypedDirectedGraph().getEdgeSegments().remove(edgeSegment.getId());
    if (hasListener(RemoveSubGraphEdgeSegmentEvent.EVENT_TYPE)) {
      fireEvent(new RemoveSubGraphEdgeSegmentEvent(this, edgeSegment));
    }
    // edge segments will be removed from edge via overridden removeEdge method
    // todo remove this line if this works (old: directedEdge.removeEdgeSegments(); here within removeSubgraph implementation before this method existed)
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeSubGraph(Set<? extends DirectedVertex> subGraphToRemove) {
    UntypedDirectedGraph<?, ?, ?> directedGraph = getUntypedDirectedGraph();

    /* remove the edge segment portion of the directed subgraph from the actual directed graph and fire event(s)*/
    for (DirectedVertex directedVertex : subGraphToRemove) {
      for (DirectedEdge directedEdge : directedVertex.getEdges()) {
        for (EdgeSegment edgeSegment : directedEdge.getEdgeSegments()) {
          removeEdgeSegment(edgeSegment);
        }
      }
    }

    /* do the same for vertices and edges */
    graphModifier.removeSubGraph(subGraphToRemove);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateManagedEntitiesIds() {
    graphModifier.recreateManagedEntitiesIds();
    if (getUntypedDirectedGraph().getEdgeSegments() instanceof ManagedIdEntities<?>) {
      ((ManagedIdEntities<?>) getUntypedDirectedGraph().getEdgeSegments()).recreateIds();
      fireEvent(new RecreatedDirectedGraphEntitiesManagedIdsEvent(this, (ManagedIdEntities<?>) getUntypedDirectedGraph().getEdgeSegments()));
    }
  }

  /**
   * Identical to the {@code GraphImpl} implementation except that we now also account for the edge segments present on the edge. Copies of the original edge segments are placed on
   * (vertexToBreakAt,vertexB), while the original ones are retained at (vertexA,vertexToBreakAt)
   * 
   * @param edgeToBreak     edge to break
   * @param vertexToBreakAt the vertex to break at
   * @param geoUtils        required to update edge lengths
   * @return newly created edge due to breaking, null if not feasible
   */
  @Override
  public <Ex extends DirectedEdge> Ex breakEdgeAt(DirectedVertex vertexToBreakAt, Ex edgeToBreak, PlanitJtsCrsUtils geoUtils) {
    Ex aToBreak = edgeToBreak;
    Ex breakToB = graphModifier.breakEdgeAt(vertexToBreakAt, edgeToBreak, geoUtils);

    /* update the underlying directed edge segments */
    updateBrokenEdgeItsEdgeSegments(aToBreak, breakToB);

    return breakToB;
  }

  /* DELEGATED METHOD CALLS */

  /**
   * {@inheritDoc}
   */
  @Override
  public <Ex extends DirectedEdge> Map<Long, Pair<Ex, Ex>> breakEdgesAt(List<Ex> edgesToBreak, DirectedVertex vertexToBreakAt, CoordinateReferenceSystem crs) {

    /* delegate regular breaking of edges */
    Map<Long, Pair<Ex, Ex>> brokenEdges = graphModifier.breakEdgesAt(edgesToBreak, vertexToBreakAt, crs);

    /* update the underlying directed edge segments */
    for (Entry<Long, Pair<Ex, Ex>> entry : brokenEdges.entrySet()) {
      updateBrokenEdgeItsEdgeSegments(entry.getValue().first(), entry.getValue().second());
    }

    return brokenEdges;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeDanglingSubGraphs(Integer belowSize, Integer aboveSize, boolean alwaysKeepLargest) {
    graphModifier.removeDanglingSubGraphs(belowSize, aboveSize, alwaysKeepLargest);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeSubGraphOf(DirectedVertex referenceVertex) throws PlanItException {
    graphModifier.removeSubGraphOf(referenceVertex);
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
    }

    if(listener instanceof GraphModifierListener){
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

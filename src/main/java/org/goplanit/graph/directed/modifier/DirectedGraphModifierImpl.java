package org.goplanit.graph.directed.modifier;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.goplanit.graph.directed.modifier.event.RemoveEdgeSegmentEvent;
import org.goplanit.graph.directed.modifier.event.RemoveMovementEvent;
import org.goplanit.graph.modifier.GraphModifierImpl;
import org.goplanit.graph.directed.modifier.event.BreakEdgeSegmentEvent;
import org.goplanit.graph.directed.modifier.event.RecreatedDirectedGraphEntitiesManagedIdsEvent;
import org.goplanit.utils.event.Event;
import org.goplanit.utils.event.EventListener;
import org.goplanit.utils.event.EventProducerImpl;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.graph.*;
import org.goplanit.utils.graph.directed.*;
import org.goplanit.utils.graph.modifier.DirectedGraphModifier;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModifierEventType;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModifierListener;
import org.goplanit.utils.graph.modifier.event.GraphModifierEventType;
import org.goplanit.utils.graph.modifier.event.GraphModifierListener;
import org.goplanit.utils.id.ManagedIdEntities;
import org.goplanit.utils.misc.Pair;

/**
 * Implementation of a directed graph modifier that supports making changes to any untyped directed graph.
 * The benefit of using the untyped directed graph is that it does not rely
 * on knowing the specific typed containers used for vertices, edges, edge segments which in turn signals
 * that no information on the underlying factories is required.
 *
 * @author markr
 *
 */
public class DirectedGraphModifierImpl extends EventProducerImpl
    implements DirectedGraphModifier<DirectedVertex, DirectedEdge, EdgeSegment> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(DirectedGraphModifierImpl.class.getCanonicalName());

  /**
   * Reuse for non-directed modifications aspects while being able to override signatures and generic types for
   * directed graph aspects
   */
  private final GraphModifierImpl<DirectedVertex, DirectedEdge> graphModifier;

  /**
   * Depending on whether these are directed or undirected evens call the appropriate notification method
   */
  @Override
  protected void fireEvent(EventListener eventListener, Event event) {
    if (event.getType() instanceof DirectedGraphModifierEventType) {
      ((DirectedGraphModifierListener) eventListener).onDirectedGraphModificationEvent(
          (DirectedGraphModificationEvent) event);
    } else {
      graphModifier.fireEvent(eventListener, event);
    }
  }

  /**
   * For each broken edge, its underlying edge segments are simply copies of the original edge. These require
   * updating as well. the two original segments are reused on one of the
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
          /* edge segment shallow copy present from breaking link in super implementation, replace by register a
          unique copy of edge segment on this edge */
          newEdgeSegmentAb =
              getGraph().getEdgeSegments().getFactory().createUniqueDeepCopyOf(oldEdgeSegmentAb);
          getGraph().getEdgeSegments().register(newEdgeSegmentAb);
          newEdgeSegmentAb.setParent(brokenEdge);
        } else {
          /* reuse the old first */
          identifiedEdgeSegmentOnEdge.add(newEdgeSegmentAb);
        }

        /* update parent edge <-> edge segment */
        brokenEdge.replace(oldEdgeSegmentAb, newEdgeSegmentAb);
        newEdgeSegmentAb.setParent(brokenEdge);

        if (hasListener(BreakEdgeSegmentEvent.EVENT_TYPE)) {
          fireEvent(new BreakEdgeSegmentEvent(this, vertexAtBreak, newEdgeSegmentAb, oldEdgeSegmentAb));
        }

        /* useful for debugging */
        // newEdgeSegmentAb.validate();
      }

      /* do the same for edge segment B-> A */
      if (brokenEdge.hasEdgeSegmentBa()) {
        EdgeSegment oldEdgeSegmentBa = brokenEdge.getEdgeSegmentBa();
        EdgeSegment newEdgeSegmentBa = oldEdgeSegmentBa;

        if (identifiedEdgeSegmentOnEdge.contains(oldEdgeSegmentBa)) {
          /* edge segment shallow copy present from breaking link in super implementation,
          replace by register a unique copy of edge segment on this edge */
          newEdgeSegmentBa = getGraph().getEdgeSegments().getFactory().
              createUniqueDeepCopyOf(oldEdgeSegmentBa);
          getGraph().getEdgeSegments().register(newEdgeSegmentBa);
          newEdgeSegmentBa.setParent(brokenEdge);
        } else {
          identifiedEdgeSegmentOnEdge.add(newEdgeSegmentBa);
        }
        /* update parent edge <-> edge segment */
        brokenEdge.replace(oldEdgeSegmentBa, newEdgeSegmentBa);
        newEdgeSegmentBa.setParent(brokenEdge);

        if (hasListener(BreakEdgeSegmentEvent.EVENT_TYPE)) {
          fireEvent(new BreakEdgeSegmentEvent(this, vertexAtBreak, newEdgeSegmentBa, oldEdgeSegmentBa));
        }

        /* useful for debugging */
        // newEdgeSegmentBa.validate();
      }
    }
  }

  /**
   * for a broken segment's attached movement, we are updating its from segment which interacts with the broken
   * edge segment, hence it has a stale segment it references now.
   *
   * @param <Ex>                    type of directed edge
   * @param aToBreak                first brokenLinkSection
   * @param breakToB                second brokenLinkSection
   * @param touchedMovements to update from segment for any touched movements
   */
  private <Ex extends DirectedEdge> void updateMovementsItsBrokenSegments(
      Ex aToBreak, Ex breakToB, List<BannedMovement> touchedMovements) {

    for(var bannedMovement : touchedMovements){

      // for from segment, this is the only situation where it is altered (as AToBreak is reused just shortened)
      if(bannedMovement.getSegmentFrom().getUpstreamVertex().equals(aToBreak.getVertexA()) &&
          breakToB.getVertexB().equals(bannedMovement.getSegmentTo().getUpstreamVertex())){
        bannedMovement.setSegmentFrom(breakToB.getSegmentUpstreamOf(breakToB.getVertexB()));
      }

      // for to segment this is the only situations where it is altered
      if(bannedMovement.getSegmentTo().getDownstreamVertex().equals(aToBreak.getVertexA()) &&
          breakToB.getVertexB().equals(bannedMovement.getSegmentFrom().getDownstreamVertex())){
        bannedMovement.setSegmentTo(breakToB.getSegmentDownstreamFrom(breakToB.getVertexB()));
      }

    }


    // no event yet since the entity itself is not really changing, may need to reconsider in future
  }

  /**
   * Constructor.
   * 
   * @param theDirectedGraph to use
   */
  public DirectedGraphModifierImpl(final UntypedDirectedGraph<?, ?, ?> theDirectedGraph) {
    this.graphModifier = new GraphModifierImpl<>(theDirectedGraph);
  }

  /**
   * Access to graph
   *
   * @return the underlying graph
   */
  public UntypedDirectedGraph<DirectedVertex,DirectedEdge,EdgeSegment> getGraph(){
    return (UntypedDirectedGraph<DirectedVertex, DirectedEdge, EdgeSegment>) graphModifier.getGraph();
  }

  /**
   * {@inheritDoc}
   *
   * No attached segments or movements will be removed
   */
  @Override
  public void removeVertex(DirectedVertex vertex) {
    this.graphModifier.removeVertex(vertex);
  }

  /**
   * {@inheritDoc}
   *
   */
  @Override
  public void removeEdge(DirectedEdge edge) {
    edge.removeEdgeSegments();
    this.graphModifier.removeEdge(edge);
  }

  /**
   * {@inheritDoc}
   *
   */
  @Override
  public void removeDanglingSubGraphs(Integer belowSize, Integer aboveSize, boolean alwaysKeepLargest) {

    // this is the lenient setup currently for the predicate, needs a way to toggle between lenient and strict
    // such that we cna use it for identification (Step 1), and then for removal (step 2)
    // we could move this to a per element check --> so check vertices, check links etc. and then remove
    // stuff

    Predicate<Edge> alwaysTrue = e -> true;
    removeDanglingSubGraphs(
        belowSize,
        aboveSize,
        alwaysKeepLargest,
        (v -> DirectedGraphUtils.identifySubGraphForVertex(
            this.getGraph(), v, alwaysTrue)));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeDanglingSubGraphs(
      Integer belowSize,
      Integer aboveSize,
      boolean alwaysKeepLargest,
      Function<DirectedVertex, ? extends UntypedSubGraph<DirectedVertex, DirectedEdge>> identifySubGraphForVertex) {
    /* supply our own removal, otherwise the delegate calls its own undirected removeSubGraph and the directed
     * portion of each subgraph - edge segments and banned movements - is left behind on the graph */
    graphModifier.removeDanglingSubGraphs(
        belowSize, aboveSize, alwaysKeepLargest, identifySubGraphForVertex, this::removeSubGraph);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeDanglingDirectedSubGraphs(
      Integer belowSize,
      Integer aboveSize,
      boolean alwaysKeepLargest,
      Function<DirectedVertex, ? extends UntypedDirectedSubGraph<DirectedVertex, DirectedEdge, EdgeSegment>>
          identifySubGraphForVertex) {
    // can delegate since function itself fully determines the directed aspect of what is being dealt with
    removeDanglingSubGraphs(belowSize, aboveSize, alwaysKeepLargest, identifySubGraphForVertex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeEdgeSegment(EdgeSegment edgeSegment) {
    getGraph().getEdgeSegments().remove(edgeSegment.getId());
    if(edgeSegment.getParent() != null){
      edgeSegment.getParent().removeEdgeSegment(edgeSegment);
    }
    edgeSegment.removeParentEdge();

    if (hasListener(RemoveEdgeSegmentEvent.EVENT_TYPE)) {
      fireEvent(new RemoveEdgeSegmentEvent(this, edgeSegment));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeMovement(BannedMovement movement) {
    getGraph().getMovements().remove(movement.getId());
    if(movement.hasSegmentFrom()){
      movement.setSegmentFrom(null);
    }
    if(movement.hasSegmentTo()){
      movement.setSegmentTo(null);
    }

    if (hasListener(RemoveEdgeSegmentEvent.EVENT_TYPE)) {
      fireEvent(new RemoveMovementEvent(this, movement));
    }
  }


  @Override
  public void removeDirectedSubGraph(
      UntypedDirectedSubGraph<DirectedVertex, DirectedEdge, EdgeSegment> subGraphToRemove) {

    /* establish what actually disappears before mutating anything. The graph's containers are live views, so
     * removing while traversing them risks a ConcurrentModificationException */
    var verticesToRemove = subGraphToRemove.getVertices(getGraph().getVertices());

    /* an edge cannot outlive its vertices and a segment cannot outlive its parent edge, so every segment hanging
     * off an edge of a removed vertex has to go. Deriving these from the vertices rather than from subgraph
     * membership alone is what prevents stranded segments: the identification rules may exclude a segment while
     * still including its parent edge, and that edge is removed along with its vertex */
    var edgeSegmentsToRemove = new HashSet<EdgeSegment>();
    for (var vertex : verticesToRemove) {
      for (var directedEdge : vertex.getEdges()) {
        if (directedEdge.hasEdgeSegmentAb()) {
          edgeSegmentsToRemove.add(directedEdge.getEdgeSegmentAb());
        }
        if (directedEdge.hasEdgeSegmentBa()) {
          edgeSegmentsToRemove.add(directedEdge.getEdgeSegmentBa());
        }
      }
    }

    /* supplement with anything the subgraph listed explicitly, which covers segments whose vertices are retained */
    for (var segment : getGraph().getEdgeSegments()) {
      if(subGraphToRemove.containsEdgeSegment(segment)) {
        edgeSegmentsToRemove.add(segment);
      }
    }

    // remove the movements touching the sub graph to remove (+ test positive for removal on the edges as well)
    if(getGraph().hasMovements()){
      var movementsByCentreVertex =
          getGraph().getMovements().createGroupByIndex(BannedMovement::getCentreVertex);
      movementsByCentreVertex.values().stream()
          .flatMap(Collection::stream)
          .filter(bm ->
              edgeSegmentsToRemove.contains(bm.getSegmentFrom()) ||
                  edgeSegmentsToRemove.contains(bm.getSegmentTo()))
          .forEach(this::removeMovement);
    }

    /* remove the edge segment portion of the directed subgraph from the actual directed graph and fire event(s)*/
    for (var segment : edgeSegmentsToRemove) {
      removeEdgeSegment(segment);
    }

    /* do the same for vertices and edges */
    graphModifier.removeSubGraph(subGraphToRemove);
  }

  /**
   * when a non-directed subgraph is provided perform undirected subgraph removal otherwise delegate to
   * the directed subgraph removal provided here
   */
  @Override
  public void removeSubGraph(UntypedSubGraph<DirectedVertex, DirectedEdge> subGraphToRemove) {
    if( subGraphToRemove instanceof UntypedDirectedSubGraph){
      removeDirectedSubGraph(
          (UntypedDirectedSubGraph<DirectedVertex, DirectedEdge, EdgeSegment>)subGraphToRemove);
    }else{
      graphModifier.removeSubGraph(subGraphToRemove);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateManagedEntitiesIds() {
    graphModifier.recreateManagedEntitiesIds();
    if (getGraph().getEdgeSegments() instanceof ManagedIdEntities<?>) {
      ((ManagedIdEntities<?>) getGraph().getEdgeSegments()).recreateIds();
      fireEvent(new RecreatedDirectedGraphEntitiesManagedIdsEvent(
          this, (ManagedIdEntities<?>) getGraph().getEdgeSegments()));
    }
    if (getGraph().getMovements() != null) {
      getGraph().getMovements().recreateIds();
      fireEvent(new RecreatedDirectedGraphEntitiesManagedIdsEvent(
          this, getGraph().getMovements()));
    }
  }

  /**
   * Identical to the {@code GraphImpl} implementation except that we now also account for the edge segments
   * present on the edge. Copies of the original edge segments are placed on
   * (vertexToBreakAt,vertexB), while the original ones are retained at (vertexA,vertexToBreakAt).
   * <p> if the layer has more than a few banned movements, consider using the breakEdgeAt that takes indexed movements
   * to avoid performance penalties</p>
   * 
   * @param edgeToBreak     edge to break
   * @param vertexToBreakAt the vertex to break at
   * @param geoUtils        required to update edge lengths
   * @return newly created edge due to breaking, null if not feasible
   */
  @Override
  public <Ex extends DirectedEdge> Ex breakEdgeAt(
      DirectedVertex vertexToBreakAt, Ex edgeToBreak, PlanitJtsCrsUtils geoUtils) {

    Ex aToBreak = edgeToBreak;
    Ex breakToB = graphModifier.breakEdgeAt(vertexToBreakAt, edgeToBreak, geoUtils);

    /* update the underlying directed edge segments */
    updateBrokenEdgeItsEdgeSegments(aToBreak, breakToB);

    /* only movements on breakToB need updating */
    if(getGraph().hasMovements()){
      var touchedMovements = getGraph().getMovements().stream().filter(
          m -> m.getCentreVertex().equals(breakToB.getVertexB())).collect(Collectors.toList());
      updateMovementsItsBrokenSegments(aToBreak, breakToB, touchedMovements);
    }

    return breakToB;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <Ex extends DirectedEdge> Ex breakEdgeAt(
      DirectedVertex vertexToBreakAt,
      Ex edgeToBreak,
      Map<? extends DirectedVertex, List<BannedMovement>> movementsByCentreVertex,
      PlanitJtsCrsUtils geoUtils) {

    Ex aToBreak = edgeToBreak;
    Ex breakToB = graphModifier.breakEdgeAt(vertexToBreakAt, edgeToBreak, geoUtils);

    /* update the underlying directed edge segments */
    updateBrokenEdgeItsEdgeSegments(aToBreak, breakToB);

    // use provided mapping to reduce overhead compared to #breakEdgeAt(DirectedVertex, Ex, PlanitJtsCrsUtils)
    var touchedMovements = movementsByCentreVertex.get(breakToB.getVertexB());
    if(touchedMovements!= null) {
      updateMovementsItsBrokenSegments(aToBreak, breakToB, touchedMovements);
    }
    return breakToB;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <Ex extends DirectedEdge> Map<Long, Pair<Ex, Ex>> breakEdgesAt(
      List<Ex> edgesToBreak, DirectedVertex vertexToBreakAt, CoordinateReferenceSystem crs) {

    /* delegate regular breaking of edges */
    Map<Long, Pair<Ex, Ex>> brokenEdges = graphModifier.breakEdgesAt(edgesToBreak, vertexToBreakAt, crs);

    /* update the underlying directed edge segments */
    brokenEdges.entrySet().stream().sorted(Entry.comparingByKey()).forEach(entry -> {
      var aToBreak = entry.getValue().first();
      var breakToB = entry.getValue().second();
      updateBrokenEdgeItsEdgeSegments(aToBreak, breakToB);

      /* only movements on breakToB need updating */
      if(getGraph().hasMovements()){
        var touchedMovements = getGraph().getMovements().stream().filter(
            m -> m.getCentreVertex().equals(breakToB.getVertexB())).collect(Collectors.toList());
        updateMovementsItsBrokenSegments(aToBreak, breakToB, touchedMovements);
      }
    });

    return brokenEdges;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <Ex extends DirectedEdge> Map<Long, Pair<Ex, Ex>> breakEdgesAt(
      List<Ex> edgesToBreak,
      DirectedVertex vertexToBreakAt,
      Map<? extends DirectedVertex, List<BannedMovement>> movementsByCentreVertex,
      CoordinateReferenceSystem crs) {

    PlanitJtsCrsUtils geoUtils = new PlanitJtsCrsUtils(crs);

    Map<Long, Pair<Ex, Ex>> affectedEdges = new TreeMap<>();
    for (Ex edgeToBreak : edgesToBreak) {
      if (affectedEdges.containsKey(edgeToBreak.getId())) {
        LOGGER.severe(String.format("Edge (%s) cannot be broken twice at a single vertex, yet this appears " +
            "to be the case", edgeToBreak.getXmlId()));
      }

      /* break an edge and use pre-indexed movements for fast updating*/
      Ex breakToB = breakEdgeAt(vertexToBreakAt, edgeToBreak, movementsByCentreVertex, geoUtils);
      if (breakToB == null) {
        continue;
      }
      Ex aToBreak = edgeToBreak;
      affectedEdges.put(edgeToBreak.getId(), Pair.of(aToBreak, breakToB));
    }
    return affectedEdges;
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

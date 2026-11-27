package org.goplanit.graph.directed.modifier;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.goplanit.utils.graph.modifier.event.MoveEdgeEndsAtVertexEvent;
import org.goplanit.utils.graph.modifier.event.RemoveEdgeSegmentEvent;
import org.goplanit.utils.graph.modifier.event.RemoveBannedTurnEvent;
import org.goplanit.graph.modifier.GraphModifierImpl;
import org.goplanit.utils.graph.modifier.event.BreakEdgeSegmentEvent;
import org.goplanit.utils.graph.modifier.event.RecreatedDirectedGraphEntitiesManagedIdsEvent;
import org.goplanit.utils.event.Event;
import org.goplanit.utils.event.EventListener;
import org.goplanit.utils.event.EventProducerImpl;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
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
import org.goplanit.utils.network.layer.physical.BannedTurnUtils;
import org.goplanit.utils.containers.ContainerUtils;
import org.goplanit.utils.event.EventType;
import org.goplanit.utils.exceptions.ExceptionUtils;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModifierEventProducer;

/**
 * Directed graph modifier that can change any untyped directed graph. Working on the untyped graph means it need not
 * know the typed containers of vertices, edges and edge segments, so it needs no information on their factories.
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
   * @param touchedBannedTurns to update from segment for any touched movements
   */
  private <Ex extends DirectedEdge> void updateMovementsItsBrokenSegments(
      Ex aToBreak, Ex breakToB, List<BannedTurn> touchedBannedTurns) {

    for(var bannedTurn : touchedBannedTurns){
      /* only a segment of the broken edge moves: a parallel edge between the same two vertices, such as the other
       * carriageway of a divided road, keeps its segment */
      // for from segment, this is the only situation where it is altered (as AToBreak is reused just shortened)
      if(bannedTurn.getSegmentFrom().getParent() == aToBreak &&
          bannedTurn.getSegmentFrom().getUpstreamVertex().equals(aToBreak.getVertexA()) &&
          breakToB.getVertexB().equals(bannedTurn.getSegmentTo().getUpstreamVertex())){
        getGraph().getBannedTurns().update(
            bannedTurn, ban -> ban.setSegmentFrom(breakToB.getSegmentUpstreamOf(breakToB.getVertexB())));
      }

      // for to segment this is the only situations where it is altered
      if(bannedTurn.getSegmentTo().getParent() == aToBreak &&
          bannedTurn.getSegmentTo().getDownstreamVertex().equals(aToBreak.getVertexA()) &&
          breakToB.getVertexB().equals(bannedTurn.getSegmentFrom().getDownstreamVertex())){
        getGraph().getBannedTurns().update(
            bannedTurn, ban -> ban.setSegmentTo(breakToB.getSegmentDownstreamFrom(breakToB.getVertexB())));
      }

    }


    // no event yet since the entity itself is not really changing, may need to reconsider in future
  }

  /**
   * Whether this modifier fires one of the event types a listener supports, and can tell it, as it tells directed
   * graph listeners only
   *
   * @param listener to check
   * @return true when this modifier would register it for one or more types
   */
  private boolean firesSupportedTypeOf(GraphModifierListener listener) {
    return listener instanceof DirectedGraphModifierListener
        && listener.findKnownSupportedEventTypesAmong(super.getFiredEventTypes()).length > 0;
  }

  /**
   * Whether the graph modifier this modifier holds fires one of the event types a listener supports
   *
   * @param listener to check
   * @return true when the graph modifier would register it for one or more types
   */
  private boolean graphModifierFiresSupportedTypeOf(GraphModifierListener listener) {
    return listener.findKnownSupportedEventTypesAmong(graphModifier.getFiredEventTypes()).length > 0;
  }

  /**
   * Refuse a listener neither this modifier nor the graph modifier it holds would register for any type
   *
   * @param listener to check
   * @throws IllegalArgumentException when neither fires a type the listener supports, or could tell it
   */
  private void refuseWhenNoneFired(GraphModifierListener listener) {
    ExceptionUtils.throwIllegalArgumentIf(listener.hasKnownSupportedEventTypes() &&
        !firesSupportedTypeOf(listener) && !graphModifierFiresSupportedTypeOf(listener),
        "Listener %s refused by %s: it supports none of the event types it fires, %s", listener.getClass().getName(),
        getClass().getName(), getFiredEventTypes());
  }

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
   * Constructor.
   * 
   * @param theDirectedGraph to use
   */
  public DirectedGraphModifierImpl(final UntypedDirectedGraph<?, ?, ?> theDirectedGraph) {
    super(DirectedGraphModifierEventProducer.FIRED_DIRECTED_GRAPH_EVENT_TYPES);
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
    removeEdge(edge, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeEdge(DirectedEdge edge, boolean removeEdgeSegments) {
    if(removeEdgeSegments && edge.hasEdgeSegment()){
      /* a copy, so each removal can detach its segment from the edge while traversing */
      edge.getEdgeSegments().forEach(this::removeEdgeSegment);
    }
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
     * portion of each subgraph - edge segments and banned turns - is left behind on the graph */
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
  public void moveEdgeEndsAtVertex(
      Collection<? extends DirectedEdge> edges, DirectedVertex currentEndVertex, DirectedVertex newEndVertex) {
    for (var edge : edges) {
      PlanItRunTimeException.throwIf(edge.getVertexA() != currentEndVertex && edge.getVertexB() != currentEndVertex,
          "Vertex %s is not an end of edge %s", currentEndVertex.getIdsAsString(), edge.getIdsAsString());
    }
    for (var edge : edges) {
      edge.replace(currentEndVertex, newEndVertex);
      currentEndVertex.removeEdge(edge);
      newEndVertex.addEdge(edge);
    }
    if (hasListener(MoveEdgeEndsAtVertexEvent.EVENT_TYPE)) {
      fireEvent(new MoveEdgeEndsAtVertexEvent(this, List.copyOf(edges), currentEndVertex, newEndVertex));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeEdgeSegment(EdgeSegment edgeSegment) {
    /* only a registered segment is removed, so removing it again changes nothing and fires nothing */
    if(!getGraph().getEdgeSegments().containsValue(edgeSegment)){
      return;
    }
    getGraph().getEdgeSegments().remove(edgeSegment.getId());

    if (hasListener(RemoveEdgeSegmentEvent.EVENT_TYPE)) {
      fireEvent(new RemoveEdgeSegmentEvent(this, edgeSegment));
    }

    /* taken off its edge only once the listeners have run, so they can still ask the segment its vertices */
    if(edgeSegment.getParent() != null){
      edgeSegment.getParent().removeEdgeSegment(edgeSegment);
    }
    edgeSegment.removeParentEdge();

    /* banned turns cannot outlive a segment they start or end at */
    if (getGraph().getBannedTurns() != null) {
      getGraph().getBannedTurns().getBySegment(edgeSegment).forEach(this::removeBannedTurn);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeBannedTurn(BannedTurn movement) {
    getGraph().getBannedTurns().remove(movement.getId());
    if(movement.hasSegmentFrom()){
      movement.setSegmentFrom(null);
    }
    if(movement.hasSegmentTo()){
      movement.setSegmentTo(null);
    }

    if (hasListener(RemoveBannedTurnEvent.EVENT_TYPE)) {
      fireEvent(new RemoveBannedTurnEvent(this, movement));
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

    /* supplement with anything the subgraph listed explicitly, which covers segments whose vertices are retained.
     * Resolved from the subgraph's own registration rather than by scanning every segment on the graph, since this
     * runs once per removed subgraph and a full scan would make removal quadratic in the size of the network */
    edgeSegmentsToRemove.addAll(subGraphToRemove.getEdgeSegments(getGraph().getEdgeSegments()));

    /* remove the edge segment portion of the directed subgraph from the actual directed graph and fire event(s),
     * each removal taking the banned turns on its segment with it */
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
    if (getGraph().getBannedTurns() != null) {
      getGraph().getBannedTurns().recreateIds();
      fireEvent(new RecreatedDirectedGraphEntitiesManagedIdsEvent(
          this, getGraph().getBannedTurns()));
    }
  }

  /**
   * Identical to the {@code GraphImpl} implementation except that we now also account for the edge segments
   * present on the edge. Copies of the original edge segments are placed on
   * (vertexToBreakAt,vertexB), while the original ones are retained at (vertexA,vertexToBreakAt).
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
    if (breakToB == null) {
      return null;
    }

    /* update the underlying directed edge segments */
    updateBrokenEdgeItsEdgeSegments(aToBreak, breakToB);

    /* only movements on breakToB need updating */
    if(getGraph().hasBannedTurns()){
      var touchedBannedTurns = BannedTurnUtils.findBannedTurnsOnEdge(getGraph().getBannedTurns(), aToBreak);
      updateMovementsItsBrokenSegments(aToBreak, breakToB, touchedBannedTurns);
    }

    return breakToB;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <Ex extends DirectedEdge> Map<Long, Pair<Ex, Ex>> breakEdgesAt(
      List<Ex> edgesToBreak, DirectedVertex vertexToBreakAt, CoordinateReferenceSystem crs) {

    PlanitJtsCrsUtils geoUtils = new PlanitJtsCrsUtils(crs);

    Map<Long, Pair<Ex, Ex>> affectedEdges = new TreeMap<>();
    for (Ex edgeToBreak : edgesToBreak) {
      if (affectedEdges.containsKey(edgeToBreak.getId())) {
        LOGGER.severe(String.format("Edge (%s) cannot be broken twice at a single vertex, yet this appears " +
            "to be the case", edgeToBreak.getXmlId()));
      }

      /* break an edge, its edge segments and the movements using them */
      Ex breakToB = breakEdgeAt(vertexToBreakAt, edgeToBreak, geoUtils);
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
    removeDirectedSubGraph(DirectedGraphUtils.identifySubGraphForVertex(getGraph(), referenceVertex, e -> true));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    graphModifier.reset();
    removeAllNonInternalListeners();
  }

  /**
   * {@inheritDoc}
   * <p>
   * Each type goes to the one that fires it: directed graph types to this modifier, graph types to the graph modifier
   * it holds. Refused when neither fires a type the listener supports
   * </p>
   */
  @Override
  public void addListener(GraphModifierListener listener) {
    refuseWhenNoneFired(listener);
    if (firesSupportedTypeOf(listener)) {
      super.addListener(listener);
    }
    if (graphModifierFiresSupportedTypeOf(listener) || !listener.hasKnownSupportedEventTypes()) {
      graphModifier.addListener(listener);
    }
  }

  /**
   * {@inheritDoc}
   * <p>
   * To the one that fires the type: this modifier for a directed graph type, which it tells directed graph listeners
   * only, the graph modifier it holds for a graph type. Refused when neither fires it
   * </p>
   */
  @Override
  public void addListener(GraphModifierListener listener, GraphModifierEventType eventType) {
    if (super.getFiredEventTypes().contains(eventType)) {
      ExceptionUtils.throwIllegalArgumentIf(!(listener instanceof DirectedGraphModifierListener),
          "Listener %s refused for event type %s by %s: only a directed graph listener can be told it",
          listener.getClass().getName(), eventType, getClass().getName());
      super.addListener(listener, eventType);
    } else {
      graphModifier.addListener(listener, eventType);
    }
  }

  /**
   * Add a listener internal to the owner of this modifier, for the event types it is known to support, each on the one
   * that fires it, this modifier or the underlying graph modifier, as {@link #addListener(GraphModifierListener)}
   * does. It receives each of these events before the other listeners do, and is not removed with them
   *
   * @param listener to add
   * @throws IllegalArgumentException when neither fires a type the listener supports
   */
  @Override
  public void addInternalListener(GraphModifierListener listener) {
    refuseWhenNoneFired(listener);
    if (firesSupportedTypeOf(listener)) {
      super.addInternalListener(listener);
    }
    if (graphModifierFiresSupportedTypeOf(listener) || !listener.hasKnownSupportedEventTypes()) {
      graphModifier.addInternalListener(listener);
    }
  }

  /**
   * The event types this modifier fires, of both kinds: its own directed graph types and the graph types of the graph
   * modifier it holds
   *
   * @return unmodifiable set of the event types fired
   */
  @Override
  public Set<EventType> getFiredEventTypes() {
    return ContainerUtils.unmodifiableUnionOf(super.getFiredEventTypes(), graphModifier.getFiredEventTypes());
  }

  /**
   * {@inheritDoc}
   * <p>
   * Those of the graph modifier it holds
   * </p>
   */
  @Override
  public Set<GraphModifierEventType> getFiredGraphEventTypes() {
    return graphModifier.getFiredGraphEventTypes();
  }

  /**
   * Remove a listener internal to the owner of this modifier, from this modifier and the underlying graph modifier
   *
   * @param listener to remove
   */
  @Override
  public void removeInternalListener(GraphModifierListener listener) {
    if (listener instanceof DirectedGraphModifierListener) {
      super.removeInternalListener(listener);
    }
    graphModifier.removeInternalListener(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(GraphModifierListener listener, GraphModifierEventType eventType) {
    if (listener instanceof DirectedGraphModifierListener) {
      super.removeListener(listener, eventType);
    }
    graphModifier.removeListener(listener, eventType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(GraphModifierListener listener) {
    if (listener instanceof DirectedGraphModifierListener) {
      super.removeListener(listener);
    }
    graphModifier.removeListener(listener);
  }

  /**
   * Remove all listeners, apart from those internal to the owner of this modifier, from this modifier and the
   * underlying graph modifier
   */
  @Override
  public synchronized void removeAllNonInternalListeners() {
    super.removeAllNonInternalListeners();
    graphModifier.removeAllNonInternalListeners();
  }

}

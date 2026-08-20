package org.goplanit.graph.modifier;

import java.util.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.goplanit.graph.modifier.event.RecreatedGraphEntitiesManagedIdsEvent;
import org.goplanit.graph.modifier.event.*;
import org.goplanit.utils.event.Event;
import org.goplanit.utils.event.EventListener;
import org.goplanit.utils.event.EventProducerImpl;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.graph.*;
import org.goplanit.utils.graph.modifier.GraphModifier;
import org.goplanit.utils.graph.modifier.event.GraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.GraphModifierEventType;
import org.goplanit.utils.graph.modifier.event.GraphModifierListener;
import org.goplanit.utils.id.ManagedIdEntities;
import org.goplanit.utils.misc.Pair;
import org.locationtech.jts.geom.LineString;

/**
 * Apply modifications to the graph in an integrated fashion.
 * <p>
 * While graphs are assumed to have a managed id for all their entities it is not a given that the edges, vertices,
 * etc. are the primary containers where these are uniquely
 * tracked. For example a subgraph might only contain a subset of vertices. Therefore, whenever modifications are
 * made to a graph, the invoker of these changes should be aware
 * whether the graph entities containers are the primary containers or not. If so, and the ids used are
 * expected to remain contiguous (directly) after completion of the
 * modification, additional effort is required by invoking {@link #recreateManagedEntitiesIds()} to ensure that
 * all entity containers on the graph are triggered to perform an
 * update of their internally managed ids.
 * 
 * @author markr
 *
 */
public class GraphModifierImpl<V extends Vertex, E extends Edge>
    extends EventProducerImpl implements GraphModifier<V, E> {

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(GraphModifierImpl.class.getCanonicalName());

  /** the graph to modify */
  protected final UntypedGraph<? extends V, ? extends E> theGraph;

  /**
   * update the geometry of the broken edge, knowing at what vertex it was broken from a previously longer edge
   * 
   * @param brokenEdge     the broken edge
   * @param vertexBrokenAt the vertex it was broken at
   */
  protected static void updateBrokenEdgeGeometry(Edge brokenEdge, Vertex vertexBrokenAt) {
    LineString updatedGeometry = null;
    if (brokenEdge.getVertexA().equals(vertexBrokenAt)) {
      updatedGeometry = PlanitJtsUtils.createCopyWithoutCoordinatesBefore(
          vertexBrokenAt.getPosition(), brokenEdge.getGeometry());
    } else if (brokenEdge.getVertexB().equals(vertexBrokenAt)) {
      updatedGeometry = PlanitJtsUtils.createCopyWithoutCoordinatesAfter(
          vertexBrokenAt.getPosition(), brokenEdge.getGeometry());
    } else {
      LOGGER.warning(String.format("unable to locate vertex to break at (%s) for broken edge %s (id:%d)",
          vertexBrokenAt.getPosition().toString(), brokenEdge.getExternalId(),
          brokenEdge.getId()));
    }
    brokenEdge.setGeometry(updatedGeometry);
  }


  /**
   * Constructor
   * 
   * @param theGraph to use
   */
  public GraphModifierImpl(final UntypedGraph<? extends V, ? extends E> theGraph) {
    super();
    this.theGraph = theGraph;
  }

  /**
   * Access to graph
   *
   * @return the underlying graph
   */
  public UntypedGraph<? extends V,? extends E> getGraph(){
    return theGraph;
  }

  /**
   * Access to the graph at this modifier's own binding rather than as a wildcard.
   * <p>
   * The graph is stored as a wildcard so that any graph whose entities derive from V and E can be wrapped without
   * the caller having to name the exact types. Everything registered on it is a V and an E by construction of the
   * constructor, so narrowing here is safe; it is confined to this one accessor so no caller has to repeat it.
   * </p>
   *
   * @return the underlying graph typed to this modifier
   */
  @SuppressWarnings("unchecked")
  protected UntypedGraph<V,E> getTypedGraph(){
    return (UntypedGraph<V,E>) theGraph;
  }

  /**
   * {@inheritDoc}
   * <p>
   *   make public so derived classes can access it as well
   * </p>
   */
  @Override
  public void fireEvent(EventListener eventListener, Event event) {
    ((GraphModifierListener) eventListener).onGraphModificationEvent((GraphModificationEvent) event);
  }

  /**
   * {@inheritDoc}
   *
   */
  @Override
  public void removeVertex(V vertex) {
    /* remove vertex from vertex' edges */
    for (Edge edge : vertex.getEdges()) {
      edge.removeVertex(vertex);
    }

    /* remove edges from vertex */
    vertex.removeAllEdges();

    /* remove vertex from graph and fire event */
    theGraph.getVertices().remove(vertex.getId());
    if (hasListener(RemoveVertexEvent.EVENT_TYPE)) {
      fireEvent(new RemoveVertexEvent(this, vertex));
    }
  }

  /**
   * {@inheritDoc}
   *
   */
  @Override
  public void removeEdge(E edge) {
    /* remove edge from vertex A */
    if(edge.getVertexA()!= null){
      edge.getVertexA().removeEdge(edge);
    }

    /* remove edge from vertex B */
    if(edge.getVertexB()!= null){
      edge.getVertexB().removeEdge(edge);
    }

    /* remove edge from graph and fire event */
    theGraph.getEdges().remove(edge.getId());
    if (hasListener(RemoveEdgeEvent.EVENT_TYPE)) {
      fireEvent(new RemoveEdgeEvent(this, edge));
    }
  }

  /**i don
   * {@inheritDoc}
   *
   */
  @Override
  public void removeDanglingSubGraphs(Integer belowSize, Integer aboveSize, boolean alwaysKeepLargest) {

    // this is the lenient setup which just checks based on connectivity nothing else from the given vertex
    Predicate<E> alwaysTrue = e -> true;
    removeDanglingSubGraphs(
        belowSize,
        aboveSize,
        alwaysKeepLargest,
        (v -> UndirectedGraphUtils.identifySubGraphForVertex(
            getTypedGraph(),
            v,
            alwaysTrue, // all connected edges are always included
            false)));   // no strict mode, as it does not do anything when all edges are always considered
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public void removeDanglingSubGraphs(
      Integer belowSize,
      Integer aboveSize,
      boolean alwaysKeepLargest,
      Function<V, ? extends UntypedSubGraph<V,E>> identifySubGraphForVertex) {
    removeDanglingSubGraphs(
        belowSize, aboveSize, alwaysKeepLargest, identifySubGraphForVertex, this::removeSubGraph);
  }

  /**
   * Identical to {@link #removeDanglingSubGraphs(Integer, Integer, boolean, Function)} except that the caller
   * supplies how an identified subgraph is to be removed.
   * <p>
   * This exists because this modifier is also used by composition, e.g. the directed modifier delegates its
   * undirected work here. In that setup an unqualified call to removeSubGraph resolves against this instance, not
   * against the composing modifier, so any removal behaviour the composer adds - edge segments, movements - would
   * be skipped. Passing the action in restores that dispatch.
   * </p>
   *
   * @param belowSize         remove subgraphs below the given size
   * @param aboveSize         remove subgraphs above the given size (typically set to maximum value)
   * @param alwaysKeepLargest indicate if the largest of the subgraphs is always to be kept even if it does
   *                          not match the criteria
   * @param identifySubGraphForVertex function that given a starting vertex identifies the connected subgraph
   * @param removeSubGraphAction how to remove an identified subgraph that meets the removal criteria
   */
  public void removeDanglingSubGraphs(
      Integer belowSize,
      Integer aboveSize,
      boolean alwaysKeepLargest,
      Function<V, ? extends UntypedSubGraph<V,E>> identifySubGraphForVertex,
      Consumer<UntypedSubGraph<V,E>> removeSubGraphAction) {

    Map<Integer, LongAdder> removedDanglingNetworksBySize = new HashMap<>();
    var remainingVertices = new HashSet<V>(theGraph.getVertices().size());
    theGraph.getVertices().forEach(remainingVertices::add);
    var identifiedSubGraphs = new HashMap<V, Pair<Integer, UntypedSubGraph<V,E>>>();

    int maxSubNetworkSize = 0 ;
    while (remainingVertices.iterator().hasNext()) {
      /* recursively traverse the subnetwork */
      V referenceVertex = remainingVertices.iterator().next();
      //Set<Vertex> subNetworkVerticesToPopulate = identifySubNetworkForVertex(referenceVertex, testEdge);
      var connectedSubGraph = identifySubGraphForVertex.apply(referenceVertex);

      /* the reference vertex counts as processed even when the identification rules excluded it from its own
       * subgraph. This happens under strict rules, where a vertex with a non-conforming edge is deliberately left
       * out. Without evicting it explicitly it would be selected as reference vertex again on every subsequent
       * iteration and the loop would never terminate */
      remainingVertices.remove(referenceVertex);

      /* tested before materialising the vertices below, because that materialisation scans the entire parent
       * container. When the identification is driven by a criterion most vertices of the graph yield nothing -
       * think pruning rail on a predominantly road network - and paying a full scan for each would make this
       * quadratic in the size of the graph */
      if (connectedSubGraph.getNumberOfVertices() <= 0) {
        continue;
      }

      /* register size and remove subnetwork from remaining nodes */
      var subGraphVertices = connectedSubGraph.getVertices(theGraph.getVertices());
      subGraphVertices.forEach(remainingVertices::remove);

      /* also evict the vertices that were explored but not registered on the subgraph. Under strict rules a vertex
       * bordering non-conforming parts is deliberately left out, yet it has been traversed and must not seed a
       * search of its own: that would rediscover this same subnetwork, or merge it with a neighbouring one, which
       * corrupts the sizes and with them the protection of the largest subnetwork. Edge membership does not depend
       * on vertex membership, so the subgraph's edges still reach every explored vertex */
      for(var subGraphEdge : connectedSubGraph.getEdges(theGraph.getEdges())){
        remainingVertices.remove(subGraphEdge.getVertexA());
        remainingVertices.remove(subGraphEdge.getVertexB());
      }

      maxSubNetworkSize = Math.max(maxSubNetworkSize, subGraphVertices.size());
      identifiedSubGraphs.put(referenceVertex, Pair.of(subGraphVertices.size(),connectedSubGraph));
    }

    if (!identifiedSubGraphs.isEmpty()) {
      /* remove all non-dominating subnetworks */
      for (var entry : identifiedSubGraphs.entrySet()) {
        int subNetworkSize = entry.getValue().first();
        if (subNetworkSize < maxSubNetworkSize || !alwaysKeepLargest) {

          /* not the biggest subnetwork, remove from network if below threshold */
          if (subNetworkSize < belowSize || subNetworkSize > aboveSize) {

            // now remove the subgraph identified earlier
            removeSubGraphAction.accept(entry.getValue().second());

            removedDanglingNetworksBySize.putIfAbsent(subNetworkSize, new LongAdder());
            removedDanglingNetworksBySize.get(subNetworkSize).increment();
          }
        }
      }
      final LongAdder totalCount = new LongAdder();
      removedDanglingNetworksBySize.forEach((size, count) -> {
        totalCount.add(count.longValue());
      });
    } else {
      LOGGER.warning("No networks identified, unable to remove dangling subnetworks");
    }
  }


  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public void removeSubGraph(UntypedSubGraph<V,E> subGraphToRemove) {

    /* collect the vertices to remove up front rather than iterating the graph's own vertex container. Removal
     * mutates that container (see removeVertex), and its iterator is a live view on the backing map, so removing
     * while traversing it risks a ConcurrentModificationException */
    var verticesToRemove = subGraphToRemove.getVertices(getTypedGraph().getVertices());

    /* remove the subnetwork from the actual network */
    for (var vertex : verticesToRemove) {

      /* remove every edge attached to this vertex and fire edge removal event(s). Deliberately not filtered on
       * subgraph membership: an edge cannot outlive its vertices, so any edge touching a vertex being removed has
       * to go too. Filtering on membership would leave an edge that failed the identification criterion registered
       * on the graph with one or both of its vertices nulled out by removeVertex below */
      Set<? extends Edge> vertexEdges = new HashSet<>(vertex.getEdges());
      for (Edge edge : vertexEdges) {
        removeEdge((E) edge);
      }

      removeVertex(vertex);

      /* fire remove subgraph event */
      if (hasListener(RemoveGraphEntityEvent.EVENT_TYPE)) {
        fireEvent(new RemoveGraphEntityEvent(this));
      }

    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeSubGraphOf(V referenceVertex) {
    // no rules, just check on connectivity
    var subNetworkNodesToRemove = UndirectedGraphUtils.identifySubGraphForVertex(
        getTypedGraph(), referenceVertex, x-> true, false);
    removeSubGraph(subNetworkNodesToRemove);
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public <Ex extends E> Map<Long, Pair<Ex, Ex>> breakEdgesAt(
      final List<Ex> edgesToBreak, final V vertexToBreakAt, final CoordinateReferenceSystem crs) {
    PlanitJtsCrsUtils geoUtils = new PlanitJtsCrsUtils(crs);

    Map<Long, Pair<Ex, Ex>> affectedEdges = new TreeMap<>();
    for (Ex edgeToBreak : edgesToBreak) {
      if (affectedEdges.containsKey(edgeToBreak.getId())) {
        LOGGER.severe(String.format("Edge (%s) cannot be broken twice at a single vertex, yet this appears " +
                "to be the case", edgeToBreak.getXmlId()));
      }

      /* break an edge */
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
  @SuppressWarnings("unchecked")
  @Override
  public <Ex extends E> Ex breakEdgeAt(
          final V vertexToBreakAt, final Ex edgeToBreak, final PlanitJtsCrsUtils geoUtils) {
    Ex aToBreak = edgeToBreak;

    /* create unique copy of edge with unique id and register it, do a deep copy to ensure any input properties
     are duplicated (but shallow copy of non-owned entries) */
    Ex breakToB = (Ex) theGraph.getEdges().getFactory().createUniqueDeepCopyOf(edgeToBreak);
    ((GraphEntities<Ex>) theGraph.getEdges()).register(breakToB);

    if (edgeToBreak.getVertexA() == null || edgeToBreak.getVertexB() == null) {
      LOGGER.severe(String.format("unable to break edge since edge to break %s (id:%d) is missing one or " +
          "more vertices", edgeToBreak.getExternalId(), edgeToBreak.getId()));
      return null;
    } else {

      Vertex oldVertexB = edgeToBreak.getVertexB();
      Vertex oldVertexA = edgeToBreak.getVertexA();

      /* replace vertices on edges */
      aToBreak.replace(oldVertexB, vertexToBreakAt);
      breakToB.replace(oldVertexA, vertexToBreakAt);

      /* replace edges on original vertices */
      oldVertexB.replace(edgeToBreak, breakToB, true);
      oldVertexA.replace(edgeToBreak, aToBreak, true);

      /* add edges to new vertex */
      vertexToBreakAt.addEdge(aToBreak);
      vertexToBreakAt.addEdge(breakToB);
    }

    /* broken links geometry must be updated since it links is truncated compared to its original */
    for (Edge brokenEdge : List.of(aToBreak, breakToB)) {
      updateBrokenEdgeGeometry(brokenEdge, vertexToBreakAt);
      brokenEdge.setLengthKm(geoUtils.getDistanceInKilometres(brokenEdge.getGeometry()));
    }

    /* allow listeners to process this break edge occurrence */
    if (hasListener(BreakEdgeEvent.EVENT_TYPE)) {
      fireEvent(new BreakEdgeEvent(this, vertexToBreakAt, aToBreak, breakToB));
    }

    return breakToB;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateManagedEntitiesIds() {
    if (theGraph.getEdges() instanceof ManagedIdEntities<?>) {
      ((ManagedIdEntities<?>) theGraph.getEdges()).recreateIds();
      fireEvent(new RecreatedGraphEntitiesManagedIdsEvent(this, (ManagedIdEntities<?>)theGraph.getEdges()));
    }
    if (theGraph.getVertices() instanceof ManagedIdEntities<?>) {
      ((ManagedIdEntities<?>) theGraph.getVertices()).recreateIds();
      fireEvent(new RecreatedGraphEntitiesManagedIdsEvent(this, (ManagedIdEntities<?>)theGraph.getVertices()));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    super.removeAllListeners();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(GraphModifierListener listener) {
    super.addListener(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(GraphModifierListener listener, GraphModifierEventType eventType) {
    super.addListener(listener, eventType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(GraphModifierListener listener, GraphModifierEventType eventType) {
    super.removeListener(listener, eventType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(GraphModifierListener listener) {
    super.removeListener(listener);
  }

}

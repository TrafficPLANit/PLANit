package org.goplanit.graph.modifier;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Logger;

import org.goplanit.graph.modifier.event.BreakEdgeEvent;
import org.goplanit.graph.modifier.event.RemoveSubGraphEdgeEvent;
import org.goplanit.graph.modifier.event.RemoveSubGraphEvent;
import org.goplanit.graph.modifier.event.RemoveSubGraphVertexEvent;
import org.goplanit.utils.event.Event;
import org.goplanit.utils.event.EventListener;
import org.goplanit.utils.event.EventProducerImpl;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.graph.GraphEntities;
import org.goplanit.utils.graph.UntypedGraph;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.graph.modifier.GraphModifier;
import org.goplanit.utils.graph.modifier.event.GraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.GraphModifierEventType;
import org.goplanit.utils.graph.modifier.event.GraphModifierListener;
import org.goplanit.utils.id.ManagedIdEntities;
import org.goplanit.utils.misc.Pair;
import org.locationtech.jts.geom.LineString;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Apply modifications to the graph in an integrated fashion.
 * <p>
 * While graphs are assumed to have a managed id for all their entities it is not a given that the edges, vertices, etc. are the primary containers where these are uniquely
 * tracked. For example a subgraph might only contain a subset of vertices. Therefore, whenever modifications are made to a graph, the incoker of these changes should be aware
 * whether or not the graph entities containers are the primary containers or not. If so, and the ids used are expected to remain contiguous (directly) after completion of the
 * modification, additional effort is required by invoking {@link #recreateManagedEntitiesIds()} to ensure that all entity containers on the graph are triggered to perform an
 * update of their internally managed ids.
 * 
 * @author markr
 *
 */
public class GraphModifierImpl extends EventProducerImpl implements GraphModifier<Vertex, Edge> {

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(GraphModifierImpl.class.getCanonicalName());

  /** the graph to modify */
  protected final UntypedGraph<?, ?> theGraph;

  /**
   * update the geometry of the broken edge, knowing at what vertex it was broken from a previously longer edge
   * 
   * @param brokenEdge     the broken edge
   * @param vertexBrokenAt the vertex it was broken at
   */
  protected static void updateBrokenEdgeGeometry(Edge brokenEdge, Vertex vertexBrokenAt) {
    LineString updatedGeometry = null;
    if (brokenEdge.getVertexA().equals(vertexBrokenAt)) {
      updatedGeometry = PlanitJtsUtils.createCopyWithoutCoordinatesBefore(vertexBrokenAt.getPosition(), brokenEdge.getGeometry());
    } else if (brokenEdge.getVertexB().equals(vertexBrokenAt)) {
      updatedGeometry = PlanitJtsUtils.createCopyWithoutCoordinatesAfter(vertexBrokenAt.getPosition(), brokenEdge.getGeometry());
    } else {
      LOGGER.warning(String.format("unable to locate vertex to break at (%s) for broken edge %s (id:%d)", vertexBrokenAt.getPosition().toString(), brokenEdge.getExternalId(),
          brokenEdge.getId()));
    }
    brokenEdge.setGeometry(updatedGeometry);
  }

  /**
   * helper function for subnetwork identification (deliberately NOT recursive to avoid stack overflow on large networks)
   * 
   * @param referenceVertex to process
   * @return all vertices in the subnetwork connected to passed in reference vertex
   */
  protected Set<Vertex> processSubNetworkVertex(Vertex referenceVertex) {
    PlanItRunTimeException.throwIfNull(referenceVertex, "provided reference vertex is null when identifying its subnetwork, thisis not allowed");
    Set<Vertex> subNetworkVertices = new HashSet<Vertex>();
    subNetworkVertices.add(referenceVertex);

    Set<Vertex> verticesToExplore = new HashSet<Vertex>();
    verticesToExplore.add(referenceVertex);
    Iterator<Vertex> vertexIter = verticesToExplore.iterator();
    while (vertexIter.hasNext()) {
      /* collect and remove since it is processed */
      Vertex currVertex = vertexIter.next();
      vertexIter.remove();

      /* add newly found vertices to explore, and add then to final subnetwork list as well */
      Collection<? extends Edge> edgesOfCurrVertex = currVertex.getEdges();
      for (Edge currEdge : edgesOfCurrVertex) {
        if (currEdge.getVertexA() != null && currEdge.getVertexA().getId() != currVertex.getId() && !subNetworkVertices.contains(currEdge.getVertexA())) {
          subNetworkVertices.add(currEdge.getVertexA());
          verticesToExplore.add(currEdge.getVertexA());
        } else if (currEdge.getVertexB() != null && currEdge.getVertexB().getId() != currVertex.getId() && !subNetworkVertices.contains(currEdge.getVertexB())) {
          subNetworkVertices.add(currEdge.getVertexB());
          verticesToExplore.add(currEdge.getVertexB());
        }
      }
      /* update iterator */
      vertexIter = verticesToExplore.iterator();
    }
    return subNetworkVertices;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void fireEvent(EventListener eventListener, Event event) {
    GraphModifierListener.class.cast(eventListener).onGraphModificationEvent(GraphModificationEvent.class.cast(event));
  }

  /**
   * Constructor
   * 
   * @param theGraph to use
   */
  public GraphModifierImpl(final UntypedGraph<?, ?> theGraph) {
    super();
    this.theGraph = theGraph;
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public void removeDanglingSubGraphs(Integer belowSize, Integer aboveSize, boolean alwaysKeepLargest) {

    Map<Integer, LongAdder> removedDanglingNetworksBySize = new HashMap<>();
    Set<Vertex> remainingVertices = new HashSet<Vertex>(theGraph.getVertices().size());
    theGraph.getVertices().forEach(vertex -> remainingVertices.add(vertex));
    Map<Vertex, Integer> identifiedSubNetworkSizes = new HashMap<Vertex, Integer>();

    while (remainingVertices.iterator().hasNext()) {
      /* recursively traverse the subnetwork */
      Vertex referenceVertex = remainingVertices.iterator().next();
      Set<Vertex> subNetworkVerticesToPopulate = processSubNetworkVertex(referenceVertex);

      /* register size and remove subnetwork from remaining nodes */
      identifiedSubNetworkSizes.put(referenceVertex, subNetworkVerticesToPopulate.size());
      remainingVertices.removeAll(subNetworkVerticesToPopulate);
    }

    if (!identifiedSubNetworkSizes.isEmpty()) {
      /* remove all non-dominating subnetworks */
      int maxSubNetworkSize = Collections.max(identifiedSubNetworkSizes.values());
      LOGGER.fine(String.format("remaining vertices %d, edges %d", theGraph.getVertices().size(), theGraph.getEdges().size()));
      for (Entry<Vertex, Integer> entry : identifiedSubNetworkSizes.entrySet()) {
        int subNetworkSize = entry.getValue();
        if (subNetworkSize < maxSubNetworkSize || !alwaysKeepLargest) {

          /* not the biggest subnetwork, remove from network if below threshold */
          if (subNetworkSize < belowSize || subNetworkSize > aboveSize) {

            removedDanglingNetworksBySize.putIfAbsent(subNetworkSize, new LongAdder());
            removedDanglingNetworksBySize.get(subNetworkSize).increment();
            LOGGER.fine(String.format("removing %d vertices from graph", subNetworkSize));
            LOGGER.fine(String.format("remaining vertices %d, edges %d", theGraph.getVertices().size(), theGraph.getEdges().size()));
          }
        }
      }
      final LongAdder totalCount = new LongAdder();
      removedDanglingNetworksBySize.forEach((size, count) -> {
        LOGGER.fine(String.format("sub graph size %d - %d removed", size, count.longValue()));
        totalCount.add(count.longValue());
      });
      LOGGER.fine(String.format("removed %d dangling sub graphs", totalCount.longValue()));
    } else {
      LOGGER.warning("no networks identified, unable to remove dangling subnetworks");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeSubGraph(Set<? extends Vertex> subGraphToRemove) {

    /* remove the subnetwork from the actual network */
    for (Vertex vertex : subGraphToRemove) {
      Set<? extends Edge> vertexEdges = new HashSet<>(vertex.getEdges());

      /* remove edges from vertex */
      for (Edge edge : vertexEdges) {
        vertex.removeEdge(edge);
      }

      /* remove vertex from vertex' edges */
      for (Edge edge : vertexEdges) {
        edge.removeVertex(vertex);
      }

      /* remove vertex from graph */
      theGraph.getVertices().remove(vertex.getId());
      if (hasListener(RemoveSubGraphVertexEvent.EVENT_TYPE)) {
        fireEvent(new RemoveSubGraphVertexEvent(this, vertex));
      }

      /* remove vertex' edges from graph */
      for (Edge edge : vertexEdges) {
        theGraph.getEdges().remove(edge.getId());
        if (hasListener(RemoveSubGraphEdgeEvent.EVENT_TYPE)) {
          fireEvent(new RemoveSubGraphEdgeEvent(this, edge));
        }
      }

      if (hasListener(RemoveSubGraphEvent.EVENT_TYPE)) {
        fireEvent(new RemoveSubGraphEvent(this));
      }

    }
  }

  /**
   * {@inheritDoc}
   */
  public void removeSubGraphOf(Vertex referenceVertex) throws PlanItException {
    Set<Vertex> subNetworkNodesToRemove = processSubNetworkVertex(referenceVertex);
    removeSubGraph(subNetworkNodesToRemove);
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public <Ex extends Edge> Map<Long, Pair<Ex, Ex>> breakEdgesAt(final List<Ex> edgesToBreak, final Vertex vertexToBreakAt, final CoordinateReferenceSystem crs) {
    PlanitJtsCrsUtils geoUtils = new PlanitJtsCrsUtils(crs);

    Map<Long, Pair<Ex, Ex>> affectedEdges = new HashMap<Long, Pair<Ex, Ex>>();
    for (Ex edgeToBreak : edgesToBreak) {
      if (affectedEdges.containsKey(edgeToBreak.getId())) {
        LOGGER.severe(String.format("Edge (%s) cannot be broken twice at a single vertex, yet this appears to be the case", edgeToBreak.getXmlId()));
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
  public <Ex extends Edge> Ex breakEdgeAt(final Vertex vertexToBreakAt, final Ex edgeToBreak, final PlanitJtsCrsUtils geoUtils) {
    Ex aToBreak = edgeToBreak;

    /* create copy of edge with unique id and register it */
    Ex breakToB = (Ex) theGraph.getEdges().getFactory().createUniqueCopyOf(edgeToBreak);
    ((GraphEntities<Ex>) theGraph.getEdges()).register((Ex) breakToB);

    if (edgeToBreak.getVertexA() == null || edgeToBreak.getVertexB() == null) {
      LOGGER.severe(String.format("unable to break edge since edge to break %s (id:%d) is missing one or more vertices", edgeToBreak.getExternalId(), edgeToBreak.getId()));
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
    for (Edge brokenEdge : Set.of(aToBreak, breakToB)) {
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
    }
    if (theGraph.getVertices() instanceof ManagedIdEntities<?>) {
      ((ManagedIdEntities<?>) theGraph.getVertices()).recreateIds();
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

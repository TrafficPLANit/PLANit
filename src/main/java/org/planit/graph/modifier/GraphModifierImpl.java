package org.planit.graph.modifier;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Logger;

import org.locationtech.jts.geom.LineString;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.graph.GraphBuilder;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.geo.PlanitJtsCrsUtils;
import org.planit.utils.geo.PlanitJtsUtils;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.Graph;
import org.planit.utils.graph.Vertex;
import org.planit.utils.graph.modifier.BreakEdgeListener;
import org.planit.utils.graph.modifier.GraphModifier;
import org.planit.utils.graph.modifier.RemoveSubGraphListener;

public class GraphModifierImpl<V extends Vertex, E extends Edge> implements GraphModifier<V, E> {

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(GraphModifierImpl.class.getCanonicalName());

  /** the graph to modify */
  protected final Graph<V, E> theGraph;

  /** the graphbuilder related to the graph */
  protected final GraphBuilder<V, E> theGraphBuilder;

  /** track removeSubGraphListeners */
  protected final Set<RemoveSubGraphListener<V, E>> registeredRemoveSubGraphListeners;

  /** track breakEdgeListeners */
  protected final Set<BreakEdgeListener<V, E>> registeredBreakEdgeListeners;

  /**
   * update the geometry of the broken edge, knowing at what vertex it was broken from a previously longer edge
   * 
   * @param <V> type of vertex
   * @param <E> type of edge
   * @param brokenEdge     the broken edge
   * @param vertexBrokenAt the vertex it was broken at
   * @throws PlanItException thrown if error
   */
  protected static <V extends Vertex, E extends Edge> void updateBrokenEdgeGeometry(E brokenEdge, V vertexBrokenAt) throws PlanItException {
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
   * @throws PlanItException thrown if parameters are null
   */
  @SuppressWarnings("unchecked")
  protected Set<V> processSubNetworkVertex(V referenceVertex) throws PlanItException {
    PlanItException.throwIfNull(referenceVertex, "provided reference vertex is null when identifying its subnetwork, thisis not allowed");
    Set<V> subNetworkVertices = new HashSet<>();
    subNetworkVertices.add(referenceVertex);

    Set<V> verticesToExplore = new HashSet<>();
    verticesToExplore.add(referenceVertex);
    Iterator<V> vertexIter = verticesToExplore.iterator();
    while (vertexIter.hasNext()) {
      /* collect and remove since it is processed */
      V currVertex = vertexIter.next();
      vertexIter.remove();

      /* add newly found vertices to explore, and add then to final subnetwork list as well */
      Collection<? extends Edge> edgesOfCurrVertex = currVertex.getEdges();
      for (Edge currEdge : edgesOfCurrVertex) {
        if (currEdge.getVertexA() != null && currEdge.getVertexA().getId() != currVertex.getId() && !subNetworkVertices.contains(currEdge.getVertexA())) {
          subNetworkVertices.add((V) currEdge.getVertexA());
          verticesToExplore.add((V) currEdge.getVertexA());
        } else if (currEdge.getVertexB() != null && currEdge.getVertexB().getId() != currVertex.getId() && !subNetworkVertices.contains(currEdge.getVertexB())) {
          subNetworkVertices.add((V) currEdge.getVertexB());
          verticesToExplore.add((V) currEdge.getVertexB());
        }
      }
      /* update iterator */
      vertexIter = verticesToExplore.iterator();
    }
    return subNetworkVertices;
  }

  /**
   * Constructor
   * 
   * @param theGraph to use
   * @param graphBuilder to use
   */
  public GraphModifierImpl(final Graph<V, E> theGraph, final GraphBuilder<V, E> graphBuilder) {
    this.theGraph = theGraph;
    this.theGraphBuilder = graphBuilder;
    this.registeredRemoveSubGraphListeners = new TreeSet<RemoveSubGraphListener<V, E>>();
    this.registeredBreakEdgeListeners = new TreeSet<BreakEdgeListener<V, E>>();
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public void removeDanglingSubGraphs(Integer belowsize, Integer abovesize, boolean alwaysKeepLargest) throws PlanItException {
    boolean recreateIdsImmediately = false;

    Map<Integer, LongAdder> removedDanglingNetworksBySize = new HashMap<>();
    Set<V> remainingVertices = new HashSet<V>(theGraph.getVertices().size());
    theGraph.getVertices().forEach(vertex -> remainingVertices.add(vertex));
    Map<V, Integer> identifiedSubNetworkSizes = new HashMap<V, Integer>();

    while (remainingVertices.iterator().hasNext()) {
      /* recursively traverse the subnetwork */
      V referenceVertex = remainingVertices.iterator().next();
      Set<V> subNetworkVerticesToPopulate = processSubNetworkVertex(referenceVertex);

      /* register size and remove subnetwork from remaining nodes */
      identifiedSubNetworkSizes.put(referenceVertex, subNetworkVerticesToPopulate.size());
      remainingVertices.removeAll(subNetworkVerticesToPopulate);
    }

    if (!identifiedSubNetworkSizes.isEmpty()) {
      /* remove all non-dominating subnetworks */
      int maxSubNetworkSize = Collections.max(identifiedSubNetworkSizes.values());
      LOGGER.fine(String.format("remaining vertices %d, edges %d", theGraph.getVertices().size(), theGraph.getEdges().size()));
      for (Entry<V, Integer> entry : identifiedSubNetworkSizes.entrySet()) {
        int subNetworkSize = entry.getValue();
        if (subNetworkSize < maxSubNetworkSize || !alwaysKeepLargest) {

          /* not the biggest subnetwork, remove from network if below threshold */
          if (subNetworkSize < belowsize || subNetworkSize > abovesize) {

            removeSubGraphOf(entry.getKey(), recreateIdsImmediately);
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

    /* only recreate ids once after all subnetworks have been removed */
    if (!recreateIdsImmediately) {
      recreateIds();
    }

  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public void removeSubGraph(Set<? extends V> subGraphToRemove, boolean recreateIds) {

    /* remove the subnetwork from the actual network */
    for (V vertex : subGraphToRemove) {
      Set<Edge> vertexEdges = new HashSet<Edge>(vertex.getEdges());

      /* remove edges from vertex */
      for (Edge edge : vertexEdges) {
        vertex.removeEdge(edge);
      }

      /* remove vertex from vertex' edges */
      for (Edge edge : vertexEdges) {
        edge.removeVertex(vertex);
      }

      /* remove vertex from graph */
      theGraph.getVertices().remove(vertex);
      if (!registeredRemoveSubGraphListeners.isEmpty()) {
        for (RemoveSubGraphListener<V, E> listener : registeredRemoveSubGraphListeners) {
          listener.onRemoveSubGraphVertex(vertex);
        }
      }

      /* remove vertex' edges from graph */
      for (Edge edge : vertexEdges) {
        theGraph.getEdges().remove((E) edge);
        if (!registeredRemoveSubGraphListeners.isEmpty()) {
          for (RemoveSubGraphListener<V, E> listener : registeredRemoveSubGraphListeners) {
            listener.onRemoveSubGraphEdge((E) edge);
          }
        }
      }
    }

    if (recreateIds) {
      /* ensure no id gaps remain after the removal of internal entities */
      recreateIds();
    }

    if (!registeredRemoveSubGraphListeners.isEmpty()) {
      for (RemoveSubGraphListener<V, E> listener : registeredRemoveSubGraphListeners) {
        listener.onCompletion();
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public void removeSubGraphOf(V referenceVertex, boolean recreateIds) throws PlanItException {
    Set<V> subNetworkNodesToRemove = processSubNetworkVertex(referenceVertex);
    removeSubGraph(subNetworkNodesToRemove, recreateIds);
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public Map<Long, Set<E>> breakEdgesAt(List<? extends E> edgesToBreak, V vertexToBreakAt, CoordinateReferenceSystem crs) throws PlanItException {
    PlanitJtsCrsUtils geoUtils = new PlanitJtsCrsUtils(crs);

    Map<Long, Set<E>> affectedEdges = new HashMap<Long, Set<E>>();
    for (E edgeToBreak : edgesToBreak) {
      affectedEdges.putIfAbsent(edgeToBreak.getId(), new HashSet<E>());

      Set<E> affectedEdgesOfEdgeToBreak = affectedEdges.get(edgeToBreak.getId());
      E aToBreak = edgeToBreak;
      /* create copy of edge with unique id and register it */
      E breakToB = theGraph.getEdges().registerUniqueCopyOf(edgeToBreak);

      if (edgeToBreak.getVertexA() == null || edgeToBreak.getVertexB() == null) {
        LOGGER.severe(String.format("unable to break edge since edge to break %s (id:%d) is missing one or more vertices", edgeToBreak.getExternalId(), edgeToBreak.getId()));
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

        affectedEdgesOfEdgeToBreak.add(aToBreak);
        affectedEdgesOfEdgeToBreak.add(breakToB);
      }

      /* broken links geometry must be updated since it links is truncated compared to its original */
      for (E brokenEdge : Set.of(aToBreak, breakToB)) {
        updateBrokenEdgeGeometry(brokenEdge, vertexToBreakAt);
        brokenEdge.setLengthKm(geoUtils.getDistanceInKilometres(brokenEdge.getGeometry()));
      }

      if (!registeredBreakEdgeListeners.isEmpty()) {
        for (BreakEdgeListener<V, E> listener : registeredBreakEdgeListeners) {
          listener.onBreakEdge(vertexToBreakAt, aToBreak, breakToB);
        }
      }
    }
    return affectedEdges;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateIds() {
    theGraphBuilder.recreateIds(theGraph.getEdges());
    theGraphBuilder.recreateIds(theGraph.getVertices());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void registerRemoveSubGraphListener(RemoveSubGraphListener<V, E> listener) {
    registeredRemoveSubGraphListeners.add(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unregisterRemoveSubGraphListener(RemoveSubGraphListener<V, E> listener) {
    registeredRemoveSubGraphListeners.remove(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unregisterBreakEdgeListener(BreakEdgeListener<V, E> listener) {
    registeredBreakEdgeListeners.remove(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void registerBreakEdgeListener(BreakEdgeListener<V, E> listener) {
    registeredBreakEdgeListeners.add(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    registeredRemoveSubGraphListeners.clear();
    registeredBreakEdgeListeners.clear();
  }

}

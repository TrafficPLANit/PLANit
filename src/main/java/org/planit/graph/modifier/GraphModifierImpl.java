package org.planit.graph.modifier;

import java.io.Serializable;
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

import org.djutils.event.EventProducer;
import org.locationtech.jts.geom.LineString;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.geo.PlanitJtsCrsUtils;
import org.planit.utils.geo.PlanitJtsUtils;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.UntypedGraph;
import org.planit.utils.graph.Vertex;
import org.planit.utils.graph.modifier.BreakEdgeListener;
import org.planit.utils.graph.modifier.GraphModifier;
import org.planit.utils.graph.modifier.RemoveSubGraphListener;

/**
 * Apply modifications to the graph in an integrated fashion
 * 
 * @author markr
 *
 */
public class GraphModifierImpl extends EventProducer implements GraphModifier<Vertex, Edge> {

  /**
   * generated UID
   */
  private static final long serialVersionUID = -3354874978792212421L;

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(GraphModifierImpl.class.getCanonicalName());

  /** the graph to modify */
  protected final UntypedGraph<?, ?> theGraph;

  /** track removeSubGraphListeners */
  protected final Set<RemoveSubGraphListener> registeredRemoveSubGraphListeners;

  /** track breakEdgeListeners */
  protected final Set<BreakEdgeListener> registeredBreakEdgeListeners;

  /**
   * update the geometry of the broken edge, knowing at what vertex it was broken from a previously longer edge
   * 
   * @param <V>            type of vertex
   * @param <E>            type of edge
   * @param brokenEdge     the broken edge
   * @param vertexBrokenAt the vertex it was broken at
   * @throws PlanItException thrown if error
   */
  protected static void updateBrokenEdgeGeometry(Edge brokenEdge, Vertex vertexBrokenAt) throws PlanItException {
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
  protected Set<Vertex> processSubNetworkVertex(Vertex referenceVertex) throws PlanItException {
    PlanItException.throwIfNull(referenceVertex, "provided reference vertex is null when identifying its subnetwork, thisis not allowed");
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
   * Constructor
   * 
   * @param theGraph to use
   */
  public GraphModifierImpl(final UntypedGraph<?, ?> theGraph) {
    this.theGraph = theGraph;
    this.registeredRemoveSubGraphListeners = new TreeSet<RemoveSubGraphListener>();
    this.registeredBreakEdgeListeners = new TreeSet<BreakEdgeListener>();
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public void removeDanglingSubGraphs(Integer belowSize, Integer aboveSize, boolean alwaysKeepLargest) throws PlanItException {
    boolean recreateIdsImmediately = false;

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
  @Override
  public void removeSubGraph(Set<? extends Vertex> subGraphToRemove, boolean recreateIds) {

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
      if (!registeredRemoveSubGraphListeners.isEmpty()) {
        for (RemoveSubGraphListener listener : registeredRemoveSubGraphListeners) {
          listener.onRemoveSubGraphVertex(vertex);
        }
      }

      /* remove vertex' edges from graph */
      for (Edge edge : vertexEdges) {
        theGraph.getEdges().remove(edge.getId());
        if (!registeredRemoveSubGraphListeners.isEmpty()) {
          for (RemoveSubGraphListener listener : registeredRemoveSubGraphListeners) {
            listener.onRemoveSubGraphEdge(edge);
          }
        }
      }
    }

    if (recreateIds) {
      /* ensure no id gaps remain after the removal of internal entities */
      recreateIds();
    }

    if (!registeredRemoveSubGraphListeners.isEmpty()) {
      for (RemoveSubGraphListener listener : registeredRemoveSubGraphListeners) {
        listener.onCompletion();
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public void removeSubGraphOf(Vertex referenceVertex, boolean recreateIds) throws PlanItException {
    Set<Vertex> subNetworkNodesToRemove = processSubNetworkVertex(referenceVertex);
    removeSubGraph(subNetworkNodesToRemove, recreateIds);
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public <Ex extends Edge> Map<Long, Set<Ex>> breakEdgesAt(List<Ex> edgesToBreak, Vertex vertexToBreakAt, CoordinateReferenceSystem crs) throws PlanItException {
    PlanitJtsCrsUtils geoUtils = new PlanitJtsCrsUtils(crs);

    Map<Long, Set<Ex>> affectedEdges = new HashMap<Long, Set<Ex>>();
    for (Ex edgeToBreak : edgesToBreak) {
      affectedEdges.putIfAbsent(edgeToBreak.getId(), new HashSet<Ex>());

      Set<Ex> affectedEdgesOfEdgeToBreak = affectedEdges.get(edgeToBreak.getId());
      Ex aToBreak = edgeToBreak;

      /* create copy of edge with unique id and register it */
      @SuppressWarnings("unchecked")
      Ex breakToB = (Ex) theGraph.getEdges().getFactory().registerUniqueCopyOf(edgeToBreak);

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
      for (Edge brokenEdge : Set.of(aToBreak, breakToB)) {
        updateBrokenEdgeGeometry(brokenEdge, vertexToBreakAt);
        brokenEdge.setLengthKm(geoUtils.getDistanceInKilometres(brokenEdge.getGeometry()));
      }

      /* allow listeners to process this break edge occurrence */
      // TODO: replace current listener approach with true event based approach. Requires our events to be able to be registered
      // by source rather than globally as is currently the case to be able to localise the calls
      // fireEvent(new BreakEdgeEvent(this, vertexToBreakAt, aToBreak, breakToB));

      if (!registeredBreakEdgeListeners.isEmpty()) {
        for (BreakEdgeListener listener : registeredBreakEdgeListeners) {
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
    theGraph.getEdges().recreateIds();
    theGraph.getVertices().recreateIds();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void registerRemoveSubGraphListener(RemoveSubGraphListener listener) {
    registeredRemoveSubGraphListeners.add(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unregisterRemoveSubGraphListener(RemoveSubGraphListener listener) {
    registeredRemoveSubGraphListeners.remove(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unregisterBreakEdgeListener(BreakEdgeListener listener) {
    registeredBreakEdgeListeners.remove(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void registerBreakEdgeListener(BreakEdgeListener listener) {
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

  /**
   * {@inheritDoc}
   */
  @Override
  public Serializable getSourceId() {
    return this;
  }

}

package org.planit.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.Edges;
import org.planit.utils.graph.Graph;
import org.planit.utils.graph.Vertex;
import org.planit.utils.graph.Vertices;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;

/**
 * 
 * A graph implementation consisting of vertices and edges
 * 
 * @author markr
 *
 */
public class GraphImpl<V extends Vertex, E extends Edge> implements Graph<V, E>, GraphModifier<V, E> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(GraphImpl.class.getCanonicalName());

  /**
   * The id of this graph
   */
  private final long id;

  // Protected

  /**
   * Graph builder responsible for constructing all graph related (derived) instances
   */
  protected final GraphBuilder<V, E> graphBuilder;

  // PUBLIC

  /**
   * class instance containing all edges
   */
  protected final Edges<E> edges;

  /**
   * class instance containing all vertices
   */
  protected final Vertices<V> vertices;

  /**
   * helper function for recursive subnetwork identification
   * 
   * @param referenceVertex    to process
   * @param subNetworkVertices to add connected adjacent nodes to
   */
  @SuppressWarnings("unchecked")
  protected void processSubNetworkVertex(V referenceVertex, Set<V> subNetworkVertices) {
    if (!subNetworkVertices.contains(referenceVertex)) {
      subNetworkVertices.add(referenceVertex);

      Collection<? extends Edge> edges = referenceVertex.getEdges();
      for (Edge edge : edges) {
        if (!subNetworkVertices.contains(edge.getVertexA())) {
          processSubNetworkVertex((V) edge.getVertexA(), subNetworkVertices);
        }
        if (!subNetworkVertices.contains(edge.getVertexB())) {
          processSubNetworkVertex((V) edge.getVertexB(), subNetworkVertices);
        }
      }
    }
  }

  /**
   * Graph Constructor
   *
   * @param groupId        contiguous id generation within this group for instances of this class
   * @param networkBuilder the builder to be used to create this network
   */
  public GraphImpl(final IdGroupingToken groupId, final GraphBuilder<V, E> graphBuilder) {
    this.id = IdGenerator.generateId(groupId, GraphImpl.class);
    this.graphBuilder = graphBuilder;

    IdGroupingToken groupToken = IdGenerator.createIdGroupingToken(this, this.getId());
    this.graphBuilder.setIdGroupingToken(groupToken);

    this.edges = new EdgesImpl<V, E>(graphBuilder);
    this.vertices = new VerticesImpl<V>(graphBuilder);
  }

  // Getters - Setters

  /**
   * {@inheritDoc}
   */
  @Override
  public long getId() {
    return this.id;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Vertices<V> getVertices() {
    return vertices;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Edges<E> getEdges() {
    return edges;
  }

  /**
   * Collect the id grouping token used for all entities registered on the graph, i.e., this network's specific identifier for generating ids unique and contiguous within this
   * network and this network only
   * 
   * @return the graph id grouping token
   */
  public IdGroupingToken getGraphIdGroupingToken() {
    return this.graphBuilder.getIdGroupingToken();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeDanglingSubGraphs(Integer belowsize) {
    List<Integer> removedSubnetworksOfSize = new ArrayList<Integer>();

    Set<V> remainingVertices = new HashSet<V>(getVertices().size());
    getVertices().forEach(vertex -> remainingVertices.add(vertex));
    Map<V, Integer> identifiedSubNetworkSizes = new HashMap<V, Integer>();

    while (remainingVertices.iterator().hasNext()) {
      /* recursively traverse the subnetwork */
      V referenceVertex = remainingVertices.iterator().next();
      Set<V> subNetworkVerticesToPopulate = new HashSet<V>();
      processSubNetworkVertex(referenceVertex, subNetworkVerticesToPopulate);

      /* register size and remove subnetwork from remaining nodes */
      identifiedSubNetworkSizes.put(referenceVertex, subNetworkVerticesToPopulate.size());
      remainingVertices.removeAll(subNetworkVerticesToPopulate);
    }

    if (!identifiedSubNetworkSizes.isEmpty()) {
      /* remove all non-dominating subnetworks */
      int maxSubNetworkSize = Collections.max(identifiedSubNetworkSizes.values());
      LOGGER.info(String.format("Main network contains %d vertices", maxSubNetworkSize));
      LOGGER.fine(String.format("remaining vertices %d, edges %d", getVertices().size(), getEdges().size()));
      for (Entry<V, Integer> entry : identifiedSubNetworkSizes.entrySet()) {
        int subNetworkSize = entry.getValue();
        if (maxSubNetworkSize > subNetworkSize) {
          /* not the biggest subnetwork, remove from network if below threshold */
          if (subNetworkSize < belowsize) {
            removeSubGraphOf(entry.getKey());
            removedSubnetworksOfSize.add(subNetworkSize);
            LOGGER.fine(String.format("removing %d vertices from graph", subNetworkSize));
            LOGGER.fine(String.format("remaining vertices %d, edges %d", getVertices().size(), getEdges().size()));
          }
        }
      }

      if (belowsize == Integer.MAX_VALUE) {
        LOGGER.info(String.format("removed %d dangling sub graphs", removedSubnetworksOfSize.size()));
      } else {
        LOGGER.info(String.format("removed %d dangling sub graphs of size %d or less", removedSubnetworksOfSize.size(), belowsize));
      }
    } else {
      LOGGER.warning("no networks identified, unable to remove dangling subnetworks");
    }

  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public void removeSubGraph(Set<? extends V> subGraphToRemove) {

    /* remove the subnetwork from the actual network */
    for (V vertex : subGraphToRemove) {
      Set<Edge> vertexEdges = new HashSet<Edge>(vertex.getEdges());

      /* remove edges from vertex */
      vertexEdges.forEach(edge -> vertex.removeEdge(edge));

      /* remove vertex from vertex' edges */
      vertexEdges.forEach(edge -> edge.removeVertex(vertex));

      /* remove vertex from graph */
      getVertices().remove(vertex);
      /* remove vertex' edges from graph */
      vertexEdges.forEach(edge -> getEdges().remove((E) edge));
    }

    /* ensure no id gaps remain after the removal of internal entities */
    graphBuilder.recreateIds(getEdges());
    graphBuilder.recreateIds(getVertices());
  }

  /**
   * {@inheritDoc}
   */
  public void removeSubGraphOf(V referenceVertex) {
    Set<V> subNetworkNodesToRemove = new HashSet<V>();
    processSubNetworkVertex(referenceVertex, subNetworkNodesToRemove);
    removeSubGraph(subNetworkNodesToRemove);
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public void breakEdgesAt(List<? extends E> edgesToBreak, V vertexToBreakAt) throws PlanItException {
    for (E edgeToBreak : edgesToBreak) {
      E aToBreak = edgeToBreak;
      /* create copy of edge */
      E breakToB = this.edges.createCopy(edgeToBreak);
      /* update connections to vertices, and other components */
      aToBreak.replace(edgeToBreak.getVertexB(), vertexToBreakAt, true);
      breakToB.replace(edgeToBreak.getVertexA(), vertexToBreakAt, true);
    }
  }

}

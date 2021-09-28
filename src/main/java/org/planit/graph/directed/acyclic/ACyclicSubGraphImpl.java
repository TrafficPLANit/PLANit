package org.planit.graph.directed.acyclic;

import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Logger;

import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.directed.DirectedVertex;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;

/**
 * 
 * An acyclic sub graph contains a subset of the full graph without cycles. The active subset of the graph is tracked by explicitly registering edge segments. Edge segments are by
 * definition directed.
 * 
 * Whenever edge segments are added it is verified that no cycles are created. Also each edge segment that is added must connect to the existing subgraph's contents
 * 
 * 
 * @author markr
 *
 */
public class ACyclicSubGraphImpl implements ACyclicSubGraph {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(ACyclicSubGraphImpl.class.getCanonicalName());

  /**
   * The id of this acyclic sub graph
   */
  private final long id;

  /**
   * root of the sub graph
   */
  DirectedVertex root;

  /**
   * track data for the vertices used in this acyclic graph, mainly used to enable topological sorting
   * 
   * TODO: candidate to be generated based on registered link segments and provide as parameter to methods requiring it. This would allow one the option to not store this but
   * generate on-the-fly trading-off memory vs computational speed. Now we always have this in memory which is costly
   */
  private Map<DirectedVertex, AcyclicVertexData> vertexData;

  /** track the link segments used via a bit set, where 1 at index indicates the link segment with id=index is included */
  private BitSet registeredLinkSegments;

  /**
   * reset vertex data pre and post indices
   */
  private void resetVertexData() {
    for (AcyclicVertexData vertexData : this.vertexData.values()) {
      vertexData.postVisitIndex = 0;
      vertexData.preVisitIndex = 0;
    }
  }

  /**
   * Remove from registered vertices
   * 
   * @param vertex to remove data for
   */
  private void removeVertexData(DirectedVertex vertex) {
    vertexData.remove(vertex);
  }

  /**
   * Check if vertex is connected to any edge segment registered on this subgraph
   * 
   * @param vertex to check
   * @return true when connected, false otherwise
   */
  private boolean isConnectedToAnySubgraphEdgeSegment(DirectedVertex vertex) {
    for (EdgeSegment edgeSegment : vertex.getExitEdgeSegments()) {
      if (containsEdgeSegment(edgeSegment)) {
        return true;
      }
    }
    for (EdgeSegment edgeSegment : vertex.getEntryEdgeSegments()) {
      if (containsEdgeSegment(edgeSegment)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Traverse the graph recursively with the purposes of sorting it topologically
   * 
   * @param vertexIndex      current index we are at
   * @param visited          track which vertices have been visited
   * @param topologicalOrder the list of vertices to populate in topological order
   * 
   * @return true when acyclic, false otherwise
   */
  private boolean traverseRecursively(DirectedVertex vertex, BitSet visited, LongAdder counter, Deque<DirectedVertex> topologicalOrder) {
    visited.set((int) vertex.getId());

    AcyclicVertexData vertexData = getVertexData(vertex);
    preVisit(vertexData, counter);

    boolean isAcyclic = true;
    for (EdgeSegment exitEdgeSegment : vertex.getExitEdgeSegments()) {
      if (containsEdgeSegment(exitEdgeSegment)) {
        DirectedVertex downstreamVertex = exitEdgeSegment.getDownstreamVertex();
        AcyclicVertexData downstreamVertexData = getVertexData(downstreamVertex);
        if (downstreamVertexData.preVisitIndex == 0) {
          /* valid so far, not yet explored at all, proceed */
          isAcyclic = traverseRecursively(downstreamVertex, visited, counter, topologicalOrder);
        } else if (downstreamVertexData.postVisitIndex == 0) {
          /*
           * not valid, when already visited before, then it must have been fully explored, if not it means that this vertex being expanded originates from this (not fully
           * exhausted) downstream vertex and it ends up at the starting point again (current downstream vertex) -> cycle, not a DAG
           */
          isAcyclic = false;
          LOGGER.warning(String.format("Cycle detected in supposed acyclic graph at vertex %s, terminating", downstreamVertex.getXmlId()));
        } /*
           * else { do nothing, valid but downstream vertex already exhausted, so no need to explore further }
           */

        if (!isAcyclic) {
          return isAcyclic;
        }
      }
    }
    postVisit(vertexData, counter);
    /* off the "being processed" stack, add to topological order list as no "earlier" vertices remain */
    topologicalOrder.push(vertex);

    return isAcyclic;
  }

  /**
   * While traversing the graph recursively, postVisit is invoked AFTER exploring a vertex (successfully). In this implementation, the preVisit simply increments the counter and
   * updates the postVisit variable on the vertex data. See also Gupta et al. 2008
   * 
   * @param vertexData data of the vertex
   * @param counter    track the progress of traversing the graph, increment by one
   */
  protected void postVisit(AcyclicVertexData vertexData, LongAdder counter) {
    vertexData.postVisitIndex = counter.intValue();
    counter.increment();
  }

  /**
   * While traversing the graph recursively, preVisit is invoked BEFORE exploring a vertex further. In this implementation, the preVisit simply increments the counter and updates
   * the preVisit variable on the vertex data. See also Gupta et al. 2008
   * 
   * @param vertexData data of the vertex
   * @param counter    track the progress of traversing the graph, increment by one
   */
  protected void preVisit(AcyclicVertexData vertexData, LongAdder counter) {
    vertexData.preVisitIndex = counter.intValue();
    counter.increment();
  }

  /**
   * Collect vertex data for given vertex
   * 
   * @param vertex to collect for
   * @return vertex data
   */
  protected AcyclicVertexData getVertexData(DirectedVertex vertex) {
    return this.vertexData.get(vertex);
  }

  /**
   * Constructor
   * 
   * @param groupId                    generate id based on the group it resides in
   * @param numberOfParentEdgeSegments number of directed edge segments of the parent this subgraph is a subset from
   * @param root                       (initial) root of the subgraph
   */
  public ACyclicSubGraphImpl(final IdGroupingToken groupId, int numberOfParentEdgeSegments, DirectedVertex root) {
    this.id = IdGenerator.generateId(groupId, ACyclicSubGraph.class);
    this.root = root;
    this.vertexData = new HashMap<DirectedVertex, AcyclicVertexData>();
    this.registeredLinkSegments = new BitSet(numberOfParentEdgeSegments);
  }

  /**
   * Copy constructor
   * 
   * @param aCyclicSubGraphImpl to copy
   */
  public ACyclicSubGraphImpl(ACyclicSubGraphImpl aCyclicSubGraphImpl) {
    this.id = aCyclicSubGraphImpl.getId();
    this.root = aCyclicSubGraphImpl.getRootVertex();
    this.registeredLinkSegments = BitSet.valueOf(aCyclicSubGraphImpl.registeredLinkSegments.toByteArray());
    this.vertexData = new HashMap<DirectedVertex, AcyclicVertexData>();
    aCyclicSubGraphImpl.vertexData.forEach((v, d) -> this.vertexData.put(v, d.clone()));
  }

  /**
   * Perform topological sorting from root, based on Gupta et al. 2008.
   * 
   * @return Topologically sorted list of vertices, null when graph is not acyclic, or disconnected
   */
  @Override
  public Collection<DirectedVertex> topologicalSort() {

    ArrayDeque<DirectedVertex> topologicalOrder = new ArrayDeque<DirectedVertex>(vertexData.size());
    resetVertexData();

    BitSet visited = new BitSet(vertexData.size());
    LongAdder counter = new LongAdder();
    counter.increment();
    boolean isAcyclic = traverseRecursively(root, visited, counter, topologicalOrder);

    if (!isAcyclic) {
      return null;
    } else {
      for (Entry<DirectedVertex, AcyclicVertexData> vertexEntry : this.vertexData.entrySet()) {
        if (!visited.get((int) vertexEntry.getKey().getId())) {
          LOGGER.warning(String.format("Topological sort applied, but some vertices not connected to the root (%s) of the acyclic graph (%d), unable to determine sorting order",
              root.getXmlId(), getId()));
          return null;
        }
      }
    }

    return topologicalOrder;
  }

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
  public DirectedVertex getRootVertex() {
    return root;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addEdgeSegment(EdgeSegment edgeSegment) {
    registeredLinkSegments.set((int) edgeSegment.getId());
    if (!vertexData.containsKey(edgeSegment.getUpstreamVertex())) {
      vertexData.put(edgeSegment.getUpstreamVertex(), new AcyclicVertexData());
    }
    if (!vertexData.containsKey(edgeSegment.getDownstreamVertex())) {
      vertexData.put(edgeSegment.getDownstreamVertex(), new AcyclicVertexData());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean containsEdgeSegment(EdgeSegment edgeSegment) {
    return registeredLinkSegments.get((int) edgeSegment.getId());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getNumberOfVertices() {
    return vertexData.size();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeEdgeSegment(EdgeSegment edgeSegment) {
    registeredLinkSegments.set((int) edgeSegment.getId(), false);
    if (!isConnectedToAnySubgraphEdgeSegment(edgeSegment.getDownstreamVertex())) {
      removeVertexData(edgeSegment.getDownstreamVertex());
    }
    if (!isConnectedToAnySubgraphEdgeSegment(edgeSegment.getUpstreamVertex())) {
      removeVertexData(edgeSegment.getUpstreamVertex());
    }

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ACyclicSubGraphImpl clone() {
    return new ACyclicSubGraphImpl(this);
  }

}

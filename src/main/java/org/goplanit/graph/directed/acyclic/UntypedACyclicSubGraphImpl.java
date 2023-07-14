package org.goplanit.graph.directed.acyclic;

import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Logger;

import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.graph.directed.acyclic.ACyclicSubGraph;
import org.goplanit.utils.graph.directed.acyclic.UntypedACyclicSubGraph;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;

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
public class UntypedACyclicSubGraphImpl<V extends DirectedVertex, E extends EdgeSegment> implements UntypedACyclicSubGraph<V, E> {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(UntypedACyclicSubGraphImpl.class.getCanonicalName());

  /**
   * The id of this acyclic sub graph
   */
  private final long id;

  /**
   * track data for the vertices used in this acyclic graph, mainly used to enable topological sorting
   * 
   * TODO: candidate to be generated based on registered link segments and provide as parameter to methods requiring it. This would allow one the option to not store this but
   * generate on-the-fly trading-off memory vs computational speed. Now we always have this in memory which is costly
   */
  private Map<V, AcyclicVertexData> vertexData;

  /** track most recent topological order available */
  private ArrayDeque<V> topologicalOrder;

  /** track the link segments used via a bit set, where 1 at index indicates the link segment with id=index is included */
  private BitSet registeredLinkSegments;

  /** root vertices of the dag */
  private Set<V> rootVertices;

  /** indicate if direction of dag is inverted compared to "normal" downstream direction */
  private boolean invertedDirection;

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
  private void removeVertexData(V vertex) {
    vertexData.remove(vertex);
  }

  /**
   * Check if vertex is connected to any edge segment registered on this subgraph
   * 
   * @param vertex to check
   * @return true when connected, false otherwise
   */
  private boolean isConnectedToAnySubgraphEdgeSegment(V vertex) {
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
   * Traverse the graph recursively with the purpose of sorting it topologically
   * 
   * @param vertex      current index we are at
   * @param visited          track which vertices have been visited
   * @param topologicalOrder the list of vertices to populate in topological order
   * 
   * @return true when acyclic, false otherwise
   */
  @SuppressWarnings("unchecked")
  private boolean traverseRecursively(DirectedVertex vertex, BitSet visited, LongAdder counter, Deque<V> topologicalOrder) {
    visited.set((int) vertex.getId());

    AcyclicVertexData vertexData = getVertexData(vertex);
    if(vertexData == null){
      throw new PlanItRunTimeException("No vertex data available for vertex %s, this shouldn't happen", vertex.toString());
    }
    preVisit(vertexData, counter);

    var getNextVertex = EdgeSegment.getVertexForEdgeSegmentLambda(isDirectionInverted());
    var getNextEdgeSegments = DirectedVertex.getEdgeSegmentsForVertexLambda(isDirectionInverted());

    boolean isAcyclic = true;
    var nextEdgeSegments = getNextEdgeSegments.apply(vertex);
    for (EdgeSegment nextEdgeSegment : nextEdgeSegments) {
      if (containsEdgeSegment(nextEdgeSegment)) {
        DirectedVertex nextVertex = getNextVertex.apply(nextEdgeSegment);
        AcyclicVertexData nextVertexData = getVertexData(nextVertex);
        if (nextVertexData.preVisitIndex == 0) {
          /* valid so far, not yet explored at all, proceed */
          isAcyclic = traverseRecursively(nextVertex, visited, counter, topologicalOrder);
        } else if (nextVertexData.postVisitIndex == 0) {
          /*
           * not valid, when already visited before, then it must have been fully explored, if not it means that this vertex being expanded originates from this (not fully
           * exhausted) downstream vertex and it ends up at the starting point again (current downstream vertex) -> cycle, not a DAG
           */
          isAcyclic = false;
          LOGGER.warning(String.format("Cycle detected in supposed acyclic graph at vertex %s, terminating", nextVertex.getXmlId()));
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
    topologicalOrder.push((V) vertex);

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
   * @param rootVertex                 of the dag
   * @param invertedDirection          when true dag ends at root and all other vertices precede it, when false the root is the starting point and all other vertices succeed it
   * @param numberOfParentEdgeSegments number of directed edge segments of the parent this subgraph is a subset from
   */
  public UntypedACyclicSubGraphImpl(final IdGroupingToken groupId, V rootVertex, boolean invertedDirection, int numberOfParentEdgeSegments) {
    this.id = IdGenerator.generateId(groupId, ACyclicSubGraph.class);
    this.vertexData = new HashMap<>();
    this.registeredLinkSegments = new BitSet(numberOfParentEdgeSegments);
    this.topologicalOrder = null;
    this.invertedDirection = invertedDirection;

    this.rootVertices = new HashSet<>();
    rootVertices.add(rootVertex);
  }

  /**
   * Constructor
   * 
   * @param groupId                    generate id based on the group it resides in
   * @param rootVertices               the root vertices of the conjugate dag which can be the end or starting point depending whether or not direction is inverted.
   * @param invertedDirection          when true dag ends at root and all other vertices precede it, when false the root is the starting point and all other vertices succeed it
   * @param numberOfParentEdgeSegments number of directed edge segments of the parent this subgraph is a subset from
   */
  public UntypedACyclicSubGraphImpl(final IdGroupingToken groupId, Set<V> rootVertices, boolean invertedDirection, int numberOfParentEdgeSegments) {
    this.id = IdGenerator.generateId(groupId, ACyclicSubGraph.class);
    this.vertexData = new HashMap<>();
    this.registeredLinkSegments = new BitSet(numberOfParentEdgeSegments);
    this.topologicalOrder = null;
    this.invertedDirection = invertedDirection;

    this.rootVertices = new HashSet<>(rootVertices);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public UntypedACyclicSubGraphImpl(UntypedACyclicSubGraphImpl<V, E> other, boolean deepCopy) {
    this.id = other.getId();

    this.rootVertices = new HashSet<>(other.rootVertices);
    this.invertedDirection = other.isDirectionInverted();

    this.registeredLinkSegments = BitSet.valueOf(other.registeredLinkSegments.toByteArray());

    this.vertexData = new HashMap<>();
    other.vertexData.forEach((v, d) -> this.vertexData.put(v, deepCopy ? new AcyclicVertexData(d) : d));

    this.topologicalOrder = other.topologicalOrder != null ? new ArrayDeque<>(other.topologicalOrder) : null;
  }

  /**
   * Perform topological sorting from root, based on Gupta et al. 2008.
   * 
   * @param update when true we force an update, when false we return the most recent result without performing an update (if any exist)
   * @return Topologically sorted list of vertices, null when graph is not acyclic, or disconnected
   */
  @Override
  public Deque<V> topologicalSort(boolean update) {

    if (!update && topologicalOrder != null && !topologicalOrder.isEmpty()) {
      return topologicalOrder;
    }

    topologicalOrder = new ArrayDeque<V>(vertexData.size());
    resetVertexData();
    BitSet visited = new BitSet(vertexData.size());
    LongAdder counter = new LongAdder();

    /* for each root vertex */
    boolean isAcyclic = true;
    counter.increment();
    for (var rootVertex : rootVertices) {
      isAcyclic = traverseRecursively(rootVertex, visited, counter, topologicalOrder);
      if (!isAcyclic) {
        return null;
      }
    }

    for (Entry<V, AcyclicVertexData> vertexEntry : this.vertexData.entrySet()) {
      if (!visited.get((int) vertexEntry.getKey().getId())) {
        LOGGER.warning(String.format("Topological sort applied, but some vertices not connected to a root of the acyclic graph (%d), unable to determine sorting order", getId()));
        return null;
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
  @SuppressWarnings("unchecked")
  @Override
  public void addEdgeSegment(E edgeSegment) {
    if (edgeSegment == null) {
      LOGGER.warning("Unable to add edge segment to acyclic subgraph, null provided");
      return;
    }

    registeredLinkSegments.set((int) edgeSegment.getId());
    if (!vertexData.containsKey(edgeSegment.getUpstreamVertex())) {
      vertexData.put((V) edgeSegment.getUpstreamVertex(), new AcyclicVertexData());
    }
    if (!vertexData.containsKey(edgeSegment.getDownstreamVertex())) {
      vertexData.put((V) edgeSegment.getDownstreamVertex(), new AcyclicVertexData());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean containsEdgeSegment(EdgeSegment edgeSegment) {
    if (edgeSegment == null) {
      return false;
    }
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
  public Iterator<V> iterator() {
    return Collections.unmodifiableSet(this.vertexData.keySet()).iterator();
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public void removeEdgeSegment(E edgeSegment) {
    registeredLinkSegments.set((int) edgeSegment.getId(), false);
    if (!isConnectedToAnySubgraphEdgeSegment((V) edgeSegment.getDownstreamVertex())) {
      removeVertexData((V) edgeSegment.getDownstreamVertex());
    }
    if (!isConnectedToAnySubgraphEdgeSegment((V) edgeSegment.getUpstreamVertex())) {
      removeVertexData((V) edgeSegment.getUpstreamVertex());
    }

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UntypedACyclicSubGraphImpl<V, E> shallowClone() {
    return new UntypedACyclicSubGraphImpl<>(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UntypedACyclicSubGraphImpl<V, E> deepClone() {
    LOGGER.severe("Not a smart deep clone on untyped acyclic sub graph, so interdependencies will get screwed up, recommend not to use until properly implemented");
    return new UntypedACyclicSubGraphImpl<>(this, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<V> getRootVertices() {
    return this.rootVertices;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDirectionInverted() {
    return this.invertedDirection;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addRootVertex(V rootVertex) {
    rootVertices.add(rootVertex);
  }

}

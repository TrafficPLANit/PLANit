package org.goplanit.assignment.ltm.sltm;

import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.goplanit.algorithms.shortest.MinMaxPathResult;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.graph.directed.acyclic.UntypedACyclicSubGraph;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.network.virtual.graph.CentroidVertex;
import org.goplanit.utils.zoning.Zone;

/**
 * A rooted bush is an acyclic directed graph comprising implicit paths along a network. It has a root which can be any
 * vertex with only outgoing edge segments (or ingoing ones if it is an inverted bushed), so
 * while acyclic its direction can be either be in up or downstream direction compared to the super network it is situated on.
 * <p>
 * The vertices in the bush represent link segments in the physical network, whereas each edge represents a turn from
 * one link to another. This way each splitting rate uniquely relates to a single turn and all outgoing edges of a
 * vertex represent all turns of a node's incoming link
 * 
 * @author markr
 *
 */
public abstract class RootedBush<V extends DirectedVertex, ES extends EdgeSegment> implements Bush {

  /** Logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(RootedBush.class.getCanonicalName());

  /** the directed acyclic subgraph representation of the bush, pertaining solely to the topology */
  private final UntypedACyclicSubGraph<V, ES> dag;

  /** the origin demands (PCU/h) of the bush this may or may not be at the root (depending on whether we root in
   * origin or destination) and may or may not be located at a centroid vertex */
  private Map<V, Double> originDemandsPcuH;

  /** token for id generation unique within this bush */
  protected final IdGroupingToken bushGroupingToken;

  /** track if underlying acyclic graph is modified, if so, an update of the topological sort is required flagged by this member */
  protected boolean requireTopologicalSortUpdate = true;

  /**
   * Access to the underlying dag
   *
   * @return dag of the bush
   */
  protected UntypedACyclicSubGraph<V, ES> getDag() {
    return this.dag;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterator<V> getTopologicalIterator() {
    boolean invertDirection = false; /* do not invert direction, dag is in d-o direction */
    return getDag().getTopologicalIterator(requireTopologicalSortUpdate, invertDirection);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterator<V> getInvertedTopologicalIterator() {
    boolean invertDirection = true; /* do invert direction, dag is in o-d direction */
    return getDag().getTopologicalIterator(requireTopologicalSortUpdate, invertDirection);
  }

  /**
   * Verify if adding the sub-path edge segments would introduce a cycle in this bush
   * TODO: very costly operation as it may traverses entire bush so... find a way to bake in some more information
   *  in the topological sorting to track more information to make this much quicker, e.g., track the ordering indices
   *  and allow for direct lookup of index of vertices so we can start directly at the alternative....
   *
   * @param alternative to verify
   * @return edge segment that would introduce a cycle, null otherwise
   */
  @SuppressWarnings("unchecked")
  public ES determineIntroduceCycle(ES[] alternative) {
    if (alternative == null) {
      LOGGER.severe("Cannot verify if edge segments introduce cycle when parameters are null");
      return null;
    }

    // to see if a cycle is introduced for adding an edge segment not yet on a bush between (u,v)
    // there must be no path available on the bush between (v) and (u).

    // 1. until we get to the starting point of the alternative, all vertices before that
    //    cannot introduce a cycle when the alternative intersects with them after diverging.
    // 2. while traversing the alternative, each vertex (v) we encounter that reattaches to the bush after
    //    diverging causes a cycle if it can reach any vertex in the cycleIntroducing vertices. this set
    //    contains any preceding vertex on the alternative up till the current point (all (u)s).
    //    if such a reattaching vertex however can reach any non cycle introducing vertices we know it won't introduce
    //    a cycle (because it reattaches earlier than (u) so it can't be reached, this saves time in the BFS
    Set<V> cycleIntroducingVertices = new HashSet<>();
    Map<V, Integer> topoTraversedVertices = new HashMap<>();

    int altIndex = 0;
    final int maxAltIndex = alternative.length-1;
    V currAltVertex = (V) alternative[altIndex].getUpstreamVertex();
    cycleIntroducingVertices.add(currAltVertex);
    V currOrderedVertex;

    var topologicalIter = isInverted() ?  getTopologicalIterator() : getInvertedTopologicalIterator();
    if(topologicalIter == null){
      throw new PlanItRunTimeException("Unable to obtain topological iterator for bush (%s), this should not happen",
              getRootZoneVertex().getParent().getParentZone().getIdsAsString());
    }
    int index = 0;
    while(topologicalIter.hasNext()) {
      currOrderedVertex = topologicalIter.next();
      if (!currOrderedVertex.idEquals(currAltVertex)) {
        // register all preceding vertices as traversed up to a first match
        topoTraversedVertices.put(currOrderedVertex, index);
      }else{
        break;
      }
      ++index;
    }

    // now traverse the alternative and whenever it touches the bush, verify no path back to any preceding
    // vertices can be found
    ES nextSegment = alternative[altIndex];
    ES currSegment = alternative[altIndex];
    boolean currCoincidingVertexFound;
    int maxAllowedTopologicalIndex = Integer.MAX_VALUE;
    do{
      if(altIndex < maxAltIndex){
        currSegment = nextSegment;
        nextSegment = alternative[++altIndex];
        currAltVertex = (V) nextSegment.getUpstreamVertex();
      }else if(altIndex++ == maxAltIndex){
        currSegment = nextSegment;
        nextSegment = null;
        currAltVertex = (V) alternative[maxAltIndex].getDownstreamVertex();
      }

      currCoincidingVertexFound = containsAnyEdgeSegmentOf(currAltVertex);
      boolean directCycle =
              currSegment.getOppositeDirectionSegment()!=null &&
                      contains((ES)currSegment.getOppositeDirectionSegment());
      if(directCycle){
        // direct cycle detected since opposite direction already present, abort
        return currSegment;
      }

      boolean guaranteedNoCycle = false;
      boolean ableToDirectlyVerifyCycle = topoTraversedVertices.containsKey(currAltVertex);
      if(ableToDirectlyVerifyCycle){
        if(topoTraversedVertices.get(currAltVertex) < maxAllowedTopologicalIndex){
          // when curr vertex has a more restricting location in the topological order, then reduce the index so that we ensure we do
          // not allow any connections to a vertex that occurs later, i.e, closing a loop. For now it means no cycle though
          // because it should be smaller each time
          maxAllowedTopologicalIndex = topoTraversedVertices.get(currAltVertex);
          guaranteedNoCycle = true;
        }else{
          return currSegment;
        }
      }


      boolean potentialCycle = currCoincidingVertexFound && !guaranteedNoCycle;
      if(potentialCycle) {
        // touching - possible complex cycle

        // see if adding alternative segment would introduce cycle via BFS search to reach a cycle introducing vertex
        var result = getDag().breadthFirstSearch(
                currAltVertex,
                false,
                (es) -> true,
                (prevEs,es) -> true,
                (v, prevEs) -> cycleIntroducingVertices.contains(v));
        if(result == null){
          LOGGER.severe("BFS for cycle detection has no result, this shouldn't happen");
          return alternative[0]; // pretend cycle is found to not break
        }else if(cycleIntroducingVertices.contains(result.first())){
          // cycle - get edge segment on alternative that caused the cycle if it were to be added
          return Arrays.stream(alternative).filter(es -> es.anyVertexMatches(v -> v.idEquals(result.first()))).findFirst().get();
        }else if(result.first() != null){
          LOGGER.severe("found BFS result for cycle detection but it is not cycle introducing, this shouldn't happen");
        }
        // no cycle could be detected continue
      }

      cycleIntroducingVertices.add(currAltVertex);
      if(currCoincidingVertexFound) {
        topoTraversedVertices.remove(currAltVertex); // by considering alternative it would now close a cycle when downstream segments could reach it
      }
    }while((altIndex-1) <= maxAltIndex);
    // done, no cycle
    return null;
  }

  /**
   * Track origin demands for bush. Should only be used for initialisation and then left as is.
   *
   * @param originDemandVertex to use
   * @param demandPcuH demand to set
   */
  public void addOriginDemandPcuH(V originDemandVertex, double demandPcuH) {
    double currentDemandPcuH = this.originDemandsPcuH.getOrDefault(originDemandVertex, 0.0);
    this.originDemandsPcuH.put(originDemandVertex, currentDemandPcuH + demandPcuH);
  }

  /**
   * The alternative subpath on the network we're finding a within-bush alternative for is provided through link
   * segment labels of value -1. The point at which they coincide with the bush is indicated with label 1 at the
   * given reference vertex (passed in). Here we do a breadth-first search on the bush in the direction towards
   * its root to find a location the alternative path reconnects to the bush, which, at the latest, should be at
   * the root and at the earliest directly at the next vertex compared to the reference vertex.
   * <p>
   * The returned map contains the next edge segment for each vertex, from the vertex closer to the bush root
   * to the reference vertex where for the reference vertex the edge segment remains null
   * </p>
   *
   * @param referenceVertex                to start breadth first search from as it is the point of coincidence of
   *                                       the alternative path (via labelled vertices) and bush
   * @param forbiddenInitialSegment        the first segment of the shortest path segment from the root, that we
   *                                       cannot use otherwise this alternative is partly overlapping
   * @param alternativeSubpathVertexLabels indicating the shortest (network) path at the reference vertex but not
   *                                       part of the bush at that point (different edge segment used)
   * @return vertex at which the two paths coincided again and the map (back link tree effectively) to extract the
   * path from this vertex to the reference vertex that was found using the breadth-first method
   */
  public abstract Pair<V, Map<V, ES>> findBushAlternativeSubpathByBackLinkTree(
          V referenceVertex,
          ES forbiddenInitialSegment,
          final short[] alternativeSubpathVertexLabels);

  /**
   * Determine the sending flow on the subpath given by the  subPathArray in order from start to finish.
   *
   * @param subPathArray to use (in bush segment representation)
   * @param flowAcceptanceFactors to use (in network segment indexed representation)
   * @return sendingFlowPcuH between start and end vertex following the sub-path
   */
  public abstract double determineSubPathSendingFlow(ES[] subPathArray, double[] flowAcceptanceFactors);

  /**
   * Collect the sending flow of an edge segment in the bush, if not present, zero flow is returned
   *
   * @param edgeSegment to collect sending flow for
   * @return bush sending flow on edge segment
   */
  public abstract double getSendingFlowPcuH(final ES edgeSegment);

  /**
   * Add turn sending flow to the bush. In case the turn does not yet exist on the bush it is newly registered.
   * If it does exist and there is already flow present, the provided flow is added to it. If by adding the
   * flow (can be negative) the turn no longer has any flow, the labels are removed
   *
   * @param from             from segment of the turn
   * @param to               to segment of the turn
   * @param addFlowPcuH      to add
   * @return new labelled turn sending flow after adding given flow
   */
  public abstract double addTurnSendingFlow(
          final EdgeSegment from,
          final EdgeSegment to,
          double addFlowPcuH);

  /**
   * Traverse a bush in topological order, invert traversal of root is inverted
   *
   * @param invertIterator when true invert iterator direction
   * @param vertexConsumer to apply to each vertex
   */
  public void forEachTopologicalSortedVertex(boolean invertIterator, Consumer<V> vertexConsumer) {

    /* get topological sorted vertices to process in indicated direction */
    var vertexIter = invertIterator ? getInvertedTopologicalIterator() : getTopologicalIterator();
    if (vertexIter == null) {
      LOGGER.severe(String.format("Topologically sorted vertices on bush not available, this shouldn't happen, skip vertex traversal"));
      LOGGER.info(String.format("Bush at risk: %s", this));
      return;
    }
    var currVertex = vertexIter.next();

    /* pass over bush in topological order updating turn sending flows based on flow acceptance factors */
    while (vertexIter.hasNext()) {
      currVertex = vertexIter.next();
      vertexConsumer.accept(currVertex);
    }
  }

  /**
   * Conduct an update of the bush turn flows based on the network flow acceptance factors by conducting a bush DAG loading and updating the turn sending flows from the root, i.e.,
   * scale them back with the flow acceptance factor whenever one is encountered.
   *
   * @param flowAcceptanceFactors to use
   */
  public abstract void syncToNetworkFlows(double[] flowAcceptanceFactors);

  /**
   * To avoid bushes keeping low flow links occupied and limiting options to use links or opposite links
   * more efficiently, we will remove very low flow links from each bush, implicitly shifting this flow to
   * higher usage branches.
   *
   * @param flowThreshold         any links with flow below this threshold will be implicitly branch shifted
   * @param flowAcceptanceFactors edge segment flow acceptance factors indexed by internal id
   * @param detailedLogging       when true log what branch shifted links are affected
   * @return the edge segments that were removed during the flow shifts from this bush
   */
  public abstract TreeSet<? extends ES> performLowFlowBranchShifts(
          double flowThreshold, double[] flowAcceptanceFactors, boolean detailedLogging);

  /**
   * Constructor
   *
   * @param dag        to use for the subgraph representation
   */
  public RootedBush(UntypedACyclicSubGraph<V, ES> dag) {
    this.dag = dag;
    this.bushGroupingToken = IdGenerator.createIdGroupingToken(this, dag.getId());
    this.originDemandsPcuH = new HashMap<>();
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a eep copy, shallow copy otherwise
   */
  public RootedBush(RootedBush<V, ES> other, boolean deepCopy) {
    this.originDemandsPcuH = new HashMap<>(other.originDemandsPcuH);
    this.requireTopologicalSortUpdate = other.requireTopologicalSortUpdate;
    this.bushGroupingToken = other.bushGroupingToken;

    this.dag = deepCopy ? other.getDag().deepClone() : other.dag.shallowClone();
  }

  /**
   * Compute the min-max path tree rooted in location depending on underlying dag configuration of derived implementation and given the provided (network wide) costs. The provided
   * costs are at the network level so should contain all the segments active in the bush
   * 
   * @param linkSegmentCosts              to use
   * @param totalTransportNetworkVertices needed to be able to create primitive array recording the (partial) subgraph backward link segment results (efficiently)
   * @return minMaxPathResult, null if unable to complete
   */
  public abstract MinMaxPathResult computeMinMaxShortestPaths(
      final double[] linkSegmentCosts, final int totalTransportNetworkVertices);

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract RootedBush<V, ES> shallowClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract RootedBush<V, ES> deepClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public long getId() {
    return dag.getId();
  }

  /**
   * root vertex of the bush
   * 
   * @return root vertex of the bush
   */
  public V getRootVertex() {
    return dag.getRootVertices().iterator().next();
  }

  /**
   * Verify if bush contains the edge segment provided
   *
   * @param edgeSegment to check
   * @return true when present, false otherwise
   */
  public boolean contains(ES edgeSegment) {
    return getDag().containsEdgeSegment(edgeSegment);
  }

  /**
   * Verify if the bush contains the given edge segment
   *
   * @param edgeSegmentId to verify
   * @return true when present, false otherwise
   */
  public boolean contains(long edgeSegmentId) {
    return getDag().containsEdgeSegment(edgeSegmentId);
  }

  /**
   * Verify if the bush contains any edge segment of the edge in either direction
   *
   * @param edge to verify
   * @return true when an edge segment of the edge is present, false otherwise
   */
  public boolean containsAnyEdgeSegmentOf(DirectedEdge edge) {
    for (var edgeSegment : edge.getEdgeSegments()) {
      if (contains((ES) edgeSegment)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Verify if the bush contains any edge segment attached to the vertex
   *
   * @param vertex to verify
   * @return true when an edge segment of the vertex is registered, false otherwise
   */
  public boolean containsAnyEdgeSegmentOf(V vertex) {
    for (var edge : vertex.getEdges()) {
      if (containsAnyEdgeSegmentOf(edge)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Indicates if bush has inverted direction w.r.t. its root
   * 
   * @return true when inverted, false otherwise
   */
  public boolean isInverted() {
    return dag.isDirectionInverted();
  }

  /**
   * Origins (with non-zero flow) registered on this bush
   * 
   * @return origins on this bush
   */
  public Set<? extends V> getOriginVertices() {
    return this.originDemandsPcuH.keySet();
  }

  /**
   * Get the origin demand for a given origin
   * 
   * @param originVertex to collect demand for
   * @return demand (if any)
   */
  public Double getOriginDemandPcuH(DirectedVertex originVertex) {
    return this.originDemandsPcuH.get(originVertex);
  }

  /**
   * Collect iterator for all unique directed vertices in the bush
   *
   * @return iterator
   */
  public Iterator<V> getDirectedVertexIterator() {
    return getDag().iterator();
  }

  /**
   * Each rooted bush is expected to have a zone attached to its root vertex, which is to be returned here
   *
   * @return root zone
   */
  public abstract CentroidVertex getRootZoneVertex();

  /**
   * Access to root zone
   *
   * @return root zone
   */
  public Zone getRootZone(){
    return getRootZoneVertex().getParent().getParentZone();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    var sb = new StringBuilder("[");
    /* log all edge segments on bush */
    var root = getRootVertex();
    Queue<DirectedVertex> openVertices = new PriorityQueue<>();
    openVertices.add(root);
    Set<DirectedVertex> processed = new HashSet<>();

    final var getNextEdgeSegments =
            isInverted() ? DirectedVertex.getEntryEdgeSegments : DirectedVertex.getExitEdgeSegments;
    final var getNextVertex =
            isInverted() ? EdgeSegment.getUpstreamVertex : EdgeSegment.getDownstreamVertex;

    while (!openVertices.isEmpty()) {
      var vertex = openVertices.poll();
      processed.add(vertex);
      for (var nextSegment : getNextEdgeSegments.apply(vertex)) {
        if (!contains((ES) nextSegment)) {
          continue;
        }
        var nextVertex = getNextVertex.apply(nextSegment);
        sb.append(nextSegment.getXmlId()).append(",");
        if (processed.contains(nextVertex)) {
          continue;
        }
        openVertices.add(nextVertex);
      }
    }
    sb.deleteCharAt(sb.length() - 1);
    sb.append("]");
    return sb.toString();
  }

}

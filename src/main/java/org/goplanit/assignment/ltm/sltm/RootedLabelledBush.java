package org.goplanit.assignment.ltm.sltm;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.algorithms.shortest.MinMaxPathResult;
import org.goplanit.algorithms.shortest.ShortestPathSearchUtils;
import org.goplanit.graph.directed.acyclic.ACyclicSubGraphImpl;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.graph.directed.acyclic.ACyclicSubGraph;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.Pair;

/**
 * A rooted bush is an acyclic directed graph comprising implicit paths along a network. It has a single root which
 * can be any vertex with only outgoing edge segments. while acyclic its direction can be either be in up or
 * downstream direction compared to the super network it is situated on.
 * <p>
 * The vertices in the bush represent link segments in the physical network, whereas each edge represents a turn
 * from one link to another. This way each splitting rate uniquely relates to a single turn and all outgoing edges
 * of a vertex represent all turns of a node's incoming link
 * 
 * @author markr
 *TODO: should be no more distinction between this and a rootedbush after label removal
 */
public abstract class RootedLabelledBush extends RootedBush<DirectedVertex, EdgeSegment> {

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(RootedLabelledBush.class.getCanonicalName());

  /**
   * Determine the sending flow between origin,destination vertex using the subpath given by the subPathArray in order from start to finish. We utilise the initial sending flow on
   * the indexed segment and label as the base flow which is then followed along the subpath through the bush splitting rates up to the final link segment
   * 
   * @param subPathSendingFlow to start with
   * @param index              offset to start in array with
   * @param subPathArray       to extract path from
   * @return sendingFlowPcuH between index and end vertex following the sub-path
   */
  private double determineSubPathSendingFlow(
      double subPathSendingFlow, int index, final EdgeSegment[] subPathArray) {
    if(subPathSendingFlow <= 0.0){
      return subPathSendingFlow;
    }

    var currEdgeSegment = subPathArray[index++];

    // in case due to other local flow reductions the link flow has become lower than the NL consistent
    // flow following the path and applying alphas and splitting rates, cap to this more restricting avalable flow instead
    double linkRestrictedSubPathSendingFlow =
            Math.min(subPathSendingFlow, bushData.getTotalSendingFlowFromPcuH(currEdgeSegment));

    if (index < subPathArray.length && Precision.positive(subPathSendingFlow)) {
      var nextEdgeSegment = subPathArray[index];

      var currSplittingRate = bushData.getSplittingRate(currEdgeSegment, nextEdgeSegment);
      if (currSplittingRate <= 0) {
          return 0.0;
      }
      double remainingSubPathSendingFlow = linkRestrictedSubPathSendingFlow * currSplittingRate;
      return determineSubPathSendingFlow(remainingSubPathSendingFlow, index, subPathArray);
    }
    return linkRestrictedSubPathSendingFlow;
  }

  /**
   * Access to DAG as regular acylic subgraph rather than untyped
   * 
   * @return dag
   */
  @Override
  protected ACyclicSubGraph getDag() {
    return (ACyclicSubGraph) super.getDag();
  }

  /** track bush specific data */
  protected final LabelledBushTurnData bushData;

  /**
   * Constructor
   * 
   * @param idToken                 the token to base the id generation on
   * @param rootVertex              the root vertex of the bush which can be the end or starting point depending whether or not direction is inverted
   * @param inverted                when true bush ends at root vertex and all other vertices precede it, when false the root is the starting point and all other vertices succeed
   *                                it
   * @param maxSubGraphEdgeSegments The maximum number of edge segments the bush can at most register given the parent network it is a subset of
   */
  public RootedLabelledBush(final IdGroupingToken idToken, DirectedVertex rootVertex, boolean inverted, long maxSubGraphEdgeSegments) {
    super(new ACyclicSubGraphImpl(idToken, rootVertex, inverted, (int) maxSubGraphEdgeSegments));
    this.bushData = new LabelledBushTurnData(this);
  }

  /**
   * Copy constructor
   * 
   * @param bush to copy
   * @param deepCopy when true, create a eep copy, shallow copy otherwise
   */
  public RootedLabelledBush(RootedLabelledBush bush, boolean deepCopy) {
    super(bush, deepCopy);
    this.bushData = deepCopy ? bush.bushData.deepClone() : bush.bushData.shallowClone();
  }

  /**
   * Compute the min-max path tree rooted in location depending on underlying dag configuration of derived
   * implementation and given the provided (network wide) costs. The provided costs are at the network level
   * so should contain all the segments active in the bush
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
  public abstract RootedLabelledBush shallowClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract RootedLabelledBush deepClone();

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

    final var getNextEdgeSegments = isInverted() ? DirectedVertex.getEntryEdgeSegments : DirectedVertex.getExitEdgeSegments;
    final var getNextVertex = isInverted() ? EdgeSegment.getUpstreamVertex : EdgeSegment.getDownstreamVertex;

    while (!openVertices.isEmpty()) {
      var vertex = openVertices.poll();
      processed.add(vertex);
      for (EdgeSegment nextSegment : getNextEdgeSegments.apply(vertex)) {
        if (!containsEdgeSegment(nextSegment)) {
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

  /**
   * Verify if adding the sub-path edge segments would introduce a cycle in this bush
   * TODO: very costly operation as it may traverses entire bush --> find a way to bake in some more information
   *  in the topological sorting to track more information to make this much quicker, e.g., track the ordering indices
   *  and allow for direct lookup of index of vertices so we can start directly at the alternative....
   * 
   * @param alternative to verify
   * @return edge segment that would introduce a cycle, null otherwise
   */
  public EdgeSegment determineIntroduceCycle(EdgeSegment[] alternative) {
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
    Set<DirectedVertex> cycleIntroducingVertices = new HashSet<>();
    Map<DirectedVertex, Integer> topoTraversedVertices = new HashMap<>();

    int altIndex = 0;
    final int maxAltIndex = alternative.length-1;
    DirectedVertex currAltVertex = alternative[altIndex].getUpstreamVertex();
    cycleIntroducingVertices.add(currAltVertex);
    DirectedVertex currOrderedVertex;

    var topologicalIter = isInverted() ?  getTopologicalIterator() : getInvertedTopologicalIterator();
    int index = 0;
    while(topologicalIter.hasNext()) {
      currOrderedVertex = topologicalIter.next();
      if (!currOrderedVertex.idEquals(currAltVertex)) {
        // register all preceding vertices as non-cycle introducing up to a first match to hopefully save some time in BFS
        topoTraversedVertices.put(currOrderedVertex, index);
      }else{
        break;
      }
      ++index;
    }

    // now traverse the alternative and whenever it touches the bush, verify no path back to any preceding
    // vertices can be found
    EdgeSegment nextSegment = alternative[altIndex];
    EdgeSegment currSegment = alternative[altIndex];
    boolean currCoincidingVertexFound;
    boolean currLocalNoCycle;
    int maxAllowedTopologicalIndex = Integer.MAX_VALUE;
    do{
      if(altIndex < maxAltIndex){
        currSegment = nextSegment;
        nextSegment = alternative[++altIndex];
        currAltVertex = nextSegment.getUpstreamVertex();
      }else if(altIndex++ == maxAltIndex){
        currSegment = nextSegment;
        nextSegment = null;
        currAltVertex = alternative[maxAltIndex].getDownstreamVertex();
      }

      currCoincidingVertexFound = containsAnyEdgeSegmentOf(currAltVertex);
      boolean directCycle = currSegment.getOppositeDirectionSegment()!=null && containsEdgeSegment(currSegment.getOppositeDirectionSegment());
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
            (prevEs,es) -> !topoTraversedVertices.containsKey(es.getUpstreamVertex()), // do not explore beyond vertices that we can check in main loop via index more efficiently
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
   * Add turn sending flow to the bush. In case the turn does not yet exist on the bush it is newly registered. If it does exist and there is already flow present, the provided
   * flow is added to it. If by adding the flow (can be negative) the turn no longer has any flow, the labels are removed
   * 
   * @param from             from segment of the turn
   * @param to               to segment of the turn
   * @param addFlowPcuH      to add
   * @return new labelled turn sending flow after adding given flow
   */
  public double addTurnSendingFlow(
          final EdgeSegment from,
          final EdgeSegment to,
          double addFlowPcuH) {

    if (addFlowPcuH > 0) {
      if (!containsEdgeSegment(from)) {
        if (containsEdgeSegment(from.getOppositeDirectionSegment())) {
          LOGGER.warning(String.format("Trying to add turn flow (%s,%s) on bush (%s) where the opposite direction (of segment %s) already is part of the bush, this break acyclicity",
              from.getXmlId(), to.getXmlId(), getRootZoneVertex().getParent().getParentZone().getIdsAsString(), from.getXmlId()));
        }
        getDag().addEdgeSegment(from);
        requireTopologicalSortUpdate = true;
      }
      if (!containsEdgeSegment(to)) {
        if (containsEdgeSegment(to.getOppositeDirectionSegment())) {
          LOGGER.warning(String.format("Trying to add turn flow (%s,%s) on bush (%s) where the opposite direction (of segment %s) already is part of the bush, this break acyclicity",
              from.getXmlId(), to.getXmlId(), getRootZoneVertex().getParent().getParentZone().getIdsAsString(), to.getXmlId()));
        }
        getDag().addEdgeSegment(to);
        requireTopologicalSortUpdate = true;
      }
    }
    return bushData.addTurnSendingFlow(from, to, addFlowPcuH);
  }

  /**
   * Collect bush turn sending flow (if any)
   * 
   * @param from to use
   * @param to   to use
   * @return sending flow, zero if unknown
   */
  public double getTurnSendingFlow(final EdgeSegment from, final EdgeSegment to) {
    return bushData.getTurnSendingFlowPcuH(from, to);
  }

  /**
   * Collect the sending flow of an edge segment in the bush, if not present, zero flow is returned
   * 
   * @param edgeSegment to collect sending flow for
   * @return bush sending flow on edge segment
   */
  public double getSendingFlowPcuH(final EdgeSegment edgeSegment) {
    return bushData.getTotalSendingFlowFromPcuH(edgeSegment);
  }

  /**
   * Verify if the provided turn has any registered sending flow
   * 
   * @param from to use
   * @param to   to use
   * @return true when turn sending flow is present, false otherwise
   */
  public boolean containsTurnSendingFlow(final EdgeSegment from, final EdgeSegment to) {
    return bushData.getTurnSendingFlowPcuH(from, to) > 0;
  }

  /**
   * Collect the bush splitting rate on the given turn
   * 
   * @param from to use
   * @param to   to use
   * @return found splitting rate, in case the turn is not used, 0 is returned
   */
  public double getSplittingRate(final EdgeSegment from, final EdgeSegment to) {
    return bushData.getSplittingRate(from, to);
  }

  /**
   * Collect the bush splitting rates for a given incoming edge segment. If entry segment has no flow, zero splitting rates are returned for all turns
   * 
   * @param entrySegment to use
   * @return splitting rates in primitive array in order of which one iterates over the outgoing edge segments of the downstream from segment vertex
   */
  public double[] getSplittingRates(final EdgeSegment entrySegment) {
    return bushData.getSplittingRates(entrySegment);
  }

  /**
   * Remove a turn from the bush by removing it from the acyclic graph and removing any data associated with it. Edge segments are also removed in case the no longer carry any flow
   * 
   * @param fromEdgeSegment of the turn
   * @param toEdgeSegment   of the turn
   */
  public void removeTurn(final EdgeSegment fromEdgeSegment, final EdgeSegment toEdgeSegment) {
    bushData.removeTurn(fromEdgeSegment, toEdgeSegment);
    // LOGGER.info(String.format("Removing turn (%s,%s) from bush", fromEdgeSegment.getXmlId(), toEdgeSegment.getXmlId()));

    if (!Precision.positive(getSendingFlowPcuH(fromEdgeSegment))) {
      removeEdgeSegment(fromEdgeSegment);
    }
    if (!Precision.positive(getSendingFlowPcuH(toEdgeSegment))) {
      removeEdgeSegment(toEdgeSegment);
    }
    requireTopologicalSortUpdate = true;
  }

  /**
   * Remove edge segment from bush, if it no longer has flow
   * 
   * @param edgeSegment to remove
   * @return true when removed, false otherwise
   */
  public boolean removeEdgeSegment(EdgeSegment edgeSegment) {
    /* update graph if edge segment is unused */
    if (!Precision.positive(getSendingFlowPcuH(edgeSegment))) {
      // LOGGER.info(String.format("Removing edge segment (%s) from bush", edgeSegment.getXmlId()));
      getDag().removeEdgeSegment(edgeSegment);
      return true;
    }

    LOGGER.warning(String.format(
            "Unable to remove edge segment %s from bush (origin %s) unless it has no flow", edgeSegment.getXmlId()));
    return false;
  }

  /**
   * Verify if the bush contains the given edge segment
   * 
   * @param edgeSegment to verify
   * @return true when present, false otherwise
   */
  public boolean containsEdgeSegment(EdgeSegment edgeSegment) {
    return getDag().containsEdgeSegment(edgeSegment);
  }

  /**
   * Verify if the bush contains the given edge segment
   *
   * @param edgeSegmentId to verify
   * @return true when present, false otherwise
   */
  public boolean containsEdgeSegment(long edgeSegmentId) {
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
      if (getDag().containsEdgeSegment(edgeSegment)) {
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
  public boolean containsAnyEdgeSegmentOf(DirectedVertex vertex) {
    for (var edge : vertex.getEdges()) {
      if (containsAnyEdgeSegmentOf(edge)) {
        return true;
      }
    }
    return false;
  }

  /**
   * The alternative subpath is provided through link segment labels of value -1. The point at which they coincide
   * with the bush is indicated with label 1 at the given reference vertex (passed in). Here we do a
   * breadth-first search on the bush in the direction towards its root to find a location the alternative path
   * reconnects to the bush, which, at the latest, should be at the root and at the earliest directly at the next
   * vertex compared to the reference vertex.
   * <p>
   * Note that the breadth-first approach is a choice not a necessity but the underlying idea is that a shorter
   * PAS (which is likely to be found) is used by more origins and therefore more useful to explore than a really
   * long PAS. This is preferred - in the original TAPAS - over simply backtracking along either the shortest or
   * longest path of the min-max tree which would also be viable options,a s would a depth-first search.
   * <p>
   * Consider implementing various strategies here in order to explore what works best but for now we adopt a
   * breadth-first search
   * <p>
   * The returned map contains the next edge segment for each vertex, from the vertex closer to the bush root
   * to the reference vertex where for the reference vertex the edge segment remains null
   * 
   * @param referenceVertex                to start breadth first search from as it is the point of coincidence of the alternative path (via labelled vertices) and bush
   * @param forbiddenInitialSegment        the first segment of the shortest path segment from the root, that we cannot use
   *                                       otherwise this alternative is partly overlapping
   * @param alternativeSubpathVertexLabels indicating the shortest (network) path at the reference vertex but not part of the bush at that point (different edge segment used)
   * @return vertex at which the two paths coincided again and the map (back link tree effectively) to extract the path from this vertex to the reference vertex that was found using the breadth-first
   *         method
   */
  public Pair<DirectedVertex, Map<DirectedVertex, EdgeSegment>> findBushAlternativeSubpathByBackLinkTree(
          DirectedVertex referenceVertex, EdgeSegment forbiddenInitialSegment, final short[] alternativeSubpathVertexLabels) {

    // cannot use the initial segment that is part of the cheapest option.
    // Note that we cannot check for the -1 marking here because it is possible that the shortest alternative loops
    // around and the alternative we are looking is exactly 1 link long starting at vertex marked with 1 and ending at vertex marked -1
    // so actual initial rival edge segment is needed for exclusion
    Predicate<EdgeSegment> initialInclusionCondition = es -> !es.equals(forbiddenInitialSegment);

    // only consider turns with positive flow on bush
    BiPredicate<EdgeSegment, EdgeSegment> regularInclusionCondition = bushData::containsTurnSendingFlow;

    // terminate when shortest path reconnects to the bush
    BiPredicate<DirectedVertex, EdgeSegment> terminationCondition = (v, prevEs) ->
        alternativeSubpathVertexLabels[(int) v.getId()] == -1;

    // when bush is inverted, shortest path search runs from root outward and backlinks run in graph direction
    //   so do not invert BFS to create backlinks consistent with that approach
    // when not inverted, shortest path search runs from root outward and backlinks run opposite graph direction
    //   so invert BFS to create backlinks consistent with that approach
    boolean invertBfs = !this.isInverted();

    // perform BFS
    var result = getDag().breadthFirstSearch(
        referenceVertex,
        invertBfs,
        initialInclusionCondition,
        regularInclusionCondition,
        terminationCondition);

    /*
     * no result could be found, only possible when cycle is detected before reaching origin Not sure this will actually happen, so created warning to check, when it does happen
     * investigate and see if this expected behaviour (if so remove statement). this would equate to finding a vertex marked with a '1' in Xie & Xie, which I do not do because I
     * don't think it is needed, but I might be wrong.
     */
    if(result== null || result.first() == null) {
      LOGGER.warning(String.format("Cycle found when finding alternative subpath on bush merging at vertex %s", referenceVertex.getXmlId()));
    }
    return result;
  }

  /**
   * Determine the sending flow between origin,destination vertex using the subpath given by the subPathMap, where each vertex provides its exit segment. This is used to traverse
   * the subpath and extract the portion of the sending flow currently known at the bushes startVertex provided to the end vertex
   * 
   * @param startVertex to use
   * @param endVertex   to use
   * @param subPathMap  to extract path from
   * @return sendingFlowPcuH between start and end vertex following the found sub-path
   */
  public double computeSubPathSendingFlow(
          final DirectedVertex startVertex, final DirectedVertex endVertex, final Map<DirectedVertex, EdgeSegment> subPathMap) {
    EdgeSegment nextEdgeSegment = subPathMap.get(startVertex);
    double subPathSendingFlow = bushData.getTotalSendingFlowFromPcuH(nextEdgeSegment);

    if (Precision.positive(subPathSendingFlow)) {
      var currEdgeSegment = nextEdgeSegment;
      nextEdgeSegment = subPathMap.get(currEdgeSegment.getDownstreamVertex());
      do {
        subPathSendingFlow *= bushData.getSplittingRate(currEdgeSegment, nextEdgeSegment);
        currEdgeSegment = nextEdgeSegment;
        nextEdgeSegment = subPathMap.get(currEdgeSegment.getDownstreamVertex());
      } while (nextEdgeSegment != null && Precision.positive(subPathSendingFlow));
    }

    return subPathSendingFlow;
  }

  /**
   * Determine the accepted flow between origin,destination vertex using the subpath given by the subPathArray in order from start to finish. We utilise the initial sending flow on
   * the first segment as the base flow which is then reduced by the splitting rates and acceptance factor up to and including the final link segment
   * 
   * @param startVertex                  to use
   * @param endVertex                    to use
   * @param subPathArray                 to extract path from
   * @param linkSegmentAcceptanceFactors the acceptance factor to apply along the path, indexed by link segment id
   * @return acceptedFlowPcuH between start and end vertex following the sub-path
   */
  public double computeSubPathAcceptedFlow(
          final DirectedVertex startVertex, final DirectedVertex endVertex, final EdgeSegment[] subPathArray,
      final double[] linkSegmentAcceptanceFactors) {

    int index = 0;
    EdgeSegment currEdgeSegment = subPathArray[index++];
    double subPathAcceptedFlowPcuH = bushData.getTotalSendingFlowFromPcuH(currEdgeSegment);

    var nextEdgeSegment = currEdgeSegment;
    while (index < subPathArray.length && Precision.positive(subPathAcceptedFlowPcuH)) {
      currEdgeSegment = nextEdgeSegment;
      nextEdgeSegment = subPathArray[index++];
      subPathAcceptedFlowPcuH *=
              bushData.getSplittingRate(currEdgeSegment, nextEdgeSegment) *
                      linkSegmentAcceptanceFactors[(int) currEdgeSegment.getId()];
    }
    subPathAcceptedFlowPcuH *= linkSegmentAcceptanceFactors[(int) nextEdgeSegment.getId()];

    return subPathAcceptedFlowPcuH;
  }

  /**
   * Determine the sending flow between origin,destination vertex using the subpath given by the segment +
   * subPathArray in order from start to finish. We utilise the initial sending flow on the entry segment as the
   * base flow which is then followed along the subpath through the bush splitting rates up to the final link segment
   *
   * @param entrySegment to start subpath from
   * @param subPathArray to append to entry segment to extract path from
   * @return sendingFlowPcuH between start and end vertex following the sub-path
   */
  public double determineSubPathSendingFlow(EdgeSegment entrySegment, EdgeSegment[] subPathArray) {

    int index = 0;
    double subPathSendingFlow = 0;

    /* determine flow from entry segment into initial segment, from there on recursively traverse sub-path */
    var initialSubPathEdgeSegment = subPathArray[index];
    double currSplittingRate = bushData.getSplittingRate(entrySegment, initialSubPathEdgeSegment);
    if (currSplittingRate <= 0) {
        return subPathSendingFlow;
    }
    double remainingSubPathSendingFlow = bushData.getTotalSendingFlowFromPcuH(entrySegment) * currSplittingRate;
    subPathSendingFlow = determineSubPathSendingFlow(remainingSubPathSendingFlow, index, subPathArray);
    return subPathSendingFlow;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void syncToNetworkFlows(double[] flowAcceptanceFactors) {

    /* get topological sorted vertices to process always in origin-destination direction */
    var vertexIter = isInverted() ? getInvertedTopologicalIterator() : getTopologicalIterator();
    if (vertexIter == null) {
      LOGGER.severe(String.format("Topologically sorted vertices on bush not available, this shouldn't happen, skip turn flow update"));
      LOGGER.info(String.format("Bush at risk: %s", this));
      return;
    }
    var currVertex = vertexIter.next();

    /* pass over bush in topological order updating turn sending flows based on flow acceptance factors */
    while (vertexIter.hasNext()) {
      currVertex = vertexIter.next();
      for (var entrySegment : currVertex.getEntryEdgeSegments()) {
        if (!containsEdgeSegment(entrySegment)) {
          continue;
        }

        double entryAcceptedFlow = bushData.getTotalAcceptedFlowToPcuH(entrySegment, flowAcceptanceFactors);
        double[] splittingRates = getSplittingRates(entrySegment);

        int splittingRateIndex = 0;
        for (var exitSegment : currVertex.getExitEdgeSegments()) {
          if (!containsEdgeSegment(exitSegment)) {
            ++splittingRateIndex;
            continue;
          }

          double bushExitSegmentSplittingRate = splittingRates[splittingRateIndex];
          if (Precision.positive(bushExitSegmentSplittingRate)) {
            double bushTurnLabeledAcceptedFlow = entryAcceptedFlow * bushExitSegmentSplittingRate;
            bushData.setTurnSendingFlow(
                entrySegment, exitSegment, bushTurnLabeledAcceptedFlow, true);
          }else if(bushExitSegmentSplittingRate > 0){
            LOGGER.warning(String.format(
                    "Minute splitting rate found on turn from (%s) to (%s) on bush %s, ignored, but should probably be dealt with properly!",
                    entrySegment.getIdsAsString(), exitSegment.getIdsAsString(), this.getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
          }
          ++splittingRateIndex;
        }
      }
    }
  }

  /**
   * Verify if empty
   * 
   * @return true when empty, false otherwise
   */
  public boolean isEmpty() {
    return bushData.hasTurnFlows();
  }

}

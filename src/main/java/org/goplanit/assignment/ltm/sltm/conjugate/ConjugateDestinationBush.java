package org.goplanit.assignment.ltm.sltm.conjugate;

import java.util.*;
import java.util.logging.Logger;

import org.goplanit.algorithms.shortest.MinMaxPathResult;
import org.goplanit.algorithms.shortest.ShortestSearchType;
import org.goplanit.assignment.ltm.sltm.BushFlowLabel;
import org.goplanit.assignment.ltm.sltm.RootedBush;
import org.goplanit.graph.directed.acyclic.ConjugateACyclicSubGraphImpl;
import org.goplanit.utils.graph.directed.ConjugateDirectedEdge;
import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.graph.directed.acyclic.ConjugateACyclicSubGraph;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.network.virtual.graph.CentroidVertex;
import org.goplanit.utils.network.virtual.physical.conjugate.ConjugateConnectoidNode;

/**
 * A conjugate rooted bush is an acyclic directed graph comprising implicit paths along a conjugate network, i.e.
 * turn based network (conjugate edge segments). It has a single root based on the original network, i.e. in the
 * conjugate network it can represent multiple conjugate nodes since conjugate nodes are edges/edgeSegments on the
 * original network leading to this vertex.
 * <p>
 * The conjugate edge segments in the conjugate bush represent pairs of original link segments, i.e. turns in the
 * physical network
 * 
 * @author markr
 *
 */
public class ConjugateDestinationBush extends RootedBush<ConjugateDirectedVertex, ConjugateEdgeSegment> {

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(ConjugateDestinationBush.class.getCanonicalName());

  /**
   * Determine the sending flow between origin,destination vertex using the subpath given by the subPathArray in
   * order from start to finish. We utilise the initial sending flow on the indexed conjugate segment as the base
   * flow which is then followed along the subpath through the bush splitting rates up to the final link segment
   *
   * todo: (not todo) NOTE to self, last synced with RootLabelledBush/DestinationBush implementation on 22/11
   * 
   * @param subPathAcceptedFlow to start with
   * @param index              offset to start in array with
   * @param subPathArray       to extract path from
   * @return sendingFlowPcuH between index and end vertex following the sub-path
   */
  private double determineSubPathSendingFlow(
          double subPathAcceptedFlow,
          double compoundedFlowAcceptanceScalingFactor,
          int index,
          double[] flowAcceptanceFactors,
          final ConjugateEdgeSegment[] subPathArray) {

    if(subPathAcceptedFlow <= 0.0){
      return subPathAcceptedFlow;
    }

    var currConjugateSegment = subPathArray[index++];

    if (index < subPathArray.length && Precision.positive(subPathAcceptedFlow)) {
      var nextConjugateSegment = subPathArray[index];

      var currSendingFlow = bushData.getTurnSendingFlowPcuH(currConjugateSegment);
      // restrict by what is available on our subpath
      double subPathSendingFlow = Math.min(subPathAcceptedFlow, currSendingFlow);
      double flowAcceptanceFactor = flowAcceptanceFactors[(int)currConjugateSegment.getId()];
      return determineSubPathSendingFlow(
              subPathSendingFlow * flowAcceptanceFactor,
              compoundedFlowAcceptanceScalingFactor * flowAcceptanceFactor,
              index,
              flowAcceptanceFactors,
              subPathArray);
    }

    // done, rescale to original sending flow using reciprocal of compounded flow acceptance factors
    return subPathAcceptedFlow * 1/(compoundedFlowAcceptanceScalingFactor);
  }

  /** destination of this conjugate bush */
  protected final CentroidVertex destination;

  /** track bush specific data */
  protected final ConjugateBushTurnData bushData;

  /**
   * {@inheritDoc}
   */
  @Override
  protected ConjugateACyclicSubGraph getDag() {
    return (ConjugateACyclicSubGraph) super.getDag();
  }

  /**
   * Constructor. It is expected that all provided root vertices represent edges in the original network leading
   * to a single root.
   *
   * todo: (not todo) NOTE to self, last synced with RootLabelledBush/DestinationBush implementation on 22/11
   * 
   * @param idToken          the token to base the id generation on
   * @param destination      this conjugate destination bush has rooted conjugate vertices for
   * @param rootVertex       this conjugate node represents the root vertex as it is the dummy node from which all
   *                         initial turns enter/exit the conjugate network from the conjugate destination
   * @param maxSubGraphConjugateSegments The maximum number of conjugate edge segments, i.e. turns, the conjugate bush
   *                                     can at most register given the parent network it is a subset of
   */
  public ConjugateDestinationBush(
      final IdGroupingToken idToken,
      final CentroidVertex destination,
      ConjugateConnectoidNode rootVertex,
      int maxSubGraphConjugateSegments) {
    super(new ConjugateACyclicSubGraphImpl(idToken, rootVertex, true /* inverted */, maxSubGraphConjugateSegments));
    this.bushData = new ConjugateBushTurnData(this);
    this.destination = destination;
  }

  /**
   * Copy constructor
   * 
   * @param bush to copy
   * @param deepCopy when true, create a eep copy, shallow copy otherwise
   */
  public ConjugateDestinationBush(ConjugateDestinationBush bush, boolean deepCopy) {
    super(bush, deepCopy);
    this.destination = bush.destination;

    // container wrapper with primitives, so always clone
    this.bushData = bush.bushData.shallowClone();
  }

  /**
   * Compute the min-max path tree rooted in location depending on underlying dag configuration of derived
   * implementation and given the provided conjugate (network wide) costs. The provided costs are at the conjugate
   * network level so should contain all the conjugate segments active in the bush
   * 
   * @param conjugatelinkSegmentCosts to use
   * @param totalConjugateVertices    needed to be able to create primitive array recording the (partial) subgraph
   *                                  backward conjugate link segment results (efficiently)
   * @return minMaxPathResult, null if unable to complete
   */
  public MinMaxPathResult computeMinMaxShortestPaths(
          final double[] conjugatelinkSegmentCosts, final int totalConjugateVertices) {
    // TODO: not rewritten yet
    return null;
  }

  @Override
  public Iterator<ConjugateDirectedVertex> getTopologicalIterator() {
    // TODO: not rewritten yet
    return null;
  }

  @Override
  public Iterator<ConjugateDirectedVertex> getInvertedTopologicalIterator() {
    // TODO: not rewritten yet
    return null;
  }

  @Override
  public ShortestSearchType getShortestSearchType() {
    // TODO: not rewritten yet
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateDestinationBush shallowClone() {
    return new ConjugateDestinationBush(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateDestinationBush deepClone() {
    return new ConjugateDestinationBush(this, true);
  }

  /**
   * Verify if adding the sub-path conjugated edge segments (turns) would introduce a cycle in this bush
   * 
   * @param alternative to verify
   * @return edge segment that would introduces a cycle, null otherwise
   */
  public ConjugateEdgeSegment determineIntroduceCycle(ConjugateEdgeSegment[] alternative) {
    // TODO: not rewritten yet
    return null;
  }

  /**
   * Add turn sending flow to the bush. In case the turn does not yet exist on the bush it is newly registered.
   * If it does exist and there is already flow present, the provided flow is added to it.
   * If by adding the flow (can be negative) the turn no longer has any flow, it is removed
   *
   * todo: (not todo) NOTE to self, last synced with RootLabelledBush/DestinationBush implementation on 22/11
   * 
   * @param turn             the turn in conjugate form
   * @param addFlowPcuH      to add
   * @return new turn sending flow after adding given flow
   */
  public double addTurnSendingFlow(final ConjugateEdgeSegment turn, double addFlowPcuH) {

    if (addFlowPcuH > 0) {
      if (!containsConjugateSegment(turn)) {
        if (containsConjugateSegment(turn.getOppositeDirectionSegment())) {
          var originalTurnSegments = turn.getOriginalAdjacentEdgeSegments();
          LOGGER.warning(String.format("Trying to add turn flow (%s,%s) on bush (%s) where the opposite direction turn" +
                          "is already is part of the bush, this breaks acyclicity",
                  originalTurnSegments.first().getXmlId(), originalTurnSegments.second().getXmlId(),
                  getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
        }
        getDag().addEdgeSegment(turn);
        requireTopologicalSortUpdate = true;
      }
    }
    return bushData.addTurnSendingFlow(turn, addFlowPcuH);
  }

  /**
   * Collect bush turn sending flow (if any)
   *
   * todo: (not todo) NOTE to self, last synced with RootLabelledBush/DestinationBush implementation on 22/11
   * 
   * @param turn to use
   * @return sending flow, zero if unknown
   */
  public double getTurnSendingFlow(final ConjugateEdgeSegment turn) {
    return bushData.getTurnSendingFlowPcuH(turn);
  }

  /**
   * Collect the sending flow of a conjugate node (original edge segment) in the conjugate bush, if not present,
   * zero flow is returned.
   *
   * todo: (not todo) NOTE to self, last synced with RootLabelledBush/DestinationBush implementation on 22/11
   * 
   * @param conjugateNode to collect sending flow for
   * @return bush sending flow
   */
  public double getSendingFlowPcuH(final ConjugateDirectedVertex conjugateNode) {
    return bushData.getTotalSendingFlowFromPcuH(conjugateNode);
  }

  /**
   * Verify if the provided turn has any registered sending flow
   *
   * todo: (not todo) NOTE to self, last synced with RootLabelledBush/DestinationBush implementation on 22/11
   * 
   * @param turn to use
   * @return true when turn sending flow is present, false otherwise
   */
  public boolean containsTurnSendingFlow(final ConjugateEdgeSegment turn) {
    return bushData.getTurnSendingFlowPcuH(turn) > 0;
  }

  /**
   * Collect the bush splitting rate on the given turn
   *
   * todo: (not todo) NOTE to self, last synced with RootLabelledBush/DestinationBush implementation on 22/11
   * 
   * @param turn to use
   * @return found splitting rate, in case the turn is not used, 0 is returned
   */
  public double getSplittingRate(final ConjugateEdgeSegment turn) {
    return bushData.getSplittingRate(turn);
  }

  /**
   * Collect the bush splitting rates for a given conjugate node (original incoming edge segment). If no flow,
   * zero splitting rates are returned
   *
   * todo: (not todo) NOTE to self, last synced with RootLabelledBush/DestinationBush implementation on 22/11
   * 
   * @param conjugateVertex to use
   * @return splitting rates in primitive array in order of which one iterates over the outgoing (conjugate)
   * edge segments
   */
  public double[] getSplittingRates(final ConjugateDirectedVertex conjugateVertex) {
    return bushData.getSplittingRates(conjugateVertex);
  }

  /**
   * Remove a turn from the conjugate bush
   *
   * todo: (not todo) NOTE to self, last synced with RootLabelledBush/DestinationBush implementation on 22/11
   * 
   * @param turn of the turn
   */
  public void removeTurn(final ConjugateEdgeSegment turn) {
    bushData.removeTurn(turn);
//    LOGGER.info(String.format("Removing turn (%s,%s) from bush",
//            turn.getOriginalAdjacentEdgeSegments().first().getXmlId(), turn.getOriginalAdjacentEdgeSegments().second().getXmlId()));

    // unlike non-conjugate bushes, we do not need to check if we are to remove edge/link segments since a turn is an
    // edge segment, so this can just be done. conjugate vertices could be removed but there is no benefit in doing so
    // given that no data is tracked on them nor are subgraphs like bushes tracked via vertices. So ignore (for now)
    getDag().removeEdgeSegment(turn);
    requireTopologicalSortUpdate = true;
  }

  /**
   * Verify if the bush contains any conjugate edge segment (turn) of the conjugate edge in either direction
   *
   * todo: (not todo) NOTE to self, last synced with RootLabelledBush/DestinationBush implementation on 22/11
   * 
   * @param conjugateEdge to verify
   * @return true when present, false otherwise
   */
  public boolean containsAnyConjugateSegmentFrom(ConjugateDirectedEdge conjugateEdge) {
    for (var turnSegment : conjugateEdge.getEdgeSegments()) {
      if (getDag().containsEdgeSegment(turnSegment)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Verify if the bush contains any edge segment attached to the vertex
   *
   * todo: (not todo) NOTE to self, last synced with RootLabelledBush/DestinationBush implementation on 22/11
   *
   * @param conjugateVertex to verify
   * @return true when an edge segment of the vertex is registered, false otherwise
   */
  public boolean containsAnyConjugateSegmentAttachedTo(ConjugateDirectedVertex conjugateVertex) {
    for (var conjugateEdge : conjugateVertex.getEdges()) {
      if (containsAnyConjugateSegmentFrom(conjugateEdge)) {
        return true;
      }
    }
    return false;
  }

  /**
   * The alternative subpath is provided through link segment labels of value -1. The point at which they coincide
   * with the bush is indicated with label 1 at the given reference vertex (passed in). Here we do a breadth-first
   * search on the bush in the direction towards its root to find a location the alternative path reconnects to the
   * bush, which, at the latest, should be at the root and at the earliest directly at the next vertex compared to
   * the reference vertex.
   * <p>
   * Note that the breadth-first approach is a choice not a necessity but the underlying idea is that a shorter
   * PAS (which is likely to be found) is used by more origins and therefore more useful to explore than a really
   * long PAS. This is preferred - in the original TAPAS - over simply backtracking along either the shortest or
   * longest path of the min-max tree which would also be viable options,a s would a depth-first search.
   * <p>
   * Consider implementing various strategies here in order to explore what works best but for now we adopt a
   * breadth-first search
   * <p>
   * The returned map contains the next edge segment for each vertex, from the vertex closer to the bush root to
   * the reference vertex where for the reference vertex the edge segment remains null
   * 
   * @param referenceVertex                to start breadth first search from as it is the point of coincidence
   *                                       of the alternative path (via labelled vertices) and bush
   * @param forbiddenInitialSegment        the first segment of the shortest path segment from the root, that we
   *                                       cannot use otherwise this alternative is partly overlapping
   * @param alternativeSubpathVertexLabels indicating the shortest (network) path at the reference vertex but
   *                                       not part of the bush at that point (different edge segment used)
   * @return vertex at which the two paths coincided again and the map to extract the path from this vertex to
   * the reference vertex that was found using the breadth-first method
   *
   * todo: (not todo) NOTE to self, last synced with RootLabelledBush/DestinationBush implementation on 22/11
   */
  public Pair<ConjugateDirectedVertex, Map<ConjugateDirectedVertex, ConjugateEdgeSegment>> findBushAlternativeSubpath(
          ConjugateDirectedVertex referenceVertex,
          ConjugateEdgeSegment forbiddenInitialSegment,
          final short[] alternativeSubpathVertexLabels) {

    // TODO: not rewritten yet
    return null;
  }

  /**
   * Determine the accepted flow between origin,destination vertex using the subpath given by the subPathArray in
   * order from start to finish. We utilise the initial sending flow on the first segment as the base flow which
   * is then reduced by the splitting rates and acceptance factor up to and including the final link segment
   * 
   * @param startVertex to use
   * @param endVertex   to use
   * @param subPathMap  to extract path from
   * @param nonConjugateLinkSegmentAcceptanceFactors the acceptance factor to apply along the path, indexed by link segment id
   * @return sendingFlowPcuH between start and end vertex following the found sub-path
   *
   * todo: (not todo) NOTE to self, last synced with RootLabelledBush/DestinationBush implementation on 22/11   *
   */
  public double computeSubPathAcceptedFlow(
          final ConjugateDirectedVertex startVertex,
          final ConjugateDirectedVertex endVertex,
          final Map<ConjugateDirectedVertex, ConjugateEdgeSegment> subPathMap,
          final double[] nonConjugateLinkSegmentAcceptanceFactors) {
    // TODO: not rewritten yet
    return Double.NEGATIVE_INFINITY;
  }

  /**
   * Determine the sending flow on the subpath given by the  subPathArray in order from start to finish.
   *
   * @param subPathArray to use
   * @param nonConjugateFlowAcceptanceFactors to use
   * @return sendingFlowPcuH between start and end vertex following the sub-path
   *
   * todo: (not todo) NOTE to self, last synced with RootLabelledBush/DestinationBush implementation on 22/11
   */
  public double determineSubPathSendingFlow(ConjugateEdgeSegment[] subPathArray, double[] nonConjugateFlowAcceptanceFactors) {
    // TODO: not rewritten yet
    return Double.NEGATIVE_INFINITY;
  }

  /**
   * Determine the sending flow between origin,destination vertex using the subpath given by the segment +
   * subPathArray in order from start to finish. We utilise the initial sending flow on the entry segment as the
   * base flow which is then followed along the subpath through the bush splitting rates up to the final link segment
   *
   * @param entrySegment to start subpath from
   * @param subPathArray to append to entry segment to extract path from
   * @param nonConjugateFlowAcceptanceFactors to use
   * @return sendingFlowPcuH between start and end vertex following the sub-path
   *
   * todo: (not todo) NOTE to self, last synced with RootLabelledBush/DestinationBush implementation on 22/11
   */
  public double determineSubPathSendingFlow(
          ConjugateEdgeSegment entrySegment, ConjugateEdgeSegment[] subPathArray, double[] nonConjugateFlowAcceptanceFactors) {
    // TODO: not rewritten yet <---- SHOULD NOT BE NEEDED NOW THAT WE ARE IN CONJUGATE FORM
    return Double.NEGATIVE_INFINITY;
  }

  /**
   * Verify if empty
   *
   * @return true when empty, false otherwise
   * todo: (not todo) NOTE to self, last synced with RootLabelledBush/DestinationBush implementation on 22/11
   */
  public boolean isEmpty() {
    return bushData.hasTurnFlows();
  }

  /**
   * collect destination of this bush
   *
   * @return destination zone
   * todo: (not todo) NOTE to self, last synced with RootLabelledBush/DestinationBush implementation on 22/11
   */
  public CentroidVertex getDestination() {
    return getRootZoneVertex();
  }

  /**
   * Each conjugate destination bush is expected to have a single destination zone to which all of its root vertices are connected, which is to be returned here
   *
   * @return destination zone
   * todo: (not todo) NOTE to self, last synced with RootLabelledBush/DestinationBush implementation on 22/11
   */
  @Override
  public CentroidVertex getRootZoneVertex() {
    return this.destination;
  }

  /**
   * Verify if the bush contains the given turn segment.
   *
   * todo: (not todo) NOTE to self, last synced with RootLabelledBush/DestinationBush implementation on 22/11
   *
   * @param turnSegment to verify
   * @return true when present, false otherwise
   */
  public boolean containsConjugateSegment(ConjugateEdgeSegment turnSegment) {
    return getDag().containsEdgeSegment(turnSegment);
  }

  /**
   * {@inheritDoc}
   *
   * todo: (not todo) NOTE to self, last synced with RootLabelledBush/DestinationBush implementation on 22/11
   */
  @Override
  public boolean contains(EdgeSegment turnSegment) {
    return containsConjugateSegment((ConjugateEdgeSegment)turnSegment);
  }

  /**
   * {@inheritDoc}
   *
   * todo: (not todo) NOTE to self, last synced with RootLabelledBush/DestinationBush implementation on 22/11
   */
  @Override
  public String toString() {
    var sb = new StringBuilder("[");

    /* log all original edge segments on conjugate bush */
    var root = getRootVertex();
    Queue<ConjugateDirectedVertex> openVertices = new PriorityQueue<>();
    openVertices.add(root);
    Set<ConjugateDirectedVertex> processed = new HashSet<>();

    // inverted bush so work backwards
    final var getNextEdgeSegments = ConjugateDirectedVertex.getEntryEdgeSegments;
    final var getNextVertex = ConjugateEdgeSegment.getUpstreamVertex;

    while (!openVertices.isEmpty()) {
      var vertex = openVertices.poll();
      processed.add(vertex);
      vertex.getOriginalEdge().forEachSegment(es -> sb.append(es.getXmlId()).append(","));
      for (var nextSegment : getNextEdgeSegments.apply(vertex)) {
        if(!contains(nextSegment)) {
          continue;
        }
        ConjugateDirectedVertex nextVertex = (ConjugateDirectedVertex) getNextVertex.apply(nextSegment);
        if (processed.contains(nextVertex)) {
          continue;
        }
        openVertices.add((ConjugateDirectedVertex) nextVertex);
      }
    }
    sb.deleteCharAt(sb.length() - 1);
    sb.append("]");

    return "Conjugate Bush: destination zone: " +
            getRootZoneVertex().getParent().getParentZone().getXmlId() + "\n" + sb;
  }

  /**
   * {@inheritDoc}
   *
   * todo: (not todo) NOTE to self, last synced with RootLabelledBush/DestinationBush implementation on 22/11
   */
  @Override
  public void syncToNetworkFlows(double[] originalNetworkFlowAcceptanceFactors) {

    /* get topological sorted vertices to process from origin to destination*/
    var conjugateVertexIter = getInvertedTopologicalIterator();
    if (conjugateVertexIter == null) {
      LOGGER.severe(String.format("Topologically sorted vertices on bush not available, this shouldn't happen, " +
              "skip turn flow update"));
      return;
    }
    var currConjugateVertex = conjugateVertexIter.next();

    /* pass over conjugate bush in topological order updating turn sending flows based on flow acceptance factors */
    final boolean AllowTurnRemoval = false;
    while (conjugateVertexIter.hasNext()) {
      currConjugateVertex = conjugateVertexIter.next();
      double conjugateVertexAcceptedFlow =
              bushData.getTotalAcceptedFlowToPcuH(currConjugateVertex, originalNetworkFlowAcceptanceFactors);

      /*
       * bush splitting rates by [conjugate exit segment index] - splitting rates are computed based on turn
       * flows but placed in new array. So once we have the splitting rates we can safely update the turn
       * flows without affecting these splitting rates
       */
      double[] splittingRates = getSplittingRates(currConjugateVertex);
      int index = -1;
      for (var turnSegment : currConjugateVertex.getExitEdgeSegments()) {
        ++index;
        if (!containsConjugateSegment(turnSegment)) {
          continue;
        }
        double currTurnSplittingRate = splittingRates[index];
        if (currTurnSplittingRate > 0) {
          double bushTurnLabeledAcceptedFlow = conjugateVertexAcceptedFlow * currTurnSplittingRate;
          bushData.setTurnSendingFlow(turnSegment, bushTurnLabeledAcceptedFlow, AllowTurnRemoval);
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TreeSet<ConjugateEdgeSegment> performLowFlowBranchShifts(
          double flowThreshold, double[] nonConjugateFlowAcceptanceFactors, boolean detailedLogging){
    //todo
    LOGGER.severe("NOT YET IMPLEMENTED");
    return null;
  }

}

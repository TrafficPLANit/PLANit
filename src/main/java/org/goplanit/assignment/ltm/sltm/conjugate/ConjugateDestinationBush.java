package org.goplanit.assignment.ltm.sltm.conjugate;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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
import org.goplanit.utils.network.layer.physical.ConjugateNode;
import org.goplanit.utils.network.virtual.CentroidVertex;
import org.goplanit.utils.network.virtual.ConjugateConnectoidNode;
import org.goplanit.utils.zoning.OdZone;

/**
 * A conjugate rooted bush is an acyclic directed graph comprising implicit paths along a conjugate network, i.e. turn based network (conjugate edge segments). It has a single
 * root based on the original network, i.e. in the conjugate network it can represent multiple conjugate nodes since conjugate nodes are edges/edgeSegments on the original network
 * leading to this vertex.
 * <p>
 * The conjugate edge segments in the conjugate bush represent pairs of original link segments, i.e. turns in the physical network
 * 
 * @author markr
 *
 */
public class ConjugateDestinationBush extends RootedBush<ConjugateDirectedVertex, ConjugateEdgeSegment> {

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(ConjugateDestinationBush.class.getCanonicalName());

  /**
   * Determine the sending flow between origin,destination vertex using the subpath given by the subPathArray in order from start to finish. We utilise the initial sending flow on
   * the indexed conjugate segment as the base flow which is then followed along the subpath through the bush splitting rates up to the final link segment
   * 
   * @param subPathSendingFlow to start with
   * @param index              offset to start in array with
   * @param subPathArray       to extract path from
   * @return sendingFlowPcuH between index and end vertex following the sub-path
   */
  private double determineSubPathSendingFlow(double subPathSendingFlow, int index, final ConjugateEdgeSegment[] subPathArray) {
    if (index < subPathArray.length && Precision.positive(subPathSendingFlow)) {
      var turnSegment = subPathArray[index];
      double splittingRate = bushData.getSplittingRate(turnSegment);
      if (splittingRate > 0) {
        subPathSendingFlow *= splittingRate;
        return determineSubPathSendingFlow(subPathSendingFlow, ++index, subPathArray);
      } else {
        return 0;
      }
    }
    return subPathSendingFlow;
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
   * Constructor. It is expected that all provided root vertices represent edges in the orignal network leading to a single root.
   * 
   * @param idToken          the token to base the id generation on
   * @param destination      this conjugate destination bush has rooted conjugate vertices for
   * @param rootVertex       this conjugate node represents the root vertex as it is the dummy node from which all initial turns enter/exit the conjugate network from theconjugate
   *                         destination
   * @param maxSubGraphTurns The maximum number of conjugate edge segments, i.e. turns, the conjugate bush can at most register given the parent network it is a subset of
   */
  public ConjugateDestinationBush(final IdGroupingToken idToken, final CentroidVertex destination, ConjugateConnectoidNode rootVertex, int maxSubGraphTurns) {
    super(new ConjugateACyclicSubGraphImpl(idToken, rootVertex, true /* inverted */, maxSubGraphTurns));
    this.bushData = new ConjugateBushTurnData();
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
   * Compute the min-max path tree rooted in location depending on underlying dag configuration of derived implementation and given the provided conjugate (network wide) costs. The
   * provided costs are at the conjugate network level so should contain all the conjugate segments active in the bush
   * 
   * @param conjugatelinkSegmentCosts to use
   * @param totalConjugateVertices    needed to be able to create primitive array recording the (partial) subgraph backward conjugate link segment results (efficiently)
   * @return minMaxPathResult, null if unable to complete
   */
  public MinMaxPathResult computeMinMaxShortestPaths(final double[] conjugatelinkSegmentCosts, final int totalConjugateVertices) {
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
    // TODO: see if can be reused across other bush implementations
//    if (alternative == null) {
//      LOGGER.severe("Cannot verify if conjugate edge segments introduce cycle when parameters are null");
//      return null;
//    }
//    ConjugateEdgeSegment currTurnSegment = null;
//    for (int index = 0; index < alternative.length; ++index) {
//      currTurnSegment = alternative[index];
//      if (currTurnSegment == null) {
//        LOGGER.severe(String.format("Alternative's conjugate edge segment at position %d on array is null, this shouldn't happen", index));
//        break;
//      }
//      currTurnSegment = currTurnSegment.getOppositeDirectionSegment();
//      if (currTurnSegment != null && containsTurnSegment(currTurnSegment)) {
//        return alternative[index];
//      }
//    }
    return null;
  }

  /**
   * Add turn sending flow to the conjugate bush. In case the turn does not yet exist on the bush it is newly registered. If it does exist and there is already flow present, the
   * provided flow is added to it. If by adding the flow (can be negative) the turn no longer has any flow, the turn itself is removed
   * 
   * @param turn        the turn
   * @param addFlowPcuH to add
   * @return new labelled turn sending flow after adding given flow
   */
  public double addTurnSendingFlow(final ConjugateEdgeSegment turn, double addFlowPcuH) {
    return addTurnSendingFlow(turn, addFlowPcuH, false);
  }

  /**
   * Add turn sending flow to the bush. In case the turn does not yet exist on the bush it is newly registered. If it does exist and there is already flow present, the provided
   * flow is added to it. If by adding the flow (can be negative) the turn no longer has any flow, it is removed if allowed
   * 
   * @param turn             the turn
   * @param addFlowPcuH      to add
   * @param allowTurnRemoval when true we remove turn when no flow remains after adding (negative) flow, when false, we only change the flow to zero but the bush is not adjusted
   * @return new turn sending flow after adding given flow
   */
  public double addTurnSendingFlow(final ConjugateEdgeSegment turn, double addFlowPcuH, boolean allowTurnRemoval) {

    if (addFlowPcuH > 0) {
      if (!containsTurnSegment(turn)) {
        if (containsTurnSegment(turn.getOppositeDirectionSegment())) {
          LOGGER.warning(String.format("Trying to add turn flow on bush where the opposite direction is already part of the bush, this break acyclicity"));
        }
        getDag().addEdgeSegment(turn);
        requireTopologicalSortUpdate = true;
      }
    }
    return bushData.addTurnSendingFlow(turn, addFlowPcuH, allowTurnRemoval);
  }

  /**
   * Collect bush turn sending flow (if any)
   * 
   * @param turn to use
   * @return sending flow, zero if unknown
   */
  public double getTurnSendingFlow(final ConjugateEdgeSegment turn) {
    return bushData.getTurnSendingFlowPcuH(turn);
  }

  /**
   * Collect the sending flow of a conjugate node (original edge segment) in the conjugate bush, if not present, zero flow is returned
   * 
   * @param conjugateNode to collect sending flow for
   * @return bush sending flow
   */
  public double getSendingFlowPcuH(final ConjugateNode conjugateNode) {
    return bushData.getTotalSendingFlowFromPcuH(conjugateNode);
  }

  /**
   * Verify if the provided turn has any registered sending flow
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
   * @param turn to use
   * @return found splitting rate, in case the turn is not used, 0 is returned
   */
  public double getSplittingRate(final ConjugateEdgeSegment turn) {
    return bushData.getSplittingRate(turn);
  }

  /**
   * Collect the bush splitting rates for a given conjugate node (original incoming edge segment). If no flow, zero splitting rates are returned
   * 
   * @param conjugateVertex to use
   * @return splitting rates in primitive array in order of which one iterates over the outgoing (conjugate) edge segments
   */
  public double[] getSplittingRates(final ConjugateDirectedVertex conjugateVertex) {
    return bushData.getSplittingRates(conjugateVertex);
  }

  /**
   * Remove a turn from the conjugate bush
   * 
   * @param turn of the turn
   */
  public void removeTurn(final ConjugateEdgeSegment turn) {
    bushData.removeTurn(turn);
    getDag().removeEdgeSegment(turn);
    requireTopologicalSortUpdate = true;
  }

  /**
   * Verify if the bush contains the given turn segment
   * 
   * @param turnSegment to verify
   * @return true when present, false otherwise
   */
  public boolean containsTurnSegment(ConjugateEdgeSegment turnSegment) {
    return getDag().containsEdgeSegment(turnSegment);
  }

  /**
   * Verify if the bush contains any conjugate edge segment (turn) of the conjugate edge in either direction
   * 
   * @param conjugateEdge to verify
   * @return true when present, false otherwise
   */
  public boolean containsAnyTurnSegmentOf(ConjugateDirectedEdge conjugateEdge) {
    for (var turnSegment : conjugateEdge.getEdgeSegments()) {
      if (getDag().containsEdgeSegment(turnSegment)) {
        return true;
      }
    }
    return false;
  }

  /**
   * The alternative subpath is provided through link segment labels of value -1. The point at which they coincide with the bush is indicated with label 1 at the given reference
   * vertex (passed in). Here we do a breadth-first search on the bush in the direction towards its root to find a location the alternative path reconnects to the bush, which, at
   * the latest, should be at the root and at the earliest directly at the next vertex compared to the reference vertex.
   * <p>
   * Note that the breadth-first approach is a choice not a necessity but the underlying idea is that a shorter PAS (which is likely to be found) is used by more origins and
   * therefore more useful to explore than a really long PAS. This is preferred - in the original TAPAS - over simply backtracking along either the shortest or longest path of the
   * min-max tree which would also be viable options,a s would a depth-first search.
   * <p>
   * Consider implementing various strategies here in order to explore what works best but for now we adopt a breadth-first search
   * <p>
   * The returned map contains the next edge segment for each vertex, from the vertex closer to the bush root to the reference vertex where for the reference vertex the edge
   * segment remains null
   * 
   * @param referenceVertex                to start breadth first search from as it is the point of coincidence of the alternative path (via labelled vertices) and bush
   * @param alternativeSubpathVertexLabels indicating the shortest (network) path at the reference vertex but not part of the bush at that point (different edge segment used)
   * @return vertex at which the two paths coincided again and the map to extract the path from the this vertex to the reference vertex that was found using the breadth-first
   *         method
   */
  public Pair<DirectedVertex, Map<DirectedVertex, EdgeSegment>> findBushAlternativeSubpath(DirectedVertex referenceVertex, final short[] alternativeSubpathVertexLabels) {
    // TODO: not rewritten yet
//    Deque<Pair<DirectedVertex, EdgeSegment>> openVertexQueue = new ArrayDeque<>(30);
//    Map<DirectedVertex, EdgeSegment> processedVertices = new TreeMap<>();
//
//    /*
//     * Construct results in same direction as shortest path search, so So, for one-to-all regular search, we construct results where we have for each vertex its upstream segment,
//     * while for all-to-one we have the downstream segment for each vertex
//     */
//    final boolean invertNextDirection = true;
//    final var getNextEdgeSegments = ShortestPathSearchUtils.getEdgeSegmentsInDirectionLambda(this, invertNextDirection);
//    final var getNextVertex = ShortestPathSearchUtils.getVertexFromEdgeSegmentLambda(this, invertNextDirection);
//
//    /* start with eligible edge segments of reference vertex except alternative labelled segment */
//    processedVertices.put(referenceVertex, null);
//    var nextEdgeSegments = getNextEdgeSegments.apply(referenceVertex);
//    for (var nextSegment : nextEdgeSegments) {
//      if (containsTurnSegment(nextSegment) && alternativeSubpathVertexLabels[(int) referenceVertex.getId()] != -1) {
//        openVertexQueue.add(Pair.of(getNextVertex.apply(nextSegment), nextSegment));
//      }
//    }
//
//    while (!openVertexQueue.isEmpty()) {
//      Pair<DirectedVertex, EdgeSegment> current = openVertexQueue.pop();
//      var currentVertex = current.first();
//      if (processedVertices.containsKey(currentVertex)) {
//        continue;
//      }
//
//      if (alternativeSubpathVertexLabels[(int) currentVertex.getId()] == -1) {
//        /* first point of coincidence with alternative labelled path */
//        processedVertices.put(currentVertex, current.second());
//        return Pair.of(current.first(), processedVertices);
//      }
//
//      /* breadth-first loop for used turns that not yet have been processed */
//      nextEdgeSegments = getNextEdgeSegments.apply(currentVertex);
//      for (var nextSegment : nextEdgeSegments) {
//        if (containsTurnSegment(nextSegment) && bushData.containsTurnSendingFlow(nextSegment, current.second())) {
//          var nextVertex = getNextVertex.apply(nextSegment);
//          if (!processedVertices.containsKey(nextVertex)) {
//            openVertexQueue.add(Pair.of(nextVertex, nextSegment));
//          }
//        }
//      }
//
//      processedVertices.put(currentVertex, current.second());
//    }
//
//    /*
//     * no result could be found, only possible when cycle is detected before reaching origin Not sure this will actually happen, so created warning to check, when it does happen
//     * investigate and see if this expected behaviour (if so remove statement). this would equate to finding a vertex marked with a '1' in Xie & Xie, which I do not do because I
//     * don't think it is needed, but I might be wrong.
//     */
//    LOGGER.warning(String.format("Cycle found when finding alternative subpath on bush merging at vertex %s", referenceVertex.getXmlId()));
    return null;
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
  public double computeSubPathSendingFlow(final DirectedVertex startVertex, final DirectedVertex endVertex, final Map<DirectedVertex, EdgeSegment> subPathMap) {
    // TODO: not rewritten yet
//    EdgeSegment nextEdgeSegment = subPathMap.get(startVertex);
//    double subPathSendingFlow = bushData.getTotalSendingFlowFromPcuH(nextEdgeSegment);
//
//    if (Precision.positive(subPathSendingFlow)) {
//      var currEdgeSegment = nextEdgeSegment;
//      nextEdgeSegment = subPathMap.get(currEdgeSegment.getDownstreamVertex());
//      do {
//        subPathSendingFlow *= bushData.getSplittingRate(currEdgeSegment, nextEdgeSegment);
//        currEdgeSegment = nextEdgeSegment;
//        nextEdgeSegment = subPathMap.get(currEdgeSegment.getDownstreamVertex());
//      } while (nextEdgeSegment != null && Precision.positive(subPathSendingFlow));
//    }
//
//    return subPathSendingFlow;
    return Double.NEGATIVE_INFINITY;
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
  public double computeSubPathAcceptedFlow(final DirectedVertex startVertex, final DirectedVertex endVertex, final EdgeSegment[] subPathArray,
      final double[] linkSegmentAcceptanceFactors) {
    // TODO: not rewritten yet
//
//    int index = 0;
//    EdgeSegment currEdgeSegment = subPathArray[index++];
//    double subPathAcceptedFlowPcuH = bushData.getTotalSendingFlowFromPcuH(currEdgeSegment);
//
//    var nextEdgeSegment = currEdgeSegment;
//    while (index < subPathArray.length && Precision.positive(subPathAcceptedFlowPcuH)) {
//      currEdgeSegment = nextEdgeSegment;
//      nextEdgeSegment = subPathArray[index++];
//      subPathAcceptedFlowPcuH *= bushData.getSplittingRate(currEdgeSegment, nextEdgeSegment) * linkSegmentAcceptanceFactors[(int) currEdgeSegment.getId()];
//    }
//    subPathAcceptedFlowPcuH *= linkSegmentAcceptanceFactors[(int) nextEdgeSegment.getId()];
//
//    return subPathAcceptedFlowPcuH;
    return Double.NEGATIVE_INFINITY;
  }

  /**
   * Determine the sending flow between origin,destination vertex using the subpath given by the segment + subPathArray in order from start to finish. We utilise the initial
   * sending flow on the entry segment as the base flow which is then followed along the subpath through the bush splitting rates up to the final link segment
   *
   * @param entrySegment to start subpath from
   * @param subPathArray to append to entry segment to extract path from
   * @return sendingFlowPcuH between start and end vertex following the sub-path
   */
  public double determineSubPathSendingFlow(EdgeSegment entrySegment, EdgeSegment[] subPathArray) {
    // TODO: not rewritten yet
//
//    int index = 0;
//    var usedEntryLabels = getFlowCompositionLabels(entrySegment);
//    double subPathSendingFlow = 0;
//    for (var entryLabel : usedEntryLabels) {
//      double labelSendingFlow = bushData.getTotalSendingFlowFromPcuH(entrySegment, entryLabel);
//
//      /* determine flow from entry segment into initial segment, from there on recursively traverse sub-path */
//      var initialSubPathEdgeSegment = subPathArray[index];
//      var exitLabels = getFlowCompositionLabels(initialSubPathEdgeSegment);
//      if (exitLabels == null) {
//        return 0;
//      }
//
//      var exitSegmentExitLabelSplittingRates = bushData.getSplittingRates(entrySegment, entryLabel);
//      double remainingSubPathSendingFlow = 0;
//      for (var exitLabel : exitLabels) {
//        Double currSplittingRate = exitSegmentExitLabelSplittingRates.get(initialSubPathEdgeSegment, exitLabel);
//        if (currSplittingRate == null || currSplittingRate <= 0) {
//          continue;
//        }
//        remainingSubPathSendingFlow += labelSendingFlow * currSplittingRate;
//      }
//
//      labelSendingFlow = determineSubPathSendingFlow(remainingSubPathSendingFlow, entryLabel, index, subPathArray);
//      subPathSendingFlow += labelSendingFlow;
//    }
//
//    return subPathSendingFlow;
    return Double.NEGATIVE_INFINITY;
  }

  /**
   * Find out the portion of the origin attributed flow on the segment that belongs to each available flow composition label proportional to the total flow across all provided
   * labels on this same segment
   * 
   * @param edgeSegment              to determine the label rates for
   * @param pasFlowCompositionLabels to determine relative proportions for based on total flow across provided labels on the link segment
   * @return the rates at hand for each found composition label
   */
  public TreeMap<BushFlowLabel, Double> determineProportionalFlowCompositionRates(final EdgeSegment edgeSegment, final Set<BushFlowLabel> pasFlowCompositionLabels) {
    // TODO: not rewritten yet
//    double totalSendingFlow = 0;
//    var rateMap = new TreeMap<BushFlowLabel, Double>();
//    for (var label : pasFlowCompositionLabels) {
//      double labelFlow = bushData.getTotalSendingFlowFromPcuH(edgeSegment, label);
//      rateMap.put(label, labelFlow);
//      totalSendingFlow += labelFlow;
//    }
//
//    for (var entry : rateMap.entrySet()) {
//      entry.setValue(entry.getValue() / totalSendingFlow);
//    }
//
//    return rateMap;
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void syncToNetworkFlows(double[] originalNetworkFlowAcceptanceFactors) {

    /* get topological sorted vertices to process from origin to destination*/
    var conjugateVertexIter = getInvertedTopologicalIterator();
    if (conjugateVertexIter == null) {
      LOGGER.severe(String.format("Topologically sorted vertices on bush not available, this shouldn't happen, skip turn flow update"));
      return;
    }
    var currConjugateVertex = conjugateVertexIter.next();

    /* pass over conjugate bush in topological order updating turn sending flows based on flow acceptance factors */
    final boolean AllowTurnRemoval = false;
    while (conjugateVertexIter.hasNext()) {
      currConjugateVertex = conjugateVertexIter.next();
      double conjugateVertexAcceptedFlow = bushData.getTotalAcceptedFlowToPcuH(currConjugateVertex, originalNetworkFlowAcceptanceFactors);

      /*
       * bush splitting rates by [conjugate exit segment index] - splitting rates are computed based on turn flows but placed in new array. So once we have the splitting rates we
       * can safely update the turn flows without affecting these splitting rates
       */
      double[] splittingRates = getSplittingRates(currConjugateVertex);
      int index = -1;
      for (var turnSegment : currConjugateVertex.getExitEdgeSegments()) {
        ++index;
        if (!containsTurnSegment(turnSegment)) {
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
  public void performLowFlowBranchShifts(double flowThreshold, double[] flowAcceptanceFactors, boolean detailedLogging){
    //todo
    LOGGER.severe("NOT YET IMPLEMENTED");
  }

  /**
   * Verify if empty
   * 
   * @return true when empty, false otherwise
   */
  public boolean isEmpty() {
    return bushData.hasTurnFlows();
  }

  /**
   * Each conjugate destination bush is expected to have a single destination zone to which all of its root vertices are connected, which is to be returned here
   *
   * @return destination zone
   */
  @Override
  public CentroidVertex getRootZoneVertex() {
    return this.destination;
  }

}

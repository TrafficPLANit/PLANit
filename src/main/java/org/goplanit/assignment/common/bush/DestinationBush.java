package org.goplanit.assignment.common.bush;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.algorithms.shortest.MinMaxPathResult;
import org.goplanit.algorithms.shortest.ShortestPathAcyclicMinMaxGeneralised;
import org.goplanit.algorithms.shortest.ShortestSearchType;
import org.goplanit.graph.directed.acyclic.ACyclicSubGraphImpl;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.graph.directed.acyclic.ACyclicSubGraph;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.IterableUtils;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.network.virtual.graph.CentroidVertex;

/**
 * A destination bush is an (inverted) acyclic directed graph rooted at many origins going to a single destination representing all implicit paths along a network to the given
 * destination. Demand on the bush is placed along its root node(s) which is then split across the graph by (bush specific) splitting rates that reside on each edge. The sum of the
 * edge splitting rates originating from a vertex must always sum to 1.
 * 
 * @author markr
 *
 */
public class DestinationBush extends RootedBush<DirectedVertex, EdgeSegment> {

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(DestinationBush.class.getCanonicalName());

  /** Destination of this bush */
  protected final CentroidVertex destination;

  /** track bush specific data */
  protected final EntryExitBushTurnData bushData;

  /**
   * Access to DAG as regular acylic subgraph rather than untyped
   *
   * @return dag
   */
  @Override
  public ACyclicSubGraph getDag() {
    return (ACyclicSubGraph) super.getDag();
  }

  /**
   * Determine the sending flow between origin,destination vertex using the subpath given by the subPathArray in
   * order from start to finish. We utilise the initial sending flow on the indexed segment and label as the base
   * flow which is then followed along the subpath through the bush splitting rates up to the final link segment
   *
   * @param subPathAcceptedFlow accepted flow so far
   * @param compoundedFlowAcceptanceScalingFactor combined multiplied alphas so far
   * @param index              offset to start in array with
   * @param flowAcceptanceFactors to use
   * @param subPathArray       to extract path from*
   * @return sendingFlowPcuH between index and end vertex following the sub-path
   */
  private double determineSubPathSendingFlow(
      double subPathAcceptedFlow,
      double compoundedFlowAcceptanceScalingFactor,
      int index,
      double[] flowAcceptanceFactors,
      final EdgeSegment[] subPathArray) {

    if(subPathAcceptedFlow <= 0.0){
      return subPathAcceptedFlow;
    }

    var currEdgeSegment = subPathArray[index++];

    if (index < subPathArray.length && Precision.positive(subPathAcceptedFlow)) {
      var nextEdgeSegment = subPathArray[index];

      var currSendingFlow = bushData.getTurnSendingFlowPcuH(currEdgeSegment, nextEdgeSegment);
      // restrict by what is following our subpath
      double subPathSendingFlow = Math.min(subPathAcceptedFlow, currSendingFlow);
      double flowAcceptanceFactor = flowAcceptanceFactors[(int)currEdgeSegment.getId()];
      return determineSubPathSendingFlow(
          subPathSendingFlow * flowAcceptanceFactor,
          compoundedFlowAcceptanceScalingFactor * flowAcceptanceFactor,
          index,
          flowAcceptanceFactors,
          subPathArray);
    }

    // restrict by what is left going out of the segment (in rare cases this can be restricting due to
    // flow being removed at outgoing turns as a starting point of a PAS for example
    double totalSendingFlowLastSubPathSegment = getSendingFlowPcuH(subPathArray[subPathArray.length-1]);
    double restrictedSubPathAcceptedFlow = Math.min(subPathAcceptedFlow, totalSendingFlowLastSubPathSegment);

    // done, rescale to original sending flow using reciprocal of compounded flow acceptance factors
    return restrictedSubPathAcceptedFlow * 1/(compoundedFlowAcceptanceScalingFactor);
  }

  /**
   * Constructor
   * 
   * @param idToken                 the token to base the id generation on
   * @param destination             destination of the bush
   * @param maxSubGraphEdgeSegments The maximum number of edge segments the bush can at most register given the parent network it is a subset of
   */
  public DestinationBush(final IdGroupingToken idToken, CentroidVertex destination, long maxSubGraphEdgeSegments) {
    super(new ACyclicSubGraphImpl(idToken, destination, true /* inverted */, (int) maxSubGraphEdgeSegments));
    if(!destination.isSinkVertex() || destination.isSourceVertex()){
      throw new PlanItRunTimeException(
              "Destination bush does not have a sink centroid vertex as its root, this is not allowed");
    }
    this.destination = destination;
    this.bushData = new EntryExitBushTurnData(this);
  }

  /**
   * Copy constructor
   * 
   * @param bush to copy
   * @param deepCopy when true, create a eep copy, shallow copy otherwise
   */
  public DestinationBush(DestinationBush bush, boolean deepCopy) {
    super(bush, deepCopy);
    this.destination = bush.destination;
    this.bushData = deepCopy ? bush.bushData.deepClone() : bush.bushData.shallowClone();
  }

  /**
   * Determine the sending flow on the subpath given by the  subPathArray in order from start to finish.
   *
   * @param subPathArray                to use
   * @param flowAcceptanceFactors       to use
   * @return sendingFlowPcuH between start and end vertex following the sub-path
   */
  @Override
  public double determineSubPathSendingFlow(EdgeSegment[] subPathArray, double[] flowAcceptanceFactors) {

    int index = 0;

    /* determine flow on initial segment, from there on recursively traverse sub-path */
    var initialSubPathEdgeSegment = subPathArray[index];
    // must use minimum of incoming and outgoing flows because they maybe inconsistent due to overlapping other PASs
    // in case the subpath is only one link we should consider both to avoid not overestimating flow
    double subPathSendingFlow = Math.min(
        bushData.getTotalAcceptedFlowToPcuH(initialSubPathEdgeSegment, flowAcceptanceFactors),
        bushData.getTotalSendingFlowFromPcuH(initialSubPathEdgeSegment));
    if (subPathSendingFlow <= 0) {
      return subPathSendingFlow;
    }
    subPathSendingFlow = determineSubPathSendingFlow(
        subPathSendingFlow, 1, index, flowAcceptanceFactors, subPathArray);
    return subPathSendingFlow;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void syncToNetworkFlows(double[] flowAcceptanceFactors) {

    /* traverse form origin->destination */
    forEachTopologicalSortedVertex(isInverted(), currVertex -> {

      for (var entrySegment : currVertex.getEntryEdgeSegments()) {
        if (!contains(entrySegment)) {
          continue; // next vertex
        }

        double entryAcceptedFlow = bushData.getTotalAcceptedFlowToPcuH(entrySegment, flowAcceptanceFactors);
        double[] splittingRates = getSplittingRates(entrySegment);

        int splittingRateIndex = 0;
        for (var exitSegment : currVertex.getExitEdgeSegments()) {
          if (!contains(exitSegment)) {
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
    });
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
   * collect destination of this bush
   *
   * @return destination zone
   */
  public CentroidVertex getDestination() {
    return this.destination;
  }

  /**
   * Compute the min-max path tree rooted at the destination towards all origins given the provided (network wide)
   * costs. The provided costs are at the network level so should contain all the segments active in the bush
   *
   * @param excludeZeroFlowLinkSegmentsFromMaxPaths when true we do not consider link segments with zero flow when
   *                                                constructing max paths.
   * @param linkSegmentCosts              to use
   * @param totalTransportNetworkVertices number of vertices in overall network needed to be able to construct result
   *                                      per vertex based on id
   * @return minMaxPathResult, null if unable to complete
   */
  @Override
  public MinMaxPathResult computeMinMaxShortestPaths(boolean excludeZeroFlowLinkSegmentsFromMaxPaths,
          final double[] linkSegmentCosts, final int totalTransportNetworkVertices) {

    //todo: excludeZeroFlowLinkSegmentsFromMaxPaths ignored, see conjugate on how to use

    /* build min/max path tree */
    var minMaxBushPaths = new ShortestPathAcyclicMinMaxGeneralised(
            getDag(), requireTopologicalSortUpdate, linkSegmentCosts, totalTransportNetworkVertices);
    try {
      return minMaxBushPaths.executeAllToOne(getRootVertex());
    } catch (Exception e) {
      LOGGER.severe(String.format("Unable to complete minmax path three for destination-based bush ending at destination %s", getDestination().getXmlId()));
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void remove(EdgeSegment edgeSegment) {
    for(var exit : edgeSegment.getDownstreamVertex().getExitEdgeSegments()) {
      bushData.removeTurn(edgeSegment,exit);
    }
    if(getDag().containsEdgeSegment(edgeSegment)) {
      getDag().removeEdgeSegment(edgeSegment);
      requireTopologicalSortUpdate = true;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ShortestSearchType getShortestSearchType() {
    return ShortestSearchType.ALL_TO_ONE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CentroidVertex getRootZoneVertex() {
    return getDestination();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DestinationBush shallowClone() {
    return new DestinationBush(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DestinationBush deepClone() {
    return new DestinationBush(this, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public Set<CentroidVertex> getOriginVertices() {
    return (Set<CentroidVertex>) super.getOriginVertices();
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
  @Override
  public double addTurnSendingFlow(
      final EdgeSegment from,
      final EdgeSegment to,
      double addFlowPcuH) {

    if (addFlowPcuH > 0) {
      if (!contains(from)) {
        if (contains(from.getOppositeDirectionSegment())) {
          LOGGER.warning(String.format("Trying to add turn flow (%s,%s) on bush (%s) where the opposite direction (of segment %s) already is part of the bush, this break acyclicity",
              from.getXmlId(), to.getXmlId(), getRootZoneVertex().getParent().getParentZone().getIdsAsString(), from.getXmlId()));
        }
        getDag().addEdgeSegment(from);
        requireTopologicalSortUpdate = true;
      }
      if (!contains(to)) {
        if (contains(to.getOppositeDirectionSegment())) {
          LOGGER.warning(String.format("Trying to add turn flow (%s,%s) on bush (%s) where the opposite direction (of segment %s) already is part of the bush, this break acyclicity",
              from.getXmlId(), to.getXmlId(), getRootZoneVertex().getParent().getParentZone().getIdsAsString(), to.getXmlId()));
        }
        getDag().addEdgeSegment(to);
        requireTopologicalSortUpdate = true;
      }
    }
    return bushData.addTurnSendingFlow(from, to, addFlowPcuH);
  }

  @Override
  public <T> double determineConstrainedSubPathSendingFlow(
      EdgeSegment[] subPathArray,
      double[] onTheFlyFlowAcceptanceFactors,
      double[] nlNonConjugateFlowAcceptanceFactors,
      T bushConstrainedFlowData) {
    throw new PlanItRunTimeException("determineConstrainedSubPathSendingFlow not yet implemented in DestinationBush");
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
   * {@inheritDoc}
   */
  @Override
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
   * Collect the bush splitting rates for a given incoming edge segment. If entry segment has no flow,
   * zero splitting rates are returned for all turns
   *
   * @param entrySegment to use
   * @return splitting rates in primitive array in order of which one iterates over the outgoing edge segments
   * of the downstream from segment vertex
   */
  public double[] getSplittingRates(final EdgeSegment entrySegment) {
    return bushData.getSplittingRates(entrySegment);
  }

  /**
   * Remove a turn from the bush by removing it from the acyclic graph and removing any data associated with it.
   * Edge segments are also removed in case the no longer carry any flow
   *
   * @param fromEdgeSegment of the turn
   * @param toEdgeSegment   of the turn
   */
  public void removeTurn(final EdgeSegment fromEdgeSegment, final EdgeSegment toEdgeSegment) {
    bushData.removeTurn(fromEdgeSegment, toEdgeSegment);
    // LOGGER.info(String.format("Removing turn (%s,%s) from bush", fromEdgeSegment.getXmlId(), toEdgeSegment.getXmlId()));

    // we remove if no more flow gets sent out (unless it is a sink, i.e., destination in which case we never remove it)
    if (!Precision.positive(getSendingFlowPcuH(fromEdgeSegment))
        && !(fromEdgeSegment.getDownstreamVertex() instanceof CentroidVertex)) {
      removeEdgeSegment(fromEdgeSegment);
    }
    if (!Precision.positive(getSendingFlowPcuH(toEdgeSegment))
        && !(toEdgeSegment.getDownstreamVertex() instanceof CentroidVertex)) {
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
   * @param referenceVertex                to start breadth first search from as it is the point of coincidence of
   *                                       the alternative path (via labelled vertices) and bush
   * @param forbiddenInitialSegment        the first segment of the shortest path segment from the root, that we
   *                                       cannot use otherwise this alternative is partly overlapping
   * @param alternativeSubpathVertexLabels indicating the shortest (network) path at the reference vertex but not
   *                                       part of the bush at that point (different edge segment used)
   * @return vertex at which the two paths coincided again and the map (back link tree effectively) to extract the
   * path from this vertex to the reference vertex that was found using the breadth-first method
   */
  @Override
  public Pair<DirectedVertex, Map<DirectedVertex, EdgeSegment>> findBushAlternativeSubpathBfs(
      DirectedVertex referenceVertex,
      EdgeSegment forbiddenInitialSegment,
      final short[] alternativeSubpathVertexLabels) {

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
   * Determine the accepted flow between origin,destination vertex using the subpath given by the subPathArray in
   * order from start to finish. We utilise the initial sending flow on the first segment as the base flow which
   * is then reduced by the splitting rates and acceptance factor up to and including the final link segment
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
   * @param flowAcceptanceFactors to use
   * @return sendingFlowPcuH between start and end vertex following the sub-path
   */
  public double determineSubPathSendingFlow(
      EdgeSegment entrySegment, EdgeSegment[] subPathArray, double[] flowAcceptanceFactors) {

    int index = 0;

    /* determine flow from entry segment into initial segment, from there on recursively traverse sub-path */
    var initialSubPathEdgeSegment = subPathArray[index];
    double subPathSendingFlow = bushData.getTurnSendingFlowPcuH(entrySegment, initialSubPathEdgeSegment);
    if (subPathSendingFlow <= 0) {
      return subPathSendingFlow;
    }
    double flowAcceptanceFactor = flowAcceptanceFactors[(int)entrySegment.getId()];
    subPathSendingFlow = determineSubPathSendingFlow(
        subPathSendingFlow * flowAcceptanceFactor, flowAcceptanceFactor, index, flowAcceptanceFactors, subPathArray);
    return subPathSendingFlow;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TreeSet<EdgeSegment> performLowFlowBranchShifts(
          double flowThreshold, double[] flowAcceptanceFactors, boolean detailedLogging){

    // removed turn flows with multikey being entry and exit segment
    final MultiKeyMap<Object, Double> removedTurnFlows = new MultiKeyMap<>();
    // a removed turn flow does not mean a removed edge segment necessarily (as we avoid performing a full bush update
    // as a result, so track removed edge segments separately. We do so because removed edge segments allow us to deregister
    // bushes from PASs that have this edge segment (but after branch shift this bush is no longer eleigible for the PAS)
    final TreeSet<EdgeSegment> removedEdgeSegments = new TreeSet<>();

    /* traverse form origin->destination */
    forEachTopologicalSortedVertex(isInverted(), currVertex -> {

      Map<EdgeSegment, Double> exitSegmentsWithRemovedIncomingFlows = new TreeMap<>();
      for (var exitSegment : currVertex.getExitEdgeSegments()) {
        if (!contains(exitSegment)) {
          continue; // next vertex
        }
        if (exitSegment.getDownstreamVertex() instanceof CentroidVertex) {
          //todo not ideal in case we have a continuing shift that should remove this connector becuase it is no longer used
          // should gennerally not happen but it could... Conversely we don't want to remove ways to a destination
          continue;
        }

        // check if any preceding link flow was removed as a result of a threshold violation (see below).
        // if so, propagate this removal of flow before assessing if link is eligible for removal
        if (!removedTurnFlows.isEmpty()) {
          for (var entrySegment : currVertex.getEntryEdgeSegments()) {
            if (removedTurnFlows.keySet().stream().noneMatch(e -> e.getKey(1).equals(entrySegment)) ||
                    !containsTurnSendingFlow(entrySegment, exitSegment)) {
              continue;
            }
            double removedPortionIntoExit = bushData.getSplittingRate(entrySegment, exitSegment);
            double removedTotalOnEntry = removedTurnFlows.entrySet().stream().filter(
                    e -> e.getKey().getKey(1).equals(entrySegment)).mapToDouble(Map.Entry::getValue).sum();
            // incoming flow removed into this exit as a result of branch shift, track what was removed in total going
            // into current exit segment
            exitSegmentsWithRemovedIncomingFlows.put(exitSegment, exitSegmentsWithRemovedIncomingFlows.getOrDefault(exitSegment, 0.0) +
                    flowAcceptanceFactors[(int) exitSegment.getId()] * removedPortionIntoExit * removedTotalOnEntry);
          }
        }
      }

      // now determine which exit segments are eligible for removal (may be multiple) and if they are initiating a new
      // branch shift or not (when initiating a new shift, this may indicate merging with a continuing one, but this is dealt
      // with after)
      Map<EdgeSegment, Boolean> exitSegmentToRemove = new TreeMap<>();
      Set<EdgeSegment> exitSegmentsToTerminateTrackingButFinaliseUpstreamRemovals = new TreeSet<>();
      for (var exitSegment : currVertex.getExitEdgeSegments()) {
        double totalInflowPcuH = bushData.getTotalAcceptedFlowToPcuH(exitSegment, flowAcceptanceFactors);
        if(totalInflowPcuH<=0){
          continue;
        }

        // test for eligibility of removal based on the total inflow into the exit segment
        // (adjusted with any removed upstream flow)
        double removedExitSegmentIncomingFlow = exitSegmentsWithRemovedIncomingFlows.getOrDefault(exitSegment, 0.0);
        if ((totalInflowPcuH - removedExitSegmentIncomingFlow) < flowThreshold) {
          // below threshold hold, so initiate (or continue) a branch merge. Check if (new) flow has been merged into this link
          // from other incoming links, because if so, it is a new branch shift (possibly in addition to a continuing one)
          boolean initiateNewShift = IterableUtils.asStream(currVertex.getEntryEdgeSegments()).filter(
                  es -> removedTurnFlows.keySet().stream().noneMatch(k -> k.getKey(1).equals(es))).anyMatch(
                  es -> containsTurnSendingFlow(es, exitSegment));
          exitSegmentToRemove.put(exitSegment, initiateNewShift);
        } else if (removedExitSegmentIncomingFlow > 0 /* but above threshold for removal*/) {
            exitSegmentsToTerminateTrackingButFinaliseUpstreamRemovals.add(exitSegment);
        }
      }


      // perform continuation/new branch shift on nominated exit segments when eligible
      for (var candidate : exitSegmentToRemove.entrySet()) {
        EdgeSegment lowFlowSegment = candidate.getKey();
        boolean initiateNewShift = candidate.getValue();

        // safety --> we can only initiate an implicit shift if there is an alternative flow into another exit segment available
        // that is not a candidate for removal. if not then we cannot remove this flow for an implicit shift to another branch, so check this availability
        // note: In case we allow multiple branch shifts per bush we must exclude any removed segments from this selection as their turn sending flows may not have been removed yet
        // (this is triggered by the exit link, rather than the removed entry). As a result we should disallow multiple branch shifts per bush er iteration to avoid
        // such complexities (also because we not fully propagate the shift anyway potentially cuasing other problems).
        var alternativeUsedExitSegmentFlows = IterableUtils.asStream(lowFlowSegment.getUpstreamVertex().getExitEdgeSegments()).filter(
            es -> !exitSegmentToRemove.containsKey(es)).map(es ->
                Pair.of(es, bushData.getTotalAcceptedFlowToPcuH(es, flowAcceptanceFactors))).collect(Collectors.toList());
        if (initiateNewShift && alternativeUsedExitSegmentFlows.stream().mapToDouble(Pair::second).sum() <= 0) {
          // no other branch available to reallocate flow to, so we must maintain this flow despite it being low
          // this can happen 1) halfway along a corridor with alphas < 1 such that flow reduces below threshold halfway but without an
          // option to divert. Since, we may alos have conintuing removals at the same time, we flag the exit for temrination
          // in case this happens
          exitSegmentsToTerminateTrackingButFinaliseUpstreamRemovals.add(lowFlowSegment);
          continue;
        }

        // WHEN REACING THIS POINT WE ARE: continuing an existing or initiating a new branch shift on threshold compliant
        // segment that is to be removed...

        //remove edge segment explicitly, because otherwise it may not be removed if it still
        // has sending flow, but we can only deal with that later, so do it explicitly
        getDag().removeEdgeSegment(lowFlowSegment);
        removedEdgeSegments.add(lowFlowSegment);

        for (var entrySegment : currVertex.getEntryEdgeSegments()) {
          double turnFlow = getTurnSendingFlow(entrySegment, lowFlowSegment);
          if(turnFlow <= 0){
            continue;
          }

          // remove turn coming into this exit segment.
          boolean entryIsContinuingRemoval = !getDag().containsEdgeSegment(entrySegment);
          removeTurn(entrySegment,lowFlowSegment);
          if(!entryIsContinuingRemoval) {
            // when initiating a new shift, we only consider moving flow across from those incoming links that
            // were not already removed, i.e., the continuing portion of a branch shift cannot be redistributed because that
            // flow was already redistributed upstream

            // It may be that some entry segments have no current other used exit turns, while others do.
            // We therefore use the general distribution across exit segments as a proxy and shift
            // the removed turn flow to all these used exit segments (across all entries) to forcibly create
            // a used turn for such entries in case it does not exist (but already exists for other entries)
            double totalAcceptedExitFlow = alternativeUsedExitSegmentFlows.stream().mapToDouble(
                Pair::second).reduce(0.0, Double::sum);
            Map<EdgeSegment, Double> altExitSegmentFlowSplittingRates = new TreeMap<>();
            alternativeUsedExitSegmentFlows.forEach(
                e -> altExitSegmentFlowSplittingRates.put(e.first(), e.second() / totalAcceptedExitFlow));
            for (var altExitSegment : currVertex.getExitEdgeSegments()) {
              var splittingRate = altExitSegmentFlowSplittingRates.getOrDefault(altExitSegment, 0.0);
              if (splittingRate > 0) {
                double shiftedTurnFlow = turnFlow * splittingRate;
                addTurnSendingFlow(entrySegment, altExitSegment, shiftedTurnFlow);

                if (detailedLogging) {
                  LOGGER.info(String.format(
                      "%s branch shift for too low flows: shifted edge segment (%s) flow: %.10f) from exit link (%s) to other exit link (%s) from bush (%s)",
                          exitSegmentsWithRemovedIncomingFlows.getOrDefault(lowFlowSegment,0.0) > 0 ? "initiate additional" : "Initiate",
                      entrySegment.getIdsAsString(),
                      shiftedTurnFlow,
                          lowFlowSegment.getIdsAsString(),
                      altExitSegment.getIdsAsString(),
                      getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
                }
              }
            }
          }else if(exitSegmentsWithRemovedIncomingFlows.getOrDefault(lowFlowSegment, 0.0) > 0){
            //  continuing existing branch shift, but not initiating a new one, which will continue tracking of the removed flows
            if (detailedLogging) {
              LOGGER.info(String.format(
                  "Continuing Implicit branch shift: shifted flow: %.10f, from edge segment (%s) to other exit segment (%s) from bush (%s)",
                  turnFlow,
                  entrySegment.getIdsAsString(),
                      lowFlowSegment.getIdsAsString(),
                  getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
            }
          }

          // propagate removed link's flow in case it should lead to downstream removal of more link segments which
          //   may be required to avoid dangling links within the bush
          //   Note: since we are removing turns on-the-fly which affects the topological order, we should not create another
          //         topological iterator at this point as the bush's state is in flux and may be invalid temporarily. therefore
          //         we will track the to be removed flow as we go and deal with it here while traversing the bush instead
          removedTurnFlows.put(entrySegment, lowFlowSegment, turnFlow);
        }

      }

      // finalise by removal of preceding incoming turns from removed segments but without further tracking/removal required
      // because too much flow remains on exit segment despite removed incoming flow, so just remove the turns from removed
      // entry segments into this exit to finalise the shift but do not track removal propagation any further because
      // it is not required (no dangling links can occur)
      for (var nonCandidateWithPrecedingLowFlowremoval : exitSegmentsToTerminateTrackingButFinaliseUpstreamRemovals) {
        for (var entrySegment : currVertex.getEntryEdgeSegments()) {
          if (removedTurnFlows.keySet().stream().noneMatch(e -> e.getKey(1).equals(entrySegment)) ||
                  !containsTurnSendingFlow(entrySegment, nonCandidateWithPrecedingLowFlowremoval)) {
            continue;
          }
          removeTurn(entrySegment, nonCandidateWithPrecedingLowFlowremoval);
          if (detailedLogging) {
            LOGGER.info(String.format(
                    "Finalising branch shift; keep segment (%s) with above threshold flow : removed turn from edge segment (%s) into  (%s) from bush (%s)",
                    nonCandidateWithPrecedingLowFlowremoval.getIdsAsString(),
                    entrySegment.getIdsAsString(),
                    nonCandidateWithPrecedingLowFlowremoval.getIdsAsString(),
                    getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
          }
        }
      }
    });

    if (detailedLogging && !removedEdgeSegments.isEmpty()) {
      removedEdgeSegments.forEach( es -> LOGGER.info(String.format(
              "Branch shift removed edge segment (%s)",es.getIdsAsString())));
    }
    return removedEdgeSegments;
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
        isInverted() ? DirectedVertex.GET_ENTRY_EDGE_SEGMENTS : DirectedVertex.GET_EXIT_EDGE_SEGMENTS;
    final var getNextVertex =
        isInverted() ? EdgeSegment.getUpstreamVertex : EdgeSegment.getDownstreamVertex;

    while (!openVertices.isEmpty()) {
      var vertex = openVertices.poll();
      processed.add(vertex);
      for (EdgeSegment nextSegment : getNextEdgeSegments.apply(vertex)) {
        if (!contains(nextSegment)) {
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
    return "Bush: destination zone: " + getDestination().getParent().getParentZone().getXmlId() + "\n" + sb.toString();
  }

}

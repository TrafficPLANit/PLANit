package org.goplanit.assignment.common.bush;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.goplanit.algorithms.shortest.*;
import org.goplanit.assignment.ltm.sltm.consumer.ConjugateBushSyncBushFlowConsumer;
import org.goplanit.graph.directed.acyclic.ConjugateACyclicSubGraphImpl;
import org.goplanit.network.transport.ConjugateTransportModelNetwork;
import org.goplanit.utils.network.layer.physical.CompiledRelationMapping;
import org.goplanit.zoning.zonetozone.OdDemands;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.directed.*;
import org.goplanit.utils.graph.directed.acyclic.ConjugateACyclicSubGraph;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.IterableUtils;
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
public class ConjugateDestinationBush extends
    RootedBush<ConjugateDirectedVertex, ConjugateDirectedEdge, ConjugateEdgeSegment> {

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(ConjugateDestinationBush.class.getCanonicalName());

  private static final IdGroupingToken BUSH_SMOOTHING_TOKEN = IdGroupingToken.create("Bush MSRA smoothing");

  // BUSH GAP VARIABLES - todo:  provide a GapFunction Component instance to each bush to simplify
  private double demandScaledNetworkMinPathCostBush;
  private double demandScaledRealisedCostBush;
  private double demandScaledWithinBushMinPathCost;

  /** converged */
  public boolean converged = false; // temp

  /** track previous gap to see if bush is converging over assignment iterations */
  public double prevIterationInitialGap = Double.MAX_VALUE; // temp

  /**
   * Based on non-conjugate flow acceptance factors in the network obtain the acceptance factor for the
   * conjugate segment by looking at its original incoming segment. If it does not exist then 1 is returned
   *
   * @param conjugateEdgeSegment to get acceptance factor for
   * @param nonConjugateFlowAcceptanceFactors to use
   * @return conjugate converted acceptance factor
   */
  private static double getConjugateFlowAcceptanceFactor(
          ConjugateEdgeSegment conjugateEdgeSegment, double[] nonConjugateFlowAcceptanceFactors){
    if(!conjugateEdgeSegment.hasOriginalEntryEdgeSegment()){
      return 1;
    }else{
      return nonConjugateFlowAcceptanceFactors[
              (int) conjugateEdgeSegment.getOriginalAdjacentEdgeSegments().first().getId()];
    }
  }

  /**
   * Determine the sending flow between origin,destination vertex using the subpath given by the subPathArray in
   * order from start to finish. We utilise the initial sending flow on the indexed conjugate segment as the base
   * flow which is then followed along the subpath through the bush splitting rates up to the final link segment
   *
   * @param subPathAcceptedFlow accepted flow so far
   * @param compoundedFlowAcceptanceScalingFactor combined multiplied alphas so far
   * @param index              offset to start in array with
   * @param nonConjugateFlowAcceptanceFactors to use
   * @param subPathArray       to extract path from*
   * @return sendingFlowPcuH between index and end vertex following the sub-path
   */
  private double determineSubPathSendingFlow(
          double subPathAcceptedFlow,
          double compoundedFlowAcceptanceScalingFactor,
          int index,
          double[] nonConjugateFlowAcceptanceFactors,
          final ConjugateEdgeSegment[] subPathArray) {

    if(subPathAcceptedFlow <= 0.0){
      return subPathAcceptedFlow;
    }

    var currConjugateSegment = subPathArray[index++];
    // restrict by what is available on our subpath
    var currSendingFlow = bushData.getTurnSendingFlowPcuH(currConjugateSegment);
    double subPathSendingFlow = Math.min(subPathAcceptedFlow, currSendingFlow);
    if (index < subPathArray.length && Precision.positive(subPathAcceptedFlow)) {
      // restrict by what is available on our subpath
      double flowAcceptanceFactor =
              getConjugateFlowAcceptanceFactor(currConjugateSegment, nonConjugateFlowAcceptanceFactors);
      return determineSubPathSendingFlow(
              subPathSendingFlow * flowAcceptanceFactor,
              compoundedFlowAcceptanceScalingFactor * flowAcceptanceFactor,
              index,
              nonConjugateFlowAcceptanceFactors,
              subPathArray);
    }

    // done, rescale to original sending flow using reciprocal of compounded flow acceptance factors
    return subPathSendingFlow * 1/(compoundedFlowAcceptanceScalingFactor);
  }

  private double determineConstrainedSubPathSendingFlow(
      double subPathAcceptedFlow,
      double compoundedFlowAcceptanceScalingFactor,
      int index,
      double[] onTheFlyFlowAcceptanceFactors,
      double[] nlNonConjugateFlowAcceptanceFactors,
      final ConjugateEdgeSegment[] subPathArray,
      ConjugateBushTurnData bushConstrainedFlowData) {

    if(subPathAcceptedFlow <= 0.0){
      return subPathAcceptedFlow;
    }

    var currConjugateSegment = subPathArray[index++];

    // restrict by what is available on our subpath
    double constrainedSendingFlow = bushConstrainedFlowData.getTurnSendingFlowPcuH(currConjugateSegment);
    double currSendingFlow = bushData.getTurnSendingFlowPcuH(currConjugateSegment);
    if(constrainedSendingFlow > 0) {
      currSendingFlow = Math.min(constrainedSendingFlow, currSendingFlow);
    }

    double subPathSendingFlow = Math.min(subPathAcceptedFlow, currSendingFlow);
    if (index < subPathArray.length && Precision.positive(subPathAcceptedFlow)) {

      // restrict by what is available on our subpath
      double mostRecentFlowAcceptanceFactor =
          getConjugateFlowAcceptanceFactor(currConjugateSegment, onTheFlyFlowAcceptanceFactors);
      double networkLoadingFlowAcceptanceFactor =
          Math.min(1, getConjugateFlowAcceptanceFactor(currConjugateSegment, nlNonConjugateFlowAcceptanceFactors));
      double flowAcceptanceFactor = Math.min(mostRecentFlowAcceptanceFactor,networkLoadingFlowAcceptanceFactor);

      return determineConstrainedSubPathSendingFlow(
          subPathSendingFlow * flowAcceptanceFactor,
          compoundedFlowAcceptanceScalingFactor * flowAcceptanceFactor,
          index,
          onTheFlyFlowAcceptanceFactors,
          nlNonConjugateFlowAcceptanceFactors,
          subPathArray,
          bushConstrainedFlowData);
    }

    // done, rescale to original sending flow using reciprocal of compounded flow acceptance factors
    return subPathSendingFlow * 1/(compoundedFlowAcceptanceScalingFactor);
  }

  /** destination of this conjugate bush */
  protected final CentroidVertex destination;

  /** track bush specific data */
  public final ConjugateBushTurnData bushData;

  /** inverse mapping from turn edge segments (double key) to conjugate edge segment */
  protected final CompiledRelationMapping<ConjugateEdgeSegment> turn2ConjugateSegmentMapping;

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateACyclicSubGraph getDag() {
    return (ConjugateACyclicSubGraph) super.getDag();
  }

  /**
   * Run as BFS search.
   * {@inheritDoc}
   *
   *
   */
  @Override
  public Pair<ConjugateDirectedVertex, Map<ConjugateDirectedVertex, ConjugateEdgeSegment>>
  findBushAlternativeSubpathBfs(
          ConjugateDirectedVertex referenceVertex,
          ConjugateEdgeSegment forbiddenInitialSegment,
          short[] alternativeSubpathVertexLabels) {

    // cannot use the initial segment that is part of the cheapest option.
    // Note that we cannot check for the -1 marking here because it is possible that the shortest alternative loops
    // around and the alternative we are looking is exactly 1 link long starting at vertex marked with 1 and ending
    // at vertex marked -1 so actual initial rival edge segment is needed for exclusion
    Predicate<ConjugateEdgeSegment> initialInclusionCondition = es -> !es.equals(forbiddenInitialSegment);

    // only consider (original) turns with positive flow on bush
    BiPredicate<ConjugateEdgeSegment, ConjugateEdgeSegment> regularInclusionCondition =
            (prevEs,es) -> bushData.containsTurnSendingFlow(es);

    // terminate when shortest path reconnects to the bush
    BiPredicate<ConjugateDirectedVertex, ConjugateEdgeSegment> terminationCondition = (v, prevEs) ->
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
     * no result could be found, only possible when cycle is detected before reaching origin Not sure this will
     * actually happen, so created warning to check, when it does happen investigate and see if this expected
     * behaviour (if so remove statement). this would equate to finding a vertex marked with a '1' in Xie & Xie,
     * which I do not do because I  don't think it is needed, but I might be wrong.
     */
    if(result== null || result.first() == null) {
      LOGGER.warning(String.format(
              "Cycle found when finding alternative subpath on conjugate bush (%s) merging at conjugate vertex (%s)",
              getRootZone().getIdsAsString(),
              referenceVertex.getIdsAsString()));
      result = getDag().breadthFirstSearch(
              referenceVertex,
              invertBfs,
              initialInclusionCondition,
              regularInclusionCondition,
              terminationCondition);
    }
    return result;
  }

  /**
   * Constructor. It is expected that all provided root vertices represent edges in the original network leading
   * to a single root.
   *
   * @param idToken          the token to base the id generation on
   * @param destinationCentroidVertex      this conjugate destination bush has rooted conjugate vertices for
   * @param rootVertex       this conjugate node represents the root vertex as it is the dummy node from which all
   *                         initial turns enter/exit the conjugate network from the conjugate destination
   * @param maxSubGraphConjugateSegments The maximum number of conjugate edge segments, i.e. turns, the conjugate bush
   *                                     can at most register given the parent network it is a subset of
   * @param turn2ConjugateSegmentMapping to use
   */
  public ConjugateDestinationBush(
      final IdGroupingToken idToken,
      final CentroidVertex destinationCentroidVertex,
      ConjugateConnectoidNode rootVertex,
      int maxSubGraphConjugateSegments,
      final CompiledRelationMapping<ConjugateEdgeSegment> turn2ConjugateSegmentMapping) {
    super(new ConjugateACyclicSubGraphImpl(idToken, rootVertex, true /* inverted */, maxSubGraphConjugateSegments));
    this.bushData = new ConjugateBushTurnData(this);
    if(!destinationCentroidVertex.isSinkVertex() || destinationCentroidVertex.isSourceVertex()){
      throw new PlanItRunTimeException(
              "Conjugate Destination bush does not have a sink centroid vertex as its root, this is not allowed");
    }
    this.destination = destinationCentroidVertex;
    this.turn2ConjugateSegmentMapping = turn2ConjugateSegmentMapping;

    this.demandScaledNetworkMinPathCostBush = 0;
    this.demandScaledRealisedCostBush = 0;
    this.demandScaledWithinBushMinPathCost = 0;
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
    this.turn2ConjugateSegmentMapping = bush.turn2ConjugateSegmentMapping.copy();

    this.demandScaledNetworkMinPathCostBush = 0;
    this.demandScaledRealisedCostBush = 0;
    this.demandScaledWithinBushMinPathCost = 0;

    throw new PlanItRunTimeException("incomplete");
  }

  /**
   * Compute the min-max path tree rooted in location depending on underlying dag configuration of derived
   * implementation and given the provided conjugate (network wide) costs. The provided costs are at the conjugate
   * network level so should contain all the conjugate segments active in the bush
   *
   * @param excludeZeroFlowLinkSegmentsFromMaxPaths when true we do not consider link segments with zero flow when
   *                                                constructing max paths.
   * @param conjugateLinkSegmentCosts to use
   * @param totalConjugateVertices    needed to be able to create primitive array recording the (partial) subgraph
   *                                  backward conjugate link segment results (efficiently)
   * @return minMaxPathResult, null if unable to complete
   */
  @Override
  public MinMaxPathResult computeMinMaxShortestPaths(boolean excludeZeroFlowLinkSegmentsFromMaxPaths,
          final double[] conjugateLinkSegmentCosts, final int totalConjugateVertices) {
    //todo: effectively duplicated from non conjugate implementation, consider consolidating

    /* build min/max path tree */
    var minMaxBushPaths = new ShortestPathAcyclicMinMaxGeneralised(
            getDag(), requireTopologicalSortUpdate, conjugateLinkSegmentCosts, totalConjugateVertices);
    try {

      if(excludeZeroFlowLinkSegmentsFromMaxPaths) {
        // make sure that for the max tree we only consider turns with flow otherwise there will be nothing
        // to shift anyway
        Predicate<EdgeSegment> maxPathEdgeSegmentFilter = turn -> getTurnSendingFlow((ConjugateEdgeSegment) turn) > 0;
        //return minMaxBushPaths.executeAllToOne(getRootVertex());
        return minMaxBushPaths.executeAllToOneWithFilter(
            getRootVertex(), null, maxPathEdgeSegmentFilter);
      }else{
        return minMaxBushPaths.executeAllToOne(getRootVertex());
      }
    } catch (Exception e) {
      LOGGER.severe(String.format("Unable to complete minmax path three for conjugate destination-based bush ending at " +
              "destination %s", getDestination().getXmlId()));
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public Set<ConjugateDirectedVertex> getOriginVertices() {
    return (Set<ConjugateDirectedVertex>) super.getOriginVertices();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double addTurnSendingFlow(EdgeSegment from, EdgeSegment to, double addFlowPcuH) {
    var conjugateSegment = turn2ConjugateSegmentMapping.get(from.getId(),to.getId());
    return addTurnSendingFlow(conjugateSegment, addFlowPcuH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getSendingFlowPcuH(final ConjugateEdgeSegment edgeSegment) {
    return bushData.getTurnSendingFlowPcuH(edgeSegment);
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
   * Add turn sending flow to the bush. In case the turn does not yet exist on the bush it is newly registered.
   * If it does exist and there is already flow present, the provided flow is added to it.
   * If by adding the flow (can be negative) the turn no longer has any flow, it is removed
   *
   * @param turn             the turn in conjugate form
   * @param addFlowPcuH      to add
   * @return new turn sending flow after adding given flow
   */
  public double addTurnSendingFlow(final ConjugateEdgeSegment turn, double addFlowPcuH) {

    if (addFlowPcuH > 0) {
      if (!containsConjugateSegment(turn)) {
        if(LOGGER.getLevel() == Level.FINE) {
          LOGGER.fine(String.format("Add turn (%s) in conjugate bush (%s), but this should have already happened earlier, " +
              "should not happen", turn.getIdsAsString(), getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
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
   * @param conjugateNode to collect sending flow for
   * @return bush sending flow
   */
  public double getSendingFlowPcuH(final ConjugateDirectedVertex conjugateNode) {
    return bushData.getTotalSendingFlowFromPcuH(conjugateNode);
  }

  /**
   * Verify if any sending flow exists that traverses the conjugate node (original edge segment) in the conjugate bush,
   *
   * @param conjugateNode to verify  for
   * @return true if present, false otherwise
   */
  public boolean containsSendingFlow(final ConjugateDirectedVertex conjugateNode) {
    return bushData.containsSendingFlow(conjugateNode);
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
   * Collect the bush splitting rates for a given conjugate node (original incoming edge segment). If no flow,
   * zero splitting rates are returned
   * 
   * @param conjugateVertex to use
   * @return splitting rates in primitive array in order of which one iterates over the outgoing (conjugate)
   * edge segments
   */
  public double[] getSplittingRates(final ConjugateDirectedVertex conjugateVertex) {
    return bushData.getSplittingRates(conjugateVertex, getDag());
  }

  /**
   * Remove a turn from the conjugate bush
   *
   * @param turn of the turn
   */
  public void remove(final ConjugateEdgeSegment turn) {
    bushData.removeTurnData(turn);

    if(contains(turn)) {
      getDag().removeEdgeSegment(turn);
      requireTopologicalSortUpdate = true;
    }
  }

  /**
   * Verify if the bush contains any conjugate edge segment (turn) of the conjugate edge in either direction
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
   * @param subPathArray                      to use
   * @param nonConjugateFlowAcceptanceFactors to use
   * @return sendingFlowPcuH between start and end vertex following the sub-path
   */
  @Override
  public double determineSubPathSendingFlow(
      ConjugateEdgeSegment[] subPathArray,
      double[] nonConjugateFlowAcceptanceFactors) {
    int index = 0;

    /* determine flow on initial segment, from there on recursively traverse sub-path */
    var initialSubPathEdgeSegment = subPathArray[index];
    double subPathSendingFlow = bushData.getTurnSendingFlowPcuH(initialSubPathEdgeSegment);
    if (subPathSendingFlow <= 0) {
      return subPathSendingFlow;
    }
    subPathSendingFlow = determineSubPathSendingFlow(
            subPathSendingFlow,
            1,
            index,
            nonConjugateFlowAcceptanceFactors,
            subPathArray);
    return subPathSendingFlow;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> double determineConstrainedSubPathSendingFlow(
      ConjugateEdgeSegment[] subPathArray,
      double[] onTheFlyFlowAcceptanceFactors,
      double[] nlNonConjugateFlowAcceptanceFactors,
      T bushConstrainedFlow){
    if(!(bushConstrainedFlow instanceof ConjugateBushTurnData)){
      throw new PlanItRunTimeException("invalid bush turn data type");
    }

    int index = 0;
    var conjBushConstrainedFlowData = (ConjugateBushTurnData)bushConstrainedFlow;

    /* determine flow on initial segment, from there on recursively traverse sub-path */
    var initialSubPathEdgeSegment = subPathArray[index];

    // note 0 is used if no entry exists in ConjugateBushTurnData, so that's is why we check for larger than zero
    // todo: ugly to use 0 as magic number, ideally change at some point if possible
    double constrainedSendingFlow = conjBushConstrainedFlowData.getTurnSendingFlowPcuH(initialSubPathEdgeSegment);
    double subPathSendingFlow = bushData.getTurnSendingFlowPcuH(initialSubPathEdgeSegment);
    if(constrainedSendingFlow > 0) {
      subPathSendingFlow = Math.min(constrainedSendingFlow, subPathSendingFlow);
    }

    if (subPathSendingFlow <= 0) {
      return subPathSendingFlow;
    }
    subPathSendingFlow = determineConstrainedSubPathSendingFlow(
        subPathSendingFlow,
        1,
        index,
        onTheFlyFlowAcceptanceFactors,
        nlNonConjugateFlowAcceptanceFactors,
        subPathArray,
        conjBushConstrainedFlowData);
    return subPathSendingFlow;
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
    return getRootZoneVertex();
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

  /**
   * Verify if the bush contains the given turn segment.
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
   */
  @Override
  public void syncToNetworkFlows(double[] originalNetworkFlowAcceptanceFactors) {
    var syncBushToNetworkLoadingFlowsConsumer = new ConjugateBushSyncBushFlowConsumer(
        this, originalNetworkFlowAcceptanceFactors);
    forEachTopologicalSortedVertex(true, syncBushToNetworkLoadingFlowsConsumer);
  }

  @Override
  public String toString() {
    /* log all edge segments with flow on bush */
    return "Conj Bush ("+getRootZone().getIdsAsString()+"): [ " + IterableUtils.asStream(bushData).filter(
        e -> e.getValue() > 0).map(
            e -> e.getKey().getXmlId()).sorted().collect(
                Collectors.joining(",")) + " ]";
  }

  // todo: replace with proper gap function for now just inject raw data
  /** setter
   *
   * @param demandScaledNetworkMinPathCostBush  value
   */
  public void setNetworkBasedMinCostForGap(double demandScaledNetworkMinPathCostBush) {
    this.demandScaledNetworkMinPathCostBush = demandScaledNetworkMinPathCostBush;
  }

  /** getter
   *
   * @return result
   */
  public double getNetworkBasedMinCostForGap() {
    return this.demandScaledNetworkMinPathCostBush;
  }

  /** setter
   *
   * @param demandScaledWithinBushMinPathCost  value
   */
  public void setWithinBushMinCostForGap(double demandScaledWithinBushMinPathCost) {
    this.demandScaledWithinBushMinPathCost = demandScaledWithinBushMinPathCost;
  }

  /** getter
   *
   * @return result
   */
  public double getWithinBushMinCostForGap() {
    return this.demandScaledWithinBushMinPathCost;
  }

  /** setter
   *
   * @param demandScaledRealisedCostBush  value
   */
  public void setRealisedCostForGap(double demandScaledRealisedCostBush) {
    this.demandScaledRealisedCostBush = demandScaledRealisedCostBush;
  }

  /** getter
   *
   * @return result
   */
  public double getRealisedCostForGap() {
    return this.demandScaledRealisedCostBush;
  }

  /** getter
   *
   * @param thresholdGap to use
   * @return result
   */
  public boolean isConvergedBeyond(double thresholdGap){
    double bushGap = (demandScaledRealisedCostBush - demandScaledNetworkMinPathCostBush)/ demandScaledNetworkMinPathCostBush;
    // only apply margin if not interfering with precision of check
    double margin = thresholdGap < Precision.EPSILON_12 ? 0 : Precision.EPSILON_12;
    return (bushGap + margin) < thresholdGap;
  }

  /**
   * Calculate within-bush OD min cost and realised cost (both scaled yb unconstrained demand) for gap based on
   * non-discontinuous costs. Result stored on bush itself.
   *
   * @param conjugateTransportModelNetwork        to use
   * @param odDemands                             to use
   * @param conjLinkSegmentCosts                  to use (assumed without considering discontinuities)
   */
  public void updateWithinBushMinCostAndRealisedCostGapInformation(
      ConjugateTransportModelNetwork conjugateTransportModelNetwork,
      OdDemands odDemands,
      double[] conjLinkSegmentCosts) {

    boolean excludeZeroFlowLinksFromMaxPaths = true;
    var bushMinMaxTree = computeMinMaxShortestPaths(excludeZeroFlowLinksFromMaxPaths,
        conjLinkSegmentCosts, conjugateTransportModelNetwork.getNumberOfVerticesAllLayers());

    double totalWithinBushMinPathCost = 0;
    double totalWithinBushMaxPathCost = 0;
    bushMinMaxTree.setMinPathState(false);
    var destination = getDestination().getParent().getParentZone();
    for (var originVertex : getOriginVertices()) {
      var origin = ((ConjugateConnectoidNode)originVertex).getCentroidVertex().getParent().getParentZone();
      double odDemand = odDemands.getValue(origin, destination);
      if(odDemand <= 0.0){
        continue;
      }
      double maxOdCost = bushMinMaxTree.getCostToReach(originVertex);
      double scaledMaxCostBushOd = maxOdCost * odDemand;
      totalWithinBushMaxPathCost += scaledMaxCostBushOd;

      double minOdCost = bushMinMaxTree.getMinCostToReach(originVertex);
      totalWithinBushMinPathCost += minOdCost * odDemand;
    }
    // update
    setRealisedCostForGap(totalWithinBushMaxPathCost);
    setWithinBushMinCostForGap(totalWithinBushMinPathCost);
  }
}

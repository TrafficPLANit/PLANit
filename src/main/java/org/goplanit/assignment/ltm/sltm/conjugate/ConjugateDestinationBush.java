package org.goplanit.assignment.ltm.sltm.conjugate;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.algorithms.shortest.MinMaxPathResult;
import org.goplanit.algorithms.shortest.ShortestPathAcyclicMinMaxGeneralised;
import org.goplanit.algorithms.shortest.ShortestSearchType;
import org.goplanit.assignment.ltm.sltm.BushFlowLabel;
import org.goplanit.assignment.ltm.sltm.RootedBush;
import org.goplanit.graph.directed.acyclic.ConjugateACyclicSubGraphImpl;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.directed.ConjugateDirectedEdge;
import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
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
public class ConjugateDestinationBush extends RootedBush<ConjugateDirectedVertex, ConjugateEdgeSegment> {

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(ConjugateDestinationBush.class.getCanonicalName());

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
   * todo: (not todo) NOTE to self, last synced with RootLabelledBush/DestinationBush implementation on 12/12
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

    if (index < subPathArray.length && Precision.positive(subPathAcceptedFlow)) {
      var currSendingFlow = bushData.getTurnSendingFlowPcuH(currConjugateSegment);
      // restrict by what is available on our subpath
      double subPathSendingFlow = Math.min(subPathAcceptedFlow, currSendingFlow);
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
    return subPathAcceptedFlow * 1/(compoundedFlowAcceptanceScalingFactor);
  }

  /** destination of this conjugate bush */
  protected final CentroidVertex destination;

  /** track bush specific data */
  protected final ConjugateBushTurnData bushData;

  /** inverse mapping from turn edge segments (double key) to conjugate edge segment */
  protected final MultiKeyMap<Object, ConjugateEdgeSegment> turn2ConjugateSegmentMapping;

  /**
   * {@inheritDoc}
   */
  @Override
  protected ConjugateACyclicSubGraph getDag() {
    return (ConjugateACyclicSubGraph) super.getDag();
  }

  /**
   * Run as BFS search.
   * {@inheritDoc}
   *
   */
  @Override
  public Pair<ConjugateDirectedVertex, Map<ConjugateDirectedVertex, ConjugateEdgeSegment>>
  findBushAlternativeSubpathByBackLinkTree(
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
     * no result could be found, only possible when cycle is detected before reaching origin Not sure this will actually happen, so created warning to check, when it does happen
     * investigate and see if this expected behaviour (if so remove statement). this would equate to finding a vertex marked with a '1' in Xie & Xie, which I do not do because I
     * don't think it is needed, but I might be wrong.
     */
    if(result== null || result.first() == null) {
      LOGGER.warning(String.format(
              "Cycle found when finding alternative subpath on conjugate bush merging at conjugate vertex (%s)",
              referenceVertex.getIdsAsString()));
    }
    return result;
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
   * @param turn2ConjugateSegmentMapping to use
   */
  public ConjugateDestinationBush(
          final IdGroupingToken idToken,
          final CentroidVertex destination,
          ConjugateConnectoidNode rootVertex,
          int maxSubGraphConjugateSegments,
          final MultiKeyMap<Object, ConjugateEdgeSegment> turn2ConjugateSegmentMapping) {
    super(new ConjugateACyclicSubGraphImpl(idToken, rootVertex, true /* inverted */, maxSubGraphConjugateSegments));
    this.bushData = new ConjugateBushTurnData(this);
    this.destination = destination;
    this.turn2ConjugateSegmentMapping = turn2ConjugateSegmentMapping;
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
    this.turn2ConjugateSegmentMapping = bush.turn2ConjugateSegmentMapping.clone();
  }

  /**
   * Compute the min-max path tree rooted in location depending on underlying dag configuration of derived
   * implementation and given the provided conjugate (network wide) costs. The provided costs are at the conjugate
   * network level so should contain all the conjugate segments active in the bush
   * 
   * @param conjugateLinkSegmentCosts to use
   * @param totalConjugateVertices    needed to be able to create primitive array recording the (partial) subgraph
   *                                  backward conjugate link segment results (efficiently)
   * @return minMaxPathResult, null if unable to complete
   */
  public MinMaxPathResult computeMinMaxShortestPaths(
          final double[] conjugateLinkSegmentCosts, final int totalConjugateVertices) {
    //todo: effectively duplicated from non conjugate implementation, consider consolidating

    /* build min/max path tree */
    var minMaxBushPaths = new ShortestPathAcyclicMinMaxGeneralised(
            getDag(), requireTopologicalSortUpdate, conjugateLinkSegmentCosts, totalConjugateVertices);
    try {
      return minMaxBushPaths.executeAllToOne(getRootVertex());
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
    var conjugateSegment = turn2ConjugateSegmentMapping.get(from,to);

    if (addFlowPcuH > 0) {
      if (!contains(conjugateSegment)) {
        if (contains(conjugateSegment.getOppositeDirectionSegment())) {
          LOGGER.warning(String.format("Trying to add turn flow (%s,%s) on conjugate bush (%s) where the opposite direction (of segment %s) already is part of the bush, this breaks acyclicity",
                  from.getXmlId(), to.getXmlId(), getRootZoneVertex().getParent().getParentZone().getIdsAsString(), from.getXmlId()));
        }
        getDag().addEdgeSegment(conjugateSegment);
        requireTopologicalSortUpdate = true;
      }
    }
    return bushData.addTurnSendingFlow(conjugateSegment, addFlowPcuH);
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
  @Override
  public double determineSubPathSendingFlow(
          ConjugateEdgeSegment[] subPathArray, double[] nonConjugateFlowAcceptanceFactors) {
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
   * Option ot choose whether to log conjugate ids instead of original network ids or vice versa
   *
   * @param asUnderlyingOriginal when true log underlying original ids, otherwise the conjugate ids
   * @return created string
   */
  public String toString(boolean asUnderlyingOriginal) {
    var sb = asUnderlyingOriginal ?
        new StringBuilder("Original registered segments [") : new StringBuilder("Conjugate registered segments [");

    /* log all original edge segments on conjugate bush */
    var root = getRootVertex();
    Queue<ConjugateDirectedVertex> openVertices = new PriorityQueue<>();
    openVertices.add(root);
    Set<ConjugateDirectedVertex> processed = new HashSet<>();

    // inverted bush so work backwards
    final var getNextEdgeSegments = ConjugateDirectedVertex.getEntryEdgeSegments;
    final var getNextVertex = ConjugateEdgeSegment.getUpstreamVertex;

    final Set<Long> processedIds = new HashSet<>(); // only required if logging conjugate form segments
    while (!openVertices.isEmpty()) {
      var vertex = openVertices.poll();
      processed.add(vertex);

      if(asUnderlyingOriginal && vertex.hasOriginalEdge()) {
        vertex.getOriginalEdge().forEachSegment(es -> sb.append("("+es.getIdsAsString()+")").append(","));
      }else{
        vertex.getEdges().forEach(e -> e.forEachSegment( es -> {
          if(processedIds.contains(es.getId())){
            return;
          }
          sb.append("("+es.getIdsAsString()+")").append(",");
          processedIds.add(es.getId());
        }));

      }
      for (var nextSegment : getNextEdgeSegments.apply(vertex)) {
        if(!contains((ConjugateEdgeSegment) nextSegment)) {
          continue;
        }
        ConjugateDirectedVertex nextVertex = (ConjugateDirectedVertex) getNextVertex.apply(nextSegment);
        if (processed.contains(nextVertex)) {
          continue;
        }
        openVertices.add(nextVertex);
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
  public String toString() {
    return toString(true /* asUnderlyingOriginal */);
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
    ConjugateDirectedVertex currConjugateVertex = null;

    /* pass over conjugate bush in topological order updating turn sending flows based on flow acceptance factors
    *  these turn flows inform the network level splitting rates now that they are consistent with network loading */
    final boolean allowTurnRemoval = false;
    while (conjugateVertexIter.hasNext()) {
      currConjugateVertex = conjugateVertexIter.next();
      double conjugateVertexAcceptedFlow =
              bushData.getTotalAcceptedFlowToPcuH(currConjugateVertex, originalNetworkFlowAcceptanceFactors);

      /*
       * bush splitting rates by [conjugate exit segment index] - splitting rates are computed based on turn
       * flows but placed in new array. So once we have the splitting rates we can safely update the turn
       * flows without affecting these splitting rates
       */
      double[] bushSplittingRates = getSplittingRates(currConjugateVertex);
      int index = -1;
      for (var turnSegment : currConjugateVertex.getExitEdgeSegments()) {
        ++index;
        if (!containsConjugateSegment(turnSegment)) {
          continue;
        }
        double currTurnSplittingRate = bushSplittingRates[index];
        if (currTurnSplittingRate > 0) {
          double bushTurnLabeledAcceptedFlow = conjugateVertexAcceptedFlow * currTurnSplittingRate;
          bushData.setTurnSendingFlow(turnSegment, bushTurnLabeledAcceptedFlow, allowTurnRemoval);
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

    //todo: implemented in conjugate context, but not tested yet. Should be checked for correctness

    // removed turn flows with multikey being entry and exit conjugate segments
    final Map<ConjugateEdgeSegment, Double> conjSegmentRemovedFlows = new TreeMap<>();

    // in conjugate setting a removed turn flow is a removed conjugate edge segment
    final TreeSet<ConjugateEdgeSegment> removedConjSegments = new TreeSet<>();

    /* traverse form origin->destination */
    forEachTopologicalSortedVertex(isInverted(), currVertex -> {

      int index = 0;
      double[] splittingRates = bushData.getSplittingRates(currVertex);

      for (ConjugateEdgeSegment exitSegment : currVertex.getExitEdgeSegments()) {
        if (!contains(exitSegment)) {
          ++index;
          continue; // next vertex
        }
        if (exitSegment.getDownstreamVertex() instanceof ConjugateConnectoidNode &&
                ((ConjugateConnectoidNode)exitSegment.getDownstreamVertex()).getCentroidVertex() != null) {
          // not ideal in case we have a continuing shift that should remove this connector because it is no
          // longer used should generally not happen but it could... Conversely, we don't want to remove ways
          // to a destination
          ++index;
          continue;
        }

        // check if any preceding link flow was removed as a result of a threshold violation (see below).
        // if so, propagate this removal of flow before assessing if link is eligible for removal
        if (!conjSegmentRemovedFlows.isEmpty()) {
          for (var entrySegment : currVertex.getEntryEdgeSegments()) {
            if (!conjSegmentRemovedFlows.containsKey(entrySegment)) {
              continue;
            }
            double removedPortionIntoExit = splittingRates[index];
            double removedTotalOnEntry = conjSegmentRemovedFlows.get(entrySegment);
            double entryAcceptanceFactor =
                    nonConjugateFlowAcceptanceFactors[
                            (int) exitSegment.getOriginalAdjacentEdgeSegments().first().getId()];
            // incoming flow removed into this exit as a result of branch shift, track what was removed in total going
            // into current exit segment as continuing. Only used to distinguish between new to be removed flow and already
            // removed flow from upstream
            conjSegmentRemovedFlows.put(exitSegment,
                    conjSegmentRemovedFlows.getOrDefault(exitSegment, 0.0) +
                            removedPortionIntoExit * removedTotalOnEntry * entryAcceptanceFactor);
          }
        }
        ++index;
      }

      // now determine which turns (conj segments) are eligible for removal (may be multiple) and if they are
      // initiating a new branch shift or not (when initiating a new shift, this may indicate merging with a
      // continuing one, but this is dealt with after)
      Map<ConjugateEdgeSegment, Boolean> conjSegmentToRemove = new TreeMap<>();
      for (var conjExitSegment : currVertex.getExitEdgeSegments()) {
        double originalTurnSendingPcuH = bushData.getTurnSendingFlowPcuH(conjExitSegment);
        if(originalTurnSendingPcuH<=0){
          continue;
        }

        // test for eligibility of removal based on the total inflow into the exit segment
        // (adjusted with any removed upstream flow)
        double conjSegmentRemovedUpstreamFlow =
                conjSegmentRemovedFlows.getOrDefault(conjExitSegment, 0.0);
        if ((originalTurnSendingPcuH - conjSegmentRemovedUpstreamFlow) < flowThreshold) {
          // below threshold hold, so initiate (or continue) a branch merge. Check if (new) flow has been merged into
          // this link from other incoming links, because if so, it is a new branch shift (possibly in addition to a
          // continuing one)
          boolean initiateNewShift = IterableUtils.asStream(currVertex.getEntryEdgeSegments()).filter(
                  es -> !conjSegmentRemovedFlows.containsKey(es)).anyMatch(this::containsTurnSendingFlow);
          conjSegmentToRemove.put(conjExitSegment, initiateNewShift);
        }
      }

      // perform continuation/new branch shift on nominated exit segments when eligible
      // track actual flow to redistribute after removal of low flow candidates have been removed
      double flowtoRedistribute = 0;
      for (var candidate : conjSegmentToRemove.entrySet()) {
        ConjugateEdgeSegment lowFlowConjSegment = candidate.getKey();
        boolean initiateNewShift = candidate.getValue();

        // safety --> we can only initiate a shift if there is an alternative flow into another exit segment
        // available that is not a candidate for removal to shift to. if not then we cannot remove this flow for
        // an implicit shift to another branch, so check this availability.
        List<Pair<ConjugateEdgeSegment, Double>> alternativeUsedExitSegmentFlows = IterableUtils.asStream(
                lowFlowConjSegment.getUpstreamVertex().getExitEdgeSegments()).filter(
                es -> !conjSegmentToRemove.containsKey(es)).map(es ->
                Pair.of((ConjugateEdgeSegment) es, bushData.getTurnSendingFlowPcuH(es))).collect(Collectors.toList());
        if(initiateNewShift && alternativeUsedExitSegmentFlows.stream().mapToDouble(Pair::second).sum() <= 0) {
          // no other branch available to reallocate flow to, so we must maintain this flow despite it being low
          // this can happen 1) halfway along a corridor with alphas < 1 such that flow reduces below threshold halfway
          // but without an option to divert.
          continue;
        }

        // remove segment
        double originalConjSegmentFlow = bushData.getTurnSendingFlowPcuH(lowFlowConjSegment);
        removeTurn(lowFlowConjSegment);
        removedConjSegments.add(lowFlowConjSegment);

        if(initiateNewShift){
          // update total flow to redistribute for conjugate vertex after removal of eligible...
          double conjSegmentRemovedUpstreamFlow =
                  conjSegmentRemovedFlows.getOrDefault(lowFlowConjSegment, 0.0);
          double newlyShiftedFlowToRedistribute = originalConjSegmentFlow - conjSegmentRemovedUpstreamFlow;
          flowtoRedistribute += newlyShiftedFlowToRedistribute;

          if (detailedLogging) {
            LOGGER.info(String.format(
                    "Initiate branch shift for too low flows: shifted from turn (%s) flow: %.10f) - conj bush (%s)",
                    lowFlowConjSegment.getOriginalAdjacentEdgeSegmentsIdsAsString(),
                    flowtoRedistribute,
                    lowFlowConjSegment.getIdsAsString(),
                    getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
          }
        }else {
          // ...or propagate removed link's (newly removed) flow in case it should lead to downstream removal of more link
          // segments which may be required to avoid dangling links within the bush.
          //   Note: since we are removing turns on-the-fly which affects the topological order, we should not create another
          //         topological iterator at this point as the bush's state is in flux and may be invalid temporarily. therefore
          //         we will track the to be removed flow as we go and deal with it here while traversing the bush instead
          conjSegmentRemovedFlows.put(lowFlowConjSegment,
                  conjSegmentRemovedFlows.getOrDefault(lowFlowConjSegment,0.0) + originalConjSegmentFlow);

          //  continuing existing branch shift, but not initiating a new one, which will continue tracking of the removed flows
          if (detailedLogging) {
            LOGGER.info(String.format(
                    "Continuing Implicit branch shift: removed flow: %.10f, from turn (%s) - bush (%s)",
                    conjSegmentRemovedFlows.get(lowFlowConjSegment),
                    lowFlowConjSegment.getOriginalAdjacentEdgeSegmentsIdsAsString(),
                    getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
          }
        }
      }

      // WHEN REACHING THIS POINT WE ARE: finalising an existing shift or redistributing flow of a new branch shift
      // triggered by one or more conjugate exit segments that have been removed...

      // REDISTRIBUTION OF FLOW OF NEW BRANCH SHIFT
      if(flowtoRedistribute > 0) {
        // updated splitting rates now that flow has been removed, so distribution has changed, use this for
        // redistribution purposes.
        var updatedSplittingRates = bushData.getSplittingRates(currVertex);
        index = 0;
        for (var altExitSegment : currVertex.getExitEdgeSegments()) {
          var splittingRate = updatedSplittingRates[index++];
          if (splittingRate > 0) {
            double shiftedTurnFlow = flowtoRedistribute * splittingRate;
            addTurnSendingFlow(altExitSegment, shiftedTurnFlow);

            if (detailedLogging) {
              LOGGER.info(String.format(
                      "Redistributed flow (%.10f) to turn (%s) - conj bush (%s)",
                      shiftedTurnFlow,
                      altExitSegment.getOriginalAdjacentEdgeSegmentsIdsAsString(),
                      getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
            }
          }
        }
      }
    });

    return removedConjSegments;
  }

}

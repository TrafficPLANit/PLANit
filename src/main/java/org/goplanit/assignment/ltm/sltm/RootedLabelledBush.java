package org.goplanit.assignment.ltm.sltm;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.goplanit.algorithms.shortest.MinMaxPathResult;
import org.goplanit.assignment.ltm.sltm.conjugate.ConjugateBushTurnData;
import org.goplanit.assignment.ltm.sltm.conjugate.ConjugateDestinationBush;
import org.goplanit.graph.directed.acyclic.ACyclicSubGraphImpl;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.graph.directed.acyclic.ACyclicSubGraph;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.network.virtual.graph.CentroidVertex;

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

  /** track bush specific data */
  protected final LabelledBushTurnData bushData;

  /**
   * Access to DAG as regular acylic subgraph rather than untyped
   * 
   * @return dag
   */
  @Override
  protected ACyclicSubGraph getDag() {
    return (ACyclicSubGraph) super.getDag();
  }

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
    return sb.toString();
  }

}

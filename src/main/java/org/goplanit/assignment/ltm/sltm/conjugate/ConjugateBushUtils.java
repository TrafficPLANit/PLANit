package org.goplanit.assignment.ltm.sltm.conjugate;

import org.goplanit.algorithms.shortest.MinMaxPathResult;
import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.CollectionUtils;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.network.virtual.physical.ConnectoidSegment;
import org.goplanit.utils.network.virtual.physical.conjugate.ConjugateConnectoidNode;

import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * Uitlities when using Conjugate bushes
 */
public class ConjugateBushUtils {

  private static final Logger LOGGER = Logger.getLogger(ConjugateBushUtils.class.getCanonicalName());

  /**
   * construct the bush sending flows for the bush's root exit edge segments based on origin demand and
   * splitting rates at origin
   *
   * @param bush             at hand
   * @return bushSendingFlows by origin exit segment
   */
  public static TreeMap<ConjugateEdgeSegment, Double> createAllOriginExitSegmentSendingFlows(
      final ConjugateDestinationBush bush) {
    return createOriginExitSegmentSendingFlowsNodeFiltered(bush, null /* all*/);
  }

  /**
   * construct the bush sending flows for the bush's root exit edge segments based on origin demand and
   * splitting rates at origin
   *
   * @param bush             at hand
   * @param edgeSegmentsToConsider when null all (origin) edge segments are considered, otherwise only the origin exits
   *                               that are in this set are considered
   * @return bushSendingFlows by origin exit segment
   */
  public static TreeMap<ConjugateEdgeSegment, Double> createOriginExitSegmentSendingFlowsSegmentFiltered(
      final ConjugateDestinationBush bush, Set<EdgeSegment> edgeSegmentsToConsider) {

    var bushSendingFlows = new TreeMap<ConjugateEdgeSegment, Double>();

    var originVertices = bush.getOriginVertices();
    for (ConjugateDirectedVertex originVertex : originVertices) {
      double totalOriginsSendingFlow = 0;
      for (var originExit : originVertex.getExitEdgeSegments()) {
        if (bush.contains(originExit)) {
          if(!CollectionUtils.nullOrEmpty(edgeSegmentsToConsider) &&
              !edgeSegmentsToConsider.contains(originExit)){
            continue;
          }
          double sendingFlow = bush.getTurnSendingFlow(originExit);
          bushSendingFlows.put(originExit, sendingFlow);
          totalOriginsSendingFlow += sendingFlow;
        }
      }

      if (CollectionUtils.nullOrEmpty(edgeSegmentsToConsider) &&
          Precision.notEqual(totalOriginsSendingFlow, bush.getOriginDemandPcuH(originVertex), Precision.EPSILON_3)) {
        LOGGER.severe(String.format("conjugate bush with root zone (%s) origin's (%s) travel demand (%.8f pcu/h) not equal " +
                "to total flow (%.8f pcu/h), this shouldn't happen",
            bush.getRootZoneVertex().getParent().getParentZone().getIdsAsString(),
            ((ConjugateConnectoidNode)originVertex).getCentroidVertex().getParent().getParentZone().getXmlId(),
            bush.getOriginDemandPcuH(originVertex), totalOriginsSendingFlow));
      }
    }
    return bushSendingFlows;
  }

  /**
   * construct the bush sending flows for the bush's root exit edge segments based on origin demand and
   * splitting rates at origin
   *
   * @param bush             at hand
   * @param nodesToConsider when null all (origins) are considered, otherwise only the selected
   * @return bushSendingFlows by origin exit segment
   */
  public static TreeMap<ConjugateEdgeSegment, Double> createOriginExitSegmentSendingFlowsNodeFiltered(
      final ConjugateDestinationBush bush, Set<DirectedVertex> nodesToConsider) {
    var bushSendingFlows = new TreeMap<ConjugateEdgeSegment, Double>();

    var originVertices = bush.getOriginVertices();
    for (ConjugateDirectedVertex originVertex : originVertices) {
      double totalOriginsSendingFlow = 0;
      if(!CollectionUtils.nullOrEmpty(nodesToConsider) &&
          !nodesToConsider.contains(originVertex)){
        continue;
      }
      for (var originExit : originVertex.getExitEdgeSegments()) {
        if (bush.contains(originExit)) {
          double sendingFlow = bush.getTurnSendingFlow(originExit);
          bushSendingFlows.put(originExit, sendingFlow);
          totalOriginsSendingFlow += sendingFlow;
        }
      }

      if (CollectionUtils.nullOrEmpty(nodesToConsider) &&
          Precision.notEqual(totalOriginsSendingFlow, bush.getOriginDemandPcuH(originVertex), Precision.EPSILON_3)) {
        LOGGER.severe(String.format("conjugate bush with root zone (%s) origin's (%s) travel demand (%.8f pcu/h) not equal " +
                "to total flow (%.8f pcu/h), this shouldn't happen",
            bush.getRootZoneVertex().getParent().getParentZone().getIdsAsString(),
            ((ConjugateConnectoidNode)originVertex).getCentroidVertex().getParent().getParentZone().getXmlId(),
            bush.getOriginDemandPcuH(originVertex), totalOriginsSendingFlow));
      }
    }
    return bushSendingFlows;
  }

  public static Pair<Boolean, Double> isEligibleForAdding(
      ConjugateEdgeSegment linkSegment,
      double[] conjLinkSegmentCosts,
      MinMaxPathResult conjBushMinMaxPaths) {
    // restricted mode P1&P2
    return isEligibleForAdding(linkSegment, conjLinkSegmentCosts, conjBushMinMaxPaths, false);
  }

  /**
   * Check if segment is worth adding based on whether it is both in the min and in the max path search.
   * Consistent with intersection of P1 and P2 sets in Nie (2009) - A class of bush-based algorithms for the traffic
   * assignment problem.
   *
   * @param linkSegment          to check
   * @param conjLinkSegmentCosts segment costs to use
   * @param conjBushMinMaxPaths  min max path cost to check
   * @return true when eligible, false otherwise, second result is the min bush cost to the root considering we
   * add the link
   */
  public static Pair<Boolean, Double> isEligibleForAdding(
      ConjugateEdgeSegment linkSegment,
      double[] conjLinkSegmentCosts,
      MinMaxPathResult conjBushMinMaxPaths,
      boolean allowEligibilityBasedOnOnlyP2) {
    var endVertex = linkSegment.getUpstreamVertex();
    var startVertex = linkSegment.getDownstreamVertex();

    // connectoids are guaranteed to be one way and be a leaf, so they can never cause a cycle
    // currently we must check this because it may otherwise deny some links to be added based on for example
    // max cost paths appearing to possibly cause a cycle while we know they cannot
    //      1
    //      / --> ----- ----\
    //   O--|                |-->D
    //          ------ -----/
    //      2
    // if O>1 is in bush for D and has a large max cost, and O>2 is not in bush yet and would have a lower min cost
    // we'd want to add it, but if its max cost becomes higher it is rejected. However, we know its max cost can only
    // go up due to downstream differences, NOT because of a cycle since no flow other than the origin can go the
    // upstream of 2 (which would cause this alleged cycle), so for those links we'll bypass this condition.
    //
    // todo: ideally we'd use a more "scientific" check than this reasoning.
    boolean forceAllow = false;
    if(!endVertex.hasOriginalEdgeSegment() || endVertex.getOriginalEdgeSegment() instanceof ConnectoidSegment){
      forceAllow = true;
    }

    double startToEndCost = conjLinkSegmentCosts[(int)linkSegment.getId()];

    double minCostEnd = conjBushMinMaxPaths.getMinCostToReach(endVertex);
    double minCostStart = conjBushMinMaxPaths.getMinCostToReach(startVertex);
    if(forceAllow || (minCostStart + startToEndCost < minCostEnd) || allowEligibilityBasedOnOnlyP2 ){
      double maxCostEnd = conjBushMinMaxPaths.getMaxCostToReach(endVertex);
      double maxCostStart = Math.abs(conjBushMinMaxPaths.getMaxCostToReach(startVertex));
      if(forceAllow || (maxCostStart + startToEndCost < maxCostEnd)){
        return Pair.of(true, minCostStart + startToEndCost);
      }
    }
    return Pair.of(false, minCostStart + startToEndCost);
  }

}

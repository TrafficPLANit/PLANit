package org.goplanit.assignment.ltm.sltm.conjugate;

import org.goplanit.algorithms.shortest.MinMaxPathResult;
import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.CollectionUtils;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.misc.Triple;
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

  public static Triple<Boolean, Double, Boolean> isEligibleForAdding(
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
   * @param allowEligibilityBasedOnOnlyP2  flag
   * @return true when eligible, false otherwise, second result is the min bush cost to the root considering we
   * add the link, third result is indicates if it was accepted by P1 test, false otherwise
   */
  public static Triple<Boolean, Double, Boolean> isEligibleForAdding(
      ConjugateEdgeSegment linkSegment,
      double[] conjLinkSegmentCosts,
      MinMaxPathResult conjBushMinMaxPaths,
      boolean allowEligibilityBasedOnOnlyP2) {
    var endVertex = linkSegment.getUpstreamVertex();
    var startVertex = linkSegment.getDownstreamVertex();

    double startToEndCost = conjLinkSegmentCosts[(int)linkSegment.getId()];

    double minCostEnd = conjBushMinMaxPaths.getMinCostToReach(endVertex);
    double minCostStart = conjBushMinMaxPaths.getMinCostToReach(startVertex);
    double maxCostEnd = conjBushMinMaxPaths.getMaxCostToReach(endVertex);
    double maxCostStart = Math.abs(conjBushMinMaxPaths.getMaxCostToReach(startVertex));

    boolean p1Eligible = (minCostStart + startToEndCost < minCostEnd);
    boolean p2Eligible = (maxCostStart + startToEndCost < maxCostEnd);

    if(allowEligibilityBasedOnOnlyP2){
      return Triple.of(p2Eligible, maxCostStart + startToEndCost, p1Eligible);
    }else{
      return Triple.of(p1Eligible && p2Eligible, minCostStart + startToEndCost, p1Eligible);
    }
  }
}

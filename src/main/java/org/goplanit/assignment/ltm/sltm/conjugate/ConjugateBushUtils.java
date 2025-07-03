package org.goplanit.assignment.ltm.sltm.conjugate;

import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.network.virtual.physical.conjugate.ConjugateConnectoidNode;

import java.util.Map;
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
  public static TreeMap<ConjugateEdgeSegment, Double> createOriginExitSegmentSendingFlows(
      final ConjugateDestinationBush bush) {

    var bushSendingFlows = new TreeMap<ConjugateEdgeSegment, Double>();

    var originVertices = bush.getOriginVertices();
    for (ConjugateDirectedVertex originVertex : originVertices) {
      double totalOriginsSendingFlow = 0;
      for (var originExit : originVertex.getExitEdgeSegments()) {
        if (bush.contains(originExit)) {
          double sendingFlow = bush.getTurnSendingFlow(originExit);
          bushSendingFlows.put(originExit, sendingFlow);
          totalOriginsSendingFlow += sendingFlow;
        }
      }

      if (Precision.notEqual(totalOriginsSendingFlow, bush.getOriginDemandPcuH(originVertex), Precision.EPSILON_3)) {
        LOGGER.severe(String.format("conjugate bush with root zone (%s) origin's (%s) travel demand (%.8f pcu/h) not equal " +
                "to total flow (%.8f pcu/h), this shouldn't happen",
            bush.getRootZoneVertex().getParent().getParentZone().getIdsAsString(),
            ((ConjugateConnectoidNode)originVertex).getCentroidVertex().getParent().getParentZone().getXmlId(),
            bush.getOriginDemandPcuH(originVertex), totalOriginsSendingFlow));
      }
    }
    return bushSendingFlows;
  }
}

package org.planit.assignment.ltm.sltm.consumer;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.planit.assignment.ltm.sltm.Bush;
import org.planit.assignment.ltm.sltm.loading.NetworkLoadingFactorData;
import org.planit.assignment.ltm.sltm.loading.SendingFlowData;
import org.planit.assignment.ltm.sltm.loading.SplittingRateData;
import org.planit.assignment.ltm.sltm.loading.StaticLtmLoadingScheme;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.directed.DirectedVertex;

/**
 * Consumer to apply during bush based turn flow update for each non-zero demand bush
 * <p>
 * Depending on the applied solution scheme a slightly different approach is taken to this update where:
 * <p>
 * POINT QUEUE BASIC: Also update the network sending flow. Only during basic point queue solution scheme, sending flows are NOT locally updated in the sending flow update step.
 * Therefore sending flows of most links are not updated during this sending flow update because it only updates the sending flows of outgoing links of potentially blocking nodes.
 * When an incoming link of any node is not also an outgoing link of another potentially blocking node its sending flow remains the same even if it actually changes due to further
 * upstream changes in restrictions. In this approach this is only identified when we make sure the sending flows are updated during (this) loading on the path level. Hence, we
 * must update sending flows here.
 * <p>
 * ANY OTHER SOLUTION APPROACH: Here we update all used nodes and sending flows are updated iteratively and locally propagated without the need of the loading in the sending flow
 * update. Therefore, there is no need to update the sending flows. On the other hand we now update the turn flows on all used nodes rather than only the potentially blocking ones.
 * 
 * @author markr
 *
 */
public class BushTurnFlowUpdateConsumer extends NetworkTurnFlowUpdate implements Consumer<Bush> {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(BushTurnFlowUpdateConsumer.class.getCanonicalName());

  /**
   * Initialise the bush sending flows for the bush's root exit edge segments to bootstrap the loading for this bush
   * 
   * @param originBush       at hand
   * @param bushSendingFlows to populate as a starting point for the bush loading
   */
  private void initialiseRootExitSegmentSendingFlows(final Bush originBush, final double[] bushSendingFlows) {
    int index = 0;
    double[] rootVertexSplittingRates = originBush.getRootVertexSplittingRates();
    for (EdgeSegment rootExit : originBush.getOrigin().getCentroid().getExitEdgeSegments()) {
      if (originBush.containsEdgeSegment(rootExit)) {
        // TODO: if this works well, then we can replace this by simply collecting the rootExit sending flow directly which is less costly (but has no checks)
        bushSendingFlows[(int) rootExit.getId()] = originBush.getTravelDemandPcuH() * rootVertexSplittingRates[index];
      }
      ++index;
    }
  }

  /**
   * constructor
   * 
   * @param splittingRateData        to use
   * @param sendingFlowData          to use
   * @param solutionScheme           to apply
   * @param networkLoadingFactorData to use
   */
  public BushTurnFlowUpdateConsumer(final StaticLtmLoadingScheme solutionScheme, final SendingFlowData sendingFlowData, final SplittingRateData splittingRateData,
      NetworkLoadingFactorData networkLoadingFactorData) {
    super(solutionScheme, sendingFlowData, splittingRateData, networkLoadingFactorData);
  }

  /**
   * Update the network turn flow for the bush at hand
   * 
   * @param origin bush to apply
   */
  @Override
  public void accept(final Bush originBush) {

    /*
     * track bush sending flows propagated from the origin Note: We cannot use the bush's own turn sending flows because we are performing a network loading based on the most
     * recent bush's splitting rates, we only use the bush's sending flows for bush flow shifts. The bush's sending flows are updated AFTER the network loading is complete
     * (converged) by using the network splitting rates and reduction factors
     */
    double[] bushSendingFlows = new double[getSendingFlows().length];

    /* get topological sorted vertices to process */
    Collection<DirectedVertex> topSortedVertices = originBush.getTopologicallySortedVertices();
    Iterator<DirectedVertex> vertexIter = topSortedVertices.iterator();
    DirectedVertex currVertex = vertexIter.next();
    if (!currVertex.idEquals(originBush.getOrigin().getCentroid())) {
      LOGGER.severe(String.format("Topologically sorted bush rooted at origin %s, does not commence with its root vertex %s", originBush.getOrigin().getXmlId(),
          originBush.getOrigin().getCentroid().getXmlId()));
      return;
    }

    /* initialise with root vertex outgoing edge flows */
    initialiseRootExitSegmentSendingFlows(originBush, bushSendingFlows);

    /* pass over bush in topological order propagating flow from origin */
    while (vertexIter.hasNext()) {
      currVertex = vertexIter.next();
      for (EdgeSegment entrySegment : currVertex.getEntryEdgeSegments()) {
        if (originBush.containsEdgeSegment(entrySegment)) {
          int entrySegmentId = (int) entrySegment.getId();
          double bushSendingFlow = bushSendingFlows[entrySegmentId];

          /* s_a = u_a */
          if (isUpdateSendingFlow()) {
            getSendingFlows()[(int) entrySegment.getId()] += bushSendingFlow;
          }

          /* v_a = s_a * alpha_a */
          double bushEntryAcceptedFlow = bushSendingFlow * flowAcceptanceFactors[entrySegmentId];
          double[] splittingRates = originBush.getSplittingRates(entrySegment);
          int index = 0;
          for (EdgeSegment exitSegment : currVertex.getExitEdgeSegments()) {
            if (originBush.containsEdgeSegment(exitSegment)) {
              int exitSegmentId = (int) exitSegment.getId();
              /* v_ab = v_a * phi_ab */
              double turnAcceptedFlow = bushEntryAcceptedFlow * splittingRates[index];
              bushSendingFlows[exitSegmentId] += turnAcceptedFlow; // bush level
              addToAcceptedTurnFlows(createTurnHashCode(entrySegmentId, exitSegmentId), turnAcceptedFlow); // network level
            }
            ++index;
          }
        }
      }
    }
  }

}

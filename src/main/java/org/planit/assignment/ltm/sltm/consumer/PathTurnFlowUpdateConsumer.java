package org.planit.assignment.ltm.sltm.consumer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import org.planit.assignment.ltm.sltm.NetworkLoadingFactorData;
import org.planit.assignment.ltm.sltm.SendingFlowData;
import org.planit.assignment.ltm.sltm.SplittingRateData;
import org.planit.assignment.ltm.sltm.StaticLtmSolutionScheme;
import org.planit.od.path.OdPaths;
import org.planit.utils.functionalinterface.TriConsumer;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.misc.HashUtils;
import org.planit.utils.path.DirectedPath;
import org.planit.utils.zoning.OdZone;

/**
 * Consumer to apply during path based turn flow update for each combination of origin, destination, and demand
 * <p>
 * Depending on the applied solution scheme a slightly different approach is taken to this update where:
 * <p>
 * POINT QUEUE BASIC: Also update the current sending flow. Only during basic point queue solution scheme, sending flows are NOT locally updated in the sending flow update step.
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
public class PathTurnFlowUpdateConsumer implements TriConsumer<OdZone, OdZone, Double> {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(PathTurnFlowUpdateConsumer.class.getCanonicalName());

  /**
   * Od Paths to use
   */
  private final OdPaths odPaths;

  /**
   * Splitting rate data to use
   */
  private final SplittingRateData splittingRateData;

  /**
   * Flow acceptance factors to use
   */
  double[] flowAcceptanceFactors;

  /**
   * Flag indicating if we are tracking all (used) node turn flows or not
   */
  private final boolean trackAllNodeTurnFlows;

  /**
   * Flag indicating if we are updating sending flows in addition to the turn flows
   */
  private final boolean updateSendingFlow;

  /**
   * The sending flows to update in case the applied solution scheme is the POINT_QUEUE_BASIC scheme
   */
  private final double[] sendingFlows;

  /**
   * The output of this update that can be collected after execution
   */
  private final Map<Integer, Double> acceptedTurnFlows;

  /**
   * constructor
   * 
   * @param odPaths                  to use
   * @param splittingRateData        to use
   * @param sendingFlowData          to use
   * @param solutionScheme           to apply
   * @param networkLoadingFactorData to use
   */
  public PathTurnFlowUpdateConsumer(final StaticLtmSolutionScheme solutionScheme, final SendingFlowData sendingFlowData, final SplittingRateData splittingRateData,
      NetworkLoadingFactorData networkLoadingFactorData, final OdPaths odPaths) {
    this.acceptedTurnFlows = new HashMap<Integer, Double>();

    this.odPaths = odPaths;
    this.splittingRateData = splittingRateData;

    /* see class description on why we use these flags */
    this.trackAllNodeTurnFlows = !solutionScheme.equals(StaticLtmSolutionScheme.POINT_QUEUE_BASIC);
    this.updateSendingFlow = solutionScheme.equals(StaticLtmSolutionScheme.POINT_QUEUE_BASIC);
    if (updateSendingFlow) {
      sendingFlowData.resetAllSendingFlows();
    }
    this.sendingFlows = sendingFlowData.getCurrentSendingFlows();

    this.flowAcceptanceFactors = networkLoadingFactorData.getCurrentFlowAcceptanceFactors();
  }

  /**
   * Update the turn flow for the path of the given origin,destination,demand combination
   */
  @Override
  public void accept(OdZone origin, OdZone destination, Double odDemand) {
    /* path */
    DirectedPath odPath = odPaths.getValue(origin, destination);
    double acceptedPathFlowRate = odDemand;
    if (odPath.isEmpty()) {
      LOGGER.warning(String.format("IGNORE: encountered empty path %s", odPath.getXmlId()));
      return;
    }

    /* turn */
    Iterator<EdgeSegment> edgeSegmentIter = odPath.iterator();
    int previousEdgeSegmentId = (int) edgeSegmentIter.next().getId();
    int currentEdgeSegmentId = previousEdgeSegmentId;
    while (edgeSegmentIter.hasNext()) {
      EdgeSegment currEdgeSegment = edgeSegmentIter.next();
      currentEdgeSegmentId = (int) currEdgeSegment.getId();

      if (trackAllNodeTurnFlows || this.splittingRateData.isTracked(currEdgeSegment.getUpstreamVertex())) {

        /* s_a = u_a */
        if (updateSendingFlow) {
          sendingFlows[previousEdgeSegmentId] += acceptedPathFlowRate;
        }

        /* v_ap = u_bp = alpha_a*...*f_p where we consider all preceding alphas (flow acceptance factors) up to now */
        acceptedPathFlowRate *= flowAcceptanceFactors[previousEdgeSegmentId];
        acceptedTurnFlows.put(HashUtils.createCombinedHashCode(previousEdgeSegmentId, currentEdgeSegmentId), acceptedPathFlowRate);
      }

      previousEdgeSegmentId = currentEdgeSegmentId;
    }
  }

  /**
   * Access to the result, the accepted turn flows, where key comprises a combined hash of entry and exit edge segment ids and value is the accepted turn flow v_ab
   * 
   * @return accepted turn flows
   */
  public Map<Integer, Double> getAcceptedTurnFlows() {
    return this.acceptedTurnFlows;
  }
}

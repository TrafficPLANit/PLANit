package org.goplanit.assignment.ltm.sltm.consumer;

import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.assignment.ltm.sltm.StaticLtmDirectedPath;
import org.goplanit.od.path.OdMultiPaths;
import org.goplanit.od.path.OdPaths;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.network.layer.physical.Movement;

/**
 * Consumer to apply during path based turn flow update for each combination of origin, destination, and demand
 * <p>
 * Depending on the configuration which in turn depends on the active solution scheme a slightly different approach is taken to this update where:
 * <p>
 * POINT QUEUE BASIC: Also update the current sending flow. Only during basic point queue solution scheme, sending flows are NOT locally updated in the sending flow update step.
 * Therefore, sending flows of most links are not updated during this sending flow update because it only updates the sending flows of outgoing links of potentially blocking nodes.
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
public class PathTurnFlowUpdateConsumer extends PathFlowUpdateConsumer<NetworkTurnFlowUpdateData> {

  /** logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(PathTurnFlowUpdateConsumer.class.getCanonicalName());

  /**
   * Apply the flow to the turn (and update link sending flow if required)
   * 
   * @param movement         the movement
   * @param turnSendingFlowPcuH sending flow rate of turn
   * @return accepted flow rate of turn after applying link acceptance factor
   */
  @Override
  protected double applySingleFlowUpdate(final Movement movement, final double turnSendingFlowPcuH) {

    if (dataConfig.trackAllNodeTurnFlows || dataConfig.splittingRateData.isTracked(movement.getCentreVertex())) {

      int prevSegmentId = (int) movement.getSegmentFrom().getId();

      /* s_a = u_a where we only need to update the sending flows of tracked turns */
      if (dataConfig.isSendingflowsUpdate()) {
        dataConfig.sendingFlows[prevSegmentId] += turnSendingFlowPcuH;
      }

      /* v_ap = u_bp = alpha_a*...*f_p where we implicitly consider all preceding alphas (flow acceptance factors) up to now */
      double acceptedTurnFlowPcuH = turnSendingFlowPcuH * dataConfig.flowAcceptanceFactors[prevSegmentId];
      dataConfig.addToAcceptedTurnFlows(movement, acceptedTurnFlowPcuH);

      /* v_a = SUM(v_ap) (only when enabled) */
      if (dataConfig.isOutflowsUpdate()) {
        dataConfig.outFlows[prevSegmentId] += acceptedTurnFlowPcuH;
      }

      return acceptedTurnFlowPcuH;
    } else {
      return turnSendingFlowPcuH;
    }
  }

  /**
   * DO NOTHING - since this is a turn update and the final turn has no outgoing edge segment it is never tracked (because it does not exist), therefore, we can disregard updating
   * the final segment flow in a turn flow update setting, even when tracking link segment sending flows (since this sending flow is never required as input to a node model update
   * 
   * @param lastEdgeSegment      to use
   * @param acceptedPathFlowRate to use
   */
  @Override
  protected void applyPathFinalSegmentFlowUpdate(final EdgeSegment lastEdgeSegment, double acceptedPathFlowRate) {
    // do nothing
  }

  /**
   * Constructor, where sending flows are not to be updated, only turn flows
   * 
   * @param dataConfig to use
   * @param odPaths    to use
   */
  public PathTurnFlowUpdateConsumer(
          final NetworkTurnFlowUpdateData dataConfig,
          final OdMultiPaths<StaticLtmDirectedPath, ? extends List<StaticLtmDirectedPath>> odPaths) {
    super(dataConfig, odPaths);
  }

  /**
   * The found accepted turn flows by the combined entry-exit segment hash code
   * 
   * @return accepted turn flows
   */
  public double[] getAcceptedTurnFlows() {
    return dataConfig.getAcceptedTurnFlows();
  }

}

package org.goplanit.assignment.ltm.sltm.consumer;

import java.util.List;
import java.util.logging.Logger;

import org.goplanit.assignment.ltm.sltm.util.StaticLtmDirectedPath;
import org.goplanit.assignment.ltm.sltm.loading.TurnFlowAccessorCompiledTurns;
import org.goplanit.utils.network.layer.physical.CompiledRelationIndex;
import org.goplanit.zoning.od.path.OdMultiPaths;
import org.goplanit.utils.graph.directed.EdgeSegment;

/**
 * Consumer to apply during path based turn flow update for each combination of origin, destination, and demand
 * <p>
 * The update differs slightly by configuration, which depends on the active solution scheme:
 * <p>
 * POINT QUEUE BASIC: also update the current sending flow. Only in the basic point queue solution scheme are sending
 * flows NOT updated locally in the sending flow update step: it updates only the sending flows of outgoing links of
 * potentially blocking nodes, so most links are not updated. An incoming link of a node that is not also an outgoing
 * link of another potentially blocking node keeps its sending flow, even if it changes due to further upstream changes
 * in restrictions. This is only picked up by updating the sending flows during (this) loading on the path level, so
 * sending flows must be updated here.
 * <p>
 * ANY OTHER SOLUTION APPROACH: all used nodes are updated, and sending flows are updated iteratively and propagated
 * locally without needing the loading in the sending flow update. So there is no need to update the sending flows.
 * On the other hand, the turn flows are updated on all used nodes rather than only the potentially blocking ones.
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
   * @param fromSegment         of the movement
   * @param toSegment         of the movement
   * @param turnSendingFlowPcuH sending flow rate of turn
   * @param turnUnconstrainedFlowPcuH unconstrained flow rate of turn
   * @return accepted flow rate of turn after applying link acceptance factor
   */
  @Override
  protected double applySingleFlowUpdate(
          final EdgeSegment fromSegment,
          final EdgeSegment toSegment,
          final double turnSendingFlowPcuH,
          final double turnUnconstrainedFlowPcuH) {
    if(fromSegment == null){
      return turnSendingFlowPcuH;
    }

    if (dataConfig.trackAllNodeTurnFlows ||
        dataConfig.nlSplittingRateData.isTracked(fromSegment.getDownstreamVertex())) {

      int fromSegmentId = (int) fromSegment.getId();

      /* s_a = u_a where we only need to update the sending flows of tracked turns */
      if (dataConfig.isSendingFlowsUpdate()) {
        dataConfig.sendingFlows[fromSegmentId] += turnSendingFlowPcuH;
      }

      if(dataConfig.isUnconstrainedFlowsUpdate()){
        dataConfig.unconstrainedFlows[fromSegmentId] += turnUnconstrainedFlowPcuH;
      }

      /* v_ap = u_bp = alpha_a*...*f_p where we implicitly consider all
       * preceding alphas (flow acceptance factors) up to now */
      double acceptedTurnFlowPcuH = turnSendingFlowPcuH * dataConfig.flowAcceptanceFactors[fromSegmentId];
      long movementId = compiledTurnIds.get(fromSegmentId, toSegment.getId());
      dataConfig.addToAcceptedTurnFlows((int)movementId, acceptedTurnFlowPcuH);

      /* v_a = SUM(v_ap) (only when enabled) */
      if (dataConfig.isOutflowsUpdate()) {
        dataConfig.outFlows[fromSegmentId] += acceptedTurnFlowPcuH;
      }

      return acceptedTurnFlowPcuH;
    } else {
      return turnSendingFlowPcuH;
    }
  }

  /**
   * In a turn update the final turn has no outgoing edge segment: it does not exist, so it is never tracked. The final
   * segment flow therefore need not be updated, even when tracking link segment sending flows, as this sending flow is
   * never an input to a node model update.
   *
   * @param lastEdgeSegment      to use
   * @param acceptedPathFlowRate to use
   * @param turnUnconstrainedFlowPcuH unconstrained flow rate of turn
   */
  @Override
  protected void applyPathFinalSegmentFlowUpdate(
          final EdgeSegment lastEdgeSegment, double acceptedPathFlowRate, final double turnUnconstrainedFlowPcuH) {
    //do nothing
  }

  /**
   * Constructor, where sending flows are not to be updated, only turn flows
   * 
   * @param dataConfig to use
   * @param odPaths    to use
   * @param compiledTurnIds to use
   */
  public PathTurnFlowUpdateConsumer(
          final NetworkTurnFlowUpdateData dataConfig,
          final OdMultiPaths<StaticLtmDirectedPath, ? extends List<StaticLtmDirectedPath>> odPaths,
          final CompiledRelationIndex compiledTurnIds) {
    super(dataConfig, odPaths, compiledTurnIds);
  }

  /**
   * The found accepted turn flows by the combined entry-exit segment hash code
   * 
   * @return accepted turn flows
   */
  public TurnFlowAccessorCompiledTurns getAcceptedTurnFlows() {
    return TurnFlowAccessorCompiledTurns.of(compiledTurnIds, dataConfig.getAcceptedTurnFlows());
  }

}

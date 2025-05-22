package org.goplanit.assignment.ltm.sltm.consumer;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.assignment.ltm.sltm.StaticLtmDirectedPath;
import org.goplanit.od.path.OdMultiPaths;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.network.layer.physical.Movement;

import java.util.List;
import java.util.logging.Logger;

/**
 * Consumer to apply during path based loading to update the inflows on the provided raw array by their link segment ids
 * for each combination of origin, destination, and demand
 * 
 * @author markr
 *
 */
public class PathLinkFlowUpdateConsumer extends PathFlowUpdateConsumer<NetworkFlowUpdateData> {

  /** logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(PathLinkFlowUpdateConsumer.class.getCanonicalName());

  /**
   * For each entry segment update the in(sending)flow (and outflow if so specified)
   * 
   * @param movement         to use
   * @param turnSendingFlowPcuH to use
   * @param turnUnconstrainedFlowPcuH unconstrained flow rate of turn
   */
  @Override
  protected double applySingleFlowUpdate(
          final Movement movement, double turnSendingFlowPcuH, final double turnUnconstrainedFlowPcuH) {
    /* u_a: update inflow for link segment */
    int prevSegmentId = (int) movement.getSegmentFrom().getId();
    this.dataConfig.sendingFlows[prevSegmentId] += turnSendingFlowPcuH;

    if(this.dataConfig.isInflowsUpdate()){
      this.dataConfig.inFlows[prevSegmentId] += turnSendingFlowPcuH;
    }

    if(dataConfig.isUnconstrainedFlowsUpdate()){
      dataConfig.unconstrainedFlows[prevSegmentId] += turnUnconstrainedFlowPcuH;
    }

    /* v_ap = u_bp = alpha_a*...*f_p  */
    double acceptedTurnFlowPcuH = turnSendingFlowPcuH * dataConfig.flowAcceptanceFactors[prevSegmentId];

    /* v_a = SUM(v_ap) (only when enabled) */
    if (dataConfig.isOutflowsUpdate()) {
      this.dataConfig.outFlows[prevSegmentId] += acceptedTurnFlowPcuH;
    }
    return acceptedTurnFlowPcuH;
  }

  /**
   * Apply final path flow on last segment that otherwise would not have been updated in the turn
   * based {@link #applySingleFlowUpdate(Movement, double, double)}
   * 
   * @param lastEdgeSegment      to use
   * @param acceptedPathFlowRate to use
   * @param turnUnconstrainedFlowPcuH unconstrained flow rate of turn
   */
  @Override
  protected void applyPathFinalSegmentFlowUpdate(
          EdgeSegment lastEdgeSegment, double acceptedPathFlowRate, final double turnUnconstrainedFlowPcuH) {
    dataConfig.sendingFlows[(int) lastEdgeSegment.getId()] += acceptedPathFlowRate;

    if(dataConfig.isInflowsUpdate()){
      dataConfig.inFlows[(int) lastEdgeSegment.getId()] += acceptedPathFlowRate;
    }
    if(dataConfig.isOutflowsUpdate()){
      dataConfig.outFlows[(int) lastEdgeSegment.getId()] += acceptedPathFlowRate;
    }

    if(dataConfig.isUnconstrainedFlowsUpdate()){
      dataConfig.unconstrainedFlows[(int) lastEdgeSegment.getId()] += turnUnconstrainedFlowPcuH;
    }
  }

  /**
   * Constructor
   * 
   * @param dataConfig containing data configuration to use
   * @param odPaths    to use
   */
  public PathLinkFlowUpdateConsumer(
          final NetworkFlowUpdateData dataConfig,
          final OdMultiPaths<StaticLtmDirectedPath, ? extends List<StaticLtmDirectedPath>> odPaths) {
    super(dataConfig, odPaths, null);
  }

}

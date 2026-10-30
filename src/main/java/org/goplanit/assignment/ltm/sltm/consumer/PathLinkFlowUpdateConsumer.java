package org.goplanit.assignment.ltm.sltm.consumer;

import org.goplanit.assignment.ltm.sltm.util.StaticLtmDirectedPath;
import org.goplanit.zoning.od.path.OdMultiPaths;
import org.goplanit.utils.graph.directed.EdgeSegment;

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
   * @param fromSegment         of the movement
   * @param toSegment         of the movement
   * @param turnSendingFlowPcuH to use
   * @param turnUnconstrainedFlowPcuH unconstrained flow rate of turn
   */
  @Override
  protected double applySingleFlowUpdate(
      final EdgeSegment fromSegment,
      final EdgeSegment toSegment,
      double turnSendingFlowPcuH,
      final double turnUnconstrainedFlowPcuH) {
    if(fromSegment == null){
      return turnUnconstrainedFlowPcuH;
    }

    /* u_a: update inflow for link segment */
    int fromSegmentId = (int) fromSegment.getId();
    this.dataConfig.sendingFlows[fromSegmentId] += turnSendingFlowPcuH;

    if(this.dataConfig.isInflowsUpdate()){
      this.dataConfig.inFlows[fromSegmentId] += turnSendingFlowPcuH;
    }

    if(dataConfig.isUnconstrainedFlowsUpdate()){
      dataConfig.unconstrainedFlows[fromSegmentId] += turnUnconstrainedFlowPcuH;
    }

    /* v_ap = u_bp = alpha_a*...*f_p  */
    double acceptedTurnFlowPcuH = turnSendingFlowPcuH * dataConfig.flowAcceptanceFactors[fromSegmentId];

    /* v_a = SUM(v_ap) (only when enabled) */
    if (dataConfig.isOutflowsUpdate()) {
      this.dataConfig.outFlows[fromSegmentId] += acceptedTurnFlowPcuH;
    }
    return acceptedTurnFlowPcuH;
  }

  /**
   * Apply final path flow on last segment that otherwise would not have been updated in the turn
   * based {@link #applySingleFlowUpdate(EdgeSegment, EdgeSegment, double, double)}
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

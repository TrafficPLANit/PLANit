package org.goplanit.assignment.ltm.sltm.consumer;

import java.util.logging.Logger;

import org.goplanit.od.path.OdPaths;
import org.goplanit.utils.graph.EdgeSegment;

/**
 * Consumer to apply during path based loading to update the inflows on the provided raw array by their link segment ids for each combination of origin, destination, and demand
 * 
 * @author markr
 *
 */
public class PathLinkSendingFlowUpdateConsumer extends PathFlowUpdateConsumer<NetworkFlowUpdateData> {

  /** logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(PathLinkSendingFlowUpdateConsumer.class.getCanonicalName());

  /**
   * For each entry segment update the in(sending)flow
   * 
   * @param prevSegmentId       to use
   * @param currentSegment      to use
   * @param turnSendingFlowPcuH to use
   */
  @Override
  protected double applySingleFlowUpdate(int prevSegmentId, EdgeSegment currentSegment, double turnSendingFlowPcuH) {
    /* u_a: update inflow for link segment */
    dataConfig.sendingFlows[prevSegmentId] += turnSendingFlowPcuH;
    return turnSendingFlowPcuH * dataConfig.flowAcceptanceFactors[prevSegmentId];
  }

  /**
   * Apply final path flow on last segment that otherwise would not have been updated in the turn based {@link #applySingleFlowUpdate(int, EdgeSegment, double)}
   * 
   * @param lastEdgeSegment      to use
   * @param acceptedPathFlowRate
   */
  @Override
  protected void applyPathFinalSegmentFlowUpdate(EdgeSegment lastEdgeSegment, double acceptedPathFlowRate) {
    dataConfig.sendingFlows[(int) lastEdgeSegment.getId()] += acceptedPathFlowRate;
  }

  /**
   * constructor
   * 
   * @param odPaths                          to use
   * @param linkSegmentFlowAcceptanceFactors flow acceptance factors to apply to path flows while traversing the path
   * @param linkSegmentSendingFlows          to populate
   */
  public PathLinkSendingFlowUpdateConsumer(final NetworkFlowUpdateData dataConfig, final OdPaths odPaths) {
    super(dataConfig, odPaths);
  }

}

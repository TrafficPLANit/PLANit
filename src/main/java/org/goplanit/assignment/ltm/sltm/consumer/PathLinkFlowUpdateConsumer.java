package org.goplanit.assignment.ltm.sltm.consumer;

import java.util.logging.Logger;

import org.goplanit.od.path.OdPaths;
import org.goplanit.utils.graph.directed.EdgeSegment;

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
   * For each entry segment update the in(sending)flow
   * 
   * @param prevSegment         to use
   * @param currentSegment      to use
   * @param turnSendingFlowPcuH to use
   */
  @Override
  protected double applySingleFlowUpdate(final EdgeSegment prevSegment, final EdgeSegment currentSegment, double turnSendingFlowPcuH) {
    /* u_a: update inflow for link segment */
    int prevSegmentId = (int) prevSegment.getId();
    dataConfig.sendingFlows[prevSegmentId] += turnSendingFlowPcuH;
    return turnSendingFlowPcuH * dataConfig.flowAcceptanceFactors[prevSegmentId];
  }

  /**
   * Apply final path flow on last segment that otherwise would not have been updated in the turn based {@link #applySingleFlowUpdate(EdgeSegment, EdgeSegment, double)}
   * 
   * @param lastEdgeSegment      to use
   * @param acceptedPathFlowRate to use
   */
  @Override
  protected void applyPathFinalSegmentFlowUpdate(EdgeSegment lastEdgeSegment, double acceptedPathFlowRate) {
    dataConfig.sendingFlows[(int) lastEdgeSegment.getId()] += acceptedPathFlowRate;
  }

  /**
   * Constructor
   * 
   * @param dataConfig containing data configuration to use
   * @param odPaths    to use
   */
  public PathLinkFlowUpdateConsumer(final NetworkFlowUpdateData dataConfig, final OdPaths odPaths) {
    super(dataConfig, odPaths);
  }

}

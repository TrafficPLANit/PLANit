package org.goplanit.assignment.ltm.sltm.consumer;

import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.ojalgo.array.Array1D;
import org.ojalgo.array.Array2D;
import org.ojalgo.function.aggregator.Aggregator;

/**
 * A functional class that consumes the result of a node model update in order to determine the next accepted outflows of incoming links of all nodes it is applied to
 * 
 * @author markr
 *
 */
public class UpdateEntryLinksOutflowConsumer implements ApplyToNodeModelResult {

  /** the array to store the accepted outflows in while updating the receiving flows */
  private final double[] outflowsToPopulate;

  /**
   * Constructor
   * 
   * @param outflowsToPopulate to use
   */
  public UpdateEntryLinksOutflowConsumer(final double[] outflowsToPopulate) {
    this.outflowsToPopulate = outflowsToPopulate;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void consumeCentroidResult(final DirectedVertex node, final double[] linkSegmentSendingFlows) {
    int linkSegmentId = 0;
    for (EdgeSegment entryLinkSegment : node.getEntryEdgeSegments()) {
      linkSegmentId = (int) entryLinkSegment.getId();
      outflowsToPopulate[linkSegmentId] = linkSegmentSendingFlows[linkSegmentId];
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void consumeRegularResult(DirectedVertex node, Array1D<Double> flowAcceptanceFactor, Array2D<Double> turnSendingFlows) {
    int entryIndex = 0;
    int linkSegmentId = 0;
    for (EdgeSegment entryLinkSegment : node.getEntryEdgeSegments()) {
      linkSegmentId = (int) entryLinkSegment.getId();
      /* s_a = Sum_b(s_ab) */
      double sendingFlow = turnSendingFlows.aggregateRow(entryIndex, Aggregator.SUM);
      /* v_a = s_a * alpha_a */
      double acceptedOutflow = sendingFlow * flowAcceptanceFactor.get(entryIndex);
      outflowsToPopulate[linkSegmentId] = acceptedOutflow;
      ++entryIndex;
    }
  }

}

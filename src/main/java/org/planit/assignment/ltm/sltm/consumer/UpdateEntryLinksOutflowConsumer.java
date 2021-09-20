package org.planit.assignment.ltm.sltm.consumer;

import org.ojalgo.array.Array1D;
import org.ojalgo.array.Array2D;
import org.ojalgo.function.aggregator.Aggregator;
import org.planit.algorithms.nodemodel.TampereNodeModel;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.network.layer.physical.Node;

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
  public void accept(final Node potentiallyBlockingNode, final Array1D<Double> localFlowAcceptanceFactor, final TampereNodeModel nodeModel) {

    /* s_ab */
    Array2D<Double> turnSendingFlows = nodeModel.getInputs().getTurnSendingFlows();

    int entryIndex = 0;
    for (EdgeSegment entryLinkSegment : potentiallyBlockingNode.getEntryLinkSegments()) {
      int linkSegmentId = (int) entryLinkSegment.getId();
      /* s_a = Sum_b(s_ab) */
      double sendingFlow = turnSendingFlows.aggregateRow(entryIndex, Aggregator.SUM);
      /* v_a = s_a * alpha_a */
      double acceptedOutflow = sendingFlow * localFlowAcceptanceFactor.get(entryIndex);
      outflowsToPopulate[linkSegmentId] = acceptedOutflow;
      ++entryIndex;
    }

  }

}

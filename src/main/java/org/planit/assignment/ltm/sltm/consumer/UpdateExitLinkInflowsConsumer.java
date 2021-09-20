package org.planit.assignment.ltm.sltm.consumer;

import org.ojalgo.array.Array1D;
import org.ojalgo.array.Array2D;
import org.ojalgo.function.PrimitiveFunction;
import org.ojalgo.function.aggregator.Aggregator;
import org.planit.algorithms.nodemodel.TampereNodeModel;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.network.layer.physical.Node;

/**
 * A functional class that consumes the result of a node model update in order to update the inflows of all the outgoing links of the nodes it is applied to *
 * 
 * @author markr
 *
 */
public class UpdateExitLinkInflowsConsumer implements ApplyToNodeModelResult {

  /** the next sending flows to update based on the found accepted in flows on outgoing links of the node */
  private double[] inFlowsToUpdate;

  /**
   * Constructor
   * 
   * @param inFlowsToUpdate to use
   */
  public UpdateExitLinkInflowsConsumer(final double[] inFlowsToUpdate) {
    this.inFlowsToUpdate = inFlowsToUpdate;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void accept(final Node potentiallyBlockingNode, final Array1D<Double> localFlowAcceptanceFactor, final TampereNodeModel nodeModel) {
    Array2D<Double> turnSendingFlows = nodeModel.getInputs().getTurnSendingFlows();

    /* v_ab = s_ab*alpha_a: Convert turn sending flows to turn accepted flows (to avoid duplication we reuse sending flow 2d array) */
    for (int entryIndex = 0; entryIndex < localFlowAcceptanceFactor.length; ++entryIndex) {
      turnSendingFlows.modifyRow(entryIndex, PrimitiveFunction.MULTIPLY.by(localFlowAcceptanceFactor.get(entryIndex)));
    }
    /* u_b = SUM_a(v_ab): set inflow */
    int exitIndex = 0;
    for (EdgeSegment exitLinkSegment : potentiallyBlockingNode.getExitLinkSegments()) {
      inFlowsToUpdate[(int) exitLinkSegment.getId()] = turnSendingFlows.aggregateColumn(exitIndex++, Aggregator.SUM);
    }
  }

}

package org.planit.assignment.ltm.sltm;

import org.ojalgo.array.Array1D;
import org.ojalgo.array.Array2D;
import org.ojalgo.function.PrimitiveFunction;
import org.ojalgo.function.aggregator.Aggregator;
import org.planit.algorithms.nodemodel.TampereNodeModel;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.network.layer.physical.Node;

/**
 * A functional class that consumes the result of a node model update in order to update the next sending flows of all the outgoing links of the node in question.
 * 
 * @author markr
 *
 */
public class UpdateNextSendingFlowsConsumer implements ApplyToNodeModelResult {

  /** the next sending flows to update based on the found accepted in flows on outgoing links of the node */
  private double[] nextSendingFlows;

  /**
   * Constructor
   * 
   * @param nextSendingFlows to use
   */
  public UpdateNextSendingFlowsConsumer(final double[] nextSendingFlows) {
    this.nextSendingFlows = nextSendingFlows;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void accept(final Node potentiallyBlockingNode, final Array1D<Double> localFlowAcceptanceFactor, final TampereNodeModel nodeModel) throws PlanItException {
    Array2D<Double> turnSendingFlows = nodeModel.getInputs().getTurnSendingFlows();

    /* v_ab = s_ab*alpha_a: Convert turn sending flows to turn accepted flows (to avoid duplication we reuse sending flow 2d array) */
    for (int entryIndex = 0; entryIndex < localFlowAcceptanceFactor.length; ++entryIndex) {
      turnSendingFlows.modifyRow(entryIndex, PrimitiveFunction.MULTIPLY.by(localFlowAcceptanceFactor.get(entryIndex)));
    }
    /* s^tilde_b = SUM(v_ab): set next sending flow */
    int exitIndex = 0;
    for (EdgeSegment exitLinkSegment : potentiallyBlockingNode.getExitLinkSegments()) {
      nextSendingFlows[(int) exitLinkSegment.getId()] = turnSendingFlows.aggregateColumn(exitIndex++, Aggregator.SUM);
    }
  }

}

package org.goplanit.assignment.ltm.sltm.consumer;

import org.goplanit.algorithms.nodemodel.NodeModel;
import org.goplanit.algorithms.nodemodel.TampereNodeModel;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.math.Precision;
import org.ojalgo.array.Array1D;
import org.ojalgo.array.Array2D;
import org.ojalgo.function.PrimitiveFunction;
import org.ojalgo.function.aggregator.Aggregator;

/**
 * A functional class that consumes the result of a node model update in order to update the inflows of all the outgoing links of the nodes it is applied to *
 * 
 * @author markr
 *
 */
public class NMRUpdateExitLinkInflowsConsumer implements ApplyToNodeModelResult {

  /** the next sending flows to update based on the found accepted in flows on outgoing links of the node */
  private double[] inFlowsToUpdate;

  /**
   * Constructor
   * 
   * @param inFlowsToUpdate to use
   */
  public NMRUpdateExitLinkInflowsConsumer(final double[] inFlowsToUpdate) {
    this.inFlowsToUpdate = inFlowsToUpdate;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void acceptNonBlockingLinkBasedResult(final DirectedVertex node, double[] sendingFlows) {
    int segmentId = -1;
    for (var exitLinkSegment : node.getExitEdgeSegments()) {
      segmentId = (int) exitLinkSegment.getId();
      inFlowsToUpdate[segmentId] = sendingFlows[segmentId];
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void acceptTurnBasedResult(final DirectedVertex node, final Array1D<Double> flowAcceptanceFactors, final NodeModel nodeModel) {
    // TODO: should not cast directly
    Array2D<Double> turnSendingFlows = ((TampereNodeModel) nodeModel).getInputs().getTurnSendingFlows();

    /* v_ab = s_ab*alpha_a: Convert turn sending flows to turn accepted flows (to avoid duplication we reuse sending flow 2d array) */
    for (int entryIndex = 0; entryIndex < flowAcceptanceFactors.length; ++entryIndex) {
      double alpha = flowAcceptanceFactors.get(entryIndex);
      if (Precision.smaller(alpha, 1)) {
        turnSendingFlows.modifyRow(entryIndex, PrimitiveFunction.MULTIPLY.by(alpha));
      }
    }
    /* u_b = SUM_a(v_ab): set inflow */
    int exitIndex = 0;
    for (var exitLinkSegment : node.getExitEdgeSegments()) {
      inFlowsToUpdate[(int) exitLinkSegment.getId()] = turnSendingFlows.aggregateColumn(exitIndex++, Aggregator.SUM);
    }
  }

}

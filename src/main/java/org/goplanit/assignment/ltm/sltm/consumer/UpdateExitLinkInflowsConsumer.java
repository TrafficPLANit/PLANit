package org.goplanit.assignment.ltm.sltm.consumer;

import org.goplanit.utils.graph.EdgeSegment;
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
  public void consumeCentroidResult(final DirectedVertex node, double[] sendingFlows) {
    int segmentId = -1;
    for (EdgeSegment exitLinkSegment : node.getExitEdgeSegments()) {
      segmentId = (int) exitLinkSegment.getId();
      inFlowsToUpdate[segmentId] = sendingFlows[segmentId];
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void consumeRegularResult(final DirectedVertex node, final Array1D<Double> flowAcceptanceFactors, final Array2D<Double> turnSendingFlows) {
    /* v_ab = s_ab*alpha_a: Convert turn sending flows to turn accepted flows (to avoid duplication we reuse sending flow 2d array) */
    for (int entryIndex = 0; entryIndex < flowAcceptanceFactors.length; ++entryIndex) {
      double alpha = flowAcceptanceFactors.get(entryIndex);
      if (Precision.isSmaller(alpha, 1)) {
        turnSendingFlows.modifyRow(entryIndex, PrimitiveFunction.MULTIPLY.by(alpha));
      }
    }
    /* u_b = SUM_a(v_ab): set inflow */
    int exitIndex = 0;
    for (EdgeSegment exitLinkSegment : node.getExitEdgeSegments()) {
      inFlowsToUpdate[(int) exitLinkSegment.getId()] = turnSendingFlows.aggregateColumn(exitIndex++, Aggregator.SUM);
    }
  }

}

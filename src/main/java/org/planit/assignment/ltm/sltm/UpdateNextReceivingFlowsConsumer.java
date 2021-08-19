package org.planit.assignment.ltm.sltm;

import org.ojalgo.array.Array1D;
import org.ojalgo.array.Array2D;
import org.ojalgo.function.aggregator.Aggregator;
import org.planit.algorithms.nodemodel.TampereNodeModel;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.layer.physical.Node;

/**
 * A functional class that consumes the result of a node model update in order to update the next receiving flows of all the incoming links of the node in question.
 * 
 * @author markr
 *
 */
public class UpdateNextReceivingFlowsConsumer implements ApplyToNodeModelResult {

  /** the next sending receiving flows to update based on the found accepted in flows on incoming links of the node */
  private final double[] nextReceivingFlows;

  /** the array to store the accepted outflows in while updating the receiving flows */
  private final double[] acceptedOutflows;

  /**
   * Constructor
   * 
   * @param nextReceivingFlows to use
   */
  public UpdateNextReceivingFlowsConsumer(final double[] nextReceivingFlows, final double[] acceptedOutflows) {
    this.nextReceivingFlows = nextReceivingFlows;
    this.acceptedOutflows = acceptedOutflows;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void accept(final Node potentiallyBlockingNode, final Array1D<Double> localFlowAcceptanceFactor, final TampereNodeModel nodeModel) throws PlanItException {

    /* s_ab */
    Array2D<Double> turnSendingFlows = nodeModel.getInputs().getTurnSendingFlows();

    int entryIndex = 0;
    for (MacroscopicLinkSegment entryLinkSegment : potentiallyBlockingNode.<MacroscopicLinkSegment>getEntryLinkSegments()) {
      int linkSegmentId = (int) entryLinkSegment.getId();
      /* s_a = Sum_b(s_ab) */
      double sendingFlow = turnSendingFlows.aggregateRow(entryIndex++, Aggregator.SUM);
      /* v_a = s_a * alpha_a */
      double acceptedOutflow = sendingFlow * localFlowAcceptanceFactor.get(entryIndex);
      acceptedOutflows[linkSegmentId] = acceptedOutflow;
      /* storage_capacity_a = (L*FD^-1(v_a))/T) */
      double storageCapacity = Double.POSITIVE_INFINITY; // TODO: entryLinkSegment.getParentLink().getLengthKm() * etc.;
      /* r_a = min(C_a, v_a + storage_Capacity_a) */
      double receivingFlow = Math.min(entryLinkSegment.computeCapacityPcuH(), acceptedOutflow + storageCapacity);
      nextReceivingFlows[linkSegmentId] = receivingFlow;
      ++entryIndex;
    }

  }

}

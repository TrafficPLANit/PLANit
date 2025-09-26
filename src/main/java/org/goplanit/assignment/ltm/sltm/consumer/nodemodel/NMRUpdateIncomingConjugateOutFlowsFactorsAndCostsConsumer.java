package org.goplanit.assignment.ltm.sltm.consumer.nodemodel;

import org.goplanit.algorithms.nodemodel.NodeModel;
import org.goplanit.algorithms.nodemodel.TampereNodeModelUtils;
import org.goplanit.assignment.ltm.sltm.util.ConjugateCostUtils;
import org.goplanit.assignment.ltm.sltm.StaticLtmConjugateBushStrategy;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.mode.Mode;
import org.ojalgo.array.Array2D;
import org.ojalgo.function.aggregator.Aggregator;

/**
 * A functional class that consumes the result of a node model update in order to update all its incoming conjugate
 * segment (turn) outflows, acceptance factors and costs on the network and conjugate network level
 * 
 * @author markr
 *
 */
public class NMRUpdateIncomingConjugateOutFlowsFactorsAndCostsConsumer implements NodeModelRunTurnBasedResultConsumer {

  private final  DirectedVertex node;

  private final StaticLtmConjugateBushStrategy assignmentStrategy;
  private final Mode theMode;
  private final double[] originalNetworkCosts;
  private final double[] conjNetworkCosts;
  private final boolean doCostUpdate;
  private final boolean doOutflowUpdate;

  /** consumer to use to apply discontinuity cost update */
  private final DiscontinuityTurnCostReplacementConsumer discontinuityTurnCostReplacementConsumer;

  /**
   * Update flows and "normal" costs based on flows on a link level first
   *
   * @param entrySegment  at hand
   * @param flowAcceptanceFactor new factor
   */
  private void processLinkBasedResult(EdgeSegment entrySegment, Double flowAcceptanceFactor){
    if(entrySegment == null){
      return;
    }

    int entrySegmentId = (int)entrySegment.getId();
    var networkLoading = assignmentStrategy.getLoading();

    // update all incoming flow acceptance factors (link level in sLTM currently)
    networkLoading.getCurrentFlowAcceptanceFactors()[entrySegmentId] = flowAcceptanceFactor;

    // update outflow based on new acceptance factor
    if(doOutflowUpdate) {
      networkLoading.getCurrentOutflowsPcuH()[entrySegmentId] =
          networkLoading.getCurrentSendingFlowsPcuH()[entrySegmentId] * flowAcceptanceFactor;
    }

    // update cost
    if(doCostUpdate) {
      ConjugateCostUtils.updateLinkAndConjugateSegmentCost(
          entrySegment, assignmentStrategy, theMode, originalNetworkCosts, conjNetworkCosts);
    }
  }

  /**
   * Constructor for simplified approach where we do not update costs
   *
   * @param node           the model is run for
   * @param theMode        to use
   * @param assignmentStrategy to update
   * @param updateNetworkOutflows flag indicating if outflows are to be updated
   */
  public NMRUpdateIncomingConjugateOutFlowsFactorsAndCostsConsumer(
      DirectedVertex node,
      Mode theMode,
      final StaticLtmConjugateBushStrategy assignmentStrategy,
      boolean updateNetworkOutflows) {
    this.assignmentStrategy = assignmentStrategy;
    this.node = node;

    this.theMode = theMode;

    this.doOutflowUpdate = updateNetworkOutflows;

    this.doCostUpdate = false;
    this.originalNetworkCosts = null;
    this.conjNetworkCosts = null;
    this.discontinuityTurnCostReplacementConsumer = null;
  }

  /**
   * Constructor
   *
   * @param node           the model is run for
   * @param theMode        to use
   * @param assignmentStrategy to update
   * @param originalNetworkCosts to update
   * @param conjNetworkCosts to update
   * @param updateNetworkOutflows flag indicating if outflows are to be updated
   */
  public NMRUpdateIncomingConjugateOutFlowsFactorsAndCostsConsumer(
      DirectedVertex node,
      Mode theMode,
      final StaticLtmConjugateBushStrategy assignmentStrategy,
      double[] originalNetworkCosts,
      double[] conjNetworkCosts,
      boolean updateNetworkOutflows) {
    this.assignmentStrategy = assignmentStrategy;
    this.node = node;

    this.theMode = theMode;

    this.doOutflowUpdate = updateNetworkOutflows;

    this.doCostUpdate = true;
    this.originalNetworkCosts = originalNetworkCosts;
    this.conjNetworkCosts = conjNetworkCosts;

    this.discontinuityTurnCostReplacementConsumer =
        new DiscontinuityTurnCostReplacementConsumer(
            assignmentStrategy.getLoading(),
            theMode,
            assignmentStrategy.getPhysicalCost(),
            assignmentStrategy.getVirtualCost(),
            assignmentStrategy.getTurn2ConjugateSegmentMapping(),
            conjNetworkCosts);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void acceptNonBlockingLinkBasedResult(final DirectedVertex node, double[] sendingFlows) {
    // change in flow may still result in change in cost even if it is a non-blocking node. So trigger
    // using flow acceptance factors of 1
    node.getEntryEdgeSegments().forEach(es -> processLinkBasedResult(es, 1.0));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void acceptTurnBasedResult(
      final DirectedVertex node, final Array2D<Double> flowAcceptanceFactors, final NodeModel nodeModel) {

    // update acceptance factors and outflow for each incoming link
    TampereNodeModelUtils.forEachLinkBasedResult(
        node, this::processLinkBasedResult, flowAcceptanceFactors.reduceRows(Aggregator.MAXIMUM));

    if(doCostUpdate) {
      // update discontinuity based costs if any change for each affected turn
      TampereNodeModelUtils.forEachTurnBasedResult(
          node, flowAcceptanceFactors, discontinuityTurnCostReplacementConsumer);
    }
  }

}

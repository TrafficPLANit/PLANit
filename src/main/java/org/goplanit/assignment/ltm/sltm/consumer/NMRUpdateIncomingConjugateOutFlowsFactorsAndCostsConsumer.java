package org.goplanit.assignment.ltm.sltm.consumer;

import org.goplanit.algorithms.nodemodel.NodeModel;
import org.goplanit.algorithms.nodemodel.TampereNodeModelUtils;
import org.goplanit.assignment.ltm.sltm.conjugate.ConjugateCostUtils;
import org.goplanit.assignment.ltm.sltm.conjugate.StaticLtmConjugateBushStrategy;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.virtual.physical.ConnectoidSegment;
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
    networkLoading.getCurrentOutflowsPcuH()[entrySegmentId] =
      networkLoading.getCurrentSendingFlowsPcuH()[entrySegmentId] * flowAcceptanceFactor;

    // update cost
    ConjugateCostUtils.updateLinkAndConjugateSegmentCost(
        entrySegment, assignmentStrategy, theMode, originalNetworkCosts, conjNetworkCosts);
  }

  /**
   * Constructor
   *
   * @param node           the model is run for
   * @param theMode        to use
   * @param assignmentStrategy to update
   * @param originalNetworkCosts to update
   * @param conjNetworkCosts to update
   */
  public NMRUpdateIncomingConjugateOutFlowsFactorsAndCostsConsumer(
      DirectedVertex node,
      Mode theMode,
      final StaticLtmConjugateBushStrategy assignmentStrategy,
      double[] originalNetworkCosts,
      double[] conjNetworkCosts) {
    this.assignmentStrategy = assignmentStrategy;
    this.node = node;

    this.theMode = theMode;
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
    // update discontinuity based costs if any change for each affected turn
    TampereNodeModelUtils.forEachTurnBasedResult(
        node, flowAcceptanceFactors, discontinuityTurnCostReplacementConsumer);
  }

}

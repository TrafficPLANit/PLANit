package org.goplanit.assignment.ltm.sltm.consumer;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingBushConjugate;
import org.goplanit.cost.physical.AbstractPhysicalCost;
import org.goplanit.cost.virtual.AbstractVirtualCost;
import org.goplanit.utils.functionalinterface.TriConsumer;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.virtual.physical.ConnectoidSegment;

/**
 * Perform the conjugate turn based cost update after the node model calculation if a discontinuity is found
 */
public class DiscontinuityTurnCostReplacementConsumer implements TriConsumer<EdgeSegment, EdgeSegment, Double> {

  private final StaticLtmLoadingBushConjugate networkLoading;

  private final Mode theMode;

  private final AbstractPhysicalCost physicalCost;

  private final AbstractVirtualCost virtualCost;

  protected final MultiKeyMap<Object, ConjugateEdgeSegment> turn2ConjugateSegmentMapping;

  private final double[] conjSegmentCostsToUpdate;

  private int numDiscontinuities;


  public DiscontinuityTurnCostReplacementConsumer(
      StaticLtmLoadingBushConjugate networkLoading,
      Mode theMode,
      final AbstractPhysicalCost physicalCost,
      final AbstractVirtualCost virtualCost,
      final MultiKeyMap<Object, ConjugateEdgeSegment> turn2ConjugateSegmentMapping,
      double[] conjSegmentCostsToUpdate){
    this.networkLoading = networkLoading;
    this.theMode = theMode;
    this.physicalCost = physicalCost;
    this.virtualCost = virtualCost;
    this.turn2ConjugateSegmentMapping = turn2ConjugateSegmentMapping;

    this.conjSegmentCostsToUpdate = conjSegmentCostsToUpdate;
    this.numDiscontinuities = 0;
  }

  @Override
  public void accept(EdgeSegment entry, EdgeSegment exit, Double alpha) {
    // TODO: currently we do not actually check if it is a zero-flow turn, we should because due to slight
    //  inconsistencies in the iterative loading procedure this sometimes gets triggered for turns with non-zero flow
    //  which is not great.

    if(entry.getOppositeDirectionSegment() == exit){
      return;
    }
    var nlAppliedFlowAcceptanceFactor = networkLoading.getCurrentFlowAcceptanceFactors()[(int)entry.getId()];
    if(Precision.greaterEqual(alpha, nlAppliedFlowAcceptanceFactor, Precision.EPSILON_9)){
      return;
    }
    // discontinuity found since the turn acceptance factor is more restricting than the link based one applied in loading
    // this only happens at a zero flow discontinuity.

    //HACK: because the cost calculation hides its internal workings for now we modify the link outflow locally
    //      based on the changed alphas.
    // TODO: create a nice fix so we can compute generalised cost on-the-fly for a given flow acceptance factor
    double originalNlOutflow = networkLoading.getCurrentOutflowsPcuH()[(int)entry.getId()];
    double outflowConsistentWithNonZeroTurnFlow = networkLoading.getCurrentInflowsPcuH()[(int)entry.getId()] * alpha;
    networkLoading.getCurrentOutflowsPcuH()[(int)entry.getId()] = outflowConsistentWithNonZeroTurnFlow;
    double disContinuitySegmentCost = (entry instanceof ConnectoidSegment) ?
        virtualCost.getGeneralisedCost(theMode, (ConnectoidSegment) entry):
        physicalCost.getGeneralisedCost(theMode, (MacroscopicLinkSegment) entry);
    networkLoading.getCurrentOutflowsPcuH()[(int)entry.getId()] = originalNlOutflow; // place original cost back
    assert(originalNlOutflow >= outflowConsistentWithNonZeroTurnFlow);

    //3. overwrite existing costs for turns where discontinuity was found
    var conjugateSegment = turn2ConjugateSegmentMapping.get(entry, exit);
    assert (conjSegmentCostsToUpdate[(int)conjugateSegment.getId()] <= disContinuitySegmentCost);

    // in case cost has not changed (can happen in change in drop in alpha is offset in increased inflow due to
    // slight discrepancy when ending iterative loading procedure, then ignore update, otherwise, true
    // discontinuity found and update
    if(conjSegmentCostsToUpdate[(int)conjugateSegment.getId()] < disContinuitySegmentCost) {
      conjSegmentCostsToUpdate[(int) conjugateSegment.getId()] = disContinuitySegmentCost;
      ++numDiscontinuities;
    }
  }

  public int getNumDiscontinuitiesUpdated() {
    return numDiscontinuities;
  }
}

package org.goplanit.assignment.ltm.sltm.conjugate;

import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.network.virtual.physical.ConnectoidSegment;

public class ConjugateCostUtils {

  /**
   * Update network link segment and conjugate segment cost based on current prevailing network incoming link) flows
   *
   * @param nonConjugateEntrySegment to update costs for
   * @param assignmentStrategy to use
   * @param theMode to use
   * @param originalNetworkCosts to update
   * @param conjNetworkCosts to update
   *
   */
    public static void  updateLinkAndConjugateSegmentCost(
        EdgeSegment nonConjugateEntrySegment,
        StaticLtmConjugateBushStrategy assignmentStrategy,
        Mode theMode,
        double[] originalNetworkCosts,
        double[] conjNetworkCosts) {

    DirectedVertex node = nonConjugateEntrySegment.getDownstreamVertex();

    // UPDATE LINK COSTS
    double currentCost;
    if(nonConjugateEntrySegment instanceof MacroscopicLinkSegment) {
      // will use current network flows (including any shift applied via syncUncongestedPasFlowShiftToNetworkFlow
      currentCost = assignmentStrategy.getPhysicalCost().getGeneralisedCost(
          theMode, (MacroscopicLinkSegment) nonConjugateEntrySegment);
    }else{
      currentCost = assignmentStrategy.getVirtualCost().getGeneralisedCost(
          theMode, (ConnectoidSegment) nonConjugateEntrySegment);
    }
    originalNetworkCosts[(int)nonConjugateEntrySegment.getId()]  = currentCost;

    // UPDATE CONJ COSTS for each turn
    for(var exitSegment : node.getExitEdgeSegments()) {
      if(exitSegment.hasOppositeDirectionSegment() && exitSegment.getOppositeDirectionSegment() == nonConjugateEntrySegment){
        continue;
      }
      var conjSegment = assignmentStrategy.getTurn2ConjugateSegmentMapping().get(nonConjugateEntrySegment, exitSegment);
      if (conjSegment == null) {
        throw new PlanItRunTimeException("unable to find conjugate segment for turn [from: (%s), to: (%s)]",
            nonConjugateEntrySegment.getIdsAsString(), exitSegment.getIdsAsString());
      }
      conjNetworkCosts[(int) conjSegment.getId()] = currentCost;
    }
  }
}

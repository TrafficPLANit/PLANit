package org.goplanit.assignment.ltm.sltm.util;

import org.goplanit.assignment.ltm.sltm.StaticLtmConjugateBushStrategy;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.virtual.physical.ConnectoidSegment;

/**
 * Cost utilities for conjugate setups
 */
public class ConjugateCostUtils {


  /** Dummy constructor */
  private ConjugateCostUtils(){}

  /**
   * Update network link segment and conjugate segment cost based on current prevailing network incoming link flows
   *
   * @param turnEntrySegment to update costs for
   * @param assignmentStrategy to use
   * @param theMode to use
   * @param originalNetworkCosts to update
   * @param conjNetworkCosts to update
   *
   */
    public static void  updateLinkAndConjugateSegmentCost(
        EdgeSegment turnEntrySegment,
        StaticLtmConjugateBushStrategy assignmentStrategy,
        Mode theMode,
        double[] originalNetworkCosts,
        double[] conjNetworkCosts) {

    DirectedVertex node = turnEntrySegment.getDownstreamVertex();

    // UPDATE LINK COSTS
    double currentCost;
    if(turnEntrySegment instanceof MacroscopicLinkSegment) {
      // will use current network flows (including any shift applied via syncUncongestedPasFlowShiftToNetworkFlow
      currentCost = assignmentStrategy.getPhysicalCost().getGeneralisedCost(
          theMode, (MacroscopicLinkSegment) turnEntrySegment);
    }else{
      currentCost = assignmentStrategy.getVirtualCost().getGeneralisedCost(
          theMode, (ConnectoidSegment) turnEntrySegment);
    }
    originalNetworkCosts[(int)turnEntrySegment.getId()]  = currentCost;

    // UPDATE CONJ COSTS for each turn
    for(var turnExitSegment : node.getExitEdgeSegments()) {
      if(turnExitSegment.hasOppositeDirectionSegment() &&
          turnExitSegment.getOppositeDirectionSegment() == turnEntrySegment){
        continue;
      }
      //todo: can be done by index instead to be even faster as compiled index has functionality for it
      var conjSegment = assignmentStrategy.getTurn2ConjugateSegmentCompiledIndex().get(
          turnEntrySegment.getId(), turnExitSegment.getId());

      if (conjSegment == null) {
        throw new PlanItRunTimeException("unable to find conjugate segment for turn [from: (%s), to: (%s)]",
            turnEntrySegment.getIdsAsString(), turnExitSegment.getIdsAsString());
      }
      conjNetworkCosts[(int) conjSegment.getId()] = currentCost;
    }
  }
}

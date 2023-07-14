package org.goplanit.assignment.ltm.sltm;

import java.util.logging.Logger;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.utils.arrays.ArrayUtils;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.math.Precision;

/**
 * Functionality to conduct a PAS flow shift based on underlying destination based bush approach. A destination-based bush approach no longer requires labelling and should therefore outperform
 * origin-based alternatives.
 * 
 * @author markr
 *
 */
public class PasFlowShiftDestinationBasedExecutor extends PasFlowShiftExecutor {

  /**
   * Logger to use
   */
  private final static Logger LOGGER = Logger.getLogger(PasFlowShiftDestinationBasedExecutor.class.getCanonicalName());
  
  /** dummy label to use for all flow on destination based bush */
  private final BushFlowLabel dummyLabel;

  /**
   * Helper to perform a flow shift on a turn. If the turn has no more flow it is removed from the bush
   * 
   * @param bush        bush to use
   * @param turnEntry     turn entry segment
   * @param turnExit      turn exit segment
   * @param flowShiftPcuH turn flow shift to apply by adding this flow to the turn
   * @return new turn flow after shift
   */
  private double executeTurnFlowShift(RootedLabelledBush bush, EdgeSegment turnEntry, EdgeSegment turnExit, double flowShiftPcuH) {
    double newTurnFlow = bush.addTurnSendingFlow(turnEntry, dummyLabel, turnExit, dummyLabel, flowShiftPcuH, isPasS2RemovalAllowed());
    if (isPasS2RemovalAllowed() && !Precision.positive(newTurnFlow, EPSILON) && !Precision.positive(bush.getTurnSendingFlow(turnEntry, turnExit), EPSILON)) {
      /* no remaining flow at all on turn after flow shift, remove turn from bush entirely */
      bush.removeTurn(turnEntry, turnExit);
      newTurnFlow = 0.0;
    }
    return newTurnFlow;
  }

  /**
   * Perform the flow shift through the end merge vertex of the PASs high cost segment for the given origin bush flow composition
   * 
   * @param bush                      to use
   * @param s2FinalFlowShift          the flow shift applied so far up to the final merge
   * @param s2MergeExitSplittingRates splitting rates to use with multi-key (exit segment, label)
   * @return exitShiftedSendingFlows  found exit segment flows
   */
  private double[] executeBushS2FlowShiftEndMerge(RootedLabelledBush bush, double s2FinalFlowShift, MultiKeyMap<Object, Double> s2MergeExitSplittingRates) {

    var exitShiftedSendingFlows = new double[pasMergeVertexNumExitSegments];
    /* remove shifted flows through final merge towards exit segments proportionally, to later add to s1 turns through merge */
    if (pas.getMergeVertex().hasExitEdgeSegments()) {

      var lastS2Segment = pas.getLastEdgeSegment(false /* high cost */);

      /* key: [exitSegment, exitLabel] */
      int index = 0;
      for (var exitSegment : pas.getMergeVertex().getExitEdgeSegments()) {
        if (bush.containsEdgeSegment(exitSegment)) {
          Double splittingRate = s2MergeExitSplittingRates.get(exitSegment, dummyLabel);
          if (splittingRate == null || !Precision.positive(splittingRate, EPSILON)) {
            ++index;
            continue;
          }

          /* remove flow for s2 */
          double s2FlowShift = s2FinalFlowShift * splittingRate;
          double newturnFlow = bush.addTurnSendingFlow(lastS2Segment, dummyLabel, exitSegment, dummyLabel, s2FlowShift, isPasS2RemovalAllowed());
          if (isPasS2RemovalAllowed() && !Precision.positive(newturnFlow, EPSILON) && !Precision.positive(bush.getTurnSendingFlow(lastS2Segment, exitSegment), EPSILON)) {
            /* no remaining flow at all on turn after flow shift, remove turn from bush entirely */
            bush.removeTurn(lastS2Segment, exitSegment);
          }

          /* track so we can attribute it to s1 segment later */
          exitShiftedSendingFlows[index] += -s2FlowShift;
        }
        ++index;
      }
    }
    return exitShiftedSendingFlows;
  }

  /**
   * Perform the flow shift through the end merge vertex of the PASs low cost segment for the given origin bush flow composition
   * 
   * @param bush                      to use
   * @param s1FinalFlowShift          the flow shift applied so far up to the final merge
   * @param exitSegmentSplittingRates the splitting rates to apply towards the available exit segments
   */
  private void executeBushS1FlowShiftEndMerge(RootedLabelledBush bush, double s1FinalFlowShift, double[] exitSegmentSplittingRates) {

    /* add shifted flows through final merge towards exit segments proportionally based on labelled exit usage */
    if (pas.getMergeVertex().hasExitEdgeSegments()) {
      EdgeSegment lastS1Segment = pas.getLastEdgeSegment(true /* low cost */);

      int index = 0;
      for (var exitSegment : pas.getMergeVertex().getExitEdgeSegments()) {
        double splittingRate = exitSegmentSplittingRates[index];
        if (Precision.positive(splittingRate, EPSILON)) {
          /* add flow for s1 */
          double s1FlowShift = s1FinalFlowShift * splittingRate;
          double newLabelledTurnFlow = bush.addTurnSendingFlow(lastS1Segment, dummyLabel, exitSegment, dummyLabel, s1FlowShift);
          if (!Precision.positive(newLabelledTurnFlow, EPSILON)) {
            LOGGER.severe("Flow shift towards cheaper S1 alternative should always result in non-negative remaining flow, but this was not found, this shouldn't happen");
          }
        }
        ++index;
      }
    }
  }

  /**
   * Execute a flow shift on a given bush for the given entry+PAS alternative. This does not move flow through the final merge vertex but does flow through the initial diverge.
   * 
   * @param origin                bush at hand
   * @param entrySegment          entry segment for the initial turn leading into the pasSegment
   * @param flowShiftPcuH         to execute (assumed to be correctly proportioned in relation to other bushes and labels within bush for this PAS)
   * @param pasSegment            to update on bush
   * @param flowAcceptanceFactors to use when updating the flows
   * @return sending flow on last edge segment of the PAS alternative after the flow shift (considering encountered reductions)
   */
  private double executeBushPasFlowShift(final RootedLabelledBush origin, final EdgeSegment entrySegment, double flowShiftPcuH,
                                         final EdgeSegment[] pasSegment, final double[] flowAcceptanceFactors) {

    /* initial turn into pas segment */
    int index = 0;
    EdgeSegment currentSegment = entrySegment;
    var nextSegment = pasSegment[index];

    executeTurnFlowShift(origin, entrySegment, nextSegment, flowShiftPcuH);
    flowShiftPcuH *= flowAcceptanceFactors[(int) entrySegment.getId()];

    /* pas alternative itself */
    while (++index < pasSegment.length) {
      currentSegment = nextSegment;
      nextSegment = pasSegment[index];

      executeTurnFlowShift(origin, currentSegment, nextSegment, flowShiftPcuH);
      flowShiftPcuH *= flowAcceptanceFactors[(int) currentSegment.getId()];
    }

    return flowShiftPcuH;
  }

  /**
   * {@inheritDoc}
   */
  protected void executeBushFlowShift(RootedLabelledBush bush, EdgeSegment entrySegment, double bushFlowShift, double[] flowAcceptanceFactors) {
    /* prep - pas */
    final var s2 = pas.getAlternative(false);
    final var s1 = pas.getAlternative(true);

    /*
     * ------------------------------------------------- S2 FLOW SHIFT ----------------------------------------------------------------------------------------------------------
     * Update S2 by shifting flow proportionally along encountered flow compositions matching with the PAS/origin/alternative
     */

      /* obtain splitting rates before flow shift in case turns/edges are removed on S2, then splitting rate information is lost while required for final merge afterwards */
      var s2MergeExitSplittingRates = bush.getSplittingRates(pas.getLastEdgeSegment(false /* high cost */), dummyLabel);

      double s2StartLabeledFlowShift = -bushFlowShift;
      double s2FinalLabeledFlowShift = executeBushPasFlowShift(bush, entrySegment, s2StartLabeledFlowShift, s2, flowAcceptanceFactors);

      /* shift flow across final merge for S2 */
      double[] bushS2MergeExitShiftedSendingFlows =executeBushS2FlowShiftEndMerge(bush, s2FinalLabeledFlowShift, s2MergeExitSplittingRates);

    /* convert flows to portions by label */
    ArrayUtils.divideBySum(bushS2MergeExitShiftedSendingFlows, 0);
    var bushS2MergeExitShiftedSplittingRates = bushS2MergeExitShiftedSendingFlows;
    bushS2MergeExitShiftedSendingFlows = null;

    /*
     * ------------------------------------------------- S1 FLOW SHIFT ----------------------------------------------------------------------------------------------------------
     * Update S1 by shifting flow proportionally along encountered flow compositions matching with the PAS/origin/alternative
     */
      double s1StartLabeledFlowShift = bushFlowShift;
      double s1FinalLabeledFlowShift = executeBushPasFlowShift(bush, entrySegment, s1StartLabeledFlowShift, s1, flowAcceptanceFactors);

      /* shift flow across final merge for S1 based on findings in s2 */
      executeBushS1FlowShiftEndMerge(bush, s1FinalLabeledFlowShift, bushS2MergeExitShiftedSplittingRates);
  }

  /**
   * Constructor
   * 
   * @param pas      to use
   * @param settings to use
   * @param dummyLabel used as only label for each destination bush
   */
  protected PasFlowShiftDestinationBasedExecutor(final Pas pas, final StaticLtmSettings settings, final BushFlowLabel dummyLabel) {
    super(pas, settings);
    this.dummyLabel = dummyLabel;
  }

}

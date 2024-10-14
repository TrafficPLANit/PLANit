package org.goplanit.assignment.ltm.sltm;

import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.utils.arrays.ArrayUtils;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.IterableUtils;
import org.goplanit.utils.misc.Pair;

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
  private double executeTurnFlowShift(
          RootedLabelledBush bush, EdgeSegment turnEntry, EdgeSegment turnExit, double flowShiftPcuH) {

    // track what edge segments were added to what bush, so we can (in case of overlapping PAS update allowance)
    // flag if additional cycle checks are needed for subsequent PASs that may not be compatible with this current
    // PAS that we chose to prefer over those later ones
    if(flowShiftPcuH > 0){
      if(!bush.containsEdgeSegment(turnEntry.getId())){
        addBushAddedLinkSegment(bush, turnEntry);
      }
      if(!bush.containsEdgeSegment(turnExit.getId())){
        addBushAddedLinkSegment(bush, turnExit);
      }
    }
    // when we are reducing flow (negative flow shift) --> avoid rounding issues, ugly but necessary...
    else {

      // ...and the turn entry link segment was removed from the bush in
      // the previous shift then we should remove all turn sending flow. By explicitly setting this value we avoid rounding issues
      // and ensures that high cost segment flows get removed in its entirety when we no longer route flow through them
      // todo: now that we explicitly check for this earlier, this should not be necessary anymore!
      if(!bush.containsEdgeSegment(turnEntry)){
        var availableFlow = bush.getTurnSendingFlow(
            turnEntry, dummyLabel, turnExit, dummyLabel);
        if(Precision.greater(availableFlow, -flowShiftPcuH, Precision.EPSILON_3)) {
          LOGGER.severe(String.format("adding %.6f to flow shift (%.10f) to empty already removed turn (from: %s, to: %s) when removing turn flow" +
                          "from bush (%s)",
              availableFlow + flowShiftPcuH, -availableFlow, turnEntry.getIdsAsString(), turnExit.getIdsAsString(), bush.getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
        }
        flowShiftPcuH = -availableFlow; // sync
      }
      double totalSendingFlowIntoExit = IterableUtils.asStream(turnExit.getUpstreamVertex().getEntryEdgeSegments()).mapToDouble(
          es -> bush.getTurnSendingFlow(es, dummyLabel, turnExit, dummyLabel)).sum();
      if(totalSendingFlowIntoExit<=0){
        // dangling segment with no more entering flow, meaning that due to rounding ALL residual exiting turn flow
        // should be removed even if it exceeds the flow shift
        flowShiftPcuH = Math.min(flowShiftPcuH,-bush.getSendingFlowPcuH(turnExit));
        if(flowShiftPcuH > Precision.EPSILON_3){
          LOGGER.severe(String.format("Found dangling edge segment on bush (%s) with ghost flow exceeding non-trivial amount, " +
              "this shouldn't happen", bush.getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
        }
      }
    }

    double newTurnFlow = bush.addTurnSendingFlow(
            turnEntry, dummyLabel, turnExit, dummyLabel, flowShiftPcuH);

    //todo make sure that when very close to zero we remove all flow on the high cost segment somehow
    // so we do not get into trouble with precision...
    if (!Precision.positive(newTurnFlow, EPSILON) &&
            !Precision.positive(bush.getTurnSendingFlow(turnEntry, turnExit), EPSILON)) {

      /* no remaining flow at all on turn after flow shift, remove turn from bush entirely */
      bush.removeTurn(turnEntry, turnExit);
      if(isDestinationTrackedForLogging(bush)){
        LOGGER.info(String.format("     [No more flow --> Removed turn: FROM (%s) TO (%s) from bush (%s)]", turnEntry.getIdsAsString(), turnExit.getIdsAsString(), bush.getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
      }

      if(!bush.containsEdgeSegment(turnEntry)) {
        addBushRemovedLinkSegment(bush, turnEntry);
      }
      if(!bush.containsEdgeSegment(turnExit)) {
        addBushRemovedLinkSegment(bush, turnExit);
      }
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
  private double[] executeBushS2FlowShiftEndMerge(
          RootedLabelledBush bush, double s2FinalFlowShift, MultiKeyMap<Object, Double> s2MergeExitSplittingRates) {

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

          // precision sync like in regular flow shift
          double currentFlow = bush.getTurnSendingFlow(lastS2Segment, dummyLabel, exitSegment, dummyLabel);
          if(currentFlow + s2FlowShift < 0){
            double diff= currentFlow + s2FlowShift;
            s2FlowShift = -currentFlow; // sync to available flow
          }

          double newturnFlow = bush.addTurnSendingFlow(lastS2Segment, dummyLabel, exitSegment, dummyLabel, s2FlowShift);
          if (!Precision.positive(newturnFlow, EPSILON) && !Precision.positive(bush.getTurnSendingFlow(lastS2Segment, exitSegment), EPSILON)) {
            /* no remaining flow at all on turn after flow shift, remove turn from bush entirely */
            bush.removeTurn(lastS2Segment, exitSegment);
            /* track for further processing, so we can deregister bush on other PASs with these links */
            if(bush.getSendingFlowPcuH(lastS2Segment) <= 0.0) {
              addBushRemovedLinkSegment(bush, lastS2Segment);
            }
            if(bush.getSendingFlowPcuH(exitSegment) <= 0.0) {
              addBushRemovedLinkSegment(bush, exitSegment);
            }
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
            LOGGER.severe(String.format(
                "Flow shift of (%.12f) towards cheaper S1 alternative on turn [from (%s), to (%s)] should result in non-negative flow, but found %.12f, this shouldn't happen",
                s1FlowShift, lastS1Segment.getIdsAsString(), exitSegment.getIdsAsString(), newLabelledTurnFlow));
          }
        }
        ++index;
      }
    }
  }

  /**
   * Execute a flow shift on a given bush for the given entry+PAS alternative. This does not move flow through the final merge vertex but does flow through the initial diverge.
   * 
   * @param bush                bush at hand
   * @param entrySegment          entry segment for the initial turn leading into the pasSegment
   * @param flowShiftPcuH         to execute (assumed to be correctly proportioned in relation to other bushes and labels within bush for this PAS)
   * @param pasSegment            to update on bush
   * @param flowAcceptanceFactors to use when updating the flows
   * @return sending flow on last edge segment of the PAS alternative after the flow shift (considering encountered reductions)
   */
  private double executeBushPasFlowShift(
          final RootedLabelledBush bush,
          final EdgeSegment entrySegment,
          double flowShiftPcuH,
          final EdgeSegment[] pasSegment,
          final double[] flowAcceptanceFactors) {

    /* initial turn into pas segment */
    int index = 0;
    EdgeSegment currentSegment = entrySegment;
    var nextSegment = pasSegment[index];

    double currentFlow = bush.getTurnSendingFlow(currentSegment, dummyLabel, nextSegment, dummyLabel);
    if(Precision.negative(currentFlow + flowShiftPcuH)){
      double diff= currentFlow + flowShiftPcuH;
      flowShiftPcuH = -currentFlow; // sync to available flow
    }
    double newFlow = executeTurnFlowShift(bush, currentSegment, nextSegment, flowShiftPcuH);
    double appliedFlowShift = newFlow-currentFlow;
    if(Precision.notEqual(Math.abs(appliedFlowShift), Math.abs(flowShiftPcuH))){
      double diff= currentFlow + flowShiftPcuH;
      flowShiftPcuH = appliedFlowShift;
      LOGGER.severe("sync shouldn't trigger");
    }
    flowShiftPcuH *= flowAcceptanceFactors[(int) entrySegment.getId()];

    /* pas alternative itself */
    while (++index < pasSegment.length) {
      currentSegment = nextSegment;
      nextSegment = pasSegment[index];

      currentFlow = bush.getTurnSendingFlow(currentSegment, dummyLabel, nextSegment, dummyLabel);
      if(currentFlow + flowShiftPcuH < 0){
        double diff= currentFlow + flowShiftPcuH;
        flowShiftPcuH = -currentFlow; // sync to available flow
      }
      newFlow = executeTurnFlowShift(bush, currentSegment, nextSegment, flowShiftPcuH);
      appliedFlowShift = newFlow-currentFlow;
      if(Precision.notEqual(Math.abs(appliedFlowShift), Math.abs(flowShiftPcuH))){
        double diff= currentFlow + flowShiftPcuH;
        flowShiftPcuH = appliedFlowShift;
        LOGGER.severe("sync shouldn't trigger");
      }
      flowShiftPcuH *= flowAcceptanceFactors[(int) currentSegment.getId()];
    }

    return flowShiftPcuH;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected double[] executeBushS2FlowShift(
          RootedLabelledBush bush,
          EdgeSegment entrySegment,
          double bushFlowShift,
          double[] flowAcceptanceFactors) {

    /* prep - pas */
    final var s2 = pas.getAlternative(false);

    /*
     * ------------------------------------------------- S2 FLOW SHIFT ----------------------------------------------------------------------------------------------------------
     * Update S2 by shifting flow proportionally along encountered flow compositions matching with the PAS/origin/alternative
     */

    /* obtain splitting rates before flow shift in case turns/edges are removed on S2, then splitting rate information is lost while required for final merge afterwards */
    var s2MergeExitSplittingRates = bush.getSplittingRates(pas.getLastEdgeSegment(false /* high cost */), dummyLabel);

    double s2StartLabeledFlowShift = -bushFlowShift;
    double s2FinalLabeledFlowShift =
            executeBushPasFlowShift(bush, entrySegment, s2StartLabeledFlowShift, s2, flowAcceptanceFactors);

    /* shift flow across final merge for S2 */
    double[] bushS2MergeExitShiftedSendingFlows =
            executeBushS2FlowShiftEndMerge(bush, s2FinalLabeledFlowShift, s2MergeExitSplittingRates);

    /* convert flows to splitting rates  */
    return ArrayUtils.divideBySum(bushS2MergeExitShiftedSendingFlows, 0);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeBushS1FlowShift(
          RootedLabelledBush bush,
          EdgeSegment entrySegment,
          double bushFlowShift,
          double[] flowAcceptanceFactors,
          double[] mergeExitSplittingRates) {

    var s1 = pas.getAlternative(true);

    double s1FinalLabeledFlowShift = executeBushPasFlowShift(
            bush,
            entrySegment,
            bushFlowShift,
            s1,
            flowAcceptanceFactors);

    /* shift flow across final merge for S1 based on findings in s2 */
    executeBushS1FlowShiftEndMerge(bush, s1FinalLabeledFlowShift, mergeExitSplittingRates);
  }

  /**
   * Constructor
   * 
   * @param pas      to use
   * @param settings to use
   * @param dummyLabel used as only label for each destination bush
   */
  protected PasFlowShiftDestinationBasedExecutor(
          final Pas pas, final StaticLtmSettings settings, final BushFlowLabel dummyLabel) {
    super(pas, settings);
    this.dummyLabel = dummyLabel;
  }

}

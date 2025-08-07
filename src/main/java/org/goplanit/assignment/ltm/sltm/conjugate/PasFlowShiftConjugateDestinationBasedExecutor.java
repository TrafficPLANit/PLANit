package org.goplanit.assignment.ltm.sltm.conjugate;

import org.goplanit.algorithms.nodemodel.TampereNodeModelFixedInput;
import org.goplanit.assignment.ltm.sltm.*;
import org.goplanit.assignment.ltm.sltm.consumer.NMRUpdateIncomingConjugateOutFlowsFactorsAndCostsConsumer;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingBushBase;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingBushConjugate;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmNetworkLoading;
import org.goplanit.cost.physical.AbstractPhysicalCost;
import org.goplanit.cost.physical.SteadyStateTravelTimeCost;
import org.goplanit.cost.virtual.AbstractVirtualCost;
import org.goplanit.gap.GapFunction;
import org.goplanit.sdinteraction.smoothing.Smoothing;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.misc.Triple;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.virtual.physical.ConnectoidSegment;
import org.goplanit.utils.pcu.PcuCapacitated;
import org.ojalgo.array.Array1D;
import org.ojalgo.function.PrimitiveFunction;
import org.ojalgo.function.aggregator.Aggregator;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.goplanit.utils.math.Precision.*;

/**
 * Functionality to conduct a PAS flow shift based on underlying destination based conjugate bush approach.
 *
 * @author markr
 *
 */
public class PasFlowShiftConjugateDestinationBasedExecutor
        extends PasFlowShiftExecutor<ConjugateDirectedVertex, ConjugateEdgeSegment> {

  private static final double STATE_CHANGE_LEEWAY_PERCENTAGE = 0.005;
  private static final double SLACK_ABSOLUTE_FLOW_LEEWAY = 1;

  private static double adjustFlowShiftBasedOnSlackFlow(double proposedShift, double slackFlowEstimate, boolean isS1){
    double leeway = Math.max(SLACK_ABSOLUTE_FLOW_LEEWAY, slackFlowEstimate * STATE_CHANGE_LEEWAY_PERCENTAGE);
    if(isS1) {
      return adjustFlowShiftBasedOnS1SlackFlow(proposedShift, slackFlowEstimate, leeway);
    }else{
      return adjustFlowShiftBasedOnS2SlackFlow(proposedShift, slackFlowEstimate, leeway);
    }
  }

  /** track total sending flow on s1 and s2 alternatives of this flow shift executor's PAS */
  private Pair<Double,Double> s1S2SendingFlows;

  /** access to original bush turn flows. To be used for constraining identifying available sending flows
   * for flow shifts. Requires updating when constraining further as part of this instance conducting
   * flow shifts, so it is available to other PASs as a constraint.
   * Note: it should NOT contain all original turn flows per bush, only those where the original turn flow
   * was at some point was reduced to save memory.
   * NOTE: not owned by this executor, owned by parent strategy
   * todo: this injected approach is ugly, needs refactoring at some point
   */
  private Map<ConjugateDestinationBush, ConjugateBushTurnData> originalBushTurnFlowTracker;

  /**
   * Logger to use
   */
  private final static Logger LOGGER = Logger.getLogger(
          PasFlowShiftConjugateDestinationBasedExecutor.class.getCanonicalName());

  private void updateOriginalBushTurnFlowTracker() {
    var s1Alternative = pas.getAlternative(true);
    var s2Alternative = pas.getAlternative(false);
    for(var bush : pas.getRegisteredBushes()){
      var conjBush = (ConjugateDestinationBush) bush;
      if(!originalBushTurnFlowTracker.containsKey(conjBush)){
        originalBushTurnFlowTracker.put(conjBush, new ConjugateBushTurnData(conjBush));
      }
      var originalBushTurnData = originalBushTurnFlowTracker.get(bush);
      boolean forceOriginalFlowRegistration  = true;
      for(var segment : s1Alternative){
        double flow = bush.getSendingFlowPcuH(segment);
        if(!originalBushTurnData.containsTurnSendingFlow(segment) && conjBush.contains(segment)){
          originalBushTurnData.addTurnSendingFlow(segment, flow, forceOriginalFlowRegistration);
        }
      }
      for(var segment : s2Alternative){
        double flow = bush.getSendingFlowPcuH(segment);
        if(!originalBushTurnData.containsTurnSendingFlow(segment)){
          originalBushTurnData.addTurnSendingFlow(segment, flow, forceOriginalFlowRegistration);
        }
      }
    }
  }

  /**
   * Unregister bushes with zero flow from PAS
   *
   * @param allowDanglingNodes flag indicating if we allow dangling nodes after removal
   */
  private void removeZeroFlowBushesFromPas(boolean allowDanglingNodes) {

    var iter = pas.getRegisteredBushes().iterator();
    while (iter.hasNext()) {
      ConjugateDestinationBush bush = (ConjugateDestinationBush) iter.next();

      // now remove any zero flow segments from any bush after completing the shifts
      boolean anyRemoved = bush.removeZeroFlowSegmentsIn(
          pas.getAlternative(true),allowDanglingNodes, isDestinationTrackedForLogging());
      anyRemoved = bush.removeZeroFlowSegmentsIn(
          pas.getAlternative(false), allowDanglingNodes, isDestinationTrackedForLogging()) || anyRemoved;
      if(anyRemoved){
        if(isDestinationTrackedForLogging(bush)){
          LOGGER.info(String.format("[Unregistering bush (%s) from PAS %s, no more S2 flow left]",
              bush.getRootZone().getIdsAsString(), pas));
        }
        iter.remove();
      }
    }
  }

  /**
   * Helper to perform a flow shift on a network turn in conjugate form.
   * If the turn has no more flow, the flow is set to zero but the turn is not removed. If this is desired,
   * it needs to be done manually after the fact by removing it from the underlying DAG
   * (we do not remove it because it may be added back in later, to avoid inefficiencies).
   *
   * @param conjBush        bush to use
   * @param conjSegment     turn entry segment
   * @param flowShiftPcuH turn flow shift to apply by adding this flow to the turn
   * @return new turn flow after shift
   */
  private static double executeSingleBushTurnFlowShift(
          ConjugateDestinationBush conjBush, ConjugateEdgeSegment conjSegment, double flowShiftPcuH) {
    return conjBush.addTurnSendingFlow(conjSegment, flowShiftPcuH);
  }

  private static Pair<Double, Map<ConjugateDestinationBush, Double>> executeAcrossBushesTurnFlowShift(
      ConjugateEdgeSegment conjSegment,
      double referenceWeight,
      Map<ConjugateDestinationBush, Double> bushWeights,
      double pasFlowShift,
      boolean updateBushWeights,
      boolean logAll) {

    Map<ConjugateDestinationBush, Double> perBushStartingFlowShifts = new TreeMap<>();

    double totalBushAppliedFlowShift = 0;
    for (var entry : bushWeights.entrySet()) {
      ConjugateDestinationBush conjBush = entry.getKey();
      double bushWeight = entry.getValue();

      /* In case of multiple used bushes -> proportionally apply the shift */
      double bushPortion = Math.min(1, bushWeight / referenceWeight);
      if (referenceWeight == 0) {
        throw new PlanItRunTimeException("reference weight to allocate bush flow shifts is zero, this should not happen, likely no flow was " +
            "shifted for S2 alternative so s1 shift should not be triggered? Check");
      }
      double bushWeightedFlowShift = pasFlowShift * bushPortion;

//        if (isDestinationTrackedForLogging(conjBush) || logAll) {
//          LOGGER.info(String.format(
//              "     Shift: %.9f - link segment (%s) - bush (%s) ",
//              bushWeightedFlowShift, currentConjSegment.getIdsAsString(), conjBush.getRootZone().getIdsAsString()));
//        }

      double currentFlow = conjBush.getTurnSendingFlow(conjSegment);
      if (currentFlow + bushWeightedFlowShift < 0) {
        bushWeightedFlowShift = -currentFlow; // sync to available flow
      }
      double newFlow = executeSingleBushTurnFlowShift(conjBush, conjSegment, bushWeightedFlowShift);
      double appliedBushFlowShift = newFlow - currentFlow;
      if (Precision.notEqual(Math.abs(appliedBushFlowShift), Math.abs(bushWeightedFlowShift))) {
        double diff = currentFlow + bushWeightedFlowShift;
        LOGGER.severe("sync shouldn't trigger");
      }

      if(updateBushWeights) {
        if (!perBushStartingFlowShifts.containsKey(conjBush)) {
          //track amount shifted per bush, so it can be used for S1 alternative (if this is s2)
          // we explicitly capture this in case the proposed shift could not be achieved and was truncated
          // to available flow, which should be considered when adding on S1 otherwise we may introduce
          // ghost flow.
          perBushStartingFlowShifts.put(conjBush, appliedBushFlowShift);
        }
      }
      // track so we know the amount to apply on the network level
      totalBushAppliedFlowShift += appliedBushFlowShift;
    }
    return Pair.of(totalBushAppliedFlowShift, perBushStartingFlowShifts);
  }

  /**
   *  Perform the flow shift on the diverge of a Pas
   * @param referenceWeight to use to apportion bushes (sum of bush weights)
   * @param bushWeights to use to distribute flow shift across bushes
   * @param pasFlowShift to use
   * @param theMode to use
   * @param originalNlFlowAcceptanceFactors to use
   * @param assignmentStrategy to use
   * @param originalNetworkCosts to update
   * @param conjSegmentCosts to update
   * @param bushes to use
   * @param updateNetworkNodeModel flag indicating if node model is to be updated
   * @param logAll flag for logging
   * @return first entry contains remaining flow shift to propagate for s1,s2 turns respectively, boolean reflects if
   * only to sync outflows or do proper update in full. Second entry contains the updated bush weights in case
   * adjustments were required because a bush did not have enough flow to accommodate the anticipated change. Both results
   * are to be used for the remainder of the PAS update.
   */
  private Pair<Pair<Double, Boolean>[],Map<ConjugateDestinationBush, Double>> executeDivergeFlowShift(
      double referenceWeight,
      Map<ConjugateDestinationBush, Double> bushWeights,
      double pasFlowShift,
      Mode theMode,
      double[] originalNlFlowAcceptanceFactors,
      StaticLtmConjugateBushStrategy assignmentStrategy,
      double[] originalNetworkCosts,
      double[] conjSegmentCosts,
      Set<ConjugateDestinationBush> bushes,
      boolean updateNetworkNodeModel,
      boolean logAll) {

    var networkLoading = assignmentStrategy.getLoading();

    ConjugateEdgeSegment[] s1Alternative = pas.getAlternative(true);
    ConjugateEdgeSegment s1Turn = s1Alternative[0];
    ConjugateEdgeSegment[] s2Alternative = pas.getAlternative(false);
    ConjugateEdgeSegment s2Turn = s2Alternative[0];

    // BUSH-LEVEL
    // on diverge, so needs to apply for both S1 and S2 across bushes before doing network update on node
    var s2Result =
        executeAcrossBushesTurnFlowShift(s2Turn, referenceWeight, bushWeights, -pasFlowShift,true, logAll);
    double pasAppliedS2FlowShift = s2Result.first();
    Map<ConjugateDestinationBush, Double> s2PerBushStartingFlowShifts = s2Result.second();

    if(pasAppliedS2FlowShift == 0){
      return null;
    }

    // S1
    double s1ToApplyFlowShift = -pasAppliedS2FlowShift;
    var s1Result = executeAcrossBushesTurnFlowShift(
        s1Turn, pasAppliedS2FlowShift, s2PerBushStartingFlowShifts, s1ToApplyFlowShift, false, logAll);
    double pasAppliedS1FlowShift = s1Result.first();
    Pair<ConjugateEdgeSegment,Double> s1ShiftPair = Pair.of(s1Turn, pasAppliedS1FlowShift);
    Pair<ConjugateEdgeSegment,Double> s2ShiftPair = Pair.of(s2Turn, pasAppliedS2FlowShift);

    // NETWORK LEVEL - UPDATE
    var remainingFlowShiftsPerTurn = executeNetworkLevelTurnUpdate(
        theMode,
        originalNlFlowAcceptanceFactors,
        assignmentStrategy,
        originalNetworkCosts,
        conjSegmentCosts,
        bushes,
        updateNetworkNodeModel,
        s1ShiftPair, s2ShiftPair);
    return Pair.of(remainingFlowShiftsPerTurn, s2PerBushStartingFlowShifts);
  }

  // replaces executeBushPasFlowShift
  private static Pair<Double, Boolean> executeOnPasPerAlternativeFlowShift(
      Map<ConjugateDestinationBush, Double> bushWeights,
      Pair<Double, Boolean> pasFlowShiftInfo,
      ConjugateEdgeSegment[] pasAlternative,
      Mode theMode,
      double[] originalNlFlowAcceptanceFactors,
      StaticLtmConjugateBushStrategy assignmentStrategy,
      double[] originalNetworkCosts,
      double[] conjSegmentCosts,
      Set<ConjugateDestinationBush> bushes,
      boolean updateNetworkNodeModel, boolean logAll) {

    if(pasFlowShiftInfo == null || pasFlowShiftInfo.first() == 0.0){
      // nothing to shift
      return pasFlowShiftInfo;
    }
    double pasFlowShift = pasFlowShiftInfo.first();
    boolean restrictToOutflowUpdateOnly = pasFlowShiftInfo.second();

    var networkLoading = assignmentStrategy.getLoading();
    double referenceWeight = bushWeights.values().stream().mapToDouble(e->e).sum();

    int index = 1; // start beyond diverge
    double remainingPasFlowShift = pasFlowShift;
    ConjugateEdgeSegment currentConjSegment;
    while (index < pasAlternative.length-1) { // stop before merge
      currentConjSegment = pasAlternative[index++];

      // SPECIAL-CASE potentially triggered by see below
      if (restrictToOutflowUpdateOnly) {
        // no more flow shifts, but if any outflow is not yet populated for upcoming cost calculation update - set it
        if (currentConjSegment.hasOriginalEntryEdgeSegment()) {
          var originalTurnEntrySegmentIndex = (int) currentConjSegment.getOriginalAdjacentEdgeSegments().first().getId();
          if (networkLoading.getCurrentOutflowsPcuH()[originalTurnEntrySegmentIndex] <= 0) {
            networkLoading.getCurrentOutflowsPcuH()[originalTurnEntrySegmentIndex] =
                networkLoading.getCurrentInflowsPcuH()[originalTurnEntrySegmentIndex] *
                    networkLoading.getCurrentFlowAcceptanceFactors()[originalTurnEntrySegmentIndex];
          }
        }
        continue;
      }

      // BUSH-LEVEL
      // perform shifts for current segment across all bushes and determine
      // to what extent we moved flow in total (in case we cannot meet total shift restricted by s2 availability)
      var acrossBushResult = executeAcrossBushesTurnFlowShift(
          currentConjSegment, referenceWeight, bushWeights, remainingPasFlowShift, false, logAll);
      double totalBushAppliedFlowShift = acrossBushResult.first();

      if(totalBushAppliedFlowShift != 0){
        // NETWORK LEVEL - UPDATE
        var networkResult = executeNetworkLevelTurnUpdate(
            theMode,
            originalNlFlowAcceptanceFactors,
            assignmentStrategy,
            originalNetworkCosts,
            conjSegmentCosts,
            bushes,
            updateNetworkNodeModel,
            Pair.of(currentConjSegment, totalBushAppliedFlowShift));
        if(networkResult == null || networkResult.length != 1 || networkResult[0] == null){
          throw new PlanItRunTimeException("Result of a network update should always be present, should not happen");
        }
        var singleResult = networkResult[0];
        remainingPasFlowShift = singleResult.first();
        restrictToOutflowUpdateOnly = singleResult.second();
      }else{
        remainingPasFlowShift = 0.0;
        break;
      }
    }
    return Pair.of(remainingPasFlowShift, restrictToOutflowUpdateOnly);
  }

  /**
   *  Perform the flow shift on the merge of a Pas
   * @param bushWeights to use to distribute flow shift across bushes
   * @param s1flowShiftInformation to apply
   * @param s2flowShiftInformation to apply
   * @param theMode to use
   * @param originalNlFlowAcceptanceFactors to use
   * @param assignmentStrategy to use
   * @param originalNetworkCosts to update
   * @param conjSegmentCosts to update
   * @param bushes to use
   * @param updateNetworkNodeModel flag indicating if node model is to be updated
   * @param logAll flag for logging
   */
  private void executeMergeFlowShift(
      Map<ConjugateDestinationBush, Double> bushWeights,
      Pair<Double, Boolean> s1flowShiftInformation,
      Pair<Double, Boolean> s2flowShiftInformation,
      Mode theMode,
      double[] originalNlFlowAcceptanceFactors,
      StaticLtmConjugateBushStrategy assignmentStrategy,
      double[] originalNetworkCosts,
      double[] conjSegmentCosts,
      Set<ConjugateDestinationBush> bushes,
      boolean updateNetworkNodeModel,
      boolean logAll) {
    if(s1flowShiftInformation == null && s2flowShiftInformation == null){
      return;
    }
    double s2FlowShift = s2flowShiftInformation!=null ? s2flowShiftInformation.first() : 0;
    boolean onlyUpdateS2Outflows = s2flowShiftInformation!=null ?  s2flowShiftInformation.second() : true;
    double s1FlowShift = s1flowShiftInformation!=null ? s1flowShiftInformation.first() : 0;
    boolean onlyUpdateS1Outflows = s1flowShiftInformation!=null ?  s1flowShiftInformation.second() : true;

    var networkLoading = assignmentStrategy.getLoading();

    ConjugateEdgeSegment[] s1Alternative = pas.getAlternative(true);
    ConjugateEdgeSegment s1Turn = s1Alternative[s1Alternative.length-1];
    ConjugateEdgeSegment[] s2Alternative = pas.getAlternative(false);
    ConjugateEdgeSegment s2Turn = s2Alternative[s2Alternative.length-1];

    // SPECIAL-CASE potentially triggered during on pas update. If so, do not propagate any flow changes
    if (onlyUpdateS2Outflows) {
      // no more flow shifts, but if any outflow is not yet populated for upcoming cost calculation update - set it
      if (s2Turn.hasOriginalEntryEdgeSegment()) {
        var originalTurnEntrySegmentIndex = (int) s2Turn.getOriginalAdjacentEdgeSegments().first().getId();
        if (networkLoading.getCurrentOutflowsPcuH()[originalTurnEntrySegmentIndex] <= 0) {
          networkLoading.getCurrentOutflowsPcuH()[originalTurnEntrySegmentIndex] =
              networkLoading.getCurrentInflowsPcuH()[originalTurnEntrySegmentIndex] *
                  networkLoading.getCurrentFlowAcceptanceFactors()[originalTurnEntrySegmentIndex];
        }
      }
    }
    if(onlyUpdateS1Outflows){
      if (s1Turn.hasOriginalEntryEdgeSegment()) {
        var originalTurnEntrySegmentIndex = (int) s1Turn.getOriginalAdjacentEdgeSegments().first().getId();
        if (networkLoading.getCurrentOutflowsPcuH()[originalTurnEntrySegmentIndex] <= 0) {
          networkLoading.getCurrentOutflowsPcuH()[originalTurnEntrySegmentIndex] =
              networkLoading.getCurrentInflowsPcuH()[originalTurnEntrySegmentIndex] *
                  networkLoading.getCurrentFlowAcceptanceFactors()[originalTurnEntrySegmentIndex];
        }
      }
    }
    if(onlyUpdateS1Outflows && onlyUpdateS2Outflows){
      //done
      return;
    }

    // BUSH-LEVEL
    double referenceWeight = bushWeights.values().stream().mapToDouble(e->e).sum();
    // on merge, so needs to apply for both S1 and S2 across bushes before doing network update on node
    double pasAppliedS2FlowShift = 0.0;
    if(s2FlowShift != 0.0 && !onlyUpdateS2Outflows) {
      var s2Result = executeAcrossBushesTurnFlowShift(
          s2Turn, referenceWeight, bushWeights, s2FlowShift, false, logAll);
      pasAppliedS2FlowShift = s2Result.first();
      if(pasAppliedS2FlowShift == 0.0 && s2FlowShift>1e-12){
        LOGGER.warning(String.format("No S2 flow could be shifted for congested PAS on merge: %s", this.pas));
      }
    }
    // on merge, so needs to apply for both S1 and S2 across bushes before doing network update on node
    double pasAppliedS1FlowShift = 0.0;
    if(s1FlowShift != 0.0 && !onlyUpdateS1Outflows) {
      var s1Result = executeAcrossBushesTurnFlowShift(
          s1Turn, referenceWeight, bushWeights, s1FlowShift, false, logAll);
      pasAppliedS1FlowShift = s1Result.first();

      if(pasAppliedS1FlowShift == 0.0 && s1FlowShift>1e-12){
        LOGGER.warning(String.format("No S1 flow could be shifted for congested PAS on merge: %s", this.pas));
      }
    }

    Pair<ConjugateEdgeSegment,Double> s1ShiftPair = Pair.of(s1Turn, pasAppliedS1FlowShift);
    Pair<ConjugateEdgeSegment,Double> s2ShiftPair = Pair.of(s2Turn, pasAppliedS2FlowShift);

    // NETWORK LEVEL - UPDATE
    var remainingFlowShiftsPerTurn = executeNetworkLevelTurnUpdate(
        theMode,
        originalNlFlowAcceptanceFactors,
        assignmentStrategy,
        originalNetworkCosts,
        conjSegmentCosts,
        bushes,
        updateNetworkNodeModel,
        s1ShiftPair, s2ShiftPair);
  }

  /**
   * 1) update splitting rates using bushes and then 2) apply a network level turn flow shift
   *
   *
   * @param nodeTurnFlowShiftsToApply      to use
   * @param theMode               to use
   * @param assignmentStrategy        to use
   * @param originalNetworkCosts  to use
   * @param conjNetworkCosts      to use
   * @param bushes to use
   * @param doNodeModelUpdate flag to indicate if we run a node model update to consider potential congestion
   * @return remaining flow shift for next segment and flag indicating if we should only update outflows downstream per
   *  altered turn
   */
  private static Pair<Double,Boolean>[] executeNetworkLevelTurnUpdate(
      Mode theMode,
      double[] originalNlFlowAcceptanceFactors,
      StaticLtmConjugateBushStrategy assignmentStrategy,
      double[] originalNetworkCosts,
      double[] conjNetworkCosts,
      Set<ConjugateDestinationBush> bushes,
      boolean doNodeModelUpdate,
      Pair<ConjugateEdgeSegment,Double>... nodeTurnFlowShiftsToApply) {

    var resultToPopulate =  (Pair<Double,Boolean>[])new Pair[nodeTurnFlowShiftsToApply.length];

    var networkLoading = assignmentStrategy.getLoading();
    var nonConjugateFlowAcceptanceFactors = networkLoading.getCurrentFlowAcceptanceFactors();

    // prep by collecting before state per turn
    // per turn: <nlFlowAcceptanceFactorBefore, onTheFlyFlowAcceptanceFactorBefore, onTheFlyTurnInflowBefore>
    Triple<Double,Double,Double>[] beforeNodeModelUpdateInfo = new Triple[nodeTurnFlowShiftsToApply.length];
    // in case multiple of turns share same in-link we must capture the before before before we update any splitting rates
    double[] beforeNonConjugateSplittingRates = new double[nodeTurnFlowShiftsToApply.length];
    for(int index=0; index <nodeTurnFlowShiftsToApply.length; ++index) {
      var entry = nodeTurnFlowShiftsToApply[index];
      var turn = entry.first();
      var flowShift = entry.second();
      var originalTurnEntrySegment = turn.getOriginalAdjacentEdgeSegments().first();
      boolean hasOriginalEntry = (originalTurnEntrySegment != null);
      if (flowShift > 0 && hasOriginalEntry) {
        // any additional flow to a turn may potentially cause congestion. to ensure node model calculates such a node
        // it must be registered as potentially blocking. So we register it at such (if already done nothing happens).
        networkLoading.getSplittingRateData().registerPotentiallyBlockingNode(originalTurnEntrySegment.getDownstreamVertex());
      }

      // must be splitting rate before we update the splitting rates otherwise we cannot use it to determine
      // the before situation
      double nonConjugateNetworkSplittingRate = (hasOriginalEntry && turn.hasOriginalExitEdgeSegment()) ?
          networkLoading.getSplittingRateData().getSplittingRate(
              turn.getOriginalAdjacentEdgeSegments().first(), turn.getOriginalAdjacentEdgeSegments().second())
          : 1;
      beforeNonConjugateSplittingRates[index] = nonConjugateNetworkSplittingRate;
    }

    boolean doSplittingRateUpdate = false;
    for(int index=0; index <nodeTurnFlowShiftsToApply.length; ++index) {
      var entry = nodeTurnFlowShiftsToApply[index];
      var turn = entry.first();
      var flowShift = entry.second();
      var originalTurnEntrySegment = turn.getOriginalAdjacentEdgeSegments().first();
      boolean hasOriginalEntry = (originalTurnEntrySegment != null);
      doSplittingRateUpdate = flowShift != 0 && hasOriginalEntry;

      double nlFlowAcceptanceFactorBefore = 1;
      double onTheFlyFlowAcceptanceFactorBefore = 1;
      double onTheFlyTurnInflowBefore = Double.MAX_VALUE;
      if (hasOriginalEntry) {
        double nonConjugateBeforeNetworkSplittingRate = beforeNonConjugateSplittingRates[index];
        onTheFlyTurnInflowBefore =
            networkLoading.getCurrentInflowsPcuH()[(int) originalTurnEntrySegment.getId()] * nonConjugateBeforeNetworkSplittingRate;
        nlFlowAcceptanceFactorBefore = originalNlFlowAcceptanceFactors[(int) originalTurnEntrySegment.getId()];
        onTheFlyFlowAcceptanceFactorBefore = nonConjugateFlowAcceptanceFactors[(int) originalTurnEntrySegment.getId()];
      }
      beforeNodeModelUpdateInfo[index] = Triple.of(
          nlFlowAcceptanceFactorBefore, onTheFlyFlowAcceptanceFactorBefore, onTheFlyTurnInflowBefore);

      if(doSplittingRateUpdate) {
        // todo: could be done per original in link, done per turn shift to keep it simple
        // perform splitting rate update, required for correct network update below + we use splitting rate for
        // determining flow shift restrictions downstream (if any)
        executeNetworkSplittingRateUpdateForPasAlternativeSegment(turn, bushes, networkLoading);
      }
    }

    // We can now update the network/node in one go across the adjusted turns knowing how much we are shifting for each
    // and splitting rates being synced

    // do the one-off network level update
    // sync network inflows/unconstrained flows/sending flows, splitting rates, alphas, and costs via network node
    // model update <-- differs from uncongested
    executeNetworkLevelTurnFlowShiftsOnNode(
        theMode,
        assignmentStrategy,
        originalNetworkCosts,
        conjNetworkCosts,
        doNodeModelUpdate,
        nodeTurnFlowShiftsToApply);

    // per turn check what remains to shift afterwards
    for(int index=0; index <nodeTurnFlowShiftsToApply.length; ++index) {
      var entry = nodeTurnFlowShiftsToApply[index];
      var turn = entry.first();
      var turnBeforeInfo = beforeNodeModelUpdateInfo[index];
      boolean hasOriginalEntry = turn.hasOriginalEntryEdgeSegment();
      var originalTurnEntrySegment = turn.getOriginalAdjacentEdgeSegments().first();
      var appliedTurnFlowShift = nodeTurnFlowShiftsToApply[index].second();

      double onTheFlyAcceptanceFactorAfter = 1;
      if(hasOriginalEntry){
        onTheFlyAcceptanceFactorAfter =
            networkLoading.getCurrentFlowAcceptanceFactors()[(int) originalTurnEntrySegment.getId()];
      }

      double nlFlowAcceptanceFactorBefore = turnBeforeInfo.first();
      double onTheFlyFlowAcceptanceFactorBefore = turnBeforeInfo.second();
      double onTheFlyTurnInflowBefore = turnBeforeInfo.third();

      // NOTE: the calculation for remaining flow shift is wrong for the MERGE but because we stop anyway after a merge
      //        it doesn't matter; for now no precaution is taken to identify this situation since it has no impact.

      // adjust flow shift
      // case 1: factors remain 1 -> proceed with same flow shift and propagate further
      // case 2: factors not both 1, but no change in outflow      - stop flow shift propagation since traffic
      //    withholding makes that no downstream flow shift exists (it is all removed from the withheld traffic)
      // case 3: factors not both 1 and change in outflow         - determine non-withheld change in flow, namely
      //    the new outflow - old outflow
      double newTurnInflowPcuH = onTheFlyTurnInflowBefore + appliedTurnFlowShift;
      double proposedRemainingShift = appliedTurnFlowShift;
      boolean restrictToDownstreamOutflowUpdateOnly = false;
      if (onTheFlyFlowAcceptanceFactorBefore < 1 || onTheFlyAcceptanceFactorAfter < 1) {
        double onTheFlyTurnOutflowBefore = onTheFlyTurnInflowBefore * onTheFlyFlowAcceptanceFactorBefore;
        double onTheFlyTurnOutflowAfter = newTurnInflowPcuH * onTheFlyAcceptanceFactorAfter;
        if (Precision.equal(onTheFlyTurnOutflowBefore, onTheFlyTurnOutflowAfter, EPSILON_9)) {
          // case 2: nothing left, all consumed by the change in withheld flow
          proposedRemainingShift = 0;
          // we still need to make sure all outflows are present for cost calculation. switch to outflow syncing only
          restrictToDownstreamOutflowUpdateOnly = true;
        } else {
          // case 3: we propose to propagate the remaining difference that is not consumed by removing the previously
          // withheld flow
          // NOTE: if an alpha becomes less restrictive, it is possible that the flow shift INCREASES compared to the
          // original flow shift (because flow previously withheld is now propagated in addition to the flow shift itself)
          // ...
          proposedRemainingShift = onTheFlyTurnOutflowAfter - onTheFlyTurnOutflowBefore;
          // ... so we can't limit to the original applied turn flow shift, simply use the difference
        }
      }

      if(appliedTurnFlowShift * proposedRemainingShift < 0){

        // should never happen unless this is a merge where it is possible that by removing flow on one entry
        // and adding on another we get an increase in the output on the link where we remove flow.
        // this is only possible if the merge is in fact a cross-node where we are shifting flow on a non-most
        // restricting exit for the entry where we are removing flow. We currently test for all this except the
        // non-most restricting part to issue a warning or not
        EdgeSegment originalExitLink = nodeTurnFlowShiftsToApply[0].first().hasOriginalExitEdgeSegment() ?
            nodeTurnFlowShiftsToApply[0].first().getOriginalAdjacentEdgeSegments().second() : null;
        boolean isMerge = originalExitLink != null && nodeTurnFlowShiftsToApply.length>1 &&
         Arrays.stream(nodeTurnFlowShiftsToApply).allMatch( e ->
            e.first().hasOriginalExitEdgeSegment() && originalExitLink.getId() == e.first().getOriginalAdjacentEdgeSegments().second().getId());

        int uTurnAdjustment = turn.getOriginalAdjacentEdgeSegments().first().hasOppositeDirectionSegment() ? 1 : 0;
        if(!isMerge && Math.abs(proposedRemainingShift)>1) {
          LOGGER.severe(String.format("flow shift changed sign from %.8f to %.8f after segment (%s), that should never happen",
              appliedTurnFlowShift, proposedRemainingShift, entry.first().getIdsAsString()));
        }
      }

      //NOTE: we no longer restrict based on network loading because that is already handled by the above (should be)
      // the adjustment in how much we can shift should ideally only be applied when determining the s2 sending flow

//      // lastly, we want to ensure we remain consistent with the most restricting situation compared to the original
//      // network loading, so we take the minimum of our proposed remaining flow shift and the nl alphas in case that is
//      // more restricting
//      double remainingPasFlowShift = appliedTurnFlowShift > 0 ?
//          Math.min( nlFlowAcceptanceFactorBefore * appliedTurnFlowShift,  proposedRemainingShift):
//          Math.max( nlFlowAcceptanceFactorBefore * appliedTurnFlowShift,  proposedRemainingShift);
      double remainingPasFlowShift = proposedRemainingShift;
      resultToPopulate[index] = Pair.of(remainingPasFlowShift, restrictToDownstreamOutflowUpdateOnly);
    }
    return resultToPopulate;
  }

  /**
   * run node model and update unconstrained flows, inflows, sending flows, acceptance factors and costs on network
   * level rather than individual bush level, either with or without a node model update.
   *<p>
   *   this assumes network level splitting rates of the node model on the network level are correct, they are not
   *   updated as part of this exercise. If required first do that.
   *</p>
   *
   * @param theMode               to use
   * @param assignmentStrategy        to use
   * @param originalNetworkCosts  to use
   * @param conjNetworkCosts      to use
   * @param doNodeModelUpdate flag to indicate if we run a node model update to consider potential congestion
   * @param segmentFlowShiftsToApply turn flow shifts to apply (required to all be turns on the same node)
   */
  private static void executeNetworkLevelTurnFlowShiftsOnNode(
      Mode theMode,
      StaticLtmConjugateBushStrategy assignmentStrategy,
      double[] originalNetworkCosts,
      double[] conjNetworkCosts,
      boolean doNodeModelUpdate,
      Pair<ConjugateEdgeSegment,Double>... segmentFlowShiftsToApply) {
    if(segmentFlowShiftsToApply == null) {
      LOGGER.severe("No segments to perform network turn flow shift for provided");
    }
    var networkLoading = assignmentStrategy.getLoading();

    var unconstrainedFlows = networkLoading.getUnconstrainedFlowsPcuHour();
    var constrainedFlows = networkLoading.getCurrentInflowsPcuH();
    // node model update uses sending flows rather than inflows todo: see if we can generalise this because it is ugly
    // so we need to sync those as well
    var sendingFlows = networkLoading.getCurrentSendingFlowsPcuH();


    boolean anyNonZeroShiftApplied = false;
    DirectedVertex theNode = null;
    for (Pair<ConjugateEdgeSegment, Double> entry : segmentFlowShiftsToApply) {
      var pasAlternativeSegment = entry.first();
      var flowShiftToApply = entry.second();
      if(flowShiftToApply == 0 ){
        continue;
      }
      anyNonZeroShiftApplied = true;

      var originalSegment = pasAlternativeSegment.getOriginalAdjacentEdgeSegments().first();
      if (originalSegment != null) {
        int segmentIndex = (int) originalSegment.getId();
        theNode = originalSegment.getDownstreamVertex();

        // network level constrained inflow/sending flow update
        double currentConstrainedFlow = constrainedFlows[segmentIndex];
        double newConstrainedFlow = Math.max(currentConstrainedFlow + flowShiftToApply, 0);
        constrainedFlows[segmentIndex] = newConstrainedFlow;
        sendingFlows[segmentIndex] = newConstrainedFlow;

        // network level unconstrained flow update
        double currentUnconstrainedFlow = unconstrainedFlows[segmentIndex];
        double newUnconstrainedFlow = Math.max(currentUnconstrainedFlow + flowShiftToApply, 0);
        unconstrainedFlows[segmentIndex] = newUnconstrainedFlow;
      }
    }
    if(!anyNonZeroShiftApplied){
      // no remaining shifts
      return;
    }

    if(doNodeModelUpdate && theNode != null) {
      var consumer = new NMRUpdateIncomingConjugateOutFlowsFactorsAndCostsConsumer(
          theNode, theMode, assignmentStrategy, originalNetworkCosts, conjNetworkCosts);
      StaticLtmNetworkLoading.performNodeModelTurnBasedUpdate(theNode, consumer, networkLoading, constrainedFlows);
    }else {
      // update outflow based on new sending flow capped by in link capacity when we ignore node model impact
      for (Pair<ConjugateEdgeSegment, Double> entry : segmentFlowShiftsToApply) {
        var turnSegment = entry.first();
        var originalSegment = turnSegment.getOriginalAdjacentEdgeSegments().first();
        if (originalSegment != null){
          int segmentIndex = (int) originalSegment.getId();

          double updatedNetworkOutflow =
              networkLoading.getCurrentSendingFlowsPcuH()[segmentIndex] * networkLoading.getCurrentFlowAcceptanceFactors()[segmentIndex];
          networkLoading.getCurrentOutflowsPcuH()[segmentIndex] =
              Math.min(((PcuCapacitated)originalSegment).getCapacityOrDefaultPcuH(), updatedNetworkOutflow);

          // update cost
          ConjugateCostUtils.updateLinkAndConjugateSegmentCost(
              originalSegment, assignmentStrategy, theMode, originalNetworkCosts, conjNetworkCosts);
        }
      }
    }

    // no point, it is readily available, so expect caller to collect it
    //return networkLoading.getCurrentFlowAcceptanceFactors()[segmentIndex];
  }

  // given an original entry segment and turn flows, update to splitting rates on network level
  private static void updateOriginalEntrySegmentSplittingRate(
      EdgeSegment originalEntrySegment, Map<EdgeSegment, Double> exitSegmentFlowsToConvertToSplittingRates,
      StaticLtmLoadingBushConjugate networkLoading) {

    // now convert to splitting rates
    var entrySegmentSplittingRates = networkLoading.getSplittingRateData().getSplittingRates(originalEntrySegment);
    if(entrySegmentSplittingRates==null){
      //todo: bug: as below does not update them on the splitting rate data --> fix by registering created array on data
      throw new PlanItRunTimeException("easy bug fix required as described in code, should not trigger");
//      entrySegmentSplittingRates =
//          Array1D.PRIMITIVE64.makeZero(originalEntrySegment.getDownstreamVertex().getNumberOfExitEdgeSegments());
    }else{
      entrySegmentSplittingRates.reset();
    }
    int index = 0;
    for (var exitSegment : originalEntrySegment.getDownstreamVertex().getExitEdgeSegments()) {
      /* assume no u-turn flow allowed */
      if (originalEntrySegment.getParent().idEquals(exitSegment.getParent())) {
        index++;
        continue;
      }

      double totalTurnFlow = exitSegmentFlowsToConvertToSplittingRates.getOrDefault(exitSegment,0.0);
      entrySegmentSplittingRates.set(index++, totalTurnFlow);
    }

    /* sum all flows and then divide by this sum to obtain splitting rates */
    double totalEntryFlow = entrySegmentSplittingRates.aggregateAll(Aggregator.SUM);
    if (totalEntryFlow > 0) {
      entrySegmentSplittingRates.modifyAll(PrimitiveFunction.DIVIDE.by(totalEntryFlow));
    } else {
      entrySegmentSplittingRates.fillAll(1.0);
    }
  }

  // for conjugate PAS segment, obtain its current turn flows for original entry segment of the conj segment
  // and update the network splitting rates based off of this.
  private static void executeNetworkSplittingRateUpdateForPasAlternativeSegment(
      ConjugateEdgeSegment conjSegment,
      Collection<ConjugateDestinationBush> conjBushes,
      StaticLtmLoadingBushConjugate networkLoading) {

    Map<EdgeSegment, Double> networkExitSegmentSplittingRates = new TreeMap<>();
    var originalEntrySegment = conjSegment.getOriginalAdjacentEdgeSegments().first();

    // consider all turn flows originating from original entry segments and collate them in map by original exit
    // segment
    var conjSegmentsSharedOrigEntry = conjSegment.getUpstreamVertex().getExitEdgeSegments();
    for (var conjBush : conjBushes) {
      for (var conjSegmentWithSharedEntry : conjSegmentsSharedOrigEntry) {
        if (!conjBush.contains(conjSegmentWithSharedEntry)) {
          continue;
        }

        if (conjSegmentWithSharedEntry.hasOriginalEntryEdgeSegment() &&
            !conjSegmentWithSharedEntry.getOriginalAdjacentEdgeSegments().first().idEquals(originalEntrySegment)) {
          // fail safe in case conjugate network would not introduce nodes per link segment but per link. Should
          // never trigger currently
          LOGGER.severe("conjugate network nodes should never have exit links with different original entry links," +
              "unless something changed in conjugate network construction!!");
          continue;
        }

        // original network entry --> add (exit link, turn flow) to map
        var originalExitLink = conjSegmentWithSharedEntry.getOriginalAdjacentEdgeSegments().second();
        if(originalExitLink == null){
          continue;
        }
        double turnSendingFlow = conjBush.getTurnSendingFlow(conjSegmentWithSharedEntry);
        if(turnSendingFlow > 0) {
          networkExitSegmentSplittingRates.put(
              originalExitLink,
              networkExitSegmentSplittingRates.getOrDefault(originalExitLink, 0.0) +
                  conjBush.getTurnSendingFlow(conjSegmentWithSharedEntry)); // updated value
        }
      }
    }

    updateOriginalEntrySegmentSplittingRate(originalEntrySegment, networkExitSegmentSplittingRates, networkLoading);
  }

  protected static double computeRegularDTravelTimeDFlowSingleLink(
      final Mode theMode,
      final AbstractPhysicalCost physicalCost,
      final AbstractVirtualCost virtualCost,
      EdgeSegment originalEntrySegment,
      boolean unCongested) {

      if (originalEntrySegment instanceof MacroscopicLinkSegment) {
        return physicalCost.getDTravelTimeDFlow(
            unCongested, theMode, (MacroscopicLinkSegment) originalEntrySegment);
      } else if (originalEntrySegment instanceof ConnectoidSegment) {
        return virtualCost.getDTravelTimeDFlow(
            unCongested, theMode, (ConnectoidSegment) originalEntrySegment);
      } else {
        LOGGER.severe(String.format("Unsupported edge segment (%s) to obtain derivative of cost towards flow from",
            originalEntrySegment.getIdsAsString()));
        return 0.0;
      }
    }

  /**
   * Constructor
   *
   * @param pas      to use
   * @param settings to use
   */
  protected PasFlowShiftConjugateDestinationBasedExecutor(
          final Pas<ConjugateDirectedVertex, ConjugateEdgeSegment> pas, final StaticLtmSettings settings) {
    super(pas, settings);
    this.s1S2SendingFlows = Pair.of(0.0,0.0);
  }


  protected void injectOriginalBushTurnFlowAccess(
      Map<ConjugateDestinationBush, ConjugateBushTurnData> originalBushTurnFlowTracker){
    this.originalBushTurnFlowTracker = originalBushTurnFlowTracker;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  protected ConjugateEdgeSegment findFirstCongestedEdgeSegmentOnPasAlternative(
          StaticLtmLoadingBushBase<?> networkLoading, boolean lowCost) {

    ConjugateEdgeSegment[] alternative = pas.getAlternative(lowCost);
    ConjugateEdgeSegment currConjSegment = null;
    int index = 0;
    for (; index < alternative.length; ++index) {
      currConjSegment = alternative[index];

      // use the original network loading segments to determine if the conjugate segment (turn) is considered
      // congested or not
      if(!currConjSegment.hasOriginalEntryEdgeSegment()){
        continue;
      }
      if (currConjSegment.hasOriginalEntryEdgeSegment() &&
              isCongested(networkLoading , currConjSegment.getOriginalAdjacentEdgeSegments().first())) {
        return currConjSegment;
      }
    }
    return null;
  }

  /** Compute special derivative for changing flow on uncongested turn
  *         given another turn is congested and most restrictive
   *         This replaces the "normal" hypercritical derivative
  */
  protected static double computeHyperCriticalDTravelTimeDFlowNotMostRestrictiveTurnOnCongestedLink(
      final StaticLtmLoadingBushBase<?> networkLoading,
      final AbstractPhysicalCost physicalCost,
      EdgeSegment originalEntrySegment,
      EdgeSegment mostRestrictingExit,
      double mostRestrExitDemandConstrainedFlow) {

    //NOTE: assumes if it is a connectoid it works exactly as regular steady state --> any other than
    // steadystateconnectoidtraveltime virtual cost is not compatible currently....

    var alpha_i = networkLoading.getCurrentFlowAcceptanceFactors()[(int) originalEntrySegment.getId()];
    var splittingRateToMostRestricting = networkLoading.getSplittingRateData().getSplittingRate(originalEntrySegment, mostRestrictingExit);
    var u_i = networkLoading.getCurrentInflowsPcuH()[(int) originalEntrySegment.getId()];
    var u_ijMostRestricting = u_i * splittingRateToMostRestricting;
    if (u_ijMostRestricting <= 0) {
      LOGGER.severe("should always have most restricting turn flow but NOT???");
      return 0;
    }
    var c_i = Math.min(TampereNodeModelFixedInput.DEFAULT_MAX_IN_CAPACITY,
        ((PcuCapacitated) originalEntrySegment).getCapacityOrDefaultPcuH());
    var c_j_mostRestricting = Math.min(TampereNodeModelFixedInput.DEFAULT_MAX_IN_CAPACITY,
        ((PcuCapacitated) mostRestrictingExit).getCapacityOrDefaultPcuH());
    var r_jMostRestr =
        c_j_mostRestricting - mostRestrExitDemandConstrainedFlow;
    var timePeriodH = ((SteadyStateTravelTimeCost) physicalCost).getCurrentTimePeriodH();
    // b_j is the scaled sending flows of all other turns combined into the most restricting out link except for
    // the turn coming from our in link to the most restricting out link
    // b_j = (C_i*u_i_jMostRestr*(1-alpha_i))/(alpha_i*u_i) <-- see doc for how this was derived
    var approximateBMostRestricting =
        (c_i * (r_jMostRestr - (alpha_i * u_ijMostRestricting))) / (alpha_i * u_i);
    // d_hyper/du_ij for any j not going to the most restricting out link, given the link is congested due to
    // another turn into the known most restricting out link:
    // d_hyper/du_ij = (1/2*T*b_j)/(C_i*r_j)
    double dTravelTimeDFlow =
        (0.5 * timePeriodH * approximateBMostRestricting) /
            (c_i * r_jMostRestr);
    return dTravelTimeDFlow;
  }

  /** Compute special derivative for changing flow on merge where
   *  uncongested turn and congested turn towards same congested exit compete
   */
  protected static double computeHyperCriticalDTravelTimeDFlowCombinedMergeSpecialCase(
      final StaticLtmLoadingBushBase<?> networkLoading,
      final AbstractPhysicalCost physicalCost,
      EdgeSegment originalCongestedEntrySegment,
      EdgeSegment originalUncongestedEntrySegment,
      EdgeSegment mostRestrictingExit) {

    var timePeriodH = ((SteadyStateTravelTimeCost) physicalCost).getCurrentTimePeriodH();

    var c_j_mostRestricting = Math.min(TampereNodeModelFixedInput.DEFAULT_MAX_IN_CAPACITY,
        ((PcuCapacitated) mostRestrictingExit).getCapacityOrDefaultPcuH());

    var node = originalCongestedEntrySegment.getDownstreamVertex();
    int exitSegmentIndex=0;
    for(var exitSegment : node.getExitEdgeSegments()) {
      if (exitSegment == mostRestrictingExit) {
        break;
      }
      ++exitSegmentIndex;
    }

    // all accepted outflows from other turns not s1/s2 to  pas exit
    double mostRestrictingExitNonMergeAcceptedFlows = 0 ;
    // uncongested (s1/ors2)/congested (s2 or s1) sending flows to pas exit
    double congestedEntryTurnSendingFlow = 0;
    double unCongestedEntryTurnAcceptedFlow = 0;
    for(var entrySegment : node.getEntryEdgeSegments()){
      Array1D<Double> splittingRates = networkLoading.getSplittingRateData().getSplittingRates(entrySegment);
      var splittingRateToMostRestricting = splittingRates.get(exitSegmentIndex);

      int entrySegmentIndex = (int)entrySegment.getId();
      var entrySendingFlow = networkLoading.getCurrentInflowsPcuH()[entrySegmentIndex];
      if(entrySendingFlow <= 0){
        continue;
      }

      var alphaEntrySegment = networkLoading.getCurrentFlowAcceptanceFactors()[entrySegmentIndex];
      if(entrySegment == originalCongestedEntrySegment ){
        congestedEntryTurnSendingFlow = entrySendingFlow * splittingRateToMostRestricting;
      }else if(entrySegment == originalUncongestedEntrySegment){
        // uncongested here means it is not restricted by the exit at hand, as in if there is an alpha<1 it is not because
        // of our current exit and therefore it can be regarded as uncongested (nconstrained by exit would be a better term)
        // still, we need to multiply by alpha s alpha could be smaller than one
        unCongestedEntryTurnAcceptedFlow = entrySendingFlow * splittingRateToMostRestricting * alphaEntrySegment;
      }else {
        double acceptedFlowOnTurnToMostRestricting = entrySendingFlow * splittingRateToMostRestricting * alphaEntrySegment;
        mostRestrictingExitNonMergeAcceptedFlows += acceptedFlowOnTurnToMostRestricting;
      }
    }

    // actual derivative using above prep vars
    double s1s2MergeEntriesSendingFlow = congestedEntryTurnSendingFlow +  unCongestedEntryTurnAcceptedFlow;
    double x = (c_j_mostRestricting - s1s2MergeEntriesSendingFlow - mostRestrictingExitNonMergeAcceptedFlows);
    double dTravelTimeDFlow =
        (0.5 * timePeriodH * x)
            /
            Math.pow(x + congestedEntryTurnSendingFlow, 2);
    return dTravelTimeDFlow;
  }


  /**
   * @param theMode              to use
   * @param networkLoading       to use
   * @param physicalCost         to use
   * @param virtualCost          to use
   * @param isLowCostAlternative to use
   * @param derivativeReductionFactor to use
   * @return derivative, compounded derivative reduction factor and indicator whether to continue or not
   */
  @Override
  protected Triple<Double, Double, Boolean> getDTravelTimeDFlowExcludingMergeDiverge(
      final Mode theMode,
      final StaticLtmLoadingBushBase<?> networkLoading,
      final AbstractPhysicalCost physicalCost,
      final AbstractVirtualCost virtualCost,
      boolean isLowCostAlternative,
      double derivativeReductionFactor,
      Set<EdgeSegment> allowChainingBeyondBottleneck) {
    double dTravelTimeDFlow = 0.0;

    var pasAlternative = this.pas.getAlternative(isLowCostAlternative);

    boolean continueWithMergeDerivative = true;
    int index = 1;
    double compoundedDerivativeReductionFactor = derivativeReductionFactor;
    while(index < (pasAlternative.length-1)){
      ConjugateEdgeSegment currSegment = pasAlternative[index++];
      EdgeSegment originalEntrySegment = currSegment.getOriginalAdjacentEdgeSegments().first();
      EdgeSegment originalExitSegment = currSegment.getOriginalAdjacentEdgeSegments().second();

      boolean unCongested = !isCongested(networkLoading, originalEntrySegment);
      EdgeSegment mostRestrictingExit = null;
      double mostRestrExitDemandConstrainedFlow = 0;
      if(!unCongested){
        var mostRestrictingExitDemandConstrFlowResult = identifyMostRestrictingOutSegmentAndDemandConstrainedFlow(
            originalEntrySegment, networkLoading); // todo should be done once and cached
        mostRestrictingExit = mostRestrictingExitDemandConstrFlowResult.first();
        mostRestrExitDemandConstrainedFlow = mostRestrictingExitDemandConstrFlowResult.second();
      }

      // if for whatever reason we reverted to uncongested, no most restricting exists and we reset flag to avoid issues
      // with derivatives
      if(mostRestrictingExit == null){
        unCongested = true;
      }

      double currDTravelTimeDFlow = 0.0;
      boolean thisAltOnMostRestrictingTurn = (mostRestrictingExit == originalExitSegment);

      // ON PAS (congested link)
      // - case 1: flow not on most-restrictive turn --> compute hypo as if uncongested, allow to continue since
      //           flow change is expected to continue + use special derivative
      // - case 2: flow on most-restrictive turn --> treat as congested and stop after this link as flow change is not
      //           expected to propagate further
      if(!unCongested && !thisAltOnMostRestrictingTurn){
        // case 1: use hypo critical uncongested derivative + special derivative for changing flow on
        //          uncongested turn
        //         given another turn is congested and most restrictive
        currDTravelTimeDFlow += computeHyperCriticalDTravelTimeDFlowNotMostRestrictiveTurnOnCongestedLink(
            networkLoading, physicalCost, originalEntrySegment, mostRestrictingExit, mostRestrExitDemandConstrainedFlow);
        unCongested = true; // triggers adding hypo critical delay via normal approach below
      } // case 2 no action needed, remains congested on most restricting turn, or truly uncongested

      currDTravelTimeDFlow += computeRegularDTravelTimeDFlowSingleLink(
          theMode, physicalCost, virtualCost, originalEntrySegment, unCongested);
      // adjust with compounded alpha reduction
      dTravelTimeDFlow += currDTravelTimeDFlow * compoundedDerivativeReductionFactor;
      if(!unCongested) {
        var alpha = networkLoading.getCurrentFlowAcceptanceFactors()[(int) originalEntrySegment.getId()];
        compoundedDerivativeReductionFactor *= alpha;
      }

      if(!unCongested && thisAltOnMostRestrictingTurn){
        boolean ignoreBottleneckChainTruncation =
            allowChainingBeyondBottleneck!=null && allowChainingBeyondBottleneck.contains(originalEntrySegment);
        if(!ignoreBottleneckChainTruncation) {
          // no more flow change beyond here due to it being a bottleneck, and we are not allowed to ignore
          continueWithMergeDerivative = false;
          break;
        }
      }
    }
    return Triple.of(dTravelTimeDFlow, compoundedDerivativeReductionFactor, continueWithMergeDerivative);
  }

  /**
   *  compute diverge derivative from perspective of s2.
   * @param theMode to use
   * @param networkLoading to use
   * @param physicalCost to use
   * @param virtualCost to use
   * @return [dTtdFlow (double), alpha (double), s1 continue (true/false), s2 continue (true/false)]
   */
  protected Object[] getDTravelTimeDFlowDiverge(
      final Mode theMode,
      final StaticLtmLoadingBushBase<?> networkLoading,
      final AbstractPhysicalCost physicalCost,
      final AbstractVirtualCost virtualCost) {
    double dTravelTimeDFlow = 0.0;

    ConjugateEdgeSegment currS1Segment = this.pas.getFirstEdgeSegment(true);
    ConjugateEdgeSegment currS2Segment = this.pas.getFirstEdgeSegment(false);
    if(!currS1Segment.hasOriginalEntryEdgeSegment()){
      return new Object[]{0.0, 1.0, true, true};
    }

    EdgeSegment originalEntrySegment = currS1Segment.getOriginalAdjacentEdgeSegments().first();
    EdgeSegment originalS1ExitSegment = currS1Segment.getOriginalAdjacentEdgeSegments().second();
    EdgeSegment originalS2ExitSegment = currS2Segment.getOriginalAdjacentEdgeSegments().second();

    boolean unCongested = !isCongested(networkLoading, originalEntrySegment);
    EdgeSegment mostRestrictingExit = null;
    double mostRestrictingExitDemandConstrFlow = 0;
    if(!unCongested){
      var mostRestrictingExitDemandConstrFlowResult = identifyMostRestrictingOutSegmentAndDemandConstrainedFlow(
          originalEntrySegment, networkLoading);
      mostRestrictingExit = mostRestrictingExitDemandConstrFlowResult.first();
      mostRestrictingExitDemandConstrFlow = mostRestrictingExitDemandConstrFlowResult.second();
    }

    if(mostRestrictingExit == null){
      unCongested = true;
    }

    // DIVERGE:
    //
    // (un)congested: no change in flow regardless, so no derivative exists really. We can only
    //                consider to what extent the rest of the PAS should be considered. So zero
    // congested  : when s2 is on most restricting turn use hyper critical derivative
    //                s1 is not on most restricting, use compensatory special derivative case in On-PAS for this
    //              when s1 is on most restricting turn use hypercritical derivative --> combined single hyper critical, s1 may continue
    //                s2 is not on most restricting, use compensatory special derivative case in On-PAS for this --> combined single hyper critical, s2 may continue
    //              when neither is on most restricting: twice the special case derivative but in opposite directions, so
    //               no impact on most restricting
    if(unCongested || (mostRestrictingExit!=originalS1ExitSegment && mostRestrictingExit!=originalS2ExitSegment)){
      //                der.,alpha, s1 continue y/n, s2 continue y/n
      return new Object[]{
          0.0, networkLoading.getCurrentFlowAcceptanceFactors()[(int) originalEntrySegment.getId()], true, true};
    }else{

      // only hyper critical derivative differs between s1 and s2
      double dTravelTimeDFlowHypoAndHyper = computeRegularDTravelTimeDFlowSingleLink(
          theMode, physicalCost, virtualCost, originalEntrySegment, false);
      double dTravelTimeDFlowHypo = computeRegularDTravelTimeDFlowSingleLink(
          theMode, physicalCost, virtualCost, originalEntrySegment, true);
      dTravelTimeDFlow = dTravelTimeDFlowHypoAndHyper - dTravelTimeDFlowHypo;

      //determine which of the two may continue
      boolean s1MostRestricting = mostRestrictingExit==originalS1ExitSegment;

      // determine special case hyper critical for non-most restricting turn and add it to the regular hyper
      // critical derivative because from a derivative denominator point of view they should reflect the combined change
      // in cost per unit flow
      double compensatoryHyperDTravelTimeDFlow =
          computeHyperCriticalDTravelTimeDFlowNotMostRestrictiveTurnOnCongestedLink(
      networkLoading, physicalCost, originalEntrySegment, mostRestrictingExit, mostRestrictingExitDemandConstrFlow);
      // todo: approximation --> derive this analytically because the below is unlikely to be entirely correct
      dTravelTimeDFlow += compensatoryHyperDTravelTimeDFlow;
      double acceptanceFactor = networkLoading.getCurrentFlowAcceptanceFactors()[(int) originalEntrySegment.getId()];
      return new Object[]{dTravelTimeDFlow, acceptanceFactor, !s1MostRestricting, s1MostRestricting};
    }

  }

  /**
   * Compute merge derivative from perspective of S2 incorporating impact of s1
   *
   * @param theMode to use
   * @param networkLoading to use
   * @param physicalCost to use
   * @param virtualCost to use
   * @param considerS1 to use
   * @param considerS2 to use
   @return resultant derivative of the two entries combined
   */
  protected double getDTravelTimeDFlowMerge(
      final Mode theMode,
      final StaticLtmLoadingBushBase<?> networkLoading,
      final AbstractPhysicalCost physicalCost,
      final AbstractVirtualCost virtualCost,
      boolean considerS1,
      boolean considerS2,
      double s1DerivativeReductionFactor,
      double s2DerivativeReductionFactor) {

    ConjugateEdgeSegment currS1Segment = this.pas.getLastEdgeSegment(true);
    ConjugateEdgeSegment currS2Segment = this.pas.getLastEdgeSegment(false);

    EdgeSegment originalS1EntrySegment = currS1Segment.getOriginalAdjacentEdgeSegments().first();
    EdgeSegment originalS2EntrySegment = currS2Segment.getOriginalAdjacentEdgeSegments().first();
    EdgeSegment originalExitSegment = currS1Segment.getOriginalAdjacentEdgeSegments().second();

    boolean s1UnCongested = !isCongested(networkLoading, originalS1EntrySegment);
    boolean s2UnCongested = !isCongested(networkLoading, originalS2EntrySegment);
    EdgeSegment s1MostRestrictingExit = null;
    double s1MostRestrExitDemandConstrainedFlow = 0;
    EdgeSegment s2MostRestrictingExit = null;
    double s2MostRestrExitDemandConstrainedFlow = 0;
    if(!s1UnCongested){
      var mostRestrictingExitDemandConstrFlowResult = identifyMostRestrictingOutSegmentAndDemandConstrainedFlow(
          originalS1EntrySegment, networkLoading); // todo should be done once and cached
      s1MostRestrictingExit = mostRestrictingExitDemandConstrFlowResult.first();
      s1MostRestrExitDemandConstrainedFlow = mostRestrictingExitDemandConstrFlowResult.second();
    }
    if(!s2UnCongested){
      var mostRestrictingExitDemandConstrFlowResult = identifyMostRestrictingOutSegmentAndDemandConstrainedFlow(
          originalS2EntrySegment, networkLoading); // todo should be done once and cached
      s2MostRestrictingExit = mostRestrictingExitDemandConstrFlowResult.first();
      s2MostRestrExitDemandConstrainedFlow = mostRestrictingExitDemandConstrFlowResult.second();
    }

    if(s1MostRestrictingExit == null){
      s1UnCongested = true;
    }
    if(s2MostRestrictingExit == null){
      s2UnCongested = true;
    }

    boolean s2OnMostRestricting = !s2UnCongested && s2MostRestrictingExit == originalExitSegment;
    boolean s1OnMostRestricting = !s1UnCongested && s1MostRestrictingExit == originalExitSegment;

    // MERGE
    //  - case 1: both congested: and s2 on most restricting to pas exit and s1 is on same most restricting
    //      compute regular full hypo and hyper critical derivatives for both. This is the simplest of cases
    //  - case 2a: both congested: s2 on most restricting to pas exit, but s1 most restricting does not
    //      lead to PAS exit. This can be tricky
    //        because s1 is giving up its unused fair share slack to s2. So shifting flow impacts this.
    //        for hypocritical we can use the normal derivative for s1 and s2. Hyper is problematic though
    //        for s1, hyper derivative only impact its entry alpha by the on-pas non-most restricting derivative, for
    //        s2 it may be negative (!) (if s1+s2> most restricting capacity. This is
    //        always the case if it is a two way merge, it may not be for a normal cross node , or if the s2 derivative
    //        flow was diluted due to earlier alphas)
    //  - case 2b: both congested: s2 and s1 NOT on most restricting (leading to PAS exit).
    //      both will individually impact their most restricting alpha on their entry segment, but neither directly
    //      impact each other since the PAS exit segment is not the cause of the alpha at hand. So,
    //      apply the special on pas derivative for hyper critical derivative for both + regular hypo critical derivative
    //  - case 2c: swap case 2a for s1 and s2: both congested: s1 on most restricting s2 is not.
    //  - case 3: both uncongested: same as case1 only now with only hypo critical component since there is
    //            no congestion. No interaction between the two.
    //  - case 4a: s1 uncongested and s2 congested but not to most restricting AND s1 has no flow to most restricting
    //      in this case changes in s1 have no impact on the congestion of s2, simply use s1 regular derivative
    //      changes in s2 affect its bargaining power towards the congested exit. Apply the same special derivative for
    //      s2 as we use for the same on-pas situation
    //  - case 4b: s1 uncongested and s2 congested but not to most restricting AND s1 has flow to most restricting
    //      now s1 has impact on congestion on s2, but that flow does not change because it is not the PAS turn. Since
    //      s1 is not congested, there is no change to this fixed flow and as such it can be treated exactly the same
    //      as case 4a
    //  - case 5a/5b: same as 4a only now swap s1 and s2: same result only apply derivatives the other way around
    //  - case 6a: s1 uncongested and s2 congested to most restricting, this one is tricky because s1 is giving up its
    //      whatever fair share slack it has to s2. So shifting flow impacts this.
    //        for hypocritical we can use the normal derivative for s1 and s2. Hyper is problematic though
    //        for s1, hyper derivative is zero, for s2 it may be negative (if s1+s2> most restricting capacity. This is
    //        always the case if it is a two way merge, it may not be for a normal cross node , or if the s1 derivative
    //        flow was diluted due to earlier alphas)
     //  - case 6b: same as 6a only now swap s1 and s2: same result only apply derivatives the other way around
    if(!s1UnCongested && !s2UnCongested) {
      if(s2OnMostRestricting){
        //case 1
        if(s1MostRestrictingExit==s2MostRestrictingExit) {
          double s1DTravelTimeDFlow = considerS1 ? computeRegularDTravelTimeDFlowSingleLink(
              theMode, physicalCost, virtualCost, originalS1EntrySegment, s1UnCongested) : 0;
          double s2DTravelTimeDFlow = considerS2 ? computeRegularDTravelTimeDFlowSingleLink(
              theMode, physicalCost, virtualCost, originalS2EntrySegment, s2UnCongested) : 0;
          return s2DTravelTimeDFlow + s1DTravelTimeDFlow;
        }else {
          //case 2a
          // s2 (congested), start with hypo portion which is always the same
          //note: when s1 is not considered it has no impact on s2 in which case the special merge derivative does not
          // apply and instead we revert to the regular combined hypo + hyper critical derivative
          double s2HypoDTravelTimeDFlow = considerS2 ? computeRegularDTravelTimeDFlowSingleLink(
              theMode, physicalCost, virtualCost, originalS2EntrySegment, considerS2 ? true : s2UnCongested) : 0;

          // s1 (congested but not on most restricting, hypo + on-pas special derivative)
          double s1DTravelTimeDFlow = 0;
          if(considerS1) {
            s1DTravelTimeDFlow = computeHyperCriticalDTravelTimeDFlowNotMostRestrictiveTurnOnCongestedLink(
                networkLoading, physicalCost, originalS1EntrySegment, s1MostRestrictingExit, s1MostRestrExitDemandConstrainedFlow);
            s1DTravelTimeDFlow += computeRegularDTravelTimeDFlowSingleLink(
                theMode, physicalCost, virtualCost, originalS1EntrySegment, true /* uncongested only*/);
          }

          var combinedDtravelTimeDflow = s1DTravelTimeDFlow * s1DerivativeReductionFactor +
              s2HypoDTravelTimeDFlow * s2DerivativeReductionFactor;
          double specialCombinedHyperDTravelTimeDflow = 0;
          if(considerS1 && considerS2) {
            var uncongestedEntry = originalS1EntrySegment; // congested but not on most restricting, so on exit not considered congested
            var congestedEntry = originalS2EntrySegment;
            var congestedMostRestricting = s2MostRestrictingExit;
            var congestedDerivativeReductionFactor = s2DerivativeReductionFactor;
            specialCombinedHyperDTravelTimeDflow = computeHyperCriticalDTravelTimeDFlowCombinedMergeSpecialCase(
                networkLoading, physicalCost, congestedEntry, uncongestedEntry, congestedMostRestricting);
            combinedDtravelTimeDflow += specialCombinedHyperDTravelTimeDflow * congestedDerivativeReductionFactor;
            if (combinedDtravelTimeDflow < 0) {
              // for non linear FD branches the hypo derivative should generally avoid this, as only hyper derivative may be
              // negative. Regardless, we want to avoid issues and instead truncate to zero. Log for debugging todo analyse impact
              LOGGER.warning(String.format("Negative derivative on merge for pas %s. To avoid issues truncate to zero", pas));
              combinedDtravelTimeDflow = 0;
            }
          }
          return combinedDtravelTimeDflow;

// old approach.
//          // s1 (congested)
//          double s1DTravelTimeDFlow = 0;
//          if (considerS1) {
//            s1DTravelTimeDFlow = computeHyperCriticalDTravelTimeDFlowNotMostRestrictiveTurnOnCongestedLink(
//                networkLoading, physicalCost, originalS1EntrySegment, s1MostRestrictingExit, s1MostRestrExitDemandConstrainedFlow);
//            s1DTravelTimeDFlow += computeRegularDTravelTimeDFlowSingleLink(
//                theMode, physicalCost, virtualCost, originalS1EntrySegment, true /* uncongested only*/);
//          }
//          // s2 (congested)
//          double s2DTravelTimeDFlow = considerS2 ? computeRegularDTravelTimeDFlowSingleLink(
//              theMode, physicalCost, virtualCost, originalS2EntrySegment, s2UnCongested) : 0;
//          return s2DerivativeReductionFactor * s2DTravelTimeDFlow + s1DerivativeReductionFactor * s1DTravelTimeDFlow;
        }
      }else if(!s1OnMostRestricting && !s2OnMostRestricting){
        // case 2b
        double s1DTravelTimeDFlow = 0;
        if (considerS1) {
          s1DTravelTimeDFlow = computeHyperCriticalDTravelTimeDFlowNotMostRestrictiveTurnOnCongestedLink(
              networkLoading, physicalCost, originalS1EntrySegment, s1MostRestrictingExit, s1MostRestrExitDemandConstrainedFlow);
          s1DTravelTimeDFlow += computeRegularDTravelTimeDFlowSingleLink(
              theMode, physicalCost, virtualCost, originalS1EntrySegment, true /* uncongested only*/);
        }
        double s2DTravelTimeDFlow = 0;
        if(considerS2) {
          s2DTravelTimeDFlow = computeHyperCriticalDTravelTimeDFlowNotMostRestrictiveTurnOnCongestedLink(
              networkLoading, physicalCost, originalS2EntrySegment, s2MostRestrictingExit, s2MostRestrExitDemandConstrainedFlow);
          s2DTravelTimeDFlow += computeRegularDTravelTimeDFlowSingleLink(
              theMode, physicalCost, virtualCost, originalS2EntrySegment, true /* uncongested only*/);
        }
        return s2DerivativeReductionFactor * s2DTravelTimeDFlow + s1DerivativeReductionFactor * s1DTravelTimeDFlow;
      }else if(s1OnMostRestricting){
        // case 2c

        // s1 (congested), start with hypo portion which is always the same
        //note: when s2 is not considered it has no impact on s1 in which case the special merge derivative does not
        // apply and instead we revert to the regular combined hypo + hyper critical derivative
        double s1HypoDTravelTimeDFlow = considerS1 ? computeRegularDTravelTimeDFlowSingleLink(
            theMode, physicalCost, virtualCost, originalS1EntrySegment, considerS2 ? true : s1UnCongested) : 0;

        // s2 (congested but not on most restricting, hypo + on-pas sepcial derivative)
        double s2DTravelTimeDFlow = 0;
        if(considerS2) {
          s2DTravelTimeDFlow = computeHyperCriticalDTravelTimeDFlowNotMostRestrictiveTurnOnCongestedLink(
              networkLoading, physicalCost, originalS2EntrySegment, s2MostRestrictingExit, s2MostRestrExitDemandConstrainedFlow);
          s2DTravelTimeDFlow += computeRegularDTravelTimeDFlowSingleLink(
              theMode, physicalCost, virtualCost, originalS2EntrySegment, true /* uncongested only*/);
        }

        var combinedDtravelTimeDflow = s1HypoDTravelTimeDFlow * s1DerivativeReductionFactor +
            s2DTravelTimeDFlow * s2DerivativeReductionFactor;
        double specialCombinedHyperDTravelTimeDflow = 0;
        if(considerS1 && considerS2) {
          var uncongestedEntry = originalS2EntrySegment; // congested but not on most restricting, so on exit not considered congested
          var congestedEntry = originalS1EntrySegment;
          var congestedMostRestricting = s1MostRestrictingExit;
          var congestedDerivativeReductionFactor = s1DerivativeReductionFactor;
          specialCombinedHyperDTravelTimeDflow = computeHyperCriticalDTravelTimeDFlowCombinedMergeSpecialCase(
              networkLoading, physicalCost, congestedEntry, uncongestedEntry, congestedMostRestricting);
          combinedDtravelTimeDflow += specialCombinedHyperDTravelTimeDflow * congestedDerivativeReductionFactor;
          if (combinedDtravelTimeDflow < 0) {
            // for non linear FD branches the hypo derivative should generally avoid this, as only hyper derivative may be
            // negative. Regardless, we want to avoid issues and instead truncate to zero. Log for debugging todo analyse impact
            LOGGER.warning(String.format("Negative derivative on merge for pas %s. To avoid issues truncate to zero", pas));
            combinedDtravelTimeDflow = 0;
          }
        }
        return combinedDtravelTimeDflow;
      }
    }else if(s1UnCongested && s2UnCongested){
      // case 3 (identical to case 1, only now both are flagged uncongested yielding different derivative)
      // s1 (uncongested)
      double s1DTravelTimeDFlow = considerS1 ? computeRegularDTravelTimeDFlowSingleLink(
          theMode, physicalCost, virtualCost, originalS1EntrySegment, s1UnCongested) : 0;
      // s2 (uncongested)
      double s2DTravelTimeDFlow = considerS2 ?computeRegularDTravelTimeDFlowSingleLink(
          theMode, physicalCost, virtualCost, originalS2EntrySegment, s2UnCongested) : 0;
      return s2DerivativeReductionFactor * s2DTravelTimeDFlow + s1DerivativeReductionFactor * s1DTravelTimeDFlow;
    }else if(s1UnCongested && !s2UnCongested && !s2OnMostRestricting) {
      //case4(a and b)
      // s1 (uncongested)
      double s1DTravelTimeDFlow = considerS1 ? computeRegularDTravelTimeDFlowSingleLink(
          theMode, physicalCost, virtualCost, originalS1EntrySegment, s1UnCongested) : 0;
      // s2 (congested but s2 same as on-pas situation where link is congested but turn not to most restricting)
      double s2DTravelTimeDFlow = 0;
      if(considerS2) {
        s2DTravelTimeDFlow = computeHyperCriticalDTravelTimeDFlowNotMostRestrictiveTurnOnCongestedLink(
            networkLoading, physicalCost, originalS2EntrySegment, s2MostRestrictingExit, s2MostRestrExitDemandConstrainedFlow);
        s2DTravelTimeDFlow += computeRegularDTravelTimeDFlowSingleLink(
            theMode, physicalCost, virtualCost, originalS2EntrySegment, true /* uncongested only*/);
      }
      return s2DerivativeReductionFactor * s2DTravelTimeDFlow + s1DerivativeReductionFactor * s1DTravelTimeDFlow;
    }else if(!s1UnCongested && !s1OnMostRestricting && s2UnCongested) {
      //case5(a and b)
      // s1 (congested but not relevant for merge)
      double s1DTravelTimeDFlow = 0;
      if(considerS1) {
        s1DTravelTimeDFlow = computeHyperCriticalDTravelTimeDFlowNotMostRestrictiveTurnOnCongestedLink(
            networkLoading, physicalCost, originalS1EntrySegment, s1MostRestrictingExit, s1MostRestrExitDemandConstrainedFlow);
        s1DTravelTimeDFlow += computeRegularDTravelTimeDFlowSingleLink(
            theMode, physicalCost, virtualCost, originalS1EntrySegment, true /* uncongested only*/);
      }
      // s2 (uncongested)
      double s2DTravelTimeDFlow = considerS2 ? computeRegularDTravelTimeDFlowSingleLink(
          theMode, physicalCost, virtualCost, originalS2EntrySegment, s2UnCongested) : 0;
      return s2DerivativeReductionFactor * s2DTravelTimeDFlow + s1DerivativeReductionFactor * s1DTravelTimeDFlow;
    }else if( (s1UnCongested && !s2UnCongested && s2OnMostRestricting) ||
        (s2UnCongested && !s1UnCongested && s1OnMostRestricting)){
      // case 6/6b (very similar to 2c, only (uncongested/congested non-restrictive) entry segment derivative is different

      // s1 (uncongested)
      //note: when s1 is not considered it has no impact on s2 in which case the special merge derivative does not
      // apply and instead we revert to the regular combined hypo + hyper critical derivative
      double s1HypoDTravelTimeDFlow = considerS1 ? computeRegularDTravelTimeDFlowSingleLink(
          theMode, physicalCost, virtualCost, originalS1EntrySegment, true /* force uncongested */) : 0;
      // s2 (uncongested) --> always flagged uncongested to only get hypo unless s1 does not vary and we trigger full
      //                      regular derivative as well as switching off special case below. When hypo special case is
      //                      used to determine hyper
      double s2HypoDTravelTimeDFlow = considerS2 ? computeRegularDTravelTimeDFlowSingleLink(
          theMode, physicalCost, virtualCost, originalS2EntrySegment, considerS1 ? true : s2UnCongested) : 0;

      var combinedDtravelTimeDflow = s1HypoDTravelTimeDFlow * s1DerivativeReductionFactor +
          s2HypoDTravelTimeDFlow * s2DerivativeReductionFactor;
      double specialCombinedHyperDTravelTimeDflow = 0;
      if(considerS1 && considerS2) {
        var uncongestedEntry = s1UnCongested ? originalS1EntrySegment : originalS2EntrySegment;
        var congestedEntry = s1UnCongested ? originalS2EntrySegment : originalS1EntrySegment;
        var congestedMostRestricting = s1UnCongested ? s2MostRestrictingExit : s1MostRestrictingExit;
        var congestedDerivativeReductionFactor = s1UnCongested ? s2DerivativeReductionFactor : s1DerivativeReductionFactor;
        specialCombinedHyperDTravelTimeDflow = computeHyperCriticalDTravelTimeDFlowCombinedMergeSpecialCase(
            networkLoading, physicalCost, congestedEntry, uncongestedEntry, congestedMostRestricting);
        combinedDtravelTimeDflow += specialCombinedHyperDTravelTimeDflow * congestedDerivativeReductionFactor;
        if (combinedDtravelTimeDflow < 0) {
          // for non linear FD branches the hypo derivative should generally avoid this, as only hyper derivative may be
          // negative. Regardless, we want to avoid issues and instead truncate to zero. Log for debugging todo analyse impact
          LOGGER.warning(String.format("Negative derivative on merge for pas %s. To avoid issues truncate to zero", pas));
          combinedDtravelTimeDflow = 0;
        }
      }
      return combinedDtravelTimeDflow;
    }


    LOGGER.severe(String.format("Unrecognised traffic state for merge derivative calculation on pas %s, " +
        "should not happen", pas));
    return 0;
  }

  protected static Pair<Double, Boolean> determinePasLinkSegmentSlackFlow(
      StaticLtmLoadingBushBase<?> networkLoading,
      ConjugateEdgeSegment conjAltEdgeSegment,
      double proposedFlowShift, // should not be truncated with slack because we should not confound the two (to find slack we should not use upstream slack)
      boolean slackToTriggerCongestion){
    return determinePasLinkSegmentSlackFlow(
        networkLoading,
        conjAltEdgeSegment,
        proposedFlowShift,
        slackToTriggerCongestion,
        0,
        0,
        null);
  }

  /**
   * compute slack flow for segment
   *
   * @param networkLoading to use
   * @param conjAltEdgeSegment to compute for
   * @param upperBoundFlowShift expected shift on turn itself, to use for improved estimate of slack
   *                            (always positive, call will adjust for removal or addition based on
   *                            #slackToTriggerCongestion)
   * @param slackToTriggerCongestion type of slack
   * @param virtualChangeOtherConsumedEntryCapacity additional (can be negative) other consumed entry capacity (only relevant for diverges, otherwise 0)
   * @param virtualChangeWithinTrafficStateConsumedExitCapacity additional (can be negative) change to consumed exit capacity (only relevant for merges, otherwise 0)
   * @param virtualExitChangeTriggeredByEntrySegment entry segment of turn that causes virtual change on exit (only relevant for merge)
   * @return slack flow and whether it is on most restricting turn of entry link
   */
  protected static Pair<Double, Boolean> determinePasLinkSegmentSlackFlow(
      StaticLtmLoadingBushBase<?> networkLoading,
      ConjugateEdgeSegment conjAltEdgeSegment,
      double upperBoundFlowShift,
      boolean slackToTriggerCongestion,
      double virtualChangeOtherConsumedEntryCapacity,
      double virtualChangeWithinTrafficStateConsumedExitCapacity,
      EdgeSegment virtualExitChangeTriggeredByEntrySegment){

    boolean currentlyMostRestricting = false;
    EdgeSegment originalEntrySegment = conjAltEdgeSegment.getOriginalAdjacentEdgeSegments().first();
    EdgeSegment originalExitSegment = conjAltEdgeSegment.getOriginalAdjacentEdgeSegments().second();
    if(originalExitSegment == null || originalEntrySegment == null){
      return Pair.of(Double.MAX_VALUE, currentlyMostRestricting);
    }
    double originalEntrySegmentCapacity = Math.min(TampereNodeModelFixedInput.DEFAULT_MAX_IN_CAPACITY,
        ((PcuCapacitated) originalEntrySegment).getCapacityOrDefaultPcuH());
    double originalExitSegmentCapacity = Math.min(TampereNodeModelFixedInput.DEFAULT_MAX_IN_CAPACITY,
        ((PcuCapacitated) originalExitSegment).getCapacityOrDefaultPcuH());

    double turnAlpha = networkLoading.getCurrentFlowAcceptanceFactors()[(int)originalEntrySegment.getId()];
    double turnSplittingRate = networkLoading.getSplittingRateData().getSplittingRate(originalEntrySegment, originalExitSegment);
    double totalInflow = networkLoading.getCurrentInflowsPcuH()[(int)originalEntrySegment.getId()];
    double turnSendingFlow = totalInflow * turnSplittingRate;
    double turnAcceptedFlow = turnSendingFlow * turnAlpha;

    if(!slackToTriggerCongestion && turnAlpha>= 1.0){
      // cannot change state further when already uncongested for high cost
      // todo: can consider state change from most-restricting to non-most-restricting turn (remaining congested)....
      return Pair.of(Double.MAX_VALUE, currentlyMostRestricting);
    }
    // Note: we do not stop for the reciprocal situation because if the turn is congested but we're looking to trigger
    // congestion, we do not yet know if our turn is the most restricting (in future we should), now we only know this
    // after the below. If not on most restricting, we calculate the slack until it become most restricting, which is a
    // state change in itself. This is then returned as the result.

    // determine fair share the turn has a right to claim given current state
    // NOTE: we adjust the actual splitting rate to a virtual splitting rate if we expect a flow increase as it will
    // result in extra bargaining power on the exit that will delay a state change. We cannot consider the same impact
    // when a shift would reduce flow because in that case we need the current state as is to be able to extract
    // slack flow until a state change. IF we'd attempt to update splitting rate we may get negative or zero flows after
    // adjustment, which can cause problems, so we put that in the too hard basket for now.
    double compensatoryFlowShift = slackToTriggerCongestion ? upperBoundFlowShift : 0;

    double virtualTotalInflow = Math.min(
        originalEntrySegmentCapacity, totalInflow + compensatoryFlowShift + virtualChangeOtherConsumedEntryCapacity);
    double virtualTurnSendingFlow =
        Math.min(originalEntrySegmentCapacity, turnSendingFlow + compensatoryFlowShift);
    double virtualUpdatedSplittingRate = Math.min(1, virtualTurnSendingFlow/virtualTotalInflow);
    double virtualFairShareNumerator = originalEntrySegmentCapacity * virtualUpdatedSplittingRate;
    if(virtualTurnSendingFlow <= 0){
      if(slackToTriggerCongestion) {
        throw new PlanItRunTimeException("Should never trigger because virtual turn sending flow should always be positive" +
            "when moving to congestion");
      }else{
        // no possibility to move to uncongested when no flow on turn (already uncongested)
        return Pair.of(Double.MAX_VALUE, currentlyMostRestricting);
      }
    }
    double fairShareDenominator = virtualFairShareNumerator;

    // find out if our turn is on most restricting without running node model
    double totalOtherConsumedExitCapacity = 0;
    double totalOtherCongestedConsumedExitCapacity = 0;
    double virtualOtherConsumedExitCapacity = 0;
    double virtualOtherCongestedConsumedExitCapacity = 0;
    for(var currEntrySegment : originalEntrySegment.getDownstreamVertex().getEntryEdgeSegments()){
      if(currEntrySegment == originalEntrySegment){
        continue;
      }
      double currEntrySegmentCapacity = Math.min(TampereNodeModelFixedInput.DEFAULT_MAX_IN_CAPACITY,
          ((PcuCapacitated) currEntrySegment).getCapacityOrDefaultPcuH());

      double currSplittingRate = networkLoading.getSplittingRateData().getSplittingRate(currEntrySegment, originalExitSegment);
      double currTotalInflow = networkLoading.getCurrentInflowsPcuH()[(int)currEntrySegment.getId()];

      double currAlpha = networkLoading.getCurrentFlowAcceptanceFactors()[(int)currEntrySegment.getId()];
      double currTurnSendingFlow = currTotalInflow * currSplittingRate;
      double currTurnAcceptedFlow = currTurnSendingFlow * currAlpha;
      totalOtherConsumedExitCapacity += currTurnAcceptedFlow;

      if(currTurnAcceptedFlow > 0 && (currAlpha + EPSILON_6) < 1){
          // flow present towards "our" exit, so it may claim fair share hence included in calc
          // todo: we should track if the turn is most restricting, only if it is most restricting
          //  we should count it towards the fair share denominator otherwise we are being too restrictive, now
          //  we count any flow from a congested entry towards this
          fairShareDenominator += currEntrySegmentCapacity * currSplittingRate;
          totalOtherCongestedConsumedExitCapacity += currTurnAcceptedFlow;
      }

      // populate adjustments when relevant (since they are assumed to remaining with the current traffic state,
      // no change to fair share is required (and we keep splitting rate fixed for convenience for now)
      if((virtualChangeWithinTrafficStateConsumedExitCapacity!= 0 && currEntrySegment==virtualExitChangeTriggeredByEntrySegment)){
        virtualOtherConsumedExitCapacity = virtualChangeWithinTrafficStateConsumedExitCapacity;
        virtualOtherCongestedConsumedExitCapacity =
            (currAlpha + EPSILON_6) < 1 ? virtualChangeWithinTrafficStateConsumedExitCapacity : 0;
      }
    }
    // base on actual current state without virtual adjustments
    // note we are conservative here to classify as most restricting a bit quicker when really close
    // because if we get it wrong, the result may be (close to) a zero flow slack result despite already being in that
    // state.
    currentlyMostRestricting = ((turnAlpha + EPSILON_6) < 1.0) &&
        (originalExitSegmentCapacity - totalOtherConsumedExitCapacity) - turnAcceptedFlow <= EPSILON_1;

    // fair share is based on supply constrained demand towards the exit, so remove demand constrained flow from capacity
    double guaranteedDemandConstrainedExitFlow =
        (totalOtherConsumedExitCapacity + virtualOtherConsumedExitCapacity) -
            (totalOtherCongestedConsumedExitCapacity + virtualOtherCongestedConsumedExitCapacity);
    double fairShare =
        (originalExitSegmentCapacity - guaranteedDemandConstrainedExitFlow) *
            (virtualFairShareNumerator / fairShareDenominator);

    // knowing its fair share we determine the actual slack based on absolute observed slack and fair share
    // combined
    double absoluteSlackFlowWithoutTurn =
        originalExitSegmentCapacity - (totalOtherConsumedExitCapacity + virtualOtherConsumedExitCapacity);

    // LOW COST
    if(slackToTriggerCongestion) {
      if(currentlyMostRestricting){
        // cannot change state further when already congested due to being on most restricting for low cost
        return Pair.of(Double.MAX_VALUE, currentlyMostRestricting);
      }else {
        double absoluteSlackFlow = absoluteSlackFlowWithoutTurn - turnAcceptedFlow;
        // slack to triggering congestion on our turn is the slack until fair share is reached, or if more is left over, the absolute
        // slack remaining. Note it is possible that link is already congested due to other turn being most restricting,
        // hence we multiple the fair share based slack with the reciprocal of the current alpha to account for that
        //todo: we can split this in two states and consider that a state change as well
        double turnSlack = (1/turnAlpha) * Math.max(absoluteSlackFlow, Math.max(0, fairShare - turnAcceptedFlow));
        return Pair.of(turnSlack, currentlyMostRestricting);
      }
    }
    // HIGH COST
    else{
      if((turnAcceptedFlow + EPSILON_6) < fairShare){
        // already virtually uncongested (not on most restricting turn), so any reduction will not trigger
        // a traffic state change (ignoring the fact we free up bargaining power for actual most restricting turn)
        return Pair.of(Double.MAX_VALUE, false);
      }
      if((turnAcceptedFlow + EPSILON_6) < absoluteSlackFlowWithoutTurn){
        // again virtually uncongested already (not on most restricting turn), because excess consumable turn flow not used yet
        // hence further reduction will not cause a traffic state change (ignoring the fact we free up bargaining power
        // for actual most restricting turn)
        return Pair.of(Double.MAX_VALUE, false);
      }

      // slack to removing congestion is the minimum between when we would drop below fair share or when
      // sending flow no longer exceeds available consumable exit capacity - other turn accepted flows
      //todo: we can split this in two states and consider that a state change as well
      double turnSlack = turnSendingFlow - Math.max(absoluteSlackFlowWithoutTurn, fairShare);
      return Pair.of(turnSlack, currentlyMostRestricting);
    }
  }

  // s1, s2 result (slack,minsegment,continue)
  protected Pair<Triple<Double,EdgeSegment, Boolean>,Triple<Double,EdgeSegment, Boolean>> determinePasDivergeSlackFlow(
      StaticLtmLoadingBushBase<?> networkLoading, double proposedPasSendingFlowShift) {
    double s1SlackFlow = Double.MAX_VALUE;
    double s2SlackFlow = Double.MAX_VALUE;
    EdgeSegment s1MinSlackSegment = null;
    EdgeSegment s2MinSlackSegment = null;

    // track if downstream turns are still to be considered or not
    boolean continueS1 = true;
    boolean continueS2 = true;

    var s1 = this.pas.getAlternative(true);
    var s2 = this.pas.getAlternative(false);

    var s1ConjAltEdgeSegment = s1[0];
    var s2ConjAltEdgeSegment = s2[0];
    EdgeSegment originalEntry = s1ConjAltEdgeSegment.hasOriginalEntryEdgeSegment() ? s1ConjAltEdgeSegment.getOriginalAdjacentEdgeSegments().first() : null;
    if (originalEntry != null) {
      double turnAlpha =
          networkLoading.getCurrentFlowAcceptanceFactors()[(int) originalEntry.getId()];
      boolean congested = (turnAlpha + EPSILON_6) < 1;
      var s1RawTurnSlackResult = determinePasLinkSegmentSlackFlow(
          networkLoading,
          s1ConjAltEdgeSegment,
          proposedPasSendingFlowShift, // expected shift overall
          true,                        // slack to trigger congestion
          -proposedPasSendingFlowShift, // expected amount removed from diverge due to s2
          0,
          null);
      s1SlackFlow = s1RawTurnSlackResult.first();
      s1MinSlackSegment = originalEntry;

      if (congested) {
        // s2 may become uncongested
        var s2RawTurnSlackResult = determinePasLinkSegmentSlackFlow(
            networkLoading,
            s2ConjAltEdgeSegment,
            proposedPasSendingFlowShift,  // expected shift overall
            false,                        // slack to resolve congestion
            proposedPasSendingFlowShift,  // expected amount added to diverge due to s1
            0,
            null);
        boolean s1OnMostRestrictingTurn = s1RawTurnSlackResult.second();
        boolean s2OnMostRestrictingTurn = s2RawTurnSlackResult.second();

        if (s1OnMostRestrictingTurn) {
          // - For low cost flow adding flow to THIS turn and onward will not trickle down, STOP BEFORE UPDATING
          continueS1 = false;
        } else if (s2OnMostRestrictingTurn) {
          // - for high cost THIS turn is the final turn that matters, so we update
          //    the slack for this turn...and then switch off further downstream considerations.
          s2SlackFlow = s2RawTurnSlackResult.first();
          s2MinSlackSegment = originalEntry;
          continueS2 = false;
        }
      }
    }

    return Pair.of(
        Triple.of(s1SlackFlow, s1MinSlackSegment, continueS1),
        Triple.of(s2SlackFlow, s2MinSlackSegment, continueS2));
  }

  // for given  alt: [slack flow on pas, min segment, continue downstream], compoundedAlpha
  protected Pair<Triple<Double,EdgeSegment, Boolean>, Double> determineOnPasSlackFlow(
      Triple<Double, EdgeSegment, Boolean> divergeResult,
      double proposedPasSendingFlowShift,
      boolean slackToCongestion,
      double compoundedAlpha,
      StaticLtmLoadingBushBase<?> networkLoading) {

    double slackFlow = divergeResult.first();
    EdgeSegment minSlackSegment = divergeResult.second();
    boolean altContinue =  divergeResult.third();
    if(!altContinue){
      return Pair.of(divergeResult, compoundedAlpha);
    }

    // regular PAS traversal rework back to original link segments
    ConjugateEdgeSegment conjAltEdgeSegment;
    ConjugateEdgeSegment[] conjAltEdgeSegments = pas.getAlternative(slackToCongestion);
    int index = 1;
    int lastIndex = conjAltEdgeSegments.length-1;
    for (; index < lastIndex; ++index) {
      conjAltEdgeSegment = conjAltEdgeSegments[index];
      var originalEntry = conjAltEdgeSegment.getOriginalAdjacentEdgeSegments().first();

      double turnAlpha = networkLoading.getCurrentFlowAcceptanceFactors()[(int)originalEntry.getId()];
      boolean congested = (turnAlpha + EPSILON_6) < 1;
      var rawTurnSlackResult = determinePasLinkSegmentSlackFlow(
          networkLoading,
          conjAltEdgeSegment,
          proposedPasSendingFlowShift * compoundedAlpha,
          slackToCongestion);
      boolean onMostRestrictingTurn = rawTurnSlackResult.second();
      boolean congestedOnMostRestricting = congested && onMostRestrictingTurn;
      if(slackToCongestion && congestedOnMostRestricting){
        // - For low cost flow adding flow to THIS turn and onward will not trickle down, STOP BEFORE UPDATING
        // - for high cost THIS turn is the final turn that matters, so we DO NOT break here, but after updating
        //    the slack for this turn...and then break, see below.
        altContinue = false;
        break;
      }
      //todo could be improved if we determine how much we should remove from non-most restricting to make most-restricting
      // become uncongested

      double rawTurnSlack = rawTurnSlackResult.first();
      // divide because a flow change is diluted when passing through alpha<1, so slack after such a point
      // increases reciprocally
      double turnSlack = rawTurnSlack / compoundedAlpha;
      if(turnSlack < slackFlow){
        minSlackSegment = originalEntry;
        slackFlow = turnSlack;
      }

      if(!slackToCongestion && congestedOnMostRestricting){
        // same as above only now we must consider the slack on this turn as it is the final slack that matters to trigger
        // a state change for the high cost alt.
        altContinue = false;
        break;
      }

      // note alpha<1 does not lead to stopping if it is not most restricting, in those cases flow changes still
      // propagate only less, so, hence multiply with factor but continue
      compoundedAlpha *= turnAlpha;
    }

    return Pair.of(Triple.of(slackFlow, minSlackSegment, altContinue), compoundedAlpha);
  }

  // s1/s2 result [slack,slacksegment]
  protected Pair<Pair<Double,EdgeSegment>,Pair<Double,EdgeSegment>> determinePasMergeSlackFlow(
      Triple<Double, EdgeSegment, Boolean> s1OnPasResult, double s1CompoundedAlpha,
      Triple<Double, EdgeSegment, Boolean> s2OnPasResult, double s2CompoundedAlpha,
      StaticLtmLoadingBushBase<?> networkLoading,
      double proposedPasSendingFlowShift) {

    // track if downstream turns are still to be considered or not
    boolean considerS1 = s1OnPasResult.third();
    boolean considerS2 = s2OnPasResult.third();
    if((!considerS1 && !considerS2)) {
      return Pair.of(s1OnPasResult.asPairFirstSecond(), s2OnPasResult.asPairFirstSecond());
    }

    double s1SlackFlow = s1OnPasResult.first(); // up till now
    double s2SlackFlow = s2OnPasResult.first(); // up till now
    EdgeSegment s1MinSlackSegment = s1OnPasResult.second();
    EdgeSegment s2MinSlackSegment = s2OnPasResult.second();

    var s1ConjAltEdgeSegment = this.pas.getLastEdgeSegment(true);
    var s2ConjAltEdgeSegment = this.pas.getLastEdgeSegment(false);
    EdgeSegment s1OriginalEntry = s1ConjAltEdgeSegment.getOriginalAdjacentEdgeSegments().first();
    EdgeSegment s2OriginalEntry = s2ConjAltEdgeSegment.getOriginalAdjacentEdgeSegments().first();

    double s1TurnAlpha = networkLoading.getCurrentFlowAcceptanceFactors()[(int)s1OriginalEntry.getId()];
    double s2TurnAlpha = networkLoading.getCurrentFlowAcceptanceFactors()[(int)s2OriginalEntry.getId()];

    boolean s1Congested = (s1TurnAlpha + EPSILON_6) < 1;
    boolean s2Congested = (s2TurnAlpha + EPSILON_6) < 1;

    double s1UpperboundArrivingFlowShift = considerS1 ?
      proposedPasSendingFlowShift * s1CompoundedAlpha : 0;
    double s2UpperboundArrivingFlowShift = considerS2 ?
        proposedPasSendingFlowShift * s2CompoundedAlpha : 0;
    // this is used for expected impact, but do not use when we need upper bound flow shift itself
    double s1UpperboundArrivingFlowShiftIncSlack = Math.min(s1UpperboundArrivingFlowShift, s1SlackFlow);
    double s2UpperboundArrivingFlowShiftIncSlack = Math.min(s2UpperboundArrivingFlowShift, s2SlackFlow);

    // case 1: s1 congested, s2 uncongested:
    //         infinite slack both in most extreme state and becoming more extreme by adding removing flow, UNLESS
    //         s1 is on most restricting and we remove more than we add due to compounding alphas in which case s1
    //         may become uncongested. In that case:
    //          s2: compute the expected removal of flow as is
    //          s1: compute when it would become UNcongested (not injecting s2 change because that would breal calc).
    //            Then the S2 slack should be restricted to the s1 slack that we found.
    //         if s1 not on most restricting --> treat as case 3
    // case 2: s1 uncongested, s2 congested: both run risk of changing traffic state unless s2 is not on most restricting
    //         s2 on most restricting:
    //          compute individual slack flow for each, but consider the proposed flow shift as well:
    //          s1: compute when it would become congested (can ignore s2 shift since it is congested already)
    //          s2: compute when it would become uncongested considering adding proposed shift from s1 (compound alpha adjusted)
    //         s2 not on most restricting:
    //           revert to case 3
    // case 3: s1 uncongested, s2 uncongested: first check if proposed shift considering compounded alpha leads to more
    //         added than removed, if so,
    //          s1: compute when it would become congested considering removal of proposed shift from s2 (compound alpha adjusted)
    //          s2: compute when it would become CONGESTED considering expected shift from s1 (compound alpha adjusted)
    // case 4: s1 congested, s2 congested:
    //        check if s1&S2 not on most restricting --> treat as case 3
    //        check if s1 not on most restricting, s2 on most restricting --> treat as case 2
    //        check if s2 not on most restricting, s1 on most restricting --> treat as case 1
    //        if truly case 4 ==>
    //          both are claiming fair share, we are removing from s2 up to its slack to free flow which is non-zero
    //          for s1 to become uncongested, first s2 needs to become uncongested which is already captured by s2 slack
    //          so no action needed for s1.
    //
    boolean case1 = s1Congested && !s2Congested;
    boolean case2 = !s1Congested && s2Congested;
    boolean case3 = !s1Congested && !s2Congested;
    boolean case4 = s1Congested && s2Congested;

    boolean s1OnMostRestricting = false;
    boolean s2OnMostRestricting = false;
    Pair<Double, Boolean> s1ToFreeFlowMergeSlackResult = null;
    Pair<Double, Boolean> s2ToFreeFlowMergeSlackResult = null;
    if(s1Congested) {
      // do not provide any adjustments yet because we do not know if they would
      // remainn within the current traffic state. USe this result to bootstrap that for later if needed
      s1ToFreeFlowMergeSlackResult = determinePasLinkSegmentSlackFlow(
          networkLoading,
          s1ConjAltEdgeSegment,
          s1UpperboundArrivingFlowShift,
          false);
      s1OnMostRestricting = s1ToFreeFlowMergeSlackResult.second();
    }
    if(s2Congested) {
      // do not provide any adjustments yet because we do not know if they would
      // remainn within the current traffic state. USe this result to bootstrap that for later if needed
      s2ToFreeFlowMergeSlackResult = determinePasLinkSegmentSlackFlow(
          networkLoading,
          s2ConjAltEdgeSegment,
          s2UpperboundArrivingFlowShift,
          false);
      s2OnMostRestricting = s2ToFreeFlowMergeSlackResult.second();
    }

    if(case4) { //s1 and s2 congested
      // first delegate to other cases if needed
      if(!s1OnMostRestricting && !s2OnMostRestricting) {
        // treat both as uncongested
        case3 = true;
      }else if(!s1OnMostRestricting) {
        // treat s1 as uncongested
        case2 = true;
      }else if(!s2OnMostRestricting) {
        // treat s2 as uncongested
        case1 = true;
      }else {
        // congested both on most restricting, only if more flow gets removed than added we can expect a traffic state
        if(!considerS2) {
          // can never remove more than we add, done
          return Pair.of(s1OnPasResult.asPairFirstSecond(), s2OnPasResult.asPairFirstSecond());
        }

        // while possible that both become uncongested, s2 will always become uncongested first because that is where
        // flow is removed and since we stop shifting at that point, s1 slack to free flow can be ignored for the merge
        // (it is already congested so can't change to congested). Just update s2 slack
        double s2TurnSlack = s2ToFreeFlowMergeSlackResult.first() / s2CompoundedAlpha;
        if (s2TurnSlack < s2SlackFlow) {
          s2MinSlackSegment = s2OriginalEntry;
          s2SlackFlow = s2TurnSlack;
        }
      }
    }

    if(case1) { //s1 merge congested, s2 merge uncongested
      if(!considerS2) {
        // can never remove more than we add, done
        return Pair.of(s1OnPasResult.asPairFirstSecond(), s2OnPasResult.asPairFirstSecond());
      }

      if(!s1OnMostRestricting) {
        // treat as if s1 is uncongested
        case3 = true;
      }else {
        double expectedDifferenceIncludingSlackBoundBetweenS1S2 =
            s1UpperboundArrivingFlowShiftIncSlack - s2UpperboundArrivingFlowShiftIncSlack;
        if (expectedDifferenceIncludingSlackBoundBetweenS1S2 >= 0) {
          // so far add more to s1 than we  remove from s2, not possible to trigger free flow on s1, done
          return Pair.of(s1OnPasResult.asPairFirstSecond(), s2OnPasResult.asPairFirstSecond());
        }
        // s1 may become free flow (net removal of flow on merge)
        var s1MergeSlackFlow = s1ToFreeFlowMergeSlackResult.first();
        double s1TurnSlack = s1MergeSlackFlow / s1CompoundedAlpha;
        // apply as restriction on S2 because we should remove no more from S2 to avoid triggering the change on s1
        // Weird one because it triggers free flow on S1 rather than S2
        if (s1TurnSlack < s2SlackFlow) {
          s2MinSlackSegment = s2OriginalEntry;
          s2SlackFlow = s1TurnSlack;
        }
      }
    }else if(case2){ // s1 uncongested turn, s2 congested turn on merge
      // detect if on most restricting (before fiddling with additional flows which mess up that check).
      if(!s2OnMostRestricting){
        // we can treat s2 as uncongested --> become case 3 (with alpha adjustment)
        // todo: there is impact because bargaining power is increased, can improve and compute this impact (not yet done)
        case3 = true;
      }else {
        // s2 most restricting --> determine how much we expect s1 to add and use it to get improved estimate of s2
        // slack flow
        if (considerS1) {
          // consider expected remaining removal on s2 (use tentative to free flow slack instead of current s2 slack flow
          // which may be too loose (we fix that up later but want to avoid circular dependency))
          double withinStateS2ExpectedFlowShiftImpactOnS1 = considerS2 ?
              -Math.min(s2ToFreeFlowMergeSlackResult.first(), s2UpperboundArrivingFlowShiftIncSlack) * s2TurnAlpha  : 0;
          // s1 base slack + factor in base slack of s2 as maximum allowed change on s2 truncated by proposed shift
          var s1ToCongestedSlackResult = determinePasLinkSegmentSlackFlow(
              networkLoading,
              s1ConjAltEdgeSegment,
              s1UpperboundArrivingFlowShift,
              true,
              0,
              withinStateS2ExpectedFlowShiftImpactOnS1,
              s2OriginalEntry);
          double s1MergeSlackFlow = s1ToCongestedSlackResult.first();
          double s1TurnSlack = s1MergeSlackFlow / s1CompoundedAlpha;
          if (s1TurnSlack < s1SlackFlow) {
            s1MinSlackSegment = s1OriginalEntry;
            s1SlackFlow = s1TurnSlack;
          }
        }
        if (considerS2) {
          // update this to get better estimate for s2 slack by virtually conducting the flow shift as part of computation
          // (when s1 adds flow it becomes harder to become uncongested, so slack increases for s2, this is what we consider)
          double withinTrafficStateS1ExpectedFlowShiftImpactOnS2 = considerS1 ?
              Math.min(s1SlackFlow, s1UpperboundArrivingFlowShiftIncSlack) * s1TurnAlpha : 0;
          // s2 slack - considering difference between s1 and s2 additional s1 expected flow shift considering slack
          var s2ToFreeFlowSlackWithS1Adjustment = determinePasLinkSegmentSlackFlow(
              networkLoading,
              s2ConjAltEdgeSegment,
              s2UpperboundArrivingFlowShift,
              false,
              0,
              withinTrafficStateS1ExpectedFlowShiftImpactOnS2,
              s1OriginalEntry).first();
          double s2TurnSlack = s2ToFreeFlowSlackWithS1Adjustment / s2CompoundedAlpha;
          if (s2TurnSlack < s2SlackFlow) {
            s2MinSlackSegment = s2OriginalEntry;
            s2SlackFlow = s2TurnSlack;
          }
        }
      }
    }

    if(case3) { //s1 uncongested, s2 uncongested for merge turn
      double s1ExpectedExitFlowShiftIncSlack = s1UpperboundArrivingFlowShiftIncSlack;
      double s2ExpectedExitFlowShiftIncSlack = -s2UpperboundArrivingFlowShiftIncSlack;
      if(!considerS1) {
        // if we do not add flow on s1, we cannot trigger a state change
        return Pair.of(s1OnPasResult.asPairFirstSecond(), s2OnPasResult.asPairFirstSecond());
      }
      if(case1 || case4) {
        //adjust with turn alpha
        s1ExpectedExitFlowShiftIncSlack *= s1TurnAlpha;
      }
      if(case2 || case4) {
        //adjust with turn alpha
        s2ExpectedExitFlowShiftIncSlack *= s2TurnAlpha;
      }
      double expectedDifferenceBetweenS1S2 = s1ExpectedExitFlowShiftIncSlack + s2ExpectedExitFlowShiftIncSlack;
      if(expectedDifferenceBetweenS1S2 <= 0) {
        // negative so we remove more or equal from s2 than we add to s1, so no chance of traffic state change, done
        return Pair.of(s1OnPasResult.asPairFirstSecond(), s2OnPasResult.asPairFirstSecond());
      }

      // adding to same exit, only single slack value exists. We add more to s1 than we remove from s2,
      // so we should compute s1 slack given the removal of expected s2 factored in
      // note this indirectly accounts for the situation that by adding to s1, s2 may become congested as this will be
      // computed as a function of s1 slack flow as well.
      var rawSlack = determinePasLinkSegmentSlackFlow(
          networkLoading,
          s1ConjAltEdgeSegment,
          s1UpperboundArrivingFlowShift,
          true,
          0,
          s2ExpectedExitFlowShiftIncSlack,
          s2OriginalEntry).first();
      // it depends on the compounded alphas what the final sending flow slack will be, so update both
      double s1TurnSlack = rawSlack / s1CompoundedAlpha;
      if (s1TurnSlack < s1SlackFlow) {
        s1MinSlackSegment = s1OriginalEntry;
        s1SlackFlow = s1TurnSlack;
      }
      if (considerS2 && (!case2 && !case4)) { //exclude case2/4 (because we know s2 is congested already so computed slack is not representative)
        double s2TurnSlack = rawSlack / s2CompoundedAlpha;
        if (s2TurnSlack < s2SlackFlow) {
          s2MinSlackSegment = s2OriginalEntry;
          s2SlackFlow = s2TurnSlack;
        }
      }
    }
    return Pair.of(Pair.of(s1SlackFlow, s1MinSlackSegment), Pair.of(s2SlackFlow,s2MinSlackSegment));
  }

  /**
   * Determine pas slack flow considering the entire pas and interaction between the two alternatives.
   *
   * @param networkLoading to use
   * @return proposedFlowShift to consider (both provided in positive numbers)
   */
  protected Pair<Pair<Double,EdgeSegment>,Pair<Double,EdgeSegment>> determinePasSlackFlow(
      double proposedFlowShift, StaticLtmLoadingBushBase<?> networkLoading) {

//    boolean pasUncongested =
//        pas.getStatus() != PasStatus.CONGESTED && pas.getStatus() != PasStatus.UNCONGESTED_POTENTIALLY_CONGESTED;

    var pasConjAltEdgeSegment = this.pas.getAlternative(true)[0];
    EdgeSegment originalEntry = pasConjAltEdgeSegment.hasOriginalEntryEdgeSegment() ?
        pasConjAltEdgeSegment.getOriginalAdjacentEdgeSegments().first() : null;
    double startAlpha = (originalEntry != null) ?
        networkLoading.getCurrentFlowAcceptanceFactors()[(int) originalEntry.getId()] : 1;

    // DIVERGE
    var divergeResult = determinePasDivergeSlackFlow(networkLoading, proposedFlowShift);

    // ON-PAS
    var s1OnPasResult = determineOnPasSlackFlow(
        divergeResult.first(), proposedFlowShift, true, startAlpha, networkLoading);
    double s1CompoundedAlpha = s1OnPasResult.second();

    var s2OnPasResult = determineOnPasSlackFlow(
        divergeResult.second(), proposedFlowShift, false, startAlpha, networkLoading);
    double s2CompoundedAlpha = s2OnPasResult.second();

    //MERGE
    var finalResult = determinePasMergeSlackFlow(
        s1OnPasResult.first(),
        s1CompoundedAlpha,
        s2OnPasResult.first(),
        s2CompoundedAlpha,
        networkLoading,
        proposedFlowShift);

    return finalResult;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  protected Pair<Double,EdgeSegment> determinePasAlternativeSlackFlow(
          StaticLtmLoadingBushBase<?> networkLoading, double proposedFlowShift, boolean lowCost) {

    // NO LONGER USED --> USE CUSTOM ONE ABOVE

    double slackFlow = Double.POSITIVE_INFINITY;
    EdgeSegment minSlackSegment = null;

    boolean pasUncongested =
        pas.getStatus() != PasStatus.CONGESTED && pas.getStatus() != PasStatus.UNCONGESTED_POTENTIALLY_CONGESTED;

    // regular PAS traversal rework back to original link segments
    ConjugateEdgeSegment conjAltEdgeSegment;
    ConjugateEdgeSegment[] conjAltEdgeSegments = pas.getAlternative(lowCost);
    int index = 0;
    int lastIndex = conjAltEdgeSegments.length-1;
    double compoundedAlpha = 1;
    for (; index <= lastIndex; ++index) {
      conjAltEdgeSegment = conjAltEdgeSegments[index];
      if (!conjAltEdgeSegment.hasOriginalEntryEdgeSegment()) {
        continue;
      }

      if(index==lastIndex && lowCost && pasUncongested){
        // merge in this situation would shift flow from high to low with no net change on exit --> ignore for slack flow
        // calc as it would be too restricting otherwise
        break;
      }

      double turnAlpha =
          networkLoading.getCurrentFlowAcceptanceFactors()[(int)conjAltEdgeSegment.getOriginalAdjacentEdgeSegments().first().getId()];
      var rawTurnSlackResult =
          determinePasLinkSegmentSlackFlow(networkLoading, conjAltEdgeSegment, proposedFlowShift, lowCost);
      boolean onMostRestrictingTurn = rawTurnSlackResult.second();

      boolean congestedOnMostRestricting = (turnAlpha + EPSILON_6) < 1 && onMostRestrictingTurn;
      if(lowCost && congestedOnMostRestricting){
        // - For low cost flow adding flow to THIS turn and onward will not trickle down, STOP BEFORE UPDATING
        // - for high cost THIS turn is the final turn that matters, so we DO NOT break here, but after updating
        //    the slack for this turn...and then break, see below.
        if(minSlackSegment==null){
          // avoid null return
          minSlackSegment = conjAltEdgeSegment.getOriginalAdjacentEdgeSegments().first();
        }
        break;
      }

      double rawTurnSlack = rawTurnSlackResult.first();
      // divide because a flow change is diluted when passing through alpha<1, so slack after such a point
      // increases reciprocally
      double turnSlack = rawTurnSlack / compoundedAlpha;
      if(turnSlack < slackFlow){
        minSlackSegment = conjAltEdgeSegment.getOriginalAdjacentEdgeSegments().first();
        slackFlow = turnSlack;
      }

      if(!lowCost && congestedOnMostRestricting){
        // same as above only now we must consider the slack on this turn as it is the final slack that matters to trigger
        // a state change for the high cost alt.
        break;
      }

      // note alpha<1 does not lead to stopping if it is not most restricting, in those cases flow changes still
      // propagate only less, so, hence multiply with factor but continue

      compoundedAlpha *= turnAlpha;
    }
    return Pair.of(slackFlow, minSlackSegment);
  }

  /**
   * {@inheritDoc}
   */
  @Deprecated
  @Override
  protected double[] executeBushS2FlowShiftNoNodeModelUpdate(
          RootedBush<ConjugateDirectedVertex, ConjugateEdgeSegment> bush,
          EdgeSegment entrySegment,
          double bushEntrySegmentFlowShift,
          Mode theMode,
          StaticLtmAssignmentStrategy assignmentStrategy,
          double[] originalNetworkCosts,
          double[] conjSegmentCosts,
          Set<? extends RootedBush<?,?>> bushes) {

    return null;
  }

  @Deprecated
  protected double[] executeBushS2FlowShiftNodeModelUpdate(
      RootedBush<ConjugateDirectedVertex, ConjugateEdgeSegment> bush,
      double bushEntrySegmentFlowShift,
      Mode theMode,
      double[] originalNlFlowAcceptanceFactors,             // alphas from loading rather than on-the-fly, used for bounding shifts
      StaticLtmConjugateBushStrategy assignmentStrategy,
      double[] originalNetworkCosts,
      double[] conjSegmentCosts,
      Set<ConjugateDestinationBush> bushes) {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Deprecated
  @Override
  protected void executeBushS1FlowShiftNoNodeModelUpdate(
          RootedBush<ConjugateDirectedVertex, ConjugateEdgeSegment> bush,
          final EdgeSegment entrySegment,
          double bushEntrySegmentFlowShift,
          Mode theMode,
          StaticLtmAssignmentStrategy assignmentStrategy, // not relevant in conjugate form
          double[] endMergeSplittingRates,
          double[] originalNetworkCosts,
          double[] conjSegmentCosts,
          Set<? extends RootedBush<?,?>> bushes) {
  }

  @Deprecated
  protected void executeBushS1FlowShiftNodeModelUpdate(
      RootedBush<ConjugateDirectedVertex, ConjugateEdgeSegment> bush,
      double bushEntrySegmentFlowShift,
      Mode theMode,
      double[] originalNlFlowAcceptanceFactors,             // alphas from loading rather than on-the-fly, used for bounding shifts
      StaticLtmConjugateBushStrategy assignmentStrategy,
      double[] originalNetworkCosts,
      double[] conjSegmentCosts,
      Set<ConjugateDestinationBush> bushes) {
  }

  /**
   * New approach which is per PAS and then per alternative link for all bushes
   * returns applied shifts per bush on initial diverge
   */
  protected Map<ConjugateDestinationBush, Double> executePasFlowShift(
      double initialReferenceWeight,
      Map<ConjugateDestinationBush, Double> initialBushWeights,
      double pasFlowShift,
      Mode theMode,
      double[] originalNlFlowAcceptanceFactors,             // alphas from loading rather than on-the-fly, used for bounding shifts
      StaticLtmConjugateBushStrategy assignmentStrategy,
      double[] originalNetworkCosts,
      double[] conjSegmentCosts,
      Set<ConjugateDestinationBush> bushes,
      boolean updateNetworkAcceptanceFactors,
      boolean logAll) {

    /* prep - pas */
    final var s2 = pas.getAlternative(false);
    final var s1 = pas.getAlternative(true);

    // DIVERGE
    // pair< pair<s1,s2 remaining shifts>, per bush weights/applied shifts>
    var divergeFlowShiftResult = executeDivergeFlowShift(
        initialReferenceWeight, initialBushWeights, pasFlowShift, theMode, originalNlFlowAcceptanceFactors,
        assignmentStrategy, originalNetworkCosts, conjSegmentCosts, bushes, updateNetworkAcceptanceFactors, logAll);
    if(divergeFlowShiftResult==null || divergeFlowShiftResult.anyIsNull()){
      return null;
    }
    var s1Result = divergeFlowShiftResult.first()[0];
    var s2Result = divergeFlowShiftResult.first()[1];
    var bushDivergeAppliedShiftsAsWeights = divergeFlowShiftResult.second();

    // ON-PAS
      // S2
    var s2RemainingFlowShiftResult = executeOnPasPerAlternativeFlowShift(
        bushDivergeAppliedShiftsAsWeights, s2Result, s2, theMode, originalNlFlowAcceptanceFactors, assignmentStrategy,
        originalNetworkCosts, conjSegmentCosts, bushes,updateNetworkAcceptanceFactors, logAll);
      //S1
    var s1RemainingFlowShiftResult = executeOnPasPerAlternativeFlowShift(
        bushDivergeAppliedShiftsAsWeights, s1Result, s1, theMode, originalNlFlowAcceptanceFactors, assignmentStrategy,
        originalNetworkCosts, conjSegmentCosts, bushes,updateNetworkAcceptanceFactors, logAll);

    // MERGE
    executeMergeFlowShift(bushDivergeAppliedShiftsAsWeights, s1RemainingFlowShiftResult, s2RemainingFlowShiftResult,
        theMode, originalNlFlowAcceptanceFactors, assignmentStrategy,
        originalNetworkCosts, conjSegmentCosts, bushes, updateNetworkAcceptanceFactors, logAll);

    return bushDivergeAppliedShiftsAsWeights;
  }

  // remove when consolidation works
//
//  /**
//   * identical to executePasFlowShiftNodeModelUpdate, except passing in different parameters
//   * todo: consolidate into single general method with above
//   */
//  protected Map<ConjugateDestinationBush, Double> executePasFlowShiftNoNodeModelUpdate(
//      double startingReferenceWeight,
//      Map<ConjugateDestinationBush, Double> startingBushWeights,
//      double pasFlowShift,
//      Mode theMode,
//      StaticLtmConjugateBushStrategy assignmentStrategy,
//      double[] originalNetworkCosts,
//      double[] conjSegmentCosts,
//      Set<ConjugateDestinationBush> bushes,
//      boolean logAll) {
//
//    /* prep - pas */
//    final var s2 = pas.getAlternative(false);
//    final var s1 = pas.getAlternative(true);
//
//    // DIVERGE
//    var appliedFlowShiftPcuHPerBush = executeDivergeFlowShift(
//        startingReferenceWeight, startingBushWeights, pasFlowShift, theMode, null,
//        assignmentStrategy, originalNetworkCosts, conjSegmentCosts, bushes, false, logAll);
//    if(appliedFlowShiftPcuHPerBush==null || appliedFlowShiftPcuHPerBush.anyIsNull()){
//      return null;
//    }
//    var s1Result = appliedFlowShiftPcuHPerBush.first()[0];
//    var s2Result = appliedFlowShiftPcuHPerBush.first()[1];
//    double appliedSendingFlowShift = -s2Result.first();
//    var bushAppliedDivergeShiftsAsWeights = appliedFlowShiftPcuHPerBush.second();
//
//    // ON-PAS
//    // S2
//    var s2RemainingFlowShiftResult = executeOnPasPerAlternativeFlowShift(
//        bushAppliedDivergeShiftsAsWeights, s2Result, s2, theMode, null, assignmentStrategy,
//        originalNetworkCosts, conjSegmentCosts, bushes,false, logAll);
//    //S1
//    var s1RemainingFlowShiftResult = executeOnPasPerAlternativeFlowShift(
//        bushAppliedDivergeShiftsAsWeights, s1Result, s1, theMode, null, assignmentStrategy,
//        originalNetworkCosts, conjSegmentCosts, bushes,false, logAll);
//
//    // MERGE
//    executeMergeFlowShift(bushAppliedDivergeShiftsAsWeights, s1RemainingFlowShiftResult, s2RemainingFlowShiftResult,
//        theMode, null, assignmentStrategy,
//        originalNetworkCosts, conjSegmentCosts, bushes, false, logAll);
//
//    return bushAppliedDivergeShiftsAsWeights;
//  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void stepOneDetermineNetworkLoadingConsistentS1S2SendingFlows(double[] flowAcceptanceFactors) {
    var s2 = pas.getAlternative(false /* high cost */);
    var s1 = pas.getAlternative(true  /* low  cost */);

    double s1SendingFlow = 0;
    double s2SendingFlow = 0;
    for (var bush : pas.getRegisteredBushes()) {
      ConjugateDestinationBush destinationBush = (ConjugateDestinationBush) bush;

      double s2BushSendingFlow = destinationBush.determineSubPathSendingFlow(s2, flowAcceptanceFactors);
      s2SendingFlow += s2BushSendingFlow;
      double s1BushSendingFlow = destinationBush.determineSubPathSendingFlow(s1, flowAcceptanceFactors);
      s1SendingFlow += s1BushSendingFlow;
    }
    s1S2SendingFlows = Pair.of(s1SendingFlow, s2SendingFlow);
  }

  /**
   * recursive version that can be called of determineProposedFlowShiftByLoadingEntrySegment
   * boolean returned indicates if proposed shift was truncated based on discontinuity check
   */
  public Pair<Map<EdgeSegment, Double>,Boolean> determineProposedFlowShift(
      Mode theMode,
      GapFunction gapFunction,
      AbstractPhysicalCost physicalCost,
      AbstractVirtualCost virtualCost,
      Smoothing smoothing,
      double additionalSmoothingFactor,
      StaticLtmLoadingBushBase<?> networkLoading,
      double guaranteedS2SendingFlow,
      boolean logAll,
      StringBuilder sb, // for logging
      Set<EdgeSegment> chainDerivativeBeyondBottleneckSegments) {

    boolean discontinuityTruncation = false;

    // todo: once we no longer have non-conjugate implementation remove any entry segment based tracking of flow shifts
    Map<EdgeSegment, Double> result = new TreeMap<>();
    var originalEntrySegment =
        pas.getFirstEdgeSegment(false).getOriginalAdjacentEdgeSegments().first();

    var firstS2CongestedSegment = findFirstCongestedEdgeSegmentOnPasAlternative(networkLoading, false);

    var denominatorDivergeResult = getDTravelTimeDFlowDiverge(theMode, networkLoading, physicalCost, virtualCost);
    boolean s1Continue = (boolean) denominatorDivergeResult[2];
    boolean s2Continue = (boolean) denominatorDivergeResult[3];
    if(!s2Continue && chainDerivativeBeyondBottleneckSegments!= null && originalEntrySegment!=null
        && chainDerivativeBeyondBottleneckSegments.contains(originalEntrySegment)) {
      s2Continue = true;
    }
    double derivativeReductionFactorDiverge = (double) denominatorDivergeResult[1];
    double divergeDenominator = (double) denominatorDivergeResult[0];

    double onPasS1Denominator = 0;
    double onPasS2Denominator = 0;
    double derivativeReductionFactorS1UpToMerge = 1;
    double derivativeReductionFactorS2UpToMerge = 1;
    if(s1Continue) {
      var denominatorS1Result = getDTravelTimeDFlowExcludingMergeDiverge(
          theMode, networkLoading, physicalCost, virtualCost, true, derivativeReductionFactorDiverge, null);
      onPasS1Denominator = denominatorS1Result.first();
      derivativeReductionFactorS1UpToMerge = denominatorS1Result.second();
      s1Continue = denominatorS1Result.third();
    }
    if(s2Continue) {
      var denominatorS2Result = getDTravelTimeDFlowExcludingMergeDiverge(
          theMode, networkLoading, physicalCost, virtualCost, false, derivativeReductionFactorDiverge, chainDerivativeBeyondBottleneckSegments);
      onPasS2Denominator = denominatorS2Result.first();
      derivativeReductionFactorS2UpToMerge = denominatorS2Result.second();
      s2Continue = denominatorS2Result.third();
    }

    double mergeDenominator = 0;
    if(s1Continue || s2Continue) {
      mergeDenominator = getDTravelTimeDFlowMerge(
          theMode, networkLoading, physicalCost, virtualCost, s1Continue, s2Continue,
          derivativeReductionFactorS1UpToMerge, derivativeReductionFactorS2UpToMerge);
    }

    /* s1 and/or s2 congested - derivative based flow shift possible */
    // tauw_s1 + dtauw_s1/ds_1 * (flowShift) = tauw_s2 + dtauw_s2/ds_2 * (-flowShift) we find:
    // tauw_s1 - tauw_s2 = dtauw_s2/ds_2 * (-flowShift)  - dtauw_s1/ds_1 * (flowShift)
    // tauw_s1 - tauw_s2 = flowShift * (-dtauw_s1/ds_1 - dtauw_s2/ds_2)
    // flowShift = (tauw_s2 - tauw_s1) / (dtauw_s1/ds_1 + dtauw_s2/ds_2)
    double denominator = divergeDenominator + onPasS1Denominator + onPasS2Denominator + mergeDenominator;
    double numerator = pas.getAlternativeHighCost() - pas.getAlternativeLowCost();
    if(logAll){
//      LOGGER.info(String.format("divergeDenominator: %.10f", divergeDenominator));
//      LOGGER.info(String.format("onPasS1Denominator: %.10f", onPasS1Denominator));
//      LOGGER.info(String.format("onPasS2Denominator: %.10f", onPasS2Denominator));
//      LOGGER.info(String.format("mergeDenominator: %.10f", mergeDenominator));
//      LOGGER.info(String.format("*Denominator: %.10f", denominator));
//      LOGGER.info(String.format("*numerator: %.10f", numerator));
    }

    if(Double.isNaN(denominator)){
      LOGGER.severe("Found denominator being a NaN should never happen");
    }
    if(denominator < 0){
      LOGGER.severe(String.format("Found negative denominator on pas (%s), should never happen", this.pas.toString()));
    }


    double proposedFlowShift = 0;
    boolean pasCostEqual = pas.isCostEqual(EPSILON);

    if (!pasCostEqual && smallerEqual(denominator,EPSILON,EPSILON)) {
      /* s1 & S2 UNCONGESTED - no derivative estimate possible (denominator zero) */
      /* move all towards cheaper alternative */
      proposedFlowShift = guaranteedS2SendingFlow;
    }else if(numerator != 0){
      proposedFlowShift = numerator / denominator;
    }

    double chosenPerIterationFlowShift = 0;
    if(proposedFlowShift > 0){
      // apply smoothing (do before discontinuity truncation to avoid stalling any changes when close to a discontinuity)
      double smoothedFlowShift = smoothing.executeRefZero(proposedFlowShift); // deprecated, should not be active
      chosenPerIterationFlowShift = smoothedFlowShift * additionalSmoothingFactor;
      if(isDestinationTrackedForLogging() || logAll) {
        var message = String.format(" Proposed shift: %.10f, Smoothed shift: %.10f, per iteration shift: %.10f",
            proposedFlowShift,smoothedFlowShift, chosenPerIterationFlowShift);
        //sb.append(message).append(System.lineSeparator());
        LOGGER.info(message);
      }

      var slackResult = determinePasSlackFlow(chosenPerIterationFlowShift, networkLoading);
      var lowCostSlackResult = slackResult.first();
      var highCostSlackResult = slackResult.second();

      double s1SlackFlowEstimate = lowCostSlackResult.first();
      // always adjust for possible s1 discontinuities
      {
        /* possible triggering of congestion on s1 due to shift -> passing discontinuity on travel time function */
        double oldFlowShift = chosenPerIterationFlowShift;
        chosenPerIterationFlowShift =
            adjustFlowShiftBasedOnSlackFlow(chosenPerIterationFlowShift, s1SlackFlowEstimate, true);
        if(oldFlowShift > chosenPerIterationFlowShift){
          discontinuityTruncation = true;
          if((isDestinationTrackedForLogging() || logAll)){
            var message = String.format("S1 DISCONTINUITY ADJUSTMENT TRIGGERED (on segment %s) from %.10f, to %.10f",
                lowCostSlackResult.second().getIdsAsString(), oldFlowShift, chosenPerIterationFlowShift);
            //sb.append(message).append(System.lineSeparator());
            LOGGER.info(message);
          }
        }
      }

      double s2SlackFlowEstimate = highCostSlackResult.first();

      /* possible triggering of congestion on s1 due to shift -> passing discontinuity on travel time function */
      double oldFlowShift = chosenPerIterationFlowShift;
      chosenPerIterationFlowShift = adjustFlowShiftBasedOnSlackFlow(chosenPerIterationFlowShift, s2SlackFlowEstimate, false);
      if(oldFlowShift > chosenPerIterationFlowShift){
        discontinuityTruncation = true;
        if(isDestinationTrackedForLogging() || logAll ) {
          var message = String.format("S2 DISCONTINUITY ADJUSTMENT TRIGGERED (on segment %s) from %.10f, to %.10f",
              highCostSlackResult.second().getIdsAsString(), oldFlowShift, chosenPerIterationFlowShift);
          //sb.append(message).append(System.lineSeparator());
          LOGGER.info(message);
        }
        // chaining, it works but impact is very low and it is costly, so disable for now
//      if(chainDerivativeBeyondBottleneckSegments == null){
//        chainDerivativeBeyondBottleneckSegments = new TreeSet<>();
//      }
//      EdgeSegment allowChainingDerivativesBeyondSegment = highCostSlackResult.second();
//      if(!chainDerivativeBeyondBottleneckSegments.contains(allowChainingDerivativesBeyondSegment)) {
//        // redo derivative calculation for S2 since we have to anticipate flow is no longer withheld at current
//        // congested entry (if we do not do this, our step might be too high)
//        chainDerivativeBeyondBottleneckSegments.add(allowChainingDerivativesBeyondSegment);
//        var adjustedResult = determineProposedFlowShift(
//            theMode, gapFunction, physicalCost, virtualCost, smoothing, additionalSmoothingFactor, networkLoading,
//            guaranteedS2SendingFlow, logAll, chainDerivativeBeyondBottleneckSegments);
//        smoothedPerIterationFlowShift = adjustedResult.values().iterator().next();
//      }
      }
    }else{
      chosenPerIterationFlowShift = 0;
    }

    if(originalEntrySegment != null) {
      result.put(originalEntrySegment, chosenPerIterationFlowShift);
    }else{
      // use dummy since entry segment is not used in conjugate anyway, but it can't be null while
      // for conjuate connector turn there may be no original
      result.put(pas.getFirstEdgeSegment(true), chosenPerIterationFlowShift);
    }
    return Pair.of(result, discontinuityTruncation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<EdgeSegment, Double> determineProposedFlowShiftByLoadingEntrySegment(
      Mode theMode,
      GapFunction gapFunction,
      AbstractPhysicalCost physicalCost,
      AbstractVirtualCost virtualCost,
      Smoothing smoothing,
      double additionalSmoothingFactor,
      StaticLtmLoadingBushBase<?> networkLoading,
      double guaranteedS2SendingFlow,
      boolean logAll) {
    // no longer used, we bypass it but interface still requires it, remove once done
    return null;
  }

  /**
   * all PASs that - if we were to execute the proposed shift - remain uncongested, will be equilibrated over
   * multiple iterations. Costs and flows on the network, bushes will be updated in full. If an uncongested PAS
   * cannot be updated because it would trigger a state change, the flow change is NOT executed and its status is
   * changed from {@code PasStatus.UNCONGESTED_WITH_SHIFT} to {@code PasStatus.UNCONGESTED_WITHOUT_SHIFT}
   *
   * @param theMode              to use
   * @param assignmentStrategy       to use
   * @param originalNetworkCosts to use
   * @param conjSegmentCosts     to use
   * @param logAll               to use
   * @return total shifted flow
   */
  public double performEquilibratedUncongestedFlowShifts(
      Mode theMode,
      StaticLtmAssignmentStrategy assignmentStrategy,
      double[] nlConsistentFlowAcceptanceFactors,
      double[] originalNetworkCosts,
      double[] conjSegmentCosts,
      Set<ConjugateDestinationBush> bushes,
      boolean logAll,
      double additionalSmoothingFactor) {

    boolean smoothOverIterations = false;
    boolean updateNetworkAcceptanceFactors = true;
    // when alphas not updated, we must initialise this to the original slack found to minimise risk of traffic state changes while shifting
    // ie when budget is spent we stop updating.
    double initialFlowShiftBudget = Double.MAX_VALUE;

    var conjStrategy = (StaticLtmConjugateBushStrategy) assignmentStrategy;
    var networkLoading = conjStrategy.getLoading();

    // only consider PAS when it is potentially uncongested, confirm later with explicit check
    if(this.pas.getStatus() == PasStatus.CONGESTED
        || this.pas.getStatus() == PasStatus.UNCONGESTED_POTENTIALLY_CONGESTED){
      return 0.0;
    }

    // update costs because if another overlapping uncongested PAS was updated previously, current costs are no longer
    // up to date
    pas.updateCost(conjSegmentCosts);

    // Make sure original sending flows as a constraint are locked in via originalBushTurnFlowTracker
    // so we do not run the risk of
    // considering too much flow to shift as this will act as a bound for those cases where flow was added
    // to a low cost segment by another PAS. originalBushTurnFlowTracker is used in the determination of the
    // constrained subpath sending flows later
    updateOriginalBushTurnFlowTracker();

    double s1SendingFlow = 0;
    for (var bush : pas.getRegisteredBushes()) {
      s1SendingFlow += bush.determineSubPathSendingFlow(
          pas.getAlternative(true), networkLoading.getCurrentFlowAcceptanceFactors());
    }

    // post-convergence smoothing --> since s1/s2 may flip, we track by initial turn. this is used
    // to establish final flow shift based on smoothing post-converged
    // new altflow = step * original alt flow + (1-step) * converged alt flow
    final Map<ConjugateDestinationBush,Double> flowShiftTrackerForInitialAltTurn = new TreeMap<>();
    ConjugateEdgeSegment initialAltTurn = pas.getFirstEdgeSegment(false);
    var originalPasBushes = new TreeSet<RootedBush<?,?>>(pas.getRegisteredBushes());

    // enter uncongested equilibration phase.
    boolean converged = false;
    int MAX_INTERAL_ITERATIONS_ALLOWED = 10;
    int internalIteration = 1;
    boolean doNotStop = true;
    boolean flowShifted = false;
    double totalPasShift = 0;
    Map<ConjugateDestinationBush, Double> bushS2RemainingSendingFlows = new TreeMap<>();
    double guaranteedS2SendingFlow = 0;
    do{

      //--------------- UPDATE SENDING FLOWS THROUGH ALTERNATIVE ------------------------------------
      // sub path nl sending flows current (which is likely different and lower/higher than the one consistent
      // with loading due to other uncongested flow shifts performed on other PASs, or in previous updates here).
      // todo: while this is better for rounding, we could just adjust these values without going through the motions
      //  each iteration based on the flow shift applied.
      bushS2RemainingSendingFlows.clear();
      guaranteedS2SendingFlow = 0;
      for (var bush : pas.getRegisteredBushes()) {
        double remainingSubPathSendingFlow = bush.determineSubPathSendingFlow(
            pas.getAlternative(false), networkLoading.getCurrentFlowAcceptanceFactors());
        if(remainingSubPathSendingFlow > 0) {
          bushS2RemainingSendingFlows.put((ConjugateDestinationBush) bush, remainingSubPathSendingFlow);
          guaranteedS2SendingFlow += remainingSubPathSendingFlow;
        }
      }

      if(guaranteedS2SendingFlow <= 0 ){
        removeZeroFlowBushesFromPas(false);
        // make sure it is not considered for congested processing, mark done for this iteration
        // we cannot rely on zero flow check alone in congested processing because, other later uncongested PASs may
        // add flow causing it to no longer have zero flow, in which case its old status will result in an attempt for
        // congested processing:
        // todo: split uncongested_without_shift into two statuses, so we only consider congested
        //  and explicitly identified as potentially congested in the congested setup --> then this can be removed
        pas.updateStatus(PasStatus.UNCONGESTED_WITH_SHIFT);
        break;
      }

      // determine proposed flow shift now that we have costs and available flows
      var proposedShiftResult = determineProposedFlowShift(
          theMode,
          assignmentStrategy.getGapFunction(), conjStrategy.getPhysicalCost(),
          conjStrategy.getVirtualCost(),
          conjStrategy.getSmoothing(),
          1, // not here, dealt with in final smoothing now
          networkLoading,
          guaranteedS2SendingFlow,
          isDestinationTrackedForLogging() || logAll,
          new StringBuilder(), //todo: use stringbuilder for uncongested logging as well
          null);
      boolean discontinuityTruncated = proposedShiftResult.second();
      double rawProposedFlowShift = proposedShiftResult.first().values().iterator().next();
      double proposedFlowShift = Math.min(rawProposedFlowShift, guaranteedS2SendingFlow); // truncate to what is available
      if(proposedFlowShift <= 0 ){
        break;
      }

      if(discontinuityTruncated || initialFlowShiftBudget<totalPasShift){
        // insufficient slack, do not process (further) - mark for congested processing
        pas.updateStatus(PasStatus.UNCONGESTED_POTENTIALLY_CONGESTED);
        break;
      }

      if(initialFlowShiftBudget==Double.MAX_VALUE) {
        // todo if PASs overlap then this won't be able to capture how much was already shifted

        // initialise shift budget
        // compute WITHOUT leeway because that is only allowed for (potentially) congested PASs
        // in a way this is more strict that the truncation check as that includes leeway
        var slackResult = determinePasSlackFlow(proposedFlowShift, networkLoading);
        initialFlowShiftBudget = Math.min(slackResult.first().first(),slackResult.second().first());
        if(initialFlowShiftBudget < proposedFlowShift){
          pas.updateStatus(PasStatus.UNCONGESTED_POTENTIALLY_CONGESTED);
          break; // can happen as this does not consider any leeway
        }
      }

      pas.updateStatus(PasStatus.UNCONGESTED_WITH_SHIFT);
      if(isDestinationTrackedForLogging() || logAll) {
        LOGGER.info("* UNCONGESTED FLOW SHIFT "+proposedFlowShift+" on PAS:" + pas + " - S2 flow: " + guaranteedS2SendingFlow + " - cost-diff: " + pas.getReducedCost());
      }

      var iterationS2FlowShiftPerBush = executePasFlowShift(
          guaranteedS2SendingFlow,
          bushS2RemainingSendingFlows,
          proposedFlowShift,
          theMode,
          nlConsistentFlowAcceptanceFactors,
          conjStrategy,
          originalNetworkCosts,
          conjSegmentCosts,
          bushes,
          updateNetworkAcceptanceFactors,
          logAll);

      // track applied shifts per bush for smoothing
      // todo if we know no smoothing happens we should skip
      double iterationPasShift = -iterationS2FlowShiftPerBush.values().stream().mapToDouble(d->d).sum();
      boolean negateValues = !pas.getFirstEdgeSegment(false).equals(initialAltTurn);
      iterationS2FlowShiftPerBush.forEach((key, value) -> flowShiftTrackerForInitialAltTurn.put(key,
          flowShiftTrackerForInitialAltTurn.getOrDefault(key, 0.0) +
              (negateValues ? -value : value)));

      totalPasShift += iterationPasShift;
      flowShifted = flowShifted || totalPasShift>0;

      // sync costs to changes in flow, to allow for next proposed flow update
      boolean costSwitch = false;
      {
        // sync local PAS cost based on synced network costs
        costSwitch = pas.updateCost(conjSegmentCosts);
      }

      if(isDestinationTrackedForLogging() || logAll) {
        LOGGER.info("  (after shift) PAS:" + pas + " - S2 flow: " + guaranteedS2SendingFlow + " - cost-diff: " + pas.getReducedCost());
        LOGGER.info("  (after shift) cost-diff: " + pas.getReducedCost());
      }

      s1SendingFlow += iterationPasShift;
      double s2SendingFlow = Math.max(0, guaranteedS2SendingFlow - totalPasShift);
      if(costSwitch){
        double prevS1SendingFlow = s1SendingFlow;
        s1SendingFlow = s2SendingFlow;
        s2SendingFlow = prevS1SendingFlow;
      }

      // when very low flows on PAS we normalise to 1 for gap calculation to avoid stopping to early due to flow
      // distorting gap
      double pasGap = 0;
      if(s2SendingFlow > 0) {
        pasGap = pas.getReducedCost() * Math.max(1, s2SendingFlow)
            /
            (pas.getAlternativeLowCost() * Math.max(1,(s1SendingFlow + s2SendingFlow)));
      }else{
        pasGap = pas.getReducedCost();
      }

      // reuse criterion of gap (overall gap is done wider, so we do not update gap as such here)
      converged = pasGap <= conjStrategy.getGapFunction().getStopCriterion().getEpsilon();
      doNotStop = !converged && internalIteration++ < MAX_INTERAL_ITERATIONS_ALLOWED &&
        initialFlowShiftBudget > totalPasShift; // stop when we exceeded the budget (only relevant when no updating alphas)

      // remove zero-flow S2 bushes from PAS when we know they won't get used again, or it is the final iteration
      if(!costSwitch || !doNotStop) {
        removeZeroFlowBushesFromPas(false /* no dangling nodes */);
      }
    }while(doNotStop);

    if(totalPasShift > 0) {

      // consider adjustment factor (only possible when smaller than one otherwise exceed bound for traffic state change)
      if(!Double.isNaN(pas.getProposedPasFlowShiftAdjustmentFactor()) &&
          !Double.isInfinite(pas.getProposedPasFlowShiftAdjustmentFactor()) &&
          pas.getProposedPasFlowShiftAdjustmentFactor()<1){
        additionalSmoothingFactor *= pas.getProposedPasFlowShiftAdjustmentFactor();
      }

      // bush smoothing - final shift when smoothing is deemed required
      originalPasBushes.forEach(b -> pas.registerBush((RootedBush<ConjugateDirectedVertex, ConjugateEdgeSegment>) b));
      double finalAppliedFlowShift = performFinalShiftForSmoothing(
          flowShiftTrackerForInitialAltTurn,
          initialAltTurn,
          theMode,
          conjStrategy,
          nlConsistentFlowAcceptanceFactors,
          originalNetworkCosts,
          conjSegmentCosts,
          bushes,
          logAll,
          updateNetworkAcceptanceFactors,
          additionalSmoothingFactor);
    }

    return totalPasShift;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double performEquilibratedCongestedFlowShifts(
      Mode theMode,
      StaticLtmAssignmentStrategy assignmentStrategy,
      double[] originalNetworkCosts,
      double[] conjSegmentCosts,
      double[] originalNlConsistentFlowAcceptanceFactors,
      Set<? extends RootedBush<?,?>> bushes,
      boolean logAll,
      double additionalSmoothingFactor) {

    boolean smoothOverIterations = false;
    boolean updateNetworkAcceptanceFactors = true;
    // when alphas not updated, we must initialise this to the original slack found to minimise risk of traffic state changes while shifting
    // ie when budget is spent we stop updating.
    double initialFlowShiftBudget = Double.MAX_VALUE;

    //TODO: largely equivalent to uncongested setup but with some tweaks to account for complexities of
    // alphas being potentially < 1, see if can be consolidated at some point as now there is a lot of duplicate
    // code
    var conjStrategy = ((StaticLtmConjugateBushStrategy)assignmentStrategy);
    var networkLoading = conjStrategy.getLoading();

    //NOT NEEDED WE DO IT IN ONE GO!
    //getFlowShiftedS2BushData().clear();

    // update before we start since any overlap with other PASs that have been updated already will cause the current
    // cost to be outdated
    pas.updateCost(conjSegmentCosts);

    double maxDeltaPasCost = Double.MAX_VALUE;
    double totalCongestedFlowShifted = 0;

    // Make sure original sending flows as a constraint are locked in via originalBushTurnFlowTracker
    // so we do not run the risk of
    // considering too much flow to shift as this will act as a bound for those cases where flow was added
    // to a low cost segment by another PAS. originalBushTurnFlowTracker is used in the determination of the
    // constrained subpath sending flows later
    updateOriginalBushTurnFlowTracker();

    var s1Alternative = pas.getAlternative(true);
    var s2Alternative = pas.getAlternative(false);

    double s1SendingFlow = 0;
    for (var bush : pas.getRegisteredBushes()) {
      s1SendingFlow += bush.determineConstrainedSubPathSendingFlow(
          s1Alternative,
          networkLoading.getCurrentFlowAcceptanceFactors(),
          originalNlConsistentFlowAcceptanceFactors,
          ((ConjugateDestinationBush)bush).bushData); // no need to constrain on s1
    }

    // post-convergence smoothing --> since s1/s2 may flip, we track by initial turn. this is used
    // to establish final flow shift based on smoothing post-converged
    // new altflow = step * original alt flow + (1-step) * converged alt flow
    var originalPasBushes = new TreeSet<RootedBush<?,?>>(pas.getRegisteredBushes());

    double prevPasGap = Double.MAX_VALUE;
    double pasGap = prevPasGap;
    pas.goldenRatioShiftBound = Double.MAX_VALUE; // reset

    // enter congested equilibration phase.
    boolean converged = false;
    int MAX_INTERAL_ITERATIONS_ALLOWED = !updateNetworkAcceptanceFactors ? 1 : 10; //lowest level loop
    double internalIterationSmoothingFactor = 1; /*additionalSmoothingFactor *
        (smoothOverIterations ? (1.0/MAX_INTERAL_ITERATIONS_ALLOWED) : 1); //<-- commented out was used for best until now, but it is nto logical this works*/
    int internalIteration = 1;
    final Map<ConjugateDestinationBush, Double> bushS2RemainingSendingFlows = new TreeMap<>();
    boolean doNotStop = true;
    double totalPasShift = 0;
    var sb = (isDestinationTrackedForLogging() || logAll) ? new StringBuilder() : null;

    final Map<ConjugateDestinationBush,Double> flowShiftTrackerForInitialAltTurn = new TreeMap<>();
    ConjugateEdgeSegment initialAltTurn = pas.getFirstEdgeSegment(false);
    if(isDestinationTrackedForLogging() || logAll) {
      var message = "----------------NEXT PAS -----------------------------";
      //sb.append(message).append(System.lineSeparator());
      LOGGER.info(message);
    }
    do{

      if (pas.pasId == 16L) { // local PAS update
        int bla = 4;

//        var theNode = pas.getLastEdgeSegment(false).getOriginalAdjacentEdgeSegments().first().getUpstreamVertex();
//        var consumer = new NMRUpdateIncomingConjugateOutFlowsFactorsAndCostsConsumer(
//            theNode, theMode, (StaticLtmConjugateBushStrategy) assignmentStrategy, originalNetworkCosts, conjSegmentCosts);
//        StaticLtmNetworkLoading.performNodeModelTurnBasedUpdate(
//            theNode, consumer, networkLoading, networkLoading.getCurrentInflowsPcuH());
      }

      //--------------- UPDATE SENDING FLOWS THROUGH ALTERNATIVE ------------------------------------
      bushS2RemainingSendingFlows.clear();
      for (var bush : pas.getRegisteredBushes()) {
        ConjugateDestinationBush conjBush = (ConjugateDestinationBush) bush;
        double remainingSubPathSendingFlow = conjBush.determineConstrainedSubPathSendingFlow(
            pas.getAlternative(false),
            networkLoading.getCurrentFlowAcceptanceFactors(),
            originalNlConsistentFlowAcceptanceFactors,
            this.originalBushTurnFlowTracker.get(conjBush)); // <-- differs from uncongested equilibration
        if(remainingSubPathSendingFlow > 0) {
          bushS2RemainingSendingFlows.put(conjBush, remainingSubPathSendingFlow);
        }
      }
      double remainingS2SendingFlow = bushS2RemainingSendingFlows.values().stream().mapToDouble(e -> e).sum();
      double guaranteedS2SendingFlow = remainingS2SendingFlow; // use latest always as it may be higher than original

      if(guaranteedS2SendingFlow <= 0 ){
        removeZeroFlowBushesFromPas(false);
        converged = true; // effectively converged
        break;
      }

      if(isDestinationTrackedForLogging() || logAll) {
        var message = "* FLOW SHIFT on PAS:" + pas + " - cost-diff: " + pas.getReducedCost() + " (guaranteed) S2 flow: "
            + guaranteedS2SendingFlow + " - S1 flow: " + s1SendingFlow;
        //sb.append(message).append(System.lineSeparator());
        LOGGER.info(message);
      }

      // determine proposed flow shift now that we have costs and available flows
      var proposedShiftResult = determineProposedFlowShift(
          theMode,
          assignmentStrategy.getGapFunction(),
          conjStrategy.getPhysicalCost(),
          conjStrategy.getVirtualCost(),
          conjStrategy.getSmoothing(),
          internalIterationSmoothingFactor,
          networkLoading,
          guaranteedS2SendingFlow,
          isDestinationTrackedForLogging() || logAll,
          sb,
          null);
      boolean discontinuityTruncated = proposedShiftResult.second();
      double proposedFlowShift = proposedShiftResult.first().values().iterator().next();
      if(proposedFlowShift <= 0){
        break;
      }

      if(proposedFlowShift > pas.goldenRatioShiftBound){
        if((isDestinationTrackedForLogging() || logAll)) {
          var message = String.format("BISECTION BOUND VIOLATION: truncate from %.10f to %10f",
              proposedFlowShift, pas.goldenRatioShiftBound);
          //sb.append(message).append(System.lineSeparator());
          LOGGER.info(message);
        }
        proposedFlowShift = pas.goldenRatioShiftBound;
      }

      if(!updateNetworkAcceptanceFactors && initialFlowShiftBudget==Double.MAX_VALUE) {
        // todo if PASs overlap then this won't be able to capture how much was already shifted
        if(discontinuityTruncated){
          initialFlowShiftBudget = proposedFlowShift; // will terminate after shift
        }else{
          // initialise shift budget (by doing it this way we ensure we include leeway in budget
          var slackResult = determinePasSlackFlow(proposedFlowShift, networkLoading);
          double rawMinSlack = Math.min(slackResult.first().first(),slackResult.second().first());
          double slackAdjustedFlowshift =
              adjustFlowShiftBasedOnSlackFlow(proposedFlowShift, rawMinSlack, false);
          if(slackAdjustedFlowshift < proposedFlowShift){
            initialFlowShiftBudget = slackAdjustedFlowshift;
          }else {
            initialFlowShiftBudget = rawMinSlack;
          }
        }
      }

      if(isDestinationTrackedForLogging() || logAll) {
        var s1Message = "   (before shift) s1 alphas: "+
            Arrays.stream(s1Alternative).filter(ConjugateEdgeSegment::hasOriginalEntryEdgeSegment).map(
                es -> String.format("%s:%.6f",
                    es.getXmlId(), networkLoading.getCurrentFlowAcceptanceFactors()[
                        (int) es.getOriginalAdjacentEdgeSegments().first().getId()])).collect(Collectors.joining(","));
        //sb.append(s1Message).append(System.lineSeparator());
        LOGGER.info(s1Message);
        var s2Message = "   (before shift) s2 alphas: "+
            Arrays.stream(s2Alternative).filter(ConjugateEdgeSegment::hasOriginalEntryEdgeSegment).map(es -> String.format("%s:%.6f",
                es.getXmlId(), networkLoading.getCurrentFlowAcceptanceFactors()[
                    (int) es.getOriginalAdjacentEdgeSegments().first().getId()])).collect(Collectors.joining(","));
        //sb.append(s2Message).append(System.lineSeparator());
        LOGGER.info(s2Message);
      }

      double chosenFlowShift = Math.min(proposedFlowShift, guaranteedS2SendingFlow);
      if(shouldSnapToAllowS2Removal(proposedFlowShift, guaranteedS2SendingFlow, pas.getReducedCost())){
        //todo: once we have removed the global smoothing of the proposed shift this can be removed here and it is
        // only needed in the final smoothing after local convergence.

        // special case: when we have very little flow left on S2 AND derivatives indicate we should shift it all
        // then avoid the assumed unnecessary smoothing delay to get to zero and allow full move to zero flow
        chosenFlowShift = guaranteedS2SendingFlow;
      }

      /*test for eligibility to reduce to zero flow along S2 */
      if (proposedFlowShift >= guaranteedS2SendingFlow && (isDestinationTrackedForLogging() || logAll)) {
        var message = String.format("     [removal --> final proposed shift %.10f equal or higher than s2 sending flow %.10f, truncate]",
            proposedFlowShift, guaranteedS2SendingFlow);
        //sb.append(message).append(System.lineSeparator());
        LOGGER.info(message);
      }



      if(isDestinationTrackedForLogging() || logAll && proposedFlowShift!=chosenFlowShift) {
        var message = String.format("  Proposed shift: %.10f, Final shift: %.10f",
            proposedFlowShift,chosenFlowShift);
        //sb.append(message).append(System.lineSeparator());
        LOGGER.info(message);
      }

      if(chosenFlowShift <= 0){
        break;
      }

      var iterationS2FlowShiftPerBush = executePasFlowShift(
          guaranteedS2SendingFlow,
          bushS2RemainingSendingFlows,
          chosenFlowShift,
          theMode,
          originalNlConsistentFlowAcceptanceFactors,
          conjStrategy,
          originalNetworkCosts,
          conjSegmentCosts,
          (Set<ConjugateDestinationBush>) bushes,
          updateNetworkAcceptanceFactors,
          logAll);

      // track for smoothing
      double iterationFlowShift = 0;
      if(iterationS2FlowShiftPerBush !=null) {
        iterationFlowShift = -iterationS2FlowShiftPerBush.values().stream().mapToDouble(d -> d).sum();

        // track applied shifts per bush for smoothing
        // todo if we know no smoothing happens we should skip
        boolean negateValues = !pas.getFirstEdgeSegment(false).equals(initialAltTurn);
        iterationS2FlowShiftPerBush.forEach((key, value) -> flowShiftTrackerForInitialAltTurn.put(key,
            flowShiftTrackerForInitialAltTurn.getOrDefault(key, 0.0) +
                (negateValues ? -value : value)));
      }

      totalPasShift += iterationFlowShift;

      // sync costs to changes in flow, to allow for next proposed flow update
      boolean costSwitch = pas.updateCost(conjSegmentCosts);

      if(isDestinationTrackedForLogging() || logAll) {
        var s1Message = "   (after shift) s1 alphas: "+
            Arrays.stream(s1Alternative).filter(ConjugateEdgeSegment::hasOriginalEntryEdgeSegment).map(
                es -> String.format("%s:%.6f",
                    es.getXmlId(), networkLoading.getCurrentFlowAcceptanceFactors()[
                        (int) es.getOriginalAdjacentEdgeSegments().first().getId()])).collect(Collectors.joining(","));
        var s2Message = "   (after shift) s2 alphas: "+
            Arrays.stream(s2Alternative).filter(ConjugateEdgeSegment::hasOriginalEntryEdgeSegment).map(es -> String.format("%s:%.6f",
                es.getXmlId(), networkLoading.getCurrentFlowAcceptanceFactors()[
                    (int) es.getOriginalAdjacentEdgeSegments().first().getId()])).collect(Collectors.joining(","));
        //sb.append(s1Message).append(System.lineSeparator());
        //sb.append(s2Message).append(System.lineSeparator());
        LOGGER.info(s1Message);
        LOGGER.info(s2Message);
        var message = "   (after shift) cost diff: "+pas.getReducedCost();
        //sb.append(message).append(System.lineSeparator());
        LOGGER.info(message);
      }

      s1SendingFlow += iterationFlowShift;
      double s2SendingFlow = guaranteedS2SendingFlow - iterationFlowShift;
      if(costSwitch){
        double prevS1SendingFlow = s1SendingFlow;
        s1SendingFlow = s2SendingFlow;
        s2SendingFlow = prevS1SendingFlow;
        s1Alternative = pas.getAlternative(true);
        s2Alternative = pas.getAlternative(false);
      }

      // normalise to a flow of at least 1 to ensure that low flow PASs do not stop equilibrating as gap is multiplied by very small number
      // despite perhaps a large reduced cost which would hinder convergence, especially if it is heading towards removing low turn flows entirely
      prevPasGap = pasGap;
      pasGap = pas.getReducedCost() * Math.max(1,s2SendingFlow)
          /
          (pas.getAlternativeLowCost() * Math.max(1,(s1SendingFlow + s2SendingFlow)));


      if(costSwitch){
        // stepping over optimal result and in doing so moving farther away from solution --> impose bisection bound
        // halfway since we know it must reside in between the previous state and the new state with the current
        // flow shift applied
        //
        // old state     solution            current state after step
        // |_______________?________________________>|
        //                           ^
        // -  -   -   -   -   -  -   |
        //                           impose bound according to golden ratio
        pas.goldenRatioShiftBound = iterationFlowShift * 0.618;
        if(isDestinationTrackedForLogging() || logAll) {
          var message = String.format("S1/S2 Cost SWITCH  --> GOLDEN RATIO BOUND: %.10f * 0.618=%.10f",
              iterationFlowShift, pas.goldenRatioShiftBound);
          //sb.append(message).append(System.lineSeparator());
          LOGGER.info(message);
        }
      }

      // reuse criterion of gap (overall gap is done wider, so we do not update gap as such here)
      converged = pasGap <= conjStrategy.getGapFunction().getStopCriterion().getEpsilon();
      if(converged && (isDestinationTrackedForLogging() || logAll)) {
        var message = "*********************CONVERGED PAS*************************";
        //sb.append(message).append(System.lineSeparator());
        LOGGER.info(message);
      }
      //converged = pasGap <= conjStrategy.getGapFunction().getGap();
      ++internalIteration;

      doNotStop = !converged && internalIteration <= MAX_INTERAL_ITERATIONS_ALLOWED &&
          (initialFlowShiftBudget - EPSILON_6) > totalPasShift; // stop when we exceeded the budget (only relevant when no updating alphas)

      // remove zero-flow S2 bushes from PAS when we know they won't get used again, or it is the final iteration
      if(!costSwitch || !doNotStop) {
        removeZeroFlowBushesFromPas(false /* no dangling nodes */);
      }
      // todo account for switching so can't jut add everything
      totalCongestedFlowShifted += totalPasShift;
    }while(!converged && doNotStop);

    if(totalPasShift > 0) {

      if(!Double.isNaN(pas.getProposedPasFlowShiftAdjustmentFactor()) &&
          !Double.isInfinite(pas.getProposedPasFlowShiftAdjustmentFactor())){
        additionalSmoothingFactor *= pas.getProposedPasFlowShiftAdjustmentFactor();
      }

      // bush smoothing - final shift if stepsize is reduced
      // re-register all as smoothing will add some flow back in to zero flow alt if shifts occurred.
      // todo: very ugly, we probably should simply not unregister bushes and have a better mechanism in the final shift
      //  to deal with near zero situations if this approach works...
      originalPasBushes.forEach(b -> pas.registerBush((RootedBush<ConjugateDirectedVertex, ConjugateEdgeSegment>) b));
      double finalAppliedFlowShift = performFinalShiftForSmoothing(
          flowShiftTrackerForInitialAltTurn,
          initialAltTurn,
          theMode,
          conjStrategy,
          originalNlConsistentFlowAcceptanceFactors,
          originalNetworkCosts,
          conjSegmentCosts,
          (Set<ConjugateDestinationBush>) bushes,
          logAll,
          updateNetworkAcceptanceFactors,
          additionalSmoothingFactor);

      if (!converged && sb != null) {
        //LOGGER.info(sb.toString());
      }
    }

    return totalPasShift; // incorrect not compensated for with smoothing
  }

  // common code for checking if we should allow bypassing any smoothing to ensure we can still remove
  // S2 alternatives without being hampered by infinetely reduced steps towards a known outcome
  // rationale: if the cost difference is large enough and the s2 flow is sufficiently small we "know" we should
  // remove it despite the network as a whole perhaps having a restricted step size. If we do not do this
  // this will further reduce smoothing and we never get to convergence
  private static boolean shouldSnapToAllowS2Removal(
      double proposedFlowShift, double s2AvailableSendingFlow, double pasCostDiff) {
    return
        s2AvailableSendingFlow < 5 &&
            (proposedFlowShift/2.0) >= s2AvailableSendingFlow &&
            pasCostDiff > EPSILON_6;
  }

  // only exists to have common code between no node model update and node model update evrsion of bush smoothing after
  // internal pas convergence/stoppage
  //
  // provided flow shifts are expected to be for the initial high cost alt segment on pas at the start of shifting
  // (may no longer be the case in the final state now) --> so this should be a negative value for each bush
  private double performFinalShiftForSmoothing(
      Map<ConjugateDestinationBush, Double> appliedNegativeFlowShiftsForInitialTurn,
      ConjugateEdgeSegment referenceAltInitialTurn,
      Mode theMode,
      StaticLtmConjugateBushStrategy conjStrategy,
      double[] originalNlConsistentFlowAcceptanceFactors,
      double[] originalNetworkCosts,
      double[] conjSegmentCosts,
      Set<ConjugateDestinationBush> bushes,
      boolean logAll,
      boolean networkNodeModelUpdate,
      double additionalSmoothingFactor) {

    ConjugateDestinationBush mostRestrictiveSmoothingBush = null;
    double mostRestrictiveSmoothingFactor = Double.MAX_VALUE;

    // we use the bush applied flows directly for our weights as well as identifying the
    // most restricting smoothing to apply
    double totalRemovedFlow = 0;
    for (var entry : appliedNegativeFlowShiftsForInitialTurn.entrySet()) {
      var bush = entry.getKey();
      double removedBushAltFlow = entry.getValue();
      totalRemovedFlow += removedBushAltFlow;

      double stepSize = bush.bushSmoothing.executeRefZero(1);
      if(stepSize < mostRestrictiveSmoothingFactor) {
        mostRestrictiveSmoothingFactor = stepSize;
        mostRestrictiveSmoothingBush = bush;
      }
    }

    if(totalRemovedFlow > 0){
      // this may happen when preceding flow shifts beforehand that did not trigger a node/alpha update somehow caused
      // an inconsistency such that the starting pas costs are not entirely accurate. In which case more flow could be
      // added than removed to reach convergence --> this is still not correct though and points to a potential bug or
      // opportunity for improvement. For now we'll skip the smoothing to keep it simple
      // todo: track down the exact reason in a simple setting and then fix.
      LOGGER.severe(String.format("reference removed flow should not be positive when smoothing, SKIPPING SMOOTHING for pas %d",
          pas.pasId));
      return 0;
    }

    // find the most restricting stepsize across the bushes involved, that will be the step size we use for
    // smoothing overall for the PAS
    double compensatoryS2ShiftToMeetTarget = 0;
    if(mostRestrictiveSmoothingBush!=null){
      if (isDestinationTrackedForLogging() || logAll) {
        //var message = String.format("  SMOOTHING most restrictive bush (%s) - step %.10f * additional %.10f",
        var message = String.format("  SMOOTHING most restrictive bush (%s) - step %.10f (disabled) * additional %.10f",
            mostRestrictiveSmoothingBush.getRootZone().getIdsAsString(),
            mostRestrictiveSmoothingBush.bushSmoothing.executeRefZero(1),
            additionalSmoothingFactor);
        assert additionalSmoothingFactor<=1;
        //sb.append(message).append(System.lineSeparator());
        LOGGER.info(message);
      }

      double totalShiftedFlow = totalRemovedFlow * -1;
      double proposedSmoothedTargetShiftedFlow = totalShiftedFlow * additionalSmoothingFactor;
          //mostRestrictiveSmoothingBush.bushSmoothing.executeRefZero(totalShiftedFlow) * additionalSmoothingFactor;
      if((totalShiftedFlow - proposedSmoothedTargetShiftedFlow) > EPSILON_12) {
        compensatoryS2ShiftToMeetTarget = totalShiftedFlow - proposedSmoothedTargetShiftedFlow;
        // case 1: if current high cost is same as initial alt turn (high cost) it had flow removed to converge,
        //    so we now need to add to smooth, keep compesnation positive as is
        // case 2: otherwise current high cost had flow added as it originally was the low cost (not initial turn),
        //  so we need to remove some for smoothing, negate compensatory flow instead
        boolean negate = referenceAltInitialTurn.equals(pas.getFirstEdgeSegment(true));
        compensatoryS2ShiftToMeetTarget *= negate ? -1 : 1;

        if (isDestinationTrackedForLogging() || logAll) {
          var message = String.format("  SMOOTHING COMP SHIFT - appl shift to conv: %.10f - target: %.10f --> comp s2 shift: %.10f",
              totalShiftedFlow,
              proposedSmoothedTargetShiftedFlow,
              compensatoryS2ShiftToMeetTarget);
          //sb.append(message).append(System.lineSeparator());
          LOGGER.info(message);
        }
      }
    }

    if(compensatoryS2ShiftToMeetTarget == 0) {
      return 0;
    }

    if(isDestinationTrackedForLogging() || logAll) {
      if(networkNodeModelUpdate) {
        var s1Message = "   (before shift) s1 alphas: " +
            Arrays.stream(pas.getAlternative(true)).filter(ConjugateEdgeSegment::hasOriginalEntryEdgeSegment).map(
                es -> String.format("%s:%.6f",
                    es.getXmlId(), conjStrategy.getLoading().getCurrentFlowAcceptanceFactors()[
                        (int) es.getOriginalAdjacentEdgeSegments().first().getId()])).collect(Collectors.joining(","));
        //sb.append(s1Message).append(System.lineSeparator());
        LOGGER.info(s1Message);
        var s2Message = "   (before shift) s2 alphas: " +
            Arrays.stream(pas.getAlternative(false)).filter(ConjugateEdgeSegment::hasOriginalEntryEdgeSegment).map(es -> String.format("%s:%.6f",
                es.getXmlId(), conjStrategy.getLoading().getCurrentFlowAcceptanceFactors()[
                    (int) es.getOriginalAdjacentEdgeSegments().first().getId()])).collect(Collectors.joining(","));
        //sb.append(s2Message).append(System.lineSeparator());
        LOGGER.info(s2Message);
      }
    }

    // negate because we computed it from s2 perspective, but shifts are expected in positive form (s1 perspective)
    double compensatoryShiftToApply = compensatoryS2ShiftToMeetTarget *-1;

    // now we proceed as with a normal shift unless we are allowed to snap which is only allowed if
    // converged PAS has a zero flow alternative that due to smoothing would no longer be zero again
    boolean s1SlatedForAddingSmoothedFlow = compensatoryShiftToApply > 0;
    double remainingSendingFlow = 0;
    for (var bush : pas.getRegisteredBushes()) {
      ConjugateDestinationBush conjBush = (ConjugateDestinationBush) bush;
      if(networkNodeModelUpdate) {
        double remainingSubPathSendingFlow = conjBush.determineConstrainedSubPathSendingFlow(
            pas.getAlternative(s1SlatedForAddingSmoothedFlow),
            conjStrategy.getLoading().getCurrentFlowAcceptanceFactors(),
            originalNlConsistentFlowAcceptanceFactors,
            this.originalBushTurnFlowTracker.get(conjBush));
        remainingSendingFlow += remainingSubPathSendingFlow;
      }else{
        double remainingSubPathSendingFlow = bush.determineSubPathSendingFlow(
            pas.getAlternative(s1SlatedForAddingSmoothedFlow), conjStrategy.getLoading().getCurrentFlowAcceptanceFactors());
        remainingSendingFlow += remainingSubPathSendingFlow;
      }
    }

    // check if given the state of the PAS we should just ignore the smoothing and snap to the converged result
    // the relevant alternative has no more flow after stopping due to lack of flow (remaining flow = 0) + it had very
    // little flow to start with (original sending flow = total removed flow), and there is still a (significant) cost
    // gap even after removal
    // todo: we do not know here what the proposed original flow shift was, to avoid that disallowing the snapping we
    //  set it to infinite --> again eventually this call should only be made here and there we should improve this as it is ugly
    if(remainingSendingFlow <= 0 &&
        shouldSnapToAllowS2Removal(Double.MAX_VALUE, Math.abs(totalRemovedFlow), pas.getReducedCost())){
      if(isDestinationTrackedForLogging() || logAll) {
        var message = "   Skip smoothing - snapping to zero allowed!";
        LOGGER.info(message);
      }
      return 0;
    }

    executePasFlowShift(
        totalRemovedFlow,
        appliedNegativeFlowShiftsForInitialTurn,
        compensatoryShiftToApply,
        theMode,
        originalNlConsistentFlowAcceptanceFactors,
        conjStrategy,
        originalNetworkCosts,
        conjSegmentCosts,
        bushes,
        networkNodeModelUpdate,
        logAll);

    pas.updateCost(conjSegmentCosts);
    if(isDestinationTrackedForLogging() || logAll) {
      if(networkNodeModelUpdate) {
        var s1Message = "   (after shift) s1 alphas: " +
            Arrays.stream(pas.getAlternative(true)).filter(ConjugateEdgeSegment::hasOriginalEntryEdgeSegment).map(
                es -> String.format("%s:%.6f",
                    es.getXmlId(), conjStrategy.getLoading().getCurrentFlowAcceptanceFactors()[
                        (int) es.getOriginalAdjacentEdgeSegments().first().getId()])).collect(Collectors.joining(","));
        var s2Message = "   (after shift) s2 alphas: " +
            Arrays.stream(pas.getAlternative(false)).filter(ConjugateEdgeSegment::hasOriginalEntryEdgeSegment).map(es -> String.format("%s:%.6f",
                es.getXmlId(), conjStrategy.getLoading().getCurrentFlowAcceptanceFactors()[
                    (int) es.getOriginalAdjacentEdgeSegments().first().getId()])).collect(Collectors.joining(","));
        //sb.append(s1Message).append(System.lineSeparator());
        //sb.append(s2Message).append(System.lineSeparator());
        LOGGER.info(s1Message);
        LOGGER.info(s2Message);
      }
      var message = "   (after shift) cost diff: "+pas.getReducedCost();
      //sb.append(message).append(System.lineSeparator());
      LOGGER.info(message);
    }
    return 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void performAllBushesRecordedOneShotS1FlowShift(
      Mode theMode,
      StaticLtmAssignmentStrategy assignmentStrategy) {
    // not used any more in new setup
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getS2SendingFlow() {
    return s1S2SendingFlows.second();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getS1SendingFlow() {
    return s1S2SendingFlows.first();
  }


}

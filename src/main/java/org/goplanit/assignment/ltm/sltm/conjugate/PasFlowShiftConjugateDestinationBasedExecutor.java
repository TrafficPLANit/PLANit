package org.goplanit.assignment.ltm.sltm.conjugate;

import org.goplanit.assignment.ltm.sltm.*;
import org.goplanit.assignment.ltm.sltm.consumer.NMRCollectEntryLinkFlowAcceptanceFactorConsumer;
import org.goplanit.assignment.ltm.sltm.loading.NetworkLoadingSplittingRateDataPartial;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingBushBase;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingBushConjugate;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmNetworkLoading;
import org.goplanit.cost.physical.AbstractPhysicalCost;
import org.goplanit.cost.virtual.AbstractVirtualCost;
import org.goplanit.gap.GapFunction;
import org.goplanit.sdinteraction.smoothing.Smoothing;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.virtual.physical.ConnectoidSegment;
import org.goplanit.utils.pcu.PcuCapacitated;
import org.ojalgo.array.Array1D;
import org.ojalgo.function.PrimitiveFunction;
import org.ojalgo.function.aggregator.Aggregator;

import java.util.*;
import java.util.function.Consumer;
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

  private void syncUncongestedPasFlowShiftToNetworkFlow(
      StaticLtmLoadingBushBase<ConjugateDestinationBush> networkLoading, double totalPasShift) {

    for(var es : pas.getAlternative(true)){
      var originalSegments = es.getOriginalAdjacentEdgeSegments();
      if(originalSegments.firstNotNull()){
        int id = (int) (originalSegments.first()).getId();
        networkLoading.getUnconstrainedFlowsPcuHour()[id] += totalPasShift;
        networkLoading.getCurrentOutflowsPcuH()[id] += totalPasShift;
        networkLoading.getCurrentInflowsPcuH()[id] += totalPasShift;
      }
    }

    for(var es : pas.getAlternative(false)) {
      var originalSegments = es.getOriginalAdjacentEdgeSegments();
      if(originalSegments.firstNotNull()){
        int id = (int) (originalSegments.first()).getId();
        networkLoading.getUnconstrainedFlowsPcuHour()[id] -= totalPasShift;
        networkLoading.getCurrentOutflowsPcuH()[id] -= totalPasShift;
        networkLoading.getCurrentInflowsPcuH()[id] -= totalPasShift;
      }
    }
  }

  private void updateOriginalAndConjugateNetworkCostsToCurrentPasFlows(
      Mode theMode,
      StaticLtmLoadingBushBase<ConjugateDestinationBush> networkLoading,
      AbstractPhysicalCost physicalCost,
      AbstractVirtualCost virtualCost,
      double[] originalNetworkCosts,
      double[] conjSegmentCosts) {

    // for steady state travel time we technically do not need to update the original network cost because it is not
    // used anywhere during the calculation of derivatives nor flow shift amount. We do it for consistency for now
    // can potentially be removed if problematic (for example with persisting since it will be inconsistent with the
    // loading of the iteration

    Consumer<ConjugateEdgeSegment> syncNetworkCost = es -> {
      if(!es.hasOriginalEntryEdgeSegment()){
        return;
      }
      var originalEntrySegment = es.getOriginalAdjacentEdgeSegments().first();
      double currentCost;
      if(originalEntrySegment instanceof MacroscopicLinkSegment) {
        // will use current network flows (including any shift applied via syncUncongestedPasFlowShiftToNetworkFlow
        currentCost = physicalCost.getGeneralisedCost(theMode, (MacroscopicLinkSegment) originalEntrySegment);
      }else{
        currentCost = virtualCost.getGeneralisedCost(theMode, (ConnectoidSegment) originalEntrySegment);
      }
      originalNetworkCosts[(int)originalEntrySegment.getId()]  = currentCost;
      conjSegmentCosts[(int) es.getId()] = currentCost;
    };

    pas.forEachEdgeSegment(true, syncNetworkCost);
    pas.forEachEdgeSegment(false, syncNetworkCost);

    // ignore last exit because it is shared and amount of flow added, is same as the amount being removed
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
      anyRemoved = anyRemoved || bush.removeZeroFlowSegmentsIn(
          pas.getAlternative(false), allowDanglingNodes, isDestinationTrackedForLogging());
      if(anyRemoved){
        if(isDestinationTrackedForLogging(bush)){
          LOGGER.info(String.format("   [Unregistering bush (%s) from PAS %s, no more S2 flow left]",
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
  private double executeTurnFlowShift(
          ConjugateDestinationBush conjBush, ConjugateEdgeSegment conjSegment, double flowShiftPcuH) {
    return conjBush.addTurnSendingFlow(conjSegment, flowShiftPcuH);
  }

  /**
   * Execute a flow shift on a PAS alternative for a given bush
   *
   * @param conjBush to use
   * @param bushPasFlowShiftPcuH flow to shift
   * @param pasAlternative to apply to
   * @param networkLoading to use
   * @param bushes to use (only needed when updating node model splitting rates)
   * @param updateNetworkNodeModel when tru update node model on the fly
   * @return final flow shift applied on last segment of alternative
   */
  private double executeBushPasFlowShift(
          ConjugateDestinationBush conjBush,
          double bushPasFlowShiftPcuH,
          ConjugateEdgeSegment[] pasAlternative,
          StaticLtmLoadingBushBase<?> networkLoading,
          Set<ConjugateDestinationBush> bushes,
          boolean updateNetworkNodeModel) {

    var nonConjugateFlowAcceptanceFactors = networkLoading.getCurrentFlowAcceptanceFactors();

    int index = 0;
    double flowShiftPcuH = bushPasFlowShiftPcuH;
    ConjugateEdgeSegment currentConjSegment;
    boolean restrictToOutflowUpdateOnly = false;
    while (index < pasAlternative.length) {
      currentConjSegment = pasAlternative[index++];

      // SPECIAL-CASE potentially triggered by see below
      if(restrictToOutflowUpdateOnly){
        // no more flow shifts, but if any outflow is not yet populated for upcoming cost calculation update - set it
        if(currentConjSegment.hasOriginalEntryEdgeSegment()) {
          var originalTurnEntrySegmentIndex = (int) currentConjSegment.getOriginalAdjacentEdgeSegments().first().getId();
          if (networkLoading.getCurrentOutflowsPcuH()[originalTurnEntrySegmentIndex] <= 0) {
            networkLoading.getCurrentOutflowsPcuH()[originalTurnEntrySegmentIndex] =
                networkLoading.getCurrentInflowsPcuH()[originalTurnEntrySegmentIndex] *
                    networkLoading.getCurrentFlowAcceptanceFactors()[originalTurnEntrySegmentIndex];
          }
        }
        continue;
      }

      double currentFlow = conjBush.getTurnSendingFlow(currentConjSegment);
      if(currentFlow + flowShiftPcuH < 0){
        flowShiftPcuH = -currentFlow; // sync to available flow
      }
      double newFlow = executeTurnFlowShift(conjBush, currentConjSegment, flowShiftPcuH);
      double appliedFlowShift = newFlow - currentFlow;
      if(Precision.notEqual(Math.abs(appliedFlowShift), Math.abs(flowShiftPcuH))){
        double diff= currentFlow + flowShiftPcuH;
        flowShiftPcuH = appliedFlowShift;
        LOGGER.severe("sync shouldn't trigger");
      }

      // we only do this if there is a chance of the alphas changing (so potentially congested)
      if(updateNetworkNodeModel){
        double acceptanceFactorBefore = 1;

        EdgeSegment originalTurnEntrySegment = null;
        if(currentConjSegment.hasOriginalEntryEdgeSegment()) {
          originalTurnEntrySegment = currentConjSegment.getOriginalAdjacentEdgeSegments().first();
          acceptanceFactorBefore = nonConjugateFlowAcceptanceFactors[(int) originalTurnEntrySegment.getId()];
        }

        // sync network inflows/unconstrained flows/sendng flows, splitting rates, and alphas via network node model update <-- differs from uncongested
        // todo: when doing uncongested update we do this outside this loop, should also move here, except for the node
        //  model and splitting rate update which is not necessary in that case to save time

        // todo: now done per bush which is very inefficient AND incorrect under multiple bushes per PAS -->
        //  instead do per link of alternative and then per bush below to only do one node model update instead of #pas-bushes updates
        executeNetworkTurnFlowShiftWithNodeModelUpdate(flowShiftPcuH, currentConjSegment, networkLoading, bushes);
        double acceptanceFactorAfter = 1;
        if(currentConjSegment.hasOriginalEntryEdgeSegment()) {
          acceptanceFactorAfter = nonConjugateFlowAcceptanceFactors[(int) originalTurnEntrySegment.getId()];
        }

        // adjust flow shift
        // case 1: no change in factor -> proceed with same flow shift and propagate further
        // case 2: change in factor but no change in outflow      - stop flow shift propagation since traffic
        //    withholding makes that no downstream flow shift exists (it is all removed from the withheld traffic)
        // case 3: change in factor and change in outflow         - determine non-withheld change in flow, namely
        //    the new outflow - old outflow

        if(acceptanceFactorBefore != acceptanceFactorAfter){
          double outflowBefore = currentFlow * acceptanceFactorBefore;
          double outflowAfter = newFlow * acceptanceFactorAfter;
          if(Precision.equal(outflowBefore, outflowAfter, EPSILON_6)){
            // case 2: nothing left, all consumed by the change in withheld flow
            flowShiftPcuH = 0;
            // we still need to make sure all outflows are present for cost calculation. switch to outflowsyncing only
            restrictToOutflowUpdateOnly = true;
          }else{
            // case 3: we propagate the remaining difference that is not consumed by removing the previously withheld flow
            flowShiftPcuH = flowShiftPcuH>0 ?
                Math.min(flowShiftPcuH, outflowAfter - outflowBefore):
                Math.max(flowShiftPcuH, outflowAfter - outflowBefore);
          }
        }

      }
    }
    return flowShiftPcuH;
  }

//  // network version to obtain new alphas BEFORE we update bush flows using the new alphas.
//  // updated:
//  //  inflows
//  //  unconstrained inflows
//  //  outflows
//  //  acceptance factors
//  // Not updates:
//  //  splitting rates are not updated as this needs to go through bushes, but the networ level splitting rates
//  //  are used to do the node model update
//  private void executeNetworkFlowShiftWithNodeModelUpdate(
//      double flowShiftToApply,
//      ConjugateEdgeSegment[] pasAlternative,
//      StaticLtmLoadingBushBase<?> networkLoading,
//      Set<ConjugateDestinationBush> bushes) {
//
//    int index = 0;
//    double currentFlowShift = flowShiftToApply;
//
//    ConjugateEdgeSegment currentConjSegment;
//    while (index < pasAlternative.length) {
//      currentConjSegment = pasAlternative[index++];
//
//      var originalSegment = currentConjSegment.getOriginalAdjacentEdgeSegments().first();
//      if(originalSegment != null){
//        double newFlowAcceptanceFactor =
//            executeNetworkFlowShiftWithNodeModelUpdate(currentFlowShift, currentConjSegment, networkLoading, bushes);
//
//        // adjust flow shift using the new acceptance factor
//        currentFlowShift *= newFlowAcceptanceFactor;
//      }
//    }
//  }

  private double executeNetworkTurnFlowShiftWithNodeModelUpdate(
      double flowShiftToApply,
      ConjugateEdgeSegment pasAlternativeSegment,
      StaticLtmLoadingBushBase<?> networkLoading,
      Set<ConjugateDestinationBush> bushes) {

    var unconstrainedFlows = networkLoading.getUnconstrainedFlowsPcuHour();
    var constrainedFlows = networkLoading.getCurrentInflowsPcuH();
    // node model update uses sending flows rather than inflows todo: see if we can generalise this because it is ugly
    // so we need to sync those as well
    var sendingFlows = networkLoading.getCurrentSendingFlowsPcuH();
    var splittingRates = networkLoading.getSplittingRateData();

    var originalSegment = pasAlternativeSegment.getOriginalAdjacentEdgeSegments().first();
    if(originalSegment != null) {
      int segmentIndex = (int) originalSegment.getId();

      // network level constrained inflow/sending flow update
      double currentConstrainedFlow = constrainedFlows[segmentIndex];
      double newConstrainedFlow = Math.max(currentConstrainedFlow + flowShiftToApply, 0);
      constrainedFlows[segmentIndex] = newConstrainedFlow;
      sendingFlows[segmentIndex] = newConstrainedFlow;

      // network level unconstrained flow update
      double currentUnconstrainedFlow = unconstrainedFlows[segmentIndex];
      double newUnconstrainedFlow = Math.max(currentUnconstrainedFlow + flowShiftToApply, 0);
      unconstrainedFlows[segmentIndex] = newUnconstrainedFlow;


      // network level splitting rates
      executeNetworkSplittingRateUpdateForPasAlternativeSegment(
          pasAlternativeSegment, bushes, (StaticLtmLoadingBushConjugate) networkLoading);
      if(flowShiftToApply > 0 &&
          networkLoading.getSplittingRateData() instanceof NetworkLoadingSplittingRateDataPartial){
        // any additional flow to a turn may potentially cause congestion. to ensure node model calculates such a node
        // it must be registered as potentially blocking. So we register it at such.
        ((NetworkLoadingSplittingRateDataPartial)networkLoading.getSplittingRateData()).registerPotentiallyBlockingNode(
            originalSegment.getDownstreamVertex());
      }

      var consumer = new NMRCollectEntryLinkFlowAcceptanceFactorConsumer(originalSegment);
      StaticLtmNetworkLoading.performNodeModelUpdate(
          originalSegment.getDownstreamVertex(), consumer, networkLoading, constrainedFlows);
      var newFlowAcceptanceFactor = consumer.getEntrySegmentFlowAcceptanceFactor();

      // network level alpha update
      networkLoading.getCurrentFlowAcceptanceFactors()[segmentIndex] = newFlowAcceptanceFactor;

      // network level outflow update
      networkLoading.getCurrentOutflowsPcuH()[segmentIndex] = newConstrainedFlow * newFlowAcceptanceFactor;
      return newFlowAcceptanceFactor;
    }
    return 1;
  }

  // given an original entry segment and turn flows, update to splitting rates on network level
  private void updateOriginalEntrySegmentSplittingRate(
      EdgeSegment originalEntrySegment, Map<EdgeSegment, Double> exitSegmentFlowsToConvertToSplittingRates,
      StaticLtmLoadingBushConjugate networkLoading) {

    // now convert to splitting rates
    var entrySegmentSplittingRates = networkLoading.getSplittingRateData().getSplittingRates(originalEntrySegment);
    if(entrySegmentSplittingRates==null){
      entrySegmentSplittingRates =
          Array1D.PRIMITIVE64.makeZero(originalEntrySegment.getDownstreamVertex().getNumberOfExitEdgeSegments());
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
  private void executeNetworkSplittingRateUpdateForPasAlternativeSegment(
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

        if (!conjSegmentWithSharedEntry.getOriginalAdjacentEdgeSegments().first().idEquals(originalEntrySegment)) {
          // fail safe in case conjugate network would not introduce nodes per link segment but per link. Should
          // never trigger currently
          LOGGER.severe("conjugate network nodes should never have exit links with different original entry links," +
              "unless something changed in conjugate network construction!!");
          continue;
        }

        // original network entry --> add (exit link, turn flow) to map
        var originalExitLink = conjSegmentWithSharedEntry.getOriginalAdjacentEdgeSegments().second();
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
  protected Pair<ConjugateEdgeSegment, Boolean> findFirstCongestedEdgeSegmentOnPasAlternative(
          StaticLtmLoadingBushBase<?> networkLoading, boolean lowCost, boolean ignoreInitialSegment) {

    ConjugateEdgeSegment[] alternative = pas.getAlternative(lowCost);
    ConjugateEdgeSegment currConjSegment = null;
    int index = ignoreInitialSegment ? 1 : 0;
    for (; index < alternative.length; ++index) {
      currConjSegment = alternative[index];

      // use the original network loading segments to determine if the conjugate segment (turn) is considered
      // congested or not
      if(!currConjSegment.hasOriginalEntryEdgeSegment()){
        continue;
      }
      if (currConjSegment.hasOriginalEntryEdgeSegment() &&
              isCongested(networkLoading , currConjSegment.getOriginalAdjacentEdgeSegments().first())) {
        return Pair.of(currConjSegment, true);
      }else if(currConjSegment.hasOriginalExitEdgeSegment() &&
              isNearCongested(
                      networkLoading,
                      currConjSegment.getOriginalAdjacentEdgeSegments().second(),
                      UNCONGESTED_AS_CONGESTED_FLOW_THRESHOLD_PCUH)){
        //todo: once we have zero flow cost derivatives we should be able to get rid of this as it is ugly
        return Pair.of(currConjSegment, false);
      }
    }
    if(currConjSegment==null){
      return null;
    }

    // for last segment consider all exit segments out of the PAS rather as we have no
    // single next segment
    if(!currConjSegment.hasOriginalExitEdgeSegment()){
      return null;
    }
    //todo: once we have zero flow cost derivatives we should be able to get rid of this as it is ugly
    var isCongestedResult =
            isCongested(networkLoading,
                    currConjSegment.getOriginalAdjacentEdgeSegments().first(),
                    UNCONGESTED_AS_CONGESTED_FLOW_THRESHOLD_PCUH);
    if(isCongestedResult.first()){
      return Pair.of(currConjSegment, true); // true congestion match
    }else if(isCongestedResult.second()){
      return Pair.of(currConjSegment, false); // near congestion match
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected double getDTravelTimeDFlow(
          final Mode theMode,
          final StaticLtmLoadingBushBase<?> networkLoading,
          final AbstractPhysicalCost physicalCost,
          final AbstractVirtualCost virtualCost,
          boolean isLowCostAlternative,
          boolean ignoreFirstSegment) {
    double dTravelTimeDFlow = 0.0;

    var pasAlternative = this.pas.getAlternative(isLowCostAlternative);
    var otherPasAlternative = this.pas.getAlternative(!isLowCostAlternative);

    int index = ignoreFirstSegment? 1 : 0;
    while(index < pasAlternative.length){
      boolean isDiverge = (index == 0);
      boolean isMerge = (index == (pasAlternative.length-1));

      ConjugateEdgeSegment currSegment = pasAlternative[index++];
      if(!currSegment.hasOriginalEntryEdgeSegment()){
        continue;
      }
      EdgeSegment originalEntrySegment = currSegment.getOriginalAdjacentEdgeSegments().first();
      EdgeSegment originalExitSegment = currSegment.getOriginalAdjacentEdgeSegments().second();


      boolean unCongested = !isCongested(networkLoading, originalEntrySegment);
      EdgeSegment mostRestrictingExit = null;
      if(!unCongested){
        mostRestrictingExit = identifyMostRestrictingOutEdgeSegment(originalEntrySegment, networkLoading); // todo should be done once and cached
      }else if(isMerge && isLowCostAlternative){
        // MERGE (part I)
        // special case: when processing s1 and S2 is congested and most restricted by exit link at merge, then s1 turn
        // is expected to also become congested (if not already). So treat it as such to avoid overshooting
        // todo: to be replaced by analytical version  in due time
        var otherAltMergeEntry = otherPasAlternative[otherPasAlternative.length -1].getOriginalAdjacentEdgeSegments().first();
        if(otherAltMergeEntry!=null && isCongested(networkLoading, otherAltMergeEntry)) {
          unCongested = false; // treat s1 as uncongested as well
          mostRestrictingExit = identifyMostRestrictingOutEdgeSegment(otherAltMergeEntry, networkLoading); // todo should be done once and cached
        }
      }

      double currDTravelTimeDFlow = 0.0;
      if (originalEntrySegment instanceof MacroscopicLinkSegment) {
        currDTravelTimeDFlow =
                physicalCost.getDTravelTimeDFlow(unCongested, theMode, (MacroscopicLinkSegment) originalEntrySegment);
      } else if (originalEntrySegment instanceof ConnectoidSegment) {
        currDTravelTimeDFlow =
                virtualCost.getDTravelTimeDFlow(unCongested, theMode, (ConnectoidSegment) originalEntrySegment);
      } else {
        LOGGER.severe(String.format("Unsupported edge segment (%s) to obtain derivative of cost towards flow from",
                originalEntrySegment.getIdsAsString()));
      }

      //TODO: below three cases written out, eventually they should be consolidated as they are largely the same
      // WRONG TO SET TO ZERO --> WE SHOULD SET THEM TO HYPO DERIVATIVE IN THOSE CASES!!! --> compute hypo derivative
      // separately and use that instead of zero...

      // DIVERGE (congested entry link)
      // - case 1: shifting from most-restrictive to not most restrictive --> not most restrictive gets zero
      //      todo for case 1: if turn is going towards other congested out link that is less restrictive, we should be using that deravative instead of zero
      // - case 2: shifting from most-restrictive to not most restrictive --> most restrictive gets derivative as is
      // - case 3: shifting from non-most-restrictive to non-most restrictive --> set derivative to 0
      boolean thisAltOnMostRestrictingTurn = (mostRestrictingExit == originalExitSegment);
      if(isDiverge && !unCongested && !thisAltOnMostRestrictingTurn){
          // case 1 and case 3
          currDTravelTimeDFlow = 0;
      } // case 2 no action needed

      // ON PAS (congested entry link)
      // - case 1: flow not on most-restrictive turn --> set derivative to zero
      //      todo for case 1: if turn is going towards other congested out link that is less restrictive, we should be using that deravative instead of zero
      // - case 2: flow on most-restrictive turn --> derivative as is
      if(!isDiverge && !isMerge && !unCongested && !thisAltOnMostRestrictingTurn){
        currDTravelTimeDFlow = 0; // case 1
      } // case 2 no action needed

      // MERGE
      // - case 1: flow not on most-restrictive turn --> set derivative to zero
      //      todo for case 1: if turn is going towards other congested out link that is less restrictive, we should be using that deravative instead of zero
      // - case 2: flow on most-restrictive turn --> derivative as is
      if(isMerge && !unCongested && !thisAltOnMostRestrictingTurn){
        currDTravelTimeDFlow = 0; // case 1
      }


      dTravelTimeDFlow += currDTravelTimeDFlow;

      if(!unCongested && thisAltOnMostRestrictingTurn){
        // no more flow change beyond here due to it being a bottleneck
        break;
      }
    }
    return dTravelTimeDFlow;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Pair<Double,EdgeSegment> determinePasAlternativeSlackFlow(
          StaticLtmLoadingBushBase<?> networkLoading, boolean lowCost, boolean ignoreInitialSegment) {

    double slackFlow = Double.POSITIVE_INFINITY;
    EdgeSegment minSlackSegment = null;

    // regular PAS traversal rework back to original link segments
    int linkSegmentId = -1;
    ConjugateEdgeSegment conjAltEdgeSegment = null;
    ConjugateEdgeSegment[] conjAltEdgeSegments = pas.getAlternative(lowCost);
    int index = ignoreInitialSegment ? 0 : 1;
    for (; index < conjAltEdgeSegments.length; ++index) {
      conjAltEdgeSegment = conjAltEdgeSegments[index];
      if(!conjAltEdgeSegment.hasOriginalEntryEdgeSegment()){
        continue;
      }
      EdgeSegment originalNetworkSegment = conjAltEdgeSegment.getOriginalAdjacentEdgeSegments().first();
      linkSegmentId = (int) originalNetworkSegment.getId();
      /* do not use outflows directly because they are only available on potentially blocking nodes in point queue basic solution scheme */
      double inflow = networkLoading.getCurrentInflowsPcuH()[linkSegmentId];
      double currSlackFlow = ((PcuCapacitated) originalNetworkSegment).getCapacityOrDefaultPcuH() - inflow;
      if(currSlackFlow < slackFlow){
        minSlackSegment = originalNetworkSegment;
        slackFlow = currSlackFlow;
      }

      if (!Precision.positive(slackFlow, EPSILON)) {
        return Pair.of(0.0, minSlackSegment);
      }
    }

    // beyond last segment special case tracking
    ConjugateEdgeSegment lastAlternativeSegment = pas.getLastEdgeSegment(lowCost);
    if(!lastAlternativeSegment.hasOriginalExitEdgeSegment()){
      return Pair.of(slackFlow, minSlackSegment);
    }

    /* do not use outflows directly because they are only available on potentially blocking nodes in point queue basic
    solution scheme todo: should be fixed the below is ugly */
    EdgeSegment lastOriginalSegment = lastAlternativeSegment.getOriginalAdjacentEdgeSegments().first();
    EdgeSegment lastOriginalSegmentExit = lastAlternativeSegment.getOriginalAdjacentEdgeSegments().second();
    double splittingRate =
            networkLoading.getSplittingRateData().getSplittingRate(lastOriginalSegment, lastOriginalSegmentExit);
    var nextInflow = networkLoading.getCurrentInflowsPcuH()[(int)lastOriginalSegment.getId()] * splittingRate;
    double currSlackFlow = ((PcuCapacitated) lastOriginalSegmentExit).getCapacityOrDefaultPcuH() - nextInflow;
    if(currSlackFlow < slackFlow){
      minSlackSegment = lastOriginalSegmentExit;
      slackFlow = currSlackFlow;
    }
    return Pair.of(slackFlow, minSlackSegment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected double[] executeBushS2FlowShiftNoNodeModelUpdate(
          RootedBush<ConjugateDirectedVertex, ConjugateEdgeSegment> bush,
          EdgeSegment entrySegment,
          double bushEntrySegmentFlowShift,
          StaticLtmLoadingBushBase<?> networkLoading) {

    ConjugateDestinationBush conjBush = (ConjugateDestinationBush)bush;

    /* prep - pas */
    final var s2 = pas.getAlternative(false);
    double flowShiftPcuH = -bushEntrySegmentFlowShift;

    flowShiftPcuH = executeBushPasFlowShift(
        conjBush, flowShiftPcuH, s2, networkLoading, null, false);

    /*end splitting rates not required since we do not shift flow beyond end merge via its turn in conjugate form  */
    //todo: remove return value when we no longer have non-conjugate form for this
    return null;
  }

  protected double[] executeBushS2FlowShiftNodeModelUpdate(
      RootedBush<ConjugateDirectedVertex, ConjugateEdgeSegment> bush,
      double bushEntrySegmentFlowShift,
      StaticLtmLoadingBushBase<?> networkLoading,
      Set<ConjugateDestinationBush> bushes) {

    ConjugateDestinationBush conjBush = (ConjugateDestinationBush)bush;

    /* prep - pas */
    final var s2 = pas.getAlternative(false);
    double flowShiftPcuH = -bushEntrySegmentFlowShift;

    flowShiftPcuH = executeBushPasFlowShift(
        conjBush, flowShiftPcuH, s2, networkLoading, bushes, true);

    /*end splitting rates not required since we do not shift flow beyond end merge via its turn in conjugate form  */
    //todo: remove return value when we no longer have non-conjugate form for this
    return null;
  }

//  protected void executeNetworkS2FlowShiftNodeModelUpdate(
//      double flowShift, StaticLtmLoadingBushBase<?> networkLoading,
//      Set<ConjugateDestinationBush> bushes) {
//    final var s2 = pas.getAlternative(false);
//    executeNetworkFlowShiftWithNodeModelUpdate(-flowShift, s2, networkLoading, bushes);
//  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeBushS1FlowShiftNoNodeModelUpdate(
          RootedBush<ConjugateDirectedVertex, ConjugateEdgeSegment> bush,
          EdgeSegment dummyEntrySegment,                    // not relevant in conjugate form
          double bushEntrySegmentFlowShift,
          StaticLtmLoadingBushBase<?> networkLoading,
          double[] unusedEndMergeSplittingRates) {         // not relevant in conjugate form

    ConjugateDestinationBush conjBush = (ConjugateDestinationBush) bush;

    var s1 = pas.getAlternative(true);

    double s1FinalLabeledFlowShift = executeBushPasFlowShift(
          conjBush,
          bushEntrySegmentFlowShift,
          s1,
          networkLoading,
          null,
          false);
  }

//  protected void executeNetworkS1FlowShiftNodeModelUpdate(
//      double flowShift, StaticLtmLoadingBushBase<?> networkLoading, Set<ConjugateDestinationBush> bushes) {
//    final var s1 = pas.getAlternative(true);
//    executeNetworkFlowShiftWithNodeModelUpdate(flowShift, s1, networkLoading, bushes);
//  }

  protected void executeBushS1FlowShiftNodeModelUpdate(
      RootedBush<ConjugateDirectedVertex, ConjugateEdgeSegment> bush,
      double bushEntrySegmentFlowShift,
      StaticLtmLoadingBushBase<?> networkLoading,
      Set<ConjugateDestinationBush> bushes) {

    ConjugateDestinationBush conjBush = (ConjugateDestinationBush) bush;

    var s1 = pas.getAlternative(true);

    double s1FinalLabeledFlowShift = executeBushPasFlowShift(
        conjBush,
        bushEntrySegmentFlowShift,
        s1,
        networkLoading,
        bushes,
        true);
  }

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
   * {@inheritDoc}
   */
  @Override
  public Map<EdgeSegment, Double> determineProposedFlowShiftByLoadingEntrySegment(
          Mode theMode,
          AbstractPhysicalCost physicalCost,
          AbstractVirtualCost virtualCost,
          StaticLtmLoadingBushBase<?> networkLoading,
          double guaranteedS2SendingFlow) {
    // 1% of capacity is accepted as leeway for state change undicing flow shifts
    double stateChangeLeewayPercentage = 0.01;

    // todo: once we no longer have non-conjugate implementation remove any entry segment based tracking of flow shifts
    Map<EdgeSegment, Double> result = new TreeMap<>();
    var originalEntrySegment =
        pas.getFirstEdgeSegment(false).getOriginalAdjacentEdgeSegments().first();

    double denominatorS2 = 0;
    double denominatorS1 = 0;

    boolean ignoreInitialConjEdgeSegment = false;
    var s1FirstCongestedSegmentResult =
            findFirstCongestedEdgeSegmentOnPasAlternative(networkLoading, true, ignoreInitialConjEdgeSegment);
    var s2FirstCongestedSegmentResult =
            findFirstCongestedEdgeSegmentOnPasAlternative(networkLoading, false, ignoreInitialConjEdgeSegment);
    var firstS1CongestedSegment = s1FirstCongestedSegmentResult!= null ? s1FirstCongestedSegmentResult.first() : null;
    var firstS2CongestedSegment = s2FirstCongestedSegmentResult!= null ? s2FirstCongestedSegmentResult.first() : null;

    denominatorS1 = getDTravelTimeDFlow(
            theMode, networkLoading, physicalCost, virtualCost, true, ignoreInitialConjEdgeSegment);
    denominatorS2 = getDTravelTimeDFlow(
            theMode, networkLoading, physicalCost, virtualCost, false, ignoreInitialConjEdgeSegment);

    double flowShift = 0;
    boolean pasCostEqual = pas.isCostEqual(EPSILON);

    var lowCostSlackResult = determinePasAlternativeSlackFlow(
            networkLoading, true, ignoreInitialConjEdgeSegment);
    double s1SlackFlowEstimate = lowCostSlackResult.first();
    double s1SlackFlowLeeway = ((PcuCapacitated) lowCostSlackResult.second()).getCapacityOrDefaultPcuH() * stateChangeLeewayPercentage;
    if (!pasCostEqual && smallerEqual(denominatorS2,EPSILON,EPSILON) && smallerEqual(denominatorS1, EPSILON,EPSILON)) {
      /* s1 & S2 UNCONGESTED - no derivative estimate possible (denominator zero) */
      /* move all towards cheaper alternative limited by slack + delta */
      /* obtain PAS-entry segment sub-path sending flows */
      double proposedFlowShift = guaranteedS2SendingFlow;
      double finalProposedShift =
              adjustFlowShiftBasedOnS1SlackFlow(proposedFlowShift, s1SlackFlowEstimate, s1SlackFlowLeeway);
      if(originalEntrySegment != null) {
        result.put(originalEntrySegment, finalProposedShift);
      }else{
        // use dummy since entry segment is not used in conjugate anyway, but it can't be null while
        // for conjugate connector turn there may be no original
        result.put(pas.getFirstEdgeSegment(true), finalProposedShift);
      }
      return result;
    }

    /* s1 and/or s2 congested - derivative based flow shift possible */
    // tauw_s1 + dtauw_s1/ds_1 * (-flowShift) = tauw_s2 + dtauw_s2/ds_2 * (flowShift) we find:
    // flowShift = (tauw_s2-tauw_s1)/(1/v_s1_first_bottleneck + 1/v_s2_first_bottleneck))
    double denominator = denominatorS2 + denominatorS1;
    double numerator = pas.getAlternativeHighCost() - pas.getAlternativeLowCost();
    if (numerator != 0) {
      flowShift = numerator / denominator;

      /* debug only, test if shift solves travel time discrepancy, to be removed when it works */
      double diff =
              (pas.getAlternativeLowCost() + denominatorS1 * flowShift) -
                      (pas.getAlternativeHighCost() + denominatorS2 * -flowShift);
      if (Precision.notEqual(diff, 0.0)) {
        LOGGER.severe("Computation of using derivatives to shift flows between PAS segments does not result in " +
                "equal travel time after shift, this should not happen");
      }
    }

    // VERIFY CROSSING OF DISCONTINUITY on S1  - adjust shift if so to mitigate effect
    // This is triggered when S1 alternative segments are not congested yet, or near congestion
    boolean pasS1PotentialDiscontinuity = (firstS1CongestedSegment == null || !s1FirstCongestedSegmentResult.second());
    if (pasS1PotentialDiscontinuity) {
      /* possible triggering of congestion on s1 due to shift -> passing discontinuity on travel time function */
      flowShift = adjustFlowShiftBasedOnS1SlackFlow(flowShift, s1SlackFlowEstimate, s1SlackFlowLeeway);
    }

    // VERIFY CROSSING OF DISCONTINUITY on S2 travel time function - adjust shift if so, to mitigate effect
    // Now we do consider initial segment because we are looking at crossing a discontinuity, not how much flow to
    // change
    Pair<Boolean,Boolean> pasEntrySegmentCongestedResult;
    if(originalEntrySegment!=null) {
      pasEntrySegmentCongestedResult = isCongested(
              networkLoading, originalEntrySegment, UNCONGESTED_AS_CONGESTED_FLOW_THRESHOLD_PCUH);
    }else{
      pasEntrySegmentCongestedResult = Pair.of(false,false); // dummy entry, no congestion by definition
    }

    boolean pasS2PotentialDiscontinuity =
            (firstS2CongestedSegment != null && s2FirstCongestedSegmentResult.second()) ||
                    (pasEntrySegmentCongestedResult.first() && !pasEntrySegmentCongestedResult.second());
    if (pasS2PotentialDiscontinuity) {

      //TODO: this is not a good estimate since we now assume the link flow as a whole drives the queue, but it may be
      // that a tiny portion on one turn is causing the full reduction factor while the majority of flow would sail through
      // were it not for that small turn flow --> we must compute the discontinuity slack flow for each turn separate and
      // then use the minimum
      // looking at spreadsheet with small example it may not be enough either

      EdgeSegment originalCongestedS2Segment = firstS2CongestedSegment.getOriginalAdjacentEdgeSegments().first();
      int originalCongestedS2SegmentId = (int) originalCongestedS2Segment.getId();
      double s2DeltaFlowToStateChangeEstimate =
              networkLoading.getCurrentInflowsPcuH()[originalCongestedS2SegmentId] *
                      (1 - networkLoading.getCurrentFlowAcceptanceFactors()[originalCongestedS2SegmentId]);
      double s2SlackFlowLeeway = ((PcuCapacitated) originalCongestedS2Segment).getCapacityOrDefaultPcuH() * stateChangeLeewayPercentage;
      flowShift = adjustFlowShiftBasedOnS2SlackFlow(
              flowShift, s2DeltaFlowToStateChangeEstimate, s2SlackFlowLeeway);
    }

    // make sure we never shift more than the flow that is available
    flowShift = Math.min(flowShift, guaranteedS2SendingFlow);
    if(originalEntrySegment != null) {
      result.put(originalEntrySegment, flowShift);
    }else{
      // use dummy since entry segment is not used in conjugate anyway, but it can't be null while
      // for conjuate connector turn there may be no original
      result.put(pas.getFirstEdgeSegment(true), flowShift);
    }
    return result;
  }

  /**
   * all PASs that - if we were to execute the proposed shift - remain uncongested, will be equilibrated over
   * multiple iterations. Costs and flows on the network, bushes will be updated in full. If an uncongested PAS
   * cannot be updated because it would trigger a state change, the flow change is NOT executed and its status is
   * changed from {@code PasStatus.UNCONGESTED_WITH_SHIFT} to {@code PasStatus.UNCONGESTED_WITHOUT_SHIFT}
   *
   * @param theMode              to use
   * @param networkLoading       to use
   * @param gapFunction          to use
   * @param physicalCost         to use
   * @param virtualCost          to use
   * @param originalNetworkCosts to use
   * @param conjSegmentCosts     to use
   * @param logAll               to use
   * @return true when any flow was shifted, false otherwise
   */
  public boolean executeUncongestedPasEquilibration(
      Mode theMode,
      StaticLtmLoadingBushBase<ConjugateDestinationBush> networkLoading,
      GapFunction gapFunction,
      AbstractPhysicalCost physicalCost,
      AbstractVirtualCost virtualCost,
      double[] originalNetworkCosts,
      double[] conjSegmentCosts,
      boolean logAll) {

    // only consider PAS when it is potentially uncongested, confirm later with explicit check
    if(this.pas.getStatus() != PasStatus.UNCONGESTED_WITHOUT_SHIFT){
      return false;
    }

    // update costs because if another overlapping uncongested PAS was updated previously, current costs are no longer
    // up to date
    pas.updateCost(conjSegmentCosts);

    double s1SendingFlow = 0;
    for (var bush : pas.getRegisteredBushes()) {
      s1SendingFlow += bush.determineSubPathSendingFlow(
          pas.getAlternative(true), networkLoading.getCurrentFlowAcceptanceFactors());
    }

    // enter uncongested equilibration phase.
    boolean converged = false;
    int MAX_INTERAL_ITERATIONS_ALLOWED = 10;
    int internalIteration = 0;
    boolean doNotStop = true;
    boolean flowShifted = false;
    do{

      //--------------- UPDATE SENDING FLOWS THROUGH ALTERNATIVE ------------------------------------
      // sub path nl sending flows current (which is likely different and lower/higher than the one consistent
      // with loading due to other uncongested flow shifts performed on other PASs, or in previous updates here).
      // todo: while this is better for rounding, we could just adjust these values without going through the motions
      //  each iteration based on the flow shift applied.
      Map<ConjugateDestinationBush, Double> bushS2RemainingSendingFlows = new TreeMap<>();
      double guaranteedS2SendingFlow = 0;
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
      var proposedShiftResult = determineProposedFlowShiftByLoadingEntrySegment(
          theMode, physicalCost, virtualCost, networkLoading, guaranteedS2SendingFlow);
      double proposedFlowShift = proposedShiftResult.values().iterator().next();

      // verify if uncongested considering the shift we decided to apply. If so continue (UNCONGESTED_WITH_SHIFT)
      // Otherwise, stop equilibration process without shifting or part way through equilibration.
      boolean ignoreInitialConjEdgeSegment = true; // ignore initial because shift will not change amount of flow on entry
      double s1SlackFlow = determinePasAlternativeSlackFlow(
          networkLoading, true, ignoreInitialConjEdgeSegment).first();
      if(proposedFlowShift > s1SlackFlow){
        // insufficient slack, do not process (further) - mark for congested processing
        pas.updateStatus(PasStatus.UNCONGESTED_WITHOUT_SHIFT);
        break;
      }
      pas.updateStatus(PasStatus.UNCONGESTED_WITH_SHIFT);
      if(isDestinationTrackedForLogging() || logAll) {
        LOGGER.info("* UNCONGESTED FLOW SHIFT on PAS:" + pas + " - S2 flow: " + guaranteedS2SendingFlow + " - cost-diff: " + pas.getReducedCost());
      }

      double totalPasShift = 0;
      for (var entry : bushS2RemainingSendingFlows.entrySet()) {
        ConjugateDestinationBush conjBush = entry.getKey();
        double bushS2RemainingSendingFlow = entry.getValue();

        // scale to bush
        double bushS2Portion = bushS2RemainingSendingFlow / guaranteedS2SendingFlow;
        double bushPasFlowShift = proposedFlowShift * bushS2Portion;

        if(isDestinationTrackedForLogging(conjBush) || logAll) {
          LOGGER.info(String.format(
              "     Uncongested Shift: %.9f (available flow %.9f) - bush (%s) ",
              bushPasFlowShift, bushS2RemainingSendingFlow,conjBush.getRootZone().getIdsAsString()));
        }

        /* perform the flow shift IN FULL for S1 and S2 for the current bush and its attributed portion */
        // todo: for now use general flow shift, but can be optimised since we know no acceptance factors are needed
        executeBushS2FlowShiftNoNodeModelUpdate(
            conjBush, null, bushPasFlowShift, networkLoading);
        executeBushS1FlowShiftNoNodeModelUpdate(
            conjBush, null, bushPasFlowShift, networkLoading, null);
        totalPasShift += bushPasFlowShift;
      }

      flowShifted = flowShifted || totalPasShift>0;

      // sync costs to changes in flow, to allow for next proposed flow update
      boolean costSwitch = false;
      {
        // sync network flow to...
        syncUncongestedPasFlowShiftToNetworkFlow(networkLoading, totalPasShift);
        // sync conj expanded costs to ...
        updateOriginalAndConjugateNetworkCostsToCurrentPasFlows(
            theMode, networkLoading, physicalCost, virtualCost, originalNetworkCosts, conjSegmentCosts);
        // sync local PAS cost based on synced network costs
        costSwitch = pas.updateCost(conjSegmentCosts);
      }

      s1SendingFlow += totalPasShift;
      double s2SendingFlow = Math.max(0, guaranteedS2SendingFlow - totalPasShift);
      if(costSwitch){
        double prevS1SendingFlow = s1SendingFlow;
        s1SendingFlow = s2SendingFlow;
        s2SendingFlow = prevS1SendingFlow;
      }

      double pasGap = 0;
      if(s2SendingFlow > 0) {
        pasGap = pas.getReducedCost() * s2SendingFlow
            /
            (pas.getAlternativeLowCost() * (s1SendingFlow + s2SendingFlow));
      }else{
        pasGap = pas.getReducedCost();
      }

      // reuse criterion of gap (overall gap is done wider, so we do not update gap as such here)
      converged = pasGap <= gapFunction.getGap();
      ++internalIteration;

      doNotStop = !converged && internalIteration <= MAX_INTERAL_ITERATIONS_ALLOWED;

      // remove zero-flow S2 bushes from PAS when we know they won't get used again, or it is the final iteration
      if(!costSwitch || !doNotStop) {
        removeZeroFlowBushesFromPas(false /* no dangling nodes */);
      }
    }while(doNotStop);

    return flowShifted;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean performEquilibratedCongestedFlowShifts(
      Mode theMode,
      StaticLtmLoadingBushBase<?> networkLoading,
      Smoothing smoothing,
      GapFunction gapFunction,
      AbstractPhysicalCost physicalCost,
      AbstractVirtualCost virtualCost,
      double[] originalNetworkCosts,
      double[] conjSegmentCosts,
      Set<? extends RootedBush<?,?>> bushes,
      boolean logAll) {

    //TODO: largely equivalent to uncongested setup but with some tweaks to account for complexities of
    // alphas being potentially < 1, see if can be consolidated at some point as now there is a lot of duplicate
    // code

    //NOT NEEDED WE DO IT IN ONE GO!
    //getFlowShiftedS2BushData().clear();

    // update before we start since any overlap with other PASs that have been updated already will cause the current
    // cost to be outdated
    pas.updateCost(conjSegmentCosts);

    var s1Alternative = pas.getAlternative(true);
    double s1SendingFlow = 0;
    for (var bush : pas.getRegisteredBushes()) {
      s1SendingFlow += bush.determineSubPathSendingFlow(
          s1Alternative, networkLoading.getCurrentFlowAcceptanceFactors());
    }

    // Make sure original sending flows as a constraint are locked in via originalBushTurnFlowTracker
    // so we do not run the risk of
    // considering too much flow to shift as this will act as a bound for those cases where flow was added
    // to a low cost segment. originalBushTurnFlowTracker is used in the determination of the constrained subpath
    // sending flows later
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

    // enter congested equilibration phase.
    boolean converged = false;
    int MAX_INTERAL_ITERATIONS_ALLOWED = 10;
    int internalIteration = 0;
    final Map<ConjugateDestinationBush, Double> bushS2RemainingSendingFlows = new TreeMap<>();
    boolean doNotStop = true;
    boolean flowShifted = false;
    do{

      //--------------- UPDATE SENDING FLOWS THROUGH ALTERNATIVE ------------------------------------
      bushS2RemainingSendingFlows.clear();
      for (var bush : pas.getRegisteredBushes()) {
        ConjugateDestinationBush conjBush = (ConjugateDestinationBush) bush;
        double remainingSubPathSendingFlow = conjBush.determineConstrainedSubPathSendingFlow(
            pas.getAlternative(false),
            networkLoading.getCurrentFlowAcceptanceFactors(),
            this.originalBushTurnFlowTracker.get(conjBush)); // <-- differs from uncongested equilibration
        if(remainingSubPathSendingFlow > 0) {
          bushS2RemainingSendingFlows.put(conjBush, remainingSubPathSendingFlow);
        }
      }
      double remainingS2SendingFlow = bushS2RemainingSendingFlows.values().stream().mapToDouble(e -> e).sum();
      double guaranteedS2SendingFlow = remainingS2SendingFlow; // use latest always as it may be higher than original

      if(isDestinationTrackedForLogging() || logAll) {
        LOGGER.info("* S2 FLOW SHIFT on PAS:" + pas + " - S2 flow: " + guaranteedS2SendingFlow + " - cost-diff: " + pas.getReducedCost());
      LOGGER.info("s1 alphas: "+
              Arrays.stream(s1Alternative).filter(ConjugateEdgeSegment::hasOriginalEntryEdgeSegment).map(
                  es -> String.format("%s:%.2f",
                      es.getXmlId(), networkLoading.getCurrentFlowAcceptanceFactors()[
                          (int) es.getOriginalAdjacentEdgeSegments().first().getId()])).collect(Collectors.joining(",")));
      LOGGER.info("s2 alphas: "+
              Arrays.stream(s2Alternative).filter(ConjugateEdgeSegment::hasOriginalEntryEdgeSegment).map(es -> String.format("%s:%.2f",
                      es.getXmlId(), networkLoading.getCurrentFlowAcceptanceFactors()[
                          (int) es.getOriginalAdjacentEdgeSegments().first().getId()])).collect(Collectors.joining(",")));
      }

      if(guaranteedS2SendingFlow <= 0 ){
        removeZeroFlowBushesFromPas(false);
        break;
      }

      // determine proposed flow shift now that we have costs and available flows
      var proposedShiftResult = determineProposedFlowShiftByLoadingEntrySegment(
          theMode, physicalCost, virtualCost, networkLoading, guaranteedS2SendingFlow);
      double proposedFlowShift = proposedShiftResult.values().iterator().next();

      //todo if possible get rid of this
      double smoothedProportionalPasflowShift = smoothing.executeRefZero(proposedFlowShift);
      /*test for eligibility to reduce to zero flow along S2 */
      if (smoothedProportionalPasflowShift >= guaranteedS2SendingFlow) {
        if(isDestinationTrackedForLogging() || logAll) {
          LOGGER.info(String.format("     [removal --> proposed shift %.10f equal or higher than s2 sending flow %.10f]",
              smoothedProportionalPasflowShift, guaranteedS2SendingFlow));
        }

        /* truncate to guaranteed available S2 flow */
        smoothedProportionalPasflowShift = guaranteedS2SendingFlow;
      }

      if(!Double.isNaN(pas.getProposedPasFlowShiftAdjustmentFactor()) &&
          !Double.isInfinite(pas.getProposedPasFlowShiftAdjustmentFactor())){
        smoothedProportionalPasflowShift *= pas.getProposedPasFlowShiftAdjustmentFactor();
        smoothedProportionalPasflowShift = Math.min(guaranteedS2SendingFlow, smoothedProportionalPasflowShift);
      }

      if(smoothedProportionalPasflowShift < 0){
        break;
      }

      double totalPasShift = 0;
      for (var entry : bushS2RemainingSendingFlows.entrySet()) {
        ConjugateDestinationBush conjBush = entry.getKey();
        double bushS2RemainingSendingFlow = entry.getValue();

        /* In case of multiple used bushes -> we cannot let proposed shifts be executed in full because cost is affected
         * and therefore succeeding entries would "overshoot". Hence, we apply proposed shift proportionally to
         * contribution to total flow along PAS */
        double bushS2Portion = bushS2RemainingSendingFlow / guaranteedS2SendingFlow;
        double bushPasFlowShift = smoothedProportionalPasflowShift * bushS2Portion;

        if(isDestinationTrackedForLogging(conjBush) || logAll) {
          LOGGER.info(String.format(
              "     Shift: %.9f (available flow %.9f) - bush (%s) ",
              bushPasFlowShift, bushS2RemainingSendingFlow,conjBush.getRootZone().getIdsAsString()));
        }

        /* perform the flow shift IN FULL for S1 and S2 for the current bush and its attributed portion */
        executeBushS2FlowShiftNodeModelUpdate(
            conjBush, bushPasFlowShift, networkLoading, (Set<ConjugateDestinationBush>) bushes);
        executeBushS1FlowShiftNodeModelUpdate(
            conjBush,  bushPasFlowShift, networkLoading, (Set<ConjugateDestinationBush>) bushes);
        totalPasShift += bushPasFlowShift;
      }

      if(Precision.smaller(totalPasShift, smoothedProportionalPasflowShift, EPSILON_3)){
        LOGGER.info(String.format("flow shifted on network level (%.8f) larger than total flow shifted at bush level " +
            "(%.8f), ideally this does not happen", smoothedProportionalPasflowShift, totalPasShift));
      }
      flowShifted = flowShifted || totalPasShift>0;

      // sync costs to changes in flow, to allow for next proposed flow update
      boolean costSwitch = false;
      {
        var conjLoading = (StaticLtmLoadingBushConjugate) networkLoading;
        // no need to sync network flows because that was already done during alpha/node model update <-- differs from uncongested

        // need to sync network splitting rates since those are derived from the bushes that now have different flows
        // this is required in next local iteration since splitting rates are used in network node model calculation
        // TODO --> remove now done on the fly to remain consistent during the update of the nodes as we go, can't be done afterwards
        //executeNetworkSplittingRateUpdateForPas(conjLoading, (Set<ConjugateDestinationBush>) bushes);

        // sync conj expanded and original costs to new flows, this will inform updated derivatives and flow shift
        // step ...
        updateOriginalAndConjugateNetworkCostsToCurrentPasFlows(
            theMode, conjLoading, physicalCost, virtualCost, originalNetworkCosts, conjSegmentCosts);
        // sync local PAS cost based on synced network costs (may cause switch in s1/s2)
        costSwitch = pas.updateCost(conjSegmentCosts);
      }

      s1SendingFlow += totalPasShift;
      double s2SendingFlow = guaranteedS2SendingFlow - totalPasShift;
      if(costSwitch){
        double prevS1SendingFlow = s1SendingFlow;
        s1SendingFlow = s2SendingFlow;
        s2SendingFlow = prevS1SendingFlow;
        s1Alternative = pas.getAlternative(true);
        s2Alternative = pas.getAlternative(false);
      }
      double pasGap = pas.getReducedCost() * s2SendingFlow
          /
          (pas.getAlternativeLowCost() * (s1SendingFlow + s2SendingFlow));
      // reuse criterion of gap (overall gap is done wider, so we do not update gap as such here)
      //converged = pasGap <= gapFunction.getStopCriterion().getEpsilon();
      converged = pasGap <= gapFunction.getGap();
      ++internalIteration;

      doNotStop = !converged && internalIteration <= MAX_INTERAL_ITERATIONS_ALLOWED;
      // remove zero-flow S2 bushes from PAS when we know they won't get used again, or it is the final iteration
      if(!costSwitch || !doNotStop) {
        removeZeroFlowBushesFromPas(false /* no dangling nodes */);
      }
    }while(!converged && internalIteration <= MAX_INTERAL_ITERATIONS_ALLOWED);

    return flowShifted;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public boolean performOneShotCongestedS2FlowShift(
          Map<EdgeSegment, Double> proposedFlowShifts,
          Mode theMode,
          StaticLtmLoadingBushBase<?> networkLoading,
          Smoothing smoothing,
          boolean logAll) {
    if(proposedFlowShifts.size()!=1){
      throw new PlanItRunTimeException("Expecting exactly one single original network entry segment for a " +
              "conjugate bush PAS");
    }
    double proposedFlowShift = proposedFlowShifts.values().stream().mapToDouble(d->d).sum();

    // reset and repopulate the actually s2 shifted(removed) flow which is needed to later perform the
    // equivalent s1 flow shifts (by adding)
    getFlowShiftedS2BushData().clear();

    double nlConsistentS2SendingFlow = getS2SendingFlow(); // consistent with original loading

    //--------------- UPDATE SENDING FLOWS THROUGH ALTERNATIVE ------------------------------------
    // sub path nl sending flows current (which is likely different and lower than the ones consistent with loading
    // due to S2 flow shifts performed on other PASs).  In conjugate form, no need to consider entry segments anymore
    Map<ConjugateDestinationBush, Double> bushS2RemainingSendingFlows = new TreeMap<>();
    for (var bush : pas.getRegisteredBushes()) { // todo: stopgap cast
      ConjugateDestinationBush conjBush = (ConjugateDestinationBush) bush;
      double remainingSubPathSendingFlow = conjBush.determineSubPathSendingFlow(
              pas.getAlternative(false), networkLoading.getCurrentFlowAcceptanceFactors());
      if(remainingSubPathSendingFlow > 0) {
        bushS2RemainingSendingFlows.put(conjBush, remainingSubPathSendingFlow);
      }
    }
    double remainingS2SendingFlow = bushS2RemainingSendingFlows.values().stream().mapToDouble(e -> e).sum();

    // if earlier shifts have reduced available flow, capture in factor, so we remain in feasible shifting region
    double s2FlowAvailabilityFactor = Math.min(1,remainingS2SendingFlow/nlConsistentS2SendingFlow);
    double guaranteedS2SendingFlow = remainingS2SendingFlow; // use latest always as it may be higher than original

    if(isDestinationTrackedForLogging() || logAll) {
      LOGGER.info("* S2 FLOW SHIFT on PAS:" + pas + " - S2 flow: " + guaranteedS2SendingFlow + "(NL consistent: " + nlConsistentS2SendingFlow+") - cost-diff: " + pas.getReducedCost());
//      LOGGER.info("s1 alphas: "+
//              Arrays.stream(pas.getAlternative(true)).map(es -> String.format("%s:%.2f",
//                      es.getXmlId(), networkLoading.getCurrentFlowAcceptanceFactors()[(int) es.getId()])).collect(Collectors.joining(",")));
//      LOGGER.info("s2 alphas: "+
//              Arrays.stream(pas.getAlternative(false)).map(es -> String.format("%s:%.2f",
//                      es.getXmlId(), networkLoading.getCurrentFlowAcceptanceFactors()[(int) es.getId()])).collect(Collectors.joining(",")));
    }

    double smoothedProportionalPasflowShift = smoothing.executeRefZero(proposedFlowShift);
    /*test for eligibility to reduce to zero flow along S2 */
    if (smoothedProportionalPasflowShift >= guaranteedS2SendingFlow) {

      if(isDestinationTrackedForLogging() || logAll) {
        LOGGER.info(String.format("     [removal --> proposed shift %.10f equal or higher than s2 sending flow %.10f]",
                smoothedProportionalPasflowShift, guaranteedS2SendingFlow));
      }

      /* truncate to guaranteed available S2 flow */
      smoothedProportionalPasflowShift = guaranteedS2SendingFlow;
    }

    if(!Double.isNaN(pas.getProposedPasFlowShiftAdjustmentFactor()) &&
            !Double.isInfinite(pas.getProposedPasFlowShiftAdjustmentFactor())){
      smoothedProportionalPasflowShift *= pas.getProposedPasFlowShiftAdjustmentFactor();
      smoothedProportionalPasflowShift = Math.min(guaranteedS2SendingFlow, smoothedProportionalPasflowShift);
    }

    for (var entry : bushS2RemainingSendingFlows.entrySet()) {
      ConjugateDestinationBush conjBush = entry.getKey();
      double bushS2RemainingSendingFlow = entry.getValue();

      /* In case of multiple used bushes -> we cannot let proposed shifts be executed in full because cost is affected
       * and therefore succeeding entries would "overshoot". Hence, we apply proposed shift proportionally to
       * contribution to total flow along PAS */
      double bushS2Portion = bushS2RemainingSendingFlow / guaranteedS2SendingFlow;
      double bushPasFlowShift = smoothedProportionalPasflowShift * bushS2Portion;

      if(isDestinationTrackedForLogging(conjBush) || logAll) {
        LOGGER.info(String.format(
                "     Shift: %.9f (available flow %.9f) - bush (%s) ",
                bushPasFlowShift, bushS2RemainingSendingFlow,conjBush.getRootZone().getIdsAsString()));
      }

      /* perform the flow shift for the current bush and its attributed portion */
      var dummyEntrySegment = proposedFlowShifts.keySet().stream().findAny().get();
      executeBushS2FlowShiftNoNodeModelUpdate(
              conjBush, dummyEntrySegment, bushPasFlowShift, networkLoading);
      // track what was shifted for later S1 update
      // todo: when moving to fully conjugate get rid of BushEntryShiftedS2FlowData, or simplify to not have splitting
      //  rates nor entry segments as they are not used in this context
      putFlowShiftedS2Data(dummyEntrySegment, conjBush, new BushEntryShiftedS2FlowData(
              conjBush, dummyEntrySegment, bushPasFlowShift, null));
    }

    /* remove zero-flow S2 bushes from PAS */
    removeZeroFlowBushesFromPas(false /* no dangling nodes */);

    return !getFlowShiftedS2BushData().isEmpty();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void performAllBushesS1FlowShift(
          Mode theMode,
          StaticLtmLoadingBushBase<?> networkLoading) {

    if(getFlowShiftedS2BushData().size() != 1){
      throw new PlanItRunTimeException("Expecting exactly one single original network entry segment for a " +
              "conjugate bush PAS");
    }
    Map<RootedBush<ConjugateDirectedVertex, ConjugateEdgeSegment>, BushEntryShiftedS2FlowData> executedBushS2FlowShifts
            = getFlowShiftedS2BushData().values().stream().findFirst().get();
//    if(executedBushS2FlowShifts.keySet().stream().anyMatch(this::isDestinationTrackedForLogging)) {
//      LOGGER.info(String.format("* S1 FLOW SHIFT on PAS: %s", pas));
//    }

    // For each bush - execute S1 flow shift
    for (var entry : executedBushS2FlowShifts.entrySet()) {
      ConjugateDestinationBush bush = (ConjugateDestinationBush) entry.getKey();
      BushEntryShiftedS2FlowData flowShiftData = entry.getValue();

//      if(isDestinationTrackedForLogging(bush)) {
//        LOGGER.info(String.format("        Flow to shift: %.8f - bush (%s)",
//                flowShiftData.getS2Flowshifted(),
//                bush.getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
//      }

      executeBushS1FlowShiftNoNodeModelUpdate(
              bush,
              flowShiftData.getEntrySegment(),
              flowShiftData.getS2Flowshifted(),
              networkLoading,
              flowShiftData.getS2MergeExitSplittingRates());
    }
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

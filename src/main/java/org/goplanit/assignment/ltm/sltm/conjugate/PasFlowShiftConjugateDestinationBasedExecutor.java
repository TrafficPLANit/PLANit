package org.goplanit.assignment.ltm.sltm.conjugate;

import org.goplanit.assignment.ltm.sltm.*;
import org.goplanit.assignment.ltm.sltm.consumer.NMRUpdateIncomingConjugateOutFlowsFactorsAndCostsConsumer;
import org.goplanit.assignment.ltm.sltm.loading.NetworkLoadingSplittingRateDataPartial;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingBushBase;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingBushConjugate;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmNetworkLoading;
import org.goplanit.cost.physical.AbstractPhysicalCost;
import org.goplanit.cost.physical.SteadyStateTravelTimeCost;
import org.goplanit.cost.virtual.AbstractVirtualCost;
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
   * @param theMode to use
   * @param originalNlFlowAcceptanceFactors from loading rather than on-the-fly, used for bounding shifts when
   *                                        propagating in case it is more restrictive than the most recent
   * @param assignmentStrategy to use
   * @param originalNetworkCosts to use (only needed when updating node model)
   * @param conjSegmentCosts to use (only needed when updating node model)
   * @param bushes to use (only needed when updating node model splitting rates)
   * @param updateNetworkNodeModel when tru update node model on the fly
   * @return final flow shift applied on last segment of alternative
   */
  private double executeBushPasFlowShift(
          ConjugateDestinationBush conjBush,
          double bushPasFlowShiftPcuH,
          ConjugateEdgeSegment[] pasAlternative,
          Mode theMode,
          double[] originalNlFlowAcceptanceFactors,
          StaticLtmConjugateBushStrategy assignmentStrategy,
          double[] originalNetworkCosts,
          double[] conjSegmentCosts,
          Set<ConjugateDestinationBush> bushes,
          boolean updateNetworkNodeModel) {

    var networkLoading = assignmentStrategy.getLoading();
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
        LOGGER.severe("sync shouldn't trigger");
      }
      // ensure that when rounded to zero and diff is smaller than detectable above,
      // we use exact change applied rather than proposed
      flowShiftPcuH = appliedFlowShift;

      double acceptanceFactorBefore = 1;
      double acceptanceFactorAfter = acceptanceFactorBefore;
      // we only do this if there is a chance of the alphas changing (so potentially congested)
      if(updateNetworkNodeModel) {
        EdgeSegment originalTurnEntrySegment = null;
        if (currentConjSegment.hasOriginalEntryEdgeSegment()) {
          originalTurnEntrySegment = currentConjSegment.getOriginalAdjacentEdgeSegments().first();
          var nlFlowAcceptanceFactorBefore = originalNlFlowAcceptanceFactors[(int) originalTurnEntrySegment.getId()];
          var onTheFlyFlowAcceptanceFactorBefore = nonConjugateFlowAcceptanceFactors[(int) originalTurnEntrySegment.getId()];
          acceptanceFactorBefore = Math.min(nlFlowAcceptanceFactorBefore, onTheFlyFlowAcceptanceFactorBefore);
        }
      }

        // sync network inflows/unconstrained flows/sendng flows, splitting rates, and alphas via network node model update <-- differs from uncongested

        // todo: now done per bush which is very inefficient AND incorrect under multiple bushes per PAS -->
        //  instead do per link of alternative and then per bush below to only do one node model update instead of #pas-bushes updates
      acceptanceFactorAfter = executeNetworkLevelTurnFlowShift(
            flowShiftPcuH,
            currentConjSegment,
            theMode,
            assignmentStrategy,
            originalNetworkCosts,
            conjSegmentCosts,
            bushes,
            updateNetworkNodeModel);

      flowShiftPcuH *= Math.min(acceptanceFactorBefore, acceptanceFactorAfter);

      // todo: remove below when the new approach bounded by original nl flow acceptance factors is working
//      // adjust flow shift
//      // case 1: no change in factor -> proceed with same flow shift and propagate further
//      // case 2: change in factor but no change in outflow      - stop flow shift propagation since traffic
//      //    withholding makes that no downstream flow shift exists (it is all removed from the withheld traffic)
//      // case 3: change in factor and change in outflow         - determine non-withheld change in flow, namely
//      //    the new outflow - old outflow
//      if(acceptanceFactorBefore != acceptanceFactorAfter){
//        double outflowBefore = currentFlow * acceptanceFactorBefore;
//        double outflowAfter = newFlow * acceptanceFactorAfter;
//        if(Precision.equal(outflowBefore, outflowAfter, EPSILON_6)){
//          // case 2: nothing left, all consumed by the change in withheld flow
//          flowShiftPcuH = 0;
//          // we still need to make sure all outflows are present for cost calculation. switch to outflowsyncing only
//          restrictToOutflowUpdateOnly = true;
//        }else{
//          // case 3: we propagate the remaining difference that is not consumed by removing the previously withheld flow
//          flowShiftPcuH = flowShiftPcuH>0 ?
//              Math.min(flowShiftPcuH, outflowAfter - outflowBefore):
//              Math.max(flowShiftPcuH, outflowAfter - outflowBefore);
//        }
//      }
    }
    return flowShiftPcuH;
  }

  /**
   * run node model and update unconstrained flows, inflows, sending flows on network level rather than individual
   * bush level. either with or without a node model update.
   *
   * todo: also update link costs and consider any discontinuity costs as well (so run node model in turn based model)
   *
   * @param flowShiftToApply      to use
   * @param pasAlternativeSegment to use
   * @param theMode               to use
   * @param assignmentStrategy        to use
   * @param originalNetworkCosts  to use
   * @param conjNetworkCosts      to use
   * @param bushes                to use
   * @param doNodeModelUpdate flag to indicate if we run a ndoe model update toconsider potential congestion
   * @return new flow acceptance factor for turn on pas alternative segment we're traversing
   */
  private double executeNetworkLevelTurnFlowShift(
      double flowShiftToApply,
      ConjugateEdgeSegment pasAlternativeSegment,
      Mode theMode,
      StaticLtmConjugateBushStrategy assignmentStrategy,
      double[] originalNetworkCosts,
      double[] conjNetworkCosts,
      Set<ConjugateDestinationBush> bushes,
      boolean doNodeModelUpdate) {

    var networkLoading = assignmentStrategy.getLoading();

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
          pasAlternativeSegment, bushes, networkLoading);
      if(flowShiftToApply > 0 &&
          networkLoading.getSplittingRateData() instanceof NetworkLoadingSplittingRateDataPartial){
        // any additional flow to a turn may potentially cause congestion. to ensure node model calculates such a node
        // it must be registered as potentially blocking. So we register it at such.
        ((NetworkLoadingSplittingRateDataPartial)networkLoading.getSplittingRateData()).registerPotentiallyBlockingNode(
            originalSegment.getDownstreamVertex());
      }

      if(doNodeModelUpdate) {
        var consumer = new NMRUpdateIncomingConjugateOutFlowsFactorsAndCostsConsumer(
            originalSegment.getDownstreamVertex(), theMode, assignmentStrategy, originalNetworkCosts, conjNetworkCosts);
        StaticLtmNetworkLoading.performNodeModelTurnBasedUpdate(
            originalSegment.getDownstreamVertex(), consumer, networkLoading, constrainedFlows);

        return networkLoading.getCurrentFlowAcceptanceFactors()[segmentIndex];
      }
      else{
        // update outflow based on new sending flow
        networkLoading.getCurrentOutflowsPcuH()[segmentIndex] =
            networkLoading.getCurrentSendingFlowsPcuH()[segmentIndex];
        // update cost
        ConjugateCostUtils.updateLinkAndConjugateSegmentCost(
            originalSegment, assignmentStrategy, theMode, originalNetworkCosts, conjNetworkCosts);
      }
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
      double mostRestrExitDemandConstrainedFlow = 0;
      if(!unCongested){
        var mostRestrictingExitDemandConstrFlowResult = identifyMostRestrictingOutSegmentAndDemandConstrainedFlow(
            originalEntrySegment, networkLoading); // todo should be done once and cached
        mostRestrictingExit = mostRestrictingExitDemandConstrFlowResult.first();
        mostRestrExitDemandConstrainedFlow = mostRestrictingExitDemandConstrFlowResult.second();
      }
      else if(isMerge && isLowCostAlternative){
        // MERGE
        // todo: to be replaced by analytical version just like the one below, only merge should also consider
        //  change from the other alternative adding to it for now ignore
      }
      // if for whatever reason we reverted to uncongested, no most restricting exists and we reset flag to avoid issues
      // with derivatives
      if(mostRestrictingExit == null){
        unCongested = true;
      }

      double currDTravelTimeDFlow = 0.0;
      boolean thisAltOnMostRestrictingTurn = (mostRestrictingExit == originalExitSegment);

      // DIVERGE/ON PAS/MERGE (congested link)
      // - case 1: flow not on most-restrictive turn --> compute hypo as if uncongested, allow to continue since
      //           flow change is expected to continue + use special derivative
      // - case 2: flow on most-restrictive turn --> treat as congested and stop after this link as flow change is not
      //           expected to propagate further
      if(!unCongested && !thisAltOnMostRestrictingTurn){
        // case 1: use hypo critical uncongested derivative + special derivative for changing flow on uncongested turn
        //         given another turn is congested and most restrictive
        var alpha_i = networkLoading.getCurrentFlowAcceptanceFactors()[(int) originalEntrySegment.getId()];
        var splittingRateToMostRestricting = networkLoading.getSplittingRateData().getSplittingRate(originalEntrySegment, mostRestrictingExit);
        var u_i = networkLoading.getCurrentInflowsPcuH()[(int) originalEntrySegment.getId()];
        var u_ijMostRestricting = u_i * splittingRateToMostRestricting;
        if(u_ijMostRestricting <= 0){
          LOGGER.severe("should always have most restricting turn flow but NOT???");
        }else {
          var c_i = ((PcuCapacitated) originalEntrySegment).getCapacityOrDefaultPcuH();
          var r_jMostRestr =
              ((PcuCapacitated) mostRestrictingExit).getCapacityOrDefaultPcuH() - mostRestrExitDemandConstrainedFlow;
          var timePeriodH = ((SteadyStateTravelTimeCost) physicalCost).getCurrentTimePeriodH();
          // b_j is the scaled sending flows of all other turns combined into the most restricting out link except for
          // the turn coming from our in link to the most restricting out link
          // b_j = (C_i*u_i_jMostRestr*(1-alpha_i))/(alpha_i*u_i) <-- see doc for how this was derived
          var approximateBMostRestricting =
              (c_i * (r_jMostRestr - (alpha_i * u_ijMostRestricting))) / (alpha_i * u_i);
          // d_hyper/du_ij for any j not going to the most restricting out link, given the link is congested due to
          // another turn into the known most restricting out link:
          // d_hyper/du_ij = (1/2*T*b_j)/(C_i*r_j)
          currDTravelTimeDFlow =
              (0.5 * timePeriodH * approximateBMostRestricting) /
                  (c_i * r_jMostRestr);
        }

        unCongested = true; // triggers adding hypo critical delay via normal approach below
      } // case 2 no action needed, remains congested on most restricting turn, or truly uncongested

      if (originalEntrySegment instanceof MacroscopicLinkSegment) {
        currDTravelTimeDFlow +=
            physicalCost.getDTravelTimeDFlow(unCongested, theMode, (MacroscopicLinkSegment) originalEntrySegment);
      } else if (originalEntrySegment instanceof ConnectoidSegment) {
        currDTravelTimeDFlow +=
            virtualCost.getDTravelTimeDFlow(unCongested, theMode, (ConnectoidSegment) originalEntrySegment);
      } else {
        LOGGER.severe(String.format("Unsupported edge segment (%s) to obtain derivative of cost towards flow from",
            originalEntrySegment.getIdsAsString()));
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
          Mode theMode,
          StaticLtmAssignmentStrategy assignmentStrategy,
          double[] originalNetworkCosts,
          double[] conjSegmentCosts,
          Set<? extends RootedBush<?,?>> bushes) {

    ConjugateDestinationBush conjBush = (ConjugateDestinationBush)bush;

    /* prep - pas */
    final var s2 = pas.getAlternative(false);
    double flowShiftPcuH = -bushEntrySegmentFlowShift;

    flowShiftPcuH = executeBushPasFlowShift(
        conjBush,
        flowShiftPcuH,
        s2,
        theMode,
        null, // not used because node model is not updated
        (StaticLtmConjugateBushStrategy) assignmentStrategy,
        originalNetworkCosts,
        conjSegmentCosts,
        (Set<ConjugateDestinationBush>) bushes, false);

    /*end splitting rates not required since we do not shift flow beyond end merge via its turn in conjugate form  */
    //todo: remove return value when we no longer have non-conjugate form for this
    return null;
  }

  protected double[] executeBushS2FlowShiftNodeModelUpdate(
      RootedBush<ConjugateDirectedVertex, ConjugateEdgeSegment> bush,
      double bushEntrySegmentFlowShift,
      Mode theMode,
      double[] originalNlFlowAcceptanceFactors,             // alphas from loading rather than on-the-fly, used for bounding shifts
      StaticLtmConjugateBushStrategy assignmentStrategy,
      double[] originalNetworkCosts,
      double[] conjSegmentCosts,
      Set<ConjugateDestinationBush> bushes) {

    ConjugateDestinationBush conjBush = (ConjugateDestinationBush)bush;

    /* prep - pas */
    final var s2 = pas.getAlternative(false);
    double flowShiftPcuH = -bushEntrySegmentFlowShift;

    flowShiftPcuH = executeBushPasFlowShift(
        conjBush,
        flowShiftPcuH,
        s2, theMode,
        originalNlFlowAcceptanceFactors,
        assignmentStrategy,
        originalNetworkCosts,
        conjSegmentCosts,
        bushes,
        true);

    /*end splitting rates not required since we do not shift flow beyond end merge via its turn in conjugate form  */
    //todo: remove return value when we no longer have non-conjugate form for this
    return null;
  }

  /**
   * {@inheritDoc}
   */
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

    ConjugateDestinationBush conjBush = (ConjugateDestinationBush) bush;

    var s1 = pas.getAlternative(true);

    double s1FinalLabeledFlowShift = executeBushPasFlowShift(
        conjBush,
        bushEntrySegmentFlowShift,
        s1,
        theMode,
        null, // not used because node model is not updated
        (StaticLtmConjugateBushStrategy) assignmentStrategy,
        originalNetworkCosts,
        conjSegmentCosts,
        (Set<ConjugateDestinationBush>) bushes,
        false);
  }

  protected void executeBushS1FlowShiftNodeModelUpdate(
      RootedBush<ConjugateDirectedVertex, ConjugateEdgeSegment> bush,
      double bushEntrySegmentFlowShift,
      Mode theMode,
      double[] originalNlFlowAcceptanceFactors,             // alphas from loading rather than on-the-fly, used for bounding shifts
      StaticLtmConjugateBushStrategy assignmentStrategy,
      double[] originalNetworkCosts,
      double[] conjSegmentCosts,
      Set<ConjugateDestinationBush> bushes) {

    ConjugateDestinationBush conjBush = (ConjugateDestinationBush) bush;

    var s1 = pas.getAlternative(true);

    double s1FinalLabeledFlowShift = executeBushPasFlowShift(
        conjBush,
        bushEntrySegmentFlowShift,
        s1,
        theMode,
        originalNlFlowAcceptanceFactors,
        assignmentStrategy,
        originalNetworkCosts,
        conjSegmentCosts,
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
          double guaranteedS2SendingFlow, boolean logAll) {
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
      double oldFlowShift = flowShift;
      flowShift = adjustFlowShiftBasedOnS1SlackFlow(flowShift, s1SlackFlowEstimate, s1SlackFlowLeeway);
      if(logAll && oldFlowShift > flowShift){
        LOGGER.info(String.format("S1 DISCONTINUITY ADJUSTMENT TRIGGERED from %.10f, to %.10f",
            oldFlowShift, flowShift));
      }
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
      double oldFlowShift = flowShift;
      flowShift = adjustFlowShiftBasedOnS2SlackFlow(
              flowShift, s2DeltaFlowToStateChangeEstimate, s2SlackFlowLeeway);
      if(logAll && oldFlowShift > flowShift){
        LOGGER.info(String.format("S2 DISCONTINUITY ADJUSTMENT TRIGGERED on (%s) from %.10f, to %.10f",
            firstS2CongestedSegment.getIdsAsString(),oldFlowShift, flowShift));
      }
    }

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
   * @param assignmentStrategy       to use
   * @param originalNetworkCosts to use
   * @param conjSegmentCosts     to use
   * @param logAll               to use
   * @return true when any flow was shifted, false otherwise
   */
  public boolean executeUncongestedPasEquilibration(
      Mode theMode,
      StaticLtmAssignmentStrategy assignmentStrategy,
      double[] originalNetworkCosts,
      double[] conjSegmentCosts,
      Set<ConjugateDestinationBush> bushes,
      boolean logAll) {

    var conjStrategy = (StaticLtmConjugateBushStrategy) assignmentStrategy;
    var networkLoading = conjStrategy.getLoading();

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
          theMode, conjStrategy.getPhysicalCost(), conjStrategy.getVirtualCost(), networkLoading, guaranteedS2SendingFlow, logAll);
      double rawProposedFlowShift = proposedShiftResult.values().iterator().next();
      double proposedFlowShift = Math.min(rawProposedFlowShift, guaranteedS2SendingFlow); // truncate to what is available

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

        // scale to bush while minimising risk of rounding issues near zero s2 flow
        double bushS2Portion = bushS2RemainingSendingFlow / guaranteedS2SendingFlow;
        double bushRawProposedFlowShift  = rawProposedFlowShift * bushS2Portion;
        double bushPasFlowShift = Math.min(bushRawProposedFlowShift, bushS2RemainingSendingFlow);

        if(isDestinationTrackedForLogging(conjBush) || logAll) {
          LOGGER.info(String.format(
              "     Uncongested Shift: %.9f (available flow %.9f) - bush (%s) ",
              bushPasFlowShift, bushS2RemainingSendingFlow,conjBush.getRootZone().getIdsAsString()));
        }

        /* perform the flow shift IN FULL for S1 and S2 for the current bush and its attributed portion */
        // todo: for now use general flow shift, but can be optimised since we know no acceptance factors are needed
        executeBushS2FlowShiftNoNodeModelUpdate(
            conjBush, null, bushPasFlowShift, theMode, conjStrategy, originalNetworkCosts, conjSegmentCosts, bushes);

        executeBushS1FlowShiftNoNodeModelUpdate(
            conjBush, null, bushPasFlowShift, theMode, conjStrategy, null, originalNetworkCosts, conjSegmentCosts, bushes);
        totalPasShift += bushPasFlowShift;
      }

      flowShifted = flowShifted || totalPasShift>0;

      // sync costs to changes in flow, to allow for next proposed flow update
      boolean costSwitch = false;
      {
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
      converged = pasGap <= conjStrategy.getGapFunction().getGap();
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
  public double performEquilibratedCongestedFlowShifts(
      Mode theMode,
      StaticLtmAssignmentStrategy assignmentStrategy,
      double[] originalNetworkCosts,
      double[] conjSegmentCosts,
      double[] originalNlConsistentFlowAcceptanceFactors,
      Set<? extends RootedBush<?,?>> bushes,
      boolean logAll) {

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

    double s1SendingFlow = 0;
    for (var bush : pas.getRegisteredBushes()) {
      s1SendingFlow += bush.determineConstrainedSubPathSendingFlow(
          s1Alternative,
          networkLoading.getCurrentFlowAcceptanceFactors(),
          originalNlConsistentFlowAcceptanceFactors,
          originalBushTurnFlowTracker.get(bush));
    }

    // enter congested equilibration phase.
    boolean converged = false;
    int MAX_INTERAL_ITERATIONS_ALLOWED = 1;
    //int MAX_INTERAL_ITERATIONS_ALLOWED = 10;
    int internalIteration = 1;
    final Map<ConjugateDestinationBush, Double> bushS2RemainingSendingFlows = new TreeMap<>();
    boolean doNotStop = true;
    double totalPasShift = 0;
    do{

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
        break;
      }

      // determine proposed flow shift now that we have costs and available flows
      var proposedShiftResult = determineProposedFlowShiftByLoadingEntrySegment(
          theMode, conjStrategy.getPhysicalCost(), conjStrategy.getVirtualCost(), networkLoading, guaranteedS2SendingFlow, logAll);
      double rawProposedFlowShift = proposedShiftResult.values().iterator().next();
      //todo if possible get rid of smoothing
      double smoothedRawPasflowShift = conjStrategy.getSmoothing().executeRefZero(rawProposedFlowShift);

      if(isDestinationTrackedForLogging() || logAll) {
        LOGGER.info("* S2 FLOW SHIFT on PAS:" + pas + " - S2 flow: " + guaranteedS2SendingFlow + " - cost-diff: " + pas.getReducedCost());
        LOGGER.info(String.format("Raw Proposed shift: %.10f Smoother proposed shift: %.10f",rawProposedFlowShift,smoothedRawPasflowShift));
        LOGGER.info("s1 alphas: "+
            Arrays.stream(s1Alternative).filter(ConjugateEdgeSegment::hasOriginalEntryEdgeSegment).map(
                es -> String.format("%s:%.6f",
                    es.getXmlId(), networkLoading.getCurrentFlowAcceptanceFactors()[
                        (int) es.getOriginalAdjacentEdgeSegments().first().getId()])).collect(Collectors.joining(",")));
        LOGGER.info("s2 alphas: "+
            Arrays.stream(s2Alternative).filter(ConjugateEdgeSegment::hasOriginalEntryEdgeSegment).map(es -> String.format("%s:%.6f",
                es.getXmlId(), networkLoading.getCurrentFlowAcceptanceFactors()[
                    (int) es.getOriginalAdjacentEdgeSegments().first().getId()])).collect(Collectors.joining(",")));
      }

      double smoothedPasflowShift = Math.min(smoothedRawPasflowShift, guaranteedS2SendingFlow);
      if(guaranteedS2SendingFlow < 5 && (rawProposedFlowShift/2.0) >= guaranteedS2SendingFlow){
        // todo the propose flow shift is currently truncated by s2flow anyway, ideally that is rmeoved so we can check
        //  here which is cleaner.
        // special case: when we have very little flow left on S2 AND derivatives indicate we should shift it all
        // then avoid the assumed unnecessary smoothing delay to egt to zero and allow full move to zero flow
        smoothedPasflowShift = guaranteedS2SendingFlow;
      }

      /*test for eligibility to reduce to zero flow along S2 */
      if (smoothedPasflowShift >= guaranteedS2SendingFlow && (isDestinationTrackedForLogging() || logAll)) {
          LOGGER.info(String.format("     [removal --> proposed shift %.10f equal or higher than s2 sending flow %.10f]",
              smoothedPasflowShift, guaranteedS2SendingFlow));
      }

      if(!Double.isNaN(pas.getProposedPasFlowShiftAdjustmentFactor()) &&
          !Double.isInfinite(pas.getProposedPasFlowShiftAdjustmentFactor())){
        smoothedPasflowShift *= pas.getProposedPasFlowShiftAdjustmentFactor();
        smoothedPasflowShift = Math.min(guaranteedS2SendingFlow, smoothedPasflowShift);
      }

      if(smoothedPasflowShift < 0){
        break;
      }

      for (var entry : bushS2RemainingSendingFlows.entrySet()) {
        ConjugateDestinationBush conjBush = entry.getKey();
        double bushS2RemainingSendingFlow = entry.getValue();

        /* In case of multiple used bushes -> we cannot let proposed shifts be executed in full because cost is affected
         * and therefore succeeding entries would "overshoot". Hence, we apply proposed shift proportionally to
         * contribution to total flow along PAS */
        double bushS2Portion = bushS2RemainingSendingFlow / guaranteedS2SendingFlow;
        double bushRawPasFlowShift = smoothedPasflowShift * bushS2Portion;
        double bushPasFlowShift = Math.min(bushRawPasFlowShift, bushS2RemainingSendingFlow);

        if(isDestinationTrackedForLogging(conjBush) || logAll) {
          LOGGER.info(String.format(
              "     Shift: %.9f (available flow %.9f) - bush (%s) ",
              bushPasFlowShift, bushS2RemainingSendingFlow,conjBush.getRootZone().getIdsAsString()));
        }

        /* perform the flow shift IN FULL for S1 and S2 for the current bush and its attributed portion */
        executeBushS2FlowShiftNodeModelUpdate(
            conjBush, bushPasFlowShift, theMode, originalNlConsistentFlowAcceptanceFactors, conjStrategy, originalNetworkCosts, conjSegmentCosts, (Set<ConjugateDestinationBush>) bushes);
        executeBushS1FlowShiftNodeModelUpdate(
            conjBush,  bushPasFlowShift, theMode, originalNlConsistentFlowAcceptanceFactors, conjStrategy, originalNetworkCosts, conjSegmentCosts, (Set<ConjugateDestinationBush>) bushes);
        totalPasShift += bushPasFlowShift;
      }

      if(Precision.smaller(totalPasShift, smoothedPasflowShift, EPSILON_3)){
        LOGGER.info(String.format("flow shifted on network level (%.8f) larger than total flow shifted at bush level " +
            "(%.8f), ideally this does not happen", smoothedPasflowShift, totalPasShift));
      }

      // sync costs to changes in flow, to allow for next proposed flow update
      boolean costSwitch = false;
      {
        var conjLoading = (StaticLtmLoadingBushConjugate) networkLoading;
        // no need to sync network flows or sync costs because that was already done during alpha/node model
        // update <-- differs from uncongested

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
      // normalise to a flow of at least 1 to ensure that low flow PASs do not stop equilibrating as gap is multiplied by very small number
      // despite perhaps a large reduced cost which would hinder convergence, especially if it is heading towards removing low turn flows entirely
      double pasGap = pas.getReducedCost() * Math.max(1,s2SendingFlow)
          /
          (pas.getAlternativeLowCost() * Math.max(1,(s1SendingFlow + s2SendingFlow)));
      // reuse criterion of gap (overall gap is done wider, so we do not update gap as such here)
      //converged = pasGap <= gapFunction.getStopCriterion().getEpsilon();
      converged = pasGap <= conjStrategy.getGapFunction().getGap();
      ++internalIteration;

      doNotStop = !converged && internalIteration <= MAX_INTERAL_ITERATIONS_ALLOWED;
      // remove zero-flow S2 bushes from PAS when we know they won't get used again, or it is the final iteration
      if(!costSwitch || !doNotStop) {
        removeZeroFlowBushesFromPas(false /* no dangling nodes */);
      }
      totalCongestedFlowShifted += totalPasShift;
    }while(!converged && internalIteration <= MAX_INTERAL_ITERATIONS_ALLOWED);

    return totalPasShift;
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

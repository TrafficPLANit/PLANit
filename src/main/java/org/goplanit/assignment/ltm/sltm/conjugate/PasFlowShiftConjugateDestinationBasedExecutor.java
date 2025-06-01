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
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
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
   * Execute a flow shift on a PAS alternative for a given bush.
   * <p>
   * Deprecated in favour of per link approach that only then considers all bushes instead of doing this per bush
   * across the entire alternative
   * </p>
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
  @Deprecated
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
    }
    return flowShiftPcuH;
  }

  // replaces executeBushPasFlowShift
  private Map<ConjugateDestinationBush, Double> executePasPerAlternativeFlowShift(
      double referenceWeight,
      Map<ConjugateDestinationBush, Double> bushWeights,
      double pasFlowShift,
      ConjugateEdgeSegment[] pasAlternative,
      Mode theMode,
      double[] originalNlFlowAcceptanceFactors,
      StaticLtmConjugateBushStrategy assignmentStrategy,
      double[] originalNetworkCosts,
      double[] conjSegmentCosts,
      Set<ConjugateDestinationBush> bushes,
      boolean updateNetworkNodeModel, boolean logAll) {

    var networkLoading = assignmentStrategy.getLoading();
    var nonConjugateFlowAcceptanceFactors = networkLoading.getCurrentFlowAcceptanceFactors();
    Map<ConjugateDestinationBush, Double> perBushStartingFlowShifts = new TreeMap<>();

    int index = 0;
    double remainingPasFlowShift = pasFlowShift;
    ConjugateEdgeSegment currentConjSegment;
    boolean restrictToOutflowUpdateOnly = false;
    while (index < pasAlternative.length) {
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
        double bushWeightedFlowShift = remainingPasFlowShift * bushPortion;

//        if (isDestinationTrackedForLogging(conjBush) || logAll) {
//          LOGGER.info(String.format(
//              "     Shift: %.9f - link segment (%s) - bush (%s) ",
//              bushWeightedFlowShift, currentConjSegment.getIdsAsString(), conjBush.getRootZone().getIdsAsString()));
//        }

        double currentFlow = conjBush.getTurnSendingFlow(currentConjSegment);
        if (currentFlow + bushWeightedFlowShift < 0) {
          bushWeightedFlowShift = -currentFlow; // sync to available flow
        }
        double newFlow = executeTurnFlowShift(conjBush, currentConjSegment, bushWeightedFlowShift);
        double appliedBushFlowShift = newFlow - currentFlow;
        if (Precision.notEqual(Math.abs(appliedBushFlowShift), Math.abs(bushWeightedFlowShift))) {
          double diff = currentFlow + bushWeightedFlowShift;
          LOGGER.severe("sync shouldn't trigger");
        }
        if (!perBushStartingFlowShifts.containsKey(conjBush)) {
          //track amount shifted per bush, so it can be used for S1 alternative (if this is s2)
          // we explicitly capture this in case the proposed shift could not be achieved and was truncated
          // to available flow, which should be considered when adding on S1 otherwise we may introduce
          // ghost flow.
          perBushStartingFlowShifts.put(conjBush, appliedBushFlowShift);
        }
        // track so we know the amount to apply on the network level
        totalBushAppliedFlowShift += appliedBushFlowShift;
      }

      // NETWORK LEVEL - UPDATE

      // network level splitting rates update
      var originalTurnEntrySegment = currentConjSegment.getOriginalAdjacentEdgeSegments().first();
      var originalTurnExitSegment = currentConjSegment.getOriginalAdjacentEdgeSegments().second();
      boolean connectorTurn = originalTurnEntrySegment==null || originalTurnExitSegment==null;
      if(totalBushAppliedFlowShift > 0 && !connectorTurn){
        // any additional flow to a turn may potentially cause congestion. to ensure node model calculates such a node
        // it must be registered as potentially blocking. So we register it at such.
        networkLoading.getSplittingRateData().registerPotentiallyBlockingNode(originalTurnEntrySegment.getDownstreamVertex());
      }
      double nonConjugateNetworkSplittingRate = 1;
      if(!connectorTurn) {
        // perform splitting rate update, required for correct network update below + we use splitting rate for
        // determining flow shift restrictions downstream (if any)
        executeNetworkSplittingRateUpdateForPasAlternativeSegment(currentConjSegment, bushes, networkLoading);
        nonConjugateNetworkSplittingRate = networkLoading.getSplittingRateData().getSplittingRate(
            originalTurnEntrySegment, originalTurnExitSegment);
      }

      // with the bush pas shifts applied, we can now update the network in one go across the bushes
      // knowing how much we are shifting
      double nlFlowAcceptanceFactorBefore = 1;
      double onTheFlyFlowAcceptanceFactorBefore = 1;
      double onTheFlyTurnInflowBefore = Double.MAX_VALUE;
      if (updateNetworkNodeModel && originalTurnEntrySegment != null) {
        // we only do this if there is a chance of the alphas changing (so potentially congested)
        onTheFlyTurnInflowBefore =
            networkLoading.getCurrentInflowsPcuH()[(int) originalTurnEntrySegment.getId()] * nonConjugateNetworkSplittingRate;
        nlFlowAcceptanceFactorBefore = originalNlFlowAcceptanceFactors[(int) originalTurnEntrySegment.getId()];
        onTheFlyFlowAcceptanceFactorBefore = nonConjugateFlowAcceptanceFactors[(int) originalTurnEntrySegment.getId()];
      }

      // sync network inflows/unconstrained flows/sending flows, splitting rates, alphas, and costs via network node
      // model update <-- differs from uncongested
      double onTheFlyAcceptanceFactorAfter = executeNetworkLevelTurnFlowShift(
          totalBushAppliedFlowShift,
          currentConjSegment,
          theMode,
          assignmentStrategy,
          originalNetworkCosts,
          conjSegmentCosts,
          bushes,
          updateNetworkNodeModel);

      double mostRestrictingAcceptanceFactorBefore =
          Math.min(onTheFlyFlowAcceptanceFactorBefore, nlFlowAcceptanceFactorBefore);

      // adjust flow shift
      // case 1: factors remain 1 -> proceed with same flow shift and propagate further
      // case 2: factors not both 1, but no change in outflow      - stop flow shift propagation since traffic
      //    withholding makes that no downstream flow shift exists (it is all removed from the withheld traffic)
      // case 3: factors not both 1 and change in outflow         - determine non-withheld change in flow, namely
      //    the new outflow - old outflow
      double newTurnInflowPcuH = onTheFlyTurnInflowBefore + totalBushAppliedFlowShift;
      double proposedRemainingShift = remainingPasFlowShift;
      if (onTheFlyFlowAcceptanceFactorBefore < 1 || onTheFlyAcceptanceFactorAfter < 1) {
        double onTheFlyTurnOutflowBefore = onTheFlyTurnInflowBefore * onTheFlyFlowAcceptanceFactorBefore;
        double onTheFlyTurnOutflowAfter = newTurnInflowPcuH * onTheFlyAcceptanceFactorAfter;
        if (Precision.equal(onTheFlyTurnOutflowBefore, onTheFlyTurnOutflowAfter, EPSILON_9)) {
          // case 2: nothing left, all consumed by the change in withheld flow
          proposedRemainingShift = 0;
          // we still need to make sure all outflows are present for cost calculation. switch to outflow syncing only
          restrictToOutflowUpdateOnly = true;
        } else {
          // case 3: we propose to propagate the remaining difference that is not consumed by removing the previously
          // withheld flow
          proposedRemainingShift = totalBushAppliedFlowShift > 0 ?
              Math.min(remainingPasFlowShift, onTheFlyTurnOutflowAfter - onTheFlyTurnOutflowBefore) :
              Math.max(remainingPasFlowShift, onTheFlyTurnOutflowAfter - onTheFlyTurnOutflowBefore);
        }
      }

      // lastly, we want to ensure we remain consistent with the most restricting situation compared to the original
      // network loading, so we take the minimum of our proposed remaining flow shift and the nl alphas in case that is
      // more restricting
      remainingPasFlowShift = totalBushAppliedFlowShift > 0 ?
          Math.min( nlFlowAcceptanceFactorBefore * remainingPasFlowShift,  proposedRemainingShift):
          Math.max( nlFlowAcceptanceFactorBefore * remainingPasFlowShift,  proposedRemainingShift);
    }
    return perBushStartingFlowShifts;
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
  protected Pair<ConjugateEdgeSegment, Boolean> findFirstCongestedEdgeSegmentOnPasAlternative(
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

    var alpha_i = networkLoading.getCurrentFlowAcceptanceFactors()[(int) originalEntrySegment.getId()];
    var splittingRateToMostRestricting = networkLoading.getSplittingRateData().getSplittingRate(originalEntrySegment, mostRestrictingExit);
    var u_i = networkLoading.getCurrentInflowsPcuH()[(int) originalEntrySegment.getId()];
    var u_ijMostRestricting = u_i * splittingRateToMostRestricting;
    if(u_ijMostRestricting <= 0){
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
    double unCongestedEntryTurnSendingFlow = 0;
    for(var entrySegment : node.getEntryEdgeSegments()){
      Array1D<Double> splittingRates = networkLoading.getSplittingRateData().getSplittingRates(entrySegment);
      var splittingRateToMostRestricting = splittingRates.get(exitSegmentIndex);

      int entrySegmentIndex = (int)entrySegment.getId();
      var entrySendingFlow = networkLoading.getCurrentInflowsPcuH()[entrySegmentIndex];
      if(entrySendingFlow <= 0){
        continue;
      }

      if(entrySegment == originalCongestedEntrySegment ){
        congestedEntryTurnSendingFlow = entrySendingFlow * splittingRateToMostRestricting;
      }else if(entrySegment == originalUncongestedEntrySegment){
        unCongestedEntryTurnSendingFlow = entrySendingFlow * splittingRateToMostRestricting;
      }else {
        var alphaEntrySegment = networkLoading.getCurrentFlowAcceptanceFactors()[entrySegmentIndex];
        double acceptedFlowOnTurnToMostRestricting = entrySendingFlow * splittingRateToMostRestricting * alphaEntrySegment;
        mostRestrictingExitNonMergeAcceptedFlows += acceptedFlowOnTurnToMostRestricting;
      }
    }

    // actual derivative using above prep vars
    double s1s2MergeEntriesSendingFlow = congestedEntryTurnSendingFlow +  unCongestedEntryTurnSendingFlow;
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
   * @return derivative and indicator whether to continue or not
   */
  @Override
  protected Triple<Double, Double, Boolean> getDTravelTimeDFlowExcludingMergeDiverge(
      final Mode theMode,
      final StaticLtmLoadingBushBase<?> networkLoading,
      final AbstractPhysicalCost physicalCost,
      final AbstractVirtualCost virtualCost,
      boolean isLowCostAlternative,
      double derivativeReductionFactor) {
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
        // no more flow change beyond here due to it being a bottleneck
        continueWithMergeDerivative = false;
        break;
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
    if(!unCongested){
      var mostRestrictingExitDemandConstrFlowResult = identifyMostRestrictingOutSegmentAndDemandConstrainedFlow(
          originalEntrySegment, networkLoading);
      mostRestrictingExit = mostRestrictingExitDemandConstrFlowResult.first();
    }

    if(mostRestrictingExit == null){
      unCongested = true;
    }

    // DIVERGE:
    //
    // (un)congested: no change in flow regardless, so no derivative exists really. We can only
    //                consider to what extent the rest of the PAS should be considered. So zero
    // congested  : when s2 is on most restricting turn use hyper critical derivative
    //                s1 is not on most restricting, so zero impact --> combined single hyper critical, s1 may containue
    //              when s1 is on most restricting turn use hypercritical derivative
    //                s2 is not on most restricting, so zero impact --> combined single hyper critical, s2 may continue
    //              when neither is on most restricting: zero as flow shift has no impact on cost
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
    //  - case 2: both congested: s2 and on most restricting to pas exit, but s1 most restricting does not
    //      lead to PAS exit. Can only happen when s1 has no flow yet, but has flow exiting to other congested exit
    //      derivatives for s1 work same as in ON PAS special case for congested turn on non-most restricting exit ,
    //      while s2 derivatives are impacted by flow taken by s1. However for s2 we have a choice since s1 is at zero:
    //      either use zero flow or as what it will have after regarding derivatives. We choose conservative approach
    //      (less shift) which means for s2 we opt as if there is no flow from s1 and simply treat it as a regular
    //      hypo + hyper approach
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
    //        for hypocritical aspect we can use the normal aspect for s1 and s2. Hyper is problematic though
    //        for s1, hyper derivative is zero, for s2 it may be negative (if s1+s2> most restricting capacity. This is
    //        always the case if it is a two way merge, it may not be for a normal cross node , or if the s1 derivative
    //        flow was diluted due to earlier alphas)
     //  - case 6b: same as 6a only now swap s1 and s2: same result only apply derivatives the other way around
    if(!s1UnCongested && !s2UnCongested && s2OnMostRestricting) {
      //case 1
      if(s1MostRestrictingExit==s2MostRestrictingExit) {
        double s1DTravelTimeDFlow = considerS1 ? computeRegularDTravelTimeDFlowSingleLink(
            theMode, physicalCost, virtualCost, originalS1EntrySegment, s1UnCongested) : 0;
        double s2DTravelTimeDFlow = considerS2 ? computeRegularDTravelTimeDFlowSingleLink(
            theMode, physicalCost, virtualCost, originalS2EntrySegment, s2UnCongested) : 0;
        return s2DTravelTimeDFlow + s1DTravelTimeDFlow;
      }else {
        //case 2
        // s1 (congested)
        double s1DTravelTimeDFlow = 0;
        if(considerS1) {
          s1DTravelTimeDFlow = computeHyperCriticalDTravelTimeDFlowNotMostRestrictiveTurnOnCongestedLink(
              networkLoading, physicalCost, originalS1EntrySegment, s1MostRestrictingExit, s1MostRestrExitDemandConstrainedFlow);
          s1DTravelTimeDFlow += computeRegularDTravelTimeDFlowSingleLink(
              theMode, physicalCost, virtualCost, originalS1EntrySegment, true /* uncongested only*/);
        }
        // s2 (congested)
        double s2DTravelTimeDFlow = considerS2 ? computeRegularDTravelTimeDFlowSingleLink(
            theMode, physicalCost, virtualCost, originalS2EntrySegment, s2UnCongested) : 0;
        return s2DerivativeReductionFactor * s2DTravelTimeDFlow + s1DerivativeReductionFactor * s1DTravelTimeDFlow;
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
      // case 6/6b

      //note: when s1 is not considered it has no impact on s2 in which case the special merge derivative does not
      // apply and instead we revert to the regular combined hypo + hyper critical derivative

      // s1 (uncongested)
      double s1HypoDTravelTimeDFlow = considerS1 ? computeRegularDTravelTimeDFlowSingleLink(
          theMode, physicalCost, virtualCost, originalS1EntrySegment, true /* force uncongested */) : 0;
      // s2 (uncongested) --> always uncongested unless s1 does not vary and we trigger full regular derivative
      //                      as well as switching off special case below.
      double s2HypoDTravelTimeDFlow = considerS2 ? computeRegularDTravelTimeDFlowSingleLink(
          theMode, physicalCost, virtualCost, originalS2EntrySegment, considerS1 ? true : s2UnCongested) : 0;

      var combinedDtravelTimeDflow = s1HypoDTravelTimeDFlow * s1DerivativeReductionFactor +
          s2HypoDTravelTimeDFlow * s2DerivativeReductionFactor;
      double specialCombinedHyperDTravelTimeDflow = 0;
      if(considerS1 && considerS2) {
        var uncongestedEntry = s1UnCongested ? originalS1EntrySegment : originalS2EntrySegment;
        var congestedEntry = s1UnCongested ? originalS2EntrySegment : originalS1EntrySegment;
        specialCombinedHyperDTravelTimeDflow = computeHyperCriticalDTravelTimeDFlowCombinedMergeSpecialCase(
            networkLoading, physicalCost, congestedEntry, uncongestedEntry, s2MostRestrictingExit);
        combinedDtravelTimeDflow += specialCombinedHyperDTravelTimeDflow * s2DerivativeReductionFactor;
        if (combinedDtravelTimeDflow < 0) {
          // for non linear FD branches the hypo derivative should generally avoid this, as only hyper derivative may be
          // negative. Regardless, we want to avoid issues and instead truncate to zero. Log for debugging todo analyse impact
          LOGGER.warning(String.format("Negative derivative on merge for pas %s. To avoid issues truncate to zero", pas));
          combinedDtravelTimeDflow = 0;
        }
      }
      return combinedDtravelTimeDflow;
    }else{
      LOGGER.severe(String.format("Unrecognised traffic state for merge derivative calculation on pas %s, " +
          "should not happen", pas));
      return 0;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Pair<Double,EdgeSegment> determinePasAlternativeSlackFlow(
          StaticLtmLoadingBushBase<?> networkLoading, boolean lowCost) {

    double slackFlow = Double.POSITIVE_INFINITY;
    EdgeSegment minSlackSegment = null;

    // regular PAS traversal rework back to original link segments
    int linkSegmentId = -1;
    ConjugateEdgeSegment conjAltEdgeSegment = null;
    ConjugateEdgeSegment[] conjAltEdgeSegments = pas.getAlternative(lowCost);
    int index = 1;
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
   * New approach which is per PAS and then per alternative link for all bushes
   */
  protected double executePasFlowShiftNodeModelUpdate(
      double guaranteedS2SendingFlow,
      Map<ConjugateDestinationBush, Double> bushS2RemainingSendingFlows,
      double pasFlowShift,
      Mode theMode,
      double[] originalNlFlowAcceptanceFactors,             // alphas from loading rather than on-the-fly, used for bounding shifts
      StaticLtmConjugateBushStrategy assignmentStrategy,
      double[] originalNetworkCosts,
      double[] conjSegmentCosts,
      Set<ConjugateDestinationBush> bushes,
      boolean logAll) {

    /* prep - pas */
    final var s2 = pas.getAlternative(false);
    // S2
    var appliedFlowShiftPcuHPerBush = executePasPerAlternativeFlowShift(
        guaranteedS2SendingFlow,
        bushS2RemainingSendingFlows,
        -pasFlowShift,
        s2, theMode,
        originalNlFlowAcceptanceFactors,
        assignmentStrategy,
        originalNetworkCosts,
        conjSegmentCosts,
        bushes,
        true,
        logAll);

    if(appliedFlowShiftPcuHPerBush == null || appliedFlowShiftPcuHPerBush.isEmpty()){
      LOGGER.warning(String.format("No S2 flow could be shifted for congested PAS: %s", this.pas));
      return 0.0;
    }

    //S1
    final var s1 = pas.getAlternative(true);
    double pasAppliedS2FlowShift = appliedFlowShiftPcuHPerBush.values().stream().mapToDouble(e->e).sum();
    if(Math.abs(pasAppliedS2FlowShift) <= 0.0){
      LOGGER.warning(String.format("No S2 flow was shifted for congested PAS: %s", this.pas));
      return 0.0;
    }

    executePasPerAlternativeFlowShift(
        pasAppliedS2FlowShift,
        appliedFlowShiftPcuHPerBush,
        Math.abs(pasAppliedS2FlowShift),
        s1, theMode,
        originalNlFlowAcceptanceFactors,
        assignmentStrategy,
        originalNetworkCosts,
        conjSegmentCosts,
        bushes,
        true,
        logAll);

    return Math.abs(pasAppliedS2FlowShift);
  }

  /**
   * identical to executePasFlowShiftNodeModelUpdate, except passing in different parameters
   * todo: consolidate into single general method with above
   */
  protected double executePasFlowShiftNoNodeModelUpdate(
      double guaranteedS2SendingFlow,
      Map<ConjugateDestinationBush, Double> bushS2RemainingSendingFlows,
      double pasFlowShift,
      Mode theMode,
      StaticLtmConjugateBushStrategy assignmentStrategy,
      double[] originalNetworkCosts,
      double[] conjSegmentCosts,
      Set<ConjugateDestinationBush> bushes,
      boolean logAll) {

    /* prep - pas */
    final var s2 = pas.getAlternative(false);
    // S2
    var appliedFlowShiftPcuHPerBush = executePasPerAlternativeFlowShift(
        guaranteedS2SendingFlow,
        bushS2RemainingSendingFlows,
        -pasFlowShift,
        s2, theMode,
        null,
        assignmentStrategy,
        originalNetworkCosts,
        conjSegmentCosts,
        bushes,
        false,
        logAll);

    if(appliedFlowShiftPcuHPerBush == null || appliedFlowShiftPcuHPerBush.isEmpty()){
      LOGGER.warning(String.format("No S2 flow could be shifted for uncongested PAS: %s", this.pas));
      return 0.0;
    }

    //S1
    final var s1 = pas.getAlternative(true);
    double pasAppliedS2FlowShift = appliedFlowShiftPcuHPerBush.values().stream().mapToDouble(e->e).sum();
    if(Math.abs(pasAppliedS2FlowShift) <= 0.0){
      LOGGER.warning(String.format("No S2 flow was shifted for uncongested PAS: %s", this.pas));
      return 0.0;
    }

    executePasPerAlternativeFlowShift(
        pasAppliedS2FlowShift,
        appliedFlowShiftPcuHPerBush,
        Math.abs(pasAppliedS2FlowShift),
        s1, theMode,
        null,
        assignmentStrategy,
        originalNetworkCosts,
        conjSegmentCosts,
        bushes,
        false,
        logAll);

    return Math.abs(pasAppliedS2FlowShift);
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
    // 0.1% of capacity is accepted as leeway for state change inducing flow shifts
    double stateChangeLeewayPercentage = 0.001;

    // todo: once we no longer have non-conjugate implementation remove any entry segment based tracking of flow shifts
    Map<EdgeSegment, Double> result = new TreeMap<>();
    var originalEntrySegment =
        pas.getFirstEdgeSegment(false).getOriginalAdjacentEdgeSegments().first();

    double denominatorS2 = 0;
    double denominatorS1 = 0;

    var s1FirstCongestedSegmentResult =
            findFirstCongestedEdgeSegmentOnPasAlternative(networkLoading, true);
    var s2FirstCongestedSegmentResult =
            findFirstCongestedEdgeSegmentOnPasAlternative(networkLoading, false);
    var firstS1CongestedSegment = s1FirstCongestedSegmentResult!= null ? s1FirstCongestedSegmentResult.first() : null;
    var firstS2CongestedSegment = s2FirstCongestedSegmentResult!= null ? s2FirstCongestedSegmentResult.first() : null;

    // todo: add multiplication factors using alphas to reduce impact of derivatives along the way to make them more
    //  accurate
    //todo: stitch together and determine if some should be run

    var denominatorDivergeResult = getDTravelTimeDFlowDiverge(
        theMode, networkLoading, physicalCost, virtualCost);
    boolean s1Continue = (boolean) denominatorDivergeResult[2];
    boolean s2Continue = (boolean) denominatorDivergeResult[3];
    double derivativeReductionFactorDiverge = (double) denominatorDivergeResult[1];
    double divergeDenominator = (double) denominatorDivergeResult[0];

    double onPasS1Denominator = 0;
    double onPasS2Denominator = 0;
    double derivativeReductionFactorS1UpToMerge = 1;
    double derivativeReductionFactorS2UpToMerge = 1;
    if(s1Continue) {
      var denominatorS1Result = getDTravelTimeDFlowExcludingMergeDiverge(
          theMode, networkLoading, physicalCost, virtualCost, true, derivativeReductionFactorDiverge);
      onPasS1Denominator = denominatorS1Result.first();
      derivativeReductionFactorS1UpToMerge = denominatorS1Result.second();
      s1Continue = denominatorS1Result.third();
    }
    if(s2Continue) {
      var denominatorS2Result = getDTravelTimeDFlowExcludingMergeDiverge(
          theMode, networkLoading, physicalCost, virtualCost, false, derivativeReductionFactorDiverge);
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
      LOGGER.info(String.format("divergeDenominator: %.10f", divergeDenominator));
      LOGGER.info(String.format("onPasS1Denominator: %.10f", onPasS1Denominator));
      LOGGER.info(String.format("onPasS2Denominator: %.10f", onPasS2Denominator));
      LOGGER.info(String.format("mergeDenominator: %.10f", mergeDenominator));
      LOGGER.info(String.format("*Denominator: %.10f", denominator));
      LOGGER.info(String.format("*numerator: %.10f", numerator));

      if(Double.isNaN(denominator)){
        LOGGER.severe("Found denominator being a NaN should never happen");
      }
    }


    double flowShift = 0;
    boolean pasCostEqual = pas.isCostEqual(EPSILON);

    var lowCostSlackResult = determinePasAlternativeSlackFlow(networkLoading, true);
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
   * @return total shifted flow
   */
  public double performEquilibratedUncongestedFlowShifts(
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
      return 0.0;
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
    double totalPasShift = 0;
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
          theMode,
          conjStrategy.getPhysicalCost(),
          conjStrategy.getVirtualCost(),

          networkLoading,
          guaranteedS2SendingFlow,
          logAll);
      double rawProposedFlowShift = proposedShiftResult.values().iterator().next();
      double proposedFlowShift = Math.min(rawProposedFlowShift, guaranteedS2SendingFlow); // truncate to what is available

      double s1SlackFlow = determinePasAlternativeSlackFlow(networkLoading, true).first();
      if(proposedFlowShift > s1SlackFlow){
        //todo: we already adjust for discontinuities in method, probably better to split that out so we do not
        // do that and only do it here for uncongested!

        // insufficient slack, do not process (further) - mark for congested processing
        pas.updateStatus(PasStatus.UNCONGESTED_WITHOUT_SHIFT);
        break;
      }
      pas.updateStatus(PasStatus.UNCONGESTED_WITH_SHIFT);
      if(isDestinationTrackedForLogging() || logAll) {
        LOGGER.info("* UNCONGESTED FLOW SHIFT on PAS:" + pas + " - S2 flow: " + guaranteedS2SendingFlow + " - cost-diff: " + pas.getReducedCost());
      }

      double iterationPasShift = executePasFlowShiftNoNodeModelUpdate(
          guaranteedS2SendingFlow,
          bushS2RemainingSendingFlows,
          proposedFlowShift,
          theMode,
          conjStrategy,
          originalNetworkCosts,
          conjSegmentCosts,
          bushes,
          logAll);
//
//      double totalPasShift = 0;
//      for (var entry : bushS2RemainingSendingFlows.entrySet()) {
//        ConjugateDestinationBush conjBush = entry.getKey();
//        double bushS2RemainingSendingFlow = entry.getValue();
//
//        // scale to bush while minimising risk of rounding issues near zero s2 flow
//        double bushS2Portion = bushS2RemainingSendingFlow / guaranteedS2SendingFlow;
//        double bushRawProposedFlowShift  = rawProposedFlowShift * bushS2Portion;
//        double bushPasFlowShift = Math.min(bushRawProposedFlowShift, bushS2RemainingSendingFlow);
//
//        if(isDestinationTrackedForLogging(conjBush) || logAll) {
//          LOGGER.info(String.format(
//              "     Uncongested Shift: %.9f (available flow %.9f) - bush (%s) ",
//              bushPasFlowShift, bushS2RemainingSendingFlow,conjBush.getRootZone().getIdsAsString()));
//        }
//
//        /* perform the flow shift IN FULL for S1 and S2 for the current bush and its attributed portion */
//        // todo: for now use general flow shift, but can be optimised since we know no acceptance factors are needed
//        executeBushS2FlowShiftNoNodeModelUpdate(
//            conjBush, null, bushPasFlowShift, theMode, conjStrategy, originalNetworkCosts, conjSegmentCosts, bushes);
//
//        executeBushS1FlowShiftNoNodeModelUpdate(
//            conjBush, null, bushPasFlowShift, theMode, conjStrategy, null, originalNetworkCosts, conjSegmentCosts, bushes);
      totalPasShift += iterationPasShift;
      flowShifted = flowShifted || totalPasShift>0;

      // sync costs to changes in flow, to allow for next proposed flow update
      boolean costSwitch = false;
      {
        // sync local PAS cost based on synced network costs
        costSwitch = pas.updateCost(conjSegmentCosts);
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
      converged = pasGap <= conjStrategy.getGapFunction().getGap();
      ++internalIteration;

      doNotStop = !converged && internalIteration <= MAX_INTERAL_ITERATIONS_ALLOWED;

      // remove zero-flow S2 bushes from PAS when we know they won't get used again, or it is the final iteration
      if(!costSwitch || !doNotStop) {
        removeZeroFlowBushesFromPas(false /* no dangling nodes */);
      }
    }while(doNotStop);

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
    int MAX_INTERAL_ITERATIONS_ALLOWED = 1; // set to one as we're trying loop one level higher up
    //int MAX_INTERAL_ITERATIONS_ALLOWED = 10;
    int internalIteration = 3;
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
          theMode,
          conjStrategy.getPhysicalCost(),
          conjStrategy.getVirtualCost(),

          networkLoading,
          guaranteedS2SendingFlow,
          logAll);
      double rawProposedFlowShift = proposedShiftResult.values().iterator().next();

      // TODO TODO
      // SEE SIOUX FALLS ON WHY --> SHIFT of >100 --> becomes ~5 due to discotninuity crossing --> then
      // gets smoothed to ~0.5 WRONG --> WE SHOULD FIRST SMOOTH so ~100->10 --> then reduce due to discontinuity to ~5
      //MOVE SMOOTHING INTO PROPOSED FLOW SHIFT --> APPLY BEFORE DISCONTINUITY ADJUSTMENT!

      //todo if possible get rid of smoothing
      double smoothedRawPasflowShift = conjStrategy.getSmoothing().executeRefZero(rawProposedFlowShift);

      if(isDestinationTrackedForLogging() || logAll) {
        LOGGER.info("* FLOW SHIFT on PAS:" + pas + " - S2 flow: " + guaranteedS2SendingFlow + " - cost-diff: " + pas.getReducedCost());
        LOGGER.info(String.format("  Raw Proposed shift: %.10f Smoother proposed shift: %.10f",rawProposedFlowShift,smoothedRawPasflowShift));
        LOGGER.info("   s1 alphas: "+
            Arrays.stream(s1Alternative).filter(ConjugateEdgeSegment::hasOriginalEntryEdgeSegment).map(
                es -> String.format("%s:%.6f",
                    es.getXmlId(), networkLoading.getCurrentFlowAcceptanceFactors()[
                        (int) es.getOriginalAdjacentEdgeSegments().first().getId()])).collect(Collectors.joining(",")));
        LOGGER.info("   s2 alphas: "+
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

      double appliedFlowShift = executePasFlowShiftNodeModelUpdate(
          guaranteedS2SendingFlow,
          bushS2RemainingSendingFlows,
          smoothedPasflowShift,
          theMode,
          originalNlConsistentFlowAcceptanceFactors,
          conjStrategy,
          originalNetworkCosts,
          conjSegmentCosts,
          (Set<ConjugateDestinationBush>) bushes,
          logAll);

//      //todo: below shifts to be removed when above is functioning
//
//      /* perform the flow shift IN FULL for S1 and S2 for the current bush and its attributed portion */
//        executeBushS2FlowShiftNodeModelUpdate(
//            conjBush, bushPasFlowShift, theMode, originalNlConsistentFlowAcceptanceFactors, conjStrategy, originalNetworkCosts, conjSegmentCosts, (Set<ConjugateDestinationBush>) bushes);
//        executeBushS1FlowShiftNodeModelUpdate(
//            conjBush,  bushPasFlowShift, theMode, originalNlConsistentFlowAcceptanceFactors, conjStrategy, originalNetworkCosts, conjSegmentCosts, (Set<ConjugateDestinationBush>) bushes);
      totalPasShift += appliedFlowShift;

      // sync costs to changes in flow, to allow for next proposed flow update
      boolean costSwitch = pas.updateCost(conjSegmentCosts);

      s1SendingFlow += appliedFlowShift;
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

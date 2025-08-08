package org.goplanit.assignment.ltm.sltm;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.ToDoubleFunction;
import java.util.logging.Logger;

import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingBushBase;
import org.goplanit.cost.physical.AbstractPhysicalCost;
import org.goplanit.cost.virtual.AbstractVirtualCost;
import org.goplanit.gap.GapFunction;
import org.goplanit.sdinteraction.smoothing.Smoothing;
import org.goplanit.utils.arrays.ArrayUtils;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.IterableUtils;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.misc.Triple;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.virtual.physical.ConnectoidSegment;
import org.goplanit.utils.pcu.PcuCapacitated;
import org.ojalgo.array.Array1D;

import static org.goplanit.utils.math.Precision.*;

/**
 * Functionality to conduct a PAS flow shift based on underlying destination based bush approach. A destination-based bush approach no longer requires labelling and should therefore outperform
 * origin-based alternatives.
 * 
 * @author markr
 *
 */
public class PasFlowShiftDestinationBasedExecutor extends PasFlowShiftExecutor<DirectedVertex, EdgeSegment> {

  /**
   * Logger to use
   */
  private final static Logger LOGGER = Logger.getLogger(PasFlowShiftDestinationBasedExecutor.class.getCanonicalName());

  /** Track the desired sending flows for s1 and s2 per bush per entry segment */
  private final Map<DestinationBush, Map<EdgeSegment, Pair<Double, Double>>> bushEntrySegmentS1S2SendingFlows;

  private void removeZeroFlowS2Bushes(Map<DestinationBush, Map<EdgeSegment, Double>> bushEntrySegments2UpdatedFlow) {
    var iter = pas.getRegisteredBushes().iterator();
    while (iter.hasNext()) {
      DestinationBush bush = (DestinationBush) iter.next();

      // use updated flows as these are always either smaller or larger, when larger they are inconsistent but that is fine
      // because it won't lead to removals, when smaller they are more restrictive and indicate possible removals so we
      // use it
      final Map<EdgeSegment, Double> entrySegmentS1S2Flows = bushEntrySegments2UpdatedFlow.get(bush);
      if (entrySegmentS1S2Flows==null || entrySegmentS1S2Flows.values().stream().noneMatch(d -> d > EPSILON_6 )) {
        if(isDestinationTrackedForLogging(bush)){
          LOGGER.info(String.format("   [Removing bush (%s) from PAS %s, no more s2 flow left]",
                  bush.getRootZoneVertex().getParent().getParentZone().getIdsAsString(), pas));
        }
        iter.remove();
      }else if(entrySegmentS1S2Flows.values().stream().noneMatch(d -> d > 1)){
//        LOGGER.info(String.format("   [KEEPING bush (%s) on PAS %s, yet VERY LOW s2 flow remaining (less than 1 per entry segment)]",
//                bush.getRootZoneVertex().getParent().getParentZone().getIdsAsString(), pas));
      }
    }
  }

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
          DestinationBush bush, EdgeSegment turnEntry, EdgeSegment turnExit, double flowShiftPcuH) {

    // track what edge segments were added to what bush, so we can (in case of overlapping PAS update allowance)
    // flag if additional cycle checks are needed for subsequent PASs that may not be compatible with this current
    // PAS that we chose to prefer over those later ones
    if(flowShiftPcuH > 0){
      if(!bush.contains(turnEntry.getId())){
        addBushAddedLinkSegment(bush, turnEntry);
      }
      if(!bush.contains(turnExit.getId())){
        addBushAddedLinkSegment(bush, turnExit);
      }
    }
    // when we are reducing flow (negative flow shift) --> avoid rounding issues, ugly but necessary...
    else {

      // ...and the turn entry link segment was removed from the bush in
      // the previous shift then we should remove all turn sending flow. By explicitly setting this value we avoid rounding issues
      // and ensures that high cost segment flows get removed in its entirety when we no longer route flow through them
      // todo: now that we explicitly check for this earlier, this should not be necessary anymore!
      if(!bush.contains(turnEntry)){
        var availableFlow = bush.getTurnSendingFlow(turnEntry, turnExit);
        if(Precision.smaller(availableFlow, -flowShiftPcuH, Precision.EPSILON_6)) {
          LOGGER.severe(String.format("adding %.8f to flow shift (%.10f) to empty already removed turn (from: %s, to: %s) when removing turn flow" +
                          "from bush (%s)",
              availableFlow + flowShiftPcuH, -availableFlow, turnEntry.getIdsAsString(), turnExit.getIdsAsString(), bush.getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
          flowShiftPcuH = -availableFlow; // sync
        }
      }
      double totalSendingFlowIntoExit = IterableUtils.asStream(turnExit.getUpstreamVertex().getEntryEdgeSegments()).mapToDouble(
          es -> bush.getTurnSendingFlow(es, turnExit)).sum();
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
    return bush.addTurnSendingFlow(turnEntry, turnExit, flowShiftPcuH);
  }

  /**
   * Perform the flow shift through the end merge vertex of the PASs high cost segment for the given origin bush flow composition
   * 
   * @param bush                      to use
   * @param s2FinalFlowShift          the flow shift applied so far up to the final merge
   * @param s2MergeExitSplittingRates splitting rates to use by exit segment index seen from vertex
   * @return exitShiftedSendingFlows  found exit segment flows
   */
  private double[] executeBushS2FlowShiftEndMerge(
          DestinationBush bush, double s2FinalFlowShift, double[] s2MergeExitSplittingRates) {

    var exitShiftedSendingFlows = new double[pasMergeVertexNumExitSegments];
    /* remove shifted flows through final merge towards exit segments proportionally, to later add to s1 turns through merge */
    if (pas.getMergeVertex().hasExitEdgeSegments()) {

      var lastS2Segment = pas.getLastEdgeSegment(false /* high cost */);

      /* key: [exitSegment, exitLabel] */
      int index = 0;
      for (var exitSegment : pas.getMergeVertex().getExitEdgeSegments()) {
        double splittingRate = s2MergeExitSplittingRates[index];
        if (!Precision.positive(splittingRate, EPSILON)) {
          ++index;
          continue;
        }

        /* remove flow for s2 */
        double s2FlowShift = s2FinalFlowShift * splittingRate;

        // precision sync like in regular flow shift
        double currentFlow = bush.getTurnSendingFlow(lastS2Segment, exitSegment);
        if(currentFlow + s2FlowShift < 0){
          double diff= currentFlow + s2FlowShift;
          if(Math.abs(diff)>1){
            int bla = 4;
          }
          s2FlowShift = -currentFlow; // sync to available flow
        }

        bush.addTurnSendingFlow(lastS2Segment, exitSegment, s2FlowShift);

        /* track so we can attribute it to s1 segment later */
        exitShiftedSendingFlows[index] += -s2FlowShift;
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
  private void executeBushS1FlowShiftEndMerge(
          DestinationBush bush, double s1FinalFlowShift, double[] exitSegmentSplittingRates) {

    /* add shifted flows through final merge towards exit segments proportionally based on labelled exit usage */
    if (pas.getMergeVertex().hasExitEdgeSegments()) {
      EdgeSegment lastS1Segment = pas.getLastEdgeSegment(true /* low cost */);

      int index = 0;
      for (var exitSegment : pas.getMergeVertex().getExitEdgeSegments()) {
        double splittingRate = exitSegmentSplittingRates[index];
        if (Precision.positive(splittingRate, EPSILON)) {
          /* add flow for s1 */
          double s1FlowShift = s1FinalFlowShift * splittingRate;
          double newTurnFlow = bush.addTurnSendingFlow(lastS1Segment, exitSegment, s1FlowShift);
          if (!Precision.positive(newTurnFlow, EPSILON)) {
            LOGGER.severe(String.format(
                "Flow shift of (%.12f) towards cheaper S1 alternative on turn [from (%s), to (%s)] should result in non-negative flow, but found %.12f, this shouldn't happen",
                s1FlowShift, lastS1Segment.getIdsAsString(), exitSegment.getIdsAsString(), newTurnFlow));
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
          final DestinationBush bush,
          final EdgeSegment entrySegment,
          double flowShiftPcuH,
          final EdgeSegment[] pasSegment,
          final double[] flowAcceptanceFactors) {

    /* initial turn into pas segment */
    int index = 0;
    EdgeSegment currentSegment = entrySegment;
    var nextSegment = pasSegment[index];

    double currentFlow = bush.getTurnSendingFlow(currentSegment, nextSegment);
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

      currentFlow = bush.getTurnSendingFlow(currentSegment, nextSegment);
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

  protected Double getTotalEntrySegmentSendingFlow(EdgeSegment entrySegment, boolean lowCost){
    ToDoubleFunction<Pair<Double,Double>> flowSupplier = lowCost ? Pair::first : Pair::second;
    return bushEntrySegmentS1S2SendingFlows.values().stream().map(
            entry -> entry.getOrDefault(entrySegment, Pair.of(0.0,0.0))).mapToDouble(flowSupplier).sum();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected EdgeSegment findFirstCongestedEdgeSegmentOnPasAlternative(
          final StaticLtmLoadingBushBase<?> networkLoading, boolean lowCost) {

    EdgeSegment[] alternative = pas.getAlternative(lowCost);
    int index = 0;
    EdgeSegment currSegment = alternative[index++];
    EdgeSegment nextSegment = null;
    for (; index < alternative.length; ++index) {
      nextSegment = alternative[index];
      if (isCongested(networkLoading , currSegment)) {
        return currSegment;
      }
      currSegment = nextSegment;
    }
    return null;
  }

  @Override
  protected Triple<Double, Double, Boolean> getDTravelTimeDFlowExcludingMergeDiverge(
          final Mode theMode,
          final StaticLtmLoadingBushBase<?> networkLoading,
          final AbstractPhysicalCost physicalCost,
          final AbstractVirtualCost virtualCost,
          boolean isLowCostAlternative,
          double derivativeReductionFactor) {

    // allowChainingBeyondBottleneck not used yet --> ignored

    double dTravelTimeDFlow = 0.0;

    var pasAlternative = this.pas.getAlternative(isLowCostAlternative);
    int index = 0;
    var currSegment = pasAlternative[index++];
    while(index <= pasAlternative.length){
      var nextSegment = index<pasAlternative.length ? pasAlternative[index] : null;

      boolean unCongested;
      if(nextSegment == null){
        // check all exit segments using threshold to apply to curr segment state
        unCongested = !isCongested(
                networkLoading, currSegment, UNCONGESTED_AS_CONGESTED_FLOW_THRESHOLD_PCUH).anyMatch((Boolean e) -> e);
      }else{
        // check segment on congestion or near-congestion on next segment (using threshold)
        unCongested = !isCongested(networkLoading, currSegment);
        if(unCongested){
          unCongested = !isNearCongested(networkLoading, nextSegment, UNCONGESTED_AS_CONGESTED_FLOW_THRESHOLD_PCUH);
        }
      }

      double currDTravelTimeDFlow = 0.0;
      if (currSegment instanceof MacroscopicLinkSegment) {
        currDTravelTimeDFlow =
                physicalCost.getDTravelTimeDFlow(unCongested, theMode, (MacroscopicLinkSegment) currSegment);
      } else if (currSegment instanceof ConnectoidSegment) {
        currDTravelTimeDFlow =
                virtualCost.getDTravelTimeDFlow(unCongested, theMode, (ConnectoidSegment) currSegment);
      } else {
        LOGGER.severe(String.format("Unsupported edge segment (%s) to obtain derivative of cost towards flow from",
                currSegment.getIdsAsString()));
      }

      dTravelTimeDFlow += currDTravelTimeDFlow;

      if(!unCongested){
        // no more flow change beyond here due to it being a bottleneck
        break;
      }
      currSegment = nextSegment;
      ++index;
    }
    return Triple.of(dTravelTimeDFlow,1.0 /*dummy*/,true /*dummy*/);
  }



  /**
   * {@inheritDoc}
   */
  @Override
  protected Pair<Double,EdgeSegment>  determinePasAlternativeSlackFlow(
          StaticLtmLoadingBushBase<?> networkLoading, double proposedFlowShift, boolean lowCost) {

    var lastAlternativeSegment = pas.getLastEdgeSegment(lowCost);
    double slackFlow = Double.POSITIVE_INFINITY;

    Array1D<Double> splittingRates =
            networkLoading.getSplittingRateData().getSplittingRates(lastAlternativeSegment);
    //todo: add in the splitting rates of the low cost segment, since any exit flow their will be moved to the high cost
    // and distributed accordingly, so we cannot just consider the current state of the low cost here...

    int index = 0;
    int linkSegmentId = -1;
    EdgeSegment minSlackSegment = null;

    for (var exitSegment : lastAlternativeSegment.getDownstreamVertex().getExitEdgeSegments()) {
      double splittingRate = splittingRates.get(index);
      if (splittingRate > 0) {
        linkSegmentId = (int) exitSegment.getId();
        /* do not use outflows directly because they are only available on potentially blocking nodes in point queue basic solution scheme */
        var nextInflow = networkLoading.getCurrentInflowsPcuH()[(int) exitSegment.getId()];
        double currSlackFlow = ((PcuCapacitated) exitSegment).getCapacityOrDefaultPcuH() - nextInflow;
        if(currSlackFlow < slackFlow){
          minSlackSegment = exitSegment;
          slackFlow = currSlackFlow;
        }
      }
      ++index;
    }

    if (!Precision.positive(slackFlow, EPSILON)) {
      return Pair.of(0.0, minSlackSegment);
    }

    EdgeSegment alternativeEdgeSegment = null;
    EdgeSegment[] alternativeEdgeSegments = pas.getAlternative(lowCost);
    for (index = 0; index < alternativeEdgeSegments.length; ++index) {
      alternativeEdgeSegment = alternativeEdgeSegments[index];
      linkSegmentId = (int) alternativeEdgeSegment.getId();
      /* do not use outflows directly because they are only available on potentially blocking nodes in point queue basic solution scheme */
      double inflow = networkLoading.getCurrentInflowsPcuH()[linkSegmentId];
      double currSlackFlow = ((PcuCapacitated) alternativeEdgeSegment).getCapacityOrDefaultPcuH() - inflow;
      if(currSlackFlow < slackFlow){
        minSlackSegment = alternativeEdgeSegment;
        slackFlow = currSlackFlow;
      }
    }

    return Pair.of(0.0, minSlackSegment);
  }

  /**
   * For the given PAS-entrysegment determine the flow shift to apply from the high cost to the low cost segment.
   * Depending on the state of the segments we utilise their derivatives of travel time towards flow to determine the
   * optimal shift. In case one or both segments are uncongested, or the congestion occurs on the entry segment while
   * the cost on the PAS is already equal, we propose to shift as much flow as would yield an equal distribution
   * between the alternatives (maximising entropy) in order to obtain a unique solution under equal cost.
   * Would expect the segment to transition to congestion.
   *
   * @param entrySegment                 to use
   * @param theMode                      to use
   * @param physicalCost                 to use
   * @param virtualCost                  to use
   * @param networkLoading               to use
   * @return amount of flow to shift
   */
  protected double determineEntrySegmentFlowShift(
          EdgeSegment entrySegment,
          Mode theMode,
          AbstractPhysicalCost physicalCost,
          AbstractVirtualCost virtualCost,
          StaticLtmLoadingBushBase<?> networkLoading) {
    // 1% of capacity is accepted as leeway for state change undicing flow shifts
    double stateChangeLeewayPercentage = 0.01;

    double denominatorS2 = 0;
    double denominatorS1 = 0;

    /* get first congested edge segment that is affected when shifting flow, per alternative */
    var firstS1CongestedSegment = findFirstCongestedEdgeSegmentOnPasAlternative(networkLoading, true);
    var firstS2CongestedSegment = findFirstCongestedEdgeSegmentOnPasAlternative(networkLoading, false);

    // not rewritten to merge diverge excluded yet, all in one here
    denominatorS1 = getDTravelTimeDFlowExcludingMergeDiverge(
            theMode, networkLoading, physicalCost, virtualCost, true, 1).first();
    denominatorS2 = getDTravelTimeDFlowExcludingMergeDiverge(
            theMode, networkLoading, physicalCost, virtualCost, false, 1).first();

    double flowShift = 0;
    boolean pasCostEqual = pas.isCostEqual(EPSILON);
    double s2TotalEntrySendingFlow = getTotalEntrySegmentSendingFlow(entrySegment, false);
    if (!pasCostEqual && smaller(denominatorS2,EPSILON) && smaller(denominatorS2, EPSILON)) {
      /* s1 & S2 UNCONGESTED - no derivative estimate possible (denominator zero) */
      /* move all towards cheaper alternative limited by slack + delta */
      /* obtain PAS-entry segment sub-path sending flows */
      flowShift = s2TotalEntrySendingFlow;
    }else {

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
    }
    var lowCostSlackResult = determinePasAlternativeSlackFlow(networkLoading, flowShift, true);
    double s1SlackFlowEstimate = lowCostSlackResult.first();
    double s1SlackFlowLeeway = ((PcuCapacitated) lowCostSlackResult.second()).getCapacityOrDefaultPcuH() * stateChangeLeewayPercentage;

    // VERIFY CROSSING OF DISCONTINUITY on S1  - adjust shift if so to mitigate effect
    // This is triggered when S1 alternative segments are not congested yet, or near congestion, or the entry segment
    // is not congested or near congestion
    var pasEntrySegmentCongestedResult =
            isCongested(networkLoading, entrySegment, UNCONGESTED_AS_CONGESTED_FLOW_THRESHOLD_PCUH);
    boolean pasEntrySegmentDirectlyCongested = pasEntrySegmentCongestedResult.first();
    boolean pasEntrySegmentPotentialDiscontinuity =
            !pasEntrySegmentDirectlyCongested && pasEntrySegmentCongestedResult.second();
    boolean pasS1PotentialDiscontinuity = !pasEntrySegmentDirectlyCongested && firstS1CongestedSegment == null;
    if (pasEntrySegmentPotentialDiscontinuity || pasS1PotentialDiscontinuity) {
      /* possible triggering of congestion on s1 due to shift -> passing discontinuity on travel time function */
      flowShift = adjustFlowShiftBasedOnS1SlackFlow(flowShift, s1SlackFlowEstimate, s1SlackFlowLeeway);
    }

    // VERIFY CROSSING OF DISCONTINUITY on S2 travel time function - adjust shift if so to mitigate effect
    // todo: see conjugate if we are to every update this, this is no longer correct
    if (firstS2CongestedSegment != null || pasEntrySegmentDirectlyCongested) {
      var refSegment = pasEntrySegmentDirectlyCongested ? entrySegment : firstS2CongestedSegment;
      double s2DeltaFlowToStateChangeEstimate = -1;

      s2DeltaFlowToStateChangeEstimate =
              networkLoading.getCurrentInflowsPcuH()[(int) refSegment.getId()] *
                      (1 - networkLoading.getCurrentFlowAcceptanceFactors()[(int) refSegment.getId()]);
      double s2SlackFlowLeeway = ((PcuCapacitated) refSegment).getCapacityOrDefaultPcuH() * stateChangeLeewayPercentage;
      flowShift = adjustFlowShiftBasedOnS2SlackFlow(flowShift, s2DeltaFlowToStateChangeEstimate, s2SlackFlowLeeway);
    }

    // make sure we never shift more than flow than available
    flowShift = Math.min(flowShift, s2TotalEntrySendingFlow);

    return flowShift;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  protected double[] executeBushS2FlowShiftNoNodeModelUpdate(
          RootedBush<DirectedVertex, EdgeSegment> bush,
          EdgeSegment entrySegment,
          double bushFlowShift,
          Mode theMode,
          StaticLtmAssignmentStrategy assignmentStrategy,
          double[] originalNetworkCosts,
          double[] conjSegmentCosts,
          Set<? extends RootedBush<?,?>> bushes) {

    var networkLoading = assignmentStrategy.getLoading();
    var destinationBush = (DestinationBush)bush;
    /* prep - pas */
    final var s2 = pas.getAlternative(false);

    /*
     * ------------------------------------------------- S2 FLOW SHIFT ----------------------------------------------------------------------------------------------------------
     * Update S2 by shifting flow proportionally along encountered flow compositions matching with the PAS/origin/alternative
     */

    /* obtain splitting rates before flow shift in case turns/edges are removed on S2, then splitting rate information is lost while required for final merge afterwards */
    var s2MergeExitSplittingRates = destinationBush.getSplittingRates(pas.getLastEdgeSegment(false /* high cost */));

    double s2StartLabeledFlowShift = -bushFlowShift;
    double s2FinalLabeledFlowShift =
            executeBushPasFlowShift(destinationBush, entrySegment, s2StartLabeledFlowShift, s2, networkLoading.getCurrentFlowAcceptanceFactors());

    /* shift flow across final merge for S2 */
    double[] bushS2MergeExitShiftedSendingFlows =
            executeBushS2FlowShiftEndMerge(destinationBush, s2FinalLabeledFlowShift, s2MergeExitSplittingRates);

    var endMergeSplittingRates = ArrayUtils.divideBySum(bushS2MergeExitShiftedSendingFlows, 0);
    if(Precision.smaller(ArrayUtils.sumOf(endMergeSplittingRates),1, Precision.EPSILON_6)){
      LOGGER.warning(String.format(
              "Sum of splitting rates at any segment should always be 1 on bush %s, but it is not, this shouldn't happen",
              bush.getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
    }
    /* convert flows to splitting rates  */
    return endMergeSplittingRates;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeBushS1FlowShiftNoNodeModelUpdate(
          RootedBush<DirectedVertex, EdgeSegment> bush,
          EdgeSegment entrySegment,
          double bushFlowShift,
          Mode theMode,
          StaticLtmAssignmentStrategy assignmentStrategy,
          double[] mergeExitSplittingRates,
          double[] originalNetworkCosts,
          double[] conjSegmentCosts,
          Set<? extends RootedBush<?,?>> bushes) {
    var destinationBush = (DestinationBush)bush;

    var s1 = pas.getAlternative(true);

    double s1FinalLabeledFlowShift = executeBushPasFlowShift(
            destinationBush,
            entrySegment,
            bushFlowShift,
            s1,
            assignmentStrategy.getLoading().getCurrentFlowAcceptanceFactors());

    /* shift flow across final merge for S1 based on findings in s2 */
    executeBushS1FlowShiftEndMerge(destinationBush, s1FinalLabeledFlowShift, mergeExitSplittingRates);
  }

  /**
   * Constructor
   * 
   * @param pas      to use
   * @param settings to use
   */
  protected PasFlowShiftDestinationBasedExecutor(final Pas pas, final StaticLtmSettings settings) {
    super(pas, settings);
    this.bushEntrySegmentS1S2SendingFlows = new HashMap<>();
  }

  /**
   * Sending flow along PAS high cost segment
   *
   * @return high cost alternative desired flow
   */
  @Override
  public double getS2SendingFlow() {
    // aggregate over bushes and entry segments
    return bushEntrySegmentS1S2SendingFlows.values().stream().flatMap(
            e -> e.values().stream()).mapToDouble(Pair::second).sum();
  }

  /**
   * Sending flow along PAS low cost segment
   *
   * @return low cost alternative desired flow
   */
  @Override
  public double getS1SendingFlow() {
    return bushEntrySegmentS1S2SendingFlows.values().stream().flatMap(
            e -> e.values().stream()).mapToDouble(Pair::first).sum();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void stepOneDetermineNetworkLoadingConsistentS1S2SendingFlows(double[] flowAcceptanceFactors) {

    var s2 = pas.getAlternative(false /* high cost */);
    var s1 = pas.getAlternative(true /* low cost */);

    bushEntrySegmentS1S2SendingFlows.clear();
    for (var entrySegment : pas.getDivergeVertex().getEntryEdgeSegments()) {
      for (var bush : pas.getRegisteredBushes()) { //todo: stopgap cast
        DestinationBush destinationBush = (DestinationBush) bush;
        if (!destinationBush.contains(entrySegment)) {
          continue;
        }
        bushEntrySegmentS1S2SendingFlows.putIfAbsent(destinationBush, new HashMap<>());
        var entrySegmentS1S2SendingFlows = bushEntrySegmentS1S2SendingFlows.get(bush);

        double s2BushSendingFlow = destinationBush.determineSubPathSendingFlow(entrySegment, s2, flowAcceptanceFactors);

        double s1BushSendingFlow = destinationBush.determineSubPathSendingFlow(entrySegment, s1, flowAcceptanceFactors);

        entrySegmentS1S2SendingFlows.put(entrySegment, Pair.of(s1BushSendingFlow, s2BushSendingFlow));
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<EdgeSegment, Double> determineProposedFlowShiftByLoadingEntrySegment(
      Mode theMode,
      GapFunction gapFunction, AbstractPhysicalCost physicalCost,
      AbstractVirtualCost virtualCost,
      Smoothing smoothing,
      double additionalSmoothingFactor,
      StaticLtmLoadingBushBase<?> networkLoading,
      double guaranteedS2SendingFlow,
      boolean logAll) { // dummy for compliance

    Map<EdgeSegment, Double> result = new TreeMap<>();
    for (var entrySegment : pas.getDivergeVertex().getEntryEdgeSegments()) {
      double proposedPasFlowShift = 0;
      double totalEntrySegmentS2Flow = getTotalEntrySegmentSendingFlow(entrySegment, false);
      if (totalEntrySegmentS2Flow > 0) {
        /* flow shift based on entry segment - PAS combination */
        proposedPasFlowShift = determineEntrySegmentFlowShift(
                entrySegment,
                theMode,
                physicalCost,
                virtualCost,
                networkLoading);
      }
      result.put(entrySegment, proposedPasFlowShift);
    }
    return result;
  }

  @Override
  public Pair<EdgeSegment,Double> performEquilibratedCongestedFlowShifts(
      Mode theMode,
      StaticLtmAssignmentStrategy assignmentStrategy,
      double[] originalNetworkCosts,
      double[] conjSegmentCosts,
      double[] originalNlConsistentFlowAcceptanceFactors,
      Set<? extends RootedBush<?,?>> bushes,
      boolean logAll,
      FlowShiftSmoothingApproach smoothingApproach,
      double additionalSmoothingFactor) {
    throw new PlanItRunTimeException("performEquilibratedCongestedS2FlowShift not yet implemented on " +
        "non-conjugate destination based ");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void performAllBushesRecordedOneShotS1FlowShift(
      Mode theMode,
      StaticLtmAssignmentStrategy assignmentStrategy) {

    //    if(getFlowShiftedS2BushData().values().stream().flatMap(
//            e -> e.keySet().stream()).anyMatch(this::isDestinationTrackedForLogging)) {
//      LOGGER.info(String.format("* S1 FLOW SHIFT on PAS: %s", pas));
//    }

    for (var entry : getFlowShiftedS2BushData().entrySet()) {
      var entrySegment = entry.getKey();
      for (var bushFlowEntry : entry.getValue().entrySet()) {
        RootedLabelledBush bush = (RootedLabelledBush) bushFlowEntry.getKey(); //todo: stopgap cast
        var flowShiftData = bushFlowEntry.getValue();

        if(isDestinationTrackedForLogging(bush)) {
          LOGGER.info(String.format("        Flow to shift: %.8f - entry segment (%s) - alpha: %.2f - bush (%s)",
                  flowShiftData.getS2Flowshifted(),
                  entrySegment.getIdsAsString(),
                  assignmentStrategy.getLoading().getCurrentFlowAcceptanceFactors()[(int) entrySegment.getId()],
                  bush.getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
        }

        executeBushS1FlowShiftNoNodeModelUpdate(
                bush,
                entrySegment,
                flowShiftData.getS2Flowshifted(),
                theMode,
                assignmentStrategy,
                flowShiftData.getS2MergeExitSplittingRates(),
            null,null,null);
      }
    }
  }

}

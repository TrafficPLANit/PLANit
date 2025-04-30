package org.goplanit.assignment.ltm.sltm;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.ToDoubleFunction;
import java.util.logging.Logger;

import org.goplanit.assignment.ltm.sltm.conjugate.ConjugateDestinationBush;
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
  protected Pair<EdgeSegment, Boolean> findFirstCongestedEdgeSegmentOnPasAlternative(
          final StaticLtmLoadingBushBase<?> networkLoading, boolean lowCost, boolean ignoreFirstSegment) {
    assert(!ignoreFirstSegment);

    EdgeSegment[] alternative = pas.getAlternative(lowCost);
    int index = 0;
    EdgeSegment currSegment = alternative[index++];
    EdgeSegment nextSegment = null;
    for (; index < alternative.length; ++index) {
      nextSegment = alternative[index];
      if (isCongested(networkLoading , currSegment)) {
        return Pair.of(currSegment, true);
      }else if(isNearCongested(networkLoading, nextSegment, UNCONGESTED_AS_CONGESTED_FLOW_THRESHOLD_PCUH)){
        return Pair.of(currSegment, false);
      }
      currSegment = nextSegment;
    }

    // treat last segment differently because we must consider all exist segments out of the PAS rather as we have no
    // single next segment
    // todo: check could be made better by considering s1+s2 splitting rates on last segment
    var isCongestedResult =
            isCongested(networkLoading,  currSegment, UNCONGESTED_AS_CONGESTED_FLOW_THRESHOLD_PCUH);
    if(isCongestedResult.first()){
      return Pair.of(currSegment, true); // true congestion match
    }else if(isCongestedResult.second()){
      return Pair.of(currSegment, false); // near congestion match
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
    assert(!ignoreFirstSegment);

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
    return dTravelTimeDFlow;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected double determinePasAlternativeSlackFlow(
          StaticLtmLoadingBushBase<?> networkLoading, boolean lowCost, boolean ignoreInitialSegment) {
    assert (!ignoreInitialSegment);

    var lastAlternativeSegment = pas.getLastEdgeSegment(lowCost);
    double slackFlow = Double.POSITIVE_INFINITY;

    Array1D<Double> splittingRates =
            networkLoading.getSplittingRateData().getSplittingRates(lastAlternativeSegment);
    //todo: add in the splitting rates of the low cost segment, since any exit flow their will be moved to the high cost
    // and distributed accordingly, so we cannot just consider the current state of the low cost here...

    int index = 0;
    int linkSegmentId = -1;

    for (var exitSegment : lastAlternativeSegment.getDownstreamVertex().getExitEdgeSegments()) {
      double splittingRate = splittingRates.get(index);
      if (splittingRate > 0) {
        linkSegmentId = (int) exitSegment.getId();
        /* do not use outflows directly because they are only available on potentially blocking nodes in point queue basic solution scheme */
        var nextInflow = networkLoading.getCurrentInflowsPcuH()[(int) exitSegment.getId()];
        double currSlackFlow = ((PcuCapacitated) exitSegment).getCapacityOrDefaultPcuH() - nextInflow;
        slackFlow = Math.min(slackFlow, currSlackFlow);
      }
      ++index;
    }

    if (!Precision.positive(slackFlow, EPSILON)) {
      return slackFlow;
    }

    EdgeSegment alternativeEdgeSegment = null;
    EdgeSegment[] alternativeEdgeSegments = pas.getAlternative(lowCost);
    for (index = 0; index < alternativeEdgeSegments.length; ++index) {
      alternativeEdgeSegment = alternativeEdgeSegments[index];
      linkSegmentId = (int) alternativeEdgeSegment.getId();
      /* do not use outflows directly because they are only available on potentially blocking nodes in point queue basic solution scheme */
      double inflow = networkLoading.getCurrentInflowsPcuH()[linkSegmentId];
      double currSlackFlow = ((PcuCapacitated) alternativeEdgeSegment).getCapacityOrDefaultPcuH() - inflow;
      slackFlow = Math.min(slackFlow, currSlackFlow);
    }

    return slackFlow;
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
   * @param discontinuityDampeningFactor to use in dampening any flow change in case of potential discontinuity
   *                                     crossing
   * @return amount of flow to shift
   */
  protected double determineEntrySegmentFlowShift(
          EdgeSegment entrySegment,
          Mode theMode,
          AbstractPhysicalCost physicalCost,
          AbstractVirtualCost virtualCost,
          StaticLtmLoadingBushBase<?> networkLoading,
          double discontinuityDampeningFactor) {

    double denominatorS2 = 0;
    double denominatorS1 = 0;

    /* get first congested edge segment that is affected when shifting flow, per alternative */
    boolean ignoreFirstSegment = false;
    var s1FirstCongestedSegmentResult =
            findFirstCongestedEdgeSegmentOnPasAlternative(networkLoading, true, ignoreFirstSegment);
    var s2FirstCongestedSegmentResult =
            findFirstCongestedEdgeSegmentOnPasAlternative(networkLoading, false, ignoreFirstSegment);
    var firstS1CongestedSegment = s1FirstCongestedSegmentResult!= null ? s1FirstCongestedSegmentResult.first() : null;
    var firstS2CongestedSegment = s2FirstCongestedSegmentResult!= null ? s2FirstCongestedSegmentResult.first() : null;

    denominatorS1 = getDTravelTimeDFlow(
            theMode, networkLoading, physicalCost, virtualCost, true, ignoreFirstSegment);
    denominatorS2 = getDTravelTimeDFlow(
            theMode, networkLoading, physicalCost, virtualCost, false, ignoreFirstSegment);

    double flowShift = 0;
    boolean pasCostEqual = pas.isCostEqual(EPSILON);
    double s2TotalEntrySendingFlow = getTotalEntrySegmentSendingFlow(entrySegment, false);
    double slackFlowEstimate = determinePasAlternativeSlackFlow(networkLoading, true, ignoreFirstSegment);
    if (!pasCostEqual && smaller(denominatorS2,EPSILON) && smaller(denominatorS2, EPSILON)) {

      /* s1 & S2 UNCONGESTED - no derivative estimate possible (denominator zero) */
      /* move all towards cheaper alternative limited by slack + delta */
      /* obtain PAS-entry segment sub-path sending flows */
      double proposedFlowShift = Math.min(s2TotalEntrySendingFlow - 10, slackFlowEstimate) + 10;
      return adjustFlowShiftBasedOnS1SlackFlow(proposedFlowShift, slackFlowEstimate, discontinuityDampeningFactor);
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
    // This is triggered when S1 alternative segments are not congested yet, or near congestion, or the entry segment
    // is not congested or near congestion
    var pasEntrySegmentCongestedResult =
            isCongested(networkLoading, entrySegment, UNCONGESTED_AS_CONGESTED_FLOW_THRESHOLD_PCUH);
    boolean pasEntrySegmentDirectlyCongested = pasEntrySegmentCongestedResult.first();
    boolean pasEntrySegmentPotentialDiscontinuity =
            !pasEntrySegmentDirectlyCongested && pasEntrySegmentCongestedResult.second();
    boolean pasS1PotentialDiscontinuity = !pasEntrySegmentDirectlyCongested &&
            (firstS1CongestedSegment == null || !s1FirstCongestedSegmentResult.second());
    if (pasEntrySegmentPotentialDiscontinuity || pasS1PotentialDiscontinuity) {
      /* possible triggering of congestion on s1 due to shift -> passing discontinuity on travel time function */
      flowShift = adjustFlowShiftBasedOnS1SlackFlow(flowShift, slackFlowEstimate, discontinuityDampeningFactor);
    }

    // VERIFY CROSSING OF DISCONTINUITY on S2 travel time function - adjust shift if so to mitigate effect
    if (firstS2CongestedSegment != null || pasEntrySegmentDirectlyCongested) {
      var refSegment = pasEntrySegmentDirectlyCongested ? entrySegment : firstS2CongestedSegment;
      double s2DeltaFlowToStateChangeEstimate = -1;

//      var turnExitSegment = identifyMostRestrictingOutEdgeSegment(refSegment, networkLoading);
//      var criticalExitSplittingRate = networkLoading.getSplittingRateData().getSplittingRate(refSegment, turnExitSegment);

      //TODO: this is not a good estimate since we now assume the link flow as a whole drives the queue, but it may be
      // that a tiny portion on one turn is causing the full reduction factor while the majority of flow would sail through
      // were it not for that small turn flow --> we must compute the discontinuity slack flow for each turn separate and
      // then use the minimum
      // looking at spreadsheet with small exampl it may not be enough either

      s2DeltaFlowToStateChangeEstimate =
              networkLoading.getCurrentInflowsPcuH()[(int) refSegment.getId()] *
                      (1 - networkLoading.getCurrentFlowAcceptanceFactors()[(int) refSegment.getId()]);
      flowShift = adjustFlowShiftBasedOnS2SlackFlow(flowShift, s2DeltaFlowToStateChangeEstimate, discontinuityDampeningFactor);
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
          StaticLtmLoadingBushBase<?> networkLoading) {

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
          StaticLtmLoadingBushBase<?> networkLoading,
          double[] mergeExitSplittingRates) {
    var destinationBush = (DestinationBush)bush;

    var s1 = pas.getAlternative(true);

    double s1FinalLabeledFlowShift = executeBushPasFlowShift(
            destinationBush,
            entrySegment,
            bushFlowShift,
            s1,
            networkLoading.getCurrentFlowAcceptanceFactors());

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
          AbstractPhysicalCost physicalCost,
          AbstractVirtualCost virtualCost,
          StaticLtmLoadingBushBase<?> networkLoading,
          double discontinuityDampeningFactor) {

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
                networkLoading,
                discontinuityDampeningFactor);
      }
      result.put(entrySegment, proposedPasFlowShift);
    }
    return result;
  }

  /**
   * We account for the fact that per bush different incoming links to the PAS might be used so each incoming link that is used and that is congested should be the basis for the
   * flow shift instead of the first congested one within the PAS. This is currently not accounted for + if an incoming link is congested, then it has the same alpha for both
   * alternatives BUT the most restricting one might be linked to one of those. If so then we should shift towards the other! This does not exist yet. If neither is the most
   * restricting then revert to situation where we shift as if uncongested as it has no impact. So, split flow shift and execution to per incoming link rather than combining them
   * as we do in run!! Later we can optimise possibly
   * <p>
   * Each PAS per bush is split in x PASs where x is the number of used in links for each bush
   *
   * @param proposedFlowShifts proposed shifts per entry segment
   * @param theMode            to use
   * @param networkLoading     to use
   * @param smoothing          to apply to flow shift
   * @param logAll             to use
   * @return true when flow is shifted, false otherwise
   */
  @Override
  public boolean performOneShotCongestedS2FlowShift(
          Map<EdgeSegment, Double> proposedFlowShifts,
          Mode theMode,
          StaticLtmLoadingBushBase<?> networkLoading,
          Smoothing smoothing,
          boolean logAll) {

    double totalProposedFlowShift = proposedFlowShifts.values().stream().mapToDouble(d->d).sum();

    // reset and repopulate the actually s2 shifted(removed) flow which is needed to later perform the equivalent s1 flow shifts (by adding)
    getFlowShiftedS2BushData().clear();

    double networkLoadingConsistentS2SendingFlow = getS2SendingFlow(); // consistent with original loading
    if(!Precision.positive(networkLoadingConsistentS2SendingFlow)) {
      // todo: in case of overlapping pas updates this may happen, maybe more elegant way of deaing with it though
      //LOGGER.warning("no flow on S2 segment of selected PAS, PAS should not exist anymore, this shouldn't happen");
    }

    //--------------- UPDATE SENDING FLOWS THROUGH ALTERNATIVE ------------------------------------
    // sub path sending flows current (which is likely different and lower than the ones consistent with loading due to S2 flow shifts
    // performed on other PASs
    Map<DestinationBush, Map<EdgeSegment, Double>> bushEntrySegments2UpdatedFlow = new TreeMap<>();
    for (var bush : pas.getRegisteredBushes()) { // todo: stopgap cast
      DestinationBush destinationBush = (DestinationBush) bush;

      double subPathSendingFlow = destinationBush.determineSubPathSendingFlow(
              pas.getAlternative(false),
          networkLoading.getCurrentFlowAcceptanceFactors());
      var initialS2Segment = pas.getAlternative(false)[0];

      double[] entrySegmentAcceptedFlowIntoS2 = IterableUtils.asStream(
              initialS2Segment.getUpstreamVertex().getEntryEdgeSegments()).mapToDouble(
              entrySegment -> destinationBush.getTurnSendingFlow(entrySegment, initialS2Segment) *
                      networkLoading.getCurrentFlowAcceptanceFactors()[(int) entrySegment.getId()]).toArray();
      // determine the split of this flow across the entry segments on the S2 initial segment to obtain per entry portion
      double[] s2InitialSegmentFlowDistribution = ArrayUtils.divideBySum(entrySegmentAcceptedFlowIntoS2, 0);
      double[] s2EntrySegmentMaximumAvailableAcceptedFlowOnInitialS2Segment =
              ArrayUtils.multiplyBy(s2InitialSegmentFlowDistribution, subPathSendingFlow, true);
      // 4. now work backwards to entry segment sending flow by taking reciprocal of alpha on entry segment. This is
      // the final allowed proportional available sending flow on the subpath that we use
      double[] entrySegmentUpscalingFactors = IterableUtils.asStream(
              initialS2Segment.getUpstreamVertex().getEntryEdgeSegments()).mapToDouble(
              entrySegment -> 1 / networkLoading.getCurrentFlowAcceptanceFactors()[(int) entrySegment.getId()]).toArray();
      double[] s2EntrySegmentSubPathSendingFlows =
              ArrayUtils.multiplyElementWise(
                      s2EntrySegmentMaximumAvailableAcceptedFlowOnInitialS2Segment, entrySegmentUpscalingFactors);
      int index = 0;
      var currBushEntrySegmentSendingFlow = new TreeMap<EdgeSegment, Double>();
      for (var entrySegment : initialS2Segment.getUpstreamVertex().getEntryEdgeSegments()) {
        currBushEntrySegmentSendingFlow.put(entrySegment, s2EntrySegmentSubPathSendingFlows[index++]);
      }
      bushEntrySegments2UpdatedFlow.put(destinationBush, currBushEntrySegmentSendingFlow);
    }

    double currentS2SendingFlow = bushEntrySegments2UpdatedFlow.values().stream().flatMap(
            e -> e.values().stream()).mapToDouble(e -> e).sum();

//    if (Precision.greater(totalProposedFlowShift, currentS2SendingFlow, EPSILON_3)) {
//      if(isDestinationTrackedForLogging()) {
//        LOGGER.info(String.format("[removal --> proposed shift %.10f exceeds s2 sending flow %.10f]",
//                totalProposedFlowShift, currentS2SendingFlow));
//      }
//    }

    // if earlier shifts have reduced available flow, capture in factor, so we remain in feasible shifting region
    double s2FlowAvailabilityFactor = Math.min(1,currentS2SendingFlow/networkLoadingConsistentS2SendingFlow);
    // minimum of current and original is what we now is available as it is a minimum
    double guaranteedS2SendingFlow = currentS2SendingFlow; //Math.min(currentS2SendingFlow, networkLoadingConsistentS2SendingFlow);

    if(isDestinationTrackedForLogging() || logAll) {
      LOGGER.info("* S2 FLOW SHIFT on PAS:" + pas + " - S2 flow: " + guaranteedS2SendingFlow + "(NL consistent: " + networkLoadingConsistentS2SendingFlow+") - cost-diff: " + pas.getReducedCost());
//      LOGGER.info("s1 alphas: "+
//              Arrays.stream(pas.getAlternative(true)).map(es -> String.format("%s:%.2f",
//                      es.getXmlId(), networkLoading.getCurrentFlowAcceptanceFactors()[(int) es.getId()])).collect(Collectors.joining(",")));
//      LOGGER.info("s2 alphas: "+
//              Arrays.stream(pas.getAlternative(false)).map(es -> String.format("%s:%.2f",
//                      es.getXmlId(), networkLoading.getCurrentFlowAcceptanceFactors()[(int) es.getId()])).collect(Collectors.joining(",")));
    }

    for (var entrySegment : pas.getDivergeVertex().getEntryEdgeSegments()) {
      double nlConsistentEntrySegmentS2Flow = getTotalEntrySegmentSendingFlow(entrySegment, false);
      double currentEntrySegmentS2Flow = bushEntrySegments2UpdatedFlow.values().stream().map(
              entry -> entry.getOrDefault(entrySegment,0.0)).mapToDouble(d->d).sum();
      double guaranteedEntrySegmentS2SendingFlow = currentEntrySegmentS2Flow; //Math.min(nlConsistentEntrySegmentS2Flow, currentEntrySegmentS2Flow);
      if ( guaranteedEntrySegmentS2SendingFlow <= 0.0) {
        /* remove this entry segment from the PAS when done as no flow remains on high cost segment */
        pas.getRegisteredBushes().forEach(b -> bushEntrySegmentS1S2SendingFlows.get(b).remove(entrySegment));
        continue;
      }

      double proposedPasFlowShift = proposedFlowShifts.get(entrySegment);
      if (Math.abs(proposedPasFlowShift) == 0) {
        continue;
      }

      double smoothedProportionalPasflowShift = smoothing.executeRefZero(proposedPasFlowShift);
      /*test for eligibility to reduce to zero flow along S2 */
      if (smoothedProportionalPasflowShift >= guaranteedEntrySegmentS2SendingFlow) {

        if(isDestinationTrackedForLogging() || logAll) {
          LOGGER.info(String.format("     [removal --> proposed shift %.10f equal or higher than s2 sending flow %.10f, entry segment (%s)]",
                  smoothedProportionalPasflowShift, guaranteedEntrySegmentS2SendingFlow, entrySegment.getIdsAsString()));
        }

        /* truncate to guaranteed available S2 flow */
        smoothedProportionalPasflowShift = guaranteedEntrySegmentS2SendingFlow;
      }

      for (var bush : pas.getRegisteredBushes()) { //todo: stopgap cast
        DestinationBush destinationBush = (DestinationBush) bush;
        double nlConsistentBushEntrySegmentS2Flow =
                bushEntrySegmentS1S2SendingFlows.get(destinationBush).getOrDefault(entrySegment,Pair.of(0.0, 0.0)).second();
        double currentBushEntrySegmentS2Flow = bushEntrySegments2UpdatedFlow.get(bush).getOrDefault(entrySegment,0.0);
        double guaranteedBushEntrySegmentS2SendingFlow = currentBushEntrySegmentS2Flow; //Math.min(nlConsistentBushEntrySegmentS2Flow, currentBushEntrySegmentS2Flow);

        // only consider entry segments where there is still flow present to shift (may happen due to
        if (!destinationBush.containsTurnSendingFlow(entrySegment, pas.getFirstEdgeSegment(false)) || guaranteedBushEntrySegmentS2SendingFlow <= 0) {
          //bushEntrySegmentS1S2SendingFlows.get(bush).remove(entrySegment);
          continue;
        }

        /*
         * In case of multiple used bushes for this entry segment -> we cannot let proposed shifts be executed in full because cost is affected and therefore succeeding entries
         * would "overshoot". Hence, we apply proposed shift proportionally to contribution to total flow along PAS
         */
        double bushS2Portion = guaranteedBushEntrySegmentS2SendingFlow / guaranteedEntrySegmentS2SendingFlow;
        double entrySegmentBushPasflowShift = smoothedProportionalPasflowShift * bushS2Portion;

        if(isDestinationTrackedForLogging(destinationBush) || logAll) {
          LOGGER.info(String.format(
                  "     Shift: %.9f (available flow %.9f) on entry (%s) - bush (%s) - entry alpha: %.2f -",
                  entrySegmentBushPasflowShift, guaranteedBushEntrySegmentS2SendingFlow, entrySegment.getIdsAsString(),
                  destinationBush.getRootZoneVertex().getParent().getParentZone().getIdsAsString(), networkLoading.getCurrentFlowAcceptanceFactors()[(int) entrySegment.getId()]));
        }

        /* perform the flow shift for the current bush and its attributed portion */
        var endMergeSplittingRates = executeBushS2FlowShiftNoNodeModelUpdate(
                destinationBush, entrySegment, entrySegmentBushPasflowShift, networkLoading);

        // track what was shifted for later S1 update
        putFlowShiftedS2Data(entrySegment, destinationBush, new BushEntryShiftedS2FlowData(
                        destinationBush, entrySegment, entrySegmentBushPasflowShift, endMergeSplittingRates));
      }
    }

    /* remove zero-flow S2 bushes from PAS */
    removeZeroFlowS2Bushes(bushEntrySegments2UpdatedFlow);

    return !getFlowShiftedS2BushData().isEmpty();
  }

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
    throw new PlanItRunTimeException("performEquilibratedCongestedS2FlowShift not yet implemented on " +
        "non-conjugate destination based ");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void performAllBushesS1FlowShift(
          Mode theMode,
          StaticLtmLoadingBushBase<?> networkLoading) {
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
                  networkLoading.getCurrentFlowAcceptanceFactors()[(int) entrySegment.getId()],
                  bush.getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
        }

        executeBushS1FlowShiftNoNodeModelUpdate(
                bush,
                entrySegment,
                flowShiftData.getS2Flowshifted(),
                networkLoading,
                flowShiftData.getS2MergeExitSplittingRates());
      }
    }
  }

}

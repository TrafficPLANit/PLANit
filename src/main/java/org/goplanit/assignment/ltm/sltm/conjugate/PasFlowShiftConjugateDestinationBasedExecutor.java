package org.goplanit.assignment.ltm.sltm.conjugate;

import org.goplanit.assignment.ltm.sltm.*;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingBushBase;
import org.goplanit.cost.physical.AbstractPhysicalCost;
import org.goplanit.cost.virtual.AbstractVirtualCost;
import org.goplanit.sdinteraction.smoothing.Smoothing;
import org.goplanit.utils.arrays.ArrayUtils;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.IterableUtils;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.virtual.physical.ConnectoidSegment;
import org.goplanit.utils.pcu.PcuCapacitated;
import org.ojalgo.array.Array1D;

import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

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

  /**
   * Logger to use
   */
  private final static Logger LOGGER = Logger.getLogger(
          PasFlowShiftConjugateDestinationBasedExecutor.class.getCanonicalName());

  /**
   * Unregister bushes with very low or zero flow from PAS
   *
   * @param bushS2RemainingFlows remaining flows for each bush for this PAS
   */
  private void removeZeroFlowS2BushesFromPas(Map<ConjugateDestinationBush, Double> bushS2RemainingFlows) {
    var iter = pas.getRegisteredBushes().iterator();
    while (iter.hasNext()) {
      ConjugateDestinationBush bush = (ConjugateDestinationBush) iter.next();

      // use updated flows as these are always either smaller or larger, when larger they are inconsistent but that is fine
      // because it won't lead to removals, when smaller they are more restrictive and indicate possible removals so we
      // use it
      Double bushS2Flow = bushS2RemainingFlows.get(bush);
      if (bushS2Flow==null || bushS2Flow < EPSILON_6) {
        if(isDestinationTrackedForLogging(bush)){
          LOGGER.info(String.format("   [Unregistering bush (%s) from PAS %s, no more S2 flow left (%.10f)]",
                  bush.getRootZone().getIdsAsString(), pas, bushS2Flow));
        }
        iter.remove();
      }else if(bushS2Flow < 1){
//        LOGGER.info(String.format("   [KEEPING bush (%s) on PAS %s, yet VERY LOW s2 flow remaining (less than 1 per entry segment)]",
//                bush.getRootZone().getIdsAsString(), pas));
      }
    }
  }

  /**
   * Helper to perform a flow shift on a network turn in conjugate form. If the turn has no more flow it is removed
   * from the bush.
   *
   * @param conjBush        bush to use
   * @param conjSegment     turn entry segment
   * @param flowShiftPcuH turn flow shift to apply by adding this flow to the turn
   * @return new turn flow after shift
   */
  private double executeTurnFlowShift(
          ConjugateDestinationBush conjBush, ConjugateEdgeSegment conjSegment, double flowShiftPcuH) {

    // track what edge segments were added to what bush, so we can (in case of overlapping PAS update allowance)
    // flag if additional cycle checks are needed for subsequent PASs that may not be compatible with this current
    // PAS that we chose to prefer over those later ones
    if(flowShiftPcuH > 0 && !conjBush.contains(conjSegment.getId())){
        addBushAddedLinkSegment(conjBush, conjSegment);
    }

    double newTurnFlow = conjBush.addTurnSendingFlow(conjSegment, flowShiftPcuH);

    //todo make sure that when very close to zero we remove all flow on the high cost segment somehow
    // so we do not get into trouble with precision...
    if (!Precision.positive(newTurnFlow, EPSILON) &&
            !Precision.positive(conjBush.getTurnSendingFlow(conjSegment), EPSILON)) {

      /* no remaining flow at all on turn after flow shift, remove turn from bush entirely */
      conjBush.removeTurn(conjSegment);
      if(isDestinationTrackedForLogging(conjBush)){
        LOGGER.info(String.format("     [No more flow --> Removed turn: (%s) from bush (%s)]",
                conjSegment.getOriginalAdjacentEdgeSegmentsIdsAsString(),
                conjBush.getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
      }

      if(!conjBush.contains(conjSegment)) {
        addBushRemovedLinkSegment(conjBush, conjSegment);
      }
      newTurnFlow = 0.0;
    }
    return newTurnFlow;
  }

  /**
   * Execute a flow shift on a PAS alternative for a given bush
   *
   * @param conjBush to use
   * @param bushPasFlowShiftPcuH flow to shift
   * @param pasAlternative to apply to
   * @param nonConjugateFlowAcceptanceFactors to use
   * @return final flow shift applied on last segment of alternative
   */
  private double executeBushPasFlowShift(
          ConjugateDestinationBush conjBush,
          double bushPasFlowShiftPcuH,
          ConjugateEdgeSegment[] pasAlternative,
          double[] nonConjugateFlowAcceptanceFactors) {

    int index = 0;
    double flowShiftPcuH = bushPasFlowShiftPcuH;
    ConjugateEdgeSegment currentConjSegment;
    while (index < pasAlternative.length) {
      currentConjSegment = pasAlternative[index++];

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

      // adjust flow shift using acceptance factor on original network
      if(currentConjSegment.hasOriginalEntryEdgeSegment()){
        EdgeSegment originalTurnEntrySegment = currentConjSegment.getOriginalAdjacentEdgeSegments().first();
        flowShiftPcuH *= nonConjugateFlowAcceptanceFactors[(int) originalTurnEntrySegment.getId()];
      }
    }
    return flowShiftPcuH;
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
  protected double determinePasAlternativeSlackFlow(
          StaticLtmLoadingBushBase<?> networkLoading, boolean lowCost, boolean ignoreInitialSegment) {

    double slackFlow = Double.POSITIVE_INFINITY;

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
      slackFlow = Math.min(slackFlow, currSlackFlow);

      if (!Precision.positive(slackFlow, EPSILON)) {
        return slackFlow;
      }
    }

    // beyond last segment special case tracking
    ConjugateEdgeSegment lastAlternativeSegment = pas.getLastEdgeSegment(lowCost);
    if(!lastAlternativeSegment.hasOriginalExitEdgeSegment()){
      return slackFlow;
    }
    EdgeSegment lastOriginalNetworkSegment = lastAlternativeSegment.getOriginalAdjacentEdgeSegments().first();
    Array1D<Double> splittingRates =
            networkLoading.getSplittingRateData().getSplittingRates(lastOriginalNetworkSegment);
    //todo: add in the splitting rates of the low cost segment, since any exit flow their will be moved to the high cost
    // and distributed accordingly, so we cannot just consider the current state of the low cost here...

    int mergeExitIndex = 0;
    for (var exitSegment : lastOriginalNetworkSegment.getDownstreamVertex().getExitEdgeSegments()) {
      double splittingRate = splittingRates.get(mergeExitIndex);
      if (splittingRate > 0) {
        linkSegmentId = (int) exitSegment.getId();
        /* do not use outflows directly because they are only available on potentially blocking nodes in point queue basic solution scheme */
        var nextInflow = networkLoading.getCurrentInflowsPcuH()[linkSegmentId];
        double currSlackFlow = ((PcuCapacitated) exitSegment).getCapacityOrDefaultPcuH() - nextInflow;
        slackFlow = Math.min(slackFlow, currSlackFlow);
      }
      ++mergeExitIndex;
    }

    return slackFlow;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected double[] executeBushS2FlowShift(
          RootedBush<ConjugateDirectedVertex, ConjugateEdgeSegment> bush,
          EdgeSegment entrySegment,
          double bushEntrySegmentFlowShift,
          double[] nonConjugateFlowAcceptanceFactors) {

    ConjugateDestinationBush conjBush = (ConjugateDestinationBush)bush;

    /* prep - pas */
    final var s2 = pas.getAlternative(false);
    int index = 0;
    double flowShiftPcuH = -bushEntrySegmentFlowShift;

    flowShiftPcuH = executeBushPasFlowShift(conjBush, flowShiftPcuH, s2, nonConjugateFlowAcceptanceFactors);

    /*end splitting rates not required since we do not shift flow beyond end merge via its turn in conjugate form  */
    //todo: remove return value when we no longer have non-conjugate form for this
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeBushS1FlowShift(
          RootedBush<ConjugateDirectedVertex, ConjugateEdgeSegment> bush,
          EdgeSegment dummyEntrySegment,                    // not relevant in conjugate form
          double bushEntrySegmentFlowShift,
          double[] nonConjugateFlowAcceptanceFactors,
          double[] unusedEndMergeSplittingRates) {         // not relevant in conjugate form

    ConjugateDestinationBush conjBush = (ConjugateDestinationBush) bush;

    var s1 = pas.getAlternative(true);

    double s1FinalLabeledFlowShift = executeBushPasFlowShift(
            conjBush,
            bushEntrySegmentFlowShift,
            s1,
            nonConjugateFlowAcceptanceFactors);
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
          double discontinuityDampeningFactor) {
    // todo: once we no longer have non-conjugate implementation remove any entry segment based tracking of flow shifts
    Map<EdgeSegment, Double> result = new TreeMap<>();
    var originalEntrySegment = pas.getFirstEdgeSegment(false).getOriginalAdjacentEdgeSegments().first();


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

    boolean disableSlackFlowEstimate = true;
    double slackFlowEstimate = disableSlackFlowEstimate ? determinePasAlternativeSlackFlow(
            networkLoading, true, ignoreInitialConjEdgeSegment) : getS2SendingFlow();
    if (!pasCostEqual && smallerEqual(denominatorS2,EPSILON,EPSILON) && smallerEqual(denominatorS2, EPSILON,EPSILON)) {
      /* s1 & S2 UNCONGESTED - no derivative estimate possible (denominator zero) */
      /* move all towards cheaper alternative limited by slack + delta */
      /* obtain PAS-entry segment sub-path sending flows */
      double proposedFlowShift = Math.min(getS2SendingFlow() - 10, slackFlowEstimate) + 10;
      result.put(
              originalEntrySegment,
              adjustFlowShiftBasedOnS1SlackFlow(proposedFlowShift, slackFlowEstimate, discontinuityDampeningFactor));
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
      flowShift = adjustFlowShiftBasedOnS1SlackFlow(flowShift, slackFlowEstimate, discontinuityDampeningFactor);
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
      double s2DeltaFlowToStateChangeEstimate = -1;

//      var turnExitSegment = identifyMostRestrictingOutEdgeSegment(refSegment, networkLoading);
//      var criticalExitSplittingRate = networkLoading.getSplittingRateData().getSplittingRate(refSegment, turnExitSegment);

      //TODO: this is not a good estimate since we now assume the link flow as a whole drives the queue, but it may be
      // that a tiny portion on one turn is causing the full reduction factor while the majority of flow would sail through
      // were it not for that small turn flow --> we must compute the discontinuity slack flow for each turn separate and
      // then use the minimum
      // looking at spreadsheet with small example it may not be enough either

      EdgeSegment originalCongestedS2Segment = firstS2CongestedSegment.getOriginalAdjacentEdgeSegments().first();
      int originalCongestedS2SegmentId = (int) originalCongestedS2Segment.getId();
      s2DeltaFlowToStateChangeEstimate =
              networkLoading.getCurrentInflowsPcuH()[originalCongestedS2SegmentId] *
                      (1 - networkLoading.getCurrentFlowAcceptanceFactors()[originalCongestedS2SegmentId]);
      flowShift = adjustFlowShiftBasedOnS2SlackFlow(
              flowShift, s2DeltaFlowToStateChangeEstimate, discontinuityDampeningFactor);
    }

    // make sure we never shift more than the flow that is available
    flowShift = Math.min(flowShift, getS2SendingFlow());
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
   * {@inheritDoc}
   */
  @Override
  public boolean performS2FlowShift(
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
    if (!Precision.positive(nlConsistentS2SendingFlow)) {
      // todo: in case of overlapping pas updates this may happen, maybe more elegant way of dealing with it though
      //LOGGER.warning("no flow on S2 segment of selected PAS, PAS should not exist anymore, this shouldn't happen");
    }

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
      executeBushS2FlowShift(
              conjBush, dummyEntrySegment, bushPasFlowShift, networkLoading.getCurrentFlowAcceptanceFactors());
      // track what was shifted for later S1 update
      // todo: when moving to fully conjugate get rid of BushEntryShiftedS2FlowData, or simplify to not have splitting
      //  rates nor entry segments as they are not used in this context
      putFlowShiftedS2Data(dummyEntrySegment, conjBush, new BushEntryShiftedS2FlowData(
              conjBush, dummyEntrySegment, bushPasFlowShift, null));
    }

    /* remove zero-flow S2 bushes from PAS */
    removeZeroFlowS2BushesFromPas(bushS2RemainingSendingFlows);

    return !getFlowShiftedS2BushData().isEmpty();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void performS1FlowShift(
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

      executeBushS1FlowShift(
              bush,
              flowShiftData.getEntrySegment(),
              flowShiftData.getS2Flowshifted(),
              networkLoading.getCurrentFlowAcceptanceFactors(),
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

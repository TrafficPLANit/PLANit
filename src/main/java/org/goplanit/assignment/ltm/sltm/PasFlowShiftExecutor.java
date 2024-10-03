package org.goplanit.assignment.ltm.sltm;

import org.goplanit.assignment.ltm.sltm.consumer.NMRCollectMostRestrictingTurnConsumer;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingBushBase;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmNetworkLoading;
import org.goplanit.cost.physical.AbstractPhysicalCost;
import org.goplanit.cost.virtual.AbstractVirtualCost;
import org.goplanit.sdinteraction.smoothing.Smoothing;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.virtual.ConnectoidSegment;
import org.goplanit.utils.pcu.PcuCapacitated;
import org.goplanit.utils.zoning.OdZone;
import org.ojalgo.array.Array1D;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.goplanit.utils.math.Precision.*;

/**
 * Common functionality to conduct a PAS flow shift.
 * 
 * @author markr
 *
 */
public abstract class PasFlowShiftExecutor {

  /**
   * Logger to use
   */
  private final static Logger LOGGER = Logger.getLogger(PasFlowShiftExecutor.class.getCanonicalName());

  /** track any removed edge segments as a result of a flow shift on a bush level */
  private final Map<EdgeSegment, Set<RootedLabelledBush>> removedEdgeSegmentsForBushes = new TreeMap<>();

  /** track any removed edge segments as a result of a flow shift on a bush level */
  private final Map<EdgeSegment, Set<RootedLabelledBush>> addedEdgeSegmentsForBushes = new TreeMap<>();

  /**
   * Verify if entry segment is congested
   *
   * @param loading to use
   * @param segment to check
   * @return congested or not based on check
   */
  private static boolean isCongested(StaticLtmLoadingBushBase<?> loading, EdgeSegment segment){
    return smaller(loading.getCurrentFlowAcceptanceFactors()[(int) segment.getId()], 1, EPSILON_9);
  }

  /**
   * Convenience method to check if we need to perform added logging for destination
   *
   * @return true when destination is tracked for logging
   */
  protected boolean isDestinationTrackedForLogging(RootedLabelledBush bush) {
    return settings.isTrackDestinationForLogging((OdZone) bush.getRootZoneVertex().getParent().getParentZone());
  }

  /**
   * Convenience method to check if we need to perform added logging for destination
   *
   * @return true when destination is tracked for logging
   */
  protected boolean isDestinationTrackedForLogging() {
    return settings.hasTrackOdsForLogging() &&
        pas.getRegisteredBushes().stream().anyMatch(this::isDestinationTrackedForLogging);
  }

  private void removeZeroFlowS2Bushes(Map<Bush, Map<EdgeSegment, Double>> bushEntrySegments2UpdatedFlow) {
    var iter = pas.getRegisteredBushes().iterator();
    while (iter.hasNext()) {
      var bush = iter.next();

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
   * obtain derivative of cost towards flow for given alternative, all parameters mut be non-null
   *
   * @param theMode      to use
   * @param networkLoading to use
   * @param physicalCost to use
   * @param virtualCost  to use
   * @return dTravelTimedFlow or 0 if not possible to compute (with warning)
   */
  private double getDTravelTimeDFlow(
          final Mode theMode,
          final StaticLtmLoadingBushBase<?> networkLoading,
          final AbstractPhysicalCost physicalCost,
          final AbstractVirtualCost virtualCost,
          boolean isLowCostAlternative) {

    double dTravelTimeDFlow = 0.0;

    var pasAlternative = this.pas.getAlternative(isLowCostAlternative);
    int index = 0;
    while(index < pasAlternative.length){
      var currSegment = pasAlternative[index];

      double currDTravelTimeDFlow = 0.0;
      boolean unCongested = !isCongested(networkLoading,currSegment);
      if (currSegment instanceof MacroscopicLinkSegment) {
        currDTravelTimeDFlow = physicalCost.getDTravelTimeDFlow(unCongested, theMode, (MacroscopicLinkSegment) currSegment);
      } else if (currSegment instanceof ConnectoidSegment) {
        currDTravelTimeDFlow = virtualCost.getDTravelTimeDFlow(unCongested, theMode, (ConnectoidSegment) currSegment);
      } else {
        LOGGER.severe(String.format("Unsupported edge segment (%s) to obtain derivative of cost towards flow from", currSegment.getIdsAsString()));
      }

      dTravelTimeDFlow += currDTravelTimeDFlow;

      if(!unCongested){
        // no more flow change beyond here due to it being a bottleneck
        break;
      }
      ++index;
    }
    return dTravelTimeDFlow;
  }

  /**
   * obtain derivative of cost towards flow for given segment, all parameters mut be non-null
   *
   * @param theMode      to use
   * @param physicalCost to use
   * @param virtualCost  to use
   * @param edgeSegment  to use
   * @return dTravelTimedFlow or 0 if not possible to compute (with warning)
   */
  @Deprecated(forRemoval = true)
  private static double getDTravelTimeDFlow(
          Mode theMode, AbstractPhysicalCost physicalCost, AbstractVirtualCost virtualCost, EdgeSegment edgeSegment) {

    if (edgeSegment instanceof MacroscopicLinkSegment) {
      return physicalCost.getDTravelTimeDFlow(false, theMode, (MacroscopicLinkSegment) edgeSegment);
    } else if (edgeSegment instanceof ConnectoidSegment) {
      return virtualCost.getDTravelTimeDFlow(false, theMode, (ConnectoidSegment) edgeSegment);
    } else {
      LOGGER.severe(String.format("Unsupported edge segment (%s) to obtain derivative of cost towards flow from", edgeSegment.getXmlId()));
    }

    return 0;
  }

  /**
   * Helper; based on the entry segment and current loading, recompute node model to identify most restricting out edge segment for this entry segment
   * 
   * @param entrySegment   to use
   * @param networkLoading to use
   * @return identified most restricting out edge segment
   */
  private static EdgeSegment identifyMostRestrictingOutEdgeSegment(
          EdgeSegment entrySegment, StaticLtmLoadingBushBase<?> networkLoading) {
    var consumer = new NMRCollectMostRestrictingTurnConsumer(entrySegment); // collect most restricting turn for entry segment
    StaticLtmNetworkLoading.performNodeModelUpdate(entrySegment.getDownstreamVertex(), consumer, networkLoading);

    EdgeSegment mostRestrictingOutSegment = consumer.getMostRestrictingOutSegment();
    if (mostRestrictingOutSegment == null) {
      LOGGER.severe(String.format("Expected most restricting our segment to be present given that incoming segment (%s) is congested, but not found, this shouldn't happen",
          entrySegment.getXmlId()));
    }
    return mostRestrictingOutSegment;
  }

  /**
   * Find first congested segment on PAS for either alternative
   *
   * @param networkLoading to use
   * @return found segments pair on low/high cost alternative, null entries when not congested
   */
  private Pair<EdgeSegment, EdgeSegment> populateFirstCongestedEdgeSegmentOnPasAlternative(
          final StaticLtmLoadingBushBase<?> networkLoading) {

    Predicate<EdgeSegment> firstCongestedLinkSegment = es -> isCongested(networkLoading,es);
    var firstS1CongestedLinkSegment = pas.matchFirst(true, /* low cost */ firstCongestedLinkSegment);
    var firstS2CongestedLinkSegment = pas.matchFirst(false /* high cost */, firstCongestedLinkSegment);

    return Pair.of(firstS1CongestedLinkSegment, firstS2CongestedLinkSegment);
  }

  /**
   * Determine the adjusted flow shift by taking the proposed upper bound and reduce it by a
   * designated amount based on the difference between the PAS alternative costs and the provided
   * upperbound reference. When below the minimum allowed number proceed regardless without adjustment.
   *
   * @param proposedFlowShift to use
   * @param upperBoundShift that is ideally the maximum
   * @param minimumAllowedShift to use
   * @return adjusted proposed flow shift (if any)
   */
  private double adjustFlowShiftBasedOnUpperBound(
          double proposedFlowShift, double upperBoundShift, double minimumAllowedShift) {

    if (proposedFlowShift <= upperBoundShift) {
      return proposedFlowShift;
    }

    /*
     * when approaching equilibrium, small shifts should be fully executed, otherwise it takes
     * forever to converge. With such small flows chances have decreased that overshooting
     * and triggering a different state has a dramatic effect on the travel time derivative
     */
    if (Precision.smaller(proposedFlowShift, minimumAllowedShift)) {
      return proposedFlowShift;
    }

    double assumedCongestedShift = proposedFlowShift - upperBoundShift;
    double portion = (1 - pas.getAlternativeLowCost() / pas.getAlternativeHighCost());
    return upperBoundShift + assumedCongestedShift * portion;
  }

  /**
   * Determine the adjusted flow shift by taking the proposed flow shift (s2 sending flow) and reduce it by a
   * designated amount based on the difference between the PAS alternative costs and the assumed s1
   * slack flow (flow estimated to switch from uncongested to congested on the PAS's S1 (low cost) segment)
   * 
   * @param s1SlackFlow that is expected
   * @return adjusted proposed flow shift (if any)
   */
  private double adjustFlowShiftBasedOnS1SlackFlow(double proposedFlowShift, double s1SlackFlow) {
    return adjustFlowShiftBasedOnUpperBound(proposedFlowShift, s1SlackFlow, 10);
  }

  /**
   * Determine the adjusted flow shift by taking the proposed flow shift (s2 sending flow) and it by a
   * designated amount based on the difference between the PAS alternative costs and the assumed
   * s2 slack flow (flow estimated to switch from congested to uncongested on the PAS's S2
   * (high cost) segment)
   * 
   * @param s2SlackFlow that is expected
   * @return adjusted proposed flow shift (if any)
   */
  private double adjustFlowShiftBasedOnS2SlackFlow(double proposedFlowShift, double s2SlackFlow) {
    return adjustFlowShiftBasedOnUpperBound(proposedFlowShift, s2SlackFlow, 10);
  }

  /**
   * For the given PAS determine the amount of slack flow on chosen alternative, i.e., the minimum difference between the link outflow rate and the capacity across all its link
   * segments, including the link segments beyond its alternative it is directing the flows to. It is assumed the cheap cost alternative of the PAS has already been found to be
   * uncongested and as such should have a zero or higher slack flow.
   * <p>
   * In the special case that it passes through (or directs to) a segment that is at capacity (due to for example one or more of its other in-links being congested), then we return
   * a slack capacity of zero.
   * 
   * 
   * @param networkLoading to collect outflow rates from
   * @param lowCost        when true determine for low cost alternative, when false for high cost alternative
   * @return pair of slack flow and slack capacity ratio
   */
  private double determinePasAlternativeSlackFlow(StaticLtmLoadingBushBase<?> networkLoading, boolean lowCost) {
    var lastAlternativeSegment = pas.getLastEdgeSegment(lowCost);
    double slackFlow = Double.POSITIVE_INFINITY;

    Array1D<Double> splittingRates =
            networkLoading.getSplittingRateData().getSplittingRates(lastAlternativeSegment);

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

  /** local epsilon used in flow shifting */
  protected static final double EPSILON = EPSILON_12;

  /**
   * whenever a PAS S2 alternative's flow drops below this threshold for a given bush, we allow the flow shift to move all remaining flow towards the S1 segment across all entry
   * segments and unregister the bush for this PAS as it is no longer deemed a true alternative.
   */
  protected static final double PAS_MIN_S2_FLOW_THRESHOLD = 1;

  /** to operate on */
  protected final Pas pas;

  /** settings to use */
  protected final StaticLtmSettings settings;

  /** Track the desired sending flows for s1 and s2 per bush per entry segment */
  protected final Map<RootedLabelledBush, Map<EdgeSegment, Pair<Double, Double>>> bushEntrySegmentS1S2SendingFlows;

  protected final Set<EdgeSegment> usedCongestedEntryEdgeSegments;

  /** store locally as it is costly-ish to compute */
  protected final int pasMergeVertexNumExitSegments;

  protected Double getTotalEntrySegmentSendingFlow(EdgeSegment entrySegment, boolean lowCost){
    ToDoubleFunction<Pair<Double,Double>> flowSupplier = lowCost ? Pair::first : Pair::second;
    return bushEntrySegmentS1S2SendingFlows.values().stream().map(
            entry -> entry.getOrDefault(entrySegment, Pair.of(0.0,0.0))).mapToDouble(flowSupplier).sum();
  }

  /**
   * Constructor
   * 
   * @param pas      to use
   * @param settings to use
   */
  protected PasFlowShiftExecutor(final Pas pas, final StaticLtmSettings settings) {
    this.pas = pas;
    this.settings = settings;
    this.bushEntrySegmentS1S2SendingFlows = new HashMap<>();
    this.usedCongestedEntryEdgeSegments = new HashSet<>();
    this.pasMergeVertexNumExitSegments = pas.getMergeVertex().getNumberOfExitEdgeSegments();
  }

  /**
   * Perform the flow shift for a given bush. Delegate to concrete class implementation
   * 
   * @param bush                      to perform shift for
   * @param entrySegment              entry segment at hand to apply flow shift for
   * @param bushEntrySegmentFlowShift the absolute shift to apply for the given PAS-bush-entrysegment combination
   * @param flowAcceptanceFactors     to use
   */
  protected abstract void executeBushFlowShift(
          final RootedLabelledBush bush, final EdgeSegment entrySegment, double bushEntrySegmentFlowShift, final double[] flowAcceptanceFactors);

  /**
   * For the given PAS-entrysegment determine the flow shift to apply from the high cost to the low cost segment. Depending on the state of the segments we utilise their
   * derivatives of travel time towards flow to determine the optimal shift. In case one or both segments are uncongested, or the congestion occurs on the entry segment while the
   * cost on the PAS is already equal, we propose to shift as much flow as would yield an equal distribution between the alternatives (maximising entropy) in order to obtain a
   * unique solution under equal cost. would expect the segment to transition to congestion.
   * 
   * @param entrySegment   to use
   * @param theMode        to use
   * @param physicalCost   to use
   * @param virtualCost    to use
   * @param networkLoading to use
   * @return amount of flow to shift
   */
  protected double determineEntrySegmentFlowShift(
          EdgeSegment entrySegment,
          Mode theMode,
          AbstractPhysicalCost physicalCost,
          AbstractVirtualCost virtualCost,
          StaticLtmLoadingBushBase<?> networkLoading) {

    double denominatorS2 = 0;
    double denominatorS1 = 0;

    /* get first congested edge segment that is affected when shifting flow, per alternative */
    var firstCongestedSegmentPair = populateFirstCongestedEdgeSegmentOnPasAlternative(networkLoading);
    var firstS1CongestedLinkSegment = firstCongestedSegmentPair.first();
    var firstS2CongestedLinkSegment = firstCongestedSegmentPair.second();

    denominatorS1 =
            getDTravelTimeDFlow(theMode, networkLoading, physicalCost, virtualCost, true);
    denominatorS2 =
            getDTravelTimeDFlow(theMode, networkLoading, physicalCost, virtualCost, false);

    double flowShift = 0;
    boolean pasCostEqual = pas.isCostEqual(EPSILON);
    double s2TotalEntrySendingFlow = getTotalEntrySegmentSendingFlow(entrySegment, false);
    double slackFlowEstimate = determinePasAlternativeSlackFlow(networkLoading, true);
    if (!pasCostEqual && smaller(denominatorS2,EPSILON) && smaller(denominatorS2, EPSILON)) {

      /* s1 & S2 UNCONGESTED - no derivative estimate possible (denominator zero) */
      /* move all towards cheaper alternative limited by slack + delta */
      /* obtain PAS-entry segment sub-path sending flows */
      double proposedFlowShift = Math.min(s2TotalEntrySendingFlow - 10, slackFlowEstimate) + 10;
      return adjustFlowShiftBasedOnS1SlackFlow(proposedFlowShift, slackFlowEstimate);

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
        LOGGER.severe("Computation of using derivatives to shift flows between PAS segments does not result in equal travel time after shift, this should not happen");
      }
    }

    boolean pasEntrySegmentCongested = isCongested(networkLoading, entrySegment);
    // VERIFY CROSSING OF DISCONTINUITY on S1 travel time function - adjust shift if so to mitigate effect
    if (firstS1CongestedLinkSegment == null && !pasEntrySegmentCongested) {
      /* possible triggering of congestion on s1 due to shift -> passing discontinuity on travel time function */
      flowShift = adjustFlowShiftBasedOnS1SlackFlow(flowShift, slackFlowEstimate);
    }

    // VERIFY CROSSING OF DISCONTINUITY on S2 travel time function - adjust shift if so to mitigate effect
    if (firstS2CongestedLinkSegment != null || pasEntrySegmentCongested) {
      var refSegment = pasEntrySegmentCongested ? entrySegment : firstS2CongestedLinkSegment;
      double s2DeltaFlowToStateChangeEstimate = -1;
      s2DeltaFlowToStateChangeEstimate =
              networkLoading.getCurrentInflowsPcuH()[(int) refSegment.getId()] *
                      (1 - networkLoading.getCurrentFlowAcceptanceFactors()[(int) refSegment.getId()]);
      flowShift = adjustFlowShiftBasedOnS2SlackFlow(flowShift, s2DeltaFlowToStateChangeEstimate);
    }

    // make sure we never shift more than flow than available
    flowShift = Math.min(flowShift, s2TotalEntrySendingFlow);

    return flowShift;
  }

  /**
   * Determining the currently available desired flows along each subpath
   * (utilising the current state of the bush level)
   */
  public void updateS1S2EntrySendingFlows() {
    /* determine the network flow on the high cost subpath */

    var s2 = pas.getAlternative(false /* high cost */);
    var s1 = pas.getAlternative(true /* low cost */);

    bushEntrySegmentS1S2SendingFlows.clear();
    for (var entrySegment : pas.getDivergeVertex().getEntryEdgeSegments()) {
      for (var bush : pas.getRegisteredBushes()) {
        if (!bush.containsEdgeSegment(entrySegment)) {
          continue;
        }
        bushEntrySegmentS1S2SendingFlows.putIfAbsent(bush, new HashMap<>());
        var entrySegmentS1S2SendingFlows = bushEntrySegmentS1S2SendingFlows.get(bush);

        double s2BushSendingFlow = bush.determineSubPathSendingFlow(entrySegment, s2);

        double s1BushSendingFlow = bush.determineSubPathSendingFlow(entrySegment, s1);

        entrySegmentS1S2SendingFlows.put(entrySegment, Pair.of(s1BushSendingFlow, s2BushSendingFlow));
      }
    }
  }

  public Map<EdgeSegment, Double> determineProposedFlowShiftByEntrySegment(Mode theMode,
                                                                           AbstractPhysicalCost physicalCost,
                                                                           AbstractVirtualCost virtualCost,
                                                                           StaticLtmLoadingBushBase<?> networkLoading) {

    Map<EdgeSegment, Double> result = new TreeMap<>();
    for (var entrySegment : pas.getDivergeVertex().getEntryEdgeSegments()) {
      double proposedPasFlowShift = 0;
      double totalEntrySegmentS2Flow = getTotalEntrySegmentSendingFlow(entrySegment, false);
      if (totalEntrySegmentS2Flow > 0) {
        /* flow shift based on entry segment - PAS combination */
        proposedPasFlowShift = determineEntrySegmentFlowShift(
            entrySegment, theMode, physicalCost, virtualCost, networkLoading);
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
   * @param physicalCost       to use
   * @param virtualCost        to use
   * @param networkLoading     to use
   * @param smoothing          to apply to flow shift
   * @return true when flow is shifted, false otherwise
   */
  public boolean run(
      Map<EdgeSegment, Double> proposedFlowShifts,
      Mode theMode,
      AbstractPhysicalCost physicalCost,
      AbstractVirtualCost virtualCost,
      StaticLtmLoadingBushBase<?> networkLoading,
      Smoothing smoothing) {

    double networkLoadingConsistentS2SendingFlow = getS2SendingFlow(); // consistent with original loading
    if (!Precision.positive(networkLoadingConsistentS2SendingFlow)) {
      // todo: in case of overlapping pas updates this may happen, maybe more elegant way of deaing with it though
      //LOGGER.warning("no flow on S2 segment of selected PAS, PAS should not exist anymore, this shouldn't happen");
    }

    //--------------- CURRENTLY FOR DEBUGGING ------------------------------------
    // idea --> construct current situation to compare to how we constructed proposed and s2 flows consistent with loading
    //          to be used to determine best way forward to ensure flow shifts do not cause problems when distributing
    //          over entry segments and bushes that have been changed as part of other (partially) overlapping PASs
    double totalProposedFlowShift = 0;
    //todo: for now just use this to monitor any discrepancies between original distribution and current with overlapping PASs for debugging
    Map<Bush, Map<EdgeSegment, Double>> bushEntrySegments2UpdatedFlow = new TreeMap<>();
    for( var entryShiftPair : proposedFlowShifts.entrySet()){
      var entrySegment = entryShiftPair.getKey();
      totalProposedFlowShift += entryShiftPair.getValue();
      for (var bush : pas.getRegisteredBushes()) {
        if (!bush.containsEdgeSegment(entrySegment)) {
          continue;
        }
        bushEntrySegments2UpdatedFlow.computeIfAbsent(bush, b -> new TreeMap<>());
        bushEntrySegments2UpdatedFlow.get(bush).put(
            entrySegment, bush.determineSubPathSendingFlow(entrySegment, pas.getAlternative(false)));
      }
    }
    double currentS2SendingFlow = bushEntrySegments2UpdatedFlow.values().stream().flatMap(e -> e.values().stream()).mapToDouble(e -> e).sum();
    // truncate to what is available due to overlapping previous shifts taking some of the available flow away
    totalProposedFlowShift = totalProposedFlowShift;

    // if earlier shifts have reduced available flow, capture in factor, so we remain in feasible shifting region
    double s2FlowAvailabilityFactor = Math.min(1,currentS2SendingFlow/networkLoadingConsistentS2SendingFlow);
    // minimum of current and original is what we now is available as it is a minimum
    double guaranteedS2SendingFlow = Math.min(currentS2SendingFlow, networkLoadingConsistentS2SendingFlow);

    if(isDestinationTrackedForLogging()) {
      LOGGER.info("*FLOW SHIFT on PAS:" + pas + " - S2 flow: " + guaranteedS2SendingFlow + "(NL consistent: " + networkLoadingConsistentS2SendingFlow+") - cost-diff: " + pas.getReducedCost());
      LOGGER.info("s1 alphas: "+
              Arrays.stream(pas.getAlternative(true)).map(es -> String.format("%s:%.2f",
                      es.getXmlId(), networkLoading.getCurrentFlowAcceptanceFactors()[(int) es.getId()])).collect(Collectors.joining(",")));
      LOGGER.info("s2 alphas: "+
              Arrays.stream(pas.getAlternative(false)).map(es -> String.format("%s:%.2f",
                      es.getXmlId(), networkLoading.getCurrentFlowAcceptanceFactors()[(int) es.getId()])).collect(Collectors.joining(",")));
    }

    boolean flowShifted = false;
    for (var entrySegment : pas.getDivergeVertex().getEntryEdgeSegments()) {
      double nlConsistentEntrySegmentS2Flow = getTotalEntrySegmentSendingFlow(entrySegment, false);
      double currentEntrySegmentS2Flow = bushEntrySegments2UpdatedFlow.values().stream().map(
              entry -> entry.getOrDefault(entrySegment,0.0)).mapToDouble(d->d).sum();
      double guaranteedEntrySegmentS2SendingFlow = Math.min(nlConsistentEntrySegmentS2Flow, currentEntrySegmentS2Flow);
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
      if (Precision.greaterEqual(smoothedProportionalPasflowShift, guaranteedEntrySegmentS2SendingFlow, EPSILON_3)) {

        if(isDestinationTrackedForLogging()) {
          LOGGER.info(String.format("     [removal --> shift %.10f may exceed s2 sending flow %.10f, entry segment (%s)]",
                  smoothedProportionalPasflowShift, guaranteedEntrySegmentS2SendingFlow, entrySegment.getIdsAsString()));
        }

        /* truncate to guaranteed available S2 flow */
        smoothedProportionalPasflowShift = guaranteedEntrySegmentS2SendingFlow;
      }

      for (var bush : pas.getRegisteredBushes()) {
        double nlConsistentBushEntrySegmentS2Flow = bushEntrySegmentS1S2SendingFlows.get(bush).getOrDefault(entrySegment,Pair.of(0.0, 0.0)).second();
        double currentBushEntrySegmentS2Flow = bushEntrySegments2UpdatedFlow.get(bush).getOrDefault(entrySegment,0.0);
        double guaranteedBushEntrySegmentS2SendingFlow = Math.min(nlConsistentBushEntrySegmentS2Flow, currentBushEntrySegmentS2Flow);

        //todo: make sure this is now covered automatically correctly via remove bushes from PAS by looking at updated s2 flows
//        if (isPasS2RemovalAllowed()) {
//          /* no flow remaining on S2 for bush, unregister */
//          bushEntrySegmentS1S2SendingFlows.get(bush).remove(entrySegment);
//        }

        // only consider entry segments where there is flow present to shift.
        if (!bush.containsTurnSendingFlow(entrySegment, pas.getFirstEdgeSegment(false)) || guaranteedBushEntrySegmentS2SendingFlow <= 0) {
//          if(bush.determineSubPathSendingFlow(entrySegment, pas.getAlternative(false)) > 0){
//            LOGGER.severe(String.format("Thought PAS (%s) s2 to have no flow, but upon checking flow present for bush (%s)", pas, bush.getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
//          }
          continue;
        }

        /*
         * In case of multiple used bushes for this entry segment -> we cannot let proposed shifts be executed in full because cost is affected and therefore succeeding entries
         * would "overshoot". Hence, we apply proposed shift proportionally to contribution to total flow along PAS
         */
        double bushS2Portion = guaranteedBushEntrySegmentS2SendingFlow / guaranteedEntrySegmentS2SendingFlow;
        double entrySegmentBushPasflowShift = smoothedProportionalPasflowShift * bushS2Portion;

        if(guaranteedBushEntrySegmentS2SendingFlow < EPSILON_3){
          entrySegmentBushPasflowShift = guaranteedBushEntrySegmentS2SendingFlow; // make sure we ride the bush s2 PAS from all remaining flow by setting a high value
          if(isDestinationTrackedForLogging(bush)) {
            LOGGER.info(String.format(
                    "     S2 flow bush near zero -> FLOW SHIFT UPLIFT -> Attempted flow shift=%.10f) - entry segment (%s) - alpha: %.2f - bush (%s)",
                    entrySegmentBushPasflowShift, entrySegment.getIdsAsString(),
                networkLoading.getCurrentFlowAcceptanceFactors()[(int) entrySegment.getId()], bush.getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
          }
        }else{
          if(isDestinationTrackedForLogging(bush)) {
            LOGGER.info(String.format(
                    "     Flow shift: %.10f (available flow %.10f) - entry segment (%s) - alpha: %.2f - bush (%s)",
                    entrySegmentBushPasflowShift, guaranteedBushEntrySegmentS2SendingFlow, entrySegment.getIdsAsString(),
                    networkLoading.getCurrentFlowAcceptanceFactors()[(int) entrySegment.getId()], bush.getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
          }
        }

        /* perform the flow shift for the current bush and its attributed portion */
        executeBushFlowShift(
            bush, entrySegment, entrySegmentBushPasflowShift, networkLoading.getCurrentFlowAcceptanceFactors());
        flowShifted = true;

        if (isCongested(networkLoading, entrySegment)) {
          usedCongestedEntryEdgeSegments.add(entrySegment);
        }


      }
    }

    /* remove zero-flow S2 bushes from PAS */
    removeZeroFlowS2Bushes(bushEntrySegments2UpdatedFlow);

    return flowShifted;
  }

  /**
   * Sending flow along PAS high cost segment
   * 
   * @return high cost alternative desired flow
   */
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
  public double getS1SendingFlow() {
    return bushEntrySegmentS1S2SendingFlows.values().stream().flatMap(
            e -> e.values().stream()).mapToDouble(Pair::first).sum();
  }

  /**
   * All used entry Segments that were found to be congested and a flow shift has been applied to
   * 
   * @return set of found edge segments
   */
  public Set<EdgeSegment> getUsedCongestedEntrySegments() {
    return this.usedCongestedEntryEdgeSegments;
  }

  /**
   * Verify if any edge segments have been removed by a bush as a result of the PAS flow shift
   *
   * @return true if confirmed, false otherwise
   */
  public boolean hasAnyBushRemovedLinkSegments() {
    return removedEdgeSegmentsForBushes != null && !removedEdgeSegmentsForBushes.isEmpty();
  }

  /**
   * Verify if any edge segments have been added by a bush as a result of the PAS flow shift
   *
   * @return true if confirmed, false otherwise
   */
  public boolean hasAnyBushAddedLinkSegments() {
    return addedEdgeSegmentsForBushes != null && !addedEdgeSegmentsForBushes.isEmpty();
  }

  /**
   * access bushes that have removed link segments due to a flow shift
   *
   * @return tracked findings or empty map
   */
  public Map<EdgeSegment, Set<RootedLabelledBush>> getBushRemovedLinkSegments() {
    return removedEdgeSegmentsForBushes;
  }

  /**
   * access bushes that have added link segments due to a flow shift
   *
   * @return tracked findings or empty map
   */
  public Map<EdgeSegment, Set<RootedLabelledBush>> getBushAddedLinkSegments() {
    return addedEdgeSegmentsForBushes;
  }

  /**
   * access bushes that have removed the given link segment due to a flow shift
   *
   * @param linkSegment to check for
   * @return tracked findings or empty list
   */
  public Set<RootedLabelledBush> getBushRemovedLinkSegments(EdgeSegment linkSegment) {
    var bushes =
        removedEdgeSegmentsForBushes.computeIfAbsent(linkSegment, k -> new TreeSet<>());
    return bushes;
  }

  /**
   * access bushes that have added the given link segment due to a flow shift
   *
   * @param linkSegment to check for
   * @return tracked findings or empty list
   */
  public Set<RootedLabelledBush> getBushAddedLinkSegments(EdgeSegment linkSegment) {
    var bushes =
        addedEdgeSegmentsForBushes.computeIfAbsent(linkSegment, k -> new TreeSet<>());
    return bushes;
  }

  /**
   * Mark segment as removed from bush due to flow shift
   *
   * @param bush to use
   * @param linkSegment to register
   */
  public void addBushRemovedLinkSegment(
      RootedLabelledBush bush, EdgeSegment linkSegment){
    if(settings.hasTrackOdsForLogging() && isDestinationTrackedForLogging(bush)){
      LOGGER.info(String.format(
          "           Removed link segment (%s) from bush (%s)",
          linkSegment.getIdsAsString(), bush.getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
    }
    getBushRemovedLinkSegments(linkSegment).add(bush);
  }

  /**
   * Mark segment as added to bush due to flow shift
   *
   * @param bush to use
   * @param linkSegment to register
   */
  public void addBushAddedLinkSegment(
      RootedLabelledBush bush, EdgeSegment linkSegment){
    if(settings.hasTrackOdsForLogging() && isDestinationTrackedForLogging(bush)){
      LOGGER.info(String.format(
          "           Added link segment (%s) to bush (%s)",
          linkSegment.getIdsAsString(), bush.getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
    }
    getBushAddedLinkSegments(linkSegment).add(bush);
  }

}

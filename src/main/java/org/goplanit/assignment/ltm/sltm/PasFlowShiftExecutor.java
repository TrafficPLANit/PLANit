package org.goplanit.assignment.ltm.sltm;

import static org.goplanit.utils.math.Precision.EPSILON_12;
import static org.goplanit.utils.math.Precision.equal;
import static org.goplanit.utils.math.Precision.smaller;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.goplanit.assignment.ltm.sltm.consumer.NMRCollectMostRestrictingTurnConsumer;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingBushBase;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingBushRooted;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmNetworkLoading;
import org.goplanit.cost.physical.AbstractPhysicalCost;
import org.goplanit.cost.virtual.AbstractVirtualCost;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.virtual.ConnectoidSegment;
import org.goplanit.utils.pcu.PcuCapacitated;
import org.ojalgo.array.Array1D;

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

  /** flag indicating if it is allowed to remove turns,edge segments along PAS s2 segment from bush while executing flow shift */
  private boolean allowPasRemoval;

  /**
   * flag indicating of most recent call to
   * {@link #determineEntrySegmentFlowShift(EdgeSegment, Mode, AbstractPhysicalCost, AbstractVirtualCost, StaticLtmLoadingBushBase)} identified that flow distribution
   * between s1 and s2 should be made equal.
   */
  boolean towardsEqualAlternativeFlowDistribution;

  /**
   * obtain derivative of cost towards flow for given segment, all parameters mut be non-null
   * 
   * @param theMode      to use
   * @param physicalCost to use
   * @param virtualCost  to use
   * @param edgeSegment  to use
   * @return dTravelTimedFlow or 0 if not possible to compute (with warning)
   */
  private static double getDTravelTimeDFlow(Mode theMode, AbstractPhysicalCost physicalCost, AbstractVirtualCost virtualCost, EdgeSegment edgeSegment) {
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
  private static EdgeSegment identifyMostRestrictingOutEdgeSegment(EdgeSegment entrySegment, StaticLtmLoadingBushBase<?> networkLoading) {
    var consumer = new NMRCollectMostRestrictingTurnConsumer(entrySegment); // collect most restricting turn for entry segment
    StaticLtmNetworkLoading.performNodeModelUpdate(entrySegment.getDownstreamVertex(), consumer, networkLoading);

    EdgeSegment mostRestrictingOutSegment = consumer.getMostRestrictingOutSegment();
    if (mostRestrictingOutSegment == null) {
      LOGGER.severe(String.format("Expected most restricting our segment to be present given that incoming segment (%s) is congested, but not found, this shouldn't happen",
          entrySegment.getXmlId()));
    }
    return mostRestrictingOutSegment;
  }

  private Pair<EdgeSegment, EdgeSegment> populateFirstCongestedEdgeSegmentOnPasAlternative(final EdgeSegment entrySegment, final StaticLtmLoadingBushBase<?> networkLoading) {

    Predicate<EdgeSegment> firstCongestedLinkSegment = es -> networkLoading.getCurrentFlowAcceptanceFactors()[(int) es.getId()] < 1;
    var firstS1CongestedLinkSegment = pas.matchFirst(true, /* low cost */ firstCongestedLinkSegment);
    var firstS2CongestedLinkSegment = pas.matchFirst(false /* high cost */, firstCongestedLinkSegment);

    /* prep */
    var entrySegmentAlpha = networkLoading.getCurrentFlowAcceptanceFactors()[(int) entrySegment.getId()];
    if (smaller(entrySegmentAlpha, 1, EPSILON)) {

      /*
       * entry segment congested - derivative is only sensitive to most restricted out link, which pertains to one of the PAS alternatives (or none). Identify which. We can then
       * assume that the shift does not impact other directions than the most restricting one if and only if no other congested link segments other than the entry segment is
       * congested
       */
      EdgeSegment mostRestrictingOutSegment = identifyMostRestrictingOutEdgeSegment(entrySegment, networkLoading);
      if (mostRestrictingOutSegment.idEquals(pas.getFirstEdgeSegment(true))) {
        firstS1CongestedLinkSegment = entrySegment;
      } else if (mostRestrictingOutSegment.idEquals(pas.getFirstEdgeSegment(false))) {
        firstS2CongestedLinkSegment = entrySegment;
      }
    }
    return Pair.of(firstS1CongestedLinkSegment, firstS2CongestedLinkSegment);
  }

  /**
   * Determine the adjusted flow shift by taking the proposed flow shift (s2 sending flow) and reduce it by a designated amount based on the difference between the PAS alternative
   * costs and the assumed s1 slack flow (flow estimated to switch from uncongested to congested on the PAS's S1 (low cost) segment)
   * 
   * @param s1SlackFlow that is expected
   * @return adjusted proposed flow shift (if any)
   */
  private double adjustFlowShiftBasedOnS1SlackFlow(double proposedFlowShift, double s1SlackFlow) {
    if (proposedFlowShift <= s1SlackFlow) {
      return proposedFlowShift;
    }

    /*
     * when approaching equilibrium, small shifts should be fully executed, otherwise it takes forever to converge. With such small flows chances have decreased that overshooting
     * and triggering a different state has a dramatic effect on the travel time derivative
     */
    if (Precision.smaller(proposedFlowShift, 10)) {
      return proposedFlowShift;
    }

    double assumedCongestedShift = proposedFlowShift - s1SlackFlow;
    double portion = (1 - pas.getAlternativeLowCost() / pas.getAlternativeHighCost());
    return s1SlackFlow + assumedCongestedShift * portion;
  }

  /**
   * Determine the adjusted flow shift by taking the proposed flow shift (s2 sending flow) and and it by a designated amount based on the difference between the PAS alternative
   * costs and the assumed s2 slack flow (flow estimated to switch from congested to uncongested on the PAS's S2 (high cost) segment)
   * 
   * @param s2SlackFlow that is expected
   * @return adjusted proposed flow shift (if any)
   */
  private double adjustFlowShiftBasedOnS2SlackFlow(double proposedFlowShift, double s2SlackFlow) {
    if (proposedFlowShift <= s2SlackFlow) {
      return proposedFlowShift;
    }

    double assumedUncongestedShift = proposedFlowShift - s2SlackFlow;
    double portion = (1 - pas.getAlternativeLowCost() / pas.getAlternativeHighCost());
    return s2SlackFlow + assumedUncongestedShift * portion;
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

    Array1D<Double> splittingRates = networkLoading.getSplittingRateData().getSplittingRates(lastAlternativeSegment);

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

  /** S1 and S2 sending flows along (entire) alternative for a given entry segment */
  protected Map<EdgeSegment, Pair<Double, Double>> totalEntrySegmentS1S2Flow;

  /** Track the desired sending flows for s1 and s2 per bush per entry segment */
  protected final Map<RootedLabelledBush, Map<EdgeSegment, Pair<Double, Double>>> bushEntrySegmentS1S2SendingFlows;

  protected final Set<EdgeSegment> usedCongestedEntryEdgeSegments;

  /** store locally as it is costly-ish to compute */
  protected final int pasMergeVertexNumExitSegments;

  /**
   * activate if flag is true
   * 
   * @param flag to determine whether or not to activate
   */
  protected void activatePasS2RemovalIf(boolean flag) {
    this.allowPasRemoval = flag;
  }

  protected boolean isPasS2RemovalAllowed() {
    return this.allowPasRemoval;
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
  protected abstract void executeBushFlowShift(final RootedLabelledBush bush, final EdgeSegment entrySegment, double bushEntrySegmentFlowShift, final double[] flowAcceptanceFactors);

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
  protected double determineEntrySegmentFlowShift(EdgeSegment entrySegment, Mode theMode, AbstractPhysicalCost physicalCost, AbstractVirtualCost virtualCost,
      StaticLtmLoadingBushBase<?> networkLoading) {

    /* get first congested edge segment that is affected when shifting flow, per alternative */
    var firstCongestedSegmentPair = populateFirstCongestedEdgeSegmentOnPasAlternative(entrySegment, networkLoading);
    EdgeSegment firstS1CongestedLinkSegment = firstCongestedSegmentPair.first();
    EdgeSegment firstS2CongestedLinkSegment = firstCongestedSegmentPair.second();
    boolean sharedCongestedEntry = (entrySegment == firstS1CongestedLinkSegment || entrySegment == firstS2CongestedLinkSegment);

    /* obtain derivatives of travel time towards flow for ENTRY+PAS_alternatives and use them to determine the amount of flow to shift */
    double denominatorS2 = 0;
    double denominatorS1 = 0;
    /* derivative of link cost based on first congested link */
    if (firstS1CongestedLinkSegment != null) {
      denominatorS1 = getDTravelTimeDFlow(theMode, physicalCost, virtualCost, firstS1CongestedLinkSegment);
    }
    if (firstS2CongestedLinkSegment != null) {
      denominatorS2 = getDTravelTimeDFlow(theMode, physicalCost, virtualCost, firstS2CongestedLinkSegment);
    }

    /* obtain PAS-entry segment sub-path sending flows */
    var s1S2SubPathSendingFlowPair = totalEntrySegmentS1S2Flow.get(entrySegment);
    double s1WithEntrySendingFlow = s1S2SubPathSendingFlowPair.first();
    double s2WithEntrySendingFlow = s1S2SubPathSendingFlowPair.second();

    double flowShift = 0;
    boolean pasCostEqual = pas.isCostEqual(EPSILON);
    boolean pasUncongested = firstS1CongestedLinkSegment == null && firstS2CongestedLinkSegment == null;
    double slackFlowEstimate = determinePasAlternativeSlackFlow(networkLoading, true);
    if (pasUncongested && !pasCostEqual) {

      LOGGER.info("** uncongested - towards S1 - unequal cost");
      /* s1 & S2 UNCONGESTED - no derivative estimate possible (denominator zero) */
      /* move all towards cheaper alternative limited by slack + delta */
      double proposedFlowShift = Math.min(s2WithEntrySendingFlow - 10, slackFlowEstimate) + 10;
      return adjustFlowShiftBasedOnS1SlackFlow(proposedFlowShift, slackFlowEstimate);

    } else {

      if (pasCostEqual) {
        LOGGER.info("** one or both alternatives congested - towards S1 - near equal cost (<10^-12)");
      } else {
        LOGGER.info("** one or both alternatives congested - towards S1 - unequal cost");
      }
      /* s1 and/or s2 congested - derivative based flow shift possible */
      // tauw_s1 + dtauw_s1/ds_1 * (-flowShift) = tauw_s2 + dtauw_s2/ds_2 * (flowShift) we find:
      // flowShift = (tauw_s2-tauw_s1)/(1/v_s1_first_bottleneck + 1/v_s2_first_bottleneck))
      double denominator = denominatorS2 + denominatorS1;
      double numerator = pas.getAlternativeHighCost() - pas.getAlternativeLowCost();
      if (numerator != 0) {
        flowShift = numerator / denominator;

        /* debug only, test if shift solves travel time discrepancy, to be removed when it works */
        double diff = (pas.getAlternativeLowCost() + denominatorS1 * flowShift) - (pas.getAlternativeHighCost() + denominatorS2 * -flowShift);
        if (Precision.notEqual(diff, 0.0)) {
          LOGGER.severe("Computation of using derivatives to shift flows between PAS segments does not result in equal travel time after shift, this should not happen");
        }

        // VERIFY CROSSING OF DISCONTINUITY on S1 travel time function - adjust shift if so to mitigate effect
        if (firstS1CongestedLinkSegment == null) {
          /* possible triggering of congestion on s1 due to shift -> passing discontinuity on travel time function */
          flowShift = adjustFlowShiftBasedOnS1SlackFlow(flowShift, slackFlowEstimate);
        }

        // VERIFY CROSSING OF DISCONTINUITY on S2 travel time function - adjust shift if so to mitigate effect
        if (firstS2CongestedLinkSegment != null) {
          double s2SlackFlowEstimate = this.getS2SendingFlow() * (1 - networkLoading.getCurrentFlowAcceptanceFactors()[(int) firstS2CongestedLinkSegment.getId()]);
          flowShift = adjustFlowShiftBasedOnS2SlackFlow(flowShift, s2SlackFlowEstimate);
        }
      }
    }

    /*
     * UE -> if we find equal cost and turn derivative differs but link derivative is expected to be unaffected by flow shift to less restricted out link, we should enforce equal
     * flow distribution (max entropy) to obtain unique solution while also minimising cost (as equal distribution spreads pressure on outlink competition in case of the same entry
     * link).
     */
    if (flowShift == 0) {

      if (!settings.isEnforceMaxEntropyFlowSolution()) {
        LOGGER.info("** equal cost/ equal (link) derivative/non-equal flow - no max entropy required - skip flow shift");
        return flowShift;
      }

      boolean allowCongestedEqualFlowDistribution = sharedCongestedEntry && (firstS1CongestedLinkSegment == null || firstS2CongestedLinkSegment == null);
      this.towardsEqualAlternativeFlowDistribution = pasCostEqual && (pasUncongested || allowCongestedEqualFlowDistribution);
      if (towardsEqualAlternativeFlowDistribution) {

        boolean pasAlternativeFlowsEqual = equal(s1WithEntrySendingFlow, s2WithEntrySendingFlow, EPSILON);
        if (pasAlternativeFlowsEqual) {
          LOGGER.info("** proportional distribution exists under equal cost - skip flow shift");
          return flowShift;
        }

        LOGGER.info("** towards proportional distribution - equal cost/ equal (link) derivative/non-equal flow");
        double proportionalFlow = (s2WithEntrySendingFlow + s1WithEntrySendingFlow) / 2;
        /* can be positive (shift towards s1) or negative (shift towards s2) given that s1 and s2 have equal cost here */
        flowShift = s2WithEntrySendingFlow - proportionalFlow;

        /*
         * do not cap based on slack flow because then we will never achieve the desired result. By not doing so we might open ourselves up for flip-flopping, but the solution is
         * not to limit the change.
         */
      }
    }

    return flowShift;

  }

  /**
   * Initialise by determining the desired flows along each subpath (on the network level)
   */
  public void initialise() {
    /* determine the network flow on the high cost subpath */

    var s2 = pas.getAlternative(false /* high cost */);
    var s1 = pas.getAlternative(true /* low cost */);

    totalEntrySegmentS1S2Flow = new HashMap<>();
    for (var entrySegment : pas.getDivergeVertex().getEntryEdgeSegments()) {
      totalEntrySegmentS1S2Flow.put(entrySegment, Pair.of(0.0, 0.0));
      for (var bush : pas.getRegisteredBushes()) {
        var currTotalS1S2Flow = totalEntrySegmentS1S2Flow.get(entrySegment);
        if (!bush.containsEdgeSegment(entrySegment)) {
          continue;
        }
        bushEntrySegmentS1S2SendingFlows.putIfAbsent(bush, new HashMap<>());
        var entrySegmentS1S2SendingFlows = bushEntrySegmentS1S2SendingFlows.get(bush);

        double s2BushSendingFlow = bush.determineSubPathSendingFlow(entrySegment, s2);
        double newS2Total = currTotalS1S2Flow.second() + s2BushSendingFlow;

        double s1BushSendingFlow = bush.determineSubPathSendingFlow(entrySegment, s1);
        double newS1Total = currTotalS1S2Flow.first() + s1BushSendingFlow;

        totalEntrySegmentS1S2Flow.put(entrySegment, Pair.of(newS1Total, newS2Total));
        entrySegmentS1S2SendingFlows.put(entrySegment, Pair.of(s1BushSendingFlow, s2BushSendingFlow));
      }
    }
  }

  /**
   * We account for the fact that per bush different incoming links to the PAS might be used so each incoming link that is used and that is congested should be the basis for the
   * flow shift instead of the first congested one within the PAS. This is currently not accounted for + if an incoming link is congested, then it has the same alpha for both
   * alternatives BUT the most restricting one might be linked to one of those. If so then we should shift towards the other! This does not exist yet. If neither is the most
   * restricting then revert to situation where we shift as if uncongested as it has no impact. So, split flow shift and execution to per incoming link rather than combining them
   * as we do in run!! Later we can optimise possibly
   * 
   * Each PAS per bush is split in x PASs where x is the number of used in links for each bush
   * 
   * @param theMode        to use
   * @param physicalCost   to use
   * @param virtualCost    to use
   * @param networkLoading to use
   * @param factor         to apply to flow shift
   * @return true when flow is shifted, false otherwise
   */
  public boolean run(Mode theMode, AbstractPhysicalCost physicalCost, AbstractVirtualCost virtualCost, StaticLtmLoadingBushBase<?> networkLoading, double factor) {
    double totalS2SendingFlow = getS2SendingFlow();
    LOGGER.info("******************* PAS FLOW shift " + pas.toString() + "S2 Sending flow: " + totalS2SendingFlow + " cost-diff: " + pas.getReducedCost()
        + " *****************************");
    if (!Precision.positive(totalS2SendingFlow)) {
      LOGGER.warning("no flow on S2 segment of selected PAS, PAS should not exist anymore, this shouldn't happen");
    }

    boolean flowShifted = false;
    for (var entrySegment : pas.getDivergeVertex().getEntryEdgeSegments()) {

      Pair<Double, Double> totalEntrySegmentS1S1SendingFlow = totalEntrySegmentS1S2Flow.get(entrySegment);
      double totalEntrySegmentS2Flow = totalEntrySegmentS1S1SendingFlow.second();
      if (totalEntrySegmentS2Flow <= 0) {
        /* remove this entry segment from the PAS when done as no flow remains on high cost segment */
        totalEntrySegmentS1S2Flow.remove(entrySegment);
        continue;
      }

      /* flow shift based on entry segment - PAS combination */
      double proposedPasflowShift = determineEntrySegmentFlowShift(entrySegment, theMode, physicalCost, virtualCost, networkLoading);
      if (Math.abs(proposedPasflowShift) == 0) {
        continue;
      }

      double entrySegmentPortion = totalEntrySegmentS2Flow / totalS2SendingFlow;
      double proposedProportionalPasflowShift = proposedPasflowShift * entrySegmentPortion * factor;

      /* test for eligibility to reduce to zero flow along S2 */
      activatePasS2RemovalIf(Precision.greaterEqual(proposedProportionalPasflowShift, totalEntrySegmentS2Flow, EPSILON)
          || Precision.greaterEqual(PAS_MIN_S2_FLOW_THRESHOLD, totalEntrySegmentS2Flow, EPSILON));
      if (isPasS2RemovalAllowed()) {

        LOGGER.info(String.format("** Allow removal, proposed shift %.10f exceeds available s2 sending flow %.10f", proposedProportionalPasflowShift, totalEntrySegmentS2Flow));
        /* remove this entry segment from the PAS when done as no flow remains on high cost segment */
        totalEntrySegmentS1S2Flow.remove(entrySegment);
        /* remove all remaining flow */
        proposedProportionalPasflowShift = totalEntrySegmentS2Flow;
      }

      for (var bush : pas.getRegisteredBushes()) {
        if (bush.containsTurnSendingFlow(entrySegment, pas.getFirstEdgeSegment(false))) {

          final Map<EdgeSegment, Pair<Double, Double>> bushEntrySegmentS1S2Flows = bushEntrySegmentS1S2SendingFlows.get(bush);
          var bushEntrySegmentS2Flow = bushEntrySegmentS1S2Flows.get(entrySegment).second();

          /*
           * In case of multiple used bushes for this entry segment -> we cannot let proposed shifts be executed in full because cost is affected and therefore succeeding entries
           * would "overshoot". Hence we apply proposed shift proportionally to contribution to total flow along PAS
           */
          double bushS2Portion = bushEntrySegmentS2Flow / totalEntrySegmentS2Flow;
          double entrySegmentPasflowShift = proposedProportionalPasflowShift * bushS2Portion;

          LOGGER.info(String.format("** Entry segment (" + entrySegment.toString() + ") - Zone (" + bush.getRootZone().getXmlId() + ") - start flow shift: %.10f",
              entrySegmentPasflowShift));

          /* perform the flow shift for the current bush and its attributed portion */
          executeBushFlowShift(bush, entrySegment, entrySegmentPasflowShift, networkLoading.getCurrentFlowAcceptanceFactors());
          flowShifted = true;

          if (smaller(networkLoading.getCurrentFlowAcceptanceFactors()[(int) entrySegment.getId()], 1, EPSILON)) {
            usedCongestedEntryEdgeSegments.add(entrySegment);
          }

          if (isPasS2RemovalAllowed()) {
            /* no flow remaning on S2 for bush, unregister */
            bushEntrySegmentS1S2Flows.remove(entrySegment);
          }
        }
      }
    }

    /* remove zero-flow S2 bushes from PAS */
    var iter = pas.getRegisteredBushes().iterator();
    while (iter.hasNext()) {
      var bush = iter.next();
      final Map<EdgeSegment, Pair<Double, Double>> entrySegmentS1S2Flows = bushEntrySegmentS1S2SendingFlows.get(bush);
      if (entrySegmentS1S2Flows != null && entrySegmentS1S2Flows.isEmpty()) {
        iter.remove();
      }
    }

    return flowShifted;
  }

  /**
   * Sending flow along PAS high cost segment
   * 
   * @return high cost alternative desired flow
   */
  public double getS2SendingFlow() {
    double totalS2 = 0;
    for (var entry : totalEntrySegmentS1S2Flow.entrySet()) {
      totalS2 += entry.getValue().second();
    }
    return totalS2;
  }

  /**
   * Sending flow along PAS low cost segment
   * 
   * @return low cost alternative desired flow
   */
  public double getS1SendingFlow() {
    double totalS1 = 0;
    for (var entry : totalEntrySegmentS1S2Flow.entrySet()) {
      totalS1 += entry.getValue().first();
    }
    return totalS1;
  }

  /**
   * Check to see if last call to {@link #determineEntrySegmentFlowShift(EdgeSegment, Mode, AbstractPhysicalCost, AbstractVirtualCost, StaticLtmLoadingBushBase)}
   * caused a flow shift not trying to equate cost but equate flows given equal cost
   * 
   * @return true when attempting to move to equal distribution of flow across alternatives, false otherwise
   */
  public boolean isTowardsEqualAlternativeFlowDistribution() {
    return towardsEqualAlternativeFlowDistribution;
  }

  /**
   * All used entry Segments that were found to be congested and a flow shift has been applied to
   * 
   * @return set of found edge segments
   */
  public Set<EdgeSegment> getUsedCongestedEntrySegments() {
    return this.usedCongestedEntryEdgeSegments;
  }

}

package org.goplanit.assignment.ltm.sltm;

import static org.goplanit.utils.math.Precision.EPSILON_9;
import static org.goplanit.utils.math.Precision.equal;
import static org.goplanit.utils.math.Precision.smaller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.goplanit.assignment.ltm.sltm.consumer.NMRCollectMostRestrictingTurnConsumer;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingBush;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmNetworkLoading;
import org.goplanit.cost.physical.AbstractPhysicalCost;
import org.goplanit.cost.virtual.AbstractVirtualCost;
import org.goplanit.utils.graph.EdgeSegment;
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

  /** local epsilon used in flow shifting */
  private static final double EPSILON = EPSILON_9;

  /** to operate on */
  protected final Pas pas;

  /** S1 sending flow along (entire) alternative */
  protected double s2SendingFlow;

  /** S2 sending flow along (entire) alternative */
  protected double s1SendingFlow;

  /** Track the desired sending flows for s1 and s2 per origin per entry segment */
  protected final Map<Bush, Map<EdgeSegment, Pair<Double, Double>>> bushEntrySegmentS1S2SendingFlows;

  protected final Set<EdgeSegment> usedCongestedEntryEdgeSegments;

  /** store locally as it is costly-ish to compute */
  protected final int pasMergeVertexNumExitSegments;

  /** reference to settings of the overarching assignment */
  private final StaticLtmSettings staticLtmSettings;

  /**
   * flag indicating of most recent call to {@link #determineEntrySegmentFlowShift(Bush, EdgeSegment, Mode, AbstractPhysicalCost, AbstractVirtualCost, StaticLtmLoadingBush)}
   * identified that flow distribution between s1 and s2 should be made equal.
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
  private EdgeSegment identifyMostRestrictingOutEdgeSegment(EdgeSegment entrySegment, StaticLtmLoadingBush networkLoading) {
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
  private double determinePasAlternativeSlackFlow(StaticLtmLoadingBush networkLoading, boolean lowCost) {
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
        double outflow = networkLoading.getCurrentInflowsPcuH()[linkSegmentId] * networkLoading.getCurrentFlowAcceptanceFactors()[linkSegmentId];
        /* since only a portion is directed to this out link, we can multiply the slack with the reciprocal of the splitting rate */
        double scaledSlackFlow = (1 / splittingRate) * ((PcuCapacitated) exitSegment).getCapacityOrDefaultPcuH() - outflow;
        slackFlow = Math.min(slackFlow, scaledSlackFlow);
      }
      ++index;
    }

    EdgeSegment alternativeEdgeSegment = null;
    EdgeSegment[] alternativeEdgeSegments = pas.getAlternative(lowCost);
    for (index = 0; index < alternativeEdgeSegments.length; ++index) {
      alternativeEdgeSegment = alternativeEdgeSegments[index];
      linkSegmentId = (int) alternativeEdgeSegment.getId();
      /* do not use outflows directly because they are only available on potentially blocking nodes in point queue basic solution scheme */
      double outflow = networkLoading.getCurrentInflowsPcuH()[linkSegmentId] * networkLoading.getCurrentFlowAcceptanceFactors()[linkSegmentId];
      double currSlackflow = ((PcuCapacitated) alternativeEdgeSegment).getCapacityOrDefaultPcuH() - outflow;
      if (smaller(currSlackflow, slackFlow)) {
        slackFlow = currSlackflow;
      }
    }

    return slackFlow;
  }

  /**
   * Constructor
   * 
   * @param pas               to use
   * @param staticLtmSettings
   */
  protected PasFlowShiftExecutor(Pas pas, StaticLtmSettings staticLtmSettings) {
    this.pas = pas;
    this.staticLtmSettings = staticLtmSettings;
    this.bushEntrySegmentS1S2SendingFlows = new HashMap<>();
    this.usedCongestedEntryEdgeSegments = new HashSet<>();
    this.pasMergeVertexNumExitSegments = pas.getMergeVertex().sizeOfExitEdgeSegments();
  }

  /**
   * Perform the flow shift for a given origin. Delegate to conrete class implementation
   * 
   * @param origin                    to perform shift for
   * @param entrySegment              entry segment at hand to apply flow shift for
   * @param bushEntrySegmentFlowShift the absolute shift to apply for the given PAS-origin-entrysegment combination
   * @param flowAcceptanceFactors     to use
   */
  protected abstract void executeOriginFlowShift(Bush origin, EdgeSegment entrySegment, double bushEntrySegmentFlowShift, double[] flowAcceptanceFactors);

  /**
   * For the given PAS-origin-entrysegment determine the flow shift to apply from the high cost to the low cost segment. Depending on the state of the segments we utilise their
   * derivatives of travel time towards flow to determine the optimal shift. In case one or both segments are uncongested, or the congestion occurs on the entry segment while the
   * cost on the PAS is already equal, we propose to shift as much flow as would yield an equal distribution between the alternatives (maximising entropy) in order to obtain a
   * unique solution under equal cost. would expect the segment to transition to congestion.
   * 
   * @param theMode        to use
   * @param physicalCost   to use
   * @param virtualCost    to use
   * @param networkLoading to use
   * @return amount of flow to shift
   */
  protected double determineEntrySegmentFlowShift(Bush origin, EdgeSegment entrySegment, Mode theMode, AbstractPhysicalCost physicalCost, AbstractVirtualCost virtualCost,
      StaticLtmLoadingBush networkLoading) {

    /* obtain derivatives of travel time towards flow for ENTRY+PAS_alternatives and use them to determine the amount of flow to shift */
    // TODO: Currently requires instanceof, so benchmark if not too slow

    var s1S2SubPathSendingFlowPair = bushEntrySegmentS1S2SendingFlows.get(origin).get(entrySegment);
    double s1WithEntrySendingFlow = s1S2SubPathSendingFlowPair.first();
    double s2WithEntrySendingFlow = s1S2SubPathSendingFlowPair.second();
    boolean pasCostEqual = pas.isCostEqual(EPSILON);
    boolean pasAlternativeFlowsEqual = equal(s1WithEntrySendingFlow, s2WithEntrySendingFlow, EPSILON);

    /* prep */
    EdgeSegment firstS2CongestedLinkSegment = null;
    EdgeSegment firstS1CongestedLinkSegment = null;
    boolean congestedFlowShiftAffectsLinkDerivative = true;

    var entrySegmentAlpha = networkLoading.getCurrentFlowAcceptanceFactors()[(int) entrySegment.getId()];
    if (smaller(entrySegmentAlpha, 1, EPSILON)) {

      /* entry segment congested - derivative is only sensitive to most restricted out link, which pertains to one of the PAS alternatives (or none). Identify which */
      EdgeSegment mostRestrictingOutSegment = identifyMostRestrictingOutEdgeSegment(entrySegment, networkLoading);
      if (mostRestrictingOutSegment.idEquals(pas.getFirstEdgeSegment(true))) {
        firstS1CongestedLinkSegment = entrySegment;
      } else if (mostRestrictingOutSegment.idEquals(pas.getFirstEdgeSegment(false))) {
        firstS2CongestedLinkSegment = entrySegment;
      }

      /* we can assume that the shift does not impact other directions than the most restricting one */
      congestedFlowShiftAffectsLinkDerivative = false;
    } else {
      congestedFlowShiftAffectsLinkDerivative = true;
    }

    /* If not set yet, then find PAS alternative's first congested edge segment (not entry segment) */
    Predicate<EdgeSegment> firstCongestedLinkSegment = es -> networkLoading.getCurrentFlowAcceptanceFactors()[(int) es.getId()] < 1;
    if (firstS1CongestedLinkSegment == null) {
      firstS1CongestedLinkSegment = pas.matchFirst(true, /* low cost */ firstCongestedLinkSegment);
    }
    if (firstS2CongestedLinkSegment == null) {
      firstS2CongestedLinkSegment = pas.matchFirst(false /* high cost */, firstCongestedLinkSegment);
    }
    boolean pasUncongested = firstS1CongestedLinkSegment == null && firstS2CongestedLinkSegment == null;

    double denominatorS2 = 0;
    double denominatorS1 = 0;
    if (!pasUncongested) {
      /* derivative of link cost based on first congested link */
      if (firstS1CongestedLinkSegment != null) {
        denominatorS1 = getDTravelTimeDFlow(theMode, physicalCost, virtualCost, firstS1CongestedLinkSegment);
      }
      if (firstS2CongestedLinkSegment != null) {
        denominatorS2 = getDTravelTimeDFlow(theMode, physicalCost, virtualCost, firstS2CongestedLinkSegment);
      }
    }

    /*
     * UE -> if we find equal cost and turn derivative differs but link derivative is expected to be unaffected by flow shift to less restricted out link, we should enforce equal
     * flow distribution (max entropy) to obtain unique solution while also minimising cost (as equal distribution spreads pressure on outlink competition in case of the same entry
     * link).
     */
    this.towardsEqualAlternativeFlowDistribution = pasCostEqual && (pasUncongested || !congestedFlowShiftAffectsLinkDerivative);

    double flowShift = 0;
    if (towardsEqualAlternativeFlowDistribution) {
      if (pasAlternativeFlowsEqual) {
        LOGGER.severe("** proportional distribution exists under equal cost - skip flow shift");
        return flowShift;
      }
      if (!staticLtmSettings.isEnforceMaxEntropyFlowSolution()) {
        LOGGER.severe("** equal cost - no equal flow distribution enforced - skip flow shift");
        return flowShift;
      }
      LOGGER.severe("** towards proportional distribution - equal cost/ equal (link) derivative/non-equal flow");
      double proportionalFlow = (s2WithEntrySendingFlow + s1WithEntrySendingFlow) / 2;
      /* can be positive (shift towards s1) or negative (shift towards s2) given that s1 and s2 have equal cost here */
      double proposedFlowShift = s2WithEntrySendingFlow - proportionalFlow;
      /* slack flow estimate can be from s1 (when shifting to s1) or s2 (when shifting to s2) */
      double slackFlowEstimate = determinePasAlternativeSlackFlow(networkLoading, Precision.positive(proposedFlowShift, EPSILON));

      /* proportional shift limited to expected state change + delta */
      double cappedAdjustedProposedFlowShift = Math.min(Math.abs(proposedFlowShift) - 10, Math.max(0, slackFlowEstimate)) + 10;
      return proposedFlowShift > 0 ? cappedAdjustedProposedFlowShift : -cappedAdjustedProposedFlowShift;

    } else {
      double slackFlowEstimate = determinePasAlternativeSlackFlow(networkLoading, true);
      if (pasUncongested) {

        LOGGER.severe("** uncongested - towards S1 - unequal cost");
        /* s1 & S2 UNCONGESTED - no derivative estimate possible (denominator zero) */
        /* move all towards cheaper alternative limited by slack + delta */
        double proposedFlowShift = Math.min(s2WithEntrySendingFlow - 10, slackFlowEstimate) + 10;
        return adjustFlowShiftBasedOnS1SlackFlow(proposedFlowShift, slackFlowEstimate);

      } else

      if (pasCostEqual) {
        LOGGER.severe("** one or both alternatives congested - towards S1 - near equal cost (<10^-9)");
      } else {
        LOGGER.severe("** one or both alternatives congested - towards S1 - unequal cost");
      }
      /* s1 and/or s2 congested - derivative based flow shift possible */
      // tauw_s1 + dtauw_s1/ds_1 * (-flowShift) = tauw_s2 + dtauw_s2/ds_2 * (flowShift) we find:
      // flowShift = (tauw_s2-tauw_s1)/(1/v_s1_first_bottleneck + 1/v_s2_first_bottleneck))
      double denominator = denominatorS2 + denominatorS1;
      double numerator = pas.getAlternativeHighCost() - pas.getAlternativeLowCost();
      flowShift = numerator / denominator;

      /* debug only, test if shift solves travel time discrepancy, to be removed when it works */
      double diff = (pas.getAlternativeLowCost() + denominatorS1 * flowShift) - (pas.getAlternativeHighCost() + denominatorS2 * -flowShift);
      if (Precision.notEqual(diff, 0.0)) {
        LOGGER.severe("Computation of using derivatives to shift flows between PAS segments does not result in equal travel time after shift, this should not happen");
      }

      flowShift = Math.min(s2WithEntrySendingFlow, numerator / denominator);

      // VERIFY CROSSING OF DISCONTINUITY on S1 travel time function - adjust shift if so to mitigate effect
      if (firstS1CongestedLinkSegment == null) {
        /* possible triggering of congestion on s1 due to shift -> passing discontinuity on travel time function */
        flowShift = adjustFlowShiftBasedOnS1SlackFlow(flowShift, slackFlowEstimate);
      }

      // VERIFY CROSSING OF DISCONTINUITY on S2 travel time function - adjust shift if so to mitigate effect
      if (firstS2CongestedLinkSegment != null) {
        double s2SlackFlowEstimate = s2WithEntrySendingFlow * (1 - networkLoading.getCurrentFlowAcceptanceFactors()[(int) firstS2CongestedLinkSegment.getId()]);
        flowShift = adjustFlowShiftBasedOnS2SlackFlow(flowShift, s2SlackFlowEstimate);
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

    s2SendingFlow = 0;
    s1SendingFlow = 0;
    for (var origin : pas.getOrigins()) {
      var entrySegmentS1S2SendingFlows = new TreeMap<EdgeSegment, Pair<Double, Double>>();
      for (var entrySegment : pas.getDivergeVertex().getEntryEdgeSegments()) {
        if (!origin.containsEdgeSegment(entrySegment)) {
          continue;
        }

        double s2BushSendingFlow = origin.determineSubPathSendingFlow(entrySegment, s2);
        s2SendingFlow += s2BushSendingFlow;

        double s1BushSendingFlow = origin.determineSubPathSendingFlow(entrySegment, s1);
        s1SendingFlow += s1BushSendingFlow;

        entrySegmentS1S2SendingFlows.put(entrySegment, Pair.of(s1BushSendingFlow, s2BushSendingFlow));
      }

      if (!entrySegmentS1S2SendingFlows.isEmpty()) {
        bushEntrySegmentS1S2SendingFlows.put(origin, entrySegmentS1S2SendingFlows);
      }
    }
  }

  /**
   * updated version --> we account for the fact that per origin bush different incoming links to the PAS might be used -> each incoming link that is used and that is congested
   * should be the basis for the flow shift instead of the first congested one within the PAS. This is currently not accounted for + if an incoming link is congested, then it has
   * the same alpha for both alternatives BUT the most restricting one might be linked to one of those. If so then we should shift towards the other! This does not exist yet. If
   * neither is the most restricting then revert to situation where we shift as if uncongested as it has no impact. + So -> split flow shift and execution to per incoming link
   * rather than combining them as we do in run!! Later we can optimise possibly
   * 
   * Each PAS per origin is split in x PASs where x is the number of used in links for each bush
   * 
   * @param theMode
   * @param physicalCost
   * @param virtualCost
   * @param networkLoading
   * @return
   */
  public boolean run(Mode theMode, AbstractPhysicalCost physicalCost, AbstractVirtualCost virtualCost, StaticLtmLoadingBush networkLoading) {
    List<Bush> originsWithoutRemainingPasFlow = new ArrayList<>();
    LOGGER.severe("** PAS FLOW shift " + pas.toString());

    boolean flowShifted = false;
    double totalPasS1S2SendingFlow = getS1SendingFlow() + getS2SendingFlow();
    for (var origin : pas.getOrigins()) {

      LOGGER.severe("** Origin" + origin.getOrigin().getXmlId());

      final Map<EdgeSegment, Pair<Double, Double>> entrySegmentS1S2Flows = bushEntrySegmentS1S2SendingFlows.get(origin);

      /* prep - origin */
      for (var entrySegment : pas.getDivergeVertex().getEntryEdgeSegments()) {
        if (origin.containsTurnSendingFlow(entrySegment, pas.getFirstEdgeSegment(false))) {

          var entrySegmentS1S2SubPathSendingFlowPair = entrySegmentS1S2Flows.get(entrySegment);
          double bushEntrySegmentS1Flow = entrySegmentS1S2SubPathSendingFlowPair.first();
          double bushEntrySegmentS2Flow = entrySegmentS1S2SubPathSendingFlowPair.second();

          LOGGER.severe("** PAS-origin-entry-segment " + entrySegment.toString());
          /*
           * split each PAS in |entrySegment x PAS| shifts as each entry segment (depending on whether it is congested or not) might impact the flow shift that is to be executed
           */

          /* flow shift based on entry segment - PAS combination */
          double proposedEntrySegmentPasflowShift = determineEntrySegmentFlowShift(origin, entrySegment, theMode, physicalCost, virtualCost, networkLoading);
          if (equal(proposedEntrySegmentPasflowShift, 0, EPSILON)) {
            continue;
          }

          /*
           * In case of multiple used entry segments -> we cannot let proposed shifts be executed in full because cost is affected and therefore succeeding entries would
           * "overshoot". Hence we apply proposed shift proportionally to contribution to total flow along PAS
           */
          double portion = (bushEntrySegmentS1Flow + bushEntrySegmentS2Flow) / totalPasS1S2SendingFlow;
          double entrySegmentPasflowShift = proposedEntrySegmentPasflowShift * portion;

          if (Precision.greaterEqual(entrySegmentPasflowShift, bushEntrySegmentS2Flow)) {
            /* remove this origin from the PAS when done as no flow remains on high cost segment */
            entrySegmentS1S2Flows.remove(entrySegment);
            /* remove what we can */
            entrySegmentPasflowShift = bushEntrySegmentS2Flow;
          }

          /* perform the flow shift for the current bush and its attributed portion */
          executeOriginFlowShift(origin, entrySegment, entrySegmentPasflowShift, networkLoading.getCurrentFlowAcceptanceFactors());
          flowShifted = true;

          if (smaller(networkLoading.getCurrentFlowAcceptanceFactors()[(int) entrySegment.getId()], 1, EPSILON)) {
            usedCongestedEntryEdgeSegments.add(entrySegment);
          }
        }
      }

      /* all flow of all entry segments has been removed from S2, origin bush no longer utilises PAS, mark for removal */
      if (entrySegmentS1S2Flows.isEmpty()) {
        originsWithoutRemainingPasFlow.add(origin);
      }
    }

    /* remove irrelevant bushes */
    pas.removeOrigins(originsWithoutRemainingPasFlow);

    return flowShifted;
  }

  /**
   * Sending flow along PAS high cost segment
   * 
   * @return high cost alternative desired flow
   */
  public double getS2SendingFlow() {
    return s2SendingFlow;
  }

  /**
   * Sending flow along PAS low cost segment
   * 
   * @return low cost alternative desired flow
   */
  public double getS1SendingFlow() {
    return s1SendingFlow;
  }

  /**
   * Check to see if last call to {@link #determineEntrySegmentFlowShift(Bush, EdgeSegment, Mode, AbstractPhysicalCost, AbstractVirtualCost, StaticLtmLoadingBush)} caused a flow
   * shift not trying to equate cost but equate flows given equal cost
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

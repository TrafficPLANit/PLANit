package org.goplanit.assignment.ltm.sltm;

import static org.goplanit.utils.math.Precision.EPSILON_9;
import static org.goplanit.utils.math.Precision.equal;
import static org.goplanit.utils.math.Precision.positive;
import static org.goplanit.utils.math.Precision.smaller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  /** Track the desired sending flows for s1 and s2 per origin */
  protected final Map<Bush, Pair<Double, Double>> bushS1S2SendingFlows;

  /** store locally as it is costly-ish to compute */
  protected final int pasMergeVertexNumExitSegments;

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
    var lastS1Segment = pas.getLastEdgeSegment(lowCost);
    double slackFlow = Double.POSITIVE_INFINITY;

    Array1D<Double> splittingRates = networkLoading.getSplittingRateData().getSplittingRates(lastS1Segment);

    int index = 0;
    int linkSegmentId = -1;

    for (var exitSegment : lastS1Segment.getDownstreamVertex().getExitEdgeSegments()) {
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

    EdgeSegment s1EdgeSegment = null;
    EdgeSegment[] s1 = pas.getAlternative(true);
    for (index = 0; index < s1.length; ++index) {
      s1EdgeSegment = s1[index];
      linkSegmentId = (int) s1EdgeSegment.getId();
      /* do not use outflows directly because they are only available on potentially blocking nodes in point queue basic solution scheme */
      double outflow = networkLoading.getCurrentInflowsPcuH()[linkSegmentId] * networkLoading.getCurrentFlowAcceptanceFactors()[linkSegmentId];
      double currSlackflow = ((PcuCapacitated) s1EdgeSegment).getCapacityOrDefaultPcuH() - outflow;
      if (smaller(currSlackflow, slackFlow)) {
        slackFlow = currSlackflow;
      }
    }

    return slackFlow;
  }

  /**
   * Constructor
   * 
   * @param pas to use
   */
  protected PasFlowShiftExecutor(Pas pas) {
    this.pas = pas;
    this.bushS1S2SendingFlows = new HashMap<>();
    this.pasMergeVertexNumExitSegments = pas.getMergeVertex().sizeOfExitEdgeSegments();
  }

  // old (not entry segment specific)
//  /**
//   * Perform the flow shift for a given origin. Delegate to conrete class implementation
//   * 
//   * @param origin                to perform shift for
//   * @param bushFlowShift         the absolute shift to apply for the given origin
//   * @param flowAcceptanceFactors to use
//   */
//  protected abstract void executeOriginFlowShift(Bush origin, double bushFlowShift, double[] flowAcceptanceFactors);

  /**
   * Perform the flow shift for a given origin. Delegate to conrete class implementation
   * 
   * @param origin                    to perform shift for
   * @param entrySegment              entry segment at hand to apply flow shift for
   * @param bushEntrySegmentFlowShift the absolute shift to apply for the given PAS-origin-entrysegment combination
   * @param flowAcceptanceFactors     to use
   */
  protected abstract void executeOriginFlowShift(Bush origin, EdgeSegment entrySegment, double bushEntrySegmentFlowShift, double[] flowAcceptanceFactors);

// no longer needed in new setup as it is integrated in the run already
//  /**
//   * Shift flows for the PAS given the currently known costs and smoothing procedure to apply
//   * 
//   * @param flowShiftPcuH         amount to shift from high cost to low cost segment
//   * @param flowAcceptanceFactors to use
//   * @return true when flow shifted, false otherwise
//   */
//  protected boolean executeFlowShift(double flowShiftPcuH, double[] flowAcceptanceFactors) {
//
//    List<Bush> originsWithoutRemainingPasFlow = new ArrayList<>();
//    LOGGER.severe("** PAS FLOW shift " + pas.toString());
//
//    for (var origin : pas.getOrigins()) {
//
//      /* prep - origin */
//      final Pair<Double, Double> bushS1S2Flow = bushS1S2SendingFlows.get(origin);
//      double bushS2Flow = bushS1S2Flow.second();
//
//      LOGGER.severe("** Origin" + origin.getOrigin().getXmlId().toString());
//
//      /* Bush flow portion */
//      double bushPortion = positive(getS2SendingFlow()) ? Math.min(bushS2Flow / getS2SendingFlow(), 1) : 1;
//      double bushFlowShift = flowShiftPcuH * bushPortion;
//      if (greaterEqual(bushFlowShift, bushS2Flow)) {
//        /* remove this origin from the PAS when done as no flow remains on high cost segment */
//        originsWithoutRemainingPasFlow.add(origin);
//        /* remove what we can */
//        bushFlowShift = bushS2Flow;
//      }
//
//      /* perform the flow shift for the current bush and its attributed portion */
//      executeOriginFlowShift(origin, bushFlowShift, flowAcceptanceFactors);
//
//    }
//
//    /* remove irrelevant bushes */
//    pas.removeOrigins(originsWithoutRemainingPasFlow);
//
//    return true;
//  }

//  /**
//   * For the given PAS determine the flow shift to apply from the high cost to the low cost segment. Depending on the state of the segments we utilise their derivatives of travel
//   * time towards flow to determine the optimal shift. In case one or both segments are uncongested, we shift as much as possible conditional on the available slack for when we
//   * would expect the segment to transition to congestion.
//   * 
//   * @param theMode        to use
//   * @param physicalCost   to use
//   * @param virtualCost    to use
//   * @param networkLoading to use
//   * @return amount of flow to shift
//   */
//  protected double determineFlowShift(Mode theMode, AbstractPhysicalCost physicalCost, AbstractVirtualCost virtualCost, StaticLtmLoadingBush networkLoading) {
//
//    /* obtain derivatives of travel time towards flow for PAS segments. */
//    // TODO: Currently requires instanceof, so benchmark if not too slow
//    double denominatorS2 = 0;
//    double denominatorS1 = 0;
//
//    Predicate<EdgeSegment> firstCongestedLinkSegment = es -> networkLoading.getCurrentFlowAcceptanceFactors()[(int) es.getId()] < 1;
//    var firstS2CongestedLinkSegment = pas.matchFirst(false /* high cost */, firstCongestedLinkSegment);
//    var firstS1CongestedLinkSegment = pas.matchFirst(true, /* low cost */ firstCongestedLinkSegment);
//
//    if (firstS1CongestedLinkSegment == null) {
//      // cheap option not congested, derivative of zero
//      denominatorS1 = 0;
//    } else {
//      denominatorS1 = getDTravelTimeDFlow(theMode, physicalCost, virtualCost, firstS1CongestedLinkSegment);
//    }
//
//    if (firstS2CongestedLinkSegment == null) {
//      /* expensive option not congested, derivative of zero */
//      denominatorS2 = 0;
//    } else {
//      denominatorS2 = getDTravelTimeDFlow(theMode, physicalCost, virtualCost, firstS2CongestedLinkSegment);
//    }
//
//    Double s1SlackFlowEstimate = null;
//    if (firstS1CongestedLinkSegment == null) {
//      s1SlackFlowEstimate = determineS1SlackFlow(networkLoading);
//    }
//
//    /* s1 & S2 UNCONGESTED - no derivative estimate possible (denominator zero) */
//    if (firstS1CongestedLinkSegment == null && firstS2CongestedLinkSegment == null) {
//      /*
//       * propose to move exactly as much as the point that changes in state (+ small margin to trigger state change and be able to deal with situation that there is 0 slack flow)
//       */
//      double proposedFlowShift = Math.min(getS2SendingFlow() - 10, s1SlackFlowEstimate) + 10;
//      return adjustFlowShiftBasedOnS1SlackFlow(proposedFlowShift, s1SlackFlowEstimate);
//    }
//
//    /* s1 and/or s2 congested - derivative estimate possible */
//    // tauw_s1 + dtauw_s1/ds_1 * (-flowShift) = tauw_s2 + dtauw_s2/ds_2 * (flowShift) we find:
//    // flowShift = (tauw_s2-tauw_s1)/(1/v_s1_first_bottleneck + 1/v_s2_first_bottleneck))
//    double denominator = denominatorS2 + denominatorS1;
//    double numerator = pas.getAlternativeHighCost() - pas.getAlternativeLowCost();
//    double flowShift = Math.min(getS2SendingFlow(), numerator / denominator);
//
//    /* debug only, test if shift solves travel time discrepancy, to be removed when it works */
//    double diff = (pas.getAlternativeLowCost() + denominatorS1 * flowShift) - (pas.getAlternativeHighCost() + denominatorS2 * -flowShift);
//    if (notEqual(diff, 0.0)) {
//      LOGGER.severe("Computation of using derivatives to shift flows between PAS segments does not result in equal travel time after shift, this should not happen");
//    }
//
//    // VERIFY CROSSING OF DISCONTINUITY on S1 travel time function - adjust shift if so to mitigate effect
//    if (firstS1CongestedLinkSegment == null) {
//      /* possible triggering of congestion on s1 due to shift -> passing discontinuity on travel time function */
//      flowShift = adjustFlowShiftBasedOnS1SlackFlow(flowShift, s1SlackFlowEstimate);
//    }
//
//    // VERIFY CROSSING OF DISCONTINUITY on S2 travel time function - adjust shift if so to mitigate effect
//    if (firstS2CongestedLinkSegment != null) {
//      double s2SlackFlowEstimate = getS2SendingFlow() * (1 - networkLoading.getCurrentFlowAcceptanceFactors()[(int) firstS2CongestedLinkSegment.getId()]);
//      flowShift = adjustFlowShiftBasedOnS2SlackFlow(flowShift, s2SlackFlowEstimate);
//    }
//
//    return flowShift;
//  }

  // TODO
  // IF COSTS ARE EQUAL AND (LINK FLOW) DERIVATIVES ARE EQUAL -> IMPOSE PROPORTIONALITY CONSTRAINS
  // IF COSTS ARE NOT EQUAL -> MOVE FLOW TO CHEAP ALTERNATIVE REGARDLESS OF MOST RESTRICTING TURN
  /*
   * 1) Entry segment is first bottleneck for both alternatives -> derivative of alpha for both alternatives is equal -> no flow shift can be determined analytically this way </br>
   * 2) However, derivative based on alpha is based on link flow rather than turn flow, whereas only one turn is typically most restricting so... </br> 3) We should use the
   * derivative towards the turn flow rather than the link flow instead (we normally do not do this because it is costly) </br> 4) the node model can only provide the most
   * restrictive turn flow, so we assume the other turn flow is not restricted in that case </br> 5) using the turn flow derivative estimates
   */
  protected double determineEntrySegmentFlowShift(Bush origin, EdgeSegment entrySegment, Mode theMode, AbstractPhysicalCost physicalCost, AbstractVirtualCost virtualCost,
      StaticLtmLoadingBush networkLoading) {
    /* obtain derivatives of travel time towards flow for ENTRY+PAS_alternatives and use them to determine the amount of flow to shift */
    // TODO: Currently requires instanceof, so benchmark if not too slow

    double denominatorS2 = 0;
    double denominatorS1 = 0;
    EdgeSegment firstS2CongestedLinkSegment = null;
    EdgeSegment firstS1CongestedLinkSegment = null;
    var firstS1Segment = pas.getFirstEdgeSegment(true);
    var firstS2Segment = pas.getFirstEdgeSegment(false);

    boolean pasUncongested = true;
    boolean flowShiftAffectsLinkDerivative = true;
    double slackFlowEstimate = 0;
    var entrySegmentAlpha = networkLoading.getCurrentFlowAcceptanceFactors()[(int) entrySegment.getId()];
    if (smaller(entrySegmentAlpha, 1, EPSILON)) {
      pasUncongested = false;
      /*
       * Entry segment based - derivative cost is shared by both alternatives -> utilise node model insights based on most restrictive out link to determine derivative per PAS
       * alternative (based on exit link) instead
       */
      firstS2CongestedLinkSegment = firstS1CongestedLinkSegment = entrySegment;
      var consumer = new NMRCollectMostRestrictingTurnConsumer(entrySegment); // collect most restricting turn for entry segment
      StaticLtmNetworkLoading.performNodeModelUpdate(entrySegment.getDownstreamVertex(), consumer, networkLoading);

      EdgeSegment mostRestrictingOutSegment = consumer.getMostRestrictingOutSegment();
      if (mostRestrictingOutSegment == null) {
        LOGGER.severe(String.format("Expected most restricting our segment to be present given that incoming segment (%s) is congested, but not found, this shouldn't happen",
            entrySegment.getXmlId()));
      }

      /* get derivative of turn cost towards turn flow + slack estimate for non-most restricting turn */
      if (mostRestrictingOutSegment.idEquals(pas.getFirstEdgeSegment(true))) {
        denominatorS1 = getDTravelTimeDFlow(theMode, physicalCost, virtualCost, firstS1CongestedLinkSegment);
        slackFlowEstimate = ((PcuCapacitated) firstS2Segment).getCapacityOrDefaultPcuH() - networkLoading.getCurrentInflowsPcuH()[(int) firstS2Segment.getId()];
      } else if (mostRestrictingOutSegment.idEquals(pas.getFirstEdgeSegment(false))) {
        denominatorS2 = getDTravelTimeDFlow(theMode, physicalCost, virtualCost, firstS2CongestedLinkSegment);
        slackFlowEstimate = ((PcuCapacitated) firstS1Segment).getCapacityOrDefaultPcuH() - networkLoading.getCurrentInflowsPcuH()[(int) firstS1Segment.getId()];
      } else {
        /* assumed both are unaffected by changes to turn sending flow */
        var slackFlowEstimateS1 = ((PcuCapacitated) firstS1Segment).getCapacityOrDefaultPcuH() - networkLoading.getCurrentInflowsPcuH()[(int) firstS1Segment.getId()];
        var slackFlowEstimateS2 = ((PcuCapacitated) firstS2Segment).getCapacityOrDefaultPcuH() - networkLoading.getCurrentInflowsPcuH()[(int) firstS2Segment.getId()];
        slackFlowEstimate = Math.min(slackFlowEstimateS1, slackFlowEstimateS2);
      }

      /* when we have slack on turn sending flow -> link level derivative is not affected when shifting flow */
      flowShiftAffectsLinkDerivative = !positive(slackFlowEstimate, EPSILON);

    } else {
      /* PAS based - derivative of any link is NOT shared by both alternatives */
      Predicate<EdgeSegment> firstCongestedLinkSegment = es -> networkLoading.getCurrentFlowAcceptanceFactors()[(int) es.getId()] < 1;
      firstS2CongestedLinkSegment = pas.matchFirst(false /* high cost */, firstCongestedLinkSegment);
      firstS1CongestedLinkSegment = pas.matchFirst(true, /* low cost */ firstCongestedLinkSegment);

      /* get derivative of link cost towards link flow + slack estimate for cheapeast alternative (if uncongested, otherwise it is 0) */
      if (firstS1CongestedLinkSegment != null) {
        denominatorS1 = getDTravelTimeDFlow(theMode, physicalCost, virtualCost, firstS1CongestedLinkSegment);
      } else {
        slackFlowEstimate = determinePasAlternativeSlackFlow(networkLoading, true);
      }
      if (firstS2CongestedLinkSegment != null) {
        denominatorS2 = getDTravelTimeDFlow(theMode, physicalCost, virtualCost, firstS2CongestedLinkSegment);
      }

      pasUncongested = firstS1CongestedLinkSegment == null && firstS2CongestedLinkSegment == null;
    }

    // entry segment specific sending flows NEW
    double s1WithEntrySendingFlow = origin.determineSubPathSendingFlow(entrySegment, pas.getAlternative(true));
    double s2WithEntrySendingFlow = origin.determineSubPathSendingFlow(entrySegment, pas.getAlternative(false));

    /*
     * UE -> if we find equal cost and derivative -> turn flows should be distributed proportionally ceteris paribus, otherwise we do not obtain unique solution note that in case
     * turn derivative differs but link derivative is expected to be unaffected by flow shift to less restricted out link, we also go for proportionality, the latter case reflect
     * entropy maximising solution
     */
    boolean pasCostEqual = pas.isCostEqual(EPSILON);
    boolean pasCostDerivativeEqual = equal(denominatorS1, denominatorS2, EPSILON);
    boolean pasAlternativeFlowsEqual = equal(s1WithEntrySendingFlow, s2WithEntrySendingFlow, EPSILON);
    boolean towardsProportionalFlowDistribution = pasCostEqual && (pasCostDerivativeEqual || !flowShiftAffectsLinkDerivative) && !pasAlternativeFlowsEqual;

    double flowShift = 0;
    if (towardsProportionalFlowDistribution) {
      // TODO: these should be dampened
      double proportionalFlow = (s2WithEntrySendingFlow + s1WithEntrySendingFlow) / 2;
      double proposedFlowShift = proportionalFlow - s2WithEntrySendingFlow;

      /* proportional shift limited to expected state change + delta */
      return Math.min(proposedFlowShift - 10, slackFlowEstimate) + 10;

    } else if (!pasCostEqual && pasUncongested) {

      /* s1 & S2 UNCONGESTED - no derivative estimate possible (denominator zero) */
      /* move all towards cheaper alternative limited by slack + delta */
      double proposedFlowShift = Math.min(s2WithEntrySendingFlow - 10, slackFlowEstimate) + 10;
      return adjustFlowShiftBasedOnS1SlackFlow(proposedFlowShift, slackFlowEstimate);

    } else if (!pasCostEqual) {

      /* s1 and/or s2 congested - derivative based flow shift possible */
      // tauw_s1 + dtauw_s1/ds_1 * (-flowShift) = tauw_s2 + dtauw_s2/ds_2 * (flowShift) we find:
      // flowShift = (tauw_s2-tauw_s1)/(1/v_s1_first_bottleneck + 1/v_s2_first_bottleneck))
      double denominator = denominatorS2 + denominatorS1;
      double numerator = pas.getAlternativeHighCost() - pas.getAlternativeLowCost();
      flowShift = Math.min(s2WithEntrySendingFlow, numerator / denominator);

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
      double s2BushSendingFlow = origin.determineSubPathSendingFlow(s2);
      s2SendingFlow += s2BushSendingFlow;

      double s1BushSendingFlow = origin.determineSubPathSendingFlow(s1);
      s1SendingFlow += s1BushSendingFlow;

      bushS1S2SendingFlows.put(origin, Pair.of(s1BushSendingFlow, s2BushSendingFlow));
    }
  }

//  /**
//   * Perform the flow shift
//   * 
//   * @param theMode        to use
//   * @param physicalCost   to use
//   * @param virtualCost    to use
//   * @param networkLoading to use
//   * @return true when flow was shifted, false otherwise
//   */
//  public boolean run(Mode theMode, AbstractPhysicalCost physicalCost, AbstractVirtualCost virtualCost, StaticLtmLoadingBush networkLoading) {
//    double flowShift = determineFlowShift(theMode, physicalCost, virtualCost, networkLoading);
//    return executeFlowShift(flowShift, networkLoading.getCurrentFlowAcceptanceFactors());
//  }

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

    for (var origin : pas.getOrigins()) {

      LOGGER.severe("** Origin" + origin.getOrigin().getXmlId().toString());

      final Pair<Double, Double> bushS1S2Flow = bushS1S2SendingFlows.get(origin);
      double bushS2Flow = bushS1S2Flow.second();

      /* prep - origin */
      for (var entrySegment : pas.getDivergeVertex().getEntryEdgeSegments()) {
        if (origin.containsTurnSendingFlow(entrySegment, pas.getFirstEdgeSegment(true))) {
          /*
           * split each PAS in |entrySegment x PAS| shifts as each entry segment (depending on whether it is congested or not) might impact the flow shift that is to be executed
           */

          /* flow shift based on entry segment - PAS combination */
          double entrySegmentPasflowShift = determineEntrySegmentFlowShift(origin, entrySegment, theMode, physicalCost, virtualCost, networkLoading);

          if (Precision.greaterEqual(entrySegmentPasflowShift, bushS2Flow)) {
            /* remove this origin from the PAS when done as no flow remains on high cost segment */
            originsWithoutRemainingPasFlow.add(origin);
            /* remove what we can */
            entrySegmentPasflowShift = bushS2Flow;
          }

          /* perform the flow shift for the current bush and its attributed portion */
          executeOriginFlowShift(origin, entrySegment, entrySegmentPasflowShift, networkLoading.getCurrentFlowAcceptanceFactors());
          bushS2Flow -= entrySegmentPasflowShift;
        }
      }
    }

    /* remove irrelevant bushes */
    pas.removeOrigins(originsWithoutRemainingPasFlow);

    return true;
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

}

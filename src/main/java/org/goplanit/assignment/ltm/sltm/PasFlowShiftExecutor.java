package org.goplanit.assignment.ltm.sltm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingBush;
import org.goplanit.cost.physical.AbstractPhysicalCost;
import org.goplanit.cost.virtual.AbstractVirtualCost;
import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.virtual.ConnectoidSegment;
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
   * For the given PAS determine the amount of slack flow on its cheaper alternative, i.e., the minimum difference between the link outflow rate and the capacity across all its
   * link segments, including the link segments beyond its alternative it is directing the flows to. It is assumed the cheap cost alternative of the PAS has already been found to
   * be uncongested and as such should have a zero or higher slack flow.
   * <p>
   * In the special case that it passes through (or directs to) a segment that is at capacity (due to for example one or more of its other in-links being congested), then we return
   * a slack capacity of zero. In that case the caller of this method should likely still move some flow, but must assume that all shifted flow immediately causes congestion
   * 
   * 
   * @param networkLoading to collect outflow rates from
   * @return pair of slack flow and slack capacity ratio
   */
  private double determineS1SlackFlow(StaticLtmLoadingBush networkLoading) {
    var lastS2Segment = pas.getLastEdgeSegment(false);
    double slackFlow = Double.POSITIVE_INFINITY;

    Array1D<Double> splittingRates = networkLoading.getSplittingRateData().getSplittingRates(lastS2Segment);

    int index = 0;
    int linkSegmentId = -1;

    for (var exitSegment : lastS2Segment.getDownstreamVertex().getExitEdgeSegments()) {
      double splittingRate = splittingRates.get(index);
      if (splittingRate > 0) {
        linkSegmentId = (int) exitSegment.getId();
        /* do not use outflows directly because they are only available on potentially blocking nodes in point queue basic solution scheme */
        double outflow = networkLoading.getCurrentInflowsPcuH()[linkSegmentId] * networkLoading.getCurrentFlowAcceptanceFactors()[linkSegmentId];
        /* since only a portion is directed to this out link, we can multiply the slack with the reciprocal of the splitting rate */
        double scaledSlackFlow = (1 / splittingRate) * ((MacroscopicLinkSegment) exitSegment).getCapacityOrDefaultPcuH() - outflow;
        slackFlow = Math.min(slackFlow, scaledSlackFlow);
      }
      ++index;
    }

    MacroscopicLinkSegment linkSegment = null;
    EdgeSegment[] s1 = pas.getAlternative(true);
    for (index = 0; index < s1.length; ++index) {
      linkSegment = (MacroscopicLinkSegment) s1[index];
      linkSegmentId = (int) linkSegment.getId();
      /* do not use outflows directly because they are only available on potentially blocking nodes in point queue basic solution scheme */
      double outflow = networkLoading.getCurrentInflowsPcuH()[linkSegmentId] * networkLoading.getCurrentFlowAcceptanceFactors()[linkSegmentId];
      double currSlackflow = linkSegment.getCapacityOrDefaultPcuH() - outflow;
      if (Precision.smaller(currSlackflow, slackFlow)) {
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

  /**
   * Perform the flow shift for a given origin. Delegate to conrete class implementation
   * 
   * @param origin                to perform shift for
   * @param bushFlowShift         the absolute shift to apply for the given origin
   * @param flowAcceptanceFactors to use
   */
  protected abstract void executeOriginFlowShift(Bush origin, double bushFlowShift, double[] flowAcceptanceFactors);

  /**
   * Shift flows for the PAS given the currently known costs and smoothing procedure to apply
   * 
   * @param flowShiftPcuH         amount to shift from high cost to low cost segment
   * @param flowAcceptanceFactors to use
   * @return true when flow shifted, false otherwise
   */
  protected boolean executeFlowShift(double flowShiftPcuH, double[] flowAcceptanceFactors) {

    List<Bush> originsWithoutRemainingPasFlow = new ArrayList<>();
    LOGGER.severe("** PAS FLOW shift" + pas.toString());

    for (var origin : pas.getOrigins()) {

      /* prep - origin */
      final Pair<Double, Double> bushS1S2Flow = bushS1S2SendingFlows.get(origin);
      double bushS2Flow = bushS1S2Flow.second();

      LOGGER.severe("** Origin" + origin.getOrigin().getXmlId().toString());

      /* Bush flow portion */
      double bushPortion = Precision.positive(getS2SendingFlow()) ? Math.min(bushS2Flow / getS2SendingFlow(), 1) : 1;
      double bushFlowShift = flowShiftPcuH * bushPortion;
      if (Precision.greaterEqual(bushFlowShift, bushS2Flow)) {
        /* remove this origin from the PAS when done as no flow remains on high cost segment */
        originsWithoutRemainingPasFlow.add(origin);
        /* remove what we can */
        bushFlowShift = bushS2Flow;
      }

      /* perform the flow shift for the current bush and its attributed portion */
      executeOriginFlowShift(origin, bushFlowShift, flowAcceptanceFactors);

    }

    /* remove irrelevant bushes */
    pas.removeOrigins(originsWithoutRemainingPasFlow);

    return true;
  }

  /**
   * For the given PAS determine the flow shift to apply from the high cost to the low cost segment. Depending on the state of the segments we utilise their derivatives of travel
   * time towards flow to determine the optimal shift. In case one or both segments are uncongested, we shift as much as possible conditional on the available slack for when we
   * would expect the segment to transition to congestion.
   * 
   * @param theMode        to use
   * @param physicalCost   to use
   * @param virtualCost    to use
   * @param networkLoading to use
   * @return amount of flow to shift
   */
  protected double determineFlowShift(Mode theMode, AbstractPhysicalCost physicalCost, AbstractVirtualCost virtualCost, StaticLtmLoadingBush networkLoading) {

    /* obtain derivatives of travel time towards flow for PAS segments. */
    // TODO: Currently requires instanceof, so benchmark if not too slow
    double denominatorS2 = 0;
    double denominatorS1 = 0;

    Predicate<EdgeSegment> firstCongestedLinkSegment = es -> networkLoading.getCurrentFlowAcceptanceFactors()[(int) es.getId()] < 1;
    var firstS2CongestedLinkSegment = pas.matchFirst(false /* high cost */, firstCongestedLinkSegment);
    var firstS1CongestedLinkSegment = pas.matchFirst(true, /* low cost */ firstCongestedLinkSegment);

    if (firstS1CongestedLinkSegment == null) {
      // cheap option not congested, derivative of zero
      denominatorS1 = 0;
    } else {
      if (firstS1CongestedLinkSegment instanceof MacroscopicLinkSegment) {
        denominatorS1 = physicalCost.getDTravelTimeDFlow(false, theMode, (MacroscopicLinkSegment) firstS1CongestedLinkSegment);
      } else if (firstS1CongestedLinkSegment instanceof ConnectoidSegment) {
        denominatorS1 = virtualCost.getDTravelTimeDFlow(false, theMode, (ConnectoidSegment) firstS1CongestedLinkSegment);
      }
    }

    if (firstS2CongestedLinkSegment == null) {
      /* expensive option not congested, derivative of zero */
      denominatorS2 = 0;
    } else {
      if (firstS2CongestedLinkSegment instanceof MacroscopicLinkSegment) {
        denominatorS2 = physicalCost.getDTravelTimeDFlow(false, theMode, (MacroscopicLinkSegment) firstS2CongestedLinkSegment);
      } else if (firstS2CongestedLinkSegment instanceof ConnectoidSegment) {
        denominatorS2 = virtualCost.getDTravelTimeDFlow(false, theMode, (ConnectoidSegment) firstS2CongestedLinkSegment);
      }
    }

    Double s1SlackFlowEstimate = null;
    if (firstS1CongestedLinkSegment == null) {
      s1SlackFlowEstimate = determineS1SlackFlow(networkLoading);
    }

    /* s1 & S2 UNCONGESTED - no derivative estimate possible (denominator zero) */
    if (firstS1CongestedLinkSegment == null && firstS2CongestedLinkSegment == null) {
      /*
       * propose to move exactly as much as the point that changes in state (+ small margin to trigger state change and be able to deal with situation that there is 0 slack flow)
       */
      double proposedFlowShift = Math.min(getS2SendingFlow() - 10, s1SlackFlowEstimate) + 10;
      return adjustFlowShiftBasedOnS1SlackFlow(proposedFlowShift, s1SlackFlowEstimate);
    }

    /* s1 and/or s2 congested - derivative estimate possible */
    // tauw_s1 + dtauw_s1/ds_1 * (-flowShift) = tauw_s2 + dtauw_s2/ds_2 * (flowShift) we find:
    // flowShift = (tauw_s2-tauw_s1)/(1/v_s1_first_bottleneck + 1/v_s2_first_bottleneck))
    double denominator = denominatorS2 + denominatorS1;
    double numerator = pas.getAlternativeHighCost() - pas.getAlternativeLowCost();
    double flowShift = Math.min(getS2SendingFlow(), numerator / denominator);

    /* debug only, test if shift solves travel time discrepancy, to be removed when it works */
    double diff = (pas.getAlternativeLowCost() + denominatorS1 * flowShift) - (pas.getAlternativeHighCost() + denominatorS2 * -flowShift);
    if (Precision.notEqual(diff, 0.0)) {
      LOGGER.severe("Computation of using derivatives to shift flows between PAS segments does not result in equal travel time after shift, this should not happen");
    }

    // VERIFY CROSSING OF DISCONTINUITY on S1 travel time function - adjust shift if so to mitigate effect
    if (firstS1CongestedLinkSegment == null) {
      /* possible triggering of congestion on s1 due to shift -> passing discontinuity on travel time function */
      flowShift = adjustFlowShiftBasedOnS1SlackFlow(flowShift, s1SlackFlowEstimate);
    }

    // VERIFY CROSSING OF DISCONTINUITY on S2 travel time function - adjust shift if so to mitigate effect
    if (firstS2CongestedLinkSegment != null) {
      double s2SlackFlowEstimate = getS2SendingFlow() * (1 - networkLoading.getCurrentFlowAcceptanceFactors()[(int) firstS2CongestedLinkSegment.getId()]);
      flowShift = adjustFlowShiftBasedOnS2SlackFlow(flowShift, s2SlackFlowEstimate);
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

  /**
   * Perform the flow shift
   * 
   * @param theMode        to use
   * @param physicalCost   to use
   * @param virtualCost    to use
   * @param networkLoading to use
   * @return true when flow was shifted, false otherwise
   */
  public boolean run(Mode theMode, AbstractPhysicalCost physicalCost, AbstractVirtualCost virtualCost, StaticLtmLoadingBush networkLoading) {
    double flowShift = determineFlowShift(theMode, physicalCost, virtualCost, networkLoading);
    return executeFlowShift(flowShift, networkLoading.getCurrentFlowAcceptanceFactors());
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

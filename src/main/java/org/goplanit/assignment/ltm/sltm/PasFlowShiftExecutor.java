package org.goplanit.assignment.ltm.sltm;

import org.goplanit.assignment.ltm.sltm.consumer.NMRCollectMostRestrictingTurnConsumer;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingBushBase;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmNetworkLoading;
import org.goplanit.cost.physical.AbstractPhysicalCost;
import org.goplanit.cost.virtual.AbstractVirtualCost;
import org.goplanit.sdinteraction.smoothing.Smoothing;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.virtual.physical.ConnectoidSegment;
import org.goplanit.utils.pcu.PcuCapacitated;
import org.goplanit.utils.zoning.OdZone;
import org.ojalgo.array.Array1D;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import static org.goplanit.utils.math.Precision.*;

/**
 * Common functionality to conduct a PAS flow shift.
 * 
 * @author markr
 *
 */
public abstract class PasFlowShiftExecutor<V extends DirectedVertex, ES extends EdgeSegment> {

  /**
   * Threshold used to trigger derivatives based on congested situation even if we are not yet congested.
   * This threshold is the difference between those two states on the segment level for that to happen,
   * e.g. if capacity is 1000 and flow is 995, then with a threshold of 10 it would be treated as if it is congested
   * w.r.t. derivative calculation. if te flow would be 985, it would not, and would be considered uncongested.
   */
  public static double UNCONGESTED_AS_CONGESTED_FLOW_THRESHOLD_PCUH = 10;

  /**
   * Logger to use
   */
  private final static Logger LOGGER = Logger.getLogger(PasFlowShiftExecutor.class.getCanonicalName());

  /** track any removed edge segments as a result of a flow shift on a bush level */
  private final Map<ES, Set<RootedBush<V,ES>>> removedEdgeSegmentsForBushes = new TreeMap<>();

  /** track any removed edge segments as a result of a flow shift on a bush level */
  //todo: remove when no longer needed, now identified beforehand via missing s1 links method
  private final Map<ES, Set<RootedBush<V,ES>>> addedEdgeSegmentsForBushes = new TreeMap<>();

  /** track the actually performed flow shifts on S2, to know what to apply to S1 when shifting flow */
  Map<ES, Map<DestinationBush, BushEntryShiftedS2FlowData>> flowShiftedS2BushData = new TreeMap<>();

  /**
   * Verify if entry segment is congested.
   *
   * @param loading to use
   * @param segment to check
   * @return congested or not based on check
   */
  private static boolean isCongested(StaticLtmLoadingBushBase<?> loading, EdgeSegment segment){
    var acceptanceFactor = loading.getCurrentFlowAcceptanceFactors()[(int) segment.getId()];
    return smaller(acceptanceFactor, 1, EPSILON_9);
  }

  /**
   * Verify if segment is near congested using threshold.
   *
   * @param loading to use
   * @param segment to check
   * @param nearCongestionIsCongestionThresholdPcuH threshold which will flag segment as congested when it approaches
   *                                            congestion within this threshold.
   * @return near congested or not based on check
   */
  private static boolean isNearCongested(
          StaticLtmLoadingBushBase<?> loading,
          EdgeSegment segment,
          double nearCongestionIsCongestionThresholdPcuH){

    // check if we are very close to congestion based on threshold, if it falls within threshold flag it as congestion
    // as well
    var inflow = loading.getCurrentInflowsPcuH()[(int)segment.getId()];
    var capacity = ((PcuCapacitated) segment).getCapacityOrDefaultPcuH();
    var slackFlowToCapacity = Math.max(0, capacity - inflow);
    return nearCongestionIsCongestionThresholdPcuH >= slackFlowToCapacity;
  }

  /**
   * Verify if segment is congested. Use threshold in case we want to be more circumspect when reaching
   * congestion. For example when taking derivatives we may want to treat a segment already as congested
   * in case its next segment is very near to congestion. This to avoid overshooting any flow shift steps.
   * The threshold considered is configurable. When set to zero only segments themselves with a flow
   * acceptance factor smaller than one will be considered congested.
   *
   * @param loading to use
   * @param segment to check
   * @param nextEdgeSegment only relevant when threshold is set to non-zero, since if the next segment is near congestion
   *                        only then the current segment will be labelled congested if the next segment falls within
   *                        the threshold provided
   * @param nearCongestionIsCongestionThresholdPcuH threshold which will flag segment as congested when it approaches
   *                                            congestion within this threshold.
   * @return congested or not based on check
   */
  private static <Vs extends DirectedVertex, ESs extends EdgeSegment> boolean isCongested(
          StaticLtmLoadingBushBase<?> loading,
          EdgeSegment segment,
          EdgeSegment nextEdgeSegment,
          double nearCongestionIsCongestionThresholdPcuH){

    boolean realisedCongested = isCongested(loading, segment);
    if(realisedCongested || nearCongestionIsCongestionThresholdPcuH <= 0.0 || nextEdgeSegment == null){
      return realisedCongested;
    }

    // check if we are very close to congestion based on threshold using next seegment, if it falls within
    // threshold flag our current segment is deemed congested as well (we use next segment because reduction of flow
    // then occurs on current segment not the next
    return isNearCongested(loading, nextEdgeSegment, nearCongestionIsCongestionThresholdPcuH);
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
  private static <ESs extends EdgeSegment> double getDTravelTimeDFlow(
          Mode theMode, AbstractPhysicalCost physicalCost, AbstractVirtualCost virtualCost, ESs edgeSegment) {

    if (edgeSegment instanceof MacroscopicLinkSegment) {
      return physicalCost.getDTravelTimeDFlow(false, theMode, (MacroscopicLinkSegment) edgeSegment);
    } else if (edgeSegment instanceof ConnectoidSegment) {
      return virtualCost.getDTravelTimeDFlow(false, theMode, (ConnectoidSegment) edgeSegment);
    } else {
      LOGGER.severe(String.format("Unsupported edge segment (%s) to obtain derivative of cost towards flow from",
              edgeSegment.getXmlId()));
    }

    return 0;
  }

  /**
   * Helper; based on the entry segment and current loading, recompute node model to identify most restricting out
   * edge segment for this entry segment
   *
   * @param entrySegment   to use
   * @param networkLoading to use
   * @return identified most restricting out edge segment
   */
  private static EdgeSegment
  identifyMostRestrictingOutEdgeSegment(
          EdgeSegment entrySegment, StaticLtmLoadingBushBase<?> networkLoading) {
    // collect most restricting turn for entry segment
    var consumer = new NMRCollectMostRestrictingTurnConsumer(entrySegment);
    StaticLtmNetworkLoading.performNodeModelUpdate(entrySegment.getDownstreamVertex(), consumer, networkLoading);

    var mostRestrictingOutSegment = consumer.getMostRestrictingOutSegment();
    if (mostRestrictingOutSegment == null) {
      LOGGER.severe(String.format("Expected most restricting our segment to be present given that " +
                      "incoming segment (%s) is congested, but not found, this shouldn't happen",
              entrySegment.getXmlId()));
    }
    return mostRestrictingOutSegment;
  }

  /**
   * Verify if segment is congested. Use threshold in case we want to be more circumspect when reaching
   * congestion. For example when taking derivatives we may want to treat a segment as congested
   * in case ANY non-uturn exit segment is congested to not overshoot any flow shift steps. The threshold considered is
   * configurable. When set to zero only segments themselves with a flow acceptance factor smaller than one will be
   * considered congested.
   * <p>
   *   TODO: would be better to use PAS splitting rates as input to determine which exist segment are actually relevant
   *     but for now we just use the most restricting approach possible as a strtaing point, so any exit segment near
   *     capacity will trigger the label congested on our segment if it falls within the threshold
   * </p>
   *
   * @param loading to use
   * @param segment to check
   * @param nearCongestionIsCongestionThresholdPcuH threshold which will flag segment as congested when it approaches
   *                                            congestion within this threshold.
   * @return pair indicating what congestion was found - first argument indicates if segment itself is congested, second indicates
   *  if any of its exit segments are near congestion based on threshold
   */
  protected static Pair<Boolean, Boolean> isCongested(
          StaticLtmLoadingBushBase<?> loading,
          EdgeSegment segment,
          double nearCongestionIsCongestionThresholdPcuH){

    boolean realisedCongested = isCongested(loading, segment);
    if(realisedCongested || nearCongestionIsCongestionThresholdPcuH <= 0.0){
      return Pair.of(realisedCongested, realisedCongested);
    }

    // check if we are very close to congestion based on threshold, if it falls within threshold flag it as congestion
    // as well
    boolean nearCongested = false;
    for(var potentialNextSegment : segment.getDownstreamVertex().getExitEdgeSegments()){

      // u-turn is filtered out as it is deemed not a true candidate
      if(potentialNextSegment.hasOppositeDirectionSegment() &&
              potentialNextSegment.getOppositeDirectionSegment()==segment){
        continue;
      }

      if(isNearCongested(loading, potentialNextSegment, nearCongestionIsCongestionThresholdPcuH)){
        nearCongested = true;
        break;
      }
    }

    return Pair.of(realisedCongested, nearCongested);
  }

  /**
   * Convenience method to check if we need to perform added logging for destination
   *
   * @param bush to use for checking
   * @return true when destination is tracked for logging
   */
  protected boolean isDestinationTrackedForLogging(RootedBush<V,ES> bush) {
    return settings.isTrackDestinationForLogging((OdZone) bush.getRootZoneVertex().getParent().getParentZone());
  }

  /**
   * Convenience method to check if we need to perform added logging for destination
   *
   * @return true when destination is tracked for logging
   */
  protected boolean isDestinationTrackedForLogging() {
    return settings.hasTrackOdsForLogging() &&
        pas.getRegisteredBushes().stream().anyMatch(
                b -> isDestinationTrackedForLogging((RootedBush<V, ES>) b));
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
  protected double getDTravelTimeDFlow(
          final Mode theMode,
          final StaticLtmLoadingBushBase<?> networkLoading,
          final AbstractPhysicalCost physicalCost,
          final AbstractVirtualCost virtualCost,
          boolean isLowCostAlternative) {

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
   * Determine the adjusted flow shift by taking the proposed upper bound and reduce it by a
   * designated amount based on the difference between the PAS alternative costs and the provided
   * upperbound reference. When below the minimum allowed number proceed regardless without adjustment.
   *
   * @param proposedFlowShift to use
   * @param upperBoundShift that is ideally the maximum
   * @param discontinuityDampeningFactor to use
   * @return adjusted proposed flow shift (if any)
   */
  private double adjustFlowShiftBasedOnSlackFlow(
          double proposedFlowShift, double upperBoundShift, double discontinuityDampeningFactor) {

    if (proposedFlowShift <= upperBoundShift) {
      return proposedFlowShift;
    }

//    /*
//     * when approaching equilibrium, small shifts should be fully executed, otherwise it takes
//     * forever to converge. With such small flows chances have decreased that overshooting
//     * and triggering a different state has a dramatic effect on the travel time derivative
//     */
//    if (Precision.smaller(proposedFlowShift, minimumAllowedShift)) {
//      return proposedFlowShift;
//    }

    double assumedCongestedShift = proposedFlowShift - upperBoundShift;
    double portion = (1 - pas.getAlternativeLowCost() / pas.getAlternativeHighCost());
    return upperBoundShift + assumedCongestedShift * Math.min(1,discontinuityDampeningFactor);
  }

  /** local epsilon used in flow shifting */
  protected static final double EPSILON = EPSILON_12;

  /**
   * whenever a PAS S2 alternative's flow drops below this threshold for a given bush, we allow the flow shift to move all remaining flow towards the S1 segment across all entry
   * segments and unregister the bush for this PAS as it is no longer deemed a true alternative.
   */
  protected static final double PAS_MIN_S2_FLOW_THRESHOLD = 1;

  /** to operate on */
  protected final Pas<V,ES> pas;

  /** settings to use */
  protected final StaticLtmSettings settings;

  /** store locally as it is costly-ish to compute */
  protected final int pasMergeVertexNumExitSegments;

  /**
   * Constructor
   * 
   * @param pas      to use
   * @param settings to use
   */
  protected PasFlowShiftExecutor(final Pas<V,ES> pas, final StaticLtmSettings settings) {
    this.pas = pas;
    this.settings = settings;
    this.pasMergeVertexNumExitSegments = pas.getMergeVertex().getNumberOfExitEdgeSegments();
  }

  /**
   * Determine the adjusted flow shift by taking the proposed flow shift (s2 sending flow) and reduce it by a
   * designated amount based on the difference between the PAS alternative costs and the assumed s1
   * slack flow (flow estimated to switch from uncongested to congested on the PAS's S1 (low cost) segment)
   *
   * @param s1SlackFlow                  that is expected
   * @param discontinuityDampeningFactor to use
   * @return adjusted proposed flow shift (if any)
   */
  protected double adjustFlowShiftBasedOnS1SlackFlow(
          double proposedFlowShift, double s1SlackFlow, double discontinuityDampeningFactor) {
    return adjustFlowShiftBasedOnSlackFlow(proposedFlowShift, s1SlackFlow, discontinuityDampeningFactor);
  }

  /**
   * Determine the adjusted flow shift by taking the proposed flow shift (s2 sending flow) and it by a
   * designated amount based on the difference between the PAS alternative costs and the assumed
   * s2 slack flow (flow estimated to switch from congested to uncongested on the PAS's S2
   * (high cost) segment)
   *
   * @param s2SlackFlow that is expected
   * @param discontinuityDampeningFactor to use
   * @return adjusted proposed flow shift (if any)
   */
  protected double adjustFlowShiftBasedOnS2SlackFlow(
          double proposedFlowShift, double s2SlackFlow, double discontinuityDampeningFactor) {
    return adjustFlowShiftBasedOnSlackFlow(proposedFlowShift, s2SlackFlow, discontinuityDampeningFactor);
  }

  /**
   * For the given PAS determine the amount of slack flow on chosen alternative, i.e., the minimum difference between the link outflow rate and the capacity across all its link
   * segments, including the link segments beyond its alternative it is directing the flows to. It is assumed the cheap cost alternative of the PAS has already been found to be
   * uncongested and as such should have a zero or higher slack flow.
   * <p>
   * In the special case that it passes through (or directs to) a segment that is at capacity (due to for example one or more of its other in-links being congested), then we return
   * a slack capacity of zero.
   * </p>
   *
   * @param networkLoading to collect outflow rates from
   * @param lowCost        when true determine for low cost alternative, when false for high cost alternative
   * @return pair of slack flow and slack capacity ratio
   */
  protected double determinePasAlternativeSlackFlow(
          StaticLtmLoadingBushBase<?> networkLoading, boolean lowCost) {
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
   * Find first congested segment on PAS for either alternative, note that we do use some slack on when
   * to consider something congested where we treat near capacity flows as congested already
   *
   * @param networkLoading to use
   * @param lowCost flag indicating what alternative to apply
   * @return found segments on alternative, null when not congested, second argument indicates whether it
   *  is truly congested already (true), or near congestion (false) but within threshold applied
   *
   */
  protected Pair<ES, Boolean> populateFirstCongestedEdgeSegmentOnPasAlternative(
          final StaticLtmLoadingBushBase<?> networkLoading, boolean lowCost) {

    ES[] alternative = pas.getAlternative(lowCost);
    int index = 0;
    ES currSegment = alternative[index++];
    ES nextSegment = null;
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
   * Perform the flow shift for a given bush. Delegate to concrete class implementation
   *
   * @param bush                      to perform shift for
   * @param entrySegment              entry segment at hand to apply flow shift for
   * @param bushEntrySegmentFlowShift the absolute shift to apply for the given PAS-bush-entrysegment combination
   * @param flowAcceptanceFactors     to use
   * @return end merge splitting rates of s2 to be used in s1 flow shift
   */
  protected abstract double[] executeBushS2FlowShift(
          final RootedBush<V,ES> bush,
          final ES entrySegment,
          double bushEntrySegmentFlowShift,
          final double[] flowAcceptanceFactors);

  /**
   * Perform the flow shift for a given bush. Delegate to concrete class implementation
   *
   * @param bush                      to perform shift for
   * @param entrySegment              entry segment at hand to apply flow shift for
   * @param bushEntrySegmentFlowShift the absolute shift to apply for the given PAS-bush-entrysegment combination
   * @param flowAcceptanceFactors     to use
   * @param endMergeSplittingRates    end merge splitting rates of s2 to be used in s1 flow shift
   */
  protected abstract void executeBushS1FlowShift(
          final RootedBush<V,ES> bush,
          final ES entrySegment,
          double bushEntrySegmentFlowShift,
          final double[] flowAcceptanceFactors,
          double[] endMergeSplittingRates);

  /**
   * Perform the flow shift for a given bush. Delegate to concrete class implementation
   * 
   * @param bush                      to perform shift for
   * @param entrySegment              entry segment at hand to apply flow shift for
   * @param bushEntrySegmentFlowShift the absolute shift to apply for the given PAS-bush-entrysegment combination
   * @param flowAcceptanceFactors     to use
   * @deprecated to be replaced by separate calls
   */
  @Deprecated
  public void executeBushFlowShift(
          final RootedBush<V,ES> bush,
          final ES entrySegment,
          double bushEntrySegmentFlowShift,
          final double[] flowAcceptanceFactors){

    /* shift flows for S2 */
    var bushS2MergeExitSplittingRates =
            executeBushS2FlowShift(bush, entrySegment, bushEntrySegmentFlowShift, flowAcceptanceFactors);
    /* shift flows for S1 */
    executeBushS1FlowShift(bush, entrySegment, bushEntrySegmentFlowShift, flowAcceptanceFactors, bushS2MergeExitSplittingRates);

  }

  /**
   * Determining the currently available desired flows along both the high and low-cost alternatives.
   * this should be done before any flow shifts have been conducted because otherwise the Network Loading
   * acceptance factors and current bush flows are no longer consistent.
   *
   * @param flowAcceptanceFactors to use
   */
  public abstract void stepOneDetermineNetworkLoadingConsistentS1S2EntrySendingFlows(
          double[] flowAcceptanceFactors);

  public abstract Map<ES, Double> determineProposedFlowShiftByEntrySegment(
          Mode theMode,
          AbstractPhysicalCost physicalCost,
          AbstractVirtualCost virtualCost,
          StaticLtmLoadingBushBase<?> networkLoading,
          double discontinuityDampeningFactor);

  /**
   * For each bush on this PAS determine if any link segments are not yet on the S1 that would be added
   * if flow were to be shifted.
   *
   * @return found edge segments missing from a bush
   */
  public Map<ES, Set<RootedBush<V,ES>>> findS1MissingLinkSegmentsByBush() {
    var missingLinkSegmentsByBush = new TreeMap<ES, Set<RootedBush<V,ES>>>();
    var s1 = this.pas.getAlternative(true);
    for(var linkSegment : s1){
      for(var bush : this.pas.getRegisteredBushes()){
        if(!bush.contains((ES) linkSegment)){
          missingLinkSegmentsByBush.putIfAbsent((ES) linkSegment, new TreeSet<>());
          missingLinkSegmentsByBush.get(linkSegment).add(bush);
        }
      }
    }
    return missingLinkSegmentsByBush;
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
  public abstract boolean performS2FlowShift(
      Map<ES, Double> proposedFlowShifts,
      Mode theMode,
      StaticLtmLoadingBushBase<?> networkLoading,
      Smoothing smoothing,
      boolean logAll);

  /**
   * Perform S1 flow shift assuming the S2 flow shift has already been done (is prerequisite)
   *
   * @param theMode to use
   * @param networkLoading to apply
   */
  public abstract void performS1FlowShift(
          Mode theMode, StaticLtmLoadingBushBase<?> networkLoading);

  /**
   * Sending flow along PAS high cost segment
   * 
   * @return high cost alternative desired flow
   */
  public abstract double getS2SendingFlow();

  /**
   * Sending flow along PAS low cost segment
   * 
   * @return low cost alternative desired flow
   */
  public abstract double getS1SendingFlow();

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
  public Map<ES, Set<RootedBush<V,ES>>> getBushRemovedLinkSegments() {
    return removedEdgeSegmentsForBushes;
  }

  /**
   * access bushes that have added link segments due to a flow shift
   *
   * @return tracked findings or empty map
   */
  public Map<ES, Set<RootedBush<V,ES>>> getBushAddedLinkSegments() {
    return addedEdgeSegmentsForBushes;
  }

  /**
   * access bushes that have removed the given link segment due to a flow shift
   *
   * @param linkSegment to check for
   * @return tracked findings or empty list
   */
  public Set<RootedBush<V,ES>> getBushRemovedLinkSegments(ES linkSegment) {
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
  public Set<RootedBush<V,ES>> getBushAddedLinkSegments(ES linkSegment) {
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
          RootedBush<V,ES> bush, ES linkSegment){
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
          RootedBush<V,ES> bush, ES linkSegment){
    if(settings.hasTrackOdsForLogging() && isDestinationTrackedForLogging(bush)){
      LOGGER.info(String.format(
          "           Added link segment (%s) to bush (%s)",
          linkSegment.getIdsAsString(), bush.getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
    }
    getBushAddedLinkSegments(linkSegment).add(bush);
  }

}

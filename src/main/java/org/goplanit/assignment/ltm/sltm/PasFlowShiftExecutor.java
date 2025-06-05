package org.goplanit.assignment.ltm.sltm;

import org.goplanit.assignment.ltm.sltm.consumer.NmrDemandConstrainedFlowAndMostRestrictingTurnConsumer;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingBushBase;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmNetworkLoading;
import org.goplanit.cost.physical.AbstractPhysicalCost;
import org.goplanit.cost.virtual.AbstractVirtualCost;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.misc.Triple;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.virtual.physical.ConnectoidSegment;
import org.goplanit.utils.pcu.PcuCapacitated;
import org.goplanit.utils.zoning.OdZone;

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
  //todo: remove when no longer needed, now identified beforehand via missing s1 links method
  private final Map<ES, Set<RootedBush<V,ES>>> addedEdgeSegmentsForBushes = new TreeMap<>();

  /** track flow shifted data from S2 flow shifts to be used for S1 flow shifts in opposite direction */
  private final Map<EdgeSegment, Map<RootedBush<V,ES>, BushEntryShiftedS2FlowData>>
          flowShiftedS2BushData = new TreeMap<>();

  /**
   * Verify if entry segment is congested.
   *
   * @param loading to use
   * @param segment to check
   * @return congested or not based on check
   */
  protected static boolean isCongested(StaticLtmLoadingBushBase<?> loading, EdgeSegment segment){
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
  protected static boolean isNearCongested(
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
   * Helper; based on the entry segment and current loading, recompute node model to identify most restricting out
   * edge segment for this entry segment
   *
   * @param entrySegment   to use
   * @param networkLoading to use
   * @return identified most restricting out edge segment and any demand constrained flow into it
   */
  protected static Pair<EdgeSegment,Double> identifyMostRestrictingOutSegmentAndDemandConstrainedFlow(
          EdgeSegment entrySegment, StaticLtmLoadingBushBase<?> networkLoading) {

    // collect most restricting turn for entry segment
    var consumer = new NmrDemandConstrainedFlowAndMostRestrictingTurnConsumer(entrySegment);
    StaticLtmNetworkLoading.performNodeModelUpdate(
        entrySegment.getDownstreamVertex(), consumer, networkLoading);

    var mostRestrictingOutSegment = consumer.getMostRestrictingOutSegment();
    if (mostRestrictingOutSegment == null) {
      LOGGER.severe(String.format("Expected most restricting out segment to be present given that " +
                      "incoming segment (%s) of link (%s) is congested, but not found, this shouldn't happen",
              entrySegment.getIdsAsString(), entrySegment.getParent().getIdsAsString()));

      consumer = new NmrDemandConstrainedFlowAndMostRestrictingTurnConsumer(entrySegment);
      StaticLtmNetworkLoading.performNodeModelUpdate(
          entrySegment.getDownstreamVertex(), consumer, networkLoading);
    }
    return Pair.of(mostRestrictingOutSegment, consumer.getMostRestrictingOutSegmentDemandConstrainedFlow());
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
   * @return pair indicating what congestion was found - first argument indicates if segment itself is congested,
   * second indicates if any of its exit segments are near congestion based on threshold
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
   * @param theMode              to use
   * @param networkLoading       to use
   * @param physicalCost         to use
   * @param virtualCost          to use
   * @param isLowCostAlternative to use
   * @param derivativeReductionFactor to use
   * @return derivative, compounded derivative reduction factor and indicator whether to continue or not
   */
  protected abstract Triple<Double, Double, Boolean> getDTravelTimeDFlowExcludingMergeDiverge(
      final Mode theMode,
      final StaticLtmLoadingBushBase<?> networkLoading,
      final AbstractPhysicalCost physicalCost,
      final AbstractVirtualCost virtualCost,
      boolean isLowCostAlternative,
      double derivativeReductionFactor);

  /**
   * Determine the adjusted flow shift by taking the proposed upper bound and reduce it by a
   * designated amount based on the difference between the PAS alternative costs and the provided
   * upperbound reference. When below the minimum allowed number proceed regardless without adjustment.
   *
   * todo: currently switched off and replaced by always aplying the discontinuity dampening factor instead. In future
   *  consolidate to new final approach
   *
   * @param proposedFlowShift to use
   * @param upperBoundShift that is ideally the maximum
   * @param slackFlowLeeway to use
   * @return adjusted proposed flow shift (if any)
   */
  private double adjustFlowShiftBasedOnSlackFlow(
          double proposedFlowShift, double upperBoundShift, double slackFlowLeeway) {

    if (proposedFlowShift <= (upperBoundShift + slackFlowLeeway)) {
      return proposedFlowShift;
    }
    return upperBoundShift + slackFlowLeeway;
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
   * @param proposedFlowShift to adjust if needed
   * @param s1SlackFlow                  that is expected
   * @param slackFlowLeeway to use
   * @return adjusted proposed flow shift (if any)
   */
  protected double adjustFlowShiftBasedOnS1SlackFlow(
          double proposedFlowShift, double s1SlackFlow, double slackFlowLeeway) {
    return adjustFlowShiftBasedOnSlackFlow(proposedFlowShift, s1SlackFlow, slackFlowLeeway);
  }

  /**
   * Determine the adjusted flow shift by taking the proposed flow shift (s2 sending flow) and it by a
   * designated amount based on the difference between the PAS alternative costs and the assumed
   * s2 slack flow (flow estimated to switch from congested to uncongested on the PAS's S2
   * (high cost) segment)
   *
   * @param proposedFlowShift to adjust if needed
   * @param s2SlackFlow that is expected
   * @param slackFlowLeeway to use
   * @return adjusted proposed flow shift (if any)
   */
  protected double adjustFlowShiftBasedOnS2SlackFlow(
          double proposedFlowShift, double s2SlackFlow, double slackFlowLeeway) {
    return adjustFlowShiftBasedOnSlackFlow(proposedFlowShift, s2SlackFlow, slackFlowLeeway);
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
   * @return slack flow found
   */
  protected abstract Pair<Double,EdgeSegment>  determinePasAlternativeSlackFlow(
          StaticLtmLoadingBushBase<?> networkLoading, boolean lowCost);

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
  protected abstract ES findFirstCongestedEdgeSegmentOnPasAlternative(
          final StaticLtmLoadingBushBase<?> networkLoading, boolean lowCost);

  /**
   * Perform the flow shift for a given bush. Delegate to concrete class implementation
   *
   * @param bush                      to perform shift for
   * @param entrySegment              entry segment from original network to apply flow shift for
   * @param bushEntrySegmentFlowShift the absolute shift to apply for the given PAS-bush-entrysegment combination
   * @param theMode to use
   * @param assignmentStrategy     to use
   * @param originalNetworkCosts to use
   * @param conjSegmentCosts to use
   * @param bushes to use
   * @return end merge splitting rates of s2 to be used in s1 flow shift
   */
  @Deprecated
  protected abstract double[] executeBushS2FlowShiftNoNodeModelUpdate(
          final RootedBush<V,ES> bush,
          final EdgeSegment entrySegment,
          double bushEntrySegmentFlowShift,
          Mode theMode,
          StaticLtmAssignmentStrategy assignmentStrategy,
          double[] originalNetworkCosts,
          double[] conjSegmentCosts,
          Set<? extends RootedBush<?,?>> bushes);

  /**
   * Perform the flow shift for a given bush. Delegate to concrete class implementation
   *
   * @param bush                      to perform shift for
   * @param entrySegment              original network entry segment at hand to apply flow shift for
   * @param bushEntrySegmentFlowShift the absolute shift to apply for the given PAS-bush-entry segment combination
   * @param theMode to use
   * @param assignmentStrategy     to use
   * @param endMergeSplittingRates    end merge splitting rates of s2 to be used in s1 flow shift
   * @param originalNetworkCosts to use
   * @param conjSegmentCosts to use
   * @param bushes to use
   */
  @Deprecated
  protected abstract void executeBushS1FlowShiftNoNodeModelUpdate(
          final RootedBush<V,ES> bush,
          final EdgeSegment entrySegment,
          double bushEntrySegmentFlowShift,
          Mode theMode,
          StaticLtmAssignmentStrategy assignmentStrategy,
          double[] endMergeSplittingRates,
          double[] originalNetworkCosts,
          double[] conjSegmentCosts,
          Set<? extends RootedBush<?,?>> bushes);

  /**
   * Determining the currently available desired flows along both the high and low-cost alternatives.
   * this should be done before any flow shifts have been conducted because otherwise the Network Loading
   * acceptance factors and current bush flows are no longer consistent.
   *
   * @param flowAcceptanceFactors to use
   */
  public abstract void stepOneDetermineNetworkLoadingConsistentS1S2SendingFlows(
          double[] flowAcceptanceFactors);

  /**
   *  Determine proposed flow shift for the PAS of this flow shifter based on the (expected to be known) s1 and s2
   *  sending flows and costs.
   *
   * @param theMode to use
   * @param physicalCost to use
   * @param virtualCost to use
   * @param networkLoading to use
   * @param guaranteedS2SendingFlow  to use
   * @param logAll to use
   * @return proposed flow shift (in isolation) per network loading original network entry segment of the PAS, hence
   * using the edge segment in the return type and not the generics of this executor
   */
  public abstract Map<EdgeSegment, Double> determineProposedFlowShiftByLoadingEntrySegment(
          Mode theMode,
          AbstractPhysicalCost physicalCost,
          AbstractVirtualCost virtualCost,
          StaticLtmLoadingBushBase<?> networkLoading,
          double guaranteedS2SendingFlow,
          boolean logAll);

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
        if(!bush.contains(linkSegment)){
          missingLinkSegmentsByBush.putIfAbsent(linkSegment, new TreeSet<>());
          missingLinkSegmentsByBush.get(linkSegment).add(bush);
        }
      }
    }
    return missingLinkSegmentsByBush;
  }

  public abstract double performEquilibratedCongestedFlowShifts(
      Mode theMode,
      StaticLtmAssignmentStrategy assignmentStrategy,
      double[] originalNetworkCosts,
      double[] conjSegmentCosts,
      double[] originalNlConsistentFlowAcceptanceFactors,
      Set<? extends RootedBush<?,?>> bushes,
      boolean logAll);

  /**
   * Perform S1 flow shift assuming the S2 flow shift has already been done (is prerequisite)
   *
   * @param theMode to use
   * @param assignmentStrategy to apply
   */
  public abstract void performAllBushesRecordedOneShotS1FlowShift(
      Mode theMode,
      StaticLtmAssignmentStrategy assignmentStrategy);

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

  /** track the actually performed flow shifts on S2, to know what to apply to S1 when shifting flow
   *
   * @return map with data
   */
  public Map<EdgeSegment, Map<RootedBush<V, ES>, BushEntryShiftedS2FlowData>> getFlowShiftedS2BushData() {
    return flowShiftedS2BushData;
  }

  /**
   * put entry in flow shifted S2 data
   *
   * @param entrySegment to use
   * @param bush to use
   * @param data to place
   */
  public void putFlowShiftedS2Data(EdgeSegment entrySegment, RootedBush<V,ES> bush, BushEntryShiftedS2FlowData data){
    getFlowShiftedS2BushData().putIfAbsent(entrySegment, new TreeMap<>());
    getFlowShiftedS2BushData().get(entrySegment).put(bush, data);
  }

  /**
   * Verify if any edge segments have been added by a bush as a result of the PAS flow shift
   *
   * @return true if confirmed, false otherwise
   */
  @Deprecated
  public boolean hasAnyBushAddedLinkSegments() {
    return addedEdgeSegmentsForBushes != null && !addedEdgeSegmentsForBushes.isEmpty();
  }

  /**
   * access bushes that have added link segments due to a flow shift
   *
   * @return tracked findings or empty map
   */
  @Deprecated
  public Map<ES, Set<RootedBush<V,ES>>> getBushAddedLinkSegments() {
    return addedEdgeSegmentsForBushes;
  }

  /**
   * access bushes that have added the given link segment due to a flow shift
   *
   * @param linkSegment to check for
   * @return tracked findings or empty list
   */
  @Deprecated
  public Set<RootedBush<V,ES>> getBushAddedLinkSegments(ES linkSegment) {
    var bushes =
        addedEdgeSegmentsForBushes.computeIfAbsent(linkSegment, k -> new TreeSet<>());
    return bushes;
  }

  /**
   * Mark segment as added to bush due to flow shift
   *
   * @param bush to use
   * @param linkSegment to register
   */
  @Deprecated
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

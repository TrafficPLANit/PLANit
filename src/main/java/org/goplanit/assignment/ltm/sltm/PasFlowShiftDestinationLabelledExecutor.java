package org.goplanit.assignment.ltm.sltm;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.utils.arrays.ArrayUtils;
import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.math.Precision;

/**
 * Functionality to conduct a PAS flow shift by means of the simplest labelling technique possible, where all flow attributed to a destination is labelled uniquely throughout the
 * bush. This is least efficient in terms of memory but conceptually the easiest the understand, implement, and test.
 * 
 * @author markr
 *
 */
public class PasFlowShiftDestinationLabelledExecutor extends PasFlowShiftExecutor {

  /**
   * Logger to use
   */
  private final static Logger LOGGER = Logger.getLogger(PasFlowShiftDestinationLabelledExecutor.class.getCanonicalName());

  /** the labelled (sending) flows per origin (map key) that traverse S2 */
  protected final Map<Bush, Map<BushFlowLabel, Double>> s2DestinationLabelledFlows;

  /** the labelled (sending) flows, per origin (map key) that traverse S1 */
  protected final Map<Bush, Map<BushFlowLabel, Double>> s1DestinationLabelledFlows;

  /**
   * Helper to perform a flow shift on a turn. If the turn has no more flow for the given label it is removed from the bush
   * 
   * @param origin        bush to use
   * @param turnEntry     turn entry segment
   * @param turnExit      turn exit segment
   * @param label         label to use on both entry and exit
   * @param flowShiftPcuH turn flow shift to apply by adding this flow to the turn
   * @return new labelled turn flow after shift
   */
  private double executeTurnFlowShift(Bush origin, EdgeSegment turnEntry, EdgeSegment turnExit, BushFlowLabel label, double flowShiftPcuH) {
    double newLabelledTurnFlow = origin.addTurnSendingFlow(turnEntry, label, turnExit, label, flowShiftPcuH, isPasS2RemovalAllowed());
    if (isPasS2RemovalAllowed() && !Precision.positive(newLabelledTurnFlow, EPSILON) && !Precision.positive(origin.getTurnSendingFlow(turnEntry, turnExit), EPSILON)) {
      /* no remaining flow at all on turn after flow shift, remove turn from bush entirely */
      origin.removeTurn(turnEntry, turnExit);
      newLabelledTurnFlow = 0.0;
    }
    return newLabelledTurnFlow;
  }

  /**
   * Determine all labels' flows that are fully overlapping with the provided subpath (assumed present on the bush). We Utilise splitting rates to track because when destination
   * labelled it is possible that flows with the same label join the PAS halfway through but we should only track the portion of the labelled flow that participated from the start
   * <p>
   * The flows that are provided are the sending flows on the final edge segment of alternative S2, i.e., scaled back by all encountered flow acceptance factors
   * 
   * @param origin  at hand
   * @param subPath to do this for
   * @return found matching labels and their accepted absolute flows through s2, i.e., we track the flows by splitting rates and reduce by encountered flow acceptance factors
   */
  private Map<BushFlowLabel, Double> determineUsedLabelAcceptedFlows(Bush origin, final EdgeSegment[] subPath, double[] flowAcceptanceFactors) {
    var edgeSegmentCompositionLabels = origin.getFlowCompositionLabels(subPath[0]);

    var pasCompositionLabelledSendingFlows = new HashMap<BushFlowLabel, Double>();
    if (edgeSegmentCompositionLabels == null || edgeSegmentCompositionLabels.isEmpty()) {
      return pasCompositionLabelledSendingFlows;
    }

    var labelIter = edgeSegmentCompositionLabels.iterator();
    NEXTLABEL: while (labelIter.hasNext()) {
      var currLabel = labelIter.next();

      EdgeSegment currentSegment = subPath[0];
      double labelledS2AcceptedFlow = origin.getSendingFlowPcuH(currentSegment, currLabel);

      EdgeSegment succeedingSegment = currentSegment;
      for (int index = 1; index < subPath.length; ++index) {
        succeedingSegment = subPath[index];
        double currSplittingRate = origin.getSplittingRate(currentSegment, succeedingSegment, currLabel);
        if (!Precision.positive(currSplittingRate)) {
          continue NEXTLABEL;
        }
        currentSegment = succeedingSegment;
        labelledS2AcceptedFlow *= flowAcceptanceFactors[(int) currentSegment.getId()] * currSplittingRate;
      }
      labelledS2AcceptedFlow *= flowAcceptanceFactors[(int) succeedingSegment.getId()];

      pasCompositionLabelledSendingFlows.put(currLabel, labelledS2AcceptedFlow);
    }
    return pasCompositionLabelledSendingFlows;
  }

  /**
   * Perform the flow shift through the end merge vertex of the PASs high cost segment for the given origin bush flow composition
   * 
   * @param origin                           to use
   * @param destinationLabel                 flow composition label at hand
   * @param s2FinalLabeledFlowShift          the flow shift applied so far up to the final merge
   * @param s2MergeExitSplittingRates        splitting rates to use with multi-key (exit segment, label)
   * @param exitShiftedSendingFlowToPopulate map to populate with the found exit segment flows (values) by used exit label (key)
   */
  private void executeBushDestinationLabeledS2FlowShiftEndMerge(Bush origin, BushFlowLabel destinationLabel, double s2FinalLabeledFlowShift,
      MultiKeyMap<Object, Double> s2MergeExitSplittingRates, Map<BushFlowLabel, double[]> exitShiftedSendingFlowToPopulate) {

    /* remove shifted flows through final merge towards exit segments proportionally, to later add to s1 turns through merge */
    if (pas.getMergeVertex().hasExitEdgeSegments()) {

      var lastS2Segment = pas.getLastEdgeSegment(false /* high cost */);
      var destinationLabelExitSegmentShiftedSendingFlows = new double[pasMergeVertexNumExitSegments];
      exitShiftedSendingFlowToPopulate.put(destinationLabel, destinationLabelExitSegmentShiftedSendingFlows);

      /* key: [exitSegment, exitLabel] */
      int index = 0;
      for (var exitSegment : pas.getMergeVertex().getExitEdgeSegments()) {
        if (origin.containsEdgeSegment(exitSegment)) {
          Double labeledSplittingRate = s2MergeExitSplittingRates.get(exitSegment, destinationLabel);
          if (labeledSplittingRate == null || !Precision.positive(labeledSplittingRate, EPSILON)) {
            ++index;
            continue;
          }

          /* remove flow for s2 */
          double s2FlowShift = s2FinalLabeledFlowShift * labeledSplittingRate;
          double newLabelledTurnFlow = origin.addTurnSendingFlow(lastS2Segment, destinationLabel, exitSegment, destinationLabel, s2FlowShift, isPasS2RemovalAllowed());
          if (!Precision.positive(newLabelledTurnFlow, EPSILON) && !Precision.positive(origin.getTurnSendingFlow(lastS2Segment, exitSegment), EPSILON)) {
            /* no remaining flow at all on turn after flow shift, remove turn from bush entirely */
            origin.removeTurn(lastS2Segment, exitSegment);
          }

          /* track so we can attribute it to s1 segment later */
          destinationLabelExitSegmentShiftedSendingFlows[index] += -s2FlowShift;
        }
        ++index;
      }
    }
  }

  /**
   * Perform the flow shift through the end merge vertex of the PASs low cost segment for the given origin bush flow composition
   * 
   * @param origin                                    to use
   * @param destinationLabel                          destination label on s2 final segment to apply
   * @param s1FinalLabeledFlowShift                   the flow shift applied so far up to the final merge
   * @param destinationLabelExitSegmentSplittingRates the splitting rates to apply towards the available exit segments for the given exit label
   */
  private void executeBushDestinationLabeledS1FlowShiftEndMerge(Bush origin, BushFlowLabel destinationLabel, double s1FinalLabeledFlowShift,
      double[] destinationLabelExitSegmentSplittingRates) {

    /* add shifted flows through final merge towards exit segments proportionally based on labeled exit usage */
    if (pas.getMergeVertex().hasExitEdgeSegments()) {
      EdgeSegment lastS1Segment = pas.getLastEdgeSegment(true /* low cost */);

      int index = 0;
      for (var exitSegment : pas.getMergeVertex().getExitEdgeSegments()) {
        double splittingRate = destinationLabelExitSegmentSplittingRates[index];
        if (Precision.positive(splittingRate)) {
          /* add flow for s1 */
          double s1FlowShift = s1FinalLabeledFlowShift * splittingRate;
          double newLabelledTurnFlow = origin.addTurnSendingFlow(lastS1Segment, destinationLabel, exitSegment, destinationLabel, s1FlowShift);
          if (!Precision.positive(newLabelledTurnFlow, EPSILON)) {
            LOGGER.severe("Flow shift towards cheaper S1 alternative should always result in non-negative remaining flow, but this was not found, this shouldn't happen");
          }
        }
        ++index;
      }
    }
  }

  /**
   * Execute a flow shift on a given bush for the given entry+PAS alternative. This does not move flow through the final merge vertex but does flow through the initial diverge.
   * 
   * @param origin                bush at hand
   * @param entrySegment          entry segment for the initial turn leading into the pasSegment
   * @param label                 the composition label to apply the shift for
   * @param flowShiftPcuH         to execute (assumed to be correctly proportioned in relation to other bushes and labels within bush for this PAS)
   * @param pasSegment            to update on bush
   * @param flowAcceptanceFactors to use when updating the flows
   * @return sending flow on last edge segment of the PAS alternative after the flow shift (considering encountered reductions)
   */
  private double executeBushDestinationLabeledFlowShift(final Bush origin, final EdgeSegment entrySegment, final BushFlowLabel label, double flowShiftPcuH,
      final EdgeSegment[] pasSegment, final double[] flowAcceptanceFactors) {

    /* initial turn into pas segment */
    int index = 0;
    EdgeSegment currentSegment = entrySegment;
    var nextSegment = pasSegment[index];

    executeTurnFlowShift(origin, entrySegment, nextSegment, label, flowShiftPcuH);
    flowShiftPcuH *= flowAcceptanceFactors[(int) entrySegment.getId()];

    /* pas alternative itself */
    while (++index < pasSegment.length) {
      currentSegment = nextSegment;
      nextSegment = pasSegment[index];

      executeTurnFlowShift(origin, currentSegment, nextSegment, label, flowShiftPcuH);
      flowShiftPcuH *= flowAcceptanceFactors[(int) currentSegment.getId()];
    }

    return flowShiftPcuH;
  }

  /**
   * {@inheritDoc}
   */
  protected void executeOriginFlowShift(Bush origin, EdgeSegment entrySegment, double bushFlowShift, double[] flowAcceptanceFactors) {
    /* prep - pas */
    final var s2 = pas.getAlternative(false);
    final var s1 = pas.getAlternative(true);

    /* prep - origin */
    var s2DestinationLabelledAcceptedFlows = determineUsedLabelAcceptedFlows(origin, s2, flowAcceptanceFactors);

    /*
     * ------------------------------------------------- S2 FLOW SHIFT ----------------------------------------------------------------------------------------------------------
     * Update S2 by shifting flow proportionally along encountered flow compositions matching with the PAS/origin/alternative
     */

    /*
     * determine relative proportions of destination labels (based on s2). Given a PAS should be proportional on both alternatives we can use the found proportions on both
     * alternatives and can use the found proportions also for s1 (since we use destination labelling the same labels apply).
     */
    TreeMap<BushFlowLabel, Double> bushAlternativeRelativeLabelProportions = new TreeMap<>(s2DestinationLabelledAcceptedFlows);
    double sumOfS2LabelledAcceptedFlows = bushAlternativeRelativeLabelProportions.values().stream().collect(Collectors.summingDouble(d -> d));
    bushAlternativeRelativeLabelProportions.entrySet().forEach(e -> e.setValue(e.getValue() / sumOfS2LabelledAcceptedFlows));

    /* exit turn flows out of s2 by used exit label - to be populated in s2 merge update for use by s1 update */
    var bushS2MergeExitShiftedSendingFlows = new TreeMap<BushFlowLabel, double[]>();
    for (var labelPortionPair : bushAlternativeRelativeLabelProportions.entrySet()) {
      var currLabel = labelPortionPair.getKey();
      var currLabelPortion = labelPortionPair.getValue();

      // TODO: combine final merge and flow shift in one approach ideally
      /* obtain splitting rates before flow shift in case turns/edges are removed on S2, then splitting rate information is lost while required for final merge afterwards */
      var s2MergeExitSplittingRates = origin.getSplittingRates(pas.getLastEdgeSegment(false /* high cost */), currLabel);

      /* shift portion of flow attributed to composition label traversing s2 */
      double s2StartLabeledFlowShift = -currLabelPortion * bushFlowShift;
      double s2FinalLabeledFlowShift = executeBushDestinationLabeledFlowShift(origin, entrySegment, currLabel, s2StartLabeledFlowShift, s2, flowAcceptanceFactors);

      LOGGER.info(String.format("** S2 SHIFT: dest-label %d, shift-link [start,end]: [%.10f, %.10f]", currLabel.getLabelId(), s2StartLabeledFlowShift, s2FinalLabeledFlowShift));

      /* shift flow across final merge for S2 */
      executeBushDestinationLabeledS2FlowShiftEndMerge(origin, currLabel, s2FinalLabeledFlowShift, s2MergeExitSplittingRates, bushS2MergeExitShiftedSendingFlows);
    }

    /* convert flows to portions by label */
    bushS2MergeExitShiftedSendingFlows.forEach((label, flows) -> ArrayUtils.divideBySum(flows, 0));
    var bushS2MergeExitShiftedSplittingRates = bushS2MergeExitShiftedSendingFlows;
    bushS2MergeExitShiftedSendingFlows = null;

    /*
     * ------------------------------------------------- S1 FLOW SHIFT ----------------------------------------------------------------------------------------------------------
     * Update S1 by shifting flow proportionally along encountered flow compositions matching with the PAS/origin/alternative
     */
    for (var labelPortionPair : bushAlternativeRelativeLabelProportions.entrySet()) {
      var currLabel = labelPortionPair.getKey();
      var currLabelPortion = labelPortionPair.getValue();

      /* portion of flow attributed to composition label traversing s1 */
      double s1StartLabeledFlowShift = currLabelPortion * bushFlowShift;
      double s1FinalLabeledFlowShift = executeBushDestinationLabeledFlowShift(origin, entrySegment, currLabel, s1StartLabeledFlowShift, s1, flowAcceptanceFactors);

      LOGGER.info(String.format("** S1 SHIFT: dest-label %d, shift-link [start,end]: [%.10f, %.10f]", currLabel.getLabelId(), s1StartLabeledFlowShift, s1FinalLabeledFlowShift));

      /* shift flow across final merge for S1 based on findings in s2 */
      executeBushDestinationLabeledS1FlowShiftEndMerge(origin, currLabel, s1FinalLabeledFlowShift, bushS2MergeExitShiftedSplittingRates.get(currLabel));
    }
  }

  /**
   * Constructor
   * 
   * @param pas      to use
   * @param settings to use
   */
  protected PasFlowShiftDestinationLabelledExecutor(final Pas pas, final StaticLtmSettings settings) {
    super(pas, settings);
    this.s1DestinationLabelledFlows = new HashMap<>();
    this.s2DestinationLabelledFlows = new HashMap<>();
  }

}

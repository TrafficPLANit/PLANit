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
 * Functionality to conduct a PAS flow shift by means of the simples labelling technique possible, where all flow attributed to a destination is labelled uniquely throughout the
 * bush. This is least effecient in terms of memory but conceptually the easiest the understand, implement, and test.
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

  // OLD to be removed if entry segment based version works!
  //  /**
  //   * {@inheritDoc}
  //   */
  //  protected void executeOriginFlowShift(Bush origin, double bushFlowShift, double[] flowAcceptanceFactors) {
  //    /* prep - pas */
  //    final var s2 = pas.getAlternative(false);
  //    final var s1 = pas.getAlternative(true);
  //
  //    /* prep - origin */
  //    var s2DestinationLabelledAcceptedFlows = determineUsedLabelAcceptedFlows(origin, s2, flowAcceptanceFactors);
  //
  //    /*
  //     * ------------------------------------------------- S2 FLOW SHIFT ----------------------------------------------------------------------------------------------------------
  //     * Update S2 by shifting flow proportionally along encountered flow compositions matching with the PAS/origin/alternative
  //     */
  //
  //    /*
  //     * determine relative proportions of destination labels (based on s2). Given a PAS should be proportional on both alternatives we can use the found proportions on both
  //     * alternatives and can use the found proportions also for s1 (since we use destination labelling the same labels apply).
  //     */
  //    TreeMap<BushFlowLabel, Double> bushAlternativeRelativeLabelProportions = new TreeMap<>(s2DestinationLabelledAcceptedFlows);
  //    double sumOfS2LabelledAcceptedFlows = bushAlternativeRelativeLabelProportions.values().stream().collect(Collectors.summingDouble(d -> d));
  //    bushAlternativeRelativeLabelProportions.entrySet().forEach(e -> e.setValue(e.getValue() / sumOfS2LabelledAcceptedFlows));
  //
  //    /*
  //     * Determine the portion to attribute to each used [turn,entry label] combination when shifting flow across the diverge. The portions are made proportional to contribution of
  //     * each combination to the total sending flow on the s2-label flow on its initial link segment. Multikey: [entry segment, entry label]
  //     */
  //    MultiKeyMap<Object, Double> s2DivergeProportionsByTurnLabels = null;
  //    if (pas.getDivergeVertex().hasEntryEdgeSegments()) {
  //      s2DivergeProportionsByTurnLabels = createS2DivergeProportionsByTurnLabels(origin, s2DestinationLabelledAcceptedFlows, flowAcceptanceFactors);
  //    }
  //
  //    /* exit turn flows out of s2 by used exit label - to be populated in s2 merge update for use by s1 update */
  //    var bushS2MergeExitShiftedSendingFlows = new TreeMap<BushFlowLabel, double[]>();
  //    for (var labelPortionPair : bushAlternativeRelativeLabelProportions.entrySet()) {
  //      var currLabel = labelPortionPair.getKey();
  //      var currLabelPortion = labelPortionPair.getValue();
  //
  //      /* shift portion of flow attributed to composition label traversing s2 */
  //      double s2StartLabeledFlowShift = -currLabelPortion * bushFlowShift;
  //      double s2FinalLabeledFlowShift = executeBushDestinationLabeledBaseFlowShift(origin, currLabel, s2StartLabeledFlowShift, s2, flowAcceptanceFactors);
  //
  //      LOGGER.severe(
  //          String.format("** S2 SHIFT: dest-label %d, shift-start-link %.10f, shift-final-link %.10f", currLabel.getLabelId(), s2StartLabeledFlowShift, s2FinalLabeledFlowShift));
  //
  //      /* shift flow across final merge for S2 */
  //      executeBushDestinationLabeledS2FlowShiftEndMerge(origin, currLabel, s2FinalLabeledFlowShift, bushS2MergeExitShiftedSendingFlows);
  //
  //      /* shift flows across starting diverge before entering S2 using reciprocal of flow acceptance factor */
  //      if (!s2DivergeProportionsByTurnLabels.isEmpty()) {
  //        executeBushDestinationLabeledS2FlowShiftStartDiverge(origin, currLabel, s2StartLabeledFlowShift, s2DivergeProportionsByTurnLabels, flowAcceptanceFactors);
  //      }
  //    }
  //
  //    /* convert flows to portions by label */
  //    bushS2MergeExitShiftedSendingFlows.forEach((label, flows) -> ArrayUtils.divideBySum(flows, 0));
  //    var bushS2MergeExitShiftedSplittingRates = bushS2MergeExitShiftedSendingFlows;
  //    bushS2MergeExitShiftedSendingFlows = null;
  //
  //    /*
  //     * ------------------------------------------------- S1 FLOW SHIFT ----------------------------------------------------------------------------------------------------------
  //     * Update S1 by shifting flow proportionally along encountered flow compositions matching with the PAS/origin/alternative
  //     */
  //    for (var labelPortionPair : bushAlternativeRelativeLabelProportions.entrySet()) {
  //      var currLabel = labelPortionPair.getKey();
  //      var currLabelPortion = labelPortionPair.getValue();
  //
  //      /* portion of flow attributed to composition label traversing s1 */
  //      double s1StartLabeledFlowShift = currLabelPortion * bushFlowShift;
  //      double s1FinalLabeledFlowShift = executeBushDestinationLabeledBaseFlowShift(origin, currLabel, s1StartLabeledFlowShift, s1, flowAcceptanceFactors);
  //
  //      LOGGER.severe(
  //          String.format("** S1 SHIFT: dest-label %d, shift-start-link %.10f, shift-final-link %.10f", currLabel.getLabelId(), s1StartLabeledFlowShift, s1FinalLabeledFlowShift));
  //
  //      /* shift flow across final merge for S1 based on findings in s2 */
  //      executeBushDestinationLabeledS1FlowShiftEndMerge(origin, currLabel, s1FinalLabeledFlowShift, bushS2MergeExitShiftedSplittingRates.get(currLabel));
  //
  //      if (!s2DivergeProportionsByTurnLabels.isEmpty()) {
  //        /* shift flow across initial diverge into S1 based on findings in s2 */
  //        executeBushLabeledS1FlowShiftStartDiverge(origin, currLabel, s1StartLabeledFlowShift, s2DivergeProportionsByTurnLabels, flowAcceptanceFactors);
  //      }
  //    }
  //  }
  
    /**
     * Helper to perform a flow shift on a turn. If the turn has no more flow for the given label it is removed from the bush
     * 
     * @param origin        bush to use
     * @param turnEntry     turn entry segment
     * @param turnExit      turn exit segment
     * @param label         label to use on both entry and exit
     * @param flowShiftPcuH turn flow shift to apply by adding this flow to the turn
     */
    private void executeTurnFlowShift(Bush origin, EdgeSegment turnEntry, EdgeSegment turnExit, BushFlowLabel label, double flowShiftPcuH) {
      double turnSendingFlow = origin.getTurnSendingFlow(turnEntry, label, turnExit, label);
      if (!Precision.positive(turnSendingFlow + flowShiftPcuH)) {
        /* no remaining flow at all after flow shift, remove turn from bush entirely */
        origin.removeTurn(turnEntry, turnExit);
      } else {
        origin.addTurnSendingFlow(turnEntry, label, turnExit, label, flowShiftPcuH);
      }
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

  // should no longer be needed in new setup
//  /**
//   * Determine the portion of total shifted flow allocated to each used turn+label combination based on proportional distribution by dividing the accepted flow along S2 for a given
//   * label by the total flow of that label upon entering s2. In other words we scale back the labelled sending flow to the point that all of this flow is s2 compatible flow. It is
//   * this compatible amount of flow that we use to determine the portion of total flow shift that will be attributed to this label
//   * <p>
//   * The key for the MultiKeyMap is [entry segment, entry-label] while the value will hold the portion of the total flow shift we attribute to the this combinatino
//   *
//   * @param origin                     at hand
//   * @param pasS2LabelledAcceptedFlows the absolute accepted flows of the label through the PAS s2 alternative, i.e., the destination labelled sending flows at the start reduced by
//   *                                   splitting rates and flow acceptance factors until the end of the s2 alternative. Used to extract the portion of the to be executed flow shift
//   *                                   that can be attributed to this label proportional to the total flow shift
//   * @param flowAcceptanceFactors      to use
//   * @return s2DivergeTurnLabelProportionsToPopulate to populate, only entries for used labels will be present
//   */
//  private MultiKeyMap<Object, Double> createS2DivergeProportionsByTurnLabels(Bush origin, Map<BushFlowLabel, Double> pasS2LabelledAcceptedFlows,
//      final double[] flowAcceptanceFactors) {
//
//    var firstS2EdgeSegment = pas.getFirstEdgeSegment(false /* high cost segment */);
//    var s2DivergeTurnLabelProportionsToPopulate = new MultiKeyMap<Object, Double>();
//
//    /* 1: determine turn specific labelled sending flows into s2 initial segment for each label that is used */
//    double s2InitialSegmentTotalShiftableFlow = 0;
//    for (var entry : pasS2LabelledAcceptedFlows.entrySet()) {
//      var usedEntryLabel = entry.getKey();
//      double s2FinalSegmentLabelledAcceptedFlow = entry.getValue();
//
//      double s2InitialSegmentLabelledSendingFlow = 0;
//      for (var entrySegment : pas.getDivergeVertex().getEntryEdgeSegments()) {
//        if (origin.containsEdgeSegment(entrySegment)) {
//          double alpha = flowAcceptanceFactors[(int) entrySegment.getId()];
//
//          double turnSendingFlow = origin.getTurnSendingFlow(entrySegment, usedEntryLabel, firstS2EdgeSegment, usedEntryLabel);
//          if (!Precision.positive(turnSendingFlow)) {
//            continue;
//          }
//
//          /* determine entry segment labelled accepted flow towards s2 entry segment (without scaling it yet) */
//          double s2InitialSegmentTurnLabelledAcceptedFlow = turnSendingFlow * alpha;
//          Double currentFlow = s2DivergeTurnLabelProportionsToPopulate.get(entrySegment, usedEntryLabel);
//          if (currentFlow == null) {
//            currentFlow = 0.0;
//          }
//          s2DivergeTurnLabelProportionsToPopulate.put(entrySegment, usedEntryLabel, currentFlow + s2InitialSegmentTurnLabelledAcceptedFlow);
//          s2InitialSegmentLabelledSendingFlow += s2InitialSegmentTurnLabelledAcceptedFlow;
//        }
//      }
//
//      /* 2. Scale back this sending flow to the portion of the this flow that makes it to the end of s2. This gives us the correct absolute labelled flow to work with */
//      double portionAttributedToS2 = s2FinalSegmentLabelledAcceptedFlow / s2InitialSegmentLabelledSendingFlow;
//      for (var entrySegment : pas.getDivergeVertex().getEntryEdgeSegments()) {
//        if (origin.containsEdgeSegment(entrySegment)) {
//          Double turnLabelledSendingFlow = s2DivergeTurnLabelProportionsToPopulate.get(entrySegment, usedEntryLabel);
//          if (turnLabelledSendingFlow != null) {
//            double s2CompatibleTurnAcceptedFlow = turnLabelledSendingFlow * portionAttributedToS2;
//            s2DivergeTurnLabelProportionsToPopulate.put(entrySegment, usedEntryLabel, s2CompatibleTurnAcceptedFlow);
//            s2InitialSegmentTotalShiftableFlow += s2CompatibleTurnAcceptedFlow;
//          }
//        }
//      }
//    }
//
//    /* 3. convert from entry turn labelled flows entry turn labelled proportions of each non-zero contributing turn-entrylabel- identified towards PAS s2 initial segment */
//    var iter = s2DivergeTurnLabelProportionsToPopulate.mapIterator();
//    while (iter.hasNext()) {
//      iter.next();
//      iter.setValue(iter.getValue() / s2InitialSegmentTotalShiftableFlow);
//    }
//
//    return s2DivergeTurnLabelProportionsToPopulate;
//  }

  /**
   * Perform the flow shift through the end merge vertex of the PASs high cost segment for the given origin bush flow composition
   * 
   * @param origin                           to use
   * @param destinationLabel                 flow composition label at hand
   * @param s2FinalLabeledFlowShift          the flow shift applied so far up to the final merge
   * @param exitShiftedSendingFlowToPopulate map to populate with the found exit segment flows (values) by used exit label (key)
   */
  private void executeBushDestinationLabeledS2FlowShiftEndMerge(Bush origin, BushFlowLabel destinationLabel, double s2FinalLabeledFlowShift,
      Map<BushFlowLabel, double[]> exitShiftedSendingFlowToPopulate) {

    /* remove shifted flows through final merge towards exit segments proportionally, to later add to s1 turns through merge */
    if (pas.getMergeVertex().hasExitEdgeSegments()) {

      var lastS2Segment = pas.getLastEdgeSegment(false /* high cost */);
      var destinationLabelExitSegmentShiftedSendingFlows = new double[pasMergeVertexNumExitSegments];
      exitShiftedSendingFlowToPopulate.put(destinationLabel, destinationLabelExitSegmentShiftedSendingFlows);

      /* key: [exitSegment, exitLabel] */
      MultiKeyMap<Object, Double> splittingRates = origin.getSplittingRates(lastS2Segment, destinationLabel);
      int index = 0;
      for (var exitSegment : pas.getMergeVertex().getExitEdgeSegments()) {
        if (origin.containsEdgeSegment(exitSegment)) {
          Double labeledSplittingRate = splittingRates.get(exitSegment, destinationLabel);
          if (labeledSplittingRate == null || !Precision.positive(labeledSplittingRate)) {
            continue;
          }

          /* remove flow for s2 */
          double s2FlowShift = s2FinalLabeledFlowShift * labeledSplittingRate;
          if (!Precision.positive(origin.getTurnSendingFlow(lastS2Segment, exitSegment) + s2FlowShift)) {
            /* no remaining flow at all after flow shift, remove turn from bush entirely */
            origin.removeTurn(lastS2Segment, exitSegment);
          } else {
            origin.addTurnSendingFlow(lastS2Segment, destinationLabel, exitSegment, destinationLabel, s2FlowShift);
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
          origin.addTurnSendingFlow(lastS1Segment, destinationLabel, exitSegment, destinationLabel, s1FlowShift);
        }
        ++index;
      }
    }
  }

  // should no longer be needed in new setup
//  /**
//   * Perform the flow shift through the start diverge vertex of the PASs high cost segment for the given origin bush flow composition
//   * <p>
//   * s2DivergeProportionsByTurnLabels has the following expected multikey[entrysegment,destinationLabel]
//   * 
//   * @param origin                           to use
//   * @param destinationLabel                 destination label on s2 initial segment to apply
//   * @param s2StartLabeledFlowShift          the flow shift applied to the first s2 segment
//   * @param s2DivergeProportionsByTurnLabels portion to be shifted flow attributed to each used turn entry-destinationLabel towards S2 initial segment
//   * @param flowAcceptanceFactors            to use
//   */
//  private void executeBushDestinationLabeledS2FlowShiftStartDiverge(Bush origin, BushFlowLabel destinationLabel, double s2StartLabeledFlowShift,
//      MultiKeyMap<Object, Double> s2DivergeProportionsByTurnLabels, final double[] flowAcceptanceFactors) {
//
//    EdgeSegment firstS2Segment = pas.getFirstEdgeSegment(false /* high cost */);
//
//    for (var entrySegment : pas.getDivergeVertex().getEntryEdgeSegments()) {
//      if (origin.containsEdgeSegment(entrySegment)) {
//        Double portion = s2DivergeProportionsByTurnLabels.get(entrySegment, destinationLabel);
//        if (portion == null) {
//          continue;
//        }
//
//        double existingTurnLabeledSendingFlow = origin.getTurnSendingFlow(entrySegment, destinationLabel, firstS2Segment, destinationLabel);
//        if (!Precision.positive(existingTurnLabeledSendingFlow)) {
//          LOGGER.severe(String.format("Expected non-zero turn sending flow for given destination label %d, found none, this shouldn't happen", destinationLabel.getLabelId()));
//          continue;
//        }
//
//        /* convert back to sending flow as alpha<1 increases sending flow on entry segment compared to the sending flow component on the first s2 segment */
//        double s2DivergeEntryLabeledFlowShift = s2StartLabeledFlowShift * portion * (1 / flowAcceptanceFactors[(int) entrySegment.getId()]);
//
//        if (!Precision.positive(existingTurnLabeledSendingFlow + s2DivergeEntryLabeledFlowShift)) {
//          /* no remaining flow at all after flow shift, remove turn from bush entirely */
//          origin.removeTurn(entrySegment, firstS2Segment);
//        } else {
//          origin.addTurnSendingFlow(entrySegment, destinationLabel, firstS2Segment, destinationLabel, s2DivergeEntryLabeledFlowShift);
//        }
//      }
//    }
//  }

//  /**
//   * Perform the flow shift through the start diverge vertex of the PASs low cost segment for the given origin bush flow composition. We use the same proportions that were applied
//   * in the S2 diverge update via the divergeProportionsByTurnLabels. These portions are based on a proportional distribution for each used entrysegment-entrylabel-exitlabel
//   * contribution compared to the total exit label flow.
//   * <p>
//   * the multikey of the multikeymap is expected to be [entry segment,entry label] while the value reflects the to be applied portion
//   * 
//   * @param origin                         to use
//   * @param startSegmentLabel              flow composition label on s1 initial segment to apply
//   * @param s1StartLabeledFlowShift        the flow shift applied to the first s1 segment for the label
//   * @param divergeProportionsByTurnLabels portions to apply for each entrysegment-entrylabel given the to be shifted flow for a given exitlabel towards S1 initial segment
//   * @param flowAcceptanceFactors          to use
//   */
//  private void executeBushLabeledS1FlowShiftStartDiverge(Bush origin, BushFlowLabel startSegmentLabel, double s1StartLabeledFlowShift,
//      MultiKeyMap<Object, Double> divergeProportionsByTurnLabels, final double[] flowAcceptanceFactors) {
//
//    EdgeSegment firstS1Segment = pas.getFirstEdgeSegment(true /* low cost */);
//
//    for (var entrySegment : pas.getDivergeVertex().getEntryEdgeSegments()) {
//      if (origin.containsEdgeSegment(entrySegment)) {
//        var entryLabels = origin.getFlowCompositionLabels(entrySegment);
//        for (var entryLabel : entryLabels) {
//          Double portion = divergeProportionsByTurnLabels.get(entrySegment, entryLabel);
//          if (portion == null) {
//            continue;
//          }
//
//          /* convert back to sending flow as alpha<1 increases sending flow on entry segment compared to the sending flow component on the first s1 segment */
//          double s1DivergeEntryLabeledFlowShift = s1StartLabeledFlowShift * portion * (1 / flowAcceptanceFactors[(int) entrySegment.getId()]);
//          if (Precision.negative(s1DivergeEntryLabeledFlowShift)) {
//            LOGGER.severe("Expected non-negative shift on s1 turn for given label combination, skip flow shift at PAS s1 diverge");
//            continue;
//          }
//          origin.addTurnSendingFlow(entrySegment, entryLabel, firstS1Segment, startSegmentLabel, s1DivergeEntryLabeledFlowShift);
//        }
//      }
//    }
//  }

//  // to be replaced by executeBushDestinationLabeledFlowShift taking entry segment into account
//  /**
//   * Execute a flow shift on a given bush for the given PAS segment. This does not move flow through the final merge vertex nor the initial diverge vertex.
//   * 
//   * @param origin                bush at hand
//   * @param label                 the composition label to apply the shift for
//   * @param flowShiftPcuH         to execute (assumed to be correctly proportioned in relation to other bushes and labels within bush for this PAS)
//   * @param pasSegment            to update on bush
//   * @param flowAcceptanceFactors to use when updating the flows
//   * @return sending flow on last edge segment of the PAS alternative after the flow shift (considering encountered reductions)
//   */
//  private double executeBushDestinationLabeledBaseFlowShift(final Bush origin, final BushFlowLabel label, double flowShiftPcuH, final EdgeSegment[] pasSegment,
//      final double[] flowAcceptanceFactors) {
//    int index = 0;
//    EdgeSegment currentSegment = null;
//    var nextSegment = pasSegment[index];
//
//    while (++index < pasSegment.length) {
//      currentSegment = nextSegment;
//      nextSegment = pasSegment[index];
//
//      double turnSendingFlow = origin.getTurnSendingFlow(currentSegment, label, nextSegment, label);
//      if (!Precision.positive(turnSendingFlow + flowShiftPcuH)) {
//        /* no remaining flow at all after flow shift, remove turn from bush entirely */
//        origin.removeTurn(currentSegment, nextSegment);
//      } else {
//        origin.addTurnSendingFlow(currentSegment, label, nextSegment, label, flowShiftPcuH);
//      }
//      flowShiftPcuH *= flowAcceptanceFactors[(int) currentSegment.getId()];
//    }
//
//    return flowShiftPcuH;
//  }

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

  // OLD to be removed if entry segment based version works!
//  /**
//   * {@inheritDoc}
//   */
//  protected void executeOriginFlowShift(Bush origin, double bushFlowShift, double[] flowAcceptanceFactors) {
//    /* prep - pas */
//    final var s2 = pas.getAlternative(false);
//    final var s1 = pas.getAlternative(true);
//
//    /* prep - origin */
//    var s2DestinationLabelledAcceptedFlows = determineUsedLabelAcceptedFlows(origin, s2, flowAcceptanceFactors);
//
//    /*
//     * ------------------------------------------------- S2 FLOW SHIFT ----------------------------------------------------------------------------------------------------------
//     * Update S2 by shifting flow proportionally along encountered flow compositions matching with the PAS/origin/alternative
//     */
//
//    /*
//     * determine relative proportions of destination labels (based on s2). Given a PAS should be proportional on both alternatives we can use the found proportions on both
//     * alternatives and can use the found proportions also for s1 (since we use destination labelling the same labels apply).
//     */
//    TreeMap<BushFlowLabel, Double> bushAlternativeRelativeLabelProportions = new TreeMap<>(s2DestinationLabelledAcceptedFlows);
//    double sumOfS2LabelledAcceptedFlows = bushAlternativeRelativeLabelProportions.values().stream().collect(Collectors.summingDouble(d -> d));
//    bushAlternativeRelativeLabelProportions.entrySet().forEach(e -> e.setValue(e.getValue() / sumOfS2LabelledAcceptedFlows));
//
//    /*
//     * Determine the portion to attribute to each used [turn,entry label] combination when shifting flow across the diverge. The portions are made proportional to contribution of
//     * each combination to the total sending flow on the s2-label flow on its initial link segment. Multikey: [entry segment, entry label]
//     */
//    MultiKeyMap<Object, Double> s2DivergeProportionsByTurnLabels = null;
//    if (pas.getDivergeVertex().hasEntryEdgeSegments()) {
//      s2DivergeProportionsByTurnLabels = createS2DivergeProportionsByTurnLabels(origin, s2DestinationLabelledAcceptedFlows, flowAcceptanceFactors);
//    }
//
//    /* exit turn flows out of s2 by used exit label - to be populated in s2 merge update for use by s1 update */
//    var bushS2MergeExitShiftedSendingFlows = new TreeMap<BushFlowLabel, double[]>();
//    for (var labelPortionPair : bushAlternativeRelativeLabelProportions.entrySet()) {
//      var currLabel = labelPortionPair.getKey();
//      var currLabelPortion = labelPortionPair.getValue();
//
//      /* shift portion of flow attributed to composition label traversing s2 */
//      double s2StartLabeledFlowShift = -currLabelPortion * bushFlowShift;
//      double s2FinalLabeledFlowShift = executeBushDestinationLabeledBaseFlowShift(origin, currLabel, s2StartLabeledFlowShift, s2, flowAcceptanceFactors);
//
//      LOGGER.severe(
//          String.format("** S2 SHIFT: dest-label %d, shift-start-link %.10f, shift-final-link %.10f", currLabel.getLabelId(), s2StartLabeledFlowShift, s2FinalLabeledFlowShift));
//
//      /* shift flow across final merge for S2 */
//      executeBushDestinationLabeledS2FlowShiftEndMerge(origin, currLabel, s2FinalLabeledFlowShift, bushS2MergeExitShiftedSendingFlows);
//
//      /* shift flows across starting diverge before entering S2 using reciprocal of flow acceptance factor */
//      if (!s2DivergeProportionsByTurnLabels.isEmpty()) {
//        executeBushDestinationLabeledS2FlowShiftStartDiverge(origin, currLabel, s2StartLabeledFlowShift, s2DivergeProportionsByTurnLabels, flowAcceptanceFactors);
//      }
//    }
//
//    /* convert flows to portions by label */
//    bushS2MergeExitShiftedSendingFlows.forEach((label, flows) -> ArrayUtils.divideBySum(flows, 0));
//    var bushS2MergeExitShiftedSplittingRates = bushS2MergeExitShiftedSendingFlows;
//    bushS2MergeExitShiftedSendingFlows = null;
//
//    /*
//     * ------------------------------------------------- S1 FLOW SHIFT ----------------------------------------------------------------------------------------------------------
//     * Update S1 by shifting flow proportionally along encountered flow compositions matching with the PAS/origin/alternative
//     */
//    for (var labelPortionPair : bushAlternativeRelativeLabelProportions.entrySet()) {
//      var currLabel = labelPortionPair.getKey();
//      var currLabelPortion = labelPortionPair.getValue();
//
//      /* portion of flow attributed to composition label traversing s1 */
//      double s1StartLabeledFlowShift = currLabelPortion * bushFlowShift;
//      double s1FinalLabeledFlowShift = executeBushDestinationLabeledBaseFlowShift(origin, currLabel, s1StartLabeledFlowShift, s1, flowAcceptanceFactors);
//
//      LOGGER.severe(
//          String.format("** S1 SHIFT: dest-label %d, shift-start-link %.10f, shift-final-link %.10f", currLabel.getLabelId(), s1StartLabeledFlowShift, s1FinalLabeledFlowShift));
//
//      /* shift flow across final merge for S1 based on findings in s2 */
//      executeBushDestinationLabeledS1FlowShiftEndMerge(origin, currLabel, s1FinalLabeledFlowShift, bushS2MergeExitShiftedSplittingRates.get(currLabel));
//
//      if (!s2DivergeProportionsByTurnLabels.isEmpty()) {
//        /* shift flow across initial diverge into S1 based on findings in s2 */
//        executeBushLabeledS1FlowShiftStartDiverge(origin, currLabel, s1StartLabeledFlowShift, s2DivergeProportionsByTurnLabels, flowAcceptanceFactors);
//      }
//    }
//  }

  // TODO make it entrySegment+PAS compliant
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

//    /*
//     * Determine the portion to attribute to each used [turn,entry label] combination when shifting flow across the diverge. The portions are made proportional to contribution of
//     * each combination to the total sending flow on the s2-label flow on its initial link segment. Multikey: [entry segment, entry label]
//     */
//    MultiKeyMap<Object, Double> s2DivergeProportionsByTurnLabels = null;
//    if (pas.getDivergeVertex().hasEntryEdgeSegments()) {
//      s2DivergeProportionsByTurnLabels = createS2DivergeProportionsByTurnLabels(origin, s2DestinationLabelledAcceptedFlows, flowAcceptanceFactors);
//    }

    /* exit turn flows out of s2 by used exit label - to be populated in s2 merge update for use by s1 update */
    var bushS2MergeExitShiftedSendingFlows = new TreeMap<BushFlowLabel, double[]>();
    for (var labelPortionPair : bushAlternativeRelativeLabelProportions.entrySet()) {
      var currLabel = labelPortionPair.getKey();
      var currLabelPortion = labelPortionPair.getValue();

      /* shift portion of flow attributed to composition label traversing s2 */
      double s2StartLabeledFlowShift = -currLabelPortion * bushFlowShift;
      double s2FinalLabeledFlowShift = executeBushDestinationLabeledFlowShift(origin, entrySegment, currLabel, s2StartLabeledFlowShift, s2, flowAcceptanceFactors);

      LOGGER.severe(
          String.format("** S2 SHIFT: dest-label %d, shift-start-link %.10f, shift-final-link %.10f", currLabel.getLabelId(), s2StartLabeledFlowShift, s2FinalLabeledFlowShift));

      /* shift flow across final merge for S2 */
      executeBushDestinationLabeledS2FlowShiftEndMerge(origin, currLabel, s2FinalLabeledFlowShift, bushS2MergeExitShiftedSendingFlows);

//      /* shift flows across starting diverge before entering S2 using reciprocal of flow acceptance factor */
//      if (!s2DivergeProportionsByTurnLabels.isEmpty()) {
//        executeBushDestinationLabeledS2FlowShiftStartDiverge(origin, currLabel, s2StartLabeledFlowShift, s2DivergeProportionsByTurnLabels, flowAcceptanceFactors);
//      }
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

      LOGGER.severe(
          String.format("** S1 SHIFT: dest-label %d, shift-start-link %.10f, shift-final-link %.10f", currLabel.getLabelId(), s1StartLabeledFlowShift, s1FinalLabeledFlowShift));

      /* shift flow across final merge for S1 based on findings in s2 */
      executeBushDestinationLabeledS1FlowShiftEndMerge(origin, currLabel, s1FinalLabeledFlowShift, bushS2MergeExitShiftedSplittingRates.get(currLabel));

//      if (!s2DivergeProportionsByTurnLabels.isEmpty()) {
//        /* shift flow across initial diverge into S1 based on findings in s2 */
//        executeBushLabeledS1FlowShiftStartDiverge(origin, currLabel, s1StartLabeledFlowShift, s2DivergeProportionsByTurnLabels, flowAcceptanceFactors);
//      }
    }
  }

  /**
   * Constructor
   * 
   * @param pas to use
   */
  protected PasFlowShiftDestinationLabelledExecutor(Pas pas) {
    super(pas);
    this.s1DestinationLabelledFlows = new HashMap<>();
    this.s2DestinationLabelledFlows = new HashMap<>();
  }

}

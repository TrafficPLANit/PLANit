package org.goplanit.assignment.ltm.sltm;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.apache.commons.collections4.iterators.ReverseListIterator;
import org.goplanit.algorithms.shortestpath.ShortestPathResult;
import org.goplanit.utils.arrays.ArrayUtils;
import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.math.Precision;

/**
 * Paired Alternative Segment (PAS) implementation comprising two subpaths (segments), one of a higher cost than the other. In a PAS both subpaths start at the same vertex and end
 * at the same vertex without any intermediate links overlapping.
 * 
 * @author markr
 *
 */
public class Pas {

  /** logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(Pas.class.getCanonicalName());

  /** cheap PA segment s1 */
  private EdgeSegment[] s1;

  /** expensive PA segment s2 */
  private EdgeSegment[] s2;

  /** cheap path cost */
  private double s1Cost;

  /** expensive path cost */
  private double s2Cost;

  /** registered origin bushes */
  private final Set<Bush> originBushes;

  /**
   * The first time a PAS is used for flow shifting, its S1 segment has no labels yet along the PAS. Therefore we create a new label unique to the S1 alternative and populate the
   * s1MatchingLabelsToFill map so it can be used in the correct format for flow shifting. Also, we create the related pasS1EndLabelRates indicating 100% of the flow on the PAS S1
   * segment for this bush is allocated to this new label at present. Again, to directly be able to use it in the flow shifting format that we use
   * 
   * @param origin                 to initialise labelling for
   * @param s1MatchingLabelsToFill map to populate with the new label
   * @return pasS1EndLabelRates created indicatin 1 (100%) of flow is allocated to the new label on the final segment of s1
   */
  private Map<BushFlowCompositionLabel, Double> initialiseS1Labelling(final Bush origin, Map<BushFlowCompositionLabel, List<BushFlowCompositionLabel>> s1MatchingLabelsToFill) {
    BushFlowCompositionLabel pasS1Label = origin.createFlowCompositionLabel();
    s1MatchingLabelsToFill.put(pasS1Label, new ArrayList<BushFlowCompositionLabel>(1));
    s1MatchingLabelsToFill.get(pasS1Label).add(pasS1Label);

    Map<BushFlowCompositionLabel, Double> pasS1EndLabelRates = new HashMap<BushFlowCompositionLabel, Double>();
    pasS1EndLabelRates.put(pasS1Label, 1.0);

    return pasS1EndLabelRates;
  }

  /**
   * Remove bushes from this PAS
   * 
   * @param bushes to remove
   */
  private void removeOrigins(List<Bush> originsWithoutRemainingPasFlow) {
    originsWithoutRemainingPasFlow.forEach((bush) -> originBushes.remove(bush));
  }

  /**
   * Determine all labels (on the final segment) that represent flow that is fully overlapping with the indicated PAS segment (low or high cost). We also provide composite labels
   * into which the final labels have split off from during their journey from the start to the end of the PAS alternative (if any). These composite labels are provided as a
   * separate list per matched label where the last entry represents the label closest to the starting point of the PAS segment (upstream)
   * 
   * @param originBush     to do this for
   * @param lowCostSegment the segment to verify against
   * @return found matching composition labels as keys, where the values are an ordered list of encountered composite labels when traversing along the PAS (if any)
   */
  private Map<BushFlowCompositionLabel, List<BushFlowCompositionLabel>> determineMatchingLabels(final Bush originBush, boolean lowCostSegment) {
    Set<BushFlowCompositionLabel> edgeSegmentCompositionLabels = originBush.getFlowCompositionLabels(getLastEdgeSegment(lowCostSegment));
    Map<BushFlowCompositionLabel, List<BushFlowCompositionLabel>> pasCompositionLabels = new HashMap<BushFlowCompositionLabel, List<BushFlowCompositionLabel>>();

    EdgeSegment[] alternative = lowCostSegment ? s1 : s2;
    Iterator<BushFlowCompositionLabel> labelIter = edgeSegmentCompositionLabels.iterator();
    while (labelIter.hasNext()) {
      BushFlowCompositionLabel initialLabel = labelIter.next();
      BushFlowCompositionLabel currentLabel = initialLabel;
      List<BushFlowCompositionLabel> transitionLabels = null;

      EdgeSegment currentSegment = null;
      EdgeSegment succeedingSegment = getLastEdgeSegment(lowCostSegment);
      for (int index = alternative.length - 2; index >= 0; --index) {
        currentSegment = alternative[index];
        if (!originBush.containsTurnSendingFlow(currentSegment, currentLabel, succeedingSegment, currentLabel)) {
          /* label transition or no match */
          BushFlowCompositionLabel transitionLabel = null;
          Set<BushFlowCompositionLabel> potentialLabelTransitions = originBush.getFlowCompositionLabels(currentSegment);
          for (BushFlowCompositionLabel potentialLabel : potentialLabelTransitions) {
            if (originBush.containsTurnSendingFlow(currentSegment, potentialLabel, succeedingSegment, currentLabel)) {
              transitionLabel = potentialLabel;
              if (transitionLabels == null) {
                transitionLabels = new ArrayList<BushFlowCompositionLabel>();
              }
              transitionLabels.add(transitionLabel);
            }
          }
          if (transitionLabel == null) {
            /* no match - remove the original label we started with */
            break;
          }
          /* transition - update label representing composite flow that contains label under investigation */
          currentLabel = transitionLabel;
        }
        succeedingSegment = currentSegment;
      }
      pasCompositionLabels.put(initialLabel, transitionLabels == null ? new ArrayList<BushFlowCompositionLabel>(0) : transitionLabels);
    }
    return pasCompositionLabels;
  }

  /**
   * Helper method that extract from the used labels at the final segment of the PAS alternative - obtained in {@link #determineMatchingLabels(Bush, boolean)} - the used labels at
   * the start of the PAS alternative.
   * <p>
   * list of map is ordered in reverse with respect to encountered labels on PAS segment (is assumed), so we collect each key's last element and add it to the result set
   * 
   * @param pasAlternativeEndFlowCompositionLabels used labels at final S2 segment (key), and its predecessors along the segment (value)
   * @return the unique set of initial labels used at the start of the PAS
   */
  private Set<BushFlowCompositionLabel> extractUsedStartLabels(Map<BushFlowCompositionLabel, List<BushFlowCompositionLabel>> pasAlternativeEndFlowCompositionLabels) {
    Set<BushFlowCompositionLabel> usedStartLabels = new HashSet<BushFlowCompositionLabel>();
    for (List<BushFlowCompositionLabel> usedLabelPrecessors : pasAlternativeEndFlowCompositionLabels.values()) {
      usedStartLabels.add(usedLabelPrecessors.get(usedLabelPrecessors.size() - 1));
    }
    return usedStartLabels;
  }

  /**
   * Execute a flow shift on a given bush for the given PAS segment. This does not move flow through the final merge vertex nor the initial diverge vertex.
   * <p>
   * In the special case the shifted flow is to be added based on a new label because no flow exists along the PAS yet, set forceInitialLabel to true
   * 
   * @param origin                        bush at hand
   * @param reverseOrderCompositionLabels the composition labels along the PAS segment to follow (in reverse order)
   * @param flowShiftPcuH                 to execute (assumed to be correctly proportioned in relation to other bushes and labels within bush for this PAS)
   * @param pasSegment                    to update on bush
   * @param flowAcceptanceFactors         to use when updating the flows
   * @param forceInitialLabel             when true, all to be shifted flow is labelled based on the first label present in the reverseOrderCompositionLabels, when false, we only
   *                                      use existing labels conforming with the reverseOrderCompositionLabels
   * @return sending flow on last edge segment of the PAS alternative after the flow shift (considering encountered reductions)
   */
  private double executeBushLabeledAlternativeFlowShift(Bush origin, List<BushFlowCompositionLabel> reverseOrderCompositionLabels, double flowShiftPcuH, EdgeSegment[] pasSegment,
      double[] flowAcceptanceFactors, boolean forceInitialLabel) {
    int index = 0;
    EdgeSegment currentSegment = null;
    EdgeSegment nextSegment = pasSegment[index];

    ReverseListIterator<BushFlowCompositionLabel> reverseIter = new ReverseListIterator<BushFlowCompositionLabel>(reverseOrderCompositionLabels);
    BushFlowCompositionLabel currCompositionLabel = reverseIter.next();
    BushFlowCompositionLabel nextCompositionLabel = currCompositionLabel;
    while (++index < pasSegment.length) {
      currentSegment = nextSegment;
      nextSegment = pasSegment[index];

      double turnSendingFlow = origin.getTurnSendingFlow(currentSegment, currCompositionLabel, nextSegment, currCompositionLabel);
      if (!forceInitialLabel && !Precision.isPositive(turnSendingFlow)) {
        /* composition splits/ends, identify if next label it splits off in is valid/available */
        nextCompositionLabel = reverseIter.hasNext() ? reverseIter.next() : null;
        if (nextCompositionLabel != null && origin.containsTurnSendingFlow(currentSegment, currCompositionLabel, nextSegment, nextCompositionLabel)) {
          turnSendingFlow = origin.getTurnSendingFlow(currentSegment, currCompositionLabel, nextSegment, nextCompositionLabel);
        } else {
          LOGGER.warning("Unable to trace PAS s2 flow through alternative with the given flow composition chain, aborting flow shift");
        }
      }

      if (!Precision.isPositive(turnSendingFlow + flowShiftPcuH)) {
        /* no remaining flow at all after flow shift, remove turn from bush entirely */
        origin.removeTurn(currentSegment, nextSegment);
      } else {
        origin.addTurnSendingFlow(currentSegment, currCompositionLabel, nextSegment, nextCompositionLabel, flowShiftPcuH);
      }
      flowShiftPcuH *= flowAcceptanceFactors[(int) currentSegment.getId()];

      currCompositionLabel = nextCompositionLabel;
    }

    return flowShiftPcuH;
  }

  /**
   * Perform the flow shift through the end merge vertex of the PASs high cost segment for the given origin bush flow composition
   * 
   * @param origin                           to use
   * @param finalSegmentLabel                flow composition label on s2 final segment to apply
   * @param s2FinalLabeledFlowShift          the flow shift applied so far up to the final merge
   * @param exitShiftedSendingFlowToPopulate map to populate with the found exit segment flows (values) by used exit label (key)
   */
  private void executeBushLabeledS2FlowShiftEndMerge(Bush origin, BushFlowCompositionLabel finalSegmentLabel, double s2FinalLabeledFlowShift,
      Map<BushFlowCompositionLabel, double[]> exitShiftedSendingFlowToPopulate) {

    EdgeSegment lastS2Segment = getLastEdgeSegment(false /* high cost */);

    /* remove shifted flows through final merge towards exit segments proportionally, to later add to s1 turns through merge */
    if (getMergeVertex().hasExitEdgeSegments()) {
      List<BushFlowCompositionLabel> usedExitLabels = origin.determineUsedTurnCompositionLabels(lastS2Segment, finalSegmentLabel);
      for (BushFlowCompositionLabel exitLabel : usedExitLabels) {
        int index = 0;
        double[] splittingRates = origin.getSplittingRates(lastS2Segment, finalSegmentLabel, exitLabel);
        for (EdgeSegment exitSegment : getMergeVertex().getExitEdgeSegments()) {
          if (origin.containsEdgeSegment(exitSegment)) {
            double splittingRate = splittingRates[index];

            /* remove flow for s2 */
            double s2FlowShift = s2FinalLabeledFlowShift * splittingRate;
            if (!Precision.isPositive(origin.getTurnSendingFlow(lastS2Segment, exitSegment) + s2FlowShift)) {
              /* no remaining flow at all after flow shift, remove turn from bush entirely */
              origin.removeTurn(lastS2Segment, exitSegment);
            } else {
              origin.addTurnSendingFlow(lastS2Segment, finalSegmentLabel, exitSegment, exitLabel, s2FlowShift);
            }

            /* track so we can attribute it to s1 segment later */
            double[] exitLabelExitSegmentShiftedSendingFlow = exitShiftedSendingFlowToPopulate.get(exitLabel);
            if (exitLabelExitSegmentShiftedSendingFlow == null) {
              exitLabelExitSegmentShiftedSendingFlow = new double[this.getMergeVertex().getExitEdgeSegments().size()];
              exitShiftedSendingFlowToPopulate.put(exitLabel, exitLabelExitSegmentShiftedSendingFlow);
            }
            exitLabelExitSegmentShiftedSendingFlow[index] += -s2FlowShift;
            ++index;
          }
        }
      }
    }
  }

  /**
   * Perform the flow shift through the end merge vertex of the PASs low cost segment for the given origin bush flow composition
   * 
   * @param origin                  to use
   * @param finalSegmentLabel       flow composition label on s2 final segment to apply
   * @param s1FinalLabeledFlowShift the flow shift applied so far up to the final merge
   * @param usedLabelSplittingRates the splitting rates to apply per used label towards the available exit segments where the key is the exit label and the value the splitting
   *                                rates towards each exit
   */
  private void executeBushLabeledS1FlowShiftEndMerge(Bush origin, BushFlowCompositionLabel finalSegmentLabel, double s1FinalLabeledFlowShift,
      Map<BushFlowCompositionLabel, double[]> usedLabelSplittingRates) {

    EdgeSegment lastS1Segment = getLastEdgeSegment(true /* low cost */);

    /* add shifted flows through final merge towards exit segments proportionally based on labeled exit usage */
    if (getMergeVertex().hasExitEdgeSegments()) {
      for (Entry<BushFlowCompositionLabel, double[]> entry : usedLabelSplittingRates.entrySet()) {
        BushFlowCompositionLabel exitLabel = entry.getKey();
        double[] exitLabelSplittingRates = entry.getValue();
        int index = 0;
        for (EdgeSegment exitSegment : getMergeVertex().getExitEdgeSegments()) {
          if (origin.containsEdgeSegment(exitSegment)) {
            double splittingRate = exitLabelSplittingRates[index];

            /* add flow for s1 */
            double s1FlowShift = s1FinalLabeledFlowShift * splittingRate;
            origin.addTurnSendingFlow(lastS1Segment, finalSegmentLabel, exitSegment, exitLabel, s1FlowShift);
          }
          ++index;
        }
      }
    }
  }

  /**
   * Perform the flow shift through the start diverge vertex of the PASs high cost segment for the given origin bush flow composition
   * 
   * @param origin                                      to use
   * @param startSegmentLabel                           flow composition label on s2 initial segment to apply
   * @param s2StartLabeledFlowShift                     the flow shift applied to the first s2 segment
   * @param s2DivergeEntrySegmentBackwardSplittingRates backward splitting rates to S2 initial segment attributed to each entry by used label
   * @param flowAcceptanceFactors                       to use
   */
  private void executeBushLabeledS2FlowShiftStartDiverge(Bush origin, BushFlowCompositionLabel startSegmentLabel, double s2StartLabeledFlowShift,
      Map<BushFlowCompositionLabel, double[]> s2DivergeEntrySegmentBackwardSplittingRates, final double[] flowAcceptanceFactors) {

    EdgeSegment firstS2Segment = getFirstEdgeSegment(false /* high cost */);

    for (Entry<BushFlowCompositionLabel, double[]> divergeLabelEntry : s2DivergeEntrySegmentBackwardSplittingRates.entrySet()) {
      BushFlowCompositionLabel entryLabel = divergeLabelEntry.getKey();
      double[] bushEntryTurnBackwardSplittingRates = divergeLabelEntry.getValue();
      int index = 0;
      for (EdgeSegment entrySegment : getDivergeVertex().getEntryEdgeSegments()) {
        if (origin.containsEdgeSegment(entrySegment)) {
          double entryLabelPortion = bushEntryTurnBackwardSplittingRates[index];
          if (!Precision.isPositive(entryLabelPortion)) {
            continue;
          }

          /* convert back to sending flow as alpha<1 increases sending flow on entry segment compared to the sending flow component on the first s2 segment */
          double s2DivergeEntryLabeledFlowShift = s2StartLabeledFlowShift * entryLabelPortion * (1 / flowAcceptanceFactors[(int) entrySegment.getId()]);

          double existingTotalTurnLabeledSendingFlow = origin.getTurnSendingFlow(entrySegment, entryLabel, firstS2Segment, startSegmentLabel);
          if (!Precision.isPositive(existingTotalTurnLabeledSendingFlow)) {
            LOGGER.severe("Expected available turn sending flow for given label combination, found none, skip flow shift at PAS s2 diverge");
            continue;
          }

          if (!Precision.isPositive(existingTotalTurnLabeledSendingFlow + s2DivergeEntryLabeledFlowShift)) {
            /* no remaining flow at all after flow shift, remove turn from bush entirely */
            origin.removeTurn(entrySegment, firstS2Segment);
          } else {
            origin.addTurnSendingFlow(entrySegment, entryLabel, firstS2Segment, startSegmentLabel, s2DivergeEntryLabeledFlowShift);
          }
        }
        ++index;
      }
    }
  }

  /**
   * Perform the flow shift through the start diverge vertex of the PASs low cost segment for the given origin bush flow composition. We use the same proportions that were applied
   * in the S2 diverge update via the usedLabelBackwardSplittingRates. These splitting rates are not normal splitting rates bu instead reflect the backward splitting rates of ONLY
   * the shifted flows per entry label, and we should use this same distribution when dealing with t
   * 
   * @param origin                          to use
   * @param startSegmentLabel               flow composition label on s1 initial segment to apply
   * @param s1StartLabeledFlowShift         the flow shift applied to the first s1 segment for the label
   * @param usedLabelBackwardSplittingRates the backward splitting rates for a used (meaning flow has been shifted for this entry label for the PAS). To be applied to the flows
   *                                        shifted towards S1 the same way as it has been used for s2, only now apply it to the S1 exit label shifted flow
   * @param flowAcceptanceFactors           to use
   */
  private void executeBushLabeledS1FlowShiftStartDiverge(Bush origin, BushFlowCompositionLabel startSegmentLabel, double s1StartLabeledFlowShift,
      Map<BushFlowCompositionLabel, double[]> usedLabelBackwardSplittingRates, final double[] flowAcceptanceFactors) {
    EdgeSegment firstS1Segment = getFirstEdgeSegment(true /* low cost */);

    for (Entry<BushFlowCompositionLabel, double[]> divergeLabelEntry : usedLabelBackwardSplittingRates.entrySet()) {
      BushFlowCompositionLabel entryLabel = divergeLabelEntry.getKey();
      double[] bushEntryTurnBackwardSplittingRates = divergeLabelEntry.getValue();
      int index = 0;
      for (EdgeSegment entrySegment : getDivergeVertex().getEntryEdgeSegments()) {
        if (origin.containsEdgeSegment(entrySegment)) {
          double entryLabelPortion = bushEntryTurnBackwardSplittingRates[index];
          if (!Precision.isPositive(entryLabelPortion)) {
            continue;
          }

          /* convert back to sending flow as alpha<1 increases sending flow on entry segment compared to the sending flow component on the first s1 segment */
          double s1DivergeEntryLabeledFlowShift = s1StartLabeledFlowShift * entryLabelPortion * (1 / flowAcceptanceFactors[(int) entrySegment.getId()]);

          double existingTotalTurnLabeledSendingFlow = origin.getTurnSendingFlow(entrySegment, entryLabel, firstS1Segment, startSegmentLabel);
          if (!Precision.isPositive(existingTotalTurnLabeledSendingFlow)) {
            LOGGER.severe("Expected available turn sending flow for given label combination, found none, skip flow shift at PAS s1 diverge");
            continue;
          }

          origin.addTurnSendingFlow(entrySegment, entryLabel, firstS1Segment, startSegmentLabel, s1DivergeEntryLabeledFlowShift);

        }
        ++index;
      }
    }
  }

  /**
   * Constructor
   * 
   * @param s1 cheap subpath
   * @param s2 expensive subpath
   */
  private Pas(final EdgeSegment[] s1, final EdgeSegment[] s2) {
    this.s1 = s1;
    this.s2 = s2;
    this.originBushes = new HashSet<Bush>();
  }

  /**
   * update costs of an alternative
   * 
   * @param edgeSegmentCosts to use
   * @param updateS1         Flag indicating to update cost of s1 (cheap) segment, when false update the s2 (costlier) segment
   */
  protected void updateCost(final double[] edgeSegmentCosts, boolean updateS1) {

    EdgeSegment[] alternative = updateS1 ? s1 : s2;
    double cost = 0;
    for (int index = 0; index < alternative.length; ++index) {
      cost += edgeSegmentCosts[(int) alternative[index].getId()];
    }

    if (updateS1) {
      s1Cost = cost;
    } else {
      s2Cost = cost;
    }
  }

  /**
   * Shift flows for this PAS given the currently known costs and smoothing procedure to apply
   * 
   * @param networkS2FlowPcuH     total flow currently using the high cost alternative
   * @param flowShiftPcuH         amount to shift from high cost to low cost segment
   * @param flowAcceptanceFactors to use
   * @return true when flow shifted, false otherwise
   */
  protected boolean executeFlowShift(double networkS2FlowPcuH, double flowShiftPcuH, final double[] flowAcceptanceFactors) {

    List<Bush> originsWithoutRemainingPasFlow = new ArrayList<Bush>();
    EdgeSegment lastS1Segment = getLastEdgeSegment(true /* low cost */);
    EdgeSegment lastS2Segment = getLastEdgeSegment(false /* high cost */);

    for (Bush origin : originBushes) {

      double bushS2Flow = origin.computeSubPathSendingFlow(getDivergeVertex(), getMergeVertex(), s2);

      /* Bush flow portion */
      double bushPortion = Precision.isPositive(networkS2FlowPcuH) ? Math.min(bushS2Flow / networkS2FlowPcuH, 1) : 1;
      double bushFlowShift = flowShiftPcuH * bushPortion;
      if (Precision.isGreaterEqual(bushFlowShift, bushS2Flow)) {
        /* remove this origin from the PAS when done as no flow remains on high cost segment */
        originsWithoutRemainingPasFlow.add(origin);
        /* remove what we can */
        bushFlowShift = bushS2Flow;
      }

      /* LABEL SPECIFIC PREP - START */

      // TODO: below can be combined with determining the subPathSending flow for efficiency
      Map<BushFlowCompositionLabel, List<BushFlowCompositionLabel>> pasS2EndFlowCompositionLabels = determineMatchingLabels(origin, false /* high cost segment */);
      Set<BushFlowCompositionLabel> pasS2UsedStartLabels = extractUsedStartLabels(pasS2EndFlowCompositionLabels);

      /* Backward splitting rates regarding the origin of the flow towards S2 for the used entry label. Used only to determine flow shift through diverge towards PAS segments */
      /*
       * TODO: since the principle of labelling is to always use unique labels when flow merges (only true when we give each PAS alternative its own label), all backward splitting
       * rates should only have a single non-zero entry per used label. If this is the case we do not need backward splitting rates but instead simply the entering segment for each
       * label. Verify this and if so refactor this. The same goes for s2DivergeEntryShiftedSendingFlows (per used entry label) since each entry label should only exist on a single
       * in-link anyway
       */
      int numberOfUsedEntrySegments = 0;
      Map<BushFlowCompositionLabel, double[]> s2DivergeEntryBackwardSplittingRates = new HashMap<BushFlowCompositionLabel, double[]>();
      if (getDivergeVertex().hasEntryEdgeSegments()) {
        numberOfUsedEntrySegments = populateS2DivergeBackwardSplittingRates(origin, pasS2UsedStartLabels, flowAcceptanceFactors, s2DivergeEntryBackwardSplittingRates);
      }

      /* exit turn flows out of s2 by used exit label - to be populated in s2 merge update for use by s1 update */
      Map<BushFlowCompositionLabel, double[]> s2MergeExitShiftedSendingFlows = new HashMap<BushFlowCompositionLabel, double[]>();

      /* LABEL SPECIFIC PREP - END */

      /*
       * ------------------------------------------------- S2 FLOW SHIFT ----------------------------------------------------------------------------------------------------------
       * Update S2 by shifting flow proportionally along encountered flow compositions matching with the PAS/origin/alternative
       */
      Map<BushFlowCompositionLabel, Double> pasS2EndLabelRates = origin.determineProportionalFlowCompositionRates(lastS2Segment, pasS2EndFlowCompositionLabels.keySet());
      for (Entry<BushFlowCompositionLabel, Double> mergeLabelEntry : pasS2EndLabelRates.entrySet()) {

        BushFlowCompositionLabel finalSegmentLabel = mergeLabelEntry.getKey();
        List<BushFlowCompositionLabel> reverseOrderS2Labels = pasS2EndFlowCompositionLabels.get(finalSegmentLabel);
        BushFlowCompositionLabel initialSegmentLabel = reverseOrderS2Labels.get(reverseOrderS2Labels.size() - 1);

        /* shift portion of flow attributed to composition label traversing s2 */
        double s2StartLabeledFlowShift = mergeLabelEntry.getValue() * bushFlowShift;
        double s2FinalLabeledFlowShift = executeBushLabeledAlternativeFlowShift(origin, reverseOrderS2Labels, -s2StartLabeledFlowShift, s2, flowAcceptanceFactors, false);

        /* shift flow across final merge for S2 */
        executeBushLabeledS2FlowShiftEndMerge(origin, finalSegmentLabel, s2FinalLabeledFlowShift, s2MergeExitShiftedSendingFlows);

        /* shift flows across starting diverge before entering S2 using reciprocal of flow acceptance factor */
        if (numberOfUsedEntrySegments >= 1) {
          executeBushLabeledS2FlowShiftStartDiverge(origin, initialSegmentLabel, -s2StartLabeledFlowShift, s2DivergeEntryBackwardSplittingRates, flowAcceptanceFactors);
        }
      }

      /* convert flows to exit segment splitting rates by label */
      Collection<BushFlowCompositionLabel> useExitLabels = s2MergeExitShiftedSendingFlows.keySet();
      for (BushFlowCompositionLabel exitLabel : useExitLabels) {
        ArrayUtils.divideBySum(s2MergeExitShiftedSendingFlows.get(exitLabel), 0);
      }

      /*
       * ------------------------------------------------- S2 FLOW SHIFT ----------------------------------------------------------------------------------------------------------
       * Update S1 by shifting flow proportionally along encountered flow compositions matching with the PAS/origin/alternative
       */
      boolean s1SegmentNotUsedYet = false;
      Map<BushFlowCompositionLabel, List<BushFlowCompositionLabel>> pasS1FlowCompositionLabels = determineMatchingLabels(origin, true /* low cost segment */);
      Map<BushFlowCompositionLabel, Double> pasS1EndLabelRates = null;
      if (pasS1FlowCompositionLabels.isEmpty()) {
        s1SegmentNotUsedYet = true;
        pasS1EndLabelRates = initialiseS1Labelling(origin, pasS1FlowCompositionLabels);
      } else {
        pasS1EndLabelRates = origin.determineProportionalFlowCompositionRates(lastS1Segment, pasS1FlowCompositionLabels.keySet());
      }
      for (Entry<BushFlowCompositionLabel, Double> labelEntry : pasS1EndLabelRates.entrySet()) {
        BushFlowCompositionLabel finalSegmentLabel = labelEntry.getKey();
        List<BushFlowCompositionLabel> reverseOrderS1Labels = pasS1FlowCompositionLabels.get(finalSegmentLabel);
        BushFlowCompositionLabel initialSegmentLabel = reverseOrderS1Labels.get(reverseOrderS1Labels.size() - 1);

        /* portion of flow attributed to composition label traversing s1 */
        double s1StartLabeledFlowShift = labelEntry.getValue() * bushFlowShift;
        double s1FinalLabeledFlowShift = executeBushLabeledAlternativeFlowShift(origin, reverseOrderS1Labels, s1StartLabeledFlowShift, s1, flowAcceptanceFactors,
            s1SegmentNotUsedYet);

        /* shift flow across final merge for S1 based on findings in s2 */
        executeBushLabeledS1FlowShiftEndMerge(origin, finalSegmentLabel, s1FinalLabeledFlowShift, s2MergeExitShiftedSendingFlows);

        if (numberOfUsedEntrySegments >= 1) {
          /* shift flow across initial diverge into S1 based on findings in s2 */
          executeBushLabeledS1FlowShiftStartDiverge(origin, initialSegmentLabel, s1StartLabeledFlowShift, s2DivergeEntryBackwardSplittingRates, flowAcceptanceFactors);
        }
      }
    }

    /* remove irrelevant bushes */
    removeOrigins(originsWithoutRemainingPasFlow);

    return true;
  }

  /**
   * Backward splitting rates regarding the origin of the flow towards S2 for the used entry label. This allows us to determine how to redistribute shifted flow on S2 towards S1
   * for the PAS diverge's incoming edge segments with non-zero flow into the PAS. These backwards plitting rates are tracked per label since all flow shifted for this label needs
   * to be uniquely traceable and shifted from {entrylabel,entrysegment} - s2label towards {entrylabel, entrysegment} - s1label
   *
   * @param origin                                         at hand
   * @param pasS2UsedStartLabels                           the labels that have non-zero flow along S2
   * @param flowAcceptanceFactors                          to use
   * @param s2DivergeEntryBackwardSplittingRatesToPopulate to populate
   * @return number of used entry segments zero when PAS diverges at an origin
   */
  private int populateS2DivergeBackwardSplittingRates(Bush origin, Set<BushFlowCompositionLabel> pasS2UsedStartLabels, final double[] flowAcceptanceFactors,
      Map<BushFlowCompositionLabel, double[]> s2DivergeEntryBackwardSplittingRatesToPopulate) {

    int numberOfUsedEntrySegments = 0;
    EdgeSegment firstS2EdgeSegment = getFirstEdgeSegment(false /* high cost segment */);

    /* total turn accepted flow into PAS s2 from bush as well as per entry label turn flows */
    int index = 0;
    for (EdgeSegment entrySegment : getDivergeVertex().getEntryEdgeSegments()) {
      if (origin.containsEdgeSegment(entrySegment)) {
        Set<BushFlowCompositionLabel> entryLabels = origin.getFlowCompositionLabels(entrySegment);
        for (BushFlowCompositionLabel entryLabel : entryLabels) {
          for (BushFlowCompositionLabel exitLabel : pasS2UsedStartLabels) {
            double turnSendingFlow = origin.getTurnSendingFlow(entrySegment, entryLabel, firstS2EdgeSegment, exitLabel);
            double turnAcceptedFlow = turnSendingFlow * flowAcceptanceFactors[(int) entrySegment.getId()];
            double[] entryTurnAcceptedFlows = s2DivergeEntryBackwardSplittingRatesToPopulate.get(entryLabel);
            if (entryTurnAcceptedFlows == null) {
              entryTurnAcceptedFlows = new double[getDivergeVertex().getEntryEdgeSegments().size()];
              s2DivergeEntryBackwardSplittingRatesToPopulate.put(entryLabel, entryTurnAcceptedFlows);
            }
            entryTurnAcceptedFlows[index] += turnAcceptedFlow;
          }
        }
        ++numberOfUsedEntrySegments;
      }
      ++index;
    }

    /* determine split of flows across the used entry segments at diverge for each used entry label towards PAS s2 initial segment */
    for (Entry<BushFlowCompositionLabel, double[]> entry : s2DivergeEntryBackwardSplittingRatesToPopulate.entrySet()) {
      ArrayUtils.divideBySum(entry.getValue(), 0);
    }
    return numberOfUsedEntrySegments;
  }

  /**
   * Create a new PAS (factory method)
   * 
   * @param s1 to use
   * @param s2 to use
   * 
   * @return newly created PAS
   */
  protected static Pas create(final EdgeSegment[] s1, final EdgeSegment[] s2) {
    return new Pas(s1, s2);
  }

  /**
   * Collect the end vertex of the PAS
   * 
   * @return end vertex
   */
  public DirectedVertex getMergeVertex() {
    return s2[s2.length - 1].getDownstreamVertex();
  }

  /**
   * Collect the start vertex of the PAS
   * 
   * @return start vertex
   */
  public DirectedVertex getDivergeVertex() {
    return s2[0].getUpstreamVertex();
  }

  /**
   * Register origin on the PAS
   * 
   * @param origin bush to register
   */
  public void registerOrigin(final Bush origin) {
    originBushes.add(origin);
  }

  /**
   * Verify if origin is registered on PAS
   * 
   * @param originBush to check
   * @return true when registered, false otherwise
   */
  public boolean hasRegisteredOrigin(final Bush originBush) {
    return originBushes.contains(originBush);
  }

  /**
   * Verify if PAS (still) has origins registered on it
   * 
   * @return true when origins are present, false otherwise
   */
  public boolean hasOrigins() {
    return !originBushes.isEmpty();
  }

  /**
   * Check if bush is overlapping with one of the alternatives, and if it is how much sending flow this sub-path currently represents
   * 
   * @param bush                             to verify
   * @param lowCost                          when true check with low cost alternative otherwise high cost
   * @param linkSegmentFlowAcceptanceFactors to use to obtain accepted flow along subpath, where the flow at the start of the high cost segment is used as starting demand
   * @return when non-negative the segment is overlapping with the PAS, where the value indicates the accepted flow on this sub-path for the bush (with sendinf flow at start as
   *         base demand)
   */
  public double computeOverlappingAcceptedFlow(Bush bush, boolean lowCost, double[] linkSegmentFlowAcceptanceFactors) {
    EdgeSegment[] alternative = lowCost ? s1 : s2;
    return bush.computeSubPathAcceptedFlow(getDivergeVertex(), getMergeVertex(), alternative, linkSegmentFlowAcceptanceFactors);
  }

  /**
   * check if shortest path tree is overlapping with one of the alternatives
   * 
   * @param pathMatchForCheapPath to verify
   * @param lowCost               when true check with low cost alternative otherwise high cost
   * @return true when overlapping, false otherwise
   */
  public boolean isOverlappingWith(ShortestPathResult pathMatchForCheapPath, boolean lowCost) {
    EdgeSegment[] alternative = lowCost ? s1 : s2;
    EdgeSegment currEdgeSegment = null;
    EdgeSegment matchingEdgeSegment = null;
    for (int index = alternative.length - 1; index >= 0; --index) {
      currEdgeSegment = alternative[index];
      matchingEdgeSegment = pathMatchForCheapPath.getIncomingEdgeSegmentForVertex(currEdgeSegment.getDownstreamVertex());
      if (!currEdgeSegment.idEquals(matchingEdgeSegment)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Check if any of the set link segments is present on the indicated alternative
   * 
   * @param linkSegments where we verify against set link segments
   * @param lowCost      when true check with low cost alternative otherwise high cost
   * @return true when overlapping, false otherwise
   */
  public boolean containsAny(final BitSet linkSegments, boolean lowCost) {
    EdgeSegment[] alternative = lowCost ? s1 : s2;
    EdgeSegment currEdgeSegment = null;
    for (int index = alternative.length - 1; index >= 0; --index) {
      currEdgeSegment = alternative[index];
      if (linkSegments.get((int) currEdgeSegment.getId())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Check if any of the set link segments is present on either alternative
   * 
   * @param linkSegments where we verify against set link segments
   * @return true when overlapping, false otherwise
   */
  public boolean containsAny(final BitSet linkSegments) {
    return containsAny(linkSegments, true) || containsAny(linkSegments, false);
  }

  /**
   * update costs of both paths. In case the low cost path is no longer the low cost path, switch it with the high cost path
   * 
   * @param edgeSegmentCosts to use
   * @return true when updated costs caused a switch in what is the high and low cost path
   */
  public boolean updateCost(final double[] edgeSegmentCosts) {
    updateCost(edgeSegmentCosts, true);
    updateCost(edgeSegmentCosts, false);

    if (s1Cost > s2Cost) {
      double tempCost = s1Cost;
      s1Cost = s2Cost;
      s2Cost = tempCost;

      EdgeSegment[] tempSegment = s1;
      s1 = s2;
      s2 = tempSegment;
      return true;
    }
    return false;
  }

  /**
   * Apply consumer to each vertex on one of the cost segments
   * 
   * @param lowCostSegment when true applied to low cost segment, when false the high cost segment
   * @param vertexConsumer to apply
   */
  public void forEachVertex(boolean lowCostSegment, Consumer<DirectedVertex> vertexConsumer) {
    EdgeSegment[] alternative = getAlternative(lowCostSegment);
    for (int index = 0; index < alternative.length; ++index) {
      vertexConsumer.accept(alternative[index].getUpstreamVertex());
    }
    vertexConsumer.accept(alternative[alternative.length - 1].getDownstreamVertex());
  }

  /**
   * Apply consumer to each edgeSegment on one of the cost segments
   * 
   * @param lowCostSegment      when true applied to low cost segment, when false the high cost segment
   * @param edgeSegmentConsumer to apply
   */
  public void forEachEdgeSegment(boolean lowCostSegment, Consumer<EdgeSegment> edgeSegmentConsumer) {
    EdgeSegment[] alternative = getAlternative(lowCostSegment);
    for (int index = 0; index < alternative.length; ++index) {
      edgeSegmentConsumer.accept(alternative[index]);
    }
  }

  /**
   * get cost of high cost alternative segment
   * 
   * @return cost
   */
  public double getAlternativeHighCost() {
    return s2Cost;
  }

  /**
   * get cost of high cost alternative segment
   * 
   * @return cost
   */
  public double getAlternativeLowCost() {
    return s1Cost;
  }

  /**
   * Collect the last edge segment of one of the two segments
   * 
   * @param lowCostSegment when true collect for low cost segment, otherwise the high cost segment
   * @return edge segment
   */
  public EdgeSegment getLastEdgeSegment(boolean lowCostSegment) {
    return lowCostSegment ? s1[s1.length - 1] : s2[s2.length - 1];
  }

  /**
   * Collect the first edge segment of one of the two segments
   * 
   * @param lowCostSegment when true collect for low cost segment, otherwise the high cost segment
   * @return edge segment
   */
  public EdgeSegment getFirstEdgeSegment(boolean lowCostSegment) {
    return lowCostSegment ? s1[0] : s2[0];
  }

  /**
   * Access to the two alternatives that reflect the PAS
   * 
   * @param lowCostSegment when true return s1 (lowCost), otherwise s2 (highCost)
   * @return ordered edge segments representing the alternative
   */
  public EdgeSegment[] getAlternative(boolean lowCostSegment) {
    return lowCostSegment ? s1 : s2;
  }

  /**
   * Returns the difference between the cost of the high cost and the low cost segment. Should always be larger than zero assuming an {@link #updateCost(double[])} has been
   * conducted to ensure the segments are labelled correctly regarding which one is high and which one is low cost
   * 
   * @return s2Cost - s2Cost
   */
  public double getReducedCost() {
    return s2Cost - s1Cost;
  }

  /**
   * Match first link segment of PAS segment to predicate provided
   * 
   * @param lowCostSegment when true apply on s1, otherwise on s2
   * @param predicate      to test
   * @return edge segment that matches, null if none matches
   */
  public EdgeSegment matchFirst(boolean lowCostSegment, Predicate<EdgeSegment> predicate) {
    EdgeSegment[] alternative = getAlternative(lowCostSegment);
    for (int index = 0; index < alternative.length; ++index) {
      if (predicate.test(alternative[index])) {
        return alternative[index];
      }
    }
    return null;
  }

}

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
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
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
   * @return pasS1EndLabelRates created indicating 1 (100%) of flow is allocated to the new label on the final segment of s1
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
   * Relabel all eligible labels with non-zero flow on a turn on the vertex if it maintains the label for both entry and exit (where the exit is the provided segment). If found, we
   * relabel in the upstream direction until the label disappears or we arrive at the origin. We also update the eligible labels map for relabeled labels, because otherwise it is
   * no longer consistent with the bush
   * 
   * @param origin           to use
   * @param vertex           to use
   * @param exitEdgeSegment  to use
   * @param eligibleLabels   eligible labels that we consider for relabeling. Each label is eligible when it has a non-zero value. They keys are [entrysegment, entrylabel,
   *                         exitlabel] and value indicates non-zero flow into first s2 segment. Entries are updated based on executed relabelling!
   * @param compositionLabel to relabel if needed
   */
  private void relabelIfNotTerminating(final Bush origin, final DirectedVertex vertex, final EdgeSegment exitSegment, final MultiKeyMap<Object, Double> eligibleLabels) {
    if (!getDivergeVertex().hasEntryEdgeSegments()) {
      return;
    }

    var updatedLabels = new MultiKeyMap<Object, Double>();

    var iter = eligibleLabels.mapIterator();
    MultiKey<? extends Object> multiKey = null;
    while (iter.hasNext()) {
      iter.next();
      Double portion = iter.getValue();
      if (portion == null || !Precision.isPositive(portion)) {
        continue;
      }

      /* equal entry-exit labels -> relabel */
      multiKey = iter.getKey();
      var currEntryLabel = (BushFlowCompositionLabel) multiKey.getKey(1);
      if (!currEntryLabel.equals(multiKey.getKey(2))) {
        continue;
      }

      /* match found - create new label to relabel any turn flow across vertex with original labelling */
      var newLabel = origin.createFlowCompositionLabel();
      EdgeSegment entrySegment = (EdgeSegment) multiKey.getKey(0);

      /*
       * now we perform the actual relabelling after establishing the unique new label to use (which might have to relabel multiple entry segments, hence splitting the
       * identification off from the actual relabelling
       */
      origin.relabelFrom(entrySegment, currEntryLabel, exitSegment, currEntryLabel, newLabel);
      /* proceed upstream - relabel with new label recursively */
      relabelWhileNotTerminatingWith(origin, entrySegment.getUpstreamVertex(), entrySegment, currEntryLabel, newLabel);

      /* register updated label */
      updatedLabels.put(entrySegment, newLabel, currEntryLabel, portion);
      /* remove existing entry from map */
      iter.remove();
    }

    if (!updatedLabels.isEmpty()) {
      eligibleLabels.putAll(updatedLabels);
    }
  }

  /**
   * Relabel the given label on any turn on the vertex towards the given exit segment it maintains the same label. If found, we relabel in the upstream direction until the label
   * disappears or we arrive at the origin using the provided alternative label
   * 
   * @param origin          to use
   * @param vertex          to use
   * @param exitEdgeSegment to use
   * @param oldLabel        to relabel if needed
   * @param newLabel        to relabel with if needed
   */
  private void relabelWhileNotTerminatingWith(final Bush origin, final DirectedVertex vertex, final EdgeSegment exitSegment, final BushFlowCompositionLabel oldLabel,
      final BushFlowCompositionLabel newLabel) {
    for (EdgeSegment entrySegment : vertex.getEntryEdgeSegments()) {
      if (origin.containsEdgeSegment(entrySegment) && Precision.isPositive(origin.getTurnSendingFlow(entrySegment, oldLabel, exitSegment, oldLabel))) {
        /* match found - relabel across vertex */
        origin.relabel(entrySegment, oldLabel, exitSegment, oldLabel, newLabel);
        /* proceed upstream - relabel with new label recursively */
        relabelWhileNotTerminatingWith(origin, entrySegment.getUpstreamVertex(), entrySegment, oldLabel, newLabel);
      }
    }

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
   * <p>
   * If only a single label is used, the predecessor list will contain only a single entry which is the same as the key of the map
   * 
   * @param originBush     to do this for
   * @param lowCostSegment the segment to verify against
   * @return found matching composition labels as keys, where the values are an ordered list of encountered composite labels when traversing along the PAS (if any)
   */
  private Map<BushFlowCompositionLabel, List<BushFlowCompositionLabel>> determineMatchingLabels(final Bush originBush, boolean lowCostSegment) {
    Set<BushFlowCompositionLabel> edgeSegmentCompositionLabels = originBush.getFlowCompositionLabels(getLastEdgeSegment(lowCostSegment));
    Map<BushFlowCompositionLabel, List<BushFlowCompositionLabel>> pasCompositionLabels = new HashMap<BushFlowCompositionLabel, List<BushFlowCompositionLabel>>();
    if (edgeSegmentCompositionLabels == null || edgeSegmentCompositionLabels.isEmpty()) {
      return pasCompositionLabels;
    }

    EdgeSegment[] alternative = lowCostSegment ? s1 : s2;
    Iterator<BushFlowCompositionLabel> labelIter = edgeSegmentCompositionLabels.iterator();
    while (labelIter.hasNext()) {
      BushFlowCompositionLabel initialLabel = labelIter.next();
      BushFlowCompositionLabel currentLabel = initialLabel;
      List<BushFlowCompositionLabel> transitionLabels = new ArrayList<BushFlowCompositionLabel>();
      transitionLabels.add(initialLabel);

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

      pasCompositionLabels.put(initialLabel, transitionLabels);
    }
    return pasCompositionLabels;
  }

  /**
   * portion of total shifted flow allocated to each used turn+entrylabel+exitlabel combination based on proportional distribution by dividing the turnlabeled flow by the total
   * accepted turn flow into s2 for the given exit label.
   * <p>
   * The key for the MultiKeyMap is [entry segment, entry-label, exit-label] while the value will hold the portion
   *
   * @param origin                                  at hand
   * @param pasS2UsedStartLabels                    the labels that have non-zero flow along S2
   * @param flowAcceptanceFactors                   to use
   * @param s2DivergeTurnLabelProportionsToPopulate to populate, only entries for used labels will be present
   * @return number of used entry segments zero when PAS diverges at an origin
   */
  private int populateS2DivergeProportionsByTurnLabels(Bush origin, Set<BushFlowCompositionLabel> pasS2UsedStartLabels, final double[] flowAcceptanceFactors,
      MultiKeyMap<Object, Double> s2DivergeTurnLabelProportionsToPopulate) {

    EdgeSegment firstS2EdgeSegment = getFirstEdgeSegment(false /* high cost segment */);

    Set<EdgeSegment> usedEntrySegments = new HashSet<>();
    Map<BushFlowCompositionLabel, Double> s2ExitLabelTotalFlows = new HashMap<>();
    for (EdgeSegment entrySegment : getDivergeVertex().getEntryEdgeSegments()) {
      if (origin.containsEdgeSegment(entrySegment)) {
        Set<BushFlowCompositionLabel> entryLabels = origin.getFlowCompositionLabels(entrySegment);
        for (BushFlowCompositionLabel entryLabel : entryLabels) {
          for (BushFlowCompositionLabel exitLabel : pasS2UsedStartLabels) {
            double turnSendingFlow = origin.getTurnSendingFlow(entrySegment, entryLabel, firstS2EdgeSegment, exitLabel);
            if (Precision.isPositive(turnSendingFlow)) {
              double turnAcceptedFlow = turnSendingFlow * flowAcceptanceFactors[(int) entrySegment.getId()];
              usedEntrySegments.add(entrySegment);

              Double currentFlow = s2DivergeTurnLabelProportionsToPopulate.get(entrySegment, entryLabel, exitLabel);
              if (currentFlow == null) {
                currentFlow = 0.0;
              }
              s2DivergeTurnLabelProportionsToPopulate.put(entrySegment, entryLabel, exitLabel, currentFlow + turnAcceptedFlow);
              s2ExitLabelTotalFlows.put(exitLabel, s2ExitLabelTotalFlows.getOrDefault(exitLabel, 0.0) + turnAcceptedFlow);
            }
          }
        }
      }
    }

    /* determine proportional portion of each non-zero contributing turn-entrylabel-exitlable identified towards PAS s2 initial segment */
    for (BushFlowCompositionLabel exitLabel : pasS2UsedStartLabels) {
      double s2ExitLabelTotalFlow = s2ExitLabelTotalFlows.get(exitLabel);
      for (EdgeSegment entrySegment : getDivergeVertex().getEntryEdgeSegments()) {
        if (origin.containsEdgeSegment(entrySegment)) {
          Set<BushFlowCompositionLabel> entryLabels = origin.getFlowCompositionLabels(entrySegment);
          for (BushFlowCompositionLabel entryLabel : entryLabels) {
            Double turnSendingFlow = s2DivergeTurnLabelProportionsToPopulate.get(entrySegment, entryLabel, exitLabel);
            if (turnSendingFlow != null && Precision.isPositive(turnSendingFlow)) {
              s2DivergeTurnLabelProportionsToPopulate.put(entrySegment, entryLabel, exitLabel, turnSendingFlow / s2ExitLabelTotalFlow);
            }
          }
        }
      }
    }

    return usedEntrySegments.size();
  }

  /**
   * Helper method that determine which label was used at the start of the segment given the final label and any predecssors known when traversing along the PAS towards its diverge
   * 
   * @param reverseOrderUsedLabels known to use
   * @return the first label used in the segment, which is the last label in the predecessor list
   */
  private BushFlowCompositionLabel extractUsedStartLabel(List<BushFlowCompositionLabel> reverseOrderUsedLabels) {
    if (reverseOrderUsedLabels.isEmpty()) {
      LOGGER.severe("No labels present in provided reverse order used label list, unable to extract start label");
    }
    return reverseOrderUsedLabels.get(reverseOrderUsedLabels.size() - 1);
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
    for (Entry<BushFlowCompositionLabel, List<BushFlowCompositionLabel>> usedLabelPrecessors : pasAlternativeEndFlowCompositionLabels.entrySet()) {
      List<BushFlowCompositionLabel> compositePredecessors = usedLabelPrecessors.getValue();
      usedStartLabels.add(extractUsedStartLabel(compositePredecessors));
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
      /* key: [exitSegment, exitLabel] */
      MultiKeyMap<Object, Double> splittingRates = origin.getSplittingRates(lastS2Segment, finalSegmentLabel);
      int index = 0;
      for (EdgeSegment exitSegment : getMergeVertex().getExitEdgeSegments()) {
        if (origin.containsEdgeSegment(exitSegment)) {
          Set<BushFlowCompositionLabel> exitLabels = origin.getFlowCompositionLabels(exitSegment);
          for (BushFlowCompositionLabel exitLabel : exitLabels) {

            Double labeledSplittingRate = splittingRates.get(exitSegment, exitLabel);
            if (labeledSplittingRate == null || !Precision.isPositive(labeledSplittingRate)) {
              continue;
            }

            /* remove flow for s2 */
            double s2FlowShift = s2FinalLabeledFlowShift * labeledSplittingRate;
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
          }
        }
        ++index;
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
          double splittingRate = exitLabelSplittingRates[index];
          if (Precision.isPositive(splittingRate)) {
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
   * <p>
   * s2DivergeProportionsByTurnLabels has the following expected multikey[entrysegment,entrylabel,exitlabel]
   * 
   * @param origin                           to use
   * @param startSegmentLabel                flow composition label on s2 initial segment to apply
   * @param s2StartLabeledFlowShift          the flow shift applied to the first s2 segment
   * @param s2DivergeProportionsByTurnLabels portion to be shifted flow attributed to each used turn entry-exitlabel towards S2 initial segment
   * @param flowAcceptanceFactors            to use
   */
  private void executeBushLabeledS2FlowShiftStartDiverge(Bush origin, BushFlowCompositionLabel startSegmentLabel, double s2StartLabeledFlowShift,
      MultiKeyMap<Object, Double> s2DivergeProportionsByTurnLabels, final double[] flowAcceptanceFactors) {

    EdgeSegment firstS2Segment = getFirstEdgeSegment(false /* high cost */);

    for (EdgeSegment entrySegment : getDivergeVertex().getEntryEdgeSegments()) {
      if (origin.containsEdgeSegment(entrySegment)) {
        Set<BushFlowCompositionLabel> entryLabels = origin.getFlowCompositionLabels(entrySegment);
        for (BushFlowCompositionLabel entryLabel : entryLabels) {
          Double portion = s2DivergeProportionsByTurnLabels.get(entrySegment, entryLabel, startSegmentLabel);
          if (portion == null) {
            continue;
          }

          double existingTotalTurnLabeledSendingFlow = origin.getTurnSendingFlow(entrySegment, entryLabel, firstS2Segment, startSegmentLabel);
          if (!Precision.isPositive(existingTotalTurnLabeledSendingFlow)) {
            LOGGER.severe("Expected available turn sending flow for given label combination, found none, skip flow shift at PAS s2 diverge");
            continue;
          }

          /* convert back to sending flow as alpha<1 increases sending flow on entry segment compared to the sending flow component on the first s2 segment */
          double s2DivergeEntryLabeledFlowShift = s2StartLabeledFlowShift * portion * (1 / flowAcceptanceFactors[(int) entrySegment.getId()]);

          if (!Precision.isPositive(existingTotalTurnLabeledSendingFlow + s2DivergeEntryLabeledFlowShift)) {
            /* no remaining flow at all after flow shift, remove turn from bush entirely */
            origin.removeTurn(entrySegment, firstS2Segment);
          } else {
            origin.addTurnSendingFlow(entrySegment, entryLabel, firstS2Segment, startSegmentLabel, s2DivergeEntryLabeledFlowShift);
          }
        }
      }
    }
  }

  /**
   * Perform the flow shift through the start diverge vertex of the PASs low cost segment for the given origin bush flow composition. We use the same proportions that were applied
   * in the S2 diverge update via the divergeProportionsByTurnLabels. These portions are based on a proportional distribution for each used entrysegment-entrylabel-exitlabel
   * contribution compared to the total exit label flow.
   * <p>
   * the multikey of the multikeymap is expected to be [entry segment,entry label,exit label] while the value reflects the to be applied portion
   * 
   * @param origin                         to use
   * @param startSegmentLabel              flow composition label on s1 initial segment to apply
   * @param s1StartLabeledFlowShift        the flow shift applied to the first s1 segment for the label
   * @param divergeProportionsByTurnLabels portions to apply for each entrysegment-entrylabel-exitlabel given the to be shifted flow for a given exitlabel towards S1 initial
   *                                       segment
   * @param flowAcceptanceFactors          to use
   */
  private void executeBushLabeledS1FlowShiftStartDiverge(Bush origin, BushFlowCompositionLabel startSegmentLabel, double s1StartLabeledFlowShift,
      MultiKeyMap<Object, Double> divergeProportionsByTurnLabels, final double[] flowAcceptanceFactors) {

    EdgeSegment firstS1Segment = getFirstEdgeSegment(true /* low cost */);

    for (EdgeSegment entrySegment : getDivergeVertex().getEntryEdgeSegments()) {
      if (origin.containsEdgeSegment(entrySegment)) {
        Set<BushFlowCompositionLabel> entryLabels = origin.getFlowCompositionLabels(entrySegment);
        for (BushFlowCompositionLabel entryLabel : entryLabels) {
          Double portion = divergeProportionsByTurnLabels.get(entrySegment, entryLabel, startSegmentLabel);
          if (portion == null) {
            continue;
          }

          /* convert back to sending flow as alpha<1 increases sending flow on entry segment compared to the sending flow component on the first s1 segment */
          double s1DivergeEntryLabeledFlowShift = s1StartLabeledFlowShift * portion * (1 / flowAcceptanceFactors[(int) entrySegment.getId()]);
          if (Precision.isNegative(s1DivergeEntryLabeledFlowShift)) {
            LOGGER.severe("Expected non-negative shift on s1 turn for given label combination, skip flow shift at PAS s1 diverge");
            continue;
          }
          origin.addTurnSendingFlow(entrySegment, entryLabel, firstS1Segment, startSegmentLabel, s1DivergeEntryLabeledFlowShift);
        }
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
    EdgeSegment firstS2Segment = getFirstEdgeSegment(false /* high cost */);

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

      /*
       * determine the proportion attributed to each used turn-entrylabel-exitlabel combination where the exit label is used on the s2 alternative. The found portions are
       * proportional to contribution of each combination to the total sending flow on the s2-label flow on its initial link segment. Multikey: [entrysegment, entrylabel,
       * exitlabel]
       */
      int numberOfUsedEntrySegments = 0;
      var s2DivergeProportionsByTurnLabels = new MultiKeyMap<Object, Double>();
      if (getDivergeVertex().hasEntryEdgeSegments()) {
        var pasS2UsedStartLabels = extractUsedStartLabels(pasS2EndFlowCompositionLabels);
        numberOfUsedEntrySegments = populateS2DivergeProportionsByTurnLabels(origin, pasS2UsedStartLabels, flowAcceptanceFactors, s2DivergeProportionsByTurnLabels);
      }

      /* exit turn flows out of s2 by used exit label - to be populated in s2 merge update for use by s1 update */
      var s2MergeExitShiftedSendingFlows = new HashMap<BushFlowCompositionLabel, double[]>();

      /* LABEL SPECIFIC PREP - END */

      /*
       * ------------------------------------------------- S2 FLOW SHIFT ----------------------------------------------------------------------------------------------------------
       * Update S2 by shifting flow proportionally along encountered flow compositions matching with the PAS/origin/alternative
       */
      Map<BushFlowCompositionLabel, Double> pasS2EndLabelRates = origin.determineProportionalFlowCompositionRates(lastS2Segment, pasS2EndFlowCompositionLabels.keySet());
      for (Entry<BushFlowCompositionLabel, Double> mergeLabelEntry : pasS2EndLabelRates.entrySet()) {

        var finalSegmentLabel = mergeLabelEntry.getKey();
        var reverseOrderS2Labels = pasS2EndFlowCompositionLabels.get(finalSegmentLabel);
        var initialSegmentLabel = extractUsedStartLabel(reverseOrderS2Labels);

        /* shift portion of flow attributed to composition label traversing s2 */
        double s2StartLabeledFlowShift = mergeLabelEntry.getValue() * bushFlowShift;
        double s2FinalLabeledFlowShift = executeBushLabeledAlternativeFlowShift(origin, reverseOrderS2Labels, -s2StartLabeledFlowShift, s2, flowAcceptanceFactors, false);

        /* shift flow across final merge for S2 */
        executeBushLabeledS2FlowShiftEndMerge(origin, finalSegmentLabel, s2FinalLabeledFlowShift, s2MergeExitShiftedSendingFlows);

        /* shift flows across starting diverge before entering S2 using reciprocal of flow acceptance factor */
        if (numberOfUsedEntrySegments >= 1) {
          executeBushLabeledS2FlowShiftStartDiverge(origin, initialSegmentLabel, -s2StartLabeledFlowShift, s2DivergeProportionsByTurnLabels, flowAcceptanceFactors);
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
      Map<BushFlowCompositionLabel, List<BushFlowCompositionLabel>> pasS1FlowCompositionLabels = determineMatchingLabels(origin, true /* low cost segment */);
      final boolean s1SegmentNotUsedYet = pasS1FlowCompositionLabels.isEmpty();
      Map<BushFlowCompositionLabel, Double> pasS1EndLabelRates = null;
      if (s1SegmentNotUsedYet) {
        pasS1EndLabelRates = initialiseS1Labelling(origin, pasS1FlowCompositionLabels);

        /*
         * Flow into s2 that presently maintains its label at the initial diverge must be relabeled in the upstream direction because due to the flow shift it now splits off flow
         * into s1. so relabel all those "non-terminating" labels first
         */
        relabelIfNotTerminating(origin, getDivergeVertex(), firstS2Segment, s2DivergeProportionsByTurnLabels);

      } else {
        pasS1EndLabelRates = origin.determineProportionalFlowCompositionRates(lastS1Segment, pasS1FlowCompositionLabels.keySet());
      }
      for (Entry<BushFlowCompositionLabel, Double> labelEntry : pasS1EndLabelRates.entrySet()) {
        var finalSegmentLabel = labelEntry.getKey();
        var reverseOrderS1Labels = pasS1FlowCompositionLabels.get(finalSegmentLabel);
        var initialSegmentLabel = extractUsedStartLabel(reverseOrderS1Labels);

        /* portion of flow attributed to composition label traversing s1 */
        double s1StartLabeledFlowShift = labelEntry.getValue() * bushFlowShift;
        double s1FinalLabeledFlowShift = executeBushLabeledAlternativeFlowShift(origin, reverseOrderS1Labels, s1StartLabeledFlowShift, s1, flowAcceptanceFactors,
            s1SegmentNotUsedYet);

        /* shift flow across final merge for S1 based on findings in s2 */
        executeBushLabeledS1FlowShiftEndMerge(origin, finalSegmentLabel, s1FinalLabeledFlowShift, s2MergeExitShiftedSendingFlows);

        if (numberOfUsedEntrySegments >= 1) {
          /* shift flow across initial diverge into S1 based on findings in s2 */
          executeBushLabeledS1FlowShiftStartDiverge(origin, initialSegmentLabel, s1StartLabeledFlowShift, s2DivergeProportionsByTurnLabels, flowAcceptanceFactors);
        }
      }
    }

    /* remove irrelevant bushes */
    removeOrigins(originsWithoutRemainingPasFlow);

    return true;
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

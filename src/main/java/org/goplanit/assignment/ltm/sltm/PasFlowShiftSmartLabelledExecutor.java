package org.goplanit.assignment.ltm.sltm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.collections4.iterators.ReverseListIterator;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.utils.arrays.ArrayUtils;
import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.CollectionUtils;

/**
 * Functionality to conduct a PAS flow shift by means of the smart labelling technique where we try to minimise the number of labels required across each bush to reduce both
 * computational and memory burden.
 * 
 * @author markr
 *
 */
public class PasFlowShiftSmartLabelledExecutor extends PasFlowShiftExecutor {

  /**
   * Logger to use
   */
  private final static Logger LOGGER = Logger.getLogger(PasFlowShiftSmartLabelledExecutor.class.getCanonicalName());

  /** the label chains (list of list map value), per origin (map key), in reverse order (end to start) that traverse S2 */
  protected final Map<Bush, List<LinkedList<BushFlowLabel>>> s2ReverseLabelChains;

  /** the label chains (list of list map value), per origin (map key) in reverse order (end to start) that traverse S1 */
  protected final Map<Bush, List<LinkedList<BushFlowLabel>>> s1ReverseLabelChains;

  /**
   * The first time a PAS is used for flow shifting, its S1 segment has no labels yet along the PAS. Therefore we create a new label unique to the S1 alternative and populate the
   * s1MatchingLabelsToFill map so it can be used in the correct format for flow shifting. Also, we create the related pasS1EndLabelRates indicating 100% of the flow on the PAS S1
   * segment for this bush is allocated to this new label at present. Again, to directly be able to use it in the flow shifting format that we use
   * 
   * @param origin                  to initialise labelling for
   * @param s1UsedLabelChainsToFill map to populate with the new label
   * @return pasS1EndLabelRates created indicating 1 (100%) of flow is allocated to the new label on the final segment of s1
   */
  private TreeMap<BushFlowLabel, Double> initialiseS1Labelling(final Bush origin, List<LinkedList<BushFlowLabel>> s1UsedLabelChainsToFill) {
    var pasS1Label = BushFlowLabel.create(origin.bushGroupingToken);
    var s1LabelChain = new LinkedList<BushFlowLabel>();
    s1LabelChain.addFirst(pasS1Label);
    s1UsedLabelChainsToFill.add(s1LabelChain);

    var pasS1EndLabelRates = new TreeMap<BushFlowLabel, Double>();
    pasS1EndLabelRates.put(pasS1Label, 1.0);

    return pasS1EndLabelRates;
  }

  /**
   * Determine all label chains that represent flow that is fully overlapping with the provided subpath (assumed present on the bush). The chain represents the various connected
   * labels in reverse order. These composite labels are provided as a list and the number of lists indicates the number of unique chains matching the PAS alternative
   * 
   * @param origin
   * 
   * @param subPath to do this for
   * @return found matching composition labels as a reverse ordered lists of encountered composite labels when traversing along the PAS (if any)
   */
  private List<LinkedList<BushFlowLabel>> determineUsedLabelChains(Bush origin, final EdgeSegment[] subPath) {
    var edgeSegmentCompositionLabels = origin.getFlowCompositionLabels(subPath[0]);

    var pasCompositionLabels = new ArrayList<LinkedList<BushFlowLabel>>();
    if (edgeSegmentCompositionLabels == null || edgeSegmentCompositionLabels.isEmpty()) {
      return pasCompositionLabels;
    }

    var labelIter = edgeSegmentCompositionLabels.iterator();
    while (labelIter.hasNext()) {
      BushFlowLabel firstSegmentLabel = labelIter.next();
      BushFlowLabel currentLabel = firstSegmentLabel;
      LinkedList<BushFlowLabel> transitionLabels = new LinkedList<BushFlowLabel>();
      transitionLabels.add(firstSegmentLabel);

      EdgeSegment currentSegment = subPath[0];
      EdgeSegment succeedingSegment = null;
      for (int index = 1; index < subPath.length; ++index) {
        succeedingSegment = subPath[index];
        if (!origin.containsTurnSendingFlow(currentSegment, currentLabel, succeedingSegment, currentLabel)) {
          /* label transition or no match */
          BushFlowLabel transitionLabel = null;
          var potentialLabelTransitions = origin.getFlowCompositionLabels(succeedingSegment);
          if (potentialLabelTransitions != null) {
            for (var potentialLabel : potentialLabelTransitions) {
              if (origin.containsTurnSendingFlow(currentSegment, currentLabel, succeedingSegment, potentialLabel)) {
                transitionLabel = potentialLabel;
                transitionLabels.addFirst(transitionLabel);
              }
            }
          }
          if (transitionLabel == null) {
            /* no match - remove the original label we started with */
            transitionLabels = null;
            break;
          }
          /* transition - update label representing composite flow that contains label under investigation */
          currentLabel = transitionLabel;
        }
        currentSegment = succeedingSegment;
      }

      if (!CollectionUtils.nullOrEmpty(transitionLabels)) {
        pasCompositionLabels.add(transitionLabels);
      }
    }
    return pasCompositionLabels;
  }

  /**
   * Determine the portion of total shifted flow allocated to each used turn+entrylabel combination based on proportional distribution by dividing the turnlabeled flow by the total
   * accepted turn flow into s2. Note that we scale the labelled flows on the initial S2 segment based on the amount of flow that reaches the end of the PAS as we should not take
   * consider flow that splits off halfway since this flow can never be shifted and therefore should not be considered part of the PAS.
   * <p>
   * The key for the MultiKeyMap is [entry segment, entry-label] while the value will hold the portion attributed to the S2 initial segment
   *
   * @param origin                at hand
   * @param pasS2UsedLabelChains  the labels that have non-zero flow along S2 where the value provides the composite labels encountered in upstream direction
   * @param pasS2EndLabelRates    Portion of the labelled sending flow at the first segment of s2 that follows s2 in its entirety, i.e., does not split off. Use this to scale back
   *                              to labelled sending flow to the PAS compatible sending flow
   * @param flowAcceptanceFactors to use
   * @return s2DivergeTurnLabelProportionsToPopulate to populate, only entries for used labels will be present
   */
  private MultiKeyMap<Object, Double> createS2DivergeProportionsByTurnLabels(Bush origin, List<LinkedList<BushFlowLabel>> pasS2UsedLabelChains,
      Map<BushFlowLabel, Double> pasS2EndLabelRates, final double[] flowAcceptanceFactors) {

    var firstS2EdgeSegment = pas.getFirstEdgeSegment(false /* high cost segment */);
    var s2DivergeTurnLabelProportionsToPopulate = new MultiKeyMap<Object, Double>();

    double s2InitialSegmentTotalFlow = 0;
    for (var entrySegment : pas.getDivergeVertex().getEntryEdgeSegments()) {
      if (origin.containsEdgeSegment(entrySegment)) {
        double alpha = flowAcceptanceFactors[(int) entrySegment.getId()];
        var entryLabels = origin.getFlowCompositionLabels(entrySegment);
        for (var entryLabel : entryLabels) {
          for (var usedLabelPredecessors : pasS2UsedLabelChains) {
            double turnSendingFlow = origin.getTurnSendingFlow(entrySegment, entryLabel, firstS2EdgeSegment, usedLabelPredecessors.getLast());
            if (!Precision.positive(turnSendingFlow)) {
              continue;
            }

            /* scale back by identifying the portion that makes it to the end of the S2 alternative */
            var s2EndLabel = usedLabelPredecessors.getFirst();
            double s2CompatiblePortion = pasS2EndLabelRates.get(s2EndLabel);
            double s2CompatibleTurnAcceptedFlow = turnSendingFlow * s2CompatiblePortion * alpha;

            if (!Precision.positive(s2CompatibleTurnAcceptedFlow)) {
              continue;
            }

            Double currentFlow = s2DivergeTurnLabelProportionsToPopulate.get(entrySegment, entryLabel);
            if (currentFlow == null) {
              currentFlow = 0.0;
            }
            s2DivergeTurnLabelProportionsToPopulate.put(entrySegment, entryLabel, currentFlow + s2CompatibleTurnAcceptedFlow);
            s2InitialSegmentTotalFlow += s2CompatibleTurnAcceptedFlow;
          }
        }
      }
    }

    /* determine proportional portion of each non-zero contributing turn-entrylabel- identified towards PAS s2 initial segment */
    var iter = s2DivergeTurnLabelProportionsToPopulate.mapIterator();
    while (iter.hasNext()) {
      iter.next();
      iter.setValue(iter.getValue() / s2InitialSegmentTotalFlow);
    }

    return s2DivergeTurnLabelProportionsToPopulate;
  }

  /**
   * Relabel all eligible labels with non-zero flow on a turn on the vertex if it maintains the label for both entry and exit (where the exit is the provided segment). If found, we
   * relabel in the upstream direction until the label disappears or we arrive at the origin. We also update the eligible labels map for relabeled labels, because otherwise it is
   * no longer consistent with the bush
   * 
   * @param origin           to use
   * @param vertex           to use
   * @param exitEdgeSegment  to use
   * @param eligibleLabels   eligible labels that we consider for relabeling. Each label is eligible when it has a non-zero flow when adopting its entry label as its exit label.
   *                         They keys are [entrysegment, entrylabel] and value indicates non-zero flow into first s2 segment. Entries are updated based on executed relabelling!
   * @param compositionLabel to relabel if needed
   */
  private void relabelIfNotTerminating(final Bush origin, final DirectedVertex vertex, final EdgeSegment exitSegment, final MultiKeyMap<Object, Double> eligibleLabels) {
    if (!pas.getDivergeVertex().hasEntryEdgeSegments()) {
      return;
    }

    var updatedLabels = new MultiKeyMap<Object, Double>();

    var iter = eligibleLabels.mapIterator();
    MultiKey<? extends Object> multiKey = null;
    while (iter.hasNext()) {
      iter.next();
      Double portion = iter.getValue();
      if (portion == null || !Precision.positive(portion)) {
        continue;
      }

      /* equal entry-exit labels -> relabel */
      multiKey = iter.getKey();
      EdgeSegment entrySegment = (EdgeSegment) multiKey.getKey(0);
      var currEntryLabel = (BushFlowLabel) multiKey.getKey(1);

      if (!Precision.positive(origin.getTurnSendingFlow(entrySegment, currEntryLabel, exitSegment, currEntryLabel))) {
        continue;
      }

      /* match found - create new label to relabel any turn flow across vertex with original labelling */
      var newLabel = BushFlowLabel.create(origin.bushGroupingToken);

      /*
       * now we perform the actual relabelling after establishing the unique new label to use (which might have to relabel multiple entry segments, hence splitting the
       * identification off from the actual relabelling
       */
      origin.relabelFrom(entrySegment, currEntryLabel, exitSegment, currEntryLabel, newLabel);
      /* proceed upstream - relabel with new label recursively */
      relabelWhileNotTerminatingWith(origin, entrySegment.getUpstreamVertex(), entrySegment, currEntryLabel, newLabel);

      /* register updated label */
      updatedLabels.put(entrySegment, newLabel, portion);
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
  private void relabelWhileNotTerminatingWith(final Bush origin, final DirectedVertex vertex, final EdgeSegment exitSegment, final BushFlowLabel oldLabel,
      final BushFlowLabel newLabel) {
    for (var entrySegment : vertex.getEntryEdgeSegments()) {
      if (origin.containsEdgeSegment(entrySegment)) {

        if (Precision.positive(origin.getTurnSendingFlow(entrySegment, oldLabel, exitSegment, oldLabel))) {
          /* match found - relabel across vertex */
          origin.relabel(entrySegment, oldLabel, exitSegment, oldLabel, newLabel);
          /* proceed upstream - relabel with new label recursively */
          relabelWhileNotTerminatingWith(origin, entrySegment.getUpstreamVertex(), entrySegment, oldLabel, newLabel);
        } else {

          /* terminating, so update final flow from other label to old label with new label */
          var entryLabels = origin.getFlowCompositionLabels(entrySegment);
          for (var entryLabel : entryLabels) {
            if (Precision.positive(origin.getTurnSendingFlow(entrySegment, entryLabel, exitSegment, oldLabel))) {
              origin.relabelTo(entrySegment, entryLabel, exitSegment, oldLabel, newLabel);
            }
          }
        }
      }
    }
  }

  /**
   * Perform the flow shift through the end merge vertex of the PASs high cost segment for the given origin bush flow composition
   * 
   * @param origin                           to use
   * @param finalSegmentLabel                flow composition label on s2 final segment to apply
   * @param s2FinalLabeledFlowShift          the flow shift applied so far up to the final merge
   * @param exitShiftedSendingFlowToPopulate map to populate with the found exit segment flows (values) by used exit label (key)
   */
  private void executeBushLabeledS2FlowShiftEndMerge(Bush origin, BushFlowLabel finalSegmentLabel, double s2FinalLabeledFlowShift,
      Map<BushFlowLabel, double[]> exitShiftedSendingFlowToPopulate) {

    var lastS2Segment = pas.getLastEdgeSegment(false /* high cost */);

    /* remove shifted flows through final merge towards exit segments proportionally, to later add to s1 turns through merge */
    if (pas.getMergeVertex().hasExitEdgeSegments()) {
      /* key: [exitSegment, exitLabel] */
      MultiKeyMap<Object, Double> splittingRates = origin.getSplittingRates(lastS2Segment, finalSegmentLabel);
      int index = 0;
      for (var exitSegment : pas.getMergeVertex().getExitEdgeSegments()) {
        if (origin.containsEdgeSegment(exitSegment)) {
          var exitLabels = origin.getFlowCompositionLabels(exitSegment);
          for (var exitLabel : exitLabels) {

            Double labeledSplittingRate = splittingRates.get(exitSegment, exitLabel);
            if (labeledSplittingRate == null || !Precision.positive(labeledSplittingRate)) {
              continue;
            }

            /* remove flow for s2 */
            double s2FlowShift = s2FinalLabeledFlowShift * labeledSplittingRate;
            boolean positiveLabelledFlowRemaining = origin.addTurnSendingFlow(lastS2Segment, finalSegmentLabel, exitSegment, exitLabel, s2FlowShift);
            if (!positiveLabelledFlowRemaining && !Precision.positive(origin.getTurnSendingFlow(lastS2Segment, exitSegment))) {
              /* no remaining flow at all on turn after flow shift, remove turn from bush entirely */
              origin.removeTurn(lastS2Segment, exitSegment);
            }

            /* track so we can attribute it to s1 segment later */
            double[] exitLabelExitSegmentShiftedSendingFlow = exitShiftedSendingFlowToPopulate.get(exitLabel);
            if (exitLabelExitSegmentShiftedSendingFlow == null) {
              exitLabelExitSegmentShiftedSendingFlow = new double[this.pasMergeVertexNumExitSegments];
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
  private void executeBushLabeledS1FlowShiftEndMerge(Bush origin, BushFlowLabel finalSegmentLabel, double s1FinalLabeledFlowShift,
      Map<BushFlowLabel, double[]> usedLabelSplittingRates) {

    EdgeSegment lastS1Segment = pas.getLastEdgeSegment(true /* low cost */);

    /* add shifted flows through final merge towards exit segments proportionally based on labeled exit usage */
    if (pas.getMergeVertex().hasExitEdgeSegments()) {
      for (var entry : usedLabelSplittingRates.entrySet()) {
        BushFlowLabel exitLabel = entry.getKey();
        double[] exitLabelSplittingRates = entry.getValue();
        int index = 0;
        for (var exitSegment : pas.getMergeVertex().getExitEdgeSegments()) {
          double splittingRate = exitLabelSplittingRates[index];
          if (Precision.positive(splittingRate)) {
            /* add flow for s1 */
            double s1FlowShift = s1FinalLabeledFlowShift * splittingRate;
            boolean positiveLabelledFlowRemaining = origin.addTurnSendingFlow(lastS1Segment, finalSegmentLabel, exitSegment, exitLabel, s1FlowShift);
            if (!positiveLabelledFlowRemaining && !Precision.positive(origin.getTurnSendingFlow(lastS1Segment, exitSegment))) {
              /* no remaining flow at all on turn after flow shift, remove turn from bush entirely */
              origin.removeTurn(lastS1Segment, exitSegment);
            }
          }
          ++index;
        }
      }
    }
  }

  /**
   * Perform the flow shift through the start diverge vertex of the PASs high cost segment for the given origin bush flow composition
   * <p>
   * s2DivergeProportionsByTurnLabels has the following expected multikey[entrysegment,entrylabel]
   * 
   * @param origin                           to use
   * @param startSegmentLabel                flow composition label on s2 initial segment to apply
   * @param s2StartLabeledFlowShift          the flow shift applied to the first s2 segment
   * @param s2DivergeProportionsByTurnLabels portion to be shifted flow attributed to each used turn entry-exitlabel towards S2 initial segment
   * @param flowAcceptanceFactors            to use
   */
  private void executeBushLabeledS2FlowShiftStartDiverge(Bush origin, BushFlowLabel startSegmentLabel, double s2StartLabeledFlowShift,
      MultiKeyMap<Object, Double> s2DivergeProportionsByTurnLabels, final double[] flowAcceptanceFactors) {

    EdgeSegment firstS2Segment = pas.getFirstEdgeSegment(false /* high cost */);

    for (var entrySegment : pas.getDivergeVertex().getEntryEdgeSegments()) {
      if (origin.containsEdgeSegment(entrySegment)) {
        var entryLabels = origin.getFlowCompositionLabels(entrySegment);
        for (var entryLabel : entryLabels) {
          Double portion = s2DivergeProportionsByTurnLabels.get(entrySegment, entryLabel);
          if (portion == null) {
            continue;
          }

          double existingTotalTurnLabeledSendingFlow = origin.getTurnSendingFlow(entrySegment, entryLabel, firstS2Segment, startSegmentLabel);
          if (!Precision.positive(existingTotalTurnLabeledSendingFlow)) {
            LOGGER.severe("Expected available turn sending flow for given label combination, found none, skip flow shift at PAS s2 diverge");
            continue;
          }

          /* convert back to sending flow as alpha<1 increases sending flow on entry segment compared to the sending flow component on the first s2 segment */
          double s2DivergeEntryLabeledFlowShift = s2StartLabeledFlowShift * portion * (1 / flowAcceptanceFactors[(int) entrySegment.getId()]);

          boolean positiveLabelledFlowRemaining = origin.addTurnSendingFlow(entrySegment, entryLabel, firstS2Segment, startSegmentLabel, s2DivergeEntryLabeledFlowShift);
          if (!positiveLabelledFlowRemaining && !Precision.positive(origin.getTurnSendingFlow(entrySegment, firstS2Segment))) {
            /* no remaining flow at all on turn after flow shift, remove turn from bush entirely */
            origin.removeTurn(entrySegment, firstS2Segment);
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
   * the multikey of the multikeymap is expected to be [entry segment,entry label] while the value reflects the to be applied portion
   * 
   * @param origin                         to use
   * @param startSegmentLabel              flow composition label on s1 initial segment to apply
   * @param s1StartLabeledFlowShift        the flow shift applied to the first s1 segment for the label
   * @param divergeProportionsByTurnLabels portions to apply for each entrysegment-entrylabel given the to be shifted flow for a given exitlabel towards S1 initial segment
   * @param flowAcceptanceFactors          to use
   */
  private void executeBushLabeledS1FlowShiftStartDiverge(Bush origin, BushFlowLabel startSegmentLabel, double s1StartLabeledFlowShift,
      MultiKeyMap<Object, Double> divergeProportionsByTurnLabels, final double[] flowAcceptanceFactors) {

    EdgeSegment firstS1Segment = pas.getFirstEdgeSegment(true /* low cost */);

    for (var entrySegment : pas.getDivergeVertex().getEntryEdgeSegments()) {
      if (origin.containsEdgeSegment(entrySegment)) {
        var entryLabels = origin.getFlowCompositionLabels(entrySegment);
        for (var entryLabel : entryLabels) {
          Double portion = divergeProportionsByTurnLabels.get(entrySegment, entryLabel);
          if (portion == null) {
            continue;
          }

          /* convert back to sending flow as alpha<1 increases sending flow on entry segment compared to the sending flow component on the first s1 segment */
          double s1DivergeEntryLabeledFlowShift = s1StartLabeledFlowShift * portion * (1 / flowAcceptanceFactors[(int) entrySegment.getId()]);
          if (Precision.negative(s1DivergeEntryLabeledFlowShift)) {
            LOGGER.severe("Expected non-negative shift on s1 turn for given label combination, skip flow shift at PAS s1 diverge");
            continue;
          }

          boolean positiveLabelledFlowRemaining = origin.addTurnSendingFlow(entrySegment, entryLabel, firstS1Segment, startSegmentLabel, s1DivergeEntryLabeledFlowShift);
          if (!positiveLabelledFlowRemaining && !Precision.positive(origin.getTurnSendingFlow(entrySegment, firstS1Segment))) {
            /* no remaining flow at all on turn after flow shift, remove turn from bush entirely */
            origin.removeTurn(entrySegment, firstS1Segment);
          }
        }
      }
    }
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
  private double executeBushLabeledAlternativeFlowShift(Bush origin, List<BushFlowLabel> reverseOrderCompositionLabels, double flowShiftPcuH, EdgeSegment[] pasSegment,
      double[] flowAcceptanceFactors, boolean forceInitialLabel) {
    int index = 0;
    EdgeSegment currentSegment = null;
    var nextSegment = pasSegment[index];

    var reverseLabelIter = new ReverseListIterator<BushFlowLabel>(reverseOrderCompositionLabels);
    var currCompositionLabel = reverseLabelIter.next();
    var nextCompositionLabel = currCompositionLabel;
    while (++index < pasSegment.length) {
      currentSegment = nextSegment;
      nextSegment = pasSegment[index];

      double turnSendingFlow = origin.getTurnSendingFlow(currentSegment, currCompositionLabel, nextSegment, currCompositionLabel);
      if (!forceInitialLabel && !Precision.positive(turnSendingFlow)) {
        /* composition splits/ends, identify if next label it splits off in is valid/available */
        nextCompositionLabel = reverseLabelIter.hasNext() ? reverseLabelIter.next() : null;
        if (nextCompositionLabel != null && origin.containsTurnSendingFlow(currentSegment, currCompositionLabel, nextSegment, nextCompositionLabel)) {
          turnSendingFlow = origin.getTurnSendingFlow(currentSegment, currCompositionLabel, nextSegment, nextCompositionLabel);
        } else {
          LOGGER.warning("Unable to trace PAS s2 flow through alternative with the given flow composition chain, aborting flow shift");
        }
      }

      boolean positiveLabelledFlowRemaining = origin.addTurnSendingFlow(currentSegment, currCompositionLabel, nextSegment, nextCompositionLabel, flowShiftPcuH);
      if (!positiveLabelledFlowRemaining && !Precision.positive(origin.getTurnSendingFlow(currentSegment, nextSegment))) {
        /* no remaining flow at all on turn after flow shift, remove turn from bush entirely */
        origin.removeTurn(currentSegment, nextSegment);
      }

      flowShiftPcuH *= flowAcceptanceFactors[(int) currentSegment.getId()];

      currCompositionLabel = nextCompositionLabel;
    }

    return flowShiftPcuH;
  }

  /**
   * {@inheritDoc}
   */
  protected void executeOriginFlowShift(Bush origin, EdgeSegment entrySegment, double bushFlowShift, double[] flowAcceptanceFactors) {
    // TODO: not yet updated to support entry segment specific flow shifts! should allow this to be simplified as passed in flow shift is
    // expected to be specific to the entry segment already! -> anything diverge specific should be removed

    /* prep - pas */
    final EdgeSegment lastS1Segment = pas.getLastEdgeSegment(true /* low cost */);
    final EdgeSegment lastS2Segment = pas.getLastEdgeSegment(false /* high cost */);
    final EdgeSegment firstS2Segment = pas.getFirstEdgeSegment(false /* high cost */);
    final var s2 = pas.getAlternative(false);
    final var s1 = pas.getAlternative(true);

    /* prep - origin */
    final var bushS2ReverseLabelChains = s2ReverseLabelChains.get(origin);
    final var bushS2UsedEndLabels = bushS2ReverseLabelChains.stream().map(chain -> chain.get(0)).collect(Collectors.toSet());
    final var bushS1ReverseLabelChains = s1ReverseLabelChains.get(origin);
    final boolean s1SegmentNotUsedYet = bushS1ReverseLabelChains.isEmpty();

    /*
     * ------------------------------------------------- S2 FLOW SHIFT ----------------------------------------------------------------------------------------------------------
     * Update S2 by shifting flow proportionally along encountered flow compositions matching with the PAS/origin/alternative
     */
    var bushS2LastSegmentLabelPortions = origin.determineProportionalFlowCompositionRates(lastS2Segment, bushS2UsedEndLabels);

    //@formatter:off
    //TODO:
    // two bugs here to fix:
    // 1) bushS2LastSegmentLabelPortions should not be using the last segment only. It is error prone. Instead rewrite the initialise to track the sending flows along the PAS
    //    and use that to extract the final portions (similar to the destination labelled on).
    // 2) The bushS2LastSegmentLabelPortions CANNOT be used to pass in to createS2DivergeProportionsByTurnLabels! This method expects the ratio of the labelled sending flow
    //    ENTERING s2 and the sending flow of s2 on the LAST SEGMENT for THE SAME LABEL, and not the ratio BETWEEN labels on the last segment. To fix this we can store these also while computing
    //    1) so we do not have to do this loop over and over. Possibly a good idea to store all this as local members because otherwise the return type in initialise becomes insane...
    
    /*
     * Determine the portion to attribute to each used [turn,entry label] combination when shifting flow across the diverge. The portions are made proportional to contribution of
     * each combination to the total sending flow on the s2-label flow on its initial link segment. Multikey: [entry segment, entry label]
     */
    MultiKeyMap<Object, Double> s2DivergeProportionsByTurnLabels = null;
    if (pas.getDivergeVertex().hasEntryEdgeSegments()) {
      s2DivergeProportionsByTurnLabels = createS2DivergeProportionsByTurnLabels(origin, bushS2ReverseLabelChains, bushS2LastSegmentLabelPortions, flowAcceptanceFactors);
    }

    /* exit turn flows out of s2 by used exit label - to be populated in s2 merge update for use by s1 update */
    var bushS2MergeExitShiftedSendingFlows = new TreeMap<BushFlowLabel, double[]>();
    for (var s2UsedReverseLabelChain : bushS2ReverseLabelChains) {
      var finalS2Label = s2UsedReverseLabelChain.getFirst();
      var startS2Label = s2UsedReverseLabelChain.getLast();

      /* shift portion of flow attributed to composition label traversing s2 */
      double s2StartLabeledFlowShift = -bushS2LastSegmentLabelPortions.get(finalS2Label) * bushFlowShift;
      double s2FinalLabeledFlowShift = executeBushLabeledAlternativeFlowShift(origin, s2UsedReverseLabelChain, s2StartLabeledFlowShift, s2, flowAcceptanceFactors, false);

      LOGGER.severe(String.format("** S2 SHIFT: label start %d, end %d, flow shift start %.10f, end %.10f", startS2Label.getLabelId(), finalS2Label.getLabelId(),
          s2StartLabeledFlowShift, s2FinalLabeledFlowShift));

      /* shift flow across final merge for S2 */
      executeBushLabeledS2FlowShiftEndMerge(origin, finalS2Label, s2FinalLabeledFlowShift, bushS2MergeExitShiftedSendingFlows);

      /* shift flows across starting diverge before entering S2 using reciprocal of flow acceptance factor */
      if (!s2DivergeProportionsByTurnLabels.isEmpty()) {
        executeBushLabeledS2FlowShiftStartDiverge(origin, startS2Label, s2StartLabeledFlowShift, s2DivergeProportionsByTurnLabels, flowAcceptanceFactors);
      }
    }

    /* convert flows to exit segment splitting rates by label */
    //TODO: BUG I THINK: pretty sure the splitting rates per label DO NOT add up to one, they add up to one per exit segment over all labels but I think it should be totalling to 
    //      one per label over all exit segments instead
    var usedExitLabels = bushS2MergeExitShiftedSendingFlows.keySet();
    double[] exitSegmentTotalShiftedFlows = new double[this.pasMergeVertexNumExitSegments];
    for (var exitLabel : usedExitLabels) {
      ArrayUtils.addTo(exitSegmentTotalShiftedFlows, bushS2MergeExitShiftedSendingFlows.get(exitLabel));
    }
    for (var exitLabel : usedExitLabels) {
      ArrayUtils.divideBy(bushS2MergeExitShiftedSendingFlows.get(exitLabel), exitSegmentTotalShiftedFlows, 0);
    }
    var bushS2MergeExitShiftedSplittingRates = bushS2MergeExitShiftedSendingFlows;
    bushS2MergeExitShiftedSendingFlows = null;

    /*
     * ------------------------------------------------- S1 FLOW SHIFT ----------------------------------------------------------------------------------------------------------
     * Update S1 by shifting flow proportionally along encountered flow compositions matching with the PAS/origin/alternative
     */
    Map<BushFlowLabel, Double> bushS1UsedEndLabelRates = null;
    if (s1SegmentNotUsedYet) {
      bushS1UsedEndLabelRates = initialiseS1Labelling(origin, bushS1ReverseLabelChains);

      /*
       * Flow into s2 that presently maintains its label at the initial diverge must be relabeled in the upstream direction because due to the flow shift it now splits off flow
       * into s1. So relabel all those "non-terminating" labels first
       */
      relabelIfNotTerminating(origin, pas.getDivergeVertex(), firstS2Segment, s2DivergeProportionsByTurnLabels);

    } else {
      var s1EndLabelPerChain = bushS1ReverseLabelChains.stream().map(chain -> chain.get(0)).collect(Collectors.toSet());
      bushS1UsedEndLabelRates = origin.determineProportionalFlowCompositionRates(lastS1Segment, s1EndLabelPerChain);
    }
    for (var s1UsedLabelChain : bushS1ReverseLabelChains) {
      var finalSegmentLabel = s1UsedLabelChain.getFirst();
      var initialSegmentLabel = s1UsedLabelChain.getLast();

      /* portion of flow attributed to composition label traversing s1 */
      double s1StartLabeledFlowShift = bushS1UsedEndLabelRates.get(finalSegmentLabel) * bushFlowShift;
      double s1FinalLabeledFlowShift = executeBushLabeledAlternativeFlowShift(origin, s1UsedLabelChain, s1StartLabeledFlowShift, s1, flowAcceptanceFactors, s1SegmentNotUsedYet);

      LOGGER.severe(String.format("** S1 SHIFT: label start %d, end %d, flow shift start %.10f, end %.10f", initialSegmentLabel.getLabelId(), finalSegmentLabel.getLabelId(),
          s1StartLabeledFlowShift, s1FinalLabeledFlowShift));

      /* shift flow across final merge for S1 based on findings in s2 */
      executeBushLabeledS1FlowShiftEndMerge(origin, finalSegmentLabel, s1FinalLabeledFlowShift, bushS2MergeExitShiftedSplittingRates);

      if (!s2DivergeProportionsByTurnLabels.isEmpty()) {
        /* shift flow across initial diverge into S1 based on findings in s2 */
        executeBushLabeledS1FlowShiftStartDiverge(origin, initialSegmentLabel, s1StartLabeledFlowShift, s2DivergeProportionsByTurnLabels, flowAcceptanceFactors);
      }
    }
  }

  /**
   * Constructor
   * 
   * @param pas to use
   * @param settings to use
   */
  protected PasFlowShiftSmartLabelledExecutor(Pas pas, final StaticLtmSettings settings) {
    super(pas, settings);
    s1ReverseLabelChains = new HashMap<>();
    s2ReverseLabelChains = new HashMap<>();
  }

  /**
   * Initialise by determining the desired flows along each subpath (on the network level) + identify the label chains along the PAS
   */
  @Override
  public void initialise() {
    super.initialise();

    var s2 = pas.getAlternative(false /* high cost */);
    var s1 = pas.getAlternative(true /* low cost */);

    for (var origin : pas.getOrigins()) {
      var s2BushLabelChains = determineUsedLabelChains(origin, s2);
      s2ReverseLabelChains.put(origin, s2BushLabelChains);

      var s1BushLabelChains = determineUsedLabelChains(origin, s1);
      s1ReverseLabelChains.put(origin, s1BushLabelChains);
    }
  }
}

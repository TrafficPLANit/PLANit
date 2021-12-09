package org.goplanit.assignment.ltm.sltm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.collections4.iterators.ReverseListIterator;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingBush;
import org.goplanit.cost.physical.AbstractPhysicalCost;
import org.goplanit.cost.virtual.AbstractVirtualCost;
import org.goplanit.utils.arrays.ArrayUtils;
import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.virtual.ConnectoidSegment;
import org.ojalgo.array.Array1D;

/**
 * Functionality to conduct a PAS flow shift
 * 
 * @author markr
 *
 */
public class PasFlowShiftExecutor {

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

  /** the label chains (list of list map value), per origin (map key), in reverse order (end to start) that traverse S2 */
  protected final Map<Bush, List<LinkedList<BushFlowCompositionLabel>>> s2ReverseLabelChains;

  /** the label chains (list of list map value), per origin (map key) in reverse order (end to start) that traverse S1 */
  protected final Map<Bush, List<LinkedList<BushFlowCompositionLabel>>> s1ReverseLabelChains;

  /** Track the desired sending flows for s1 and s2 per origin */
  protected final Map<Bush, Pair<Double, Double>> bushS1S2SendingFlows;

  /**
   * The first time a PAS is used for flow shifting, its S1 segment has no labels yet along the PAS. Therefore we create a new label unique to the S1 alternative and populate the
   * s1MatchingLabelsToFill map so it can be used in the correct format for flow shifting. Also, we create the related pasS1EndLabelRates indicating 100% of the flow on the PAS S1
   * segment for this bush is allocated to this new label at present. Again, to directly be able to use it in the flow shifting format that we use
   * 
   * @param origin                  to initialise labelling for
   * @param s1UsedLabelChainsToFill map to populate with the new label
   * @return pasS1EndLabelRates created indicating 1 (100%) of flow is allocated to the new label on the final segment of s1
   */
  private TreeMap<BushFlowCompositionLabel, Double> initialiseS1Labelling(final Bush origin, List<LinkedList<BushFlowCompositionLabel>> s1UsedLabelChainsToFill) {
    var pasS1Label = origin.createFlowCompositionLabel();
    var s1LabelChain = new LinkedList<BushFlowCompositionLabel>();
    s1LabelChain.addFirst(pasS1Label);
    s1UsedLabelChainsToFill.add(s1LabelChain);

    var pasS1EndLabelRates = new TreeMap<BushFlowCompositionLabel, Double>();
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
      var currEntryLabel = (BushFlowCompositionLabel) multiKey.getKey(1);

      if (!Precision.positive(origin.getTurnSendingFlow(entrySegment, currEntryLabel, exitSegment, currEntryLabel))) {
        continue;
      }

      /* match found - create new label to relabel any turn flow across vertex with original labelling */
      var newLabel = origin.createFlowCompositionLabel();

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
  private void relabelWhileNotTerminatingWith(final Bush origin, final DirectedVertex vertex, final EdgeSegment exitSegment, final BushFlowCompositionLabel oldLabel,
      final BushFlowCompositionLabel newLabel) {
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
  private void executeBushLabeledS2FlowShiftEndMerge(Bush origin, BushFlowCompositionLabel finalSegmentLabel, double s2FinalLabeledFlowShift,
      Map<BushFlowCompositionLabel, double[]> exitShiftedSendingFlowToPopulate) {

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
            if (!Precision.positive(origin.getTurnSendingFlow(lastS2Segment, exitSegment) + s2FlowShift)) {
              /* no remaining flow at all after flow shift, remove turn from bush entirely */
              origin.removeTurn(lastS2Segment, exitSegment);
            } else {
              origin.addTurnSendingFlow(lastS2Segment, finalSegmentLabel, exitSegment, exitLabel, s2FlowShift);
            }

            /* track so we can attribute it to s1 segment later */
            double[] exitLabelExitSegmentShiftedSendingFlow = exitShiftedSendingFlowToPopulate.get(exitLabel);
            if (exitLabelExitSegmentShiftedSendingFlow == null) {
              exitLabelExitSegmentShiftedSendingFlow = new double[pas.getMergeVertex().getExitEdgeSegments().size()];
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

    EdgeSegment lastS1Segment = pas.getLastEdgeSegment(true /* low cost */);

    /* add shifted flows through final merge towards exit segments proportionally based on labeled exit usage */
    if (pas.getMergeVertex().hasExitEdgeSegments()) {
      for (var entry : usedLabelSplittingRates.entrySet()) {
        BushFlowCompositionLabel exitLabel = entry.getKey();
        double[] exitLabelSplittingRates = entry.getValue();
        int index = 0;
        for (var exitSegment : pas.getMergeVertex().getExitEdgeSegments()) {
          double splittingRate = exitLabelSplittingRates[index];
          if (Precision.positive(splittingRate)) {
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
   * s2DivergeProportionsByTurnLabels has the following expected multikey[entrysegment,entrylabel]
   * 
   * @param origin                           to use
   * @param startSegmentLabel                flow composition label on s2 initial segment to apply
   * @param s2StartLabeledFlowShift          the flow shift applied to the first s2 segment
   * @param s2DivergeProportionsByTurnLabels portion to be shifted flow attributed to each used turn entry-exitlabel towards S2 initial segment
   * @param flowAcceptanceFactors            to use
   */
  private void executeBushLabeledS2FlowShiftStartDiverge(Bush origin, BushFlowCompositionLabel startSegmentLabel, double s2StartLabeledFlowShift,
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

          if (!Precision.positive(existingTotalTurnLabeledSendingFlow + s2DivergeEntryLabeledFlowShift)) {
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
   * the multikey of the multikeymap is expected to be [entry segment,entry label] while the value reflects the to be applied portion
   * 
   * @param origin                         to use
   * @param startSegmentLabel              flow composition label on s1 initial segment to apply
   * @param s1StartLabeledFlowShift        the flow shift applied to the first s1 segment for the label
   * @param divergeProportionsByTurnLabels portions to apply for each entrysegment-entrylabel given the to be shifted flow for a given exitlabel towards S1 initial segment
   * @param flowAcceptanceFactors          to use
   */
  private void executeBushLabeledS1FlowShiftStartDiverge(Bush origin, BushFlowCompositionLabel startSegmentLabel, double s1StartLabeledFlowShift,
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
          origin.addTurnSendingFlow(entrySegment, entryLabel, firstS1Segment, startSegmentLabel, s1DivergeEntryLabeledFlowShift);
        }
      }
    }
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
   * @param pasS2EndLabelRates    The distribution of the flow between the used labels at the end of the PAS alternative. Use this to scale the flows at the start of the PAS
   *                              utilising the knowledge how these labels (possibly) end up in a composite label that splits off halfway along the PAS
   * @param flowAcceptanceFactors to use
   * @return s2DivergeTurnLabelProportionsToPopulate to populate, only entries for used labels will be present
   */
  private MultiKeyMap<Object, Double> createS2DivergeProportionsByTurnLabels(Bush origin, List<LinkedList<BushFlowCompositionLabel>> pasS2UsedLabelChains,
      Map<BushFlowCompositionLabel, Double> pasS2EndLabelRates, final double[] flowAcceptanceFactors) {

    var firstS2EdgeSegment = pas.getFirstEdgeSegment(false /* high cost segment */);
    var s2DivergeTurnLabelProportionsToPopulate = new MultiKeyMap<Object, Double>();

    double s2InitialSegmentTotalFlow = 0;
    for (var entrySegment : pas.getDivergeVertex().getEntryEdgeSegments()) {
      if (origin.containsEdgeSegment(entrySegment)) {
        double alpha = flowAcceptanceFactors[(int) entrySegment.getId()];
        var entryLabels = origin.getFlowCompositionLabels(entrySegment);
        for (var entryLabel : entryLabels) {
          for (var usedLabelPrecessors : pasS2UsedLabelChains) {
            double turnSendingFlow = origin.getTurnSendingFlow(entrySegment, entryLabel, firstS2EdgeSegment, usedLabelPrecessors.getLast());
            if (!Precision.positive(turnSendingFlow)) {
              continue;
            }

            /* scale back by identifying the portion that makes it to the end of the S2 alternative */
            var s2EndLabel = usedLabelPrecessors.getFirst();
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
    var nextSegment = pasSegment[index];

    var reverseLabelIter = new ReverseListIterator<BushFlowCompositionLabel>(reverseOrderCompositionLabels);
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

      if (!Precision.positive(turnSendingFlow + flowShiftPcuH)) {
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
   * For the given PAS determine the flow shift to apply from the high cost to the low cost segment. Depending on the state of the segments we utilise their derivatives of travel
   * time towards flow to determine the optimal shift. In case one or both segments are uncongested, we shift as much as possible conditional on the available slack for when we
   * would expect the segment to transition to congestion.
   * 
   * @param theMode        to use
   * @param networkLoading to use
   * @param virtualCost    to use
   * @param physicalCost   to use
   * @param theMode        to use
   * 
   * @return amount of flow to shift
   */
  private double determineFlowShift(Mode theMode, AbstractPhysicalCost physicalCost, AbstractVirtualCost virtualCost, StaticLtmLoadingBush networkLoading) {

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
   * Shift flows for the PAS given the currently known costs and smoothing procedure to apply
   * 
   * @param flowShiftPcuH         amount to shift from high cost to low cost segment
   * @param flowAcceptanceFactors to use
   * @return true when flow shifted, false otherwise
   */
  private boolean executeFlowShift(double flowShiftPcuH, double[] flowAcceptanceFactors) {

    List<Bush> originsWithoutRemainingPasFlow = new ArrayList<>();

    /* prep - pas */
    final EdgeSegment lastS1Segment = pas.getLastEdgeSegment(true /* low cost */);
    final EdgeSegment lastS2Segment = pas.getLastEdgeSegment(false /* high cost */);
    final EdgeSegment firstS2Segment = pas.getFirstEdgeSegment(false /* high cost */);
    final var s2 = pas.getAlternative(false);
    final var s1 = pas.getAlternative(true);

    LOGGER.severe("** PAS FLOW shift" + pas.toString());

    for (var origin : pas.getOrigins()) {

      /* prep - origin */
      final var bushS2ReverseLabelChains = s2ReverseLabelChains.get(origin);
      final var bushS2UsedEndLabels = bushS2ReverseLabelChains.stream().map(chain -> chain.get(0)).collect(Collectors.toSet());
      final var bushS1ReverseLabelChains = s1ReverseLabelChains.get(origin);
      final boolean s1SegmentNotUsedYet = bushS1ReverseLabelChains.isEmpty();
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

      /*
       * ------------------------------------------------- S2 FLOW SHIFT ----------------------------------------------------------------------------------------------------------
       * Update S2 by shifting flow proportionally along encountered flow compositions matching with the PAS/origin/alternative
       */
      var bushS2EndLabelRates = origin.determineProportionalFlowCompositionRates(lastS2Segment, bushS2UsedEndLabels);

      /*
       * Determine the portion to attribute to each used [turn,entry label] combination when shifting flow across the diverge. The portions are made proportional to contribution of
       * each combination to the total sending flow on the s2-label flow on its initial link segment. Multikey: [entry segment, entry label]
       */
      MultiKeyMap<Object, Double> s2DivergeProportionsByTurnLabels = null;
      if (pas.getDivergeVertex().hasEntryEdgeSegments()) {
        s2DivergeProportionsByTurnLabels = createS2DivergeProportionsByTurnLabels(origin, bushS2ReverseLabelChains, bushS2EndLabelRates, flowAcceptanceFactors);
      }

      /* exit turn flows out of s2 by used exit label - to be populated in s2 merge update for use by s1 update */
      var bushS2MergeExitShiftedSendingFlows = new TreeMap<BushFlowCompositionLabel, double[]>();
      for (var s2UsedReverseLabelChain : bushS2ReverseLabelChains) {
        var finalS2Label = s2UsedReverseLabelChain.getFirst();
        var startS2Label = s2UsedReverseLabelChain.getLast();

        /* shift portion of flow attributed to composition label traversing s2 */
        double s2StartLabeledFlowShift = -bushS2EndLabelRates.get(finalS2Label) * bushFlowShift;
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
      var usedExitLabels = bushS2MergeExitShiftedSendingFlows.keySet();
      double[] exitSegmentTotalShiftedFlows = new double[pas.getMergeVertex().getExitEdgeSegments().size()];
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
      Map<BushFlowCompositionLabel, Double> bushS1UsedEndLabelRates = null;
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

    /* remove irrelevant bushes */
    pas.removeOrigins(originsWithoutRemainingPasFlow);

    return true;
  }

  /**
   * Initialise by determining the desired flows along each subpath (on the network level)
   */
  protected void initialise() {
    /* determine the network flow on the high cost subpath */

    var s2 = pas.getAlternative(false /* high cost */);
    var s1 = pas.getAlternative(true /* low cost */);

    s2SendingFlow = 0;
    s1SendingFlow = 0;
    for (var origin : pas.getOrigins()) {
      // TODO: consider combining both to avoid looping over the alternative segments twice per alternative!
      var s2BushLabelChains = origin.determineUsedLabelChains(s2);
      double s2BushSendingFlow = origin.determineSubPathSendingFlow(s2);
      s2SendingFlow += s2BushSendingFlow;
      s2ReverseLabelChains.put(origin, s2BushLabelChains);

      var s1BushLabelChains = origin.determineUsedLabelChains(s1);
      double s1BushSendingFlow = origin.determineSubPathSendingFlow(s1);
      s1SendingFlow += s1BushSendingFlow;
      s1ReverseLabelChains.put(origin, s1BushLabelChains);

      bushS1S2SendingFlows.put(origin, Pair.of(s1BushSendingFlow, s2BushSendingFlow));
    }
  }

  /**
   * Constructor
   * 
   * @param pas to use
   */
  protected PasFlowShiftExecutor(Pas pas) {
    this.pas = pas;
    s1ReverseLabelChains = new HashMap<>();
    s2ReverseLabelChains = new HashMap<>();
    bushS1S2SendingFlows = new HashMap<>();
    initialise();
  }

  /**
   * Factory method to create the flow shift executor for a given PAS
   * 
   * @param pas to use
   * @return created executor
   */
  public static PasFlowShiftExecutor create(Pas pas) {
    return new PasFlowShiftExecutor(pas);
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

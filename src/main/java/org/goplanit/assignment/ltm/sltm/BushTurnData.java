package org.goplanit.assignment.ltm.sltm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.utils.arrays.ArrayUtils;
import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.math.Precision;

/**
 * Track the turn based data of a bush.
 * <p>
 * For now we only track turn sending flows to minimise bookkeeping and memory usage, splitting rates are deduced from the turn sending flows when needed.
 * 
 * @author markr
 *
 */
public class BushTurnData implements Cloneable {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(BushTurnData.class.getCanonicalName());

  // TODO not supported yet, but container is there
  /** track which composition labels are registered on each link segment */
  private final HashMap<EdgeSegment, Set<BushFlowCompositionLabel>> linkSegmentCompositionLabels;

  /** track known bush turn sending flows s_ab by the combined key of incoming (and composition) outgoing (and composition) link segments */
  private final MultiKeyMap<Object, Double> compositionTurnSendingFlows;

  /**
   * Based on the currently registered link segment composition labels, assess if all are still present by means of checking if at least a flow is registered for each composition
   * on any turn of the node. If not remove the compositions that are no longer present
   * 
   * @param node to check
   */
  private void pruneCompositionLabels(final DirectedVertex node) {
    Map<EdgeSegment, Set<BushFlowCompositionLabel>> identifiedExitCompositionLabelMap = new HashMap<EdgeSegment, Set<BushFlowCompositionLabel>>();
    for (EdgeSegment entrySegment : node.getEntryEdgeSegments()) {
      Set<BushFlowCompositionLabel> existingEntryLabels = linkSegmentCompositionLabels.get(entrySegment);
      if (existingEntryLabels == null) {
        continue;
      }
      Set<BushFlowCompositionLabel> remainingEntryLabels = new HashSet<BushFlowCompositionLabel>();
      for (BushFlowCompositionLabel entryComposition : existingEntryLabels) {
        for (EdgeSegment exitSegment : node.getExitEdgeSegments()) {
          Set<BushFlowCompositionLabel> exitLabels = linkSegmentCompositionLabels.get(exitSegment);
          if (exitLabels == null) {
            continue;
          }
          Set<BushFlowCompositionLabel> identifiedExitCompositionLabels = identifiedExitCompositionLabelMap.get(exitSegment);
          if (identifiedExitCompositionLabels == null) {
            identifiedExitCompositionLabels = new HashSet<BushFlowCompositionLabel>();
            identifiedExitCompositionLabelMap.put(exitSegment, identifiedExitCompositionLabels);
          }

          for (BushFlowCompositionLabel exitComposition : exitLabels) {
            if (compositionTurnSendingFlows.containsKey(entrySegment, entryComposition, exitSegment, exitComposition)) {
              remainingEntryLabels.add(entryComposition);
              identifiedExitCompositionLabels.add(exitComposition);
            }
          }
        }
      }
      if (remainingEntryLabels.isEmpty()) {
        linkSegmentCompositionLabels.remove(entrySegment);
      } else {
        linkSegmentCompositionLabels.put(entrySegment, remainingEntryLabels);
      }
    }
    identifiedExitCompositionLabelMap.forEach((exitSegment, remainingExitLabels) -> {
      if (remainingExitLabels.isEmpty()) {
        linkSegmentCompositionLabels.remove(exitSegment);
      } else {
        linkSegmentCompositionLabels.put(exitSegment, remainingExitLabels);
      }
    });
  }

  /**
   * Register a composition label on an edge segment
   * 
   * @param edgeSegment      to register label for
   * @param compositionLabel to register
   */
  private void registerEdgeSegmentCompositionLabel(final EdgeSegment edgeSegment, final BushFlowCompositionLabel compositionLabel) {
    Set<BushFlowCompositionLabel> fromLabels = linkSegmentCompositionLabels.get(edgeSegment);
    if (fromLabels == null) {
      fromLabels = new HashSet<BushFlowCompositionLabel>();
      linkSegmentCompositionLabels.put(edgeSegment, fromLabels);
    }
    fromLabels.add(compositionLabel);
  }

  /**
   * Constructor
   * 
   */
  BushTurnData() {
    this.compositionTurnSendingFlows = new MultiKeyMap<Object, Double>();
    this.linkSegmentCompositionLabels = new HashMap<EdgeSegment, Set<BushFlowCompositionLabel>>();
  }

  /**
   * copy constructor.
   * 
   * @param bushTurnData to copy
   */
  public BushTurnData(BushTurnData bushTurnData) {
    this.compositionTurnSendingFlows = bushTurnData.compositionTurnSendingFlows.clone();
    this.linkSegmentCompositionLabels = new HashMap<EdgeSegment, Set<BushFlowCompositionLabel>>();
    bushTurnData.linkSegmentCompositionLabels.forEach((k, v) -> linkSegmentCompositionLabels.put(k, new HashSet<BushFlowCompositionLabel>(v)));
  }

  /**
   * Update the turn sending flow for a given turn
   * 
   * @param fromSegment     of turn
   * @param fromComposition of turn flow
   * @param toSegment       of turn
   * @param toComposition   of turn flow
   * @param turnSendingFlow to update
   */
  public void setTurnSendingFlow(final EdgeSegment fromSegment, BushFlowCompositionLabel fromComposition, final EdgeSegment toSegment, BushFlowCompositionLabel toComposition,
      double turnSendingFlow) {

    if (!Precision.isPositive(turnSendingFlow)) {
      LOGGER.warning(String.format("Turn (%s to %s) sending flow negative (%.2f), remove entry for label (%s,%s)", fromSegment.getXmlId(), toSegment.getXmlId(), turnSendingFlow,
          fromComposition.getLabelId(), toComposition.getLabelId()));
      removeTurnFlow(fromSegment, fromComposition, toSegment, toComposition);
    } else {
      compositionTurnSendingFlows.put(fromSegment, fromComposition, toSegment, toComposition, turnSendingFlow);
      registerEdgeSegmentCompositionLabel(fromSegment, fromComposition);
      registerEdgeSegmentCompositionLabel(toSegment, toComposition);
    }
  }

  /**
   * Add turn sending flow for a given turn
   * 
   * @param fromSegment     of turn
   * @param fromComposition of turn flow
   * @param toSegment       of turn
   * @param toComposition   of turn flow
   * @param turnSendingFlow to add
   */
  public void addTurnSendingFlow(final EdgeSegment fromSegment, BushFlowCompositionLabel fromComposition, final EdgeSegment toSegment, BushFlowCompositionLabel toComposition,
      double turnSendingFlow) {

    Double newSendingFlow = turnSendingFlow + getTurnSendingFlowPcuH(fromSegment, fromComposition, toSegment, toComposition);
    if (!Precision.isPositive(newSendingFlow)) {
      LOGGER.warning(String.format("Turn (%s to %s) sending flow negative (%.2f) after adding %.2f flow, remove labelled entry (%d,%d)", fromSegment.getXmlId(),
          toSegment.getXmlId(), newSendingFlow, turnSendingFlow, fromComposition.getLabelId(), toComposition.getLabelId()));
      removeTurnFlow(fromSegment, fromComposition, toSegment, toComposition);
    } else {
      setTurnSendingFlow(fromSegment, fromComposition, toSegment, toComposition, newSendingFlow);
    }
  }

  /**
   * Remove the turn entirely
   * 
   * @param fromEdgeSegment of turn
   * @param toEdgeSegment   of turn
   */
  public void removeTurn(final EdgeSegment fromEdgeSegment, final EdgeSegment toEdgeSegment) {
    Set<BushFlowCompositionLabel> fromLabels = linkSegmentCompositionLabels.get(fromEdgeSegment);
    Set<BushFlowCompositionLabel> toLabels = linkSegmentCompositionLabels.get(toEdgeSegment);
    if (fromLabels != null && toLabels != null) {
      fromLabels.forEach(
          fromComposition -> toLabels.forEach(toComposition -> compositionTurnSendingFlows.removeMultiKey(fromEdgeSegment, fromComposition, toEdgeSegment, toComposition)));
    }
    pruneCompositionLabels(fromEdgeSegment.getDownstreamVertex());
  }

  /**
   * Remove the turn flow of the given labels (if present) and update the link composition labels in the process
   * 
   * @param fromEdgeSegment of turn
   * @param fromLabel       of turn flow
   * @param toEdgeSegment   of turn
   * @param toLabel         of turn flow
   */
  public void removeTurnFlow(final EdgeSegment fromEdgeSegment, final BushFlowCompositionLabel fromLabel, final EdgeSegment toEdgeSegment, final BushFlowCompositionLabel toLabel) {
    if (fromLabel == null || toLabel == null || fromEdgeSegment == null || toEdgeSegment != null) {
      LOGGER.severe("One or more inputs required to remove turn flow from bush data registration is null, unable to remove turn flow");
      return;
    }
    compositionTurnSendingFlows.removeMultiKey(fromEdgeSegment, fromLabel, toEdgeSegment, toLabel);

    // TODO: inefficient, we know which labels might have to be removed, so more lightweight approach would be better
    pruneCompositionLabels(fromEdgeSegment.getDownstreamVertex());
  }

  /**
   * Get the turn sending flow for a given turn
   * 
   * @param fromSegment     of turn
   * @param fromComposition of turn flow
   * @param toSegment       of turn
   * @param toComposition   of turn flow
   * @return turn sending flow, 0 if not present
   */
  public double getTurnSendingFlowPcuH(final EdgeSegment fromSegment, BushFlowCompositionLabel fromComposition, final EdgeSegment toSegment,
      BushFlowCompositionLabel toComposition) {
    Double existingSendingFlow = compositionTurnSendingFlows.get(fromSegment, fromComposition, toSegment, toComposition);
    if (existingSendingFlow != null) {
      return existingSendingFlow;
    } else {
      return 0;
    }
  }

  /**
   * Get the turn sending flow for a given turn regardless of the composition label
   * 
   * @param fromSegment of turn
   * @param toSegment   of turn
   * @return turn sending flow, 0 if not present
   */
  public double getTurnSendingFlowPcuH(final EdgeSegment fromSegment, final EdgeSegment toSegment) {
    double totalTurnSendingFlow = 0;
    Set<BushFlowCompositionLabel> fromLabels = linkSegmentCompositionLabels.get(fromSegment);
    if (fromLabels == null) {
      return totalTurnSendingFlow;
    }
    Set<BushFlowCompositionLabel> toLabels = linkSegmentCompositionLabels.get(toSegment);
    if (toLabels == null) {
      return totalTurnSendingFlow;
    }

    for (BushFlowCompositionLabel fromComposition : fromLabels) {
      for (BushFlowCompositionLabel toComposition : toLabels) {
        double s_ab = getTurnSendingFlowPcuH(fromSegment, fromComposition, toSegment, toComposition);
        totalTurnSendingFlow += s_ab;
      }
    }
    return totalTurnSendingFlow;
  }

  /**
   * Total sending flows s_a from given segment
   * 
   * @param edgeSegment to use
   * @return sending flow s_a
   */
  public double getTotalSendingFlowPcuH(final EdgeSegment edgeSegment) {
    double totalSendingFlow = 0;
    Set<BushFlowCompositionLabel> fromLabels = linkSegmentCompositionLabels.get(edgeSegment);
    if (fromLabels == null) {
      return totalSendingFlow;
    }
    for (BushFlowCompositionLabel fromComposition : fromLabels) {
      for (EdgeSegment exitSegment : edgeSegment.getDownstreamVertex().getExitEdgeSegments()) {
        Set<BushFlowCompositionLabel> toLabels = linkSegmentCompositionLabels.get(exitSegment);
        if (toLabels == null) {
          continue;
        }
        for (BushFlowCompositionLabel toComposition : toLabels) {
          double s_ab = getTurnSendingFlowPcuH(edgeSegment, fromComposition, exitSegment, toComposition);
          totalSendingFlow += s_ab;
        }
      }
    }
    return totalSendingFlow;
  }

  /**
   * Collect the sending flow of an edge segment in the bush but only for the specified label, if not present, zero flow is returned
   * 
   * @param edgeSegment      to collect sending flow for
   * @param compositionLabel to filter by
   * @return bush sending flow on edge segment
   */
  public double getTotalSendingFlowPcuH(final EdgeSegment edgeSegment, final BushFlowCompositionLabel compositionLabel) {
    if (!hasFlowCompositionLabel(edgeSegment, compositionLabel)) {
      return 0;
    }
    double totalSendingFlow = 0;
    for (EdgeSegment exitSegment : edgeSegment.getDownstreamVertex().getExitEdgeSegments()) {
      Set<BushFlowCompositionLabel> toLabels = linkSegmentCompositionLabels.get(exitSegment);
      if (toLabels == null) {
        continue;
      }
      for (BushFlowCompositionLabel toComposition : toLabels) {
        double s_ab = getTurnSendingFlowPcuH(edgeSegment, compositionLabel, exitSegment, toComposition);
        totalSendingFlow += s_ab;
      }
    }
    return totalSendingFlow;
  }

  /**
   * Check if entry exists
   * 
   * @param fromSegment of turn
   * @param toSegment   of turn
   * @return true when present, false otherwise
   */
  public boolean containsTurnSendingFlow(final EdgeSegment fromSegment, final EdgeSegment toSegment) {
    Set<BushFlowCompositionLabel> fromLabels = linkSegmentCompositionLabels.get(fromSegment);
    if (fromLabels == null) {
      return false;
    }
    for (BushFlowCompositionLabel fromComposition : fromLabels) {
      for (EdgeSegment exitSegment : fromSegment.getDownstreamVertex().getExitEdgeSegments()) {
        Set<BushFlowCompositionLabel> toLabels = linkSegmentCompositionLabels.get(exitSegment);
        if (toLabels == null) {
          continue;
        }
        for (BushFlowCompositionLabel toComposition : toLabels) {
          if (Precision.isPositive(getTurnSendingFlowPcuH(fromSegment, fromComposition, exitSegment, toComposition))) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Verify if the turn sending flow for a given turn is positive
   * 
   * @param fromSegment     of turn
   * @param fromComposition of turn flow
   * @param toSegment       of turn
   * @param toComposition   of turn flow
   * @return true when present, false otherwise
   */
  public boolean containsTurnSendingFlow(final EdgeSegment fromSegment, BushFlowCompositionLabel fromComposition, final EdgeSegment toSegment,
      BushFlowCompositionLabel toComposition) {
    return Precision.isPositive(getTurnSendingFlowPcuH(fromSegment, fromComposition, toSegment, toComposition));
  }

  /**
   * Collect the splitting rates for a given link segment and composition. Splitting rates are based on the current (labelled) turn sending flows s_ab. In case no flows are present
   * for the given composition label, zero splitting rates for all turns are returned.
   * 
   * @param fromSegment     to collect bush splitting rates for
   * @param fromComposition to restrict splitting rates to
   * @return splitting rates in primitive array in order of which one iterates over the outgoing edge segments of the downstream from segment vertex
   */
  public double[] getSplittingRates(final EdgeSegment fromSegment) {
    Set<EdgeSegment> exitEdgeSegments = fromSegment.getDownstreamVertex().getExitEdgeSegments();
    double[] splittingRates = new double[exitEdgeSegments.size()];

    double totalSendingFlow = 0;
    int index = 0;
    for (EdgeSegment exitSegment : exitEdgeSegments) {
      double s_ab = getTurnSendingFlowPcuH(fromSegment, exitSegment);
      splittingRates[index++] = s_ab;
      totalSendingFlow += s_ab;
    }
    ArrayUtils.divideBy(splittingRates, totalSendingFlow, 0);
    return splittingRates;
  }

  /**
   * Collect the bush splitting rates for a given incoming edge segment and entry-exit composition labelling. If no flow exits for this input, zero splitting rates are returned for
   * all turns
   * 
   * @param fromSegment to use
   * @param fromLabel   to use
   * @param toLabel     to use
   * @return splitting rates in primitive array in order of which one iterates over the outgoing edge segments of the downstream from segment vertex
   */
  public double[] getSplittingRates(EdgeSegment fromSegment, BushFlowCompositionLabel fromLabel, BushFlowCompositionLabel toLabel) {
    Set<EdgeSegment> exitEdgeSegments = fromSegment.getDownstreamVertex().getExitEdgeSegments();
    double[] splittingRates = new double[exitEdgeSegments.size()];

    double totalSendingFlow = 0;
    int index = 0;
    for (EdgeSegment exitSegment : exitEdgeSegments) {
      double s_ab = getTurnSendingFlowPcuH(fromSegment, fromLabel, exitSegment, toLabel);
      splittingRates[index++] = s_ab;
      totalSendingFlow += s_ab;
    }

    ArrayUtils.divideBy(splittingRates, totalSendingFlow, 0);
    return splittingRates;
  }

  /**
   * Collect the bush splitting rates for a given incoming edge segment and entry label. If no flow exits, zero splitting rates are returned
   * 
   * @param entrySegment to use
   * @param entryLabel   to use
   * @return splitting rates in multikeymap where the key is the combination of exit segment and exit label and the value is the portion of the entry segment entry label flow
   *         directed to it
   */
  public MultiKeyMap<Object, Double> getSplittingRates(EdgeSegment fromSegment, BushFlowCompositionLabel fromLabel) {
    Set<EdgeSegment> exitEdgeSegments = fromSegment.getDownstreamVertex().getExitEdgeSegments();

    MultiKeyMap<Object, Double> splittingRatesByExitSegmentLabel = new MultiKeyMap<Object, Double>();
    double totalSendingFlow = getTotalSendingFlowPcuH(fromSegment, fromLabel);
    for (EdgeSegment exitSegment : exitEdgeSegments) {
      Set<BushFlowCompositionLabel> toLabels = getFlowCompositionLabels(exitSegment);
      if (toLabels == null) {
        continue;
      }
      for (BushFlowCompositionLabel toLabel : toLabels) {
        double s_ab = getTurnSendingFlowPcuH(fromSegment, fromLabel, exitSegment, toLabel);
        if (Precision.isPositive(s_ab)) {
          splittingRatesByExitSegmentLabel.put(exitSegment, toLabel, s_ab / totalSendingFlow);
        }
        totalSendingFlow += s_ab;
      }
    }
    return splittingRatesByExitSegmentLabel;
  }

  /**
   * Collect the splitting rate for a given link segment. Splitting rates are based on the current turn sending flows s_ab.
   * <p>
   * When collecting multiple splitting rates with the same in link, do not use this method but instead collect all splitting rates at once and then filter the ones you require it
   * is computationally more efficient.
   * 
   * 
   * @param fromSegment of turn to collect splitting rate for
   * @param toSegment   of turn to collect splitting rate for
   * @return splitting rate, when turn is not present or not used, zero is returned
   */
  public double getSplittingRate(final EdgeSegment fromSegment, final EdgeSegment toSegment) {
    double turnSendingFlow = getTurnSendingFlowPcuH(fromSegment, toSegment);
    if (Precision.isPositive(turnSendingFlow)) {
      return turnSendingFlow / getTotalSendingFlowPcuH(fromSegment);
    } else {
      return 0;
    }
  }

  /**
   * The currently registered flow composition labels for this edge segment
   * 
   * @param edgeSegment to collect for
   * @return labels for edge segment
   */
  public Set<BushFlowCompositionLabel> getFlowCompositionLabels(final EdgeSegment edgeSegment) {
    return this.linkSegmentCompositionLabels.get(edgeSegment);
  }

  /**
   * Verify if the edge segment has any flow composition labels registered on it
   * 
   * @param edgeSegment to verify
   * @return true when present, false otherwise
   */
  public boolean hasFlowCompositionLabel(final EdgeSegment edgeSegment) {
    Set<BushFlowCompositionLabel> labels = getFlowCompositionLabels(edgeSegment);
    return labels != null && !labels.isEmpty();
  }

  /**
   * Verify if the edge segment has the flow composition label provided
   * 
   * @param edgeSegment      to verify
   * @param compositionLabel to verify
   * @return true when present, false otherwise
   */
  public boolean hasFlowCompositionLabel(final EdgeSegment edgeSegment, final BushFlowCompositionLabel compositionLabel) {
    Set<BushFlowCompositionLabel> labels = getFlowCompositionLabels(edgeSegment);
    if (labels != null) {
      return labels.contains(compositionLabel);
    }
    return false;
  }

  /**
   * Relabel existing flow from one composition from-to combination to a new from-to label
   * 
   * @param fromSegment    from segment of turn
   * @param oldFromLabel   from composition label to replace
   * @param toSegment      to segment of turn
   * @param oldToLabel     to composition label to replace
   * @param newFromToLabel label to replace flow with
   * @return the amount of flow that was relabelled
   */
  public double relabel(EdgeSegment fromSegment, BushFlowCompositionLabel oldFromLabel, EdgeSegment toSegment, BushFlowCompositionLabel oldToLabel,
      BushFlowCompositionLabel newFromToLabel) {

    double flowToRelabel = getTurnSendingFlowPcuH(fromSegment, oldFromLabel, toSegment, oldToLabel);
    removeTurnFlow(fromSegment, oldFromLabel, toSegment, oldToLabel);
    if (Precision.isPositive(flowToRelabel)) {
      addTurnSendingFlow(fromSegment, newFromToLabel, toSegment, newFromToLabel, flowToRelabel);
    }
    return flowToRelabel;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BushTurnData clone() {
    return new BushTurnData(this);
  }

  /**
   * Determine the composition exit labels on exit segments that have positive flow on the turn with the provided edge segment and composition label
   * 
   * @param edgeSegment      incoming turn segment
   * @param compositionLabel filter by label, i.e., only flow emanating from this label towards exit segments is considered
   * @return used labels on any exit segment with positive from frmo entry segment with given entry label
   */
  public List<BushFlowCompositionLabel> determineUsedTurnCompositionLabels(EdgeSegment edgeSegment, BushFlowCompositionLabel compositionLabel) {
    List<BushFlowCompositionLabel> usedLabels = new ArrayList<BushFlowCompositionLabel>(5);
    if (edgeSegment.getDownstreamVertex().hasExitEdgeSegments()) {
      for (EdgeSegment exitsegment : edgeSegment.getDownstreamVertex().getExitEdgeSegments()) {
        Set<BushFlowCompositionLabel> exitLabels = getFlowCompositionLabels(edgeSegment);
        for (BushFlowCompositionLabel exitLabel : exitLabels) {
          if (containsTurnSendingFlow(edgeSegment, compositionLabel, exitsegment, exitLabel)) {
            usedLabels.add(exitLabel);
          }
        }
      }
    }
    return usedLabels;
  }

}

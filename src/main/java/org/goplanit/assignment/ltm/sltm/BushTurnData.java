package org.goplanit.assignment.ltm.sltm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.utils.arrays.ArrayUtils;
import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.zoning.Centroid;

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
  private final HashMap<EdgeSegment, TreeSet<BushFlowLabel>> linkSegmentCompositionLabels;

  /** track known bush turn sending flows s_ab by the combined key of incoming (and composition) outgoing (and composition) link segments */
  private final MultiKeyMap<Object, Double> compositionTurnSendingFlows;

  /**
   * Based on the currently registered link segment composition labels, assess if all are still present by means of checking if at least a flow is registered for each composition
   * on any turn of the node. If not remove the compositions that are no longer present
   * 
   * @param node to check
   */
  private void pruneCompositionLabels(final DirectedVertex node) {

    var identifiedExitCompositionLabelMap = new HashMap<EdgeSegment, TreeSet<BushFlowLabel>>();
    for (var entrySegment : node.getEntryEdgeSegments()) {

      var existingEntryLabels = linkSegmentCompositionLabels.get(entrySegment);
      if (existingEntryLabels == null) {
        continue;
      }

      var remainingEntryLabels = new TreeSet<BushFlowLabel>();
      for (BushFlowLabel entryComposition : existingEntryLabels) {
        for (var exitSegment : node.getExitEdgeSegments()) {

          var exitLabels = linkSegmentCompositionLabels.get(exitSegment);
          if (exitLabels == null) {
            continue;
          }

          var identifiedExitCompositionLabels = identifiedExitCompositionLabelMap.get(exitSegment);
          if (identifiedExitCompositionLabels == null) {
            identifiedExitCompositionLabels = new TreeSet<BushFlowLabel>();
            identifiedExitCompositionLabelMap.put(exitSegment, identifiedExitCompositionLabels);
          }

          for (BushFlowLabel exitComposition : exitLabels) {
            Double labelledTurnFlow = compositionTurnSendingFlows.get(entrySegment, entryComposition, exitSegment, exitComposition);

            if (labelledTurnFlow == null) {
              continue;
            }
            if (Precision.positive(labelledTurnFlow)) {
              remainingEntryLabels.add(entryComposition);
              identifiedExitCompositionLabels.add(exitComposition);
            } else {
              compositionTurnSendingFlows.removeMultiKey(entrySegment, entryComposition, exitSegment, exitComposition);
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
  private void registerEdgeSegmentCompositionLabel(final EdgeSegment edgeSegment, final BushFlowLabel compositionLabel) {
    var existingLabels = linkSegmentCompositionLabels.get(edgeSegment);
    if (existingLabels == null) {
      existingLabels = new TreeSet<BushFlowLabel>();
      linkSegmentCompositionLabels.put(edgeSegment, existingLabels);
    }
    if (!existingLabels.contains(compositionLabel)) {
      existingLabels.add(compositionLabel);
    }
  }

  /**
   * Register labelled sending flow on the container while also ensuring link level composition labels are kept consistent
   * 
   * @param fromSegment     of turn
   * @param fromComposition label of turn
   * @param toSegment       of turn
   * @param toComposition   label of turn
   * @param turnSendingFlow flow of turn
   */
  private void registerLabelledTurnSendingFlow(EdgeSegment fromSegment, BushFlowLabel fromComposition, EdgeSegment toSegment, BushFlowLabel toComposition, double turnSendingFlow) {
    compositionTurnSendingFlows.put(fromSegment, fromComposition, toSegment, toComposition, turnSendingFlow);
    registerEdgeSegmentCompositionLabel(fromSegment, fromComposition);
    registerEdgeSegmentCompositionLabel(toSegment, toComposition);
  }

  /**
   * Relabel existing flow from one composition from-to combination to a new from-to label
   * 
   * @param fromSegment  from segment of turn
   * @param oldFromLabel from composition label to replace
   * @param toSegment    to segment of turn
   * @param oldToLabel   to composition label to replace
   * @param newFromLabel label to replace flow with
   * @param newToLabel   label to replace flow with
   * @return the amount of flow that was relabelled
   */
  private double relabel(EdgeSegment fromSegment, BushFlowLabel oldFromLabel, EdgeSegment toSegment, BushFlowLabel oldToLabel, BushFlowLabel newFromLabel,
      BushFlowLabel newToLabel) {

    double flowToRelabel = getTurnSendingFlowPcuH(fromSegment, oldFromLabel, toSegment, oldToLabel);
    removeTurnFlow(fromSegment, oldFromLabel, toSegment, oldToLabel);
    if (Precision.positive(flowToRelabel)) {
      addTurnSendingFlow(fromSegment, newFromLabel, toSegment, newToLabel, flowToRelabel);
    }
    return flowToRelabel;
  }

  /**
   * Constructor
   * 
   */
  BushTurnData() {
    this.compositionTurnSendingFlows = new MultiKeyMap<Object, Double>();
    this.linkSegmentCompositionLabels = new HashMap<EdgeSegment, TreeSet<BushFlowLabel>>();
  }

  /**
   * copy constructor.
   * 
   * @param bushTurnData to copy
   */
  public BushTurnData(BushTurnData bushTurnData) {
    this.compositionTurnSendingFlows = bushTurnData.compositionTurnSendingFlows.clone();
    this.linkSegmentCompositionLabels = new HashMap<EdgeSegment, TreeSet<BushFlowLabel>>();
    bushTurnData.linkSegmentCompositionLabels.forEach((k, v) -> linkSegmentCompositionLabels.put(k, new TreeSet<BushFlowLabel>(v)));
  }

  /**
   * Update the turn sending flow for a given turn
   * 
   * @param fromSegment     of turn
   * @param fromComposition of turn flow
   * @param toSegment       of turn
   * @param toComposition   of turn flow
   * @param turnSendingFlow to update
   * @param true            when turn has any labelled turn sending flow left after addition, false when labelled turn sending flow no longer exists
   */
  public boolean setTurnSendingFlow(final EdgeSegment fromSegment, BushFlowLabel fromComposition, final EdgeSegment toSegment, BushFlowLabel toComposition,
      double turnSendingFlow) {

    if (!Precision.positive(turnSendingFlow)) {
      if (Double.isNaN(turnSendingFlow)) {
        LOGGER.severe("Turn (%s to %s) sending flow is NAN, shouldn't happen - consider identifying issue as turn flow cannot be updated properly");
        return true;
      }

      LOGGER.warning(String.format("Turn (%s to %s) sending flow not positive (enough) (%.9f), remove entry for label (%s,%s)", fromSegment.getXmlId(), toSegment.getXmlId(),
          turnSendingFlow, fromComposition.getLabelId(), toComposition.getLabelId()));
      removeTurnFlow(fromSegment, fromComposition, toSegment, toComposition);
      return false;

    } else {

      registerLabelledTurnSendingFlow(fromSegment, fromComposition, toSegment, toComposition, turnSendingFlow);
      return true;

    }
  }

  /**
   * Add turn sending flow for a given turn (can be negative)
   * 
   * @param fromSegment     of turn
   * @param fromComposition of turn flow
   * @param toSegment       of turn
   * @param toComposition   of turn flow
   * @param turnSendingFlow to add
   * @return true when turn has any sending flow left after addition, false when labelled turn sending flow no longer exists
   */
  public boolean addTurnSendingFlow(final EdgeSegment fromSegment, BushFlowLabel fromComposition, final EdgeSegment toSegment, BushFlowLabel toComposition,
      double turnSendingFlow) {
    Double newSendingFlow = turnSendingFlow + getTurnSendingFlowPcuH(fromSegment, fromComposition, toSegment, toComposition);
    return setTurnSendingFlow(fromSegment, fromComposition, toSegment, toComposition, newSendingFlow);
  }

  /**
   * Remove the turn entirely
   * 
   * @param fromEdgeSegment of turn
   * @param toEdgeSegment   of turn
   */
  public void removeTurn(final EdgeSegment fromEdgeSegment, final EdgeSegment toEdgeSegment) {
    var fromLabels = linkSegmentCompositionLabels.get(fromEdgeSegment);
    var toLabels = linkSegmentCompositionLabels.get(toEdgeSegment);
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
  public void removeTurnFlow(final EdgeSegment fromEdgeSegment, final BushFlowLabel fromLabel, final EdgeSegment toEdgeSegment, final BushFlowLabel toLabel) {
    if (fromLabel == null || toLabel == null || fromEdgeSegment == null || toEdgeSegment == null) {
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
  public double getTurnSendingFlowPcuH(final EdgeSegment fromSegment, BushFlowLabel fromComposition, final EdgeSegment toSegment, BushFlowLabel toComposition) {
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
    var fromLabels = linkSegmentCompositionLabels.get(fromSegment);
    if (fromLabels == null) {
      return totalTurnSendingFlow;
    }
    var toLabels = linkSegmentCompositionLabels.get(toSegment);
    if (toLabels == null) {
      return totalTurnSendingFlow;
    }

    for (var fromComposition : fromLabels) {
      for (var toComposition : toLabels) {
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
  public double getTotalSendingFlowFromPcuH(final EdgeSegment edgeSegment) {
    double totalSendingFlow = 0;
    var fromLabels = linkSegmentCompositionLabels.get(edgeSegment);
    if (fromLabels == null) {
      return totalSendingFlow;
    }
    for (var fromComposition : fromLabels) {
      for (var exitSegment : edgeSegment.getDownstreamVertex().getExitEdgeSegments()) {
        var toLabels = linkSegmentCompositionLabels.get(exitSegment);
        if (toLabels == null) {
          continue;
        }
        for (var toComposition : toLabels) {
          double s_ab = getTurnSendingFlowPcuH(edgeSegment, fromComposition, exitSegment, toComposition);
          totalSendingFlow += s_ab;
        }
      }
    }
    return totalSendingFlow;
  }

  /**
   * Collect the sending flow originating at an edge segment in the bush but only for the specified label, if not present, zero flow is returned
   * 
   * @param edgeSegment      to collect sending flow originating from for
   * @param compositionLabel to filter by
   * @return bush sending flow found
   */
  public double getTotalSendingFlowFromPcuH(final EdgeSegment edgeSegment, final BushFlowLabel compositionLabel) {
    if (!hasFlowCompositionLabel(edgeSegment, compositionLabel)) {
      return 0;
    }
    double totalSendingFlow = 0;
    for (var exitSegment : edgeSegment.getDownstreamVertex().getExitEdgeSegments()) {
      var toLabels = linkSegmentCompositionLabels.get(exitSegment);
      if (toLabels == null) {
        continue;
      }
      for (var toComposition : toLabels) {
        double s_ab = getTurnSendingFlowPcuH(edgeSegment, compositionLabel, exitSegment, toComposition);
        totalSendingFlow += s_ab;
      }
    }
    return totalSendingFlow;
  }

  /**
   * Collect the accepted flow towards an edge segment in the bush with the specified label, if not present, zero flow is returned
   * 
   * @param edgeSegment           to collect sending flow towards to
   * @param compositionLabel      to filter by
   * @param flowAcceptanceFactors to convert sending flow to accepted flow
   * @return bush sending flow found
   */
  public double getTotalAcceptedFlowToPcuH(final EdgeSegment edgeSegment, final BushFlowLabel compositionLabel, double[] flowAcceptanceFactors) {
    if (!hasFlowCompositionLabel(edgeSegment, compositionLabel)) {
      return 0;
    }

    if (edgeSegment.getUpstreamVertex() instanceof Centroid) {
      /* no preceding link segments, so same as what is being sent out of the segment */
      return getTotalSendingFlowFromPcuH(edgeSegment, compositionLabel);
    }

    double totalAcceptedFlow = 0;
    for (var entrySegment : edgeSegment.getUpstreamVertex().getEntryEdgeSegments()) {
      var fromLabels = linkSegmentCompositionLabels.get(entrySegment);
      if (fromLabels == null) {
        continue;
      }
      for (var fromLabel : fromLabels) {
        double s_ab = getTurnSendingFlowPcuH(entrySegment, fromLabel, edgeSegment, compositionLabel);
        double v_ab = s_ab * flowAcceptanceFactors[(int) entrySegment.getId()];
        totalAcceptedFlow += v_ab;
      }
    }
    return totalAcceptedFlow;
  }

  /**
   * Check if entry exists
   * 
   * @param fromSegment of turn
   * @param toSegment   of turn
   * @return true when present, false otherwise
   */
  public boolean containsTurnSendingFlow(final EdgeSegment fromSegment, final EdgeSegment toSegment) {
    var fromLabels = linkSegmentCompositionLabels.get(fromSegment);
    if (fromLabels == null) {
      return false;
    }
    for (var fromComposition : fromLabels) {
      for (var exitSegment : fromSegment.getDownstreamVertex().getExitEdgeSegments()) {
        var toLabels = linkSegmentCompositionLabels.get(exitSegment);
        if (toLabels == null) {
          continue;
        }
        for (var toComposition : toLabels) {
          if (containsTurnSendingFlow(fromSegment, fromComposition, exitSegment, toComposition)) {
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
  public boolean containsTurnSendingFlow(final EdgeSegment fromSegment, BushFlowLabel fromComposition, final EdgeSegment toSegment, BushFlowLabel toComposition) {
    return getTurnSendingFlowPcuH(fromSegment, fromComposition, toSegment, toComposition) > 0;
  }

  /**
   * Collect the splitting rates for a given link segment and composition. Splitting rates are based on the current (labelled) turn sending flows s_ab. In case no flows are present
   * for the given composition label, zero splitting rates for all turns are returned.
   * 
   * @param fromSegment to collect bush splitting rates for
   * @return splitting rates in primitive array in order of which one iterates over the outgoing edge segments of the downstream from segment vertex
   */
  public double[] getSplittingRates(final EdgeSegment fromSegment) {
    var exitEdgeSegments = fromSegment.getDownstreamVertex().getExitEdgeSegments();

    /* determining number of edge segment is costly, instead use edges (which is larger or equal) and then copy result */
    double[] splittingRates = new double[fromSegment.getDownstreamVertex().getNumberOfEdges()];

    double totalSendingFlow = 0;
    int index = 0;
    for (var exitSegment : exitEdgeSegments) {
      double s_ab = getTurnSendingFlowPcuH(fromSegment, exitSegment);
      splittingRates[index++] = s_ab;
      totalSendingFlow += s_ab;
    }
    ArrayUtils.divideBy(splittingRates, totalSendingFlow, 0);

    /* truncate */
    Arrays.copyOf(splittingRates, index);
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
  public double[] getSplittingRates(EdgeSegment fromSegment, BushFlowLabel fromLabel, BushFlowLabel toLabel) {
    var exitEdgeSegments = fromSegment.getDownstreamVertex().getExitEdgeSegments();

    /* determining number of edge segment is costly, instead use edges (which is larger or equal) and then copy result */
    double[] splittingRates = new double[fromSegment.getDownstreamVertex().getNumberOfEdges()];

    double totalSendingFlow = 0;
    int index = 0;
    for (var exitSegment : exitEdgeSegments) {
      double s_ab = getTurnSendingFlowPcuH(fromSegment, fromLabel, exitSegment, toLabel);
      splittingRates[index++] = s_ab;
      totalSendingFlow += s_ab;
    }

    ArrayUtils.divideBy(splittingRates, totalSendingFlow, 0);
    /* truncate */
    Arrays.copyOf(splittingRates, index);
    return splittingRates;
  }

  /**
   * Collect the bush splitting rates for a given incoming edge segment and entry label. If no flow exits, no splitting rates are returned in the map
   * 
   * @param fromSegment to use
   * @param fromLabel   to use
   * @return splitting rates in multikey map where the key is the combination of exit segment and exit label and the value is the portion of the entry segment entry label flow
   *         directed to it
   */
  public MultiKeyMap<Object, Double> getSplittingRates(EdgeSegment fromSegment, BushFlowLabel fromLabel) {
    var exitEdgeSegments = fromSegment.getDownstreamVertex().getExitEdgeSegments();

    /*
     * Note: flow/label removal below threshold is done when shifting flows. Splitting rates just follow so no precision threshold applied here
     */
    MultiKeyMap<Object, Double> splittingRatesByExitSegmentLabel = new MultiKeyMap<Object, Double>();
    double totalSendingFlow = getTotalSendingFlowFromPcuH(fromSegment, fromLabel);
    if (totalSendingFlow <= 0) {
      return splittingRatesByExitSegmentLabel;
    }

    for (var exitSegment : exitEdgeSegments) {
      var toLabels = getFlowCompositionLabels(exitSegment);
      if (toLabels == null) {
        continue;
      }
      for (var toLabel : toLabels) {
        double s_ab = getTurnSendingFlowPcuH(fromSegment, fromLabel, exitSegment, toLabel);
        if (s_ab > 0) {
          splittingRatesByExitSegmentLabel.put(exitSegment, toLabel, s_ab / totalSendingFlow);
        }
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
    if (turnSendingFlow > 0) {
      double totalSendingFlow = getTotalSendingFlowFromPcuH(fromSegment);
      if (totalSendingFlow < turnSendingFlow) {
        LOGGER.severe(String.format("Total sending flow (%.10f) smaller than turn (%s,%s) sending flow (%.10f), this shouldn't happen", totalSendingFlow, fromSegment.getXmlId(),
            toSegment.getXmlId(), turnSendingFlow));
      }
      return turnSendingFlow / totalSendingFlow;
    } else {
      return 0;
    }
  }

  /**
   * Collect the bush splitting rate on the given turn for a given label. This might be 0, or 1, but cna also be something in between in case the label splits off in multiple
   * directions
   * 
   * @param fromSegment    to use
   * @param toSegment      to use
   * @param entryExitLabel label to be used for both entry and exit of the turn
   * @return found splitting rate, in case the turn is not used, 0 is returned
   */
  public double getSplittingRate(EdgeSegment fromSegment, EdgeSegment toSegment, BushFlowLabel entryExitLabel) {
    double turnSendingFlow = getTurnSendingFlowPcuH(fromSegment, entryExitLabel, toSegment, entryExitLabel);
    if (turnSendingFlow > 0) {
      double totalSendingFlow = getTotalSendingFlowFromPcuH(fromSegment, entryExitLabel);
      if (totalSendingFlow < turnSendingFlow) {
        LOGGER.severe(String.format("Total sending flow (%.10f) smaller than turn (%s,%s) sending flow (%.10f) for label (%d,%d), this shouldn't happen", totalSendingFlow,
            fromSegment.getXmlId(), toSegment.getXmlId(), entryExitLabel.getLabelId(), entryExitLabel.getLabelId(), turnSendingFlow));
      }
      return turnSendingFlow / getTotalSendingFlowFromPcuH(fromSegment, entryExitLabel);
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
  public TreeSet<BushFlowLabel> getFlowCompositionLabels(final EdgeSegment edgeSegment) {
    return this.linkSegmentCompositionLabels.get(edgeSegment);
  }

  /**
   * Verify if the edge segment has any flow composition labels registered on it
   * 
   * @param edgeSegment to verify
   * @return true when present, false otherwise
   */
  public boolean hasFlowCompositionLabel(final EdgeSegment edgeSegment) {
    Set<BushFlowLabel> labels = getFlowCompositionLabels(edgeSegment);
    return labels != null && !labels.isEmpty();
  }

  /**
   * Verify if the edge segment has the flow composition label provided
   * 
   * @param edgeSegment      to verify
   * @param compositionLabel to verify
   * @return true when present, false otherwise
   */
  public boolean hasFlowCompositionLabel(final EdgeSegment edgeSegment, final BushFlowLabel compositionLabel) {
    var labels = getFlowCompositionLabels(edgeSegment);
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
   * @return the amount of flow that was relabeled
   */
  public double relabel(EdgeSegment fromSegment, BushFlowLabel oldFromLabel, EdgeSegment toSegment, BushFlowLabel oldToLabel, BushFlowLabel newFromToLabel) {
    return relabel(fromSegment, oldFromLabel, toSegment, oldToLabel, newFromToLabel, newFromToLabel);
  }

  /**
   * Relabel the from label of existing flow from one composition from-to combination to a new from-to label
   * 
   * @param fromSegment  from segment of turn
   * @param oldFromLabel from composition label to replace
   * @param toSegment    to segment of turn
   * @param toLabel      to composition label
   * @param newFromLabel label to replace flow with
   * @return the amount of flow that was relabeled
   */
  public double relabelFrom(EdgeSegment fromSegment, BushFlowLabel oldFromLabel, EdgeSegment toSegment, BushFlowLabel toLabel, BushFlowLabel newFromLabel) {
    return relabel(fromSegment, oldFromLabel, toSegment, toLabel, newFromLabel, toLabel);
  }

  /**
   * Relabel the to label of existing flow from one composition from-to combination to a new from-to label
   * 
   * @param fromSegment from segment of turn
   * @param fromLabel   from composition label
   * @param toSegment   to segment of turn
   * @param oldToLabel  to composition label to replace
   * @param newToLabel  label to replace flow with
   * @return the amount of flow that was relabeled
   */
  public double relabelTo(EdgeSegment fromSegment, BushFlowLabel fromLabel, EdgeSegment toSegment, BushFlowLabel oldToLabel, BushFlowLabel newToLabel) {
    return relabel(fromSegment, fromLabel, toSegment, oldToLabel, fromLabel, newToLabel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BushTurnData clone() {
    return new BushTurnData(this);
  }

  /**
   * Verify if any turn flows have been registered
   * 
   * @return true if so, false otherwise
   */
  public boolean hasTurnFlows() {
    return compositionTurnSendingFlows.isEmpty();
  }

}

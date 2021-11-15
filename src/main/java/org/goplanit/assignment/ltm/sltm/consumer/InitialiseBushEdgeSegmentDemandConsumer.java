package org.goplanit.assignment.ltm.sltm.consumer;

import java.util.function.Consumer;

import org.goplanit.assignment.ltm.sltm.Bush;
import org.goplanit.assignment.ltm.sltm.BushFlowCompositionLabel;
import org.goplanit.assignment.ltm.sltm.BushTurnData;
import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.math.Precision;

/**
 * Initialise the bush for a given origin/destination with the shortest path being offered from the destination to the origin via the provided edge segments in the callback.
 * <p>
 * Add the edge segments to the bush and update the turn sending flow accordingly.
 * <p>
 * Consumer can be reused for multiple destinations by updating the destination and demand that goes with it.
 * 
 * @author markr
 *
 */
public class InitialiseBushEdgeSegmentDemandConsumer implements Consumer<EdgeSegment> {

  /** the bush to initialise */
  private final Bush originBush;

  /** current destination at hand */
  @SuppressWarnings("unused")
  private DirectedVertex currentDestination;

  /** od demand to apply to all edge segments */
  private Double originDestinationDemandPcuH;

  /** succeeding edge segment we track for state to correctly deal with link segment by link segment call backs */
  private EdgeSegment succeedingEdgeSegment;

  /** The label that is currently in use to uniquely identify the flow composition */
  private BushFlowCompositionLabel currentCompositionLabel;

  /**
   * When there exists a mergedWithLabel the destination flow has merged with earlier destination flow(s). This indicated we must relabel the earlier flows upon merging as well as
   * our own destination flow using a new label. The other destination flow label we encounter is tracked here to be able to correctly update the flow while traversing the tree in
   * backward fashion
   */
  private BushFlowCompositionLabel mostRecentMergedWithLabel;

  private final BushTurnData bushTurnData;

  /**
   * Reset to prep for next destination (if any)
   */
  private void reset() {
    this.succeedingEdgeSegment = null;
    this.originDestinationDemandPcuH = null;
    this.currentDestination = null;
    this.mostRecentMergedWithLabel = null;
  }

  /**
   * Relabel the existing turn flow of previous composition from old-to-old label to current-to-old label
   * 
   * @param edgeSegment incoming segment to relabel for
   */
  private void relabelDivergingFlow(final EdgeSegment edgeSegment) {
    for (EdgeSegment exitSegment : edgeSegment.getDownstreamVertex().getExitEdgeSegments()) {
      double flowToRelabel = bushTurnData.getTurnSendingFlowPcuH(edgeSegment, mostRecentMergedWithLabel, exitSegment, mostRecentMergedWithLabel);
      if (Precision.isPositive(flowToRelabel)) {
        bushTurnData.relabel(edgeSegment, mostRecentMergedWithLabel, exitSegment, mostRecentMergedWithLabel, currentCompositionLabel);
      }
    }
  }

  /**
   * Constructor
   * 
   * @param originBush to use
   */
  public InitialiseBushEdgeSegmentDemandConsumer(final Bush originBush, final BushTurnData bushTurnData) {
    this.originBush = originBush;
    this.bushTurnData = bushTurnData;
    reset();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void accept(EdgeSegment edgeSegment) {

    BushFlowCompositionLabel succeedingFlowCompositionLabel = currentCompositionLabel;

    /*
     * when a preceding destination already used the link segment, we must now trigger relabelling since the composition changes due to diverging flows downstream
     */
    if (bushTurnData.hasFlowCompositionLabels(edgeSegment)) {
      BushFlowCompositionLabel newMergingLabel = bushTurnData.getFlowCompositionLabels(edgeSegment).iterator().next(); // only single label can be present
      if (mostRecentMergedWithLabel == null) {
        this.mostRecentMergedWithLabel = newMergingLabel;
        this.currentCompositionLabel = originBush.createFlowCompositionLabel();
        relabelDivergingFlow(edgeSegment);
      } else if (!bushTurnData.hasFlowCompositionLabel(edgeSegment, mostRecentMergedWithLabel)) {
        /*
         * Now the earlier flow merges (with another label) rather than we merging with earlier flow. Therefore, we can now switch our current label to the merging label while only
         * relabelling the outgoing label from the earlier merged flow label to our current label while adding the turn flow of this OD.
         */
        this.currentCompositionLabel = newMergingLabel;
        relabelDivergingFlow(edgeSegment);
        /*
         * reset because both this flow and other composition flow are both merging with existing flow composition label, we can reuse this label as our own since it now takes on a
         * different (but still valid) meaning and from this point onward both labels are in sync already so no need for relabeling any further
         */
        mostRecentMergedWithLabel = null;
      } else {
        /* continuing along the same preceding composition, i.e. no diverging of flows (in downstream direction) at this node by earlier composition here, do nothing */
      }
    }

    if (succeedingEdgeSegment != null) {

      /* must relabel existing flow */
      if (mostRecentMergedWithLabel != null) {
        bushTurnData.relabel(edgeSegment, mostRecentMergedWithLabel, succeedingEdgeSegment, mostRecentMergedWithLabel, currentCompositionLabel);
      }

      /* add new destination flow */
      bushTurnData.addTurnSendingFlow(edgeSegment, currentCompositionLabel, succeedingEdgeSegment, succeedingFlowCompositionLabel, originDestinationDemandPcuH);
    }
    succeedingEdgeSegment = edgeSegment;
  }

  /**
   * Update to next destination with accompanying demand. Demand is applied to all edge segments on the provided edge segments via the consumer callback
   * 
   * @param destination                 to set
   * @param originDestinationDemandPcuH total travel demand of the OD combination
   */
  public void setDestination(final DirectedVertex destination, double originDestinationDemandPcuH) {
    reset();

    this.currentDestination = destination;
    this.originDestinationDemandPcuH = originDestinationDemandPcuH;
    this.originBush.addOriginDemandPcuH(originDestinationDemandPcuH);

    this.currentCompositionLabel = this.originBush.createFlowCompositionLabel();
  }

}

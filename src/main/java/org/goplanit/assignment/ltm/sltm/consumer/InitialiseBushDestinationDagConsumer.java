package org.goplanit.assignment.ltm.sltm.consumer;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.print.attribute.standard.Destination;

import org.goplanit.assignment.ltm.sltm.Bush;
import org.goplanit.assignment.ltm.sltm.BushFlowCompositionLabel;
import org.goplanit.graph.directed.acyclic.ACyclicSubGraph;
import org.goplanit.graph.directed.acyclic.ACyclicSubGraphImpl;
import org.goplanit.utils.functionalinterface.TriConsumer;
import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.math.Precision;

/**
 * Initialise the origin sLTM bush by including the DAGs for each origin-destination. while adding make sure the appropriate labelling is set to be able to separate overalpping merging and 
 * diverging flows. Whenever an o-d is not a single path but comprises multiple (implicit) paths, we split the OD demand proportionally
 * <p>
 * Add the edge segments to the bush and update the turn sending flow accordingly.
 * <p>
 * Consumer can be reused for multiple destinations by updating the destination and demand that goes with it.
 * 
 * @author markr
 *
 */
public class InitialiseBushDestinationDagConsumer implements TriConsumer<DirectedVertex, Double, ACyclicSubGraph> {

  /** the bush to initialise */
  private final Bush originBush;
  
  /** the current destination vertex's DAG to incorporate in the origin bush */
  private ACyclicSubGraph currentDag; 

  /**
   * When there exists a mergedWithLabel the destination flow has merged with earlier destination flow(s). This indicated we must relabel the earlier flows upon merging as well as
   * our own destination flow using a new label. The other destination flow label we encounter is tracked here to be able to correctly update the flow while traversing the tree in
   * backward fashion
   */
  private BushFlowCompositionLabel mostRecentMergedWithLabel;

  /**
   * Reset to prep for next destination (if any)
   */
  private void reset() {
    this.mostRecentMergedWithLabel = null;
    this.currentDag = null;
  }

//  /**
//   * Relabel the existing turn flow of previous composition from old-to-old label to current-to-old label
//   * 
//   * @param edgeSegment incoming segment to relabel for
//   */
//  private void relabelDivergingFlow(final EdgeSegment edgeSegment) {
//    for (var exitSegment : edgeSegment.getDownstreamVertex().getExitEdgeSegments()) {
//      double flowToRelabel = originBush.getTurnSendingFlow(edgeSegment, mostRecentMergedWithLabel, exitSegment, mostRecentMergedWithLabel);
//      if (Precision.positive(flowToRelabel)) {
//        originBush.relabelFrom(edgeSegment, mostRecentMergedWithLabel, exitSegment, mostRecentMergedWithLabel, currentCompositionLabel);
//      }
//    }
//  }
  
  private void processVertex(final DirectedVertex vertex, final EdgeSegment succeedingEdgeSegment, final BushFlowCompositionLabel currentCompositionLabel, final Double remainingOrigindemandPcuH) {
    
    var labelToUse = currentCompositionLabel;
    
    int numUsedEntrySegments = 0;
    for(var entryEdgeSegment : vertex.getEntryEdgeSegments()) {
      if(currentDag.containsEdgeSegment(entryEdgeSegment)) {
        ++numUsedEntrySegments;
      }
    }
    
    double entrySegmentProportionalDemand = remainingOrigindemandPcuH/numUsedEntrySegments;
    for(var entryEdgeSegment : vertex.getEntryEdgeSegments()) {
      if(!currentDag.containsEdgeSegment(entryEdgeSegment)) {
        continue;
      }

      
      // CONTINUE HERE
//      TODO:
//      
//        We cannot label the way we do with AON, because due to merging and diverging within a Destination.class 
//        
//        IDEA:
//          
//         - label from root instead
//         - whenever diverging create NO new labels if al used exits are labelled
//         - whenever there exists one used exit without a label, create new labels for ALL exits because it is guaranteed that this "PAS" does not overlap with any existing PAS and therefore
//           they follows different paths with possibly different reductions which require unique labels to be able to track the flow corectly.
        
// OLD      
//      /*
//       * when a preceding destination already used the link segment, we must now trigger relabelling since the composition changes due to diverging flows downstream
//       */
//      if (originBush.hasFlowCompositionLabel(entryEdgeSegment)) {
//        var newMergingLabel = originBush.getFlowCompositionLabels(entryEdgeSegment).iterator().next(); // only single label can be present
//        if (mostRecentMergedWithLabel == null) {
//          this.mostRecentMergedWithLabel = newMergingLabel;
//          labelToUse = originBush.createFlowCompositionLabel();
//          relabelDivergingFlow(entryEdgeSegment);
//        } else if (!originBush.hasFlowCompositionLabel(edgeSegment, mostRecentMergedWithLabel)) {
//          /*
//           * Now the earlier flow merges (with another label) rather than we merging with earlier flow. Therefore, we can now switch our current label to the merging label while only
//           * relabelling the outgoing label from the earlier merged flow label to our current label while adding the turn flow of this OD.
//           */
//          this.currentCompositionLabel = newMergingLabel;
//          relabelDivergingFlow(edgeSegment);
//          /*
//           * reset because both this flow and other composition flow are both merging with existing flow composition label, we can reuse this label as our own since it now takes on a
//           * different (but still valid) meaning and from this point onward both labels are in sync already so no need for relabeling any further
//           */
//          mostRecentMergedWithLabel = null;
//        } else {
//          /* continuing along the same preceding composition, i.e. no diverging of flows (in downstream direction) at this node by earlier composition here, do nothing */
//        }
//      }
//
//      if (succeedingEdgeSegment != null) {
//
//        /* must relabel existing flow */
//        if (mostRecentMergedWithLabel != null) {
//          originBush.relabel(edgeSegment, mostRecentMergedWithLabel, succeedingEdgeSegment, mostRecentMergedWithLabel, currentCompositionLabel);
//        }
//
//        /* add new flow */
//        originBush.addTurnSendingFlow(entryEdgeSegment, currentCompositionLabel, succeedingEdgeSegment, succeedingFlowCompositionLabel, originDestinationDemandPcuH);
//      }
      
      /* proceed to the next vertex */
      processVertex(entryEdgeSegment.getUpstreamVertex(), entryEdgeSegment, currentCompositionLabel, entrySegmentProportionalDemand);    
    }
  }

  /**
   * Constructor
   * 
   * @param originBush to use
   */
  public InitialiseBushDestinationDagConsumer(final Bush originBush) {
    this.originBush = originBush;
    reset();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void accept(final DirectedVertex currentDestination, final Double originDestinationDemandPcuH, final ACyclicSubGraph acyclicDirectedSubGraph) {
    
    reset();    

    originBush.addOriginDemandPcuH(originDestinationDemandPcuH);
    this.currentDag = acyclicDirectedSubGraph;
      
    /* start recursion */
    processVertex(currentDestination,null,this.originBush.createFlowCompositionLabel(),originDestinationDemandPcuH);    

  }

}

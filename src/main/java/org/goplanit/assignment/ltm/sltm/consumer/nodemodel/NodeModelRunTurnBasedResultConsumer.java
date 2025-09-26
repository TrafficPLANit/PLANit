package org.goplanit.assignment.ltm.sltm.consumer.nodemodel;

import org.goplanit.algorithms.nodemodel.NodeModel;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.ojalgo.array.Array1D;
import org.ojalgo.array.Array2D;

/**
 * Apply this to the result of a Tampere node model runTurnBased() for a particular node, it is invoked with the node
 * the model was invoked on, the resulting turn flow acceptance factors and the sending flow turn flows.
 * 
 * @author markr
 *
 */
public interface NodeModelRunTurnBasedResultConsumer {

  /**
   * A centroid node or non-blocking nodes are nodes where all flow is always accepted. For centroids these
   * can be destination incoming links, or for origin outgoing links the sending flows
   * do not come from turns but from origin zone (origin exit links). As such we present only the original link
   * segment sending flows in those cases.
   * 
   * @param node                   to use
   * @param linkSegmentSendingFlow to use. Note that these are the network wide sending flows by link segment id,
   *                               not localised for the node
   */
  public abstract void acceptNonBlockingLinkBasedResult(
      final DirectedVertex node, final double[] linkSegmentSendingFlow);

  /**
   * Result of a node model update
   * 
   * @param node                  to use
   * @param turnFlowAcceptanceFactors resulting from the update, localised for the node based on its entry segment -
   *                                  exit segment iterator order
   * @param nodeModel             that was applied
   */
  public abstract void acceptTurnBasedResult(
      final DirectedVertex node, final Array2D<Double> turnFlowAcceptanceFactors, final NodeModel nodeModel);

}

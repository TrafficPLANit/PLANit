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
   * At a centroid node or a non-blocking node all flow is always accepted. At a centroid the links are destination
   * incoming links, or origin outgoing links whose sending flows come from the origin zone (origin exit links), not
   * from turns. So in those cases only the original link segment sending flows are given.
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

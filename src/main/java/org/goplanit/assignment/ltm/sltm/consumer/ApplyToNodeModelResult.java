package org.goplanit.assignment.ltm.sltm.consumer;

import org.goplanit.utils.graph.directed.DirectedVertex;
import org.ojalgo.array.Array1D;
import org.ojalgo.array.Array2D;

/**
 * Apply this to the result of a Tampere node model execution for a particular node, it is invoked with the node the model was invoked on, the resulting flow acceptance factors and
 * the sending flow turn flows (rows are entry link segments, columns outgoing link segments).
 * 
 * @author markr
 *
 */
public interface ApplyToNodeModelResult {

  /**
   * A centroid node is a special node where all flow is always accepted (destination incoming links), or sending flows do not come from turns but from origin zone (origin exit
   * links). As such we present only the original link segment sending flows
   * 
   * @param node                    to use
   * @param linkSegmentSendingFlows to use. Note that these are the network wide sending flows by link segment id, not localised for the node
   */
  public abstract void consumeCentroidResult(final DirectedVertex node, final double[] linkSegmentSendingFlow);

  /**
   * Result of a node model update
   * 
   * @param node                  to use
   * @param flowAcceptanceFactors resulting from the update, localised for the node based on its entry segment iterator order
   * @param turnSendingFlows      that were used, localised for the node based on its entry segment, exit segment iterator order
   */
  public abstract void consumeRegularResult(final DirectedVertex node, final Array1D<Double> flowAcceptanceFactors, final Array2D<Double> turnSendingFlows);

}

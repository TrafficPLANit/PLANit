package org.goplanit.assignment.ltm.sltm.consumer;

import org.goplanit.algorithms.nodemodel.NodeModel;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.ojalgo.array.Array1D;

/**
 * A functional class that consumes the result of a node model update in order to obtain the the flow acceptance factor
 * for a given entry link
 * 
 * @author markr
 *
 */
public class NMRCollectEntryLinkFlowAcceptanceFactorConsumer implements NodeModelRunResultConsumer {

  private double flowAcceptanceFactor;

  /**
   * entry segment to find most restricting out link for (if any)
   */
  private EdgeSegment entrySegment;

  /**
   * Constructor
   *
   * @param entrySegment to collect most restricting out link for
   */
  public NMRCollectEntryLinkFlowAcceptanceFactorConsumer(final EdgeSegment entrySegment) {
    this.flowAcceptanceFactor = -1;
    this.entrySegment = entrySegment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void acceptNonBlockingLinkBasedResult(final DirectedVertex node, double[] sendingFlows) {
    this.flowAcceptanceFactor = 1;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void acceptTurnBasedResult(
      final DirectedVertex node, final Array1D<Double> flowAcceptanceFactors, final NodeModel nodeModel) {

    /* match entry segment to index */
    var iter = node.getEntryEdgeSegments().iterator();
    int index = 0;
    while (iter.hasNext() && !iter.next().idEquals(entrySegment)) {
      ++index;
    }

    /* collect acceptance factor */
    this.flowAcceptanceFactor = flowAcceptanceFactors.get(index);
  }


  public double getEntrySegmentFlowAcceptanceFactor() {
    return flowAcceptanceFactor;
  }

}

package org.goplanit.assignment.ltm.sltm.consumer;

import org.goplanit.algorithms.nodemodel.NodeModel;
import org.goplanit.algorithms.nodemodel.TampereNodeModel;
import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.ojalgo.array.Array1D;

/**
 * A functional class that consumes the result of a node model update in order to obtain the most restrictive out link for the given in link provided
 * 
 * @author markr
 *
 */
public class NMRCollectMostRestrictingTurnConsumer implements ApplyToNodeModelResult {

  /** track most restricting out link by in link */
  private EdgeSegment mostRestrictingOutSegment;

  /**
   * entry segment to find most restricting out link for (if any)
   */
  private EdgeSegment entrySegment;

  /**
   * Constructor
   * 
   * @param entrySegment to collect most restricting out link for
   */
  public NMRCollectMostRestrictingTurnConsumer(final EdgeSegment entrySegment) {
    this.mostRestrictingOutSegment = null;
    this.entrySegment = entrySegment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void acceptNonBlockingLinkBasedResult(final DirectedVertex node, double[] sendingFlows) {
    // do nothing, nothing restricted
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void acceptTurnBasedResult(final DirectedVertex node, final Array1D<Double> flowAcceptanceFactors, final NodeModel nodeModel) {

    /* match entry segment to index */
    var iter = node.getEntryEdgeSegments().iterator();
    int index = 0;
    while (iter.hasNext() && !iter.next().idEquals(entrySegment)) {
      ++index;
    }

    /* collect out index by in index */
    // TODO: should not cast directly
    Integer outSegmentIndex = ((TampereNodeModel) nodeModel).getMostRestrictedOutLinkByInLink().get(index);
    if (outSegmentIndex == null) {
      return;
    }

    /* map out index to object */
    iter = node.getExitEdgeSegments().iterator();
    index = 0;
    while (iter.hasNext()) {
      var outSegment = iter.next();
      if (index++ == outSegmentIndex) {
        mostRestrictingOutSegment = outSegment;
        break;
      }
    }
  }

  public boolean hasMostRestrictingOutSegment() {
    return mostRestrictingOutSegment != null;
  }

  public EdgeSegment getMostRestrictingOutSegment() {
    return mostRestrictingOutSegment;
  }

}

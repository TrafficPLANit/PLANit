package org.goplanit.assignment.ltm.sltm.loading;

import org.goplanit.utils.graph.directed.DirectedVertex;

import java.util.BitSet;

/**
 * Store the splitting rates used during sLTM loading updates (Step 1 and 5). In this implementation we only track splitting rates of explicitly registered node. This is compatible
 * with the basic PointQueue solution method where we do not require any information of nodes that are not potentially blocking. It requires the less memory than the other approach
 * where we track all splitting rates of all used nodes.
 * 
 * @author markr
 *
 */
public abstract class SplittingRateDataBase implements SplittingRateData {

  /**
   * tracked nodes that are also marked as potentially blocking for previous iteration
   */
  private BitSet prevIterationPotentiallyBlockingNodes;

  /**
   * Constructor
   *
   * @param numberOfVertices to expect at most
   */
  public SplittingRateDataBase(int numberOfVertices) {
    this.prevIterationPotentiallyBlockingNodes = new BitSet(numberOfVertices);
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isPrevIterationPotentiallyBlocking(DirectedVertex nodeToVerify) {
    return prevIterationPotentiallyBlockingNodes.get((int) nodeToVerify.getId());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void resetPrevIterationPotentiallyBlockingNodes() {
    prevIterationPotentiallyBlockingNodes.clear();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void initialisePrevIterationData(SplittingRateData prevIterationSplittingRateData) {
    prevIterationPotentiallyBlockingNodes.clear();
    for(DirectedVertex vertex : prevIterationSplittingRateData.getTrackedNodes()){
      if(prevIterationSplittingRateData.isPotentiallyBlocking(vertex)){
        prevIterationPotentiallyBlockingNodes.set((int) vertex.getId());
        registerTrackedNode(vertex);
      }
    }
  }

}

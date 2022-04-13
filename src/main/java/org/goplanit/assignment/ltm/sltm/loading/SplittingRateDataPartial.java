package org.goplanit.assignment.ltm.sltm.loading;

import java.util.BitSet;
import java.util.TreeSet;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.ojalgo.array.Array1D;

/**
 * Store the splitting rates used during sLTM loading updates (Step 1 and 5). In this implementation we only track splitting rates of explicitly registered node. This is compatible
 * with the basic PointQueue solution method where we do not require any information of nodes that are not potentially blocking. It requires the less memory than the other approach
 * where we track all splitting rates of all used nodes.
 * 
 * @author markr
 *
 */
public class SplittingRateDataPartial implements SplittingRateData {

  /**
   * Nodes that are tracked to maintain their splitting rates available which might or might not be also potentially blocking
   */
  private final TreeSet<DirectedVertex> trackedNodes;

  /**
   * tracked nodes that are also marked as potentially blocking
   */
  private final BitSet potentiallyBlockingNodes;

  /**
   * Splitting rates per potentially blocking node, entry, exit link combination where the key is the combined hash of the node and entry edge segment ids
   */
  private final MultiKeyMap<Object, Array1D<Double>> splittingRates = new MultiKeyMap<>();

  /**
   * Register splitting rates for given combination of node and entry segment
   * 
   * @param potentiallyBlockingNode to use
   * @param entrySegment            to use
   */
  private void registerSplittingRates(DirectedVertex potentiallyBlockingNode, EdgeSegment entrySegment) {
    var result = splittingRates.get(potentiallyBlockingNode, entrySegment);
    if (result == null) {
      splittingRates.put(potentiallyBlockingNode, entrySegment, Array1D.PRIMITIVE64.makeZero(potentiallyBlockingNode.getNumberOfExitEdgeSegments()));
    }
  }

  /**
   * Constructor
   * 
   * @param numberOfVertices to expect at most
   */
  public SplittingRateDataPartial(int numberOfVertices) {
    super();
    this.trackedNodes = new TreeSet<DirectedVertex>();
    this.potentiallyBlockingNodes = new BitSet(numberOfVertices);
  }

  /**
   * Register a potentially blocking node so we not only track its splitting rates but also mark it as potentially blocking. This see to it that it is considered for node model
   * updates which in turn might impact its splitting rates in a network setting
   * 
   * @param potentiallyBlockingNode mark as potentially blocking (and track it if not already done so)
   */
  public void registerPotentiallyBlockingNode(final DirectedVertex potentiallyBlockingNode) {
    int id = (int) potentiallyBlockingNode.getId();
    if (!potentiallyBlockingNodes.get(id)) {
      potentiallyBlockingNodes.set(id);
      registerTrackedNode(potentiallyBlockingNode); // in case not already done so
    }
  }

  /**
   * Register a node so we are able to track its splitting rates (and turn flows) during loading
   * 
   * @param trackNode node to track splitting rates and (turn) sending flows for
   */
  public void registerTrackedNode(final DirectedVertex trackNode) {
    if (!trackedNodes.contains(trackNode)) {
      trackedNodes.add(trackNode);
      for (var entrySegment : trackNode.getEntryEdgeSegments()) {
        registerSplittingRates(trackNode, entrySegment);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isTracked(final DirectedVertex nodeToVerify) {
    return trackedNodes.contains(nodeToVerify);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isPotentiallyBlocking(final DirectedVertex nodeToVerify) {
    return potentiallyBlockingNodes.get((int) nodeToVerify.getId());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TreeSet<DirectedVertex> getTrackedNodes() {
    return trackedNodes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Array1D<Double> getSplittingRates(final EdgeSegment entrySegment) {
    return splittingRates.get(entrySegment.getDownstreamVertex(), entrySegment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void resetTrackedNodes() {
    trackedNodes.clear();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void resetPotentiallyBlockingNodes() {
    potentiallyBlockingNodes.clear();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void resetSplittingRates() {
    splittingRates.clear();
  }

}

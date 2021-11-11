package org.goplanit.assignment.ltm.sltm.loading;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.misc.HashUtils;
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
  private final Set<DirectedVertex> trackedNodes;

  /**
   * tracked nodes that are also marked as potentially blocking
   */
  private final BitSet potentiallyBlockingNodes;

  /**
   * Splitting rates per potentially blocking node, entry, exit link combination where the key is the combined hash of the node and entry edge segment ids
   */
  private final HashMap<Integer, Array1D<Double>> splittingRates = new HashMap<Integer, Array1D<Double>>();

  /**
   * Register splitting rates for given combination of node and entry segment
   * 
   * @param potentiallyBlockingNode to use
   * @param entrySegment            to use
   */
  private void registerSplittingRates(DirectedVertex potentiallyBlockingNode, EdgeSegment entrySegment) {
    splittingRates.putIfAbsent(HashUtils.createCombinedHashCode(potentiallyBlockingNode.getId(), entrySegment.getId()),
        Array1D.PRIMITIVE64.makeZero(potentiallyBlockingNode.sizeOfExitEdgeSegments()));
  }

  /**
   * Constructor
   * 
   * @param numberOfVertices to expect at most
   */
  public SplittingRateDataPartial(int numberOfVertices) {
    super();
    this.trackedNodes = new HashSet<DirectedVertex>();
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
      for (EdgeSegment entrySegment : trackNode.getEntryEdgeSegments()) {
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
  public Set<DirectedVertex> getTrackedNodes() {
    return trackedNodes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Array1D<Double> getSplittingRates(final EdgeSegment entrySegment) {
    return splittingRates.get(HashUtils.createCombinedHashCode(entrySegment.getDownstreamVertex().getId(), entrySegment.getId()));
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
package org.planit.assignment.ltm.sltm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.ojalgo.array.Array1D;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.directed.DirectedVertex;
import org.planit.utils.misc.HashUtils;

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
   * track potentially blocking nodes for which splitting rates must be tracked
   */
  private final Set<DirectedVertex> potentiallyBlockingNodes = new HashSet<DirectedVertex>();

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
   */
  public SplittingRateDataPartial() {
    super();
  }

  /**
   * Register a potentially blocking node so we are able to track its splitting rates (and turn flows) during loading
   * 
   * @param potentiallyBlockingNode to start tracking turn flows and splitting rates for
   */
  public void registerPotentiallyBlockingNode(DirectedVertex potentiallyBlockingNode) {
    if (!potentiallyBlockingNodes.contains(potentiallyBlockingNode)) {
      potentiallyBlockingNodes.add(potentiallyBlockingNode);
      for (EdgeSegment entrySegment : potentiallyBlockingNode.getEntryEdgeSegments()) {
        registerSplittingRates(potentiallyBlockingNode, entrySegment);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isTracked(DirectedVertex nodeToVerify) {
    return potentiallyBlockingNodes.contains(nodeToVerify);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<DirectedVertex> getTrackedNodes() {
    return potentiallyBlockingNodes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Array1D<Double> getSplittingRates(DirectedVertex node, EdgeSegment entrySegment) {
    return splittingRates.get(HashUtils.createCombinedHashCode(node.getId(), entrySegment.getId()));
  }

}

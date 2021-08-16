package org.planit.assignment.ltm.sltm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.ojalgo.array.Array1D;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.directed.DirectedVertex;
import org.planit.utils.misc.HashUtils;

/**
 * POJO to store the sLTM variables used for splitting rate updates (Step 1 and 5) in network loading. Note that because the node model implementation requires turn sending flows
 * rather than link sending flows and splitting rates we store turn sending flows instead of splitting rates. To allow for efficient updating of turn flows we keep track of turn
 * sending flows as a 2D array for each registered node (that is potentially blocking)
 * 
 * @author markr
 *
 */
public class SplittingRateData {

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
   * @param emptySegmentArray empty array used to initialize data stores
   */
  public SplittingRateData() {
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
   * Verify if node is registered as potentially blocking
   * 
   * @param nodeToVerify the node to verify
   * @return true when registered as potentially blocking, false otherwise
   */
  public boolean isPotentiallyBlocking(DirectedVertex nodeToVerify) {
    return potentiallyBlockingNodes.contains(nodeToVerify);
  }

  /**
   * Collect all registered potentially blocking nodes
   * 
   * @return registered potentially blocking nodes
   */
  public Set<DirectedVertex> getPotentiallyBlockingNodes() {
    return potentiallyBlockingNodes;
  }

  /**
   * Obtain the splitting rates (can be modified)
   * 
   * @param node         to obtain for
   * @param entrySegment to obtain for
   * @return currently set next splitting rates
   */
  public Array1D<Double> getSplittingRates(DirectedVertex node, EdgeSegment entrySegment) {
    return splittingRates.get(HashUtils.createCombinedHashCode(node.getId(), entrySegment.getId()));
  }

}

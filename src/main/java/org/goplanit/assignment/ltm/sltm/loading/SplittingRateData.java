package org.goplanit.assignment.ltm.sltm.loading;

import java.util.TreeSet;

import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.ojalgo.array.Array1D;

/**
 * Interface for the different implementations that track splitting rates during sLTM network loading.
 * <p>
 * We make a distinction by tracking node (turn) flows to be able to construct splitting rates and whether or not a node is potentially blocking. When a node is potentially
 * blocking we must always track its (turn) flows and splitting rates otherwise we cannot compute to what extent it is blocking. However, when it is not blocking (yet) we might
 * still be required to track it. For example when we want to be able to know how much flow is using a sub-path through the network used by - for example - a PAS in a bush-based
 * implementation of sLTM. For Path based implementations we only need to track nodes that are potentially blocking because there is no need to know the flow on sub-paths to be
 * able to determine how to distribute flows.
 * 
 * @author markr
 *
 */
public interface SplittingRateData {

  /**
   * Verify if node is registered as being tracked with splitting rates
   * 
   * @param nodeToVerify the node to verify
   * @return true when registered as tracked, false otherwise
   */
  public abstract boolean isTracked(DirectedVertex nodeToVerify);

  /**
   * Verify if node is registered as potentially blocking
   * 
   * @param nodeToVerify the node to verify
   * @return true when registered as potentially blocking, false otherwise
   */
  public abstract boolean isPotentiallyBlocking(DirectedVertex nodeToVerify);

  /**
   * Collect all registered potentially blocking nodes
   * 
   * @return registered potentially blocking nodes
   */
  public abstract TreeSet<DirectedVertex> getTrackedNodes();

  /**
   * Obtain the downstream splitting rates of given node entry segment (can be modified)
   * 
   * @param entrySegment to obtain for
   * @return currently set next splitting rates
   */
  public abstract Array1D<Double> getSplittingRates(EdgeSegment entrySegment);

  /**
   * Obtain the splitting rate of a given turn (non-modifiable)
   * 
   * @param entrySegment to obtain for
   * @param exitSegment  to obtain for
   * @return currently set next splitting rates, 0 if no information present
   */
  public default double getSplittingRate(EdgeSegment entrySegment, EdgeSegment exitSegment) {
    Array1D<Double> vertexSplittingRates = getSplittingRates(entrySegment);
    int index = 0;
    for (var currExitSegment : entrySegment.getDownstreamVertex().getExitEdgeSegments()) {
      if (currExitSegment.idEquals(exitSegment)) {
        return vertexSplittingRates.get(index);
      }
      ++index;
    }
    return 0;
  }

  /**
   * Reset registered tracked nodes
   */
  public abstract void resetTrackedNodes();

  /**
   * Reset registered potentially blocking nodes
   */
  public abstract void resetPotentiallyBlockingNodes();

  /**
   * Reset splitting rate data
   */
  public abstract void resetSplittingRates();

  /**
   * Reset all splitting rate data
   */
  public default void reset() {
    resetTrackedNodes();
    resetPotentiallyBlockingNodes();
    resetSplittingRates();
  }
}
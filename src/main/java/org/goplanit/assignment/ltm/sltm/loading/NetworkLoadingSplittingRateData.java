package org.goplanit.assignment.ltm.sltm.loading;

import java.util.TreeSet;

import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.ojalgo.array.Array1D;

/**
 * Interface for the different implementations that track splitting rates during sLTM network loading.
 * <p>
 * Tracking a node's (turn) flows, to construct its splitting rates, is kept apart from whether a node is potentially
 * blocking. A potentially blocking node must always be tracked, otherwise the extent to which it blocks cannot be
 * computed. A node not (yet) blocking may still need tracking, for example to know how much flow uses a sub-path
 * through the network, such as a PAS in a bush-based implementation of sLTM. Path based implementations only track
 * potentially blocking nodes, as they need not know the flow on sub-paths to determine how to distribute flows.
 * 
 * @author markr
 *
 */
public interface NetworkLoadingSplittingRateData {

  /**
   * Initialise the prevIteration splitting rate data on this instance based on the data from the previous iteration
   *
   * @param prevIterationSplittingRateData to use
   */
  public abstract void initialisePrevIterationData(NetworkLoadingSplittingRateData prevIterationSplittingRateData);

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
   * Verify if node is registered as potentially blocking in previous iteration
   *
   * @param nodeToVerify the node to verify
   * @return true when registered as potentially blocking in previous iteration, false otherwise
   */
  public abstract boolean isPrevIterationPotentiallyBlocking(DirectedVertex nodeToVerify);

  /**
   * Collect all registered potentially blocking nodes
   * 
   * @return registered potentially blocking nodes
   */
  public abstract TreeSet<DirectedVertex> getTrackedNodes();

  /**
   * Register a node for tracking and make sue splitting rates are available
   *
   * @param node to register
   */
  public abstract void registerTrackedNode(final DirectedVertex node);

  /**
   * Register a potentially blocking node so we not only track its splitting rates but also mark it as potentially
   * blocking. This see to it that it is considered for node model updates which in turn might impact its splitting
   * rates in a network setting
   *
   * @param potentiallyBlockingNode mark as potentially blocking (and track it if not already done so)
   */
  public abstract void registerPotentiallyBlockingNode(final DirectedVertex potentiallyBlockingNode);


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
    if(vertexSplittingRates == null){
      return 0;
    }

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
   * Reset registered potentially blocking nodes
   */
  public abstract void resetPrevIterationPotentiallyBlockingNodes();

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
    resetPrevIterationPotentiallyBlockingNodes();
    resetSplittingRates();
  }

}
package org.planit.assignment.ltm.sltm.loading;

import java.util.Set;

import org.ojalgo.array.Array1D;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.directed.DirectedVertex;

/**
 * Interface for the different implementations that track splitting rates during sLTM network loading
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
   * Collect all registered potentially blocking nodes
   * 
   * @return registered potentially blocking nodes
   */
  public abstract Set<DirectedVertex> getTrackedNodes();

  /**
   * Obtain the splitting rates (can be modified)
   * 
   * @param node         to obtain for
   * @param entrySegment to obtain for
   * @return currently set next splitting rates
   */
  public abstract Array1D<Double> getSplittingRates(DirectedVertex node, EdgeSegment entrySegment);
}
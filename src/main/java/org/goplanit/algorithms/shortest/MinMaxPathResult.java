package org.goplanit.algorithms.shortest;

import org.goplanit.utils.graph.Vertex;

/**
 * Implementation of the MinMaxPathResult interface
 * 
 * @author markr
 *
 */
public interface MinMaxPathResult extends ShortestPathResult {

  /**
   * Collect the cost to reach the given vertex from the reference starting point
   *
   * @param vertex to collect cost for
   * @return cost found
   */
  public abstract double getMinCostToReach(Vertex vertex);

  /**
   * Collect the cost to reach the given vertex from the reference starting point
   *
   * @param vertex to collect cost for
   * @return cost found
   */
  public abstract double getMaxCostToReach(Vertex vertex);

  /**
   * Switch state to minimum path results
   * 
   * @param flag when true switch to minimum paths, otherwise switch to maximum paths. Default is minimum paths
   */
  public abstract void setMinPathState(boolean flag);

  /**
   * Check if state is set to minimum path results
   *
   * @return flag
   */
  public abstract boolean isMinPathState();
}

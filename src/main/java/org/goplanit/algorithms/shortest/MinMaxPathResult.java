package org.goplanit.algorithms.shortest;

/**
 * Track results of a min-max path tree path execution allowing one to extract both minimum and maximum cost paths along the tree.
 * 
 * To determine which type is obtained, the user can switch the state from minimum to maximum and vice versa. The subsequent methods will take the state into account when providing
 * the results.
 * 
 * 
 * @author markr
 *
 */
public interface MinMaxPathResult extends ShortestPathResult {

  /**
   * Switch state to minimum path results
   * 
   * @param flag when true switch to minimum paths, otherwise switch to maximum paths. Default is minimum paths
   */
  public abstract void setMinPathState(boolean flag);

}

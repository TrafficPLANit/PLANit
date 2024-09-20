package org.goplanit.algorithms.shortest;

/**
 * Implementation of the MinMaxPathResult interface
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

  /**
   * Check if state is set to minimum path results
   *
   * @return flag
   */
  public abstract boolean isMinPathState();
}

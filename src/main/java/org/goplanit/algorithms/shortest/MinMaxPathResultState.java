package org.goplanit.algorithms.shortest;

/**
 * Track state of a min-max path tree result execution allowing one to extract either minimum and maximum cost paths along the tree.
 * 
 * 
 * @author markr
 *
 */
public interface MinMaxPathResultState {

  /**
   * Switch state to minimum path results
   * 
   * @param flag when true switch to minimum paths, otherwise switch to maximum paths. Default is minimum paths
   */
  public abstract void setMinPathState(boolean flag);

}

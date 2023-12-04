package org.goplanit.assignment.ltm.sltm;

import org.goplanit.utils.path.ManagedDirectedPath;

/**
 * Decorating a ManagedDirectedPathImpl instance to include hash and assigned probability in current iteration of
 * sLTM path based assignment, so it can be easily loaded onto the network.
 */
public interface StaticLtmDirectedPath extends ManagedDirectedPath {

  public abstract void setPathChoiceProbability(double probability);

  public abstract double getPathChoiceProbability();

  /**
   * Hashcode based solely on the directed link segments of this path
   *
   * @return the hashcode
   */
  public abstract int getLinkSegmentsOnlyHashCode();

  /**
   * Verify if the directed link segments of both paths are identical via their internal pre-generated hashcode
   *
   * @param toCompare other path to compare against
   * @return true when equal hashcode, false otherwise
   */
  public default boolean isPathLinkSegmentsEqual(StaticLtmDirectedPathImpl toCompare){
    return getLinkSegmentsOnlyHashCode() == toCompare.getLinkSegmentsOnlyHashCode();
  }

}

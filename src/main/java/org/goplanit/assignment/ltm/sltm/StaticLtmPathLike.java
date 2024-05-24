package org.goplanit.assignment.ltm.sltm;

import org.goplanit.utils.network.layer.physical.Movement;
import org.goplanit.utils.path.ManagedDirectedPath;

/**
 * Replacement for StaticLtmDirectedPath since this is supposed to be movement based for improvement
 * performance. Once done remove the staticLtmDirectedPath and its implementation
 */
public interface StaticLtmPathLike {

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

  /**
   * Access to raw array of movements in order of the path
   *
   * @return movements
   */
  public abstract Movement[] getMovements();

}

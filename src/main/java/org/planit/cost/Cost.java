package org.planit.cost;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.mode.Mode;

/**
 * Cost of an EdgeSegment
 * 
 * @author markr
 *
 */
public interface Cost<T extends EdgeSegment> {

  /**
   * Returns the cost of travel along an edge segment for a specified mode
   * 
   * @param mode        the specified mode of travel
   * @param edgeSegment the specified edge segment (which can be physical or virtual)
   * @return the cost of travel along the specified segment
   * @throws PlanItException the exception thrown when not available
   */
  public abstract double getSegmentCost(final Mode mode, final T edgeSegment) throws PlanItException;

}

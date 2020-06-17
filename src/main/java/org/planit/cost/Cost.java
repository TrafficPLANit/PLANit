package org.planit.cost;

import java.io.Serializable;

import org.planit.exceptions.PlanItException;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.network.physical.Mode;

/**
 * Cost of an EdgeSegment
 * 
 * @author markr
 *
 */
public interface Cost<T extends EdgeSegment> extends Serializable {

  /**
   * Returns the cost of travel along an edge segment for a specified mode
   * 
   * @param mode the specified mode of travel
   * @param edgeSegment the specified edge segment (which can be physical or virtual)
   * @return the cost of travel along the specified segment
   * @throws PlanItException the exception thrown when not available
   */
  public double getSegmentCost(Mode mode, T edgeSegment) throws PlanItException;

}

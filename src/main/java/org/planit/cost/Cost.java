package org.planit.cost;

import java.io.Serializable;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.mode.Mode;
import org.planit.utils.time.TimePeriod;

/**
 * Cost of an EdgeSegment
 * 
 * @author markr
 *
 */
public interface Cost<T extends EdgeSegment> extends Serializable {

  /**
   * Provide the cost calculation with information regarding the time period for which the cost is to be calculated
   * 
   * @param timePeriod to apply
   */
  public abstract void updateTimePeriod(final TimePeriod timePeriod);

  /**
   * Returns the cost of travel along an edge segment for a specified mode
   * 
   * @param mode        the specified mode of travel
   * @param edgeSegment the specified edge segment (which can be physical or virtual)
   * @return the cost of travel along the specified segment
   * @throws PlanItException the exception thrown when not available
   */
  public abstract double getSegmentCost(Mode mode, T edgeSegment) throws PlanItException;

}

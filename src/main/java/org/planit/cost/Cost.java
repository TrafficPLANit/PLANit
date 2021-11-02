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
   * Returns the generalised cost of travel along an edge segment for a specified mode
   * 
   * @param mode        the specified mode of travel
   * @param edgeSegment the specified edge segment (which can be physical or virtual)
   * @return the cost of travel along the specified segment
   * @throws PlanItException the exception thrown when not available
   */
  public abstract double getGeneralisedCost(final Mode mode, final T edgeSegment);

  /**
   * Returns the generalised cost of travel along an edge segment for a specified mode
   * 
   * @param mode        the specified mode of travel
   * @param edgeSegment the specified edge segment (which can be physical or virtual)
   * @return the cost of travel along the specified segment
   * @throws PlanItException the exception thrown when not available
   */
  public abstract double getTravelTimeCost(final Mode mode, final T edgeSegment);

  /**
   * Get the first derivative of the used travel time computation method towards the edge segment (in) flow rate in PCU per hour, i.e. dTraveltime//dFlow.
   * 
   * @param uncongested flag idicating if the provided flow is uncongested or congested flow, relevant when flow can represent multiple traffic states
   * @param mode        to use
   * @param linkSegment to use
   * @return the first derivative of travel time for a unit flow rate change in PCU per Hour
   */
  public abstract double getDTravelTimeDFlow(boolean uncongested, final Mode mode, final T edgeSegment);

}

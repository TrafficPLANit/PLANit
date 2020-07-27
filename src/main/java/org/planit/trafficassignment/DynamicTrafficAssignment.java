package org.planit.trafficassignment;

import org.planit.route.choice.RouteChoice;
import org.planit.utils.id.IdGroupingToken;

/**
 * Dynamic traffic assignment class for any traffic assignment model that adheres to being capacity constrained utilizing a fundamental diagram and underlying node model
 *
 * @author markr
 *
 */
public abstract class DynamicTrafficAssignment extends CapacityConstrainedAssignment {

  /** generated UID */
  private static final long serialVersionUID = 5518351010500386771L;

  /** the route choice methodology adopted */
  protected RouteChoice routeChoice;

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  protected DynamicTrafficAssignment(IdGroupingToken groupId) {
    super(groupId);
  }

  // getters - setters

  /**
   * set the route choice methodology as to be adopted by this traffic assignment
   * 
   * @param routeChoice route choce to set
   */
  public void setRouteChoice(final RouteChoice routeChoice) {
    this.routeChoice = routeChoice;
  }

}

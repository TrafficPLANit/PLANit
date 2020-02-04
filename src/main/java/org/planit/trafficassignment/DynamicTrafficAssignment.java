package org.planit.trafficassignment;

import org.planit.route.RouteChoice;

/**
 * Dynamic traffic assignment class for any traffic assignment model that adheres to being capacity constrained
 * utilising a fundamental diagram and underlying node model
 *
 * @author markr
 *
 */
public abstract class DynamicTrafficAssignment extends CapacityConstrainedAssignment {

	/** generated UID */
	private static final long serialVersionUID = 5518351010500386771L;

	/** the route choice methodology adopted */
	protected RouteChoice routeChoice;

	// getters - setters


	/** set the route choice methodology as to be adopted by this traffic assignment
	 * @param routeChoice
	 */
	public void setRouteChoice(final RouteChoice routeChoice) {
		this.routeChoice = routeChoice;
	}

}

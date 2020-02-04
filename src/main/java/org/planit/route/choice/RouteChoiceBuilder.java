package org.planit.route.choice;
import org.planit.exceptions.PlanItException;

/** The type of Route choice is sometimes but not always a choice for any given traffic assignment
 * model. Hence, instead of equipping every traffic assignment builder with a route choice component
 * to register or create, we allow any traffic assignment builder to implement this interface to
 * make the creation and registration of route choice components possible if relevant
 *
 * @author markr
 *
 */
public interface RouteChoiceBuilder {

	/** create and register the route choice one desires.
	 *
	 * @param physicalTraveltimeCostFunctionType
	 * @return
	 * @throws PlanItException
	 */
	public RouteChoice createAndRegisterRouteChoice(final String physicalTraveltimeCostFunctionType) throws PlanItException;

}

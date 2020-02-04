package org.planit.route.choice;

import org.planit.input.InputBuilderListener;
import org.planit.trafficassignment.TrafficAssignmentComponent;
import org.planit.utils.misc.IdGenerator;

/** The route choice traffic assignment component responsible for the
 * configuration of the route choice methodology and the routes associated
 * with this procedure.
 *
 * All derived classes must adhere to this protected constructors signature as the
 * factory expects a traffic component create listener only
 *
 * @author markr
 *
 */
public class RouteChoice extends TrafficAssignmentComponent<RouteChoice> {

	/**generate UID */
	private static final long serialVersionUID = 6220514783786893944L;

	/**
     * unique identifier
     */
    protected final long id;

	/** Constructor
	 * @param trafficComponentCreateListener
	 */
	protected RouteChoice(final InputBuilderListener trafficComponentCreateListener) {
		super();
		this.id = IdGenerator.generateId(RouteChoice.class);
	}

	/**
	 * #{@inheritDoc}
	 */
	@Override
	public long getId() {
		return this.id;
	}

}

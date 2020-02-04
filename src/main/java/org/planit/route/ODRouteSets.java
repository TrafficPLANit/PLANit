package org.planit.route;

import org.planit.exceptions.PlanItException;
import org.planit.logging.PlanItLogger;
import org.planit.trafficassignment.TrafficAssignmentComponent;
import org.planit.trafficassignment.TrafficAssignmentComponentFactory;
import org.planit.utils.misc.IdGenerator;

/** Contains one or more origin-destination based route sets that can be used in assignment
 *
 * @author markr
 *
 */
public class ODRouteSets extends TrafficAssignmentComponent<ODRouteSets> {

	/** generated UID*/
	private static final long serialVersionUID = -8742549499023004121L;

	// register to be eligible in PLANit
    static {
        try {
            TrafficAssignmentComponentFactory.registerTrafficAssignmentComponentType(ODRouteSets.class);
        } catch (final PlanItException e) {
            e.printStackTrace();
        }
    }

	/**
     * unique identifier
     */
    protected final long id;

    /**
     * Constructor
     */
    public ODRouteSets() {
        super();
        this.id = IdGenerator.generateId(ODRouteSets.class);
    }

    /** Collect the first od route set available
     * @return the first od route set available
     */
    public ODRouteSet getFirstODRouteSet() {
    	PlanItLogger.severe("getFirstODRouteSet not yet implemented");
    	return null;
    }

    /**
     * {@inheritDoc}
     */
	@Override
	public long getId() {
		return this.id;
	}

}

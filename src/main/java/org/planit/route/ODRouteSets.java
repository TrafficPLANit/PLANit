package org.planit.route;

import java.util.TreeMap;

import org.planit.network.virtual.Zoning;
import org.planit.od.odroute.ODRouteMatrix;
import org.planit.trafficassignment.TrafficAssignmentComponent;
import org.planit.utils.misc.IdGenerator;

/** Contains one or more origin-destination based route sets that can be used in assignment.
 * For now each individual route set takes on the form of the already available ODPathMatrix. In future
 * versions more flexible implementation are planned
 *
 * @author markr
 *
 */
public class ODRouteSets extends TrafficAssignmentComponent<ODRouteSets> {

	/** generated UID*/
	private static final long serialVersionUID = -8742549499023004121L;

	/**
     * unique identifier
     */
    protected final long id;

    /**
     * map holding all registered od route matrices by their unique id
     */
    protected final TreeMap<Long, ODRouteMatrix> odRouteMatrices = new TreeMap<Long, ODRouteMatrix>();

    /**
     * Constructor
     */
    public ODRouteSets() {
        super();
        this.id = IdGenerator.generateId(ODRouteSets.class);
    }

    /** Create an empty od route matrix and register it on this od route sets
     * @param zoning used to derive the size of the aquare zone based matrix
     * @return newly created od route matrix
     */
    public ODRouteMatrix createAndRegisterODRouteMatrix(final Zoning zoning) {
    	final ODRouteMatrix newOdRouteMatrix = new ODRouteMatrix(zoning.zones);
    	odRouteMatrices.put(newOdRouteMatrix.getId(), newOdRouteMatrix);
    	return newOdRouteMatrix;
    }

    /** register the passed in route matrix (not copied)
     * @param odRouteMatrix to register
     */
    public void registerODRouteMatrix(final ODRouteMatrix odRouteMatrix) {
    	odRouteMatrices.put(odRouteMatrix.getId(), odRouteMatrix);
    }

    /** verify if any od route matrices have been registered or not
     * @return true if any are registered, false otherwise
     */
    public Boolean hasRegisteredODMatrices() {
    	return !odRouteMatrices.isEmpty();
    }

    /** Collect the first od route matrix available
     * @return the first od route matrix available, if not available null is returned
     */
    public ODRouteMatrix getFirstODRouteMatrix() {
    	return hasRegisteredODMatrices() ? odRouteMatrices.firstEntry().getValue() : null;
    }

    /**
     * {@inheritDoc}
     */
	@Override
	public long getId() {
		return this.id;
	}

}
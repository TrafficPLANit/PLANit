package org.planit.cost.virtual;

import java.util.logging.Logger;

import org.planit.cost.Cost;
import org.planit.event.RequestAccesseeEvent;
import org.planit.network.virtual.ConnectoidSegment;
import org.planit.trafficassignment.TrafficAssignmentComponent;

/**
 * Object to handle the travel time cost of a virtual link
 * 
 * @author markr
 *
 */
public abstract class VirtualCost extends TrafficAssignmentComponent<VirtualCost> implements Cost<ConnectoidSegment> {

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(VirtualCost.class.getName());

    /**
     * Constructor
     */
    public VirtualCost() {
        super();
    }

    /**
     * Indicate if cost object requires an interaction to be able to perform its
     * cost computation
     * 
     * @return true if class requires an Interactor, false otherwise
     */
    public boolean requiresInteractor() {
        return false;
    }

    /**
     * Perform interaction
     */
    public void performInteraction() {
        // TODO - only to be implemented when interaction is required
    }

    /**
     * Creates the interactor event request
     * 
     * @return requestedInteractorEvent, null by default
     */
    public RequestAccesseeEvent createInteractorRequest() {
        return null;
    }
}

package org.planit.cost.virtual;

import org.planit.cost.Cost;
import org.planit.event.RequestAccesseeEvent;
import org.planit.exceptions.PlanItException;
import org.planit.network.virtual.ConnectoidSegment;
import org.planit.network.virtual.VirtualNetwork;
import org.planit.trafficassignment.TrafficAssignmentComponent;

/**
 * Object to handle the travel time cost of a virtual link
 * 
 * @author markr
 *
 */
public abstract class VirtualCost extends TrafficAssignmentComponent<VirtualCost> implements Cost<ConnectoidSegment> {

    /**
     * Constructor
     */
    public VirtualCost() {
        super();
    }
    
    /**
     * Initialize the virtual cost component
     * 
     * @param VirtualNetwork the virtual network
     * @throws PlanItException thrown if a link/mode combination exists for which no cost parameters have been set
     */
    public abstract void initialiseBeforeSimulation(VirtualNetwork virtualNetwork) throws PlanItException;    

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

package org.planit.cost.virtual;

import org.planit.cost.Cost;
import org.planit.exceptions.PlanItException;
import org.planit.network.virtual.VirtualNetwork;
import org.planit.trafficassignment.TrafficAssignmentComponent;
import org.planit.utils.network.virtual.ConnectoidSegment;

/**
 * Object to handle the travel time cost of a virtual link
 * 
 * @author markr
 *
 */
public abstract class VirtualCost extends TrafficAssignmentComponent<VirtualCost> implements Cost<ConnectoidSegment> {

    /** generated UID */
	private static final long serialVersionUID = -8278650865770286434L;

	/**
     * Constructor
     */
    public VirtualCost() {
        super();
    }
    
    /**
     * Initialize the virtual cost component
     * 
     * @param virtualNetwork the virtual network
     * @throws PlanItException thrown if a link/mode combination exists for which no cost parameters have been set
     */
    public abstract void initialiseBeforeSimulation(VirtualNetwork virtualNetwork) throws PlanItException;    

}

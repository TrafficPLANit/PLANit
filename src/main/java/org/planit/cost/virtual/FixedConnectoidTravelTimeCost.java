package org.planit.cost.virtual;

import org.planit.exceptions.PlanItException;
import org.planit.network.virtual.ConnectoidSegment;
import org.planit.network.virtual.VirtualNetwork;
import org.planit.userclass.Mode;

/**
 * Class holding fixed connectoid costs for each connectoid segment
 * 
 * @author markr
 *
 */
public class FixedConnectoidTravelTimeCost extends VirtualCost {

	/**
	 * Fixed connectoid cost for connectoid segments - defaults to zero
	 */
	protected double fixedConnectoidCost = 0.0;

	/**
	 * Constructor
	 */
	public FixedConnectoidTravelTimeCost() {
		super();
	}

	/**
	 * Calculates the connectoid segment cost using a fixed travel time
	 * 
	 * @param mode              mode of travel
	 * @param connectoidSegment the connectoid segment
	 * @return the travel time for the specified connectoid segment
	 */
	@Override
	public double getSegmentCost(Mode mode, ConnectoidSegment connectoidSegment) {
		return fixedConnectoidCost;
	}
	
    /**
     * Initialize the virtual cost component
     * 
     * @param VirtualNetwork the virtual network
     * @throws PlanItException thrown if a link/mode combination exists for which no cost parameters have been set
     */
    @Override
    public void initialiseBeforeSimulation(VirtualNetwork virtualNetwork) throws PlanItException {
        // currently no specific initialization needed
    }	
    
    public void setFixedConnectoidCost(double fixedConnectoidCost) {
    	this.fixedConnectoidCost = fixedConnectoidCost;
    }

}
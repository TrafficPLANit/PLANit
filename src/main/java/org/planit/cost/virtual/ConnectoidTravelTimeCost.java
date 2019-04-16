package org.planit.cost.virtual;

import java.util.logging.Logger;

/**
 * Placeholder class for all connectoid travel time costs
 * 
 * @author markr
 *
 */
public abstract class ConnectoidTravelTimeCost extends VirtualCost {

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(ConnectoidTravelTimeCost.class.getName());
        
/**
 * Constructor
 */
	protected ConnectoidTravelTimeCost(){
		super();
	}
		
}

package org.planit.cost.physical;

import java.util.logging.Logger;

import org.planit.event.listener.InteractorListener;

/**
 * Link Travel Time cost function
 * 
 * @author markr
 *
 */
public abstract class LinkTravelTimeCost extends PhysicalCost implements InteractorListener{

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(LinkTravelTimeCost.class.getName());
        
    /**
     * Constructor
     */
    LinkTravelTimeCost() {
		super();
	}
	
}

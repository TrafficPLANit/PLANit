package org.planit.cost.physical;

import org.planit.event.listener.InteractorListener;

/**
 * Link Travel Time cost function
 * 
 * @author markr
 *
 */
public abstract class LinkTravelTimeCost extends PhysicalCost implements InteractorListener{
        
    /**
     * Constructor
     */
    LinkTravelTimeCost() {
		super();
	}
	
}

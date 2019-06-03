package org.planit.cost.physical;

import java.util.logging.Logger;

import org.planit.cost.Cost;
import org.planit.network.physical.LinkSegment;
import org.planit.trafficassignment.TrafficAssignmentComponent;

/**
 * Object to handle the travel time cost of a physical link
 * 
 * @author markr
 *
 */
public abstract class PhysicalCost extends TrafficAssignmentComponent<PhysicalCost> implements Cost<LinkSegment> {

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(PhysicalCost.class.getName());
        
    /**
     * Constructor
     */
	public PhysicalCost() {
		super();
	}
	
}

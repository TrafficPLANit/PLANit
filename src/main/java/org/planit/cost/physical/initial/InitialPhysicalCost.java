package org.planit.cost.physical.initial;

import org.planit.cost.physical.AbstractPhysicalCost;
import org.planit.network.physical.LinkSegment;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.TrafficAssignmentComponent;
import org.planit.userclass.Mode;

/**
 * The initial physical costs for the network
 * 
 * @author gman6028
 *
 */
public abstract class InitialPhysicalCost extends TrafficAssignmentComponent<InitialPhysicalCost>  implements AbstractPhysicalCost {

    /**
     * Constructor
     */
	public InitialPhysicalCost() {
		super();
	}
	
	/**
	 * Set the initial cost for a specified mode and link segment
	 * 
	 * @param mode the current mode
	 * @param linkSegment the current link segment
	 * @param cost the initial cost for this mode and link segment
	 */
	public abstract void setSegmentCost(Mode mode, LinkSegment linkSegment, double cost);
	
}

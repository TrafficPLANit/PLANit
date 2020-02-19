package org.planit.cost.physical.initial;

import java.io.Serializable;

import org.planit.cost.physical.AbstractPhysicalCost;
import org.planit.trafficassignment.TrafficAssignmentComponent;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Mode;

/**
 * The initial physical costs for the network
 * 
 * @author gman6028
 *
 */
public abstract class InitialPhysicalCost extends TrafficAssignmentComponent<InitialPhysicalCost>  implements AbstractPhysicalCost, Serializable {

    /** generated UID */
	private static final long serialVersionUID = -7894043964147010621L;

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
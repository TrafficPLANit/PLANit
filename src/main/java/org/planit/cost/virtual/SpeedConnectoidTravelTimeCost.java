package org.planit.cost.virtual;

import org.planit.exceptions.PlanItException;
import org.planit.network.virtual.ConnectoidSegment;
import org.planit.userclass.Mode;

public class SpeedConnectoidTravelTimeCost extends VirtualCost {

	//Make Connectoid speed so fast that travel time comes out as zero
	public static final double CONNECTOID_SPEED_KPH = Double.POSITIVE_INFINITY;

	/**
	 * number of segments captured by the array
	 */
	/**
	 * Constructor
	 */
	public SpeedConnectoidTravelTimeCost(){
		super();
	}
			
	public double calculateSegmentCost(Mode mode, ConnectoidSegment connectoidSegment) throws PlanItException {
		return connectoidSegment.getParentEdge().getLength()/ CONNECTOID_SPEED_KPH;
	}
	
}

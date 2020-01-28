package org.planit.cost.virtual;

import org.planit.exceptions.PlanItException;
import org.planit.network.virtual.VirtualNetwork;
import org.planit.utils.network.physical.Mode;
import org.planit.utils.network.virtual.ConnectoidSegment;

/**
 * Class to calculate the connectoid travel time using connectoid speed
 * 
 * @author gman6028
 *
 */
public class SpeedConnectoidTravelTimeCost extends VirtualCost {

	//public static final double CONNECTOID_SPEED_KPH = Double.POSITIVE_INFINITY;
	public static final double DEFAULT_CONNECTOID_SPEED_KPH = 25.0;
	
	/**
	 * Speed used for connectoid cost calculations
	 */
	private double connectoidSpeed;

	/**
	 * Constructor
	 */
	public SpeedConnectoidTravelTimeCost() {
		super();
		connectoidSpeed = DEFAULT_CONNECTOID_SPEED_KPH;
	}

	/**
	 * Return the connectoid travel time using speed
	 * 
	 * @param mode              the mode of travel
	 * @param connectoidSegment the connectoid segment
	 * @return the travel time for this connectoid segment
	 */
	@Override
	public double getSegmentCost(Mode mode, ConnectoidSegment connectoidSegment) {
		return connectoidSegment.getParentEdge().getLength() / connectoidSpeed;
	}

	/**
	 * currently no specific initialisation needed
	 */
    @Override
    public void initialiseBeforeSimulation(VirtualNetwork virtualNetwork) throws PlanItException {
        // currently no specific initialisation needed
    }
    
    public void setConnectiodSpeed(double connectoidSpeed) {
    	this.connectoidSpeed = connectoidSpeed;
    }

}

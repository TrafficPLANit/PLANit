package org.planit.cost.virtual;

import org.planit.exceptions.PlanItException;
import org.planit.network.virtual.ConnectoidSegment;
import org.planit.network.virtual.VirtualNetwork;
import org.planit.userclass.Mode;

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
     * Initialize the virtual cost component
     * 
     * @param VirtualNetwork the virtual network
     * @throws PlanItException thrown if a link/mode combination exists for which no cost parameters have been set
     */
    @Override
    public void initialiseBeforeSimulation(VirtualNetwork virtualNetwork) throws PlanItException {
        // currently no specific initialization needed
    }
    
    public void setConnectiodSpeed(double connectoidSpeed) {
    	this.connectoidSpeed = connectoidSpeed;
    }

}

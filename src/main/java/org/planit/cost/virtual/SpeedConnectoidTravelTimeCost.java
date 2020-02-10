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

	/** generated UID */
	private static final long serialVersionUID = 2813935702895030693L;

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
	public double getSegmentCost(final Mode mode, final ConnectoidSegment connectoidSegment) {
		return connectoidSegment.getParentEdge().getLength() / connectoidSpeed;
	}

    /**
     * #{@inheritDoc}
     */
    @Override
    public void initialiseBeforeSimulation(final VirtualNetwork virtualNetwork) throws PlanItException {
        // currently no specific initialization needed
    }

    /** set the connectoid speed
     * @param connectoidSpeed the speed
     */
    public void setConnectiodSpeed(final double connectoidSpeed) {
    	this.connectoidSpeed = connectoidSpeed;
    }

}
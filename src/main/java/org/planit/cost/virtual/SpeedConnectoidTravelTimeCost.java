package org.planit.cost.virtual;

import java.util.logging.Logger;

import org.planit.network.virtual.ConnectoidSegment;
import org.planit.userclass.Mode;

/**
 * Class to calculate the connectoid travel time using connectoid speed
 * 
 * @author gman6028
 *
 */
public class SpeedConnectoidTravelTimeCost extends VirtualCost {

	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = Logger.getLogger(SpeedConnectoidTravelTimeCost.class.getName());

	// TODO - At present connectoid speed is infinity, which makes travel time come
	// out as zero. Perhaps this should be configurable
	public static final double CONNECTOID_SPEED_KPH = Double.POSITIVE_INFINITY;

	/**
	 * Constructor
	 */
	public SpeedConnectoidTravelTimeCost() {
		super();
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
		return connectoidSegment.getParentEdge().getLength() / CONNECTOID_SPEED_KPH;
	}

}

package org.planit.cost.virtual;

import javax.annotation.Nonnull;

import org.planit.exceptions.PlanItException;
import org.planit.network.virtual.ConnectoidSegment;
import org.planit.userclass.Mode;

/**
 * Class holding fixed connectoid costs for each connectoid segment 
 * @author markr
 *
 */
public class FixedConnectoidTravelTimeCost extends VirtualCost {
	
	/**
	 * the fixed connectoid costs for the connectoid segments
	 */
	double[] fixedConnectoidCosts = null;
	
	/**
	 * number of segments captured by the array
	 */
	int numberOfConnectoidSegments = -1;

	public FixedConnectoidTravelTimeCost() {
		super();
	}
	/** Populate the connectoid segment costs which remain fixed throughout the simulation
	 * @param fixedConnectoidCosts
	 */
	public void populate(@Nonnull double[] fixedConnectoidCosts, int numberOfConnectoidSegments) {
		this.fixedConnectoidCosts = fixedConnectoidCosts;
		this.numberOfConnectoidSegments = numberOfConnectoidSegments;
	}
	
	public double calculateSegmentCost(Mode mode, ConnectoidSegment connectoidSegment) throws PlanItException {
		return fixedConnectoidCosts[(int) connectoidSegment.getConnectoidSegmentId()];
	}
	
}

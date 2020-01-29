package org.planit.cost.virtual;

import javax.annotation.Nonnull;

import org.planit.exceptions.PlanItException;
import org.planit.network.virtual.VirtualNetwork;
import org.planit.utils.network.physical.Mode;
import org.planit.utils.network.virtual.ConnectoidSegment;

/**
 * Class holding fixed connectoid costs for each connectoid segment
 * 
 * @author markr
 *
 */
public class FixedConnectoidTravelTimeCost extends VirtualCost {

	/** generate UID */
	private static final long serialVersionUID = -7922583510610674079L;

	/**
	 * The fixed connectoid costs for the connectoid segments
	 */
	double[] fixedConnectoidCosts = null;

	/**
	 * Number of segments captured by the array
	 */
	int numberOfConnectoidSegments = -1;

	/**
	 * Constructor
	 */
	public FixedConnectoidTravelTimeCost() {
		super();
	}

	/**
	 * Populate the connectoid segment costs which remain fixed throughout the
	 * simulation
	 * 
	 * @param fixedConnectoidCosts       array of fixed connectoid costs
	 * @param numberOfConnectoidSegments the number of connectoid segments
	 */
	public void populate(@Nonnull double[] fixedConnectoidCosts, int numberOfConnectoidSegments) {
		this.fixedConnectoidCosts = fixedConnectoidCosts;
		this.numberOfConnectoidSegments = numberOfConnectoidSegments;
	}

	/**
	 * Set all the connectoid costs to zero
	 * 
	 * @param numberOfConnectoidSegments the number of connectoid segments
	 */
	public void populateToZero(int numberOfConnectoidSegments) {
		this.fixedConnectoidCosts = new double[numberOfConnectoidSegments];
		this.numberOfConnectoidSegments = numberOfConnectoidSegments;
	}
 
	/**
	 * Calculates the connectoid segment cost using a fixed travel time
	 * 
	 * @param mode              mode of travel
	 * @param connectoidSegment the connectoid segment
	 * @return the travel time for the specified connectod segment
	 */
	@Override
	public double getSegmentCost(Mode mode, ConnectoidSegment connectoidSegment) {
		return fixedConnectoidCosts[(int) connectoidSegment.getConnectoidSegmentId()];
	}
	
    /**
     * currently no specific initialisation needed
     */
    @Override
    public void initialiseBeforeSimulation(VirtualNetwork virtualNetwork) throws PlanItException {
        // currently no specific initialisation needed
    }	

}

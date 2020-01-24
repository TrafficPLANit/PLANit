package org.planit.cost.virtual;

import javax.annotation.Nonnull;

import org.planit.exceptions.PlanItException;
import org.planit.network.virtual.ConnectoidSegment;
import org.planit.network.virtual.VirtualNetwork;
import org.planit.userclass.Mode;

/**
 * Class holding fixed connectoid costs for each connectoid segment
 * 
 * @author markr
 *
 */
public class FixedConnectoidTravelTimeCost extends VirtualCost {

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
	private void populate(@Nonnull double[] fixedConnectoidCosts, int numberOfConnectoidSegments) {
		this.fixedConnectoidCosts = fixedConnectoidCosts;
		this.numberOfConnectoidSegments = numberOfConnectoidSegments;
	}

	/**
	 * Set all the connectoid costs to zero
	 * 
	 * @param numberOfConnectoidSegments the number of connectoid segments
	 */
	private void populateToZero(int numberOfConnectoidSegments) {
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
     * Initialize the virtual cost component
     * 
     * @param VirtualNetwork the virtual network
     * @throws PlanItException thrown if a link/mode combination exists for which no cost parameters have been set
     */
    @Override
    public void initialiseBeforeSimulation(VirtualNetwork virtualNetwork) throws PlanItException {
        populateToZero(virtualNetwork.connectoidSegments.getNumberOfConnectoidSegments());
    }	

}

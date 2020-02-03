package org.planit.trafficassignment.builder;

import org.planit.cost.physical.PhysicalCost;
import org.planit.cost.virtual.VirtualCost;
import org.planit.exceptions.PlanItException;
import org.planit.input.InputBuilderListener;
import org.planit.trafficassignment.CapacityRestrainedAssignment;
import org.planit.trafficassignment.TrafficAssignmentComponentFactory;

/**
 * Builder for capacity restrained assignment methods
 *
 * @author markr
 *
 */
public class CapacityRestrainedTrafficAssignmentBuilder extends DeterministicTrafficAssignmentBuilder {

	// FACTORIES

	/**
	 * Cost factory to create physical costs to register on the generalized cost.
	 */
	protected final TrafficAssignmentComponentFactory<PhysicalCost> physicalCostFactory;

	/**
	 * Cost factory to create physical costs to register on the generalized cost.
	 */
	protected final TrafficAssignmentComponentFactory<VirtualCost> virtualCostFactory;

	/**
	 * Constructor
	 *
	 * @param capacityRestrainedAssignment CapacityRestrainedAssignment object to build
	 * @param trafficComponentCreateListener listener to register on the internal traffic component factories for notification upon creation of components
	 */
	public CapacityRestrainedTrafficAssignmentBuilder(
			final CapacityRestrainedAssignment capacityRestrainedAssignment,
			final InputBuilderListener trafficComponentCreateListener) {
		super(capacityRestrainedAssignment, trafficComponentCreateListener);
		physicalCostFactory = new TrafficAssignmentComponentFactory<PhysicalCost>(PhysicalCost.class);
		virtualCostFactory = new TrafficAssignmentComponentFactory<VirtualCost>(VirtualCost.class);
		// register listener on factories
		physicalCostFactory.addListener(trafficComponentCreateListener, TrafficAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE);
		virtualCostFactory.addListener(trafficComponentCreateListener, TrafficAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE);
	}

	/**
	 * Create and register physical link cost function to determine travel time
	 *
	 * @param physicalTraveltimeCostFunctionType the type of cost function to be
	 *                                           created
	 * @return the physical cost created
	 * @throws PlanItException thrown if there is an error
	 */
	public PhysicalCost createAndRegisterPhysicalCost(final String physicalTraveltimeCostFunctionType)
			throws PlanItException {
		final PhysicalCost physicalCost = physicalCostFactory.create(physicalTraveltimeCostFunctionType);
		if (parentAssignment.getPhysicalCost() == null) {
			parentAssignment.setPhysicalCost(physicalCost);
		}
		return physicalCost;
	}

	/**
	 * Create and Register virtual link cost function to determine travel time
	 *
	 * @param virtualTraveltimeCostFunctionType the type of cost function to be
	 *                                          created
	 * @return the cost function created
	 * @throws PlanItException thrown if there is an error
	 */
	public VirtualCost createAndRegisterVirtualTravelTimeCostFunction(final String virtualTraveltimeCostFunctionType)
			throws PlanItException {
		final VirtualCost createdCost = virtualCostFactory.create(virtualTraveltimeCostFunctionType);
		if (parentAssignment.getVirtualCost() == null) {
			parentAssignment.setVirtualCost(createdCost);
		}
		return createdCost;
	}
}

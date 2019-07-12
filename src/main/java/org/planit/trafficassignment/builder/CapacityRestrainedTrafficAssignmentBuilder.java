package org.planit.trafficassignment.builder;

import java.util.logging.Logger;

import org.planit.cost.physical.DynamicPhysicalCost;
import org.planit.cost.physical.PhysicalCost;
import org.planit.cost.virtual.VirtualCost;
import org.planit.event.management.EventManager;
import org.planit.exceptions.PlanItException;
import org.planit.trafficassignment.CapacityRestrainedAssignment;
import org.planit.trafficassignment.TrafficAssignmentComponentFactory;

/**
 * Builder for capacity restrained assignment methods
 * 
 * @author markr
 *
 */
public class CapacityRestrainedTrafficAssignmentBuilder extends TrafficAssignmentBuilder {

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(CapacityRestrainedTrafficAssignmentBuilder.class.getName());

    // FACTORIES

    /**
     * Cost factory to create physical costs to register on the generalized cost.
     */
    protected final TrafficAssignmentComponentFactory<DynamicPhysicalCost> physicalCostFactory;

    /**
     * Cost factory to create physical costs to register on the generalized cost.
     */
    protected final TrafficAssignmentComponentFactory<VirtualCost> virtualCostFactory;

    /**
     * Constructor
     * 
     * @param capacityRestrainedAssignment
     *            CapacityRestrainedAssignment object to be built
     */
    public CapacityRestrainedTrafficAssignmentBuilder(CapacityRestrainedAssignment capacityRestrainedAssignment) {
        super(capacityRestrainedAssignment);
        physicalCostFactory = new TrafficAssignmentComponentFactory<DynamicPhysicalCost>(DynamicPhysicalCost.class);
        virtualCostFactory = new TrafficAssignmentComponentFactory<VirtualCost>(VirtualCost.class);
    }

    /**
     * Create and Register physical link cost function to determine travel time
     * 
     * @param physicalTraveltimeCostFunctionType
     *            the type of cost function to be created
     * @return the cost function created
     * @throws PlanItException
     *             thrown if there is an error
     */
    public PhysicalCost createAndRegisterPhysicalTravelTimeCostFunction(String physicalTraveltimeCostFunctionType)
            throws PlanItException {
        DynamicPhysicalCost createdCost = physicalCostFactory.create(physicalTraveltimeCostFunctionType);
        if (parentAssignment.getPhysicalCost() == null) {
            parentAssignment.setPhysicalCost(createdCost);
        }
        return createdCost;
    }

    /**
     * Create and Register virtual link cost function to determine travel time
     * 
     * @param virtualTraveltimeCostFunctionType
     *            the type of cost function to be created
     * @return the cost function created
     * @throws PlanItException
     *             thrown if there is an error
     */
    public VirtualCost createAndRegisterVirtualTravelTimeCostFunction(String virtualTraveltimeCostFunctionType)
            throws PlanItException {
        VirtualCost createdCost = virtualCostFactory.create(virtualTraveltimeCostFunctionType);
        if (parentAssignment.getVirtualCost() == null) {
            parentAssignment.setVirtualCost(createdCost);
        }
        return createdCost;
    }

    /**
     * Set the EventManager to be used by the factory objects
     * 
     * The EventManager must be a singleton for each PlanItProject
     * 
     * @param eventManager
     *            EventManager to be used to generate traffic component objects
     */
    @Override
    public void setEventManager(EventManager eventManager) {
        super.setEventManager(eventManager);
        physicalCostFactory.setEventManager(eventManager);
        virtualCostFactory.setEventManager(eventManager);
    }

}

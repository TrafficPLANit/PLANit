package org.planit.trafficassignment.builder;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.planit.cost.physical.PhysicalCost;
import org.planit.cost.virtual.VirtualCost;
import org.planit.event.EventManager;
import org.planit.exceptions.PlanItException;
import org.planit.trafficassignment.CapacityRestrainedAssignment;
import org.planit.trafficassignment.TrafficAssignmentComponentFactory;

/**
 * Builder for capacity restrained assignment methods
 * @author markr
 *
 */
public class CapacityRestrainedTrafficAssignmentBuilder extends TrafficAssignmentBuilder  {

	// FACTORIES
	
	/**
	 * Cost factory to create physical costs to register on the generalized cost. 
	 */
	protected final TrafficAssignmentComponentFactory<PhysicalCost> physicalCostFactory;
	
	/**
	 * Cost factory to create physical costs to register on the generalized cost. 
	 */
	protected final TrafficAssignmentComponentFactory<VirtualCost> virtualCostFactory;	
	
	/** Constructor
	 * @param capacityRestrainedAssignment
	 */
	public CapacityRestrainedTrafficAssignmentBuilder(CapacityRestrainedAssignment capacityRestrainedAssignment) {
		super(capacityRestrainedAssignment);
		physicalCostFactory = new TrafficAssignmentComponentFactory<PhysicalCost>(PhysicalCost.class);
		virtualCostFactory = new TrafficAssignmentComponentFactory<VirtualCost>(VirtualCost.class);	
	}

	/** Create and Register physical link performance function to determine travel time
	 * @param linkPerformancefunctionType
	 * @return smoothing, that was registered
	 * @throws PlanItException 
	 * @throws ClassNotFoundException 
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws IOException 
	 */
	public PhysicalCost createAndRegisterPhysicalTravelTimeCostFunction(String physicalTraveltimeCostFunctionType) throws InstantiationException, 
																										                                                                                                        IllegalAccessException, 
																										                                                                                                        IllegalArgumentException, 
																										                                                                                                        InvocationTargetException, 
																										                                                                                                        NoSuchMethodException, 
																										                                                                                                        SecurityException, 
																										                                                                                                        ClassNotFoundException, 
																										                                                                                                        PlanItException, 
																										                                                                                                        IOException {
		PhysicalCost createdCost = physicalCostFactory.create(physicalTraveltimeCostFunctionType);
		if (parentAssignment.getPhysicalCost() == null) {
			parentAssignment.setPhysicalCost(createdCost);
		}
		return createdCost;
	}
	
	/** Create and Register virtual link performance function to determine travel time
	 * @param linkPerformancefunctionType
	 * @return smoothing, that was registered
	 * @throws PlanItException 
	 * @throws ClassNotFoundException 
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public VirtualCost createAndRegisterVirtualTravelTimeCostFunction(String virtualTraveltimeCostFunctionType) throws InstantiationException, 
																								                                                                                                     IllegalAccessException, 
																								                                                                                                     IllegalArgumentException, 
																								                                                                                                     InvocationTargetException, 
																								                                                                                                     NoSuchMethodException, 
																								                                                                                                     SecurityException, 
																								                                                                                                     ClassNotFoundException, 
																								                                                                                                     PlanItException, 
																								                                                                                                     IOException {
		VirtualCost createdCost = virtualCostFactory.create(virtualTraveltimeCostFunctionType);
		if (parentAssignment.getVirtualCost() == null) {
			parentAssignment.setVirtualCost(createdCost);
		}
		return createdCost;
	}

	@Override
	public void setEventManager(EventManager eventManager) {
		super.setEventManager(eventManager);
		physicalCostFactory.setEventManager(eventManager);
		virtualCostFactory.setEventManager(eventManager);
		}	

}

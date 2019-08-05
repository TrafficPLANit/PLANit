package org.planit.cost.physical;

import org.planit.exceptions.PlanItException;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.trafficassignment.TrafficAssignmentComponent;

/**
 * Class for dynamic cost functions, which calculate link segment costs for each iteration
 * 
 * @author gman6028
 *
 */
public abstract class PhysicalCost extends TrafficAssignmentComponent<PhysicalCost> implements AbstractPhysicalCost {
	 

	/**
	 * Initialize the cost parameter values in the network
	 * 
	 * @param physicalNetwork the physical network
	 * @throws PlanItException thrown if a link/mode combination exists for which no cost parameters have been set
	 */
	public abstract void initialiseCostsBeforeEquilibration(PhysicalNetwork physicalNetwork) throws PlanItException;

}

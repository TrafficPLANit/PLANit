package org.planit.trafficassignment.builder;

import org.planit.demands.Demands;
import org.planit.exceptions.PlanItException;
import org.planit.input.InputBuilderListener;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.trafficassignment.TrafficAssignment;

/**
 * Builder for a traditional static assignment
 *
 * @author markr
 *
 */
public class TraditionalStaticAssignmentBuilder extends TrafficAssignmentBuilder {

	/** Constructor
	 * @param parentAssignment
	 * @param trafficComponentCreateListener
	 * @param demands
	 * @param zoning
	 * @param physicalNetwork
	 * @throws PlanItException
	 */
	public TraditionalStaticAssignmentBuilder(
			final TrafficAssignment parentAssignment,
			final InputBuilderListener trafficComponentCreateListener,
			final Demands demands,
			final Zoning zoning,
			final PhysicalNetwork physicalNetwork) throws PlanItException {
		super(parentAssignment, trafficComponentCreateListener, demands, zoning, physicalNetwork);
	}

}

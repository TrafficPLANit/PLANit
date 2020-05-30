package org.planit.trafficassignment.builder;

import org.planit.cost.physical.BPRLinkTravelTimeCost;
import org.planit.cost.virtual.FixedConnectoidTravelTimeCost;
import org.planit.demands.Demands;
import org.planit.exceptions.PlanItException;
import org.planit.input.InputBuilderListener;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.sdinteraction.smoothing.MSASmoothing;
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

	/**
	 * Traditional static assignment has the following defaults set for it:
	 * - PhysicalCost: BPR
	 * - Virtualcost: FIXED
	 * - Smoothing: MSA
	 * - Gapfunction: LinkBasedRelativeDualityGap
	 */
	@Override
	public void initialiseDefaults() throws PlanItException {
		super.initialiseDefaults();
		// add defaults for traditional static assignment
		this.createAndRegisterPhysicalCost(BPRLinkTravelTimeCost.class.getCanonicalName());
		this.createAndRegisterVirtualCost(FixedConnectoidTravelTimeCost.class.getCanonicalName());
		this.createAndRegisterSmoothing(MSASmoothing.class.getCanonicalName());		
	}

}

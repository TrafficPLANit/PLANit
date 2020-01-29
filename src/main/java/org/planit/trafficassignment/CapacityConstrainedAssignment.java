package org.planit.trafficassignment;

import org.planit.exceptions.PlanItException;
import org.planit.input.InputBuilderListener;
import org.planit.supply.fundamentaldiagram.FundamentalDiagram;
import org.planit.supply.network.nodemodel.NodeModel;
import org.planit.trafficassignment.builder.CapacityConstrainedTrafficAssignmentBuilder;
import org.planit.trafficassignment.builder.TrafficAssignmentBuilder;

/**
 * Capacity constrained traffic assignment component
 * 
 * @author gman6028
 *
 */
public abstract class CapacityConstrainedAssignment extends TrafficAssignment {

	// PROTECTED

	/** generated UID */
	private static final long serialVersionUID = 2568729148299613377L;

	/**
	 * The builder for all capacity constrained traffic assignment instances
	 */
	protected CapacityConstrainedTrafficAssignmentBuilder capacityConstrainedBuilder;

	/**
	 * Fundamental diagram to use
	 */
	protected FundamentalDiagram fundamentalDiagram = null;

	/**
	 * Node model to use
	 */
	protected NodeModel nodeModel = null;

	// PUBLIC

	/**
	 * Constructor
	 */
	public CapacityConstrainedAssignment() {
		super();
	}

	/**
	 * {@inheritDoc} 
	 */
	@Override
	public TrafficAssignmentBuilder collectBuilder(final InputBuilderListener trafficComponentCreateListener) {
		if(capacityConstrainedBuilder==null) {
			capacityConstrainedBuilder = new CapacityConstrainedTrafficAssignmentBuilder(this, trafficComponentCreateListener);
		}
		return capacityConstrainedBuilder;
	}

	/**
	 * {@inheritDoc} 
	 */
	@Override
	public void verifyComponentCompatibility() throws PlanItException {
		throw new PlanItException("Not yet implemented");
	}

	/**
	 * {@inheritDoc} 
	 */
	@Override
	public void executeEquilibration() throws PlanItException {
		throw new PlanItException("Not yet implemented");
	}

	// Getters - Setters

	public void setFundamentalDiagram(FundamentalDiagram fundamentalDiagram) {
		this.fundamentalDiagram = fundamentalDiagram;
	}

	public void setNodeModel(NodeModel nodeModel) {
		this.nodeModel = nodeModel;
	}

}

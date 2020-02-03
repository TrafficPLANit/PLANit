package org.planit.trafficassignment;

import org.planit.supply.fundamentaldiagram.FundamentalDiagram;
import org.planit.supply.network.nodemodel.NodeModel;
import org.planit.trafficassignment.builder.CapacityConstrainedTrafficAssignmentBuilder;

/**
 * Capacity constrained traffic assignment component
 *
 * @author markr
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

	// Getters - Setters

	/** Set the fundamental diagram
	 * @param fundamentalDiagram the fundamental diagram
	 */
	public void setFundamentalDiagram(final FundamentalDiagram fundamentalDiagram) {
		this.fundamentalDiagram = fundamentalDiagram;
	}

	/** The node model to use
	 * @param nodeModel to use
	 */
	public void setNodeModel(final NodeModel nodeModel) {
		this.nodeModel = nodeModel;
	}

}

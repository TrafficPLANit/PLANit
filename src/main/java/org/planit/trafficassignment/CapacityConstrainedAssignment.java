package org.planit.trafficassignment;

import java.util.SortedMap;
import java.util.SortedSet;

import org.planit.dto.ResultDto;
import org.planit.exceptions.PlanItException;
import org.planit.supply.fundamentaldiagram.FundamentalDiagram;
import org.planit.supply.network.nodemodel.NodeModel;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.builder.CapacityConstrainedTrafficAssignmentBuilder;
import org.planit.trafficassignment.builder.TrafficAssignmentBuilder;
import org.planit.userclass.Mode;

public class CapacityConstrainedAssignment extends TrafficAssignment {
	
	// PROTECTED
	
	/**
	 * The builder for all capacity constrained traffic assignment instances
	 */
	protected final CapacityConstrainedTrafficAssignmentBuilder capacityConstrainedBuilder;
	
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
		this.capacityConstrainedBuilder = new CapacityConstrainedTrafficAssignmentBuilder(this);
	}

	/** collect the CapacityConstrainedTrafficAssignmentBuilder to condigure this instance
	 * @see org.planit.trafficassignment.TrafficAssignment#getBuilder()
	 * 
	 * @return capacityConstrainedAssignmentBuilder
	 */
	@Override
	public TrafficAssignmentBuilder getBuilder() {
		capacityConstrainedBuilder.setEventManager(eventManager);
		return capacityConstrainedBuilder;
    }

	@Override
	public void verifyComponentCompatibility() throws PlanItException {
		throw new PlanItException("Not yet implemented");
	}
	
	@Override
	public SortedMap<TimePeriod, SortedMap<Mode, SortedSet<ResultDto>>> executeEquilibration() throws PlanItException {
		throw new PlanItException("Not yet implemented");		
	}

	@Override
	public void initialiseBeforeEquilibration() {
		// TODO Auto-generated method stub		
	}	

	
	// Getters - Setters
	
	public void setFundamentalDiagram(FundamentalDiagram fundamentalDiagram) {
		this.fundamentalDiagram = fundamentalDiagram;
	}

	public void setNodeModel(NodeModel nodeModel) {
		this.nodeModel = nodeModel;
	}


}

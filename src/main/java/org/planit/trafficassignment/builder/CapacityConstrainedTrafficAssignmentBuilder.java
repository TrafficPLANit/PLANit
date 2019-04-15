package org.planit.trafficassignment.builder;

import org.planit.event.management.EventManager;
import org.planit.exceptions.PlanItException;
import org.planit.supply.fundamentaldiagram.FundamentalDiagram;
import org.planit.supply.network.nodemodel.NodeModel;
import org.planit.trafficassignment.CapacityConstrainedAssignment;
import org.planit.trafficassignment.TrafficAssignmentComponentFactory;

/**
 * When capacity constraints are in place we make a distinction between inflow and outflow and we assume
 * the difference between the two is governed by a fundamental diagram. 
 * 
 * @author markr
 *
 */
public class CapacityConstrainedTrafficAssignmentBuilder extends TrafficAssignmentBuilder {
	
	// FACTORIES
			
	/**
	 * fundamental diagram factory to create fundamental diagrams used to populate the link model
	 */
	protected final TrafficAssignmentComponentFactory<FundamentalDiagram> fundamentalDiagramFactory;	
	
	/**
	 * fundamental diagram factory to create fundamental diagrams used to populate the link model
	 */
	protected final TrafficAssignmentComponentFactory<NodeModel> nodeModelFactory;	
	
/** 
 * Constructor
 * 
 * @param capacityConstrainedAssignment                   CapacityConstrainedAssignment object to be built
 */
	public CapacityConstrainedTrafficAssignmentBuilder(CapacityConstrainedAssignment capacityConstrainedAssignment) {
		super(capacityConstrainedAssignment);
		fundamentalDiagramFactory = new TrafficAssignmentComponentFactory<FundamentalDiagram>(FundamentalDiagram.class);
		nodeModelFactory = new TrafficAssignmentComponentFactory<NodeModel>(NodeModel.class);
	}

	// FACTORY METHODS	
	
/** 
 * Create and register FundamentalDiagram on assignment 
 * 
 * @param fundamentalDiagramType          the type of Fundamental Diagram to be created
 * @return                                                    FundamentalDiagram created
 * @throws PlanItException                         thrown if there is an error
 */
	public FundamentalDiagram createAndRegisterFundamentalDiagram(String fundamentalDiagramType) throws PlanItException {
		FundamentalDiagram createdFundamentalDiagram = fundamentalDiagramFactory.create(fundamentalDiagramType);
		((CapacityConstrainedAssignment) parentAssignment).setFundamentalDiagram(createdFundamentalDiagram);
		return createdFundamentalDiagram;
	}
	
/** 
 * Create and register NodeMode on assignment 
 * 
 * @param nodeModelType                    the type of Node Model to be built
 * @return                                              NodeModel created
 * @throws PlanItException                   thrown if there is an error
 */
	public NodeModel createAndRegisterNodeModel(String nodeModelType) throws PlanItException {
		NodeModel createdNodeModel = nodeModelFactory.create(nodeModelType);
		((CapacityConstrainedAssignment) parentAssignment).setNodeModel(createdNodeModel);
		return createdNodeModel;
	}

/**
 * Set the EventManager for this builder
 * 
 * EventManager must be a singleton for each PlanItProject
 * 
 * @param eventManager               EventManager to be used to generate components
 */
	@Override
	public void setEventManager(EventManager eventManager) {
		super.setEventManager(eventManager);
		fundamentalDiagramFactory.setEventManager(eventManager);
		nodeModelFactory.setEventManager(eventManager);
	}	
	
}

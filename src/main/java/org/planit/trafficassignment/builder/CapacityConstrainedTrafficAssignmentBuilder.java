package org.planit.trafficassignment.builder;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.planit.event.EventManager;
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
	
	/** Constructor
	 * 
	 * @param capacityConstrainedAssignment
	 */
	public CapacityConstrainedTrafficAssignmentBuilder(CapacityConstrainedAssignment capacityConstrainedAssignment) {
		super(capacityConstrainedAssignment);
		fundamentalDiagramFactory = new TrafficAssignmentComponentFactory<FundamentalDiagram>(FundamentalDiagram.class);
		nodeModelFactory = new TrafficAssignmentComponentFactory<NodeModel>(NodeModel.class);
	}

	// FACTORY METHODS	
	
	/** Create and register FD on assignment 
	 * @param fundamentalDiagramType
	 * @return createdFundamentalDiagram
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
	public FundamentalDiagram createAndRegisterFundamentalDiagram(String fundamentalDiagramType) throws InstantiationException, 
																						                                                                                                  IllegalAccessException, 
																						                                                                                                  IllegalArgumentException, 
																						                                                                                                  InvocationTargetException,
																						                                                                                                  NoSuchMethodException, 
																						                                                                                                  SecurityException, 
																						                                                                                                  ClassNotFoundException, 
																						                                                                                                  PlanItException, 
																						                                                                                                  IOException {
		FundamentalDiagram createdFundamentalDiagram = fundamentalDiagramFactory.create(fundamentalDiagramType);
		((CapacityConstrainedAssignment)parentAssignment).setFundamentalDiagram(createdFundamentalDiagram);
		return createdFundamentalDiagram;
	}
	
	/** Create and register FD on assignment 
	 * @param fundamentalDiagramType
	 * @return createdFundamentalDiagram
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
	public NodeModel createAndRegisterNodeModel(String nodeModelType) throws InstantiationException, 
	                                                                         IllegalAccessException, 
	                                                                         IllegalArgumentException, 
	                                                                         InvocationTargetException, 
	                                                                         NoSuchMethodException, 
	                                                                         SecurityException, 
	                                                                         ClassNotFoundException, 
	                                                                         PlanItException, 
	                                                                         IOException {
		NodeModel createdNodeModel = nodeModelFactory.create(nodeModelType);
		((CapacityConstrainedAssignment)parentAssignment).setNodeModel(createdNodeModel);
		return createdNodeModel;
	}

	@Override
	public void setEventManager(EventManager eventManager) {
		super.setEventManager(eventManager);
		fundamentalDiagramFactory.setEventManager(eventManager);
		nodeModelFactory.setEventManager(eventManager);
	}	
	
}

package org.planit;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.planit.cost.physical.BPRLinkTravelTimeCost;
import org.planit.cost.virtual.SpeedConnectoidTravelTimeCost;
import org.planit.demand.Demands;
import org.planit.event.EventManager;
import org.planit.event.ProjectBuilderListener;
import org.planit.event.SimpleEventManager;
import org.planit.exceptions.PlanItException;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.project.PlanItProject;
import org.planit.sdinteraction.smoothing.MSASmoothing;
import org.planit.trafficassignment.DeterministicTrafficAssignment;
import org.planit.trafficassignment.TraditionalStaticAssignment;
import org.planit.trafficassignment.builder.CapacityRestrainedTrafficAssignmentBuilder;
import org.planit.zoning.Zoning;

public interface PlanItProjectBuilder {

	public static  PlanItProject buildCapacityRestrainedPlanitProject(ProjectBuilderListener projectBuilderListener) throws InstantiationException, 
																																																	 IllegalAccessException, 
																																																	 IllegalArgumentException, 
																																																	 InvocationTargetException, 
																																																	 NoSuchMethodException, 
																																																	 SecurityException, 
																																																	 ClassNotFoundException, 
																																																	 PlanItException, 
																																																	 IOException {

		// DEFINE EVENT MANAGER
		EventManager eventManager = new SimpleEventManager();
		eventManager.addEventListener(projectBuilderListener);
		
		PlanItProject project = new PlanItProject();
		project.setEventManager(eventManager);
		
		//RAW INPUT START --------------------------------
		PhysicalNetwork network = project.createAndRegisterPhysicalNetwork(MacroscopicNetwork.class.getCanonicalName());
		Demands demands = project.createAndRegisterDemands(); 							
		Zoning zoning = project.createAndRegisterZoning();
		//RAW INPUT END -----------------------------------	
		
		// TRAFFIC ASSIGNMENT START------------------------	
		
		DeterministicTrafficAssignment assignment = project.createAndRegisterDeterministicAssignment(TraditionalStaticAssignment.class.getCanonicalName());		
		CapacityRestrainedTrafficAssignmentBuilder taBuilder = (CapacityRestrainedTrafficAssignmentBuilder) assignment.getBuilder();
		
		// SUPPLY SIDE
		taBuilder.registerPhysicalNetwork(network);								
		// SUPPLY-DEMAND INTERACTIONS
		taBuilder.createAndRegisterPhysicalTravelTimeCostFunction(BPRLinkTravelTimeCost.class.getCanonicalName());
		taBuilder.createAndRegisterVirtualTravelTimeCostFunction(SpeedConnectoidTravelTimeCost.class.getCanonicalName()); 		
		taBuilder.createAndRegisterSmoothing(MSASmoothing.class.getCanonicalName());					
		// SUPPLY-DEMAND INTERFACE
		taBuilder.registerZoning(zoning);
		
		// DEMAND SIDE	
		taBuilder.registerDemands(demands);	
		
		return project;
	}
}

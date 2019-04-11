package org.planit.trafficassignment.builder;

import javax.annotation.Nonnull;

import org.planit.demand.Demands;
import org.planit.event.EventHandler;
import org.planit.event.EventManager;
import org.planit.exceptions.PlanItException;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.sdinteraction.smoothing.Smoothing;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.trafficassignment.TrafficAssignmentComponentFactory;
import org.planit.zoning.Zoning;

/**
 * All traffic assignment instances require a network, demand, and (equilibrium) smoothing procedure, all of which should be
 * registered via this generic builder. Specific traffic assignment methods might require special builders derived from this builder
 * 
 * @author markr
 *
 */
public abstract class TrafficAssignmentBuilder implements EventHandler {
	
	protected final TrafficAssignmentComponentFactory<Smoothing> smoothingFactory;
	/**
	 * The assignment all components will be registered on
	 */
	protected final TrafficAssignment parentAssignment;
	
	// PUBLIC
	
	/** Constructor 
	 * @param parentAssignment
	 */
	TrafficAssignmentBuilder(@Nonnull TrafficAssignment parentAssignment){
		this.parentAssignment = parentAssignment;
		 smoothingFactory = new TrafficAssignmentComponentFactory<Smoothing>(Smoothing.class);
	}
	
	// 	PUBLIC FACTORY METHODS	
	
	/** Create and Register smoothing component
	 * @param smoothingType
	 * @return smoothing, that was registered
	 * @throws PlanItException 
	 */
	public Smoothing createAndRegisterSmoothing(String smoothingType) throws PlanItException {
		Smoothing smoothing = smoothingFactory.create(smoothingType);
		parentAssignment.setSmoothing(smoothing);
		return smoothing;
	}	
	
	/** Register network
	 * @param network
	 */
	public void registerPhysicalNetwork(PhysicalNetwork network){
		parentAssignment.setPhysicalNetwork(network);
	}	
	
	/** Register demand
	 * @param network
	 */
	public void registerDemands(Demands demands){
		parentAssignment.setDemands(demands);
	}	
	
	public void registerZoning(Zoning zoning) {
		parentAssignment.setZoning(zoning);
	}

	public void setEventManager(EventManager eventManager) {
		smoothingFactory.setEventManager(eventManager);
	}	
	
}

package org.planit.trafficassignment.builder;

import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.planit.demand.Demands;
import org.planit.event.management.EventHandler;
import org.planit.event.management.EventManager;
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
	
    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(TrafficAssignmentBuilder.class.getName());
        
 /**
  * The smoothing factory used in the assignment algorithm
  */
	protected final TrafficAssignmentComponentFactory<Smoothing> smoothingFactory;
	
/**
 * The assignment all components will be registered on
 */
	protected final TrafficAssignment parentAssignment;
	
	// PUBLIC
	
/** 
 * Constructor
 *  
 * @param parentAssignment    parent traffic assignment object for this builder
 */
	TrafficAssignmentBuilder(@Nonnull TrafficAssignment parentAssignment){
		this.parentAssignment = parentAssignment;
		smoothingFactory = new TrafficAssignmentComponentFactory<Smoothing>(Smoothing.class);
	}
	
	// 	PUBLIC FACTORY METHODS	
	
/** 
 * Create and Register smoothing component
 * 
 * @param smoothingType         the type of smoothing component to be created
 * @return                                  Smoothing object created
 * @throws PlanItException       thrown if there is an error
 */
	public Smoothing createAndRegisterSmoothing(String smoothingType) throws PlanItException {
		Smoothing smoothing = smoothingFactory.create(smoothingType);
		parentAssignment.setSmoothing(smoothing);
		return smoothing;
	}	
	
/** 
 * Register physical network object
 * 
 * @param network    PhysicalNetwork object be registered
 */
	public void registerPhysicalNetwork(PhysicalNetwork network){
		parentAssignment.setPhysicalNetwork(network);
	}	
	
/** 
 * Register demands object
 * 
 * @param demands      Demands object to be registered
 */
	public void registerDemands(Demands demands){
		parentAssignment.setDemands(demands);
	}	
	
/**
 * Register zoning object
 * 
 * @param zoning     Zoning object to be registered
 */
	public void registerZoning(Zoning zoning) {
		parentAssignment.setZoning(zoning);
	}

/**
 * Set the EventManager for this builder
 * 
 * The EventManager must be a singleton for each PlanItProject application.
 * 
 * @param eventManager      EventManager to be used to create traffic assignment
 */
	public void setEventManager(EventManager eventManager) {
		smoothingFactory.setEventManager(eventManager);
	}	
	
}

package org.planit.demand;

import java.util.logging.Logger;

import org.planit.utils.IdGenerator;

/**
 * Base class for od-based trips
 * 
 * @author markr
 *
 */
public abstract class ODDemand {

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(ODDemand.class.getName());
        
/**
 * Unique demand id
 */
	protected final long id;
	
/**
 * Constructor
 */
	public ODDemand() {
		this.id = IdGenerator.generateId(ODDemand.class);
	}
	
/** 
 * Set a value (od flow)
 * 
 * @param originZone						the origin zone for the demand
 * @param destinationZone			the destination zone for the demand
 * @param odTripFlowRate 			desired od trip flow in pcu/h
 */
	public abstract void set(long originZone, long destinationZone, double odTripFlowRate);
	
/** 
 * Collect a value (odFlow) from a specified row and column
 * 
 * @param originZone						the origin zone for the demand
 * @param destinationZone			the destination zone for the demand
 * @return odFlow 						retrieved od trip flow in pcu/h
 */
	public abstract double get(long originZone, long destinationZone);
	
/**
 * Collect a dedicated iterator for od demands
 * 
 * @return odDemandIterator
 */
	public abstract ODDemandIterator iterator();
			
		
	// Getters-Setters

/**
 * Return the id of this ODDemand object
 * 
 * @return			id of this ODDemand object
 */
	public long getId() {
		return id;
	}	
}

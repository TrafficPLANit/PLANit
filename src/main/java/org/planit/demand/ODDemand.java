package org.planit.demand;

import org.planit.utils.IdGenerator;

/**
 * Base class for od-based trips
 * 
 * @author markr
 *
 */
public abstract class ODDemand {

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
	
	/** Set a value (od flow)
	 * @param originZone
	 * @param destinationZone
	 * @param odTripFlowRate, desired od trip flow in pcu/h
	 */
	public abstract void set(long originZone, long destinationZone, double odTripFlowRate);
	
	/** Collect a value (odFlow) from a specified row and column
	 * @param originZone
	 * @param destinationZone
	 * @return odFlow
	 */
	public abstract double get(long originZone, long destinationZone);
	
	/** Collect a dedicated iterator for od demands
	 * @return odDemandIterator
	 */
	public abstract ODDemandIterator iterator();
			
		
	// Getters-Setters

	public long getId() {
		return id;
	}	
}

package org.planit.trafficassignment;

import org.planit.event.management.EventHandler;
import org.planit.event.management.EventManager;

/**
 * Traffic assignment components are the main building blocks to conduct traffic assignment on
 * @author markr
 *
 */
public abstract class TrafficAssignmentComponent<T extends TrafficAssignmentComponent<T>> implements  EventHandler {

/**
 * Traffic component type used to identify the component uniquely. If not provided to the constructor
 * the class name is used
 */
	protected final String trafficComponentType;
	protected EventManager eventManager;
	
/**
 * Constructor
 */
	protected TrafficAssignmentComponent(){
		this.trafficComponentType = this.getClass().getCanonicalName();
	}	
	
	// Public

	public String getTrafficComponentType() {
		return trafficComponentType;
	}
	
	public void setEventManager(EventManager eventManager) {
		this.eventManager = eventManager;
	}
}

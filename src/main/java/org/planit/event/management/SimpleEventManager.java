package org.planit.event.management;

import org.planit.event.Event;
import org.planit.exceptions.PlanItException;

public class SimpleEventManager implements EventManager {

	/**
	 * Single event dispatcher across all projects (for now)
	 */
	private EventDispatcher eventDispatcher;

	public SimpleEventManager() {
		eventDispatcher = new EventDispatcher();
	}
	
	/** dispatch an event into the PlanIt project 
	 */
	public void dispatchEvent(Event e) throws PlanItException {
		try {
			eventDispatcher.dispatch(e);
		} catch (Exception ex) {
			throw new PlanItException(ex);
		}
	}
		
	/**
	 * Add eventListener to the project
	 * @param eventListener
	 */
	public void addEventListener(EventListener eventListener) {
		eventDispatcher.addEventListener(eventListener); 		
	}

}

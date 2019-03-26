package org.planit.event;

import java.io.IOException;

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
	public void dispatchEvent(Event e) throws IOException {
		eventDispatcher.dispatch(e);
	}
		
	/**
	 * Add eventListener to the project
	 * @param eventListener
	 */
	public void addEventListener(EventListener eventListener) {
		eventDispatcher.addEventListener(eventListener); 		
	}

}

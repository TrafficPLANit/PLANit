package org.planit.event.management;

import org.planit.event.Event;
import org.planit.exceptions.PlanItException;

public interface EventManager {

	public void dispatchEvent(Event e) throws PlanItException;
		
	/**
	 * Add eventListener to the project
	 * @param eventListener
	 */
	public void addEventListener(EventListener eventListener);	

}

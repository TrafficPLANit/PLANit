package org.planit.event;

import org.planit.event.listener.EventListener;
import org.planit.exceptions.PlanItException;

public interface EventManager {

	public void dispatchEvent(Event e) throws PlanItException;
		
	/**
	 * Add eventListener to the project
	 * @param eventListener
	 */
	public void addEventListener(EventListener eventListener);	

}

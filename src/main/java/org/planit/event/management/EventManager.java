package org.planit.event.management;


import org.planit.event.Event;
import org.planit.event.listener.EventListener;
import org.planit.exceptions.PlanItException;


public interface EventManager {

/**
 * Dispatches Event
 * 
 * @param event                       Event to be dispatched
 * @throws PlanItException      thrown if there is an error
 */
	public void dispatchEvent(Event event) throws PlanItException;

		
/**
 * Add eventListener to the project
 * 
 * @param eventListener      EventListener to be added to the project
 */
	public void addEventListener(EventListener eventListener);	



}
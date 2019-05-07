package org.planit.event.management;

import org.planit.event.listener.EventListener;

import java.util.logging.Logger;

import org.planit.event.Event;
import org.planit.exceptions.PlanItException;

/**
 * Implementation of EventManager
 * 
 * @author gman6028
 *
 */
public class SimpleEventManager implements EventManager {

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(SimpleEventManager.class.getName());
        
/**
 * Single event dispatcher across all projects (for now)
 */
	private EventDispatcher eventDispatcher;

/**
 * Constructor which instantiates the EventDispatcher
 */
	public SimpleEventManager() {
		eventDispatcher = new EventDispatcher();
	}
	
/** 
 * Dispatch an event into the PlanIt project 
 * 
 * @param event                       Event to be dispatched
 * @throws PlanItException     thrown if there is an error
 */
	public void dispatchEvent(Event event) throws PlanItException {
		try {
			eventDispatcher.dispatch(event);
		} catch (Exception ex) {
			throw new PlanItException(ex);
		}
	}
		
/**
 * Add eventListener to the project
 * 
 * @param eventListener      EventListener to be added
 */
	public void addEventListener(EventListener eventListener) {
		eventDispatcher.addEventListener(eventListener); 		
	}

}
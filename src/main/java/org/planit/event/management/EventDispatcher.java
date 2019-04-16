package org.planit.event.management;

import java.util.HashSet;
import java.util.logging.Logger;

import org.planit.event.Event;
import org.planit.event.listener.EventListener;
import org.planit.exceptions.PlanItException;

/**
 * Dispatches events to registered listeners
 * @author markr
 *
 */
public class EventDispatcher {
	
    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(EventDispatcher.class.getName());
        
	/**
	 * Registered listeners
	 */
	protected final HashSet<EventListener> registeredListeners = new HashSet<EventListener>();
	
	/**
	 * Constructor
	 */
	public EventDispatcher() {
	}

/** Register a new listener 
 * 
 * @param eventListener          eventListener to be registered with this dispatcher
 */
	public void addEventListener(EventListener eventListener) {
		registeredListeners.add(eventListener);
	}
		
/** 
 * Dispatch event to eligible registered listeners for processing
 * 
 * @param event                      Event to be dispatched
 * @throws PlanItException     thrown if there is an error
 */
	public void dispatch(Event event) throws PlanItException {
		try {
			for (EventListener listener : registeredListeners) {
				if(event.isProcessedBy(listener)) {
					listener.process(event);
				}
			}
		} catch (Exception ex) {
			throw new PlanItException(ex);
		}
			
	}
			
}

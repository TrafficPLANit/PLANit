package org.planit.event;

import java.io.IOException;
import java.util.HashSet;


/**
 * Dispatches events to registered listeners
 * @author markr
 *
 */
public class EventDispatcher {
	
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
	 * @param eventListener
	 */
	public void addEventListener(EventListener eventListener) {
		registeredListeners.add(eventListener);
	}
		
	/** Dispatch event to eligible registered listeners for processing
	 * @param event
	 */
/*
	public void dispatch(Event event){
		registeredListeners.forEach( listener -> 
			{ if(event.isProcessedBy(listener)) {
				listener.process(event);}
			});
	}	
*/	
	public void dispatch(Event event) throws IOException {
		for (EventListener listener : registeredListeners) {
			if(event.isProcessedBy(listener)) {
				listener.process(event);
			}
		}
			
	}
			
}

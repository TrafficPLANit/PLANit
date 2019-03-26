package org.planit.event;

/**
 * Objects must implement this interface if they are required to handle events
 * 
 * Note that the EventManager which is injected must be a singleton for the application.  This means it must have been instantiated by the top-level project, which then injects it into every object which needs it.
 * 
 * @author gman6028
 *
 */
public interface EventHandler {
	
/**
 * Injects the event manager 
 * 
 * @param eventManager		the EventManager to be used
 */
	public void setEventManager(EventManager eventManager) ;
	
}

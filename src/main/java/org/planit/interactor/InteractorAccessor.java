package org.planit.interactor;

import org.djutils.event.EventType;

/**
 * Interactor accessor
 * 
 * @author markr
 *
 */
public interface InteractorAccessor {
	
	/** each interactor that requires access from an accessee indicates what accessee is requested
	 * via the event type
	 * @return event type indicating that accessee is requiested
	 */
	EventType getRequestedAccesseeEventType();

}

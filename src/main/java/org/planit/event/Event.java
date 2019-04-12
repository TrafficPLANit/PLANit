package org.planit.event;

import org.planit.event.management.EventListener;

/**
 * Base event class
 * @author markr
 *
 */
public interface Event {
	
	/** The event listener this event is processed by
	 */
	public abstract boolean isProcessedBy(EventListener listener);
}

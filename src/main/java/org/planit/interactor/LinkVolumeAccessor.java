package org.planit.interactor;

import org.djutils.event.EventListenerInterface;
import org.djutils.event.EventType;

/**
 * Link Volume Accessor interface
 * 
 * @author markr
 *
 */
public interface LinkVolumeAccessor extends InteractorAccessor, EventListenerInterface {

	/** event type fired off when a new request for an interactor of type link volume accessee is created */
	public static final EventType INTERACTOR_REQUEST_LINKVOLUMEACCESSEE_TYPE = new EventType("INTERACTOR.REQUEST.LINKVOLUMEACCESSEE.TYPE");
	
	/** each interactor that requires access from an accessee indicates what accessee is requested
	 * via the event type
	 * @return event type indicating that accessee is requiested
	 */
	default EventType getRequestedAccesseeEventType() {
		return INTERACTOR_REQUEST_LINKVOLUMEACCESSEE_TYPE;
	}
	
}

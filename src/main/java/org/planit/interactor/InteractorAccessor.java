package org.planit.interactor;

import org.djutils.event.EventType;

/**
 * Interactor accessor. The accessor accesses its accessee. The benefit of using the approach is that it allows for completely decoupled data exchange. Any
 * type of information can be echanged by any two classes of any type as long as they know what they want from each other when interacting. The accessor is dominant in the sense
 * that is dictates what it wants to access while the accessee provides what the accessor wants to obtain by implementing the methods specific to the interaction at hand.
 * 
 * To obtain the accessee to access fata from, it is expected of the accessor to request for the accessee
 * either by firing of this request itself or by a proxy. Either way whoever fires the request must know who is listening to this event because it must register the listeners
 * to this accessor before firing of this event. Otherwise noone is notified of the event.
 * 
 * The {@link #getRequestedAccesseeEventType()} provides the event type for the concrete accessor that derives from this interface
 * 
 * 
 * Further, both for the accessee related event types as well as the accessor related event types, the content consists of the respective accessor or accessee
 * depending on if it is a request for an accessee (then the accessor is provided as content), or when the accessee is provided (then the accessee is the content).
 * This allows the accessor access to the accessee once it has parsed the contents of the response event from the accessee.
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

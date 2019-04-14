package org.planit.event.listener;

import org.planit.event.Event;
import org.planit.exceptions.PlanItException;

/**
 * PlaceHolder for any event listener within this project. This listener processes only events that are derived from T
 * where T is a type of event
 * 
 * @author markr
 *
 */
public interface EventListener {
	
/** 
 * Process the event
 * 
 * @param event							event to be processed
 * @throws PlanItException		thrown if there is an error
 */
	public void process(Event event) throws PlanItException;	
	
}

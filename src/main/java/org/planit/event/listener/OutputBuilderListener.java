package org.planit.event.listener;

import org.planit.event.CreatedOutputWriterEvent;
import org.planit.event.Event;
import org.planit.exceptions.PlanItException;

public interface OutputBuilderListener extends EventListener {

/** 
 * Process event and call the right onX method
 * 
 * @param event                      Event to be processed
 * @throws PlanItException    thrown if there is an error
 * @see org.planit.event.listener.EventListener#process(org.planit.event.Event)
 */
	@Override
	default public void process(Event event)  throws PlanItException {
		try {
			if (event instanceof CreatedOutputWriterEvent) {
				onCreateOutputWriter((CreatedOutputWriterEvent) event);
			}
		} catch (Exception ex) {
			throw new PlanItException(ex);
		}
	}
	
/** 
 * Whenever an output writer is created this method will be invoked
 * 
 * @param event                     Event containing the created (and unconfigured) output writer
 * @throws PlanItException    thrown if there is an error
 */
	public void onCreateOutputWriter(CreatedOutputWriterEvent event) throws PlanItException;
}

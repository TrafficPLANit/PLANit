package org.planit.event.listener;

import org.planit.event.CreatedOutputWriterEvent;
import org.planit.event.CreatedProjectComponentEvent;
import org.planit.event.Event;
import org.planit.exceptions.PlanItException;

public interface OutputBuilderListener extends EventListener {

	/** process event and call the right onX method
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
	
	/** Whenever an output writer is created this method will be invoked
	 * @param e, event containing the created (and unconfigured) output writer
	 */
	public void onCreateOutputWriter(CreatedOutputWriterEvent e) throws PlanItException;
}

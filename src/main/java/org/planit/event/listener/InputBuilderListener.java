package org.planit.event.listener;

import org.planit.event.CreatedProjectComponentEvent;
import org.planit.event.Event;
import org.planit.exceptions.PlanItException;

/**
 * Listener which is invoked whenever a project component is created. To be used to populate the
 * project components from some data source
 * 
 * @author markr
 *
 */
public interface InputBuilderListener extends EventListener {
	
	
	/** process event and call the right onX method
	 * @see org.planit.event.listener.EventListener#process(org.planit.event.Event)
	 */
	@Override
	default public void process(Event event)  throws PlanItException {
		try {
			if (event instanceof CreatedProjectComponentEvent<?>) {
				onCreateProjectComponent((CreatedProjectComponentEvent<?>) event);
			}
		} catch (Exception ex) {
			throw new PlanItException(ex);
		}
	}
	
	/** Whenever a project component is created this method will be invoked
	 * @param e, event containing the created (and empty) project component
	 */
	public void onCreateProjectComponent(CreatedProjectComponentEvent<?> e) throws PlanItException;
		
}

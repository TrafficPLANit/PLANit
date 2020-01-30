package org.planit.input;

import org.planit.event.CreatedProjectComponentEvent;
import org.planit.event.Event;
import org.planit.event.listener.EventListener;
import org.planit.exceptions.PlanItException;

/**
 * Listener which is invoked whenever a project component is created. To be used
 * to populate the project components from some data source
 * 
 * @author markr
 *
 */
public abstract class InputBuilderListener implements EventListener {
	
	/**
	 * Process event and call the right onX method
	 * 
	 * @see org.planit.event.listener.EventListener#process(org.planit.event.Event)
	 * 
	 * @param event Event to be processed
	 * @throws PlanItException thrown if there is an error
	 */
	@Override
	public void process(Event event) throws PlanItException {
		try {
			if (event instanceof CreatedProjectComponentEvent<?>) {
				onCreateProjectComponent((CreatedProjectComponentEvent<?>) event);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new PlanItException(ex);
		}
	}
	
	/**
	 * Whenever a project component is created this method will be invoked
	 * 
	 * @param event event containing the created (and empty) project component
	 * @throws PlanItException thrown if there is an error
	 */
	public abstract void onCreateProjectComponent(CreatedProjectComponentEvent<?> event) throws PlanItException;
	
}
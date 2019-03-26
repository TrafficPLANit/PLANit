package org.planit.event;

import java.io.IOException;

/**
 * Listener which is invoked whenever a project component is created. To be used to populate the
 * project components from some data source
 * 
 * @author markr
 *
 */
public interface ProjectBuilderListener extends EventListener {
	
	
	/** process event and call the right onX method
	 * @see org.planit.event.EventListener#process(org.planit.event.Event)
	 */
	@Override
	default public void process(Event e)  throws IOException {
		if(e instanceof CreatedProjectComponentEvent<?>) {
			onCreateProjectComponent((CreatedProjectComponentEvent<?>) e);
		}
	}
	
	/** Whenever a project component is created this method will be invoked
	 * @param e, event containing the created (and empty) project component
	 */
	public void onCreateProjectComponent(CreatedProjectComponentEvent<?> e) throws IOException;
	
}

package org.planit.event.listener;

import java.util.Map;

import org.planit.event.CreatedProjectComponentEvent;
import org.planit.event.Event;
import org.planit.exceptions.PlanItException;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegment;

/**
 * Listener which is invoked whenever a project component is created. To be used
 * to populate the project components from some data source
 * 
 * @author markr
 *
 */
public interface InputBuilderListener extends EventListener {

    /**
     * Process event and call the right onX method
     * 
     * @see org.planit.event.listener.EventListener#process(org.planit.event.Event)
     * 
     * @param event
     *            Event to be processed
     * @throws PlanItException
     *             thrown if there is an error
     */
    @Override
    default public void process(Event event) throws PlanItException {
        try {
            if (event instanceof CreatedProjectComponentEvent<?>) {
                onCreateProjectComponent((CreatedProjectComponentEvent<?>) event);
            }
        } catch (Exception ex) {
            throw new PlanItException(ex);
        }
    }

    /**
     * Whenever a project component is created this method will be invoked
     * 
     * @param event
     *            event containing the created (and empty) project component
     * @throws PlanItException
     *             thrown if there is an error
     */
    public void onCreateProjectComponent(CreatedProjectComponentEvent<?> event) throws PlanItException;

}

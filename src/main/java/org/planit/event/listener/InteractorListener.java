package org.planit.event.listener;

import org.planit.event.Event;
import org.planit.event.RequestAccesseeEvent;

/**
 * Listener for interactor events
 * 
 * @author markr
 *
 */
public interface InteractorListener extends EventListener {

    /**
     * Process interactor event
     * 
     * @param event
     *            Event to be processed
     * 
     */
    @Override
    default public void process(Event event) {
        if (event instanceof RequestAccesseeEvent) {
            onRequestInteractorEvent((RequestAccesseeEvent) event);
        }
    }

    public default void onRequestInteractorEvent(RequestAccesseeEvent event) {
        // TODO - this method is empty, do we still need it?
        // non-mandatory
    }

}

package org.planit.event;

import java.util.logging.Logger;

import org.planit.interactor.InteractorAccessor;

/**
 * Request accessee events
 * 
 * @author markr
 *
 */
public class RequestAccesseeEvent implements InteractorEvent {

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(RequestAccesseeEvent.class.getName());

    /**
     * Source accessor
     */
    protected InteractorAccessor sourceAccessor;

    public RequestAccesseeEvent(InteractorAccessor sourceAccessor) {
        this.sourceAccessor = sourceAccessor;
    }

    /**
     * Collect project component that was created
     * 
     * @return projectComponent
     */
    public InteractorAccessor getSourceAccessor() {
        return sourceAccessor;
    }
}

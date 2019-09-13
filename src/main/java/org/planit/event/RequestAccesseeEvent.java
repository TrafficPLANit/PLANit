package org.planit.event;

import org.planit.interactor.InteractorAccessor;

/**
 * Request accessee events
 * 
 * @author markr
 *
 */
public class RequestAccesseeEvent implements InteractorEvent {

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

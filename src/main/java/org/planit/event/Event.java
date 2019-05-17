package org.planit.event;

import org.planit.event.listener.EventListener;

/**
 * Base event class
 * 
 * @author markr
 *
 */
public interface Event {

    /**
     * Test whether this event is processed by a specified listener
     * 
     * @param listener
     *            EventListener this event is processed by
     * @return true if this event if processed by the specified listener
     */
    public abstract boolean isProcessedBy(EventListener listener);
}

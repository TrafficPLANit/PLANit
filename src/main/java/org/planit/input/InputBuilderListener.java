package org.planit.input;

import org.djutils.event.EventListenerInterface;

/**
 * Listener which is automatically registered to the creation of any traffic assignment component for
 * which it gets notified. @see #TrafficicAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE
 * 
 * @author markr
 *
 */
public abstract class InputBuilderListener implements EventListenerInterface {
	
	/** generated UID */
	private static final long serialVersionUID = 4223028100274802893L;
	
	
}
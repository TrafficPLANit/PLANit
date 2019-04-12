package org.planit.event;

import org.planit.builders.InputBuilder;
import org.planit.event.management.EventListener;

/**
 * All projectbuilder events should derive from this interface such that the listener can distinguish what to process
 * @author markr
 *
 */
public interface ProjectBuilderEvent extends Event {

	/** all project builder events are processed by the ProjectBuilderListener
	 * @see org.planit.event.Event#isProcessedBy(org.planit.event.EventListener)
	 * @return true, if event is a project builder related event
	 */
	@Override
	default public boolean isProcessedBy(EventListener listener) {
		return listener instanceof InputBuilder;
	}

}

package org.planit.event.listener;

import org.planit.event.Event;
import org.planit.event.RequestAccesseeEvent;

public interface InteractorListener extends EventListener {

	@Override
	default public void process(Event event) {
		if (event instanceof RequestAccesseeEvent) {
			onRequestInteractorEvent((RequestAccesseeEvent) event);
		}
	}
	
	public default void onRequestInteractorEvent(RequestAccesseeEvent event) {
		// non-mandatory
	}

}

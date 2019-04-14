package org.planit.event.listener;

import org.planit.event.Event;
import org.planit.event.RequestAccesseeEvent;

public interface InteractorListener extends EventListener {

	@Override
	default public void process(Event e) {
		if (e instanceof RequestAccesseeEvent) {
			onRequestInteractorEvent((RequestAccesseeEvent) e);
		}
	}
	
	public default void onRequestInteractorEvent(RequestAccesseeEvent e) {
		// non-mandatory
	}
	
}

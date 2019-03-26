package org.planit.event;

import java.io.IOException;

public interface EventManager {

	public void dispatchEvent(Event e) throws IOException;
		
	/**
	 * Add eventListener to the project
	 * @param eventListener
	 */
	public void addEventListener(EventListener eventListener);	

}

package org.planit.event;

import org.planit.interactor.InteractorAccessor;

public class RequestAccesseeEvent implements InteractorEvent {

	protected InteractorAccessor sourceAccessor;
			
	public RequestAccesseeEvent(InteractorAccessor sourceAccessor){
		this.sourceAccessor = sourceAccessor;
	}

	/** Collect project component that was created
	 * @return projectComponent
	 */
	public InteractorAccessor getSourceAccessor() {
		return sourceAccessor;
	}	
}

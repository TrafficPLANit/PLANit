package org.planit.interactor;

public interface InteractorAccessor {

	Class<? extends InteractorAccessee> getRequestedAccessee();
	
	void setAccessee(InteractorAccessee accessee);
	
}

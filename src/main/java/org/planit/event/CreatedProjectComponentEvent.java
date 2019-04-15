package org.planit.event;

import org.planit.trafficassignment.TrafficAssignmentComponent;

public class CreatedProjectComponentEvent<T extends TrafficAssignmentComponent<T>> implements InputBuilderEvent {

/**
 * component that was created
 */
	protected final T projectComponent;

/** 
 * Constructor
 * 
 * @param projectComponent      project component which is created
 */
	public CreatedProjectComponentEvent(T projectComponent) {
		this.projectComponent = projectComponent;		
	}
	
/** 
 * Collect project component that was created
 * 
 * @return projectComponent
 */
	public T getProjectComponent() {
		return projectComponent;
	}
}

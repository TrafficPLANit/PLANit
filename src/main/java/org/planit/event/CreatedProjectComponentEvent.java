package org.planit.event;

import java.util.logging.Logger;

import org.planit.trafficassignment.TrafficAssignmentComponent;

/**
 * Event which is generated when a component is created
 * 
 * @author gman6028
 *
 * @param <T>
 *            component created
 */
public class CreatedProjectComponentEvent<T extends TrafficAssignmentComponent<T>> implements InputBuilderEvent {

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(CreatedProjectComponentEvent.class.getName());

    /**
     * Component that was created
     */
    protected final T projectComponent;
    
    /**
     * First parameter to be used by the event, if required
     */
    protected Object parameter1;

    /**
     * Second parameter to be used by the event, if required
     */
    protected Object parameter2;
   
    /**
     * Constructor
     * 
     * @param projectComponent  project component which is created
     */
    public CreatedProjectComponentEvent(T projectComponent) {
        this.projectComponent = projectComponent;
        parameter1 = null;
        parameter2 = null;
    }
    
    /**
     * Constructor
     * 
     * @param projectComponent  project component which is created
     * @param parameter parameter to be used by the event
     */
    public CreatedProjectComponentEvent(T projectComponent, Object parameter) {
        this.projectComponent = projectComponent;
        this.parameter1 = parameter;
        parameter2 = null;
    }

    /**
     * Constructor
     * 
     * @param projectComponent  project component which is created
     * @param parameter1 first parameter to be used by the event
     * @param parameter2 second parameter to be used by the event
     */
    public CreatedProjectComponentEvent(T projectComponent, Object parameter1, Object parameter2) {
        this.projectComponent = projectComponent;
        this.parameter1 = parameter1;
        this.parameter2 = parameter2;
    }

    /**
     * Collect project component that was created
     * 
     * @return projectComponent
     */
    public T getProjectComponent() {
        return projectComponent;
    }
    
    public Object getParameter1() {
    	return parameter1;
    }
    
    public Object getParameter2() {
    	return parameter2;
    }
}

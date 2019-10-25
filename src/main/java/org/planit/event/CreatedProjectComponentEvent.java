package org.planit.event;

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
     * Third parameter to be used by the event, if required
     */
    protected Object parameter3;
   
   /**
     * Constructor
     * 
     * @param projectComponent  project component which is created
     */
    public CreatedProjectComponentEvent(T projectComponent) {
        this.projectComponent = projectComponent;
        parameter1 = null;
        parameter2 = null;
        parameter3 = null;
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
        parameter3 = null;
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
        parameter3 = null;
    }

    /**
     * Constructor
     * 
     * @param projectComponent  project component which is created
     * @param parameter1 first parameter to be used by the event
     * @param parameter2 second parameter to be used by the event
     * @param parameter3 third parameter to be used by the event
     */
    public CreatedProjectComponentEvent(T projectComponent, Object parameter1, Object parameter2, Object parameter3) {
        this.projectComponent = projectComponent;
        this.parameter1 = parameter1;
        this.parameter2 = parameter2;
        this.parameter3 = parameter3;
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
    
    public Object getParameter3() {
    	return parameter3;
    }
}

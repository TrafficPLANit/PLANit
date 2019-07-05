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
    
    protected Object parameter;

    /**
     * Constructor
     * 
     * @param projectComponent
     *            project component which is created
     */
    public CreatedProjectComponentEvent(T projectComponent) {
        this.projectComponent = projectComponent;
        parameter = null;
    }
    
    public CreatedProjectComponentEvent(T projectComponent, Object parameter) {
        this.projectComponent = projectComponent;
        this.parameter = parameter;
    }

    /**
     * Collect project component that was created
     * 
     * @return projectComponent
     */
    public T getProjectComponent() {
        return projectComponent;
    }
    
    public Object getParameter() {
    	return parameter;
    }
}

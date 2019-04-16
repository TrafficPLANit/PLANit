package org.planit.trafficassignment;

import java.util.logging.Logger;

import org.planit.event.listener.InteractorListener;
import org.planit.trafficassignment.builder.CapacityRestrainedTrafficAssignmentBuilder;
import org.planit.trafficassignment.builder.TrafficAssignmentBuilder;

/**
 * Capacity Restrained Deterministic Traffic Assignment
 * 
 * @author gman6028
 *
 */
public abstract class CapacityRestrainedAssignment extends DeterministicTrafficAssignment implements InteractorListener {

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(CapacityRestrainedAssignment.class.getName());
        
	/**
	 * The builder for all capacity restrained traffic assignment instances
	 */
	protected final CapacityRestrainedTrafficAssignmentBuilder capacityRestrainedBuilder;
	
	
/**
 * Constructor for CapacityRestrainedAssignment
 */
	public CapacityRestrainedAssignment() {
		super();
		capacityRestrainedBuilder = new CapacityRestrainedTrafficAssignmentBuilder(this);
	}

/** 
 * Returns a builder for CapacityRestrainedAssignment
 * 
 * @see org.planit.trafficassignment.TrafficAssignment#getBuilder()
 */
	@Override
	public TrafficAssignmentBuilder getBuilder() {
		capacityRestrainedBuilder.setEventManager(eventManager);
		return capacityRestrainedBuilder;
	}

}

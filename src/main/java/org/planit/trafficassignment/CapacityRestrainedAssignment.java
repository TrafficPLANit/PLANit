package org.planit.trafficassignment;

import org.planit.input.InputBuilderListener;
import org.planit.trafficassignment.builder.CapacityRestrainedTrafficAssignmentBuilder;
import org.planit.trafficassignment.builder.TrafficAssignmentBuilder;

/**
 * Capacity Restrained Deterministic Traffic Assignment
 *
 * @author gman6028
 *
 */
public abstract class CapacityRestrainedAssignment extends TrafficAssignment {

    /** generated UID */
	private static final long serialVersionUID = -2109589077398520002L;
	/**
     * The builder for all capacity restrained traffic assignment instances
     */
    protected CapacityRestrainedTrafficAssignmentBuilder capacityRestrainedBuilder;

    /**
     * Constructor for CapacityRestrainedAssignment
     */
    public CapacityRestrainedAssignment() {
        super();
    }

    /**
     * Returns a builder for CapacityRestrainedAssignment
     *
     * @see org.planit.trafficassignment.TrafficAssignment#getBuilder()
     */
    @Override
    public TrafficAssignmentBuilder collectBuilder(final InputBuilderListener trafficComponentCreateListener) {
    	if(capacityRestrainedBuilder==null) {
    		capacityRestrainedBuilder = new CapacityRestrainedTrafficAssignmentBuilder(this, trafficComponentCreateListener);
    	}
        return capacityRestrainedBuilder;
    }

}

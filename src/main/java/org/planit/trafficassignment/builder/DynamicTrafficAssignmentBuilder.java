package org.planit.trafficassignment.builder;

import org.planit.input.InputBuilderListener;
import org.planit.trafficassignment.DynamicTrafficAssignment;

/**
 * A dynamic traffic assignment builder is assumed to only support capacity constrained traffic assignment
 * instances. It is used to build the traffic assignment instance with the proper configuration settings
 *
 * @author markr
 *
 */
public class DynamicTrafficAssignmentBuilder extends CapacityConstrainedTrafficAssignmentBuilder {

	/** Constructor
	 *
	 * @param dynamicAssignment the dynamic assignment
	 * @param trafficComponentCreateListener the listener for further traffic components that are created by the builder
	 */
	public DynamicTrafficAssignmentBuilder(final DynamicTrafficAssignment dynamicTrafficAssignment,
			final InputBuilderListener trafficComponentCreateListener) {
		super(dynamicTrafficAssignment, trafficComponentCreateListener);
	}

}

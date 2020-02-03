package org.planit.trafficassignment.builder;

import org.planit.input.InputBuilderListener;
import org.planit.trafficassignment.TrafficAssignment;

/**
 * Builder for a traditional static assignment
 *
 * @author markr
 *
 */
public class TraditionalStaticAssignmentBuilder extends TrafficAssignmentBuilder {

	/** Constructor
	 * @param parentAssignment
	 * @param trafficComponentCreateListener
	 */
	public TraditionalStaticAssignmentBuilder(
			final TrafficAssignment parentAssignment, final InputBuilderListener trafficComponentCreateListener) {
		super(parentAssignment, trafficComponentCreateListener);
	}

}

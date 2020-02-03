package org.planit.trafficassignment.builder;

import org.planit.input.InputBuilderListener;
import org.planit.trafficassignment.TrafficAssignment;

/** Builder specific to deterministic traffic assignment components
 * @author markr
 *
 */
public abstract class DeterministicTrafficAssignmentBuilder extends TrafficAssignmentBuilder {

	DeterministicTrafficAssignmentBuilder(
			final TrafficAssignment parentAssignment, final InputBuilderListener trafficComponentCreateListener) {
		super(parentAssignment, trafficComponentCreateListener);
	}

}

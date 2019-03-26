package org.planit.cost.physical;

import org.planit.cost.Cost;
import org.planit.network.physical.LinkSegment;
import org.planit.trafficassignment.TrafficAssignmentComponent;

public abstract class PhysicalCost extends TrafficAssignmentComponent<PhysicalCost> implements Cost<LinkSegment> {

	public PhysicalCost() {
		super();
	}
}

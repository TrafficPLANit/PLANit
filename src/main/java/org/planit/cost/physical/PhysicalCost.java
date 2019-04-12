package org.planit.cost.physical;

import org.planit.cost.Cost;
import org.planit.network.physical.LinkSegment;
import org.planit.trafficassignment.TrafficAssignmentComponent;

/**
 * Object to handle the travel time cost of a physical link
 * 
 * @author gman6028
 *
 */
public abstract class PhysicalCost extends TrafficAssignmentComponent<PhysicalCost> implements Cost<LinkSegment> {

	public PhysicalCost() {
		super();
	}
}

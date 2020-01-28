package org.planit.cost.physical.initial;

import org.planit.utils.network.physical.Mode;

/**
 * Initial Link Segment Cost for which all the link segments have the same cost value for a specified mode
 * 
 * @author gman6028
 *
 */
public class ManualInitialLinkSegmentCost extends InitialLinkSegmentCost {
	
	public ManualInitialLinkSegmentCost() {
		super();
	}

	/**
	 * Sets a given cost for all link segments for a given mode
	 * 
	 * @param mode the specified mode
	 * @param cost the cost of travel to be used
	 * @param noLinkSegments the number of link segments
	 */
	public void setAllSegmentCosts(Mode mode, double cost, int noLinkSegments) {
		for (long i=0; i<noLinkSegments; i++) {
			setSegmentCost(mode, i, cost);
		}
	}
}

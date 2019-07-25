package org.planit.cost.physical.initial;

import org.planit.userclass.Mode;

/**
 * Initial Link Segment Cost which 
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
	 */
	public void setAllSegmentCosts(Mode mode, double cost) {
		
		if (!costPerModeAndLinkSegment.keySet().contains(mode.getId())) {
			costPerModeAndLinkSegment.put(mode.getId(), new double[noLinkSegments]);
		}
		for (int i=0; i<noLinkSegments; i++) {
			costPerModeAndLinkSegment.get(mode.getId())[i] = cost;
		}
	}

}

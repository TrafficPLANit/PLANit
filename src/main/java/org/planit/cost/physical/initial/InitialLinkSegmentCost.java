package org.planit.cost.physical.initial;

import org.planit.network.physical.LinkSegment;
import org.planit.userclass.Mode;

import java.util.HashMap;
import java.util.Map;

/**
 * Initial Link Segment Costs
 * 
 * @author gman6028
 *
 */
public class InitialLinkSegmentCost extends InitialPhysicalCost {
	
	protected Map<Mode, Map<LinkSegment, Double>> costPerModeAndLinkSegment;

    /**
     * Constructor
     */
	public InitialLinkSegmentCost() {
		super();
		costPerModeAndLinkSegment = new HashMap<Mode, Map<LinkSegment, Double>>();
	}
	
	/**
	 * Returns the initial cost for each link segment and mode
	 * 
	 * @param mode the current mode
	 * @param linkSegment the current link segment
	 * @return the cost for this link segment and mode
	 */
	@Override
	public double getSegmentCost(Mode mode, LinkSegment linkSegment) {
		Map<LinkSegment, Double> linkSegmentMap = costPerModeAndLinkSegment.get(mode);
		double cost = linkSegmentMap.get(linkSegment);
		return cost;
	}

	/**
	 * Sets the initial cost for each link segment and mode
	 * 
	 * @param mode the current mode
	 * @param linkSegment the current link segment
	 * @param cost the initial cost for this link segment and mode
	 */
	@Override
	public void setSegmentCost(Mode mode, LinkSegment linkSegment, double cost) {
		if (!costPerModeAndLinkSegment.keySet().contains(mode)) {
			costPerModeAndLinkSegment.put(mode, new HashMap<LinkSegment, Double>());
		}
		costPerModeAndLinkSegment.get(mode).put(linkSegment, cost);
	}
	
}

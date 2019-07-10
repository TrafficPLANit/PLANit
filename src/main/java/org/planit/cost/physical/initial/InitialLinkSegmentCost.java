package org.planit.cost.physical.initial;

import org.planit.exceptions.PlanItException;
import org.planit.network.physical.LinkSegment;
import org.planit.network.physical.PhysicalNetwork;
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
	
	public double calculateSegmentCost(Mode mode, LinkSegment edgeSegment) throws PlanItException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void initialiseBeforeEquilibration(PhysicalNetwork physicalNetwork) throws PlanItException {
		// TODO Auto-generated method stub

	}
	
	public void addValue(Mode mode, LinkSegment linkSegment, double value) {
		if (!costPerModeAndLinkSegment.keySet().contains(mode)) {
			costPerModeAndLinkSegment.put(mode, new HashMap<LinkSegment, Double>());
		}
		costPerModeAndLinkSegment.get(mode).put(linkSegment, value);
	}
	
	public double getValue(Mode mode, LinkSegment linkSegment) {
		return costPerModeAndLinkSegment.get(mode).get(linkSegment);
	}

}

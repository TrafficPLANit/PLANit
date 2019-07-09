package org.planit.cost.physical;

import org.planit.exceptions.PlanItException;
import org.planit.network.physical.LinkSegment;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.userclass.Mode;

public class InitialLinkSegmentCost extends InitialPhysicalCost {

    /**
     * Constructor
     */
	public InitialLinkSegmentCost() {
		super();
	}
	
	public double calculateSegmentCost(Mode mode, LinkSegment edgeSegment) throws PlanItException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void initialiseBeforeEquilibration(PhysicalNetwork physicalNetwork) throws PlanItException {
		// TODO Auto-generated method stub

	}

}

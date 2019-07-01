package org.planit.output.property;

import org.planit.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.userclass.Mode;

public final class DensityOutputProperty extends BaseOutputProperty{

	@Override
	public String getName() {
		return "density";
	}

	@Override
	public String getUnits() {
		return "veh/km";
	}

	@Override
	public Type getType() {
		return Type.DOUBLE;
	}

	@Override
	public Object getOutputValue(MacroscopicLinkSegment linkSegment, Mode mode, int id, double flow,
			double travelTime) {
		return linkSegment.getLinkSegmentType().getMaximumDensityPerLane();
	}

}

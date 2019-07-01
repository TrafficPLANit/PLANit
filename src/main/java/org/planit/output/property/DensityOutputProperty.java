package org.planit.output.property;

import org.planit.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.output.enums.Type;
import org.planit.output.enums.Units;
import org.planit.userclass.Mode;

public final class DensityOutputProperty extends BaseOutputProperty{

	@Override
	public String getName() {
		return "density";
	}

	@Override
	public Units getUnits() {
		return Units.VEH_KM;
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

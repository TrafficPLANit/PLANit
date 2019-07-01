package org.planit.output.property;

import org.planit.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.userclass.Mode;

public final class TravelTimeOutputProperty extends BaseOutputProperty {

	@Override
	public String getName() {
		return "travel time";
	}

	@Override
	public String getUnits() {
		return "h";
	}

	@Override
	public Type getType() {
		return Type.DOUBLE;
	}

	@Override
	public Object getOutputValue(MacroscopicLinkSegment linkSegment, Mode mode, int id, double flow,
			double travelTime) {
		return travelTime;
	}

}

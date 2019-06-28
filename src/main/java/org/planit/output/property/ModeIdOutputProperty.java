package org.planit.output.property;

import org.planit.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.userclass.Mode;

public class ModeIdOutputProperty extends BaseOutputProperty {

	@Override
	public String getName() {
		return "mode id";
	}

	@Override
	public String getUnits() {
		return "none";
	}

	@Override
	public Type getType() {
		return Type.INTEGER;
	}

	@Override
	public Object getOutputValue(MacroscopicLinkSegment linkSegment, Mode mode, int id, double flow,
			double travelTime) {
		return mode.getExternalId();
	}

}

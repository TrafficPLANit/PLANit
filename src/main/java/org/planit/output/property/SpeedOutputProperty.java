package org.planit.output.property;

import org.planit.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.userclass.Mode;

public class SpeedOutputProperty extends BaseOutputProperty {

	@Override
	public String getName() {
		return "speed";
	}

	@Override
	public String getUnits() {
		return "km/h";
	}

	@Override
	public Type getType() {
		return Type.DOUBLE;
	}

	@Override
	public Object getOutputValue(MacroscopicLinkSegment linkSegment, Mode mode, int id, double flow,
			double travelTime) {
		double length = linkSegment.getParentLink().getLength();
		double speed = length / travelTime;
		return speed;
	}

}

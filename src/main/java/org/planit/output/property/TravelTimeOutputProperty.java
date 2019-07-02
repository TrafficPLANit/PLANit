package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public final class TravelTimeOutputProperty extends BaseOutputProperty {

	@Override
	public String getName() {
		return "travel time";
	}

	@Override
	public Units getUnits() {
		return Units.H;
	}

	@Override
	public Type getType() {
		return Type.DOUBLE;
	}

	@Override
	public OutputProperty getOutputProperty() {
		return OutputProperty. TRAVEL_TIME;
	}

}

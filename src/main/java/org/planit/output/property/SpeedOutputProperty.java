package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public final class SpeedOutputProperty extends BaseOutputProperty {

	@Override
	public String getName() {
		return "Speed";
	}

	@Override
	public Units getUnits() {
		return Units.KM_H;
	}

	@Override
	public Type getType() {
		return Type.DOUBLE;
	}

	@Override
	public OutputProperty getOutputProperty() {
		return OutputProperty. SPEED;
	}

	@Override
	public int getColumnPriority() {
		return RESULT_PRORITY;
	}

}

package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public final class MaximumSpeedOutputProperty extends BaseOutputProperty {

	public static final String MAXIMUM_SPEED = "Maximum Speed";
	
	@Override
	public String getName() {
		return MAXIMUM_SPEED;
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
		return OutputProperty.MAXIMUM_SPEED;
	}

	@Override
	public OutputPropertyPriority getColumnPriority() {
		return OutputPropertyPriority.INPUT_PRIORITY;
	}

}

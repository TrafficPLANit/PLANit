package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public class ModeIdOutputProperty extends BaseOutputProperty {

	@Override
	public String getName() {
		return MODE_ID;
	}

	@Override
	public Units getUnits() {
		return Units.NONE;
	}

	@Override
	public Type getType() {
		return Type.INTEGER;
	}

	@Override
	public OutputProperty getOutputProperty() {
		return OutputProperty.MODE_ID;
	}

	@Override
	public int getColumnPriority() {
		return ID_PRIORITY;
	}

}

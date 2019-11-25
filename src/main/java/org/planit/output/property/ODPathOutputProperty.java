package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public final class ODPathOutputProperty extends BaseOutputProperty {

	public static final String OD_PATH = "OD Path";

	@Override
	public String getName() {
		return OD_PATH;
	}

	@Override
	public Units getUnits() {
		return Units.NONE;
	}

	@Override
	public Type getType() {
		return Type.STRING;
	}

	@Override
	public OutputProperty getOutputProperty() {
		return OutputProperty.OD_PATH;
	}

	@Override
	public OutputPropertyPriority getColumnPriority() {
		return OutputPropertyPriority.RESULT_PRIORITY;
	}

}
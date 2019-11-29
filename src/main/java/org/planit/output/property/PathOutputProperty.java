package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public final class PathOutputProperty extends BaseOutputProperty {

	public static final String PATH = "OD Path";

	@Override
	public String getName() {
		return PATH;
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
		return OutputProperty.PATH;
	}

	@Override
	public OutputPropertyPriority getColumnPriority() {
		return OutputPropertyPriority.RESULT_PRIORITY;
	}

}
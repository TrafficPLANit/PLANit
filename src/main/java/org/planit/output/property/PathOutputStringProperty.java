package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public final class PathOutputStringProperty extends BaseOutputProperty {

	public static final String NAME = "Path";

	@Override
	public String getName() {
		return NAME;
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
		return OutputProperty.PATH_STRING;
	}

	@Override
	public OutputPropertyPriority getColumnPriority() {
		return OutputPropertyPriority.RESULT_PRIORITY;
	}

}
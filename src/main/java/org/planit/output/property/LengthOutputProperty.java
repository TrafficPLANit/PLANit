package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public final class LengthOutputProperty extends BaseOutputProperty {

	public static final String LENGTH = "Length";
	
	@Override
	public String getName() {
		return LENGTH;
	}

	@Override
	public Units getUnits() {
		return Units.KM;
	}

	@Override
	public Type getType() {
		return Type.DOUBLE;
	}

	@Override
	public OutputProperty getOutputProperty() {
		return OutputProperty.LENGTH;
	}

	@Override
	public OutputPropertyPriority getColumnPriority() {
		return OutputPropertyPriority.INPUT_PRIORITY;
	}

}
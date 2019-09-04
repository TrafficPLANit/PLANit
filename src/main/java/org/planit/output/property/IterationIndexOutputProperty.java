package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public final class IterationIndexOutputProperty extends BaseOutputProperty {

	public static final String ITERATION_INDEX = "Iteration Index";
	
	@Override
	public String getName() {
		return ITERATION_INDEX;
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
		return OutputProperty.ITERATION_INDEX;
	}

	@Override
	public OutputPropertyPriority getColumnPriority() {
		return OutputPropertyPriority.ID_PRIORITY;
	}

}

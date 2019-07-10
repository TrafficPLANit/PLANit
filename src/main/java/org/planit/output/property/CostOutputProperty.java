package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public final class CostOutputProperty extends BaseOutputProperty {

	@Override
	public String getName() {
		return "Cost";
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
		return OutputProperty.COST;
	}

	@Override
	public int getColumnPriority() {
		return RESULT_PRORITY;
	}

}

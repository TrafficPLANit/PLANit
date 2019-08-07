package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public final class CostOutputProperty extends BaseOutputProperty {

	public static final String COST = "Cost";
	
	@Override
	public String getName() {
		return COST;
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
	protected int getColumnPriority() {
		return RESULT_PRORITY;
	}

}

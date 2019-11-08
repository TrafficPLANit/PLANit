package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public final class ODCostOutputProperty extends BaseOutputProperty {

	public static final String OD_COST = "Cost";
	
	@Override
	public String getName() {
		return OD_COST;
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
		return OutputProperty.OD_COST;
	}

	@Override
	public OutputPropertyPriority getColumnPriority() {
		return OutputPropertyPriority.RESULT_PRIORITY;
	}

}
package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public final class TotalCostToEndNodeOutputProperty extends BaseOutputProperty {

	public static final String TOTAL_COST_TO_END_NODE = "Cost to End Node";

	@Override
	public String getName() {
		return TOTAL_COST_TO_END_NODE;
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
		return OutputProperty.TOTAL_COST_TO_END_NODE;
	}

	@Override
	public OutputPropertyPriority getColumnPriority() {
		return OutputPropertyPriority.RESULT_PRIORITY;
	}

}

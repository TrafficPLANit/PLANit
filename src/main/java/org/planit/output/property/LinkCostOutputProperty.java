package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public final class LinkCostOutputProperty extends BaseOutputProperty {

	public static final String LINK_COST = "Cost";
	
	@Override
	public String getName() {
		return LINK_COST;
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
		return OutputProperty.LINK_COST;
	}

	@Override
	public OutputPropertyPriority getColumnPriority() {
		return OutputPropertyPriority.RESULT_PRIORITY;
	}

}
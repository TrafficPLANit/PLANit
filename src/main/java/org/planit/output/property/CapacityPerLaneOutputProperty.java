package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public final class CapacityPerLaneOutputProperty extends BaseOutputProperty {

	public static final String NAME = "Capacity per Lane";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public Units getUnits() {
		return Units.VEH_H;
	}

	@Override
	public Type getType() {
		return Type.DOUBLE;
	}

	@Override
	public OutputProperty getOutputProperty() {
		return OutputProperty.CAPACITY_PER_LANE;
	}

	@Override
	public OutputPropertyPriority getColumnPriority() {
		return OutputPropertyPriority.INPUT_PRIORITY;
	}

}
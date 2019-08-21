package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public final class CapacityPerLaneOutputProperty extends BaseOutputProperty {

	public static final String CAPACITY_PER_LANE = "Capacity per Lane";

	@Override
	public String getName() {
		return CAPACITY_PER_LANE;
	}

	@Override
	public Units getUnits() {
		return Units.VEH_KM;
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
	protected OutputPropertyPriority getColumnPriority() {
		return OutputPropertyPriority.INPUT_PRIORITY;
	}

}

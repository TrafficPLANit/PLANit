package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public class CapacityPerLaneOutputProperty extends BaseOutputProperty {

	@Override
	public String getName() {
		return "Capacity per Lane";
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
	public int getColumnPriority() {
		return INPUT_PRIORITY;
	}

}

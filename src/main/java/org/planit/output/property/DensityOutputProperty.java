package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public final class DensityOutputProperty extends BaseOutputProperty{

	@Override
	public String getName() {
		return DENSITY;
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
		return OutputProperty.DENSITY;
	}

	@Override
	public int getColumnPriority() {
		return RESULT_PRORITY;
	}

}

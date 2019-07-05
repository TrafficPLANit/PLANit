package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public final class FlowOutputProperty extends BaseOutputProperty {

	@Override
	public String getName() {
		return "Flow";
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
		return OutputProperty. FLOW;
	}

	@Override
	public int getColumnPriority() {
		return RESULT_PRORITY;
	}

}

package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public final class FlowOutputProperty extends BaseOutputProperty {

	public static final String FLOW = "Flow"; 
	
	@Override
	public String getName() {
		return FLOW;
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
	public OutputPropertyPriority getColumnPriority() {
		return OutputPropertyPriority.RESULT_PRIORITY;
	}

}

package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public final class DownstreamNodeIdOutputProperty extends BaseOutputProperty {

	public static final String DOWNSTREAM_NODE_ID = "Downstream Node Id";
	
	@Override
	public String getName() {
		return DOWNSTREAM_NODE_ID;
	}

	@Override
	public Units getUnits() {
		return Units.NONE;
	}

	@Override
	public Type getType() {
		return Type.LONG;
	}

	@Override
	public OutputProperty getOutputProperty() {
		return OutputProperty.DOWNSTREAM_NODE_ID;	
	}

	@Override
	public OutputPropertyPriority getColumnPriority() {
		return OutputPropertyPriority.ID_PRIORITY;
	}

}
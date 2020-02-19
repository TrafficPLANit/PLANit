package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public final class UpstreamNodeIdOutputProperty extends BaseOutputProperty {

	public static final String NAME = "Upstream Node Id";
	
	@Override
	public String getName() {
		return NAME;
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
		return OutputProperty.UPSTREAM_NODE_ID;
	}

	@Override
	public OutputPropertyPriority getColumnPriority() {
		return OutputPropertyPriority.ID_PRIORITY;
	}

}
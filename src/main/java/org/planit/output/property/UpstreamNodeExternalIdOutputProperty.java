package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public final class UpstreamNodeExternalIdOutputProperty extends BaseOutputProperty {

	@Override
	public String getName() {
		return "Upstream Node External Id";
	}

	@Override
	public Units getUnits() {
		return Units.NONE;
	}

	@Override
	public Type getType() {
		return Type.INTEGER;
	}

	@Override
	public OutputProperty getOutputProperty() {
		return OutputProperty.UPSTREAM_NODE_EXTERNAL_ID;
	}

	@Override
	public int getColumnPosition() {
		return 5;
	}

}
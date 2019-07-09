package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public final class DownstreamNodeLocationOutputProperty extends BaseOutputProperty {

	@Override
	public String getName() {
		return "Downstream Node Location";
	}

	@Override
	public Units getUnits() {
		return Units.SRS;
	}

	@Override
	public Type getType() {
		return Type.SRSNAME;
	}

	@Override
	public OutputProperty getOutputProperty() {
		return OutputProperty.DOWNSTREAM_NODE_LOCATION;
	}

	@Override
	public int getColumnPriority() {
		return INPUT_PRIORITY;
	}

}

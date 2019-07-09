package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public final class UpstreamNodeLocationOutputProperty extends BaseOutputProperty {

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "Upstream Node Location";
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
		return OutputProperty.UPSTREAM_NODE_LOCATION;		
	}

	@Override
	public int getColumnPriority() {
		return INPUT_PRIORITY;
	}

}

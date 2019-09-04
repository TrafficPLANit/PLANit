package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public final class DownstreamNodeExternalIdOutputProperty extends BaseOutputProperty {

	public static final String DOWNSTREAM_NODE_EXTERNAL_ID = "Node Downstream External Id";
	
	@Override
	public String getName() {
		return DOWNSTREAM_NODE_EXTERNAL_ID;
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
		return OutputProperty.DOWNSTREAM_NODE_EXTERNAL_ID;	
	}

	@Override
	public OutputPropertyPriority getColumnPriority() {
		return OutputPropertyPriority.ID_PRIORITY;
	}

}
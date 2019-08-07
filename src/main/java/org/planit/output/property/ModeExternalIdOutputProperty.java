package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public final class ModeExternalIdOutputProperty extends BaseOutputProperty {

	public static final String MODE_EXTERNAL_ID = "Mode External Id";
	
	@Override
	public String getName() {
		return MODE_EXTERNAL_ID;
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
		return OutputProperty.MODE_EXTERNAL_ID;
	}

	@Override
	protected int getColumnPriority() {
		return ID_PRIORITY;
	}

}

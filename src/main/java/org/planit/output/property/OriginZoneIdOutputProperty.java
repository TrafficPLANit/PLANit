package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public final class OriginZoneIdOutputProperty extends BaseOutputProperty {

	public static final String ORIGIN_ZONE_ID = "Origin Zone Id";

	@Override
	public String getName() {
		return ORIGIN_ZONE_ID;
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
		return OutputProperty.ORIGIN_ZONE_ID;
	}

	@Override
	public OutputPropertyPriority getColumnPriority() {
		return OutputPropertyPriority.ID_PRIORITY;
	}

}

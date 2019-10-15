package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public final class DestinationZoneIdOutputProperty extends BaseOutputProperty {

	public static final String DESTINATION_ZONE_ID = "Destination Zone Id";

	@Override
	public String getName() {
		return DESTINATION_ZONE_ID;
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
		return OutputProperty.DESTINATION_ZONE_ID;
	}

	@Override
	public OutputPropertyPriority getColumnPriority() {
		return OutputPropertyPriority.ID_PRIORITY;
	}

}

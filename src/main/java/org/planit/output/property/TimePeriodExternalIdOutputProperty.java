package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public final class TimePeriodExternalIdOutputProperty extends BaseOutputProperty {

	public static final String TIME_PERIOD_EXTERNAL_ID = "Time Period External Id";

	@Override
	public String getName() {
		return TIME_PERIOD_EXTERNAL_ID;
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
		return OutputProperty.TIME_PERIOD_EXTERNAL_ID;
	}

	@Override
	public OutputPropertyPriority getColumnPriority() {
		return OutputPropertyPriority.ID_PRIORITY;
	}

}

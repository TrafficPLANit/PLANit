package org.planit.output.property;

import org.planit.exceptions.PlanItException;
import org.planit.output.enums.Type;
import org.planit.output.enums.Units;
import org.planit.time.TimePeriod;

public final class TimePeriodExternalIdOutputProperty extends BaseOutputProperty {

	public static final String NAME = "Time Period External Id";

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
		return OutputProperty.TIME_PERIOD_EXTERNAL_ID;
	}

	@Override
	public OutputPropertyPriority getColumnPriority() {
		return OutputPropertyPriority.ID_PRIORITY;
	}

	/**
	 * Returns the current time period external Id
	 * 
	 * @param timePeriod current time period
	 * @return the current time period external Id
	 * @throws PlanItException thrown if there is an error
	 */
	public static long getTimePeriodExternalId(TimePeriod timePeriod) throws PlanItException {
		return timePeriod.getExternalId();
	}

}
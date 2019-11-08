package org.planit.output.property;

import org.planit.exceptions.PlanItException;
import org.planit.output.enums.Type;
import org.planit.output.enums.Units;
import org.planit.trafficassignment.TrafficAssignment;

public final class RunIdOutputProperty extends BaseOutputProperty {

	public static final String RUN_ID = "Run Id";

	@Override
	public String getName() {
		return RUN_ID;
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
		return OutputProperty.RUN_ID;
	}

	@Override
	public OutputPropertyPriority getColumnPriority() {
		return OutputPropertyPriority.ID_PRIORITY;
	}

	/**
	 * Returns the current run id
	 * 
	 * @param trafficAssignment TrafficAssignment containing data which may be required
	 * @return the current run id
	 * @throws PlanItException thrown if there is an error
	 */
	public static long getRunId(TrafficAssignment trafficAssignment) throws PlanItException {
		return trafficAssignment.getId();
	}

}
package org.planit.output.property;

import org.planit.exceptions.PlanItException;
import org.planit.od.odmatrix.ODMatrixIterator;
import org.planit.output.enums.Type;
import org.planit.output.enums.Units;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.userclass.Mode;

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

	/**
	 * Returns the origin zone Id for the current cell in the OD skim matrix
	 * 
	 * @param object ODMatrixIterator object containing the required data
	 * @param trafficAssignment TrafficAssignment containing data which may be required
	 * @param mode current mode
	 * @param timePeriod current time period
	 * @param timeUnitMultiplier multiplier to convert time durations to hours, minutes or seconds
	 * @return the value of the current property
	 * @throws PlanItException thrown if there is an error
	 */
	@Override
	public Object getValue(Object object, TrafficAssignment trafficAssignment, Mode mode, TimePeriod timePeriod, double timeUnitMultiplier) throws PlanItException {
		if (!(object instanceof ODMatrixIterator)) {
			throw new PlanItException("Tried to read an OD cell from an object which is not an ODMatrixIterator.");
		}
		ODMatrixIterator odMatrixIterator = (ODMatrixIterator) object;
		return odMatrixIterator.getCurrentOrigin().getId();
	}

}

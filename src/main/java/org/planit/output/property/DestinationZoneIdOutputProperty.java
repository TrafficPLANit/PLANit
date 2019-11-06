package org.planit.output.property;

import org.planit.exceptions.PlanItException;
import org.planit.od.odmatrix.ODMatrixIterator;
import org.planit.output.enums.Type;
import org.planit.output.enums.Units;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.userclass.Mode;

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
		return Type.LONG;
	}

	@Override
	public OutputProperty getOutputProperty() {
		return OutputProperty.DESTINATION_ZONE_ID;
	}

	@Override
	public OutputPropertyPriority getColumnPriority() {
		return OutputPropertyPriority.ID_PRIORITY;
	}

	/**
	 * Returns the Id of the destination zone
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
		return odMatrixIterator.getCurrentDestination().getId();
	}

}

package org.planit.output.property;

import org.planit.exceptions.PlanItException;
import org.planit.network.physical.LinkSegment;
import org.planit.output.enums.Type;
import org.planit.output.enums.Units;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.userclass.Mode;

public final class LengthOutputProperty extends BaseOutputProperty {

	public static final String LENGTH = "Length";
	
	@Override
	public String getName() {
		return LENGTH;
	}

	@Override
	public Units getUnits() {
		return Units.KM;
	}

	@Override
	public Type getType() {
		return Type.DOUBLE;
	}

	@Override
	public OutputProperty getOutputProperty() {
		return OutputProperty.LENGTH;
	}

	@Override
	public OutputPropertyPriority getColumnPriority() {
		return OutputPropertyPriority.INPUT_PRIORITY;
	}

	/**
	 * Returns the length of the current link segment
	 * 
	 * @param object LinkSegment object containing the required data
	 * @param trafficAssignment TrafficAssignment containing data which may be required
	 * @param mode current mode
	 * @param timePeriod current time period
	 * @param timeUnitMultiplier multiplier to convert time durations to hours, minutes or seconds
	 * @return the value of the current property
	 * @throws PlanItException thrown if there is an error
	 */
	@Override
	public Object getValue(Object object, TrafficAssignment trafficAssignment, Mode mode, TimePeriod timePeriod, double timeUnitMultiplier) throws PlanItException {
		if (!(object instanceof LinkSegment)) {
			throw new PlanItException("Tried to read the length of an object which is not a LinkSegment.");
		}
		LinkSegment linkSegment = (LinkSegment) object;
		return linkSegment.getParentLink().getLength();
	}

}

package org.planit.output.property;

import org.planit.exceptions.PlanItException;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.output.enums.Type;
import org.planit.output.enums.Units;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.userclass.Mode;

public final class MaximumSpeedOutputProperty extends BaseOutputProperty {

	public static final String MAXIMUM_SPEED = "Maximum Speed";
	
	@Override
	public String getName() {
		return MAXIMUM_SPEED;
	}

	@Override
	public Units getUnits() {
		return Units.KM_H;
	}

	@Override
	public Type getType() {
		return Type.DOUBLE;
	}

	@Override
	public OutputProperty getOutputProperty() {
		return OutputProperty.MAXIMUM_SPEED;
	}

	@Override
	public OutputPropertyPriority getColumnPriority() {
		return OutputPropertyPriority.INPUT_PRIORITY;
	}

	/**
	 * Returns the maximum speed through the current link segment
	 * 
	 * @param object MacroscopicLinkSegment object containing the required data
	 * @param trafficAssignment TrafficAssignment containing data which may be required
	 * @param mode current mode
	 * @param timePeriod current time period
	 * @param timeUnitMultiplier multiplier to convert time durations to hours, minutes or seconds
	 * @return the value of the current property
	 * @throws PlanItException thrown if there is an error
	 */
	@Override
	public Object getValue(Object object, TrafficAssignment trafficAssignment, Mode mode, TimePeriod timePeriod, double timeUnitMultiplier) throws PlanItException {
		if (!(object instanceof MacroscopicLinkSegment)) {
			throw new PlanItException("Tried to read maximum speed of an object which is not a MacroscopicLinkSegment.");
		}
		MacroscopicLinkSegment macroscopicLinkSegment = (MacroscopicLinkSegment) object;
		return macroscopicLinkSegment.getMaximumSpeed(mode.getExternalId());
	}

}

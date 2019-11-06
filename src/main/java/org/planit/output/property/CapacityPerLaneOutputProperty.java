package org.planit.output.property;

import org.planit.exceptions.PlanItException;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.output.enums.Type;
import org.planit.output.enums.Units;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.userclass.Mode;

public final class CapacityPerLaneOutputProperty extends BaseOutputProperty {

	public static final String CAPACITY_PER_LANE = "Capacity per Lane";

	@Override
	public String getName() {
		return CAPACITY_PER_LANE;
	}

	@Override
	public Units getUnits() {
		return Units.VEH_KM;
	}

	@Override
	public Type getType() {
		return Type.DOUBLE;
	}

	@Override
	public OutputProperty getOutputProperty() {
		return OutputProperty.CAPACITY_PER_LANE;
	}

	@Override
	public OutputPropertyPriority getColumnPriority() {
		return OutputPropertyPriority.INPUT_PRIORITY;
	}

	/**
	 * Returns the value of the capacity per lane
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
			throw new PlanItException("Tried to calculate capacity per link across an object which is not a MacroscopicLinkSegment.");
		}
		MacroscopicLinkSegment macroscopicLinkSegment = (MacroscopicLinkSegment) object;
		return macroscopicLinkSegment.getLinkSegmentType().getCapacityPerLane();
	}

}

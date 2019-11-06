package org.planit.output.property;

import org.planit.exceptions.PlanItException;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.output.enums.Type;
import org.planit.output.enums.Units;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.userclass.Mode;

public final class DensityOutputProperty extends BaseOutputProperty{

	public static final String DENSITY = "Density";
	
	@Override
	public String getName() {
		return DENSITY;
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
		return OutputProperty.DENSITY;
	}

	@Override
	public OutputPropertyPriority getColumnPriority() {
		return OutputPropertyPriority.RESULT_PRIORITY;
	}

	/**
	 * Returns the flow density of the current link
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
	public Object getValue(Object object, TrafficAssignment trafficAssignment, Mode mode, TimePeriod timePeriod,
			double timeUnitMultiplier) throws PlanItException {
		if (!(object instanceof MacroscopicLinkSegment)) {
			throw new PlanItException("Tried to density per lane across an object which is not a MacroscopicLinkSegment.");
		}
		MacroscopicLinkSegment macroscopicLinkSegment = (MacroscopicLinkSegment) object;
		return macroscopicLinkSegment.getLinkSegmentType().getMaximumDensityPerLane();
	}

}

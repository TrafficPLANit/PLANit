package org.planit.output.property;

import org.planit.exceptions.PlanItException;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.output.enums.Type;
import org.planit.output.enums.Units;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.userclass.Mode;

public final class LinkSegmentIdOutputProperty extends BaseOutputProperty {

	public static final String LINK_SEGMENT_ID = "Link Segment Id";
	
	@Override
	public String getName() {
		return LINK_SEGMENT_ID;
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
		return OutputProperty.LINK_SEGMENT_ID;
	}

	@Override
	public OutputPropertyPriority getColumnPriority() {
		return OutputPropertyPriority.ID_PRIORITY;
	}

	/**
	 * Returns the Id of the current link segment
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
	public Object getValue(Object object, TrafficAssignment trafficAssignment, Mode mode, TimePeriod timePeriod,
			double timeUnitMultiplier) throws PlanItException {
		if (!(object instanceof MacroscopicLinkSegment)) {
			throw new PlanItException("Tried to read the link Id of an object which is not a MacroscopicLinkSegment.");
		}
		MacroscopicLinkSegment macroscopicLinkSegment = (MacroscopicLinkSegment) object;
		return macroscopicLinkSegment.getId();
	}

}

package org.planit.output.property;

import org.planit.data.TraditionalStaticAssignmentSimulationData;
import org.planit.exceptions.PlanItException;
import org.planit.network.physical.LinkSegment;
import org.planit.output.enums.Type;
import org.planit.output.enums.Units;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.userclass.Mode;

/**
 * Calculate speed output property class
 * 
 * @author gman6028
 *
 */
public final class CalculatedSpeedOutputProperty extends BaseOutputProperty {

	public static final String CALCULATED_SPEED = "Calculated Speed";
	
	@Override
	public String getName() {
		return CALCULATED_SPEED;
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
		return OutputProperty.CALCULATED_SPEED;
	}

	@Override
	public OutputPropertyPriority getColumnPriority() {
		return OutputPropertyPriority.RESULT_PRIORITY;
	}

	/**
	 * Returns the value of the calculated speed
	 * 
	 * @param object LinkSegment containing data which may be required
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
			throw new PlanItException("Tried to calculate speed for an object which is not a link segment.");
		}
		LinkSegment linkSegment = (LinkSegment) object;
		int id = (int) linkSegment.getId();
		TraditionalStaticAssignmentSimulationData simulationData = (TraditionalStaticAssignmentSimulationData) trafficAssignment.getSimulationData();
		double[] modalNetworkSegmentCosts = simulationData.getModalNetworkSegmentCosts(mode);
		double travelTime = modalNetworkSegmentCosts[id];
		double length = linkSegment.getParentLink().getLength();
		return length / travelTime;
	}

}

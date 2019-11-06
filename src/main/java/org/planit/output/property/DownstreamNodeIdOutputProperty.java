package org.planit.output.property;

import org.planit.exceptions.PlanItException;
import org.planit.network.physical.LinkSegment;
import org.planit.network.physical.Node;
import org.planit.output.enums.Type;
import org.planit.output.enums.Units;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.userclass.Mode;

public final class DownstreamNodeIdOutputProperty extends BaseOutputProperty {

	public static final String DOWNSTREAM_NODE_ID = "Downstream Node Id";
	
	@Override
	public String getName() {
		return DOWNSTREAM_NODE_ID;
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
		return OutputProperty.DOWNSTREAM_NODE_ID;	
	}

	@Override
	public OutputPropertyPriority getColumnPriority() {
		return OutputPropertyPriority.ID_PRIORITY;
	}

	/**
	 * Returns the Id of the downstream node
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
		if (!(object instanceof LinkSegment)) {
			throw new PlanItException("Tried to read downstream node Id of an object which is not a link segment.");
		}
		LinkSegment linkSegment = (LinkSegment) object;
		return ((Node) linkSegment.getDownstreamVertex()).getId();
	}

}

package org.planit.output.property;

import org.opengis.geometry.DirectPosition;
import org.planit.exceptions.PlanItException;
import org.planit.network.physical.LinkSegment;
import org.planit.output.enums.Type;
import org.planit.output.enums.Units;
import org.planit.output.formatter.OutputFormatter;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.userclass.Mode;

public final class UpstreamNodeLocationOutputProperty extends BaseOutputProperty {

	public static final String UPSTREAM_NODE_LOCATION = "Upstream Node Location";
	
	@Override
	public String getName() {
		return UPSTREAM_NODE_LOCATION;
	}

	@Override
	public Units getUnits() {
		return Units.SRS;
	}

	@Override
	public Type getType() {
		return Type.SRSNAME;
	}

	@Override
	public OutputProperty getOutputProperty() {
		return OutputProperty.UPSTREAM_NODE_LOCATION;		
	}

	@Override
	public OutputPropertyPriority getColumnPriority() {
		return OutputPropertyPriority.INPUT_PRIORITY;
	}

	/**
	 * Returns the location of the upstream node
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
			throw new PlanItException("Tried to get upstream node location for an object which is not a link segment.");
		}
		LinkSegment linkSegment = (LinkSegment) object;
		DirectPosition centrePoint = linkSegment.getUpstreamVertex().getCentrePointGeometry();
		if (centrePoint == null) {
			return OutputFormatter.NOT_SPECIFIED;
		}
		double[] coordinates = linkSegment.getUpstreamVertex().getCentrePointGeometry().getCoordinate();
		return coordinates[0] + "-" + coordinates[1];
	}

}

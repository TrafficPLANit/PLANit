package org.planit.output.adapter;

import org.planit.odmatrix.ODMatrixIterator;
import org.planit.output.property.BaseOutputProperty;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.TraditionalStaticAssignment;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.userclass.Mode;

public class TraditionalStaticAssignmentODOutputAdapter extends OutputAdapter {

	public TraditionalStaticAssignmentODOutputAdapter(TrafficAssignment trafficAssignment) {
		super(trafficAssignment);
	}

	/**
	 * Returns whether the current assignment has converged
	 * 
	 * @return true if the current assignment has converged, false otherwise
	 */
	@Override
	public boolean isConverged() {
		return ((TraditionalStaticAssignment) trafficAssignment).getSimulationData().isConverged();
	}

	/**
	 * Returns the Origin-Destination property value corresponding to a specified
	 * OutputProperty type
	 * 
	 * @param outputProperty the OutputProperty specifying the property type to be
	 *                       reported
	 * @param matrixIterator the matrix iterator for the OD skim matrix
	 * @param mode           the current mode
	 * @param timePeriod     the current time period
	 * @return the output value for the specified property
	 */
	public Object getOdPropertyValue(BaseOutputProperty outputProperty, ODMatrixIterator odMatrixIterator, Mode mode,
			TimePeriod timePeriod) {
		switch (outputProperty.getOutputProperty()) {
		case ORIGIN_ZONE_ID:
			return odMatrixIterator.getCurrentOriginId() + 1;
		case DESTINATION_ZONE_ID:
			return odMatrixIterator.getCurrentDestinationId() + 1;
		case MODE_ID:
			return mode.getId();
		case MODE_EXTERNAL_ID:
			return mode.getExternalId();
		case RUN_ID:
			return getTrafficAssignmentId();
		case ITERATION_INDEX:
			return getSimulationData().getIterationIndex();
		case COST:
			return odMatrixIterator.getCurrentValue();
		case TIME_PERIOD_ID:
			return timePeriod.getId();
		default:
			return null;
		}
	}

}
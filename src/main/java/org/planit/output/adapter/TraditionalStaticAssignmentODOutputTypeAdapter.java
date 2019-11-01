package org.planit.output.adapter;

import java.util.Set;

import org.planit.data.TraditionalStaticAssignmentSimulationData;
import org.planit.od.odmatrix.ODMatrixIterator;
import org.planit.od.odmatrix.skim.ODSkimMatrix;
import org.planit.output.enums.ODSkimOutputType;
import org.planit.output.property.OutputProperty;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.userclass.Mode;

public class TraditionalStaticAssignmentODOutputTypeAdapter extends OutputTypeAdapter {

	public TraditionalStaticAssignmentODOutputTypeAdapter(TrafficAssignment trafficAssignment) {
		super(trafficAssignment);
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
	 * @param timeUnitMultiplier multiplier to convert time into hours, minutes or seconds
	 * @return the output value for the specified property
	 */
	public Object getOdPropertyValue(OutputProperty outputProperty, ODMatrixIterator odMatrixIterator, Mode mode,	TimePeriod timePeriod, double timeUnitMultiplier) {
		switch (outputProperty) {
		case ORIGIN_ZONE_ID:
			return odMatrixIterator.getCurrentOrigin().getId();
		case ORIGIN_ZONE_EXTERNAL_ID:
			return odMatrixIterator.getCurrentOrigin().getExternalId();
		case DESTINATION_ZONE_ID:
			return odMatrixIterator.getCurrentDestination().getId();
		case DESTINATION_ZONE_EXTERNAL_ID:
			return odMatrixIterator.getCurrentDestination().getExternalId();
		case MODE_ID:
			return mode.getId();
		case MODE_EXTERNAL_ID:
			return mode.getExternalId();
		case RUN_ID:
			return trafficAssignment.getId();
		case ITERATION_INDEX:
			return trafficAssignment.getSimulationData().getIterationIndex();
		case COST:
			return odMatrixIterator.getCurrentValue() * timeUnitMultiplier;
		case TIME_PERIOD_ID:
			return timePeriod.getId();
		case TIME_PERIOD_EXTERNAL_ID:
			return timePeriod.getExternalId();
		default:
			return null;
		}
	}

	/**
	 * Returns a Set of OD skim output types which have been activated
	 * 
	 * @return Set of OD skim output types which have been activated
	 */
    public Set<ODSkimOutputType> getActiveSkimOutputTypes() {
		TraditionalStaticAssignmentSimulationData traditionalStaticAssignmentSimulationData = (TraditionalStaticAssignmentSimulationData) trafficAssignment.getSimulationData();
		return traditionalStaticAssignmentSimulationData.getActiveSkimOutputTypes();
    }
    
    /**
     * Retrieve an OD skim matrix for a specified OD skim output type and mode
     * 
     * @param odSkimOutputType the specified OD skim output type
     * @param mode the specified mode
     * @return the OD skim matrix
     */
    public  ODSkimMatrix getODSkimMatrix(ODSkimOutputType odSkimOutputType, Mode mode) {
		TraditionalStaticAssignmentSimulationData traditionalStaticAssignmentSimulationData = (TraditionalStaticAssignmentSimulationData) trafficAssignment.getSimulationData();
		return traditionalStaticAssignmentSimulationData.getODSkimMatrix(odSkimOutputType, mode);
    }
      
}
package org.planit.output.adapter;

import org.planit.data.TraditionalStaticAssignmentSimulationData;
import org.planit.exceptions.PlanItException;
import org.planit.od.odmatrix.ODMatrixIterator;
import org.planit.od.odmatrix.skim.ODSkimMatrix;
import org.planit.output.enums.ODSkimOutputType;
import org.planit.output.enums.OutputType;
import org.planit.output.property.BaseOutputProperty;
import org.planit.output.property.IterationIndexOutputProperty;
import org.planit.output.property.ModeExternalIdOutputProperty;
import org.planit.output.property.ModeIdOutputProperty;
import org.planit.output.property.OutputProperty;
import org.planit.output.property.RunIdOutputProperty;
import org.planit.output.property.TimePeriodExternalIdOutputProperty;
import org.planit.output.property.TimePeriodIdOutputProperty;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.userclass.Mode;

public class TraditionalStaticAssignmentODOutputTypeAdapter extends OutputTypeAdapterImpl implements ODOutputTypeAdapter {

	/**
	 * Returns the external Id of the destination zone for the current cell in the OD skim matrix
	 * 
	 * @param odMatrixIterator ODMatrixIterator object containing the required data
	 * @return the external Id of the destination zone for the current cell in the OD skim matrix
	 * @throws PlanItException thrown if there is an error
	 */
	private long getDestinationZoneExternalId(ODMatrixIterator odMatrixIterator) throws PlanItException {
		return odMatrixIterator.getCurrentDestination().getExternalId();
	}

	/**
	 * Returns the Id of the destination zone for the current cell in the OD skim matrix
	 * 
	 * @param odMatrixIterator ODMatrixIterator object containing the required data
	 * @return the Id of the destination zone for the current cell in the OD skim matrix
	 * @throws PlanItException thrown if there is an error
	 */
	private long getDestinationZoneId(ODMatrixIterator odMatrixIterator) throws PlanItException {
		return odMatrixIterator.getCurrentDestination().getId();
	}

	/**
	 * Returns the origin zone external Id for the current cell in the OD skim matrix
	 * 
	 * @param odMatrixIterator ODMatrixIterator object containing the required data
	 * @return the origin zone external Id for the current cell in the OD skim matrix
	 * @throws PlanItException thrown if there is an error
	 */
	private long getOriginZoneExternalId(ODMatrixIterator odMatrixIterator) throws PlanItException {
		return odMatrixIterator.getCurrentOrigin().getExternalId();
	}

	/**
	 * Returns the origin zone Id for the current cell in the OD skim matrix
	 * 
	 * @param odMatrixIterator ODMatrixIterator object containing the required data
	 * @return the origin zone Id for the current cell in the OD skim matrix
	 * @throws PlanItException thrown if there is an error
	 */
	private long getOriginZoneId(ODMatrixIterator odMatrixIterator) throws PlanItException {
		return odMatrixIterator.getCurrentOrigin().getId();
	}

	/**
	 * Returns the OD travel cost for the current cell in the OD skim matrix
	 * 
	 * @param odMatrixIterator ODMatrixIterator object containing the required data
	 * @param timeUnitMultiplier multiplier to convert time durations to hours, minutes or seconds
	 * @return the OD travel cost for the current cell in the OD skim matrix
	 * @throws PlanItException thrown if there is an error
	 */
	private double getODCost(ODMatrixIterator odMatrixIterator, double timeUnitMultiplier) throws PlanItException {
		return odMatrixIterator.getCurrentValue() * timeUnitMultiplier;
	}

	/**
	 * Constructor
	 * 
	 * @param outputType the output type for the current persistence
	 * @param trafficAssignment the traffic assignment used to provide the data
	 */
	public TraditionalStaticAssignmentODOutputTypeAdapter(OutputType outputType, TrafficAssignment trafficAssignment) {
		super(outputType, trafficAssignment);
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

    /**
     * Returns the specified output property values for the current cell in the OD Matrix Iterator
     * 
     * @param outputProperty the specified output property
     * @param odMatrixIterator the iterator through the current OD Matrix
     * @param mode the current mode
     * @param timePeriod the current time period
     * @param timeUnitMultiplier the multiplier for time units
     * @return the value of the specified property (or an Exception if an error has occurred)
     */
	@Override
	public Object getODOutputPropertyValue(OutputProperty outputProperty, ODMatrixIterator odMatrixIterator, Mode mode, TimePeriod timePeriod, double timeUnitMultiplier) {
		try {
			switch (outputProperty) {
			case DESTINATION_ZONE_EXTERNAL_ID:
				return getDestinationZoneExternalId(odMatrixIterator);
			case DESTINATION_ZONE_ID:
				return getDestinationZoneId(odMatrixIterator);
			case ITERATION_INDEX:
				return IterationIndexOutputProperty.getIterationIndex(trafficAssignment);
			case MODE_EXTERNAL_ID:
				return ModeExternalIdOutputProperty.getModeExternalId(mode);
			case MODE_ID:
				return ModeIdOutputProperty.getModeId(mode);
			case OD_COST:
				return getODCost(odMatrixIterator, timeUnitMultiplier);
			case ORIGIN_ZONE_EXTERNAL_ID:
				return getOriginZoneExternalId(odMatrixIterator);
			case ORIGIN_ZONE_ID:
				return getOriginZoneId(odMatrixIterator);
			case RUN_ID:
				return RunIdOutputProperty.getRunId(trafficAssignment);
			case TIME_PERIOD_EXTERNAL_ID:
				return TimePeriodExternalIdOutputProperty.getTimePeriodExternalId(timePeriod);
			case TIME_PERIOD_ID:
				return TimePeriodIdOutputProperty.getTimePeriodId(timePeriod);
			default:
				return new PlanItException("Tried to find link property of " + BaseOutputProperty.convertToBaseOutputProperty(outputProperty).getName() + " which is not applicable for OD matrix.");		
			}
		} catch (PlanItException e) {
			return e;
		}
	}
      
}
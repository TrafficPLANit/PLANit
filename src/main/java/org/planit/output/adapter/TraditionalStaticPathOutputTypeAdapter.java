package org.planit.output.adapter;

import java.util.List;
import java.util.function.ToLongFunction;

import org.planit.data.TraditionalStaticAssignmentSimulationData;
import org.planit.exceptions.PlanItException;
import org.planit.network.physical.LinkSegment;
import org.planit.network.physical.Node;
import org.planit.od.odpath.ODPathMatrix;
import org.planit.od.odpath.ODPathIterator;
import org.planit.output.enums.OutputType;
import org.planit.output.enums.PathOutputType;
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

/**
 *Adapter providing access to the data of the TraditionalStaticAssignment class
 * relevant for OD path outputs without exposing the internals of the traffic
 * assignment class itself 
 * 
 * @author gman6028
 *
 */
public class TraditionalStaticPathOutputTypeAdapter extends OutputTypeAdapterImpl implements PathOutputTypeAdapter {

	/**
	 * Returns the external Id of the destination zone for the current cell in the OD path matrix
	 * 
	 * @param odPathIterator ODPathIterator object containing the required data
	 * @return the external Id of the destination zone for the current cell in the OD path matrix
	 * @throws PlanItException thrown if there is an error
	 */
	private long getDestinationZoneExternalId(ODPathIterator odPathIterator) throws PlanItException {
		return odPathIterator.getCurrentDestination().getExternalId();
	}

	/**
	 * Returns the Id of the destination zone for the current cell in the OD path matrix
	 * 
	 * @param odPathIterator ODPathIterator object containing the required data
	 * @return the Id of the destination zone for the current cell in the OD path matrix
	 * @throws PlanItException thrown if there is an error
	 */
	private long getDestinationZoneId(ODPathIterator odPathIterator) throws PlanItException {
		return odPathIterator.getCurrentDestination().getId();
	}

	/**
	 * Returns the origin zone external Id for the current cell in the OD path matrix
	 * 
	 * @param odPathIterator ODPathIterator object containing the required data
	 * @return the origin zone external Id for the current cell in the OD path matrix
	 * @throws PlanItException thrown if there is an error
	 */
	private long getOriginZoneExternalId(ODPathIterator odPathIterator) throws PlanItException {
		return odPathIterator.getCurrentOrigin().getExternalId();
	}

	/**
	 * Returns the origin zone Id for the current cell in the OD path matrix
	 * 
	 * @param odPathIterator ODPathIterator object containing the required data
	 * @return the origin zone Id for the current cell in the OD path matrix
	 * @throws PlanItException thrown if there is an error
	 */
	private long getOriginZoneId(ODPathIterator odPathIterator) throws PlanItException {
		return odPathIterator.getCurrentOrigin().getId();
	}
	
	/**
	 * Returns the path as a String of comma-separated Id or  external Id values
	 * 
	 * @param path list of objects containing the path
	 * @param idGetter lambda function to get the required Id value
	 * @return the path as a String of comma-separated Id or external Id values
	 */
	private String getPath(List<Object> path, ToLongFunction<Object> idGetter) {
		StringBuilder builder = new StringBuilder("");
		for (Object object : path) {
			builder.append(idGetter.applyAsLong(object));
			builder.append(",");
		}
		builder.deleteCharAt(builder.length()-1);
		return new String(builder);
	}
	
	/**
	 * Returns the path as a String of comma-separated Id values
	 * 
	 * @param odPathIterator ODPathIterator object containing the required data
	 * @param pathOutputType the type of objects being used in the path
	 * @return the OD path as a String of comma-separated node external Id values
	 */
	private String getPath(ODPathIterator odPathIterator, PathOutputType pathOutputType) {
		List<Object> path =  odPathIterator.getCurrentValue();
		if (path != null) {
			switch (pathOutputType) {
			case LINK_SEGMENT_EXTERNAL_ID:
			    return getPath(path, object-> {return ((LinkSegment) object).getExternalId();});
			case LINK_SEGMENT_ID:
			    return getPath(path, object-> {return ((LinkSegment) object).getId();});
			case NODE_EXTERNAL_ID:
			    return getPath(path, object-> {return ((Node) object).getExternalId();});
			case NODE_ID:
			    return getPath(path, object-> {return ((Node) object).getId();});
			}
		}
		return "";
	}
	
/**
 * Constructor
 * 
 * @param outputType the output type for the current persistence
 * @param trafficAssignment the traffic assignment used to provide the data
 */
	public TraditionalStaticPathOutputTypeAdapter(OutputType outputType, TrafficAssignment trafficAssignment) {
		super(outputType, trafficAssignment);
	}
	
    /**
     * Retrieve an OD path matrix object for a specified mode
     * 
     * @param mode the specified mode
     * @return the OD path object
     */
	@Override
    public ODPathMatrix getODPathMatrix(Mode mode) {
		TraditionalStaticAssignmentSimulationData traditionalStaticAssignmentSimulationData = (TraditionalStaticAssignmentSimulationData) trafficAssignment.getSimulationData();
		return traditionalStaticAssignmentSimulationData.getODPathMatrix(mode);
    }

    /**
     * Returns the specified output property values for the current cell in the ODPathIterator
     * 
     * @param outputProperty the specified output property
     * @param odPathIterator the iterator through the current ODPath object
     * @param mode the current mode
     * @param timePeriod the current time period
     * @param pathOutputType the type of objects in the path list
     * @return the value of the specified property (or an Exception if an error has occurred)
     */
	@Override
	public Object getPathOutputPropertyValue(OutputProperty outputProperty, ODPathIterator odPathIterator, Mode mode, TimePeriod timePeriod, PathOutputType pathOutputType) {
		try {
			switch (outputProperty) {
			case DESTINATION_ZONE_EXTERNAL_ID:
				return getDestinationZoneExternalId(odPathIterator);
			case DESTINATION_ZONE_ID:
				return getDestinationZoneId(odPathIterator);
			case ITERATION_INDEX:
				return IterationIndexOutputProperty.getIterationIndex(trafficAssignment);
			case MODE_EXTERNAL_ID:
				return ModeExternalIdOutputProperty.getModeExternalId(mode);
			case MODE_ID:
				return ModeIdOutputProperty.getModeId(mode);
			case PATH:
				return getPath(odPathIterator, pathOutputType);
			case ORIGIN_ZONE_EXTERNAL_ID:
				return getOriginZoneExternalId(odPathIterator);
			case ORIGIN_ZONE_ID:
				return getOriginZoneId(odPathIterator);
			case RUN_ID:
				return RunIdOutputProperty.getRunId(trafficAssignment);
			case TIME_PERIOD_EXTERNAL_ID:
				return TimePeriodExternalIdOutputProperty.getTimePeriodExternalId(timePeriod);
			case TIME_PERIOD_ID:
				return TimePeriodIdOutputProperty.getTimePeriodId(timePeriod);
			default:
				return new PlanItException("Tried to find link property of " + BaseOutputProperty.convertToBaseOutputProperty(outputProperty).getName() + " which is not applicable for OD path.");		
			}
		} catch (PlanItException e) {
			return e;
		}
	}

}
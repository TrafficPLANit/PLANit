package org.planit.output.adapter;

import org.planit.exceptions.PlanItException;
import org.planit.od.odpath.ODPathIterator;
import org.planit.output.enums.OutputType;
import org.planit.output.enums.PathIdType;
import org.planit.output.property.BaseOutputProperty;
import org.planit.output.property.OutputProperty;
import org.planit.route.Route;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.utils.network.physical.Mode;

/**
 * Top-level abstract class which defines the common methods required by Path output type adapters
 * 
 * @author gman6028
 *
 */
public abstract class PathOutputTypeAdapterImpl extends OutputTypeAdapterImpl implements PathOutputTypeAdapter {

	/**
	 * Returns the external Id of the destination zone for the current cell in the OD path matrix
	 * 
	 * @param odPathIterator ODPathIterator object containing the required data
	 * @return the external Id of the destination zone for the current cell in the OD path matrix
	 * @throws PlanItException thrown if there is an error
	 */
	protected long getDestinationZoneExternalId(ODPathIterator odPathIterator) throws PlanItException {
		return odPathIterator.getCurrentDestination().getExternalId();
	}

	/**
	 * Returns the Id of the destination zone for the current cell in the OD path matrix
	 * 
	 * @param odPathIterator ODPathIterator object containing the required data
	 * @return the Id of the destination zone for the current cell in the OD path matrix
	 * @throws PlanItException thrown if there is an error
	 */
	protected long getDestinationZoneId(ODPathIterator odPathIterator) throws PlanItException {
		return odPathIterator.getCurrentDestination().getId();
	}

	/**
	 * Returns the origin zone external Id for the current cell in the OD path matrix
	 * 
	 * @param odPathIterator ODPathIterator object containing the required data
	 * @return the origin zone external Id for the current cell in the OD path matrix
	 * @throws PlanItException thrown if there is an error
	 */
	protected long getOriginZoneExternalId(ODPathIterator odPathIterator) throws PlanItException {
		return odPathIterator.getCurrentOrigin().getExternalId();
	}

	/**
	 * Returns the origin zone Id for the current cell in the OD path matrix
	 * 
	 * @param odPathIterator ODPathIterator object containing the required data
	 * @return the origin zone Id for the current cell in the OD path matrix
	 * @throws PlanItException thrown if there is an error
	 */
	protected long getOriginZoneId(ODPathIterator odPathIterator) throws PlanItException {
		return odPathIterator.getCurrentOrigin().getId();
	}
	
	/**
	 * Returns the path as a String of comma-separated Id values
	 * 
	 * @param odPathIterator ODPathIterator object containing the required data
	 * @param pathOutputType the type of objects being used in the path
	 * @return the OD path as a String of comma-separated node external Id values
	 */
	protected String getPath(ODPathIterator odPathIterator, PathIdType pathOutputType) {
		Route path =  odPathIterator.getCurrentValue();
		if (path != null) {
			return path.toString(pathOutputType);
		} 
		return "";
	}
	
	/**
	 * Constructor
	 * 
	 * @param outputType the output type for the current persistence
	 * @param trafficAssignment the traffic assignment used to provide the data
	 */
	public PathOutputTypeAdapterImpl(OutputType outputType, TrafficAssignment trafficAssignment) {
		super(outputType, trafficAssignment);
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
	public Object getPathOutputPropertyValue(OutputProperty outputProperty, ODPathIterator odPathIterator, Mode mode, TimePeriod timePeriod, PathIdType pathOutputType) {
		try {
			Object obj = getCommonPropertyValue(outputProperty, mode, timePeriod);
			if (obj != null) {
				return obj;
			}
			switch (outputProperty) {
			case DESTINATION_ZONE_EXTERNAL_ID:
				return getDestinationZoneExternalId(odPathIterator);
			case DESTINATION_ZONE_ID:
				return getDestinationZoneId(odPathIterator);
			case PATH:
				return getPath(odPathIterator, pathOutputType);
			case ORIGIN_ZONE_EXTERNAL_ID:
				return getOriginZoneExternalId(odPathIterator);
			case ORIGIN_ZONE_ID:
				return getOriginZoneId(odPathIterator);
			default:
				return new PlanItException("Tried to find link property of " + BaseOutputProperty.convertToBaseOutputProperty(outputProperty).getName() + " which is not applicable for OD path.");		
			}
		} catch (PlanItException e) {
			return e;
		}
	}
}
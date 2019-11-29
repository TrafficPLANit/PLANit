package org.planit.output.configuration;

import java.util.Arrays;
import java.util.List;

import org.planit.exceptions.PlanItException;
import org.planit.logging.PlanItLogger;
import org.planit.output.enums.OutputType;
import org.planit.output.property.OutputProperty;
import org.planit.trafficassignment.TrafficAssignment;

/**
 * The configuration for the OD path output type.
 * 
 * The following OutputProperty values are included by default:
 * 
 * RUN_ID
 * TIME_PERIOD_EXTERNAL_ID 
 * MODE_EXTERNAL_ID 
 * ORIGIN_ZONE_EXTERNAL_ID 
 * DESTINATION_ZONE_EXTERNAL_ID 
 * PATH
 * 
 * 
 * @author markr
 *
 */
public class PathOutputTypeConfiguration extends OutputTypeConfiguration {

	public static final int ORIGIN_DESTINATION_ID = 1;
	public static final int ORIGIN_DESTINATION_EXTERNAL_ID = 2;
	public static final int ORIGIN_DESTINATION_NOT_IDENTIFIED = 3;
	
	/**
	 * Determine how an origin-destination cell is being identified in the output formatter
	 * 
	 * @param outputKeyProperties array of output key property types
	 * @return the value of the identification type determined
	 */
	private int findIdentificationMethod(OutputProperty [] outputKeyProperties) {
		List<OutputProperty> outputKeyPropertyList = Arrays.asList(outputKeyProperties);
		if (outputKeyPropertyList.contains(OutputProperty.ORIGIN_ZONE_ID) && outputKeyPropertyList.contains(OutputProperty.DESTINATION_ZONE_ID)) {
			return ORIGIN_DESTINATION_ID;
		}
		if (outputKeyPropertyList.contains(OutputProperty.ORIGIN_ZONE_EXTERNAL_ID) && outputKeyPropertyList.contains(OutputProperty.DESTINATION_ZONE_EXTERNAL_ID)) {
			return ORIGIN_DESTINATION_EXTERNAL_ID;
		}
		return ORIGIN_DESTINATION_NOT_IDENTIFIED;
	}

	/**
	 * Constructor
	 * 
	 * Define the default output properties here.
	 * 
	 * @param trafficAssignment TrafficAssignment object whose results are to be reported
	 * @throws PlanItException thrown if there is an error adding the default properties
	 */
	public PathOutputTypeConfiguration(TrafficAssignment trafficAssignment) throws PlanItException {
		super(trafficAssignment, OutputType.PATH);
		// add default output properties
		addProperty(OutputProperty.RUN_ID);
		addProperty(OutputProperty.TIME_PERIOD_EXTERNAL_ID);
		addProperty(OutputProperty.MODE_EXTERNAL_ID);
		addProperty(OutputProperty.ORIGIN_ZONE_EXTERNAL_ID);
		addProperty(OutputProperty.DESTINATION_ZONE_EXTERNAL_ID);
		addProperty(OutputProperty.PATH);
	}
	
	/**
	 * Validate whether the specified list of keys is valid, and if it is return only the keys which will be used
	 * 
	 * @param outputKeyProperties array of output key property types
	 * @return array of keys to be used (null if the list is not valid)
	 */
	@Override
	public OutputProperty[] validateAndFilterKeyProperties(OutputProperty [] outputKeyProperties) {
		OutputProperty[] outputKeyPropertiesArray = null;
		boolean valid = false;
		switch (findIdentificationMethod(outputKeyProperties)) {
		case OriginDestinationOutputTypeConfiguration.ORIGIN_DESTINATION_ID:
			outputKeyPropertiesArray = new OutputProperty[2];
			outputKeyPropertiesArray[0] = OutputProperty.ORIGIN_ZONE_ID;
			outputKeyPropertiesArray[1] = OutputProperty.DESTINATION_ZONE_ID;
			valid = true;
			break;
		case OriginDestinationOutputTypeConfiguration.ORIGIN_DESTINATION_EXTERNAL_ID:
			outputKeyPropertiesArray = new OutputProperty[2];
			outputKeyPropertiesArray[0] = OutputProperty.ORIGIN_ZONE_EXTERNAL_ID;
			outputKeyPropertiesArray[1] = OutputProperty.DESTINATION_ZONE_EXTERNAL_ID;
			valid = true;
			break;
		default:
			PlanItLogger.severe("Configured keys cannot identify origin-destination cell in the skim matrix.");
		}
		if (valid) {
			return outputKeyPropertiesArray;
		}
		return null;
	}
	
}
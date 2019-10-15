package org.planit.output.configuration;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.planit.exceptions.PlanItException;
import org.planit.output.OutputType;
import org.planit.output.adapter.OutputAdapter;
import org.planit.output.property.OutputProperty;

/**
 * The configuration for the origin-destination output type.
 * 
 * The following OutputProperty values are included by default:
 * 
 * RUN_ID
 * ORIGIN_ZONE_ID 
 * DESTINATION_ZONE_EXTERNAL_ID 
 * MODE_EXTERNAL_ID 
 * TIME_PERIOD_ID 
 * COST
 * 
 * 
 * @author markr
 *
 */
public class OriginDestinationOutputTypeConfiguration extends OutputTypeConfiguration {
	
//TODO - At present origins and destinations only use ID, which is the row and column of the OD matrix
	public static final int ORIGIN_DESTINATION_ID = 1;
	public static final int ORIGIN_DESTINATION_NOT_IDENTIFIED = 2;

	public OriginDestinationOutputTypeConfiguration(OutputAdapter outputAdapter) throws PlanItException {
		super(outputAdapter, OutputType.OD);
		// add default output properties
		addProperty(OutputProperty.RUN_ID);
		addProperty(OutputProperty.TIME_PERIOD_ID);
		addProperty(OutputProperty.MODE_EXTERNAL_ID);
		addProperty(OutputProperty.ORIGIN_ZONE_ID);
		addProperty(OutputProperty.DESTINATION_ZONE_ID);
		addProperty(OutputProperty.COST);
	}

	@Override
	public int findIdentificationMethod(Map<OutputType, OutputProperty[]> outputKeyProperties) {
		OutputProperty [] outputKeyPropertiesArray = outputKeyProperties.get(OutputType.OD);
		List<OutputProperty> outputKeyPropertyList = Arrays.asList(outputKeyPropertiesArray);
		if (outputKeyPropertyList.contains(OutputProperty.ORIGIN_ZONE_ID) && outputKeyPropertyList.contains(OutputProperty.DESTINATION_ZONE_ID)) {
			return ORIGIN_DESTINATION_ID;
		}
		return ORIGIN_DESTINATION_NOT_IDENTIFIED;
	}

}

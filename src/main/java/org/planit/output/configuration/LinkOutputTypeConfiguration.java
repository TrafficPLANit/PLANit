package org.planit.output.configuration;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.planit.exceptions.PlanItException;
import org.planit.output.OutputType;
import org.planit.output.adapter.OutputAdapter;
import org.planit.output.property.OutputProperty;

/**
 * The configuration for the link output type.
 * 
 * The following OutputProperty values are included by default:
 * 
 * RUN_ID
 * LINK_SEGMENT_ID 
 * LINK_SEGMENT_EXTERNAL_ID 
 * MODE_EXTERNAL_ID 
 * FLOW 
 * SPEED 
 * COST
 * 
 * 
 * @author markr
 *
 */
public class LinkOutputTypeConfiguration extends OutputTypeConfiguration {
	
	public static final int LINK_SEGMENT_IDENTIFICATION_BY_NODE_ID = 1;
	public static final int LINK_SEGMENT_IDENTIFICATION_BY_ID = 2;
	public static final int LINK_SEGMENT_IDENTIFICATION_BY_EXTERNAL_ID = 3;
	public static final int LINK_SEGMENT_NOT_IDENTIFIED = 4;

	/**
	 * Constructor
	 * 
	 * Define the default output properties here.
	 * 
	 * @param outputAdapter OutputAdapter to access data for output persistence
	 * @throws PlanItException thrown if there is an error adding the default
	 *                         properties
	 */
	public LinkOutputTypeConfiguration(OutputAdapter outputAdapter) throws PlanItException {
		super(outputAdapter, OutputType.LINK);
		addProperty(OutputProperty.TIME_PERIOD_ID);
		addProperty(OutputProperty.MODE_EXTERNAL_ID);
		addProperty(OutputProperty.UPSTREAM_NODE_EXTERNAL_ID);
		addProperty(OutputProperty.DOWNSTREAM_NODE_EXTERNAL_ID);
		addProperty(OutputProperty.FLOW);
		addProperty(OutputProperty.CAPACITY_PER_LANE);
		addProperty(OutputProperty.NUMBER_OF_LANES);
		addProperty(OutputProperty.LENGTH);
		addProperty(OutputProperty.CALCULATED_SPEED);
		addProperty(OutputProperty.COST);
	}

	/**
	 * Determine how a link is being identified in the output formatter
	 * 
	 * @param outputKeyProperties Map of arrays of keys used to identify the link
	 * @return the identification method
	 */
	@Override
	public int findIdentificationMethod(Map<OutputType, OutputProperty[]> outputKeyProperties) {
		OutputProperty [] outputKeyPropertiesArray = outputKeyProperties.get(OutputType.LINK);
		List<OutputProperty> outputKeyPropertyList = Arrays.asList(outputKeyPropertiesArray);
		if (outputKeyPropertyList.contains(OutputProperty.DOWNSTREAM_NODE_EXTERNAL_ID) && outputKeyPropertyList.contains(OutputProperty.UPSTREAM_NODE_EXTERNAL_ID)) {
			return LINK_SEGMENT_IDENTIFICATION_BY_NODE_ID;
		}
		if (outputKeyPropertyList.contains(OutputProperty.LINK_SEGMENT_ID)) {
			return LINK_SEGMENT_IDENTIFICATION_BY_ID;
		}
		if (outputKeyPropertyList.contains(OutputProperty.LINK_SEGMENT_EXTERNAL_ID)) {
			return LINK_SEGMENT_IDENTIFICATION_BY_EXTERNAL_ID;
		}
		return LINK_SEGMENT_NOT_IDENTIFIED;
	}
	
}

package org.planit.output.configuration;

import java.util.Arrays;
import java.util.List;

import org.planit.exceptions.PlanItException;
import org.planit.logging.PlanItLogger;
import org.planit.output.enums.OutputType;
import org.planit.output.property.OutputProperty;
import org.planit.trafficassignment.TrafficAssignment;

/**
 * The configuration for the link output type.
 * 
 * The following OutputProperty values are included by default:
 * 
 * MODE_EXTERNAL_ID 
 * UPSTREAM_NODE_EXTERNAL_ID
 * DOWNSTREAM_NODE_EXTERNAL_ID
 * FLOW 
 * CAPACITY_PER_LANE
 * NUMBER_OF_LANES
 * LENGTH
 * CALCULATED_SPEED 
 * COST
 * DENSITY
 * LINK_SEGMENT_ID
 * UPSTREAM_NODE_ID
 * UPSTREAM_NODE_LOCATION
 * DOWNSTREAM_NODE_ID
 * DOWNSTREAM_NODE_LOCATION
 * CAPACITY_PER_LANE
 * LINK_COST
 * MODE_ID
 * MAXIMUM_SPEED
 * TIME_PERIOD_EXTERNAL_ID
 * TIME_PERIOD_ID
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
	 * Determine how a link is being identified in the output formatter
	 * 
	 * @param outputKeyProperties array of output key property types
	 * @return the value of the identification type determined
	 */
	private int findIdentificationMethod(OutputProperty [] outputKeyProperties) {
		List<OutputProperty> outputKeyPropertyList = Arrays.asList(outputKeyProperties);
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
	
	/**
	 * Constructor
	 * 
	 * Define the default output properties here.
	 * 
	 * @param trafficAssignment TrafficAssignment object whose results are to be reported
	 * @throws PlanItException thrown if there is an error adding the default properties
	 */
	public LinkOutputTypeConfiguration(TrafficAssignment trafficAssignment) throws PlanItException {
		super(trafficAssignment, OutputType.LINK);
		addProperty(OutputProperty.DENSITY);
		addProperty(OutputProperty.LINK_SEGMENT_ID);
		addProperty(OutputProperty.MODE_EXTERNAL_ID);
		addProperty(OutputProperty.UPSTREAM_NODE_EXTERNAL_ID);
		addProperty(OutputProperty.UPSTREAM_NODE_ID);
		addProperty(OutputProperty.UPSTREAM_NODE_LOCATION);
		addProperty(OutputProperty.DOWNSTREAM_NODE_EXTERNAL_ID);
		addProperty(OutputProperty.DOWNSTREAM_NODE_ID);
		addProperty(OutputProperty.DOWNSTREAM_NODE_LOCATION);
		addProperty(OutputProperty.FLOW);
		addProperty(OutputProperty.CAPACITY_PER_LANE);
		addProperty(OutputProperty.NUMBER_OF_LANES);
		addProperty(OutputProperty.LENGTH);
		addProperty(OutputProperty.CALCULATED_SPEED);
		addProperty(OutputProperty.LINK_COST);
		addProperty(OutputProperty.MODE_ID);
		addProperty(OutputProperty.MODE_EXTERNAL_ID);
		addProperty(OutputProperty.MAXIMUM_SPEED);
		addProperty(OutputProperty.TIME_PERIOD_EXTERNAL_ID);
		addProperty(OutputProperty.TIME_PERIOD_ID);
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
		case LinkOutputTypeConfiguration.LINK_SEGMENT_IDENTIFICATION_BY_NODE_ID:
			outputKeyPropertiesArray = new OutputProperty[2];
			outputKeyPropertiesArray[0] = OutputProperty.DOWNSTREAM_NODE_EXTERNAL_ID;
			outputKeyPropertiesArray[1] = OutputProperty.UPSTREAM_NODE_EXTERNAL_ID;
			valid = true;
			break;
		case LinkOutputTypeConfiguration.LINK_SEGMENT_IDENTIFICATION_BY_ID:
			outputKeyPropertiesArray = new OutputProperty[1];
			outputKeyPropertiesArray[0] = OutputProperty.LINK_SEGMENT_ID;
			valid = true;
			break;
		case LinkOutputTypeConfiguration.LINK_SEGMENT_IDENTIFICATION_BY_EXTERNAL_ID:
			outputKeyPropertiesArray = new OutputProperty[1];
			outputKeyPropertiesArray[0] = OutputProperty.LINK_SEGMENT_EXTERNAL_ID;
			valid = true;
			break;
		default:
			PlanItLogger.severe("Configured keys cannot identify link segments.");
		}
		if (valid) {
			return outputKeyPropertiesArray;
		}
		return null;
	}
}
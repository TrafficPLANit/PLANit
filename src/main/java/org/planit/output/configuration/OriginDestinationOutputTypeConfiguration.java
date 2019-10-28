package org.planit.output.configuration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.planit.exceptions.PlanItException;
import org.planit.logging.PlanItLogger;
import org.planit.output.adapter.OutputAdapter;
import org.planit.output.enums.OutputType;
import org.planit.output.property.OutputProperty;
import org.planit.output.enums.ODSkimOutputType;

/**
 * The configuration for the origin-destination output type.
 * 
 * The following OutputProperty values are included by default:
 * 
 * RUN_ID
 * TIME_PERIOD_EXTERNAL_ID 
 * MODE_EXTERNAL_ID 
 * ORIGIN_ZONE_EXTERNAL_ID 
 * DESTINATION_ZONE_EXTERNAL_ID 
 * COST
 * 
 * 
 * @author markr
 *
 */
public class OriginDestinationOutputTypeConfiguration extends OutputTypeConfiguration {
	
	public static final int ORIGIN_DESTINATION_ID = 1;
	public static final int ORIGIN_DESTINATION_EXTERNAL_ID = 2;
	public static final int ORIGIN_DESTINATION_NOT_IDENTIFIED = 3;
	
	private Set<ODSkimOutputType> activeOdSkimOutputTypes;

	public OriginDestinationOutputTypeConfiguration(OutputAdapter outputAdapter) throws PlanItException {
		super(outputAdapter, OutputType.OD);
		activeOdSkimOutputTypes = new HashSet<ODSkimOutputType>();
		activeOdSkimOutputTypes.add(ODSkimOutputType.COST);
		// add default output properties
		addProperty(OutputProperty.RUN_ID);
		addProperty(OutputProperty.TIME_PERIOD_EXTERNAL_ID);
		addProperty(OutputProperty.MODE_EXTERNAL_ID);
		addProperty(OutputProperty.ORIGIN_ZONE_EXTERNAL_ID);
		addProperty(OutputProperty.DESTINATION_ZONE_EXTERNAL_ID);
		addProperty(OutputProperty.COST);
	}

	/**
	 * Determine how a origin-destination cell is being identified in the output formatter
	 * 
	 * @param outputKeyProperties Map of arrays of keys used to identify the origin and destination
	 * @return the identification method
	 */
	@Override
	public int findIdentificationMethod(Map<OutputType, OutputProperty[]> outputKeyProperties) {
		return findIdentificationMethod(outputKeyProperties, OutputType.OD);
	}

	/**
	 * Determine how a link is being identified in the output formatter
	 * 
	 * @param outputKeyPropertiesArray array of output key property types
	 * @return the value of the identification type determined
	 */
	@Override 
	public int findIdentificationMethod(OutputProperty [] outputKeyPropertiesArray) {
		List<OutputProperty> outputKeyPropertyList = Arrays.asList(outputKeyPropertiesArray);
		if (outputKeyPropertyList.contains(OutputProperty.ORIGIN_ZONE_ID) && outputKeyPropertyList.contains(OutputProperty.DESTINATION_ZONE_ID)) {
			return ORIGIN_DESTINATION_ID;
		}
		if (outputKeyPropertyList.contains(OutputProperty.ORIGIN_ZONE_EXTERNAL_ID) && outputKeyPropertyList.contains(OutputProperty.DESTINATION_ZONE_EXTERNAL_ID)) {
			return ORIGIN_DESTINATION_EXTERNAL_ID;
		}
		return ORIGIN_DESTINATION_NOT_IDENTIFIED;
	}
	
	/**
	 * Validate whether the specified list of keys is valid, and if it is return only the keys which will be used
	 * 
	 * @param identificationMethod the identification method being used
	 * @return array of keys to be used (null if the list is not valid)
	 */
	@Override
	public OutputProperty[] validateAndFilterKeyProperties(int identificationMethod) {
		OutputProperty[] outputKeyPropertiesArray = null;
		boolean valid = false;
		switch (identificationMethod) {
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
	
	/**
	 * Activate an ODSkimOutputType for this output type configuration
	 * 
	 * @param odSkimOutputType ODSkimOutputType to be activated
	 */
	public void activateOdSkimOutputType(ODSkimOutputType odSkimOutputType) {
		activeOdSkimOutputTypes.add(odSkimOutputType);
	}
	
	/**
	 * Deactivate an ODSkimOutputType for this output type configuration
	 * 
	 * @param odSkimOutputType ODSkimOutputType to be deactivated
	 */
    public void deactivateOdSkimOutputType(ODSkimOutputType odSkimOutputType) {
    	activeOdSkimOutputTypes.remove(odSkimOutputType);
    }
    
    public Set<ODSkimOutputType> getActiveOdSkimOutputTypes() {
    	return activeOdSkimOutputTypes;
    }
	
}

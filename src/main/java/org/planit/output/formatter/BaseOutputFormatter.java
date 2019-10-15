package org.planit.output.formatter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.planit.exceptions.PlanItException;
import org.planit.output.OutputManager;
import org.planit.utils.IdGenerator;
import org.planit.output.property.OutputProperty;
import org.planit.time.TimePeriod;
import org.planit.userclass.Mode;
import org.planit.output.OutputType;
import org.planit.output.configuration.LinkOutputTypeConfiguration;
import org.planit.output.configuration.OriginDestinationOutputTypeConfiguration;
import org.planit.output.configuration.OutputTypeConfiguration;

/**
 * Base class for all formatters of output data, i.e. persistence of certain
 * types of data into a particular format
 * 
 * @author markr
 *
 */
public abstract class BaseOutputFormatter implements OutputFormatter {

	private static final OutputTimeUnit DEFAULT_TIME_UNIT = OutputTimeUnit.HOURS;

	/**
	 * Map of OutputProperty types of keys for each OutputType
	 */
	protected Map<OutputType, OutputProperty[]> outputKeyProperties;

	/**
	 * Map of OutputProperty types for values for each OutputType
	 */
	protected Map<OutputType, OutputProperty[]> outputValueProperties;

	/**
	 * Unique internal id of the output writer
	 */
	protected long id;

	/**
	 * Time unit to be used in outputs
	 */
	protected OutputTimeUnit outputTimeUnit;
	
	/**
	 * List of registered OutputTypes
	 */
	protected Set<OutputType> outputTypes;

	/**
	 * Tests whether appropriate key properties have been set to identify a link,
	 * and update the outputKeyProperties Map if they have
	 * 
	 * @param identificationMethod the identification method being used
	 * @return true if the key set acceptable, false otherwise
	 */
	private boolean updatePropertiesForLinkType(int identificationMethod) {

		OutputProperty[] outputKeyPropertiesArray = null;
		boolean valid = false;
		switch (identificationMethod) {
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
		}
		if (valid) {
			outputKeyProperties.put(OutputType.LINK, outputKeyPropertiesArray);
		}
		return valid;
	}
	
	/**
	 * Tests whether appropriate key properties have been set to identify an origin-destination path,
	 * and update the outputKeyProperties Map if they have
	 * 
	 * @param identificationMethod the identification method being used
	 * @return true if the key set acceptable, false otherwise
	 */
	private boolean updatePropertiesForOriginDestinationType(int identificationMethod) {
		OutputProperty[] outputKeyPropertiesArray = null;
		boolean valid = false;
		switch (identificationMethod) {
		case OriginDestinationOutputTypeConfiguration.ORIGIN_DESTINATION_ID:
			outputKeyPropertiesArray = new OutputProperty[2];
			outputKeyPropertiesArray[0] = OutputProperty.ORIGIN_ZONE_ID;
			outputKeyPropertiesArray[1] = OutputProperty.DESTINATION_ZONE_ID;
			valid = true;
			break;
		}
		if (valid) {
			outputKeyProperties.put(OutputType.OD, outputKeyPropertiesArray);
		}
		return valid;
	}

	/**
	 * Tests whether the current output keys are valid for the current output type
	 * configuration, and update the output key properties Map if they are
	 * 
	 * @param outputTypeConfiguration the current output type configuration
	 * @return true if the current output type is valid, false otherwise
	 */
	protected boolean isOutputKeysValid(OutputTypeConfiguration outputTypeConfiguration) {
		OutputType outputType = outputTypeConfiguration.getOutputType();
		int identificationMethod = outputTypeConfiguration.findIdentificationMethod(outputKeyProperties);

		switch (outputType) {
		case GENERAL:
			return true;
		case LINK:
			return updatePropertiesForLinkType(identificationMethod);
		case SIMULATION:
			return true;
		case OD:
			return updatePropertiesForOriginDestinationType(identificationMethod);
		}
		return false;
	}

	/**
	 * Set the output properties of the key values for the current output type configuration
	 * 
	 * @param outputTypeConfiguration            the current output type configuration
	 */
	protected void setOutputKeyProperties(OutputTypeConfiguration outputTypeConfiguration) {
		OutputType outputType = outputTypeConfiguration.getOutputType();
		OutputProperty[] outputKeyPropertyArray = outputTypeConfiguration.getOutputKeyProperties();
		outputKeyProperties.put(outputType, outputKeyPropertyArray);
	}

	/**
	 * Sets the output properties of the data values for the current output type configuration
	 * 
	 * @param outputTypeConfiguration            the current output type configuration
	 */
	protected void setOutputValueProperties(OutputTypeConfiguration outputTypeConfiguration) {
		OutputType outputType = outputTypeConfiguration.getOutputType();
		OutputProperty[] outputValuePropertyArray = outputTypeConfiguration.getOutputValueProperties();
		outputValueProperties.put(outputType, outputValuePropertyArray);
	}

	/**
	 * Write link results for the current time period to the CSV file
	 * 
	 * @param outputTypeConfiguration OutputTypeConfiguration for current
	 *                                persistence
	 * @param modes                   Set of modes of travel
	 * @param timePeriod              current time period
	 * @throws PlanItException thrown if there is an error
	 */
	protected abstract void writeLinkResultsForCurrentModeAndTimePeriod(OutputTypeConfiguration outputTypeConfiguration,
			Set<Mode> modes, TimePeriod timePeriod) throws PlanItException;

	/**
	 * Write General results for the current time period to the CSV file
	 * 
	 * @param outputTypeConfiguration OutputTypeConfiguration for current
	 *                                persistence
	 * @param modes                   Set of current modes of travel
	 * @param timePeriod              current time period
	 * @throws PlanItException thrown if there is an error
	 */
	protected abstract void writeGeneralResultsForCurrentModeAndTimePeriod(OutputTypeConfiguration outputTypeConfiguration,
			Set<Mode> modes, TimePeriod timePeriod) throws PlanItException;
	
	/**
	 * Write Origin-Destination results for the time period to the CSV file
	 * 
	 * @param outputTypeConfiguration OutputTypeConfiguration for current
	 *                                persistence
	 * @param modes                   Set of modes of travel
	 * @param timePeriod              current time period
	 * @throws PlanItException thrown if there is an error
	 */
	protected abstract void writeOdResultsForCurrentModeAndTimePeriod(OutputTypeConfiguration outputTypeConfiguration,
			Set<Mode> modes, TimePeriod timePeriod) throws PlanItException;
	
	/**
	 * Write Simulation results for the current time period to the CSV file
	 * 
	 * @param outputTypeConfiguration OutputTypeConfiguration for current
	 *                                persistence
	 * @param modes                   Set of modes of travel
	 * @param timePeriod              current time period
	 * @throws PlanItException thrown if there is an error
	 */
	protected  abstract void writeSimulationResultsForCurrentModeAndTimePeriod(OutputTypeConfiguration outputTypeConfiguration,
			Set<Mode> modes, TimePeriod timePeriod) throws PlanItException;

	/**
	 * Constructor,
	 */
	public BaseOutputFormatter() {
		this.id = IdGenerator.generateId(OutputManager.class);
		outputKeyProperties = new HashMap<OutputType, OutputProperty[]>();
		outputValueProperties = new HashMap<OutputType, OutputProperty[]>();
		outputTimeUnit = DEFAULT_TIME_UNIT;
	}

	/**
	 * Write data to output file
	 * 
	 * @param timePeriod              time period for current results
	 * @param modes                   Set of modes covered by current results
	 * @param outputTypeConfiguration output configuration being used
	 * @throws PlanItException thrown if there is an error
	 */
	@Override
	public void persist(TimePeriod timePeriod, Set<Mode> modes, OutputTypeConfiguration outputTypeConfiguration)
			throws PlanItException {
		setOutputValueProperties(outputTypeConfiguration);
		setOutputKeyProperties(outputTypeConfiguration);
		if (!isOutputKeysValid(outputTypeConfiguration)) {
			throw new PlanItException("Invalid output keys defined for output type.");
		}
		switch (outputTypeConfiguration.getOutputType()) {
		case GENERAL:
			writeGeneralResultsForCurrentModeAndTimePeriod(outputTypeConfiguration, modes, timePeriod);
			break;
		case LINK:
			writeLinkResultsForCurrentModeAndTimePeriod(outputTypeConfiguration, modes, timePeriod);
			break;
		case OD:
			writeOdResultsForCurrentModeAndTimePeriod(outputTypeConfiguration, modes, timePeriod);
			break;
		case SIMULATION:
			writeSimulationResultsForCurrentModeAndTimePeriod(outputTypeConfiguration, modes, timePeriod);
			break;
		}
	}
	
	// getters - setters

	public long getId() {
		return id;
	}

	/**
	 * Returns the current time units
	 * 
	 * @return the current time units 
	 */
	public OutputTimeUnit getOutputTimeUnit() {
		return outputTimeUnit;
	}

	/**
	 * Sets the current time units
	 * 
	 * @param outputTimeUnit the specified time units
	 */
	public void setOutputTimeUnit(OutputTimeUnit outputTimeUnit) {
		this.outputTimeUnit = outputTimeUnit;
	}
	
	/**
	 * Returns the current time units as a String
	 * 
	 * @return the current time units as a String
	 */
	public String getOutputTimeUnitString() {
		return outputTimeUnit.value();
	}

	/**
	 * Returns the multiplier for the current time unit
	 * 
	 * @return the multiplier for the current time unit
	 */
	public double getTimeUnitMultiplier() {
		switch (outputTimeUnit) {
		case HOURS:
			return 1.0;
		case MINUTES:
			return 60.0;
		case SECONDS:
			return 3600.0;
		}
		return -1.0;
	}

}

package org.planit.output.formatter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.planit.exceptions.PlanItException;
import org.planit.output.OutputManager;
import org.planit.output.adapter.OutputAdapter;
import org.planit.output.configuration.OutputTypeConfiguration;
import org.planit.output.enums.OutputTimeUnit;
import org.planit.output.enums.OutputType;
import org.planit.output.property.OutputProperty;
import org.planit.time.TimePeriod;
import org.planit.userclass.Mode;
import org.planit.utils.IdGenerator;

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
	 * Map to store whether any data values have been stored for a given output
	 * type.
	 * 
	 * If data have been stored for an output type, it is "locked" so its key and
	 * output properties cannot be reset
	 */
	protected Map<OutputType, Boolean> outputTypeValuesLocked;

	/**
	 * Map to store which output types are already in use as keys
	 */
	protected Map<OutputType, Boolean> outputTypeKeysLocked;

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
	 * Initialize the output key properties for the specified output type configuration
	 * 
	 * @param outputTypeConfiguration the specified output type configuration
	 * @throws PlanItException thrown if the output keys are invalid or the output type has not been configured yet
	 */
	private void initializeKeyProperties(OutputTypeConfiguration outputTypeConfiguration) throws PlanItException {
		OutputProperty[] outputKeyPropertyArray = outputTypeConfiguration.getOutputKeyProperties();
		OutputType outputType = outputTypeConfiguration.getOutputType();
		OutputProperty[] outputKeyPropertiesArray = outputTypeConfiguration.validateAndFilterKeyProperties(outputKeyPropertyArray);
		if (outputKeyPropertiesArray == null ) {
			throw new PlanItException("Key properties invalid for OutputType " + outputType.value() + " not correctly defined.");
		}
		outputKeyProperties.put(outputType, outputKeyPropertiesArray);
	}

	/**
	 * Lock the output keys and values for a specified output type
	 * 
	 * @param outputType the output type to be locked
	 * 
	 * @param outputType
	 */
	protected void lockOutputProperties(OutputType outputType) {
		outputTypeValuesLocked.put(outputType, true);
		outputTypeKeysLocked.put(outputType, true);
	}

	/**
	 * Write link results for the current time period to the CSV file
	 * 
	 * @param outputTypeConfiguration OutputTypeConfiguration for current  persistence
	 * @param outputTypeAdapter OutputTypeAdapter for current persistence
	 * @param modes                   Set of modes of travel
	 * @param timePeriod              current time period
	 * @throws PlanItException thrown if there is an error
	 */
	protected abstract void writeLinkResultsForCurrentTimePeriod(OutputTypeConfiguration outputTypeConfiguration, OutputAdapter outputAdapter,
			Set<Mode> modes, TimePeriod timePeriod) throws PlanItException;

	/**
	 * Write General results for the current time period to the CSV file
	 * 
	 * @param outputTypeConfiguration OutputTypeConfiguration for current persistence
	 * @param outputAdapter OutputAdapter for current persistence
	 * @param modes                   Set of current modes of travel
	 * @param timePeriod              current time period
	 * @throws PlanItException thrown if there is an error
	 */
	protected abstract void writeGeneralResultsForCurrentTimePeriod(OutputTypeConfiguration outputTypeConfiguration, OutputAdapter outputAdapter,
			Set<Mode> modes, TimePeriod timePeriod) throws PlanItException;

	/**
	 * Write Origin-Destination results for the time period to the CSV file
	 * 
	 * @param outputTypeConfiguration OutputTypeConfiguration for current persistence
	 * @param outputAdapter OutputAdapter for current persistence
	 * @param modes                   Set of modes of travel
	 * @param timePeriod              current time period
	 * @throws PlanItException thrown if there is an error
	 */
	protected abstract void writeOdResultsForCurrentTimePeriod(OutputTypeConfiguration outputTypeConfiguration, OutputAdapter outputAdapter,
			Set<Mode> modes, TimePeriod timePeriod) throws PlanItException;

	/**
	 * Write Simulation results for the current time period to the CSV file
	 * 
	 * @param outputTypeConfiguration OutputTypeConfiguration for current  persistence
	 * @param outputAdapter OutputAdapter for current persistence
	 * @param modes                   Set of modes of travel
	 * @param timePeriod              current time period
	 * @throws PlanItException thrown if there is an error
	 */
	protected abstract void writeSimulationResultsForCurrentTimePeriod(OutputTypeConfiguration outputTypeConfiguration, OutputAdapter outputAdapter,
			Set<Mode> modes, TimePeriod timePeriod) throws PlanItException;

	/**
	 * Constructor
	 */
	public BaseOutputFormatter() {
		this.id = IdGenerator.generateId(OutputManager.class);
		outputKeyProperties = new HashMap<OutputType, OutputProperty[]>();
		outputValueProperties = new HashMap<OutputType, OutputProperty[]>();
		outputTimeUnit = DEFAULT_TIME_UNIT;
		outputTypeValuesLocked = new HashMap<OutputType, Boolean>();
		outputTypeKeysLocked = new HashMap<OutputType, Boolean>();
		for (OutputType outputType : OutputType.values()) {
			outputTypeValuesLocked.put(outputType, false);
			outputTypeKeysLocked.put(outputType, false);
		}
	}
	
	/**
	 * Write data to output file
	 * 
	 * @param timePeriod              time period for current results
	 * @param modes                   Set of modes covered by current results
	 * @param outputTypeConfiguration output configuration being used
	 * @param outputAdapter output adapter being used
	 * @throws PlanItException thrown if there is an error
	 */
	@Override
	public void persist(TimePeriod timePeriod, Set<Mode> modes, OutputTypeConfiguration outputTypeConfiguration, OutputAdapter outputAdapter) throws PlanItException {
		OutputType outputType = outputTypeConfiguration.getOutputType();
		OutputProperty[] outputValuePropertyArray = outputTypeConfiguration.getOutputValueProperties();
		if (!outputTypeValuesLocked.get(outputType)) {			
			outputValueProperties.put(outputType, outputValuePropertyArray);
		} else {
			OutputProperty[] existingOutputValuePropertyArray = outputValueProperties.get(outputType);
			if (outputValuePropertyArray.length != existingOutputValuePropertyArray.length) {
				throw new PlanItException("An attempt was made to change the output value properties after they had been locked.");
			}
			for (int i=0; i<outputValuePropertyArray.length ; i++) {
				if (!existingOutputValuePropertyArray[i].equals(outputValuePropertyArray[i])) {
					throw new PlanItException("An attempt was made to change the output value properties after they had been locked.");
				}
			}
		}
		
		if (!outputTypeKeysLocked.get(outputType)) {
			initializeKeyProperties(outputTypeConfiguration);
		} 

		switch (outputType) {
		case GENERAL:
			writeGeneralResultsForCurrentTimePeriod(outputTypeConfiguration, outputAdapter, modes, timePeriod);
			break;
		case LINK:
			writeLinkResultsForCurrentTimePeriod(outputTypeConfiguration, outputAdapter, modes, timePeriod);
			break;
		case OD:
			writeOdResultsForCurrentTimePeriod(outputTypeConfiguration, outputAdapter, modes, timePeriod);
			break;
		case SIMULATION:
			writeSimulationResultsForCurrentTimePeriod(outputTypeConfiguration, outputAdapter, modes, timePeriod);
			break;
		}
		lockOutputProperties(outputType);
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

}
package org.planit.output.formatter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.planit.exceptions.PlanItException;
import org.planit.output.OutputManager;
import org.planit.output.adapter.OutputAdapter;
import org.planit.output.configuration.OutputConfiguration;
import org.planit.output.configuration.OutputTypeConfiguration;
import org.planit.output.enums.OutputTimeUnit;
import org.planit.output.enums.OutputType;
import org.planit.output.enums.OutputTypeEnum;
import org.planit.output.enums.SubOutputTypeEnum;
import org.planit.output.property.OutputProperty;
import org.planit.time.TimePeriod;
import org.planit.utils.misc.IdGenerator;
import org.planit.utils.network.physical.Mode;

/**
 * Base class for all formatters of output data, i.e. persistence of certain types of data into a
 * particular format
 * 
 * @author markr
 *
 */
public abstract class BaseOutputFormatter implements OutputFormatter {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(BaseOutputFormatter.class.getCanonicalName());

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
   * Map to store whether any data values have been stored for a given output type.
   * 
   * If data have been stored for an output type, it is "locked" so its key and output properties
   * cannot be reset
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
   * @throws PlanItException thrown if the output keys are invalid or the output type has not been
   *           configured yet
   */
  private void initializeKeyProperties(OutputTypeConfiguration outputTypeConfiguration) throws PlanItException {
    OutputProperty[] outputKeyPropertyArray = outputTypeConfiguration.getOutputKeyProperties();
    OutputType outputType = outputTypeConfiguration.getOutputType();
    OutputProperty[] outputKeyPropertiesArray = outputTypeConfiguration.validateAndFilterKeyProperties(
        outputKeyPropertyArray);
    if (outputKeyPropertiesArray == null) {
      String errorMessage = "Key properties invalid for OutputType " + outputType.value() + " not correctly defined.";
      throw new PlanItException(errorMessage);
    }
    outputKeyProperties.put(outputType, outputKeyPropertiesArray);
  }

  /**
   * Lock the output keys and values for a specified output type
   * 
   * @param outputType the output type to be locked
   */
  protected void lockOutputProperties(OutputType outputType) {
    outputTypeValuesLocked.put(outputType, true);
    outputTypeKeysLocked.put(outputType, true);
  }

  /**
   * Write link results for the current time period to the CSV file
   * 
   * @param outputConfiguration output configuration
   * @param outputTypeConfiguration OutputTypeConfiguration for current persistence
   * @param currentOutputType active OutputTypeEnum of the configuration we are persisting for (can
   *          be a  SubOutputTypeEnum or an OutputType)
   * @param outputAdapter OutputAdapter for current persistence
   * @param modes Set of modes of travel
   * @param timePeriod current time period
   * @param iterationIndex current iteration index
   * @throws PlanItException thrown if there is an error
   */
  protected abstract void writeLinkResultsForCurrentTimePeriod(OutputConfiguration outputConfiguration, OutputTypeConfiguration outputTypeConfiguration,
      OutputTypeEnum currentOutputType, OutputAdapter outputAdapter,
      Set<Mode> modes, TimePeriod timePeriod, int iterationIndex) throws PlanItException;

  /**
   * Write General results for the current time period to the CSV file
   * 
   * @param outputConfiguration output configuration
   * @param outputTypeConfiguration OutputTypeConfiguration for current persistence
   * @param currentOutputType active OutputTypeEnum of the configuration we are persisting for (can
   *          be a SubOutputTypeEnum or an OutputType)
   * @param outputAdapter OutputAdapter for current persistence
   * @param modes Set of modes of travel
   * @param timePeriod current time period
   * @param iterationIndex current iteration index
   * @throws PlanItException thrown if there is an error
   */
  protected abstract void writeGeneralResultsForCurrentTimePeriod(OutputConfiguration outputConfiguration, OutputTypeConfiguration outputTypeConfiguration,
      OutputTypeEnum currentOutputType, OutputAdapter outputAdapter,
      Set<Mode> modes, TimePeriod timePeriod, int iterationIndex) throws PlanItException;

  /**
   * Write Origin-Destination results for the time period to the CSV file
   * 
   * @param outputConfiguration output configuration
   * @param outputTypeConfiguration OutputTypeConfiguration for current persistence
   * @param currentOutputType active OutputTypeEnum of the configuration we are persisting for (can
   *          be a  SubOutputTypeEnum or an OutputType)
   * @param outputAdapter OutputAdapter for current persistence
   * @param modes Set of modes of travel
   * @param timePeriod current time period
   * @param iterationIndex current iteration index
   * @throws PlanItException thrown if there is an error
   */
  protected abstract void writeOdResultsForCurrentTimePeriod(OutputConfiguration outputConfiguration, OutputTypeConfiguration outputTypeConfiguration,
      OutputTypeEnum currentOutputType, OutputAdapter outputAdapter,
      Set<Mode> modes, TimePeriod timePeriod, int iterationIndex) throws PlanItException;

  /**
   * Write Simulation results for the current time period to the CSV file
   * 
   * @param outputConfiguration output configuration
   * @param outputTypeConfiguration OutputTypeConfiguration for current persistence
   * @param currentOutputType active OutputTypeEnum of the configuration we are persisting for (can
   *          be a  SubOutputTypeEnum or an OutputType)
   * @param outputAdapter OutputAdapter for current persistence
   * @param modes Set of modes of travel
   * @param timePeriod current time period
   * @param iterationIndex current iteration index
   * @throws PlanItException thrown if there is an error
   */
  protected abstract void writeSimulationResultsForCurrentTimePeriod(OutputConfiguration outputConfiguration, OutputTypeConfiguration outputTypeConfiguration,
      OutputTypeEnum currentOutputType, OutputAdapter outputAdapter,
      Set<Mode> modes, TimePeriod timePeriod, int iterationIndex) throws PlanItException;

  /**
   * Write OD Path results for the time period to the CSV file
   * 
   * @param outputConfiguration output configuration
   * @param outputTypeConfiguration OutputTypeConfiguration for current persistence
   * @param currentOutputType active OutputTypeEnum of the configuration we are persisting for (can
   *          be a  SubOutputTypeEnum or an OutputType)
   * @param outputAdapter OutputAdapter for current persistence
   * @param modes Set of modes of travel
   * @param timePeriod current time period
   * @param iterationIndex current iteration index
   * @throws PlanItException thrown if there is an error
   */
  protected abstract void writePathResultsForCurrentTimePeriod(OutputConfiguration outputConfiguration, OutputTypeConfiguration outputTypeConfiguration,
      OutputTypeEnum currentOutputType, OutputAdapter outputAdapter,
      Set<Mode> modes, TimePeriod timePeriod, int iterationIndex) throws PlanItException;

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
   * @param timePeriod time period for current results
   * @param modes Set of modes covered by current results
   * @param outputConfiguration output configuration
   * @param outputTypeConfiguration output configuration being used
   * @param outputAdapter output adapter being used
   * @throws PlanItException thrown if there is an error
   */
  @Override
  public void persist(TimePeriod timePeriod, Set<Mode> modes, OutputConfiguration outputConfiguration, OutputTypeConfiguration outputTypeConfiguration,
      OutputAdapter outputAdapter) throws PlanItException {
    OutputType outputType = outputTypeConfiguration.getOutputType();
    OutputProperty[] outputValuePropertyArray = outputTypeConfiguration.getOutputValueProperties();
    if (!outputTypeValuesLocked.get(outputType)) {
      outputValueProperties.put(outputType, outputValuePropertyArray);
    } else {
      OutputProperty[] existingOutputValuePropertyArray = outputValueProperties.get(outputType);
      if (outputValuePropertyArray.length != existingOutputValuePropertyArray.length) {
        String errorMessage = "An attempt was made to change the output value properties after they had been locked.";
        throw new PlanItException(errorMessage);
      }
      for (int i = 0; i < outputValuePropertyArray.length; i++) {
        if (!existingOutputValuePropertyArray[i].equals(outputValuePropertyArray[i])) {
          String errorMessage = "An attempt was made to change the output value properties after they had been locked.";
          throw new PlanItException(errorMessage);
        }
      }
    }

    if (!outputTypeKeysLocked.get(outputType)) {
      initializeKeyProperties(outputTypeConfiguration);
    }

    // Each output type configuration can contain multiple suboutputypes (or not). We collect the
    // iteration reference (which
    // might be different
    // from the simulation iteration, and the (sub)outputtype combination before proceeding with the
    // actual persisting
    Map<OutputTypeEnum, Integer> outputTypeIterationInformation = new HashMap<OutputTypeEnum, Integer>();
    if (outputTypeConfiguration.hasActiveSubOutputTypes()) {
      // subdivided in suboutputtypes, each having their own file and possible a different reference
      // iteration index
      Set<SubOutputTypeEnum> subOutputTypes = outputTypeConfiguration.getActiveSubOutputTypes();
      for (SubOutputTypeEnum subOutputTypeEnum : subOutputTypes) {
        int iterationIndex = outputAdapter.getOutputTypeAdapter(outputType).getIterationIndexForSubOutputType(
            subOutputTypeEnum);
        outputTypeIterationInformation.put(subOutputTypeEnum, iterationIndex);
      }
    } else {
      // regular approach, single outputtype with single iteration reference
      int iterationIndex = outputAdapter.getOutputTypeAdapter(outputType).getIterationIndexForSubOutputType(null);
      outputTypeIterationInformation.put(outputType, iterationIndex);
    }

    // Each unique combination of (sub)output type (configuration), its iteration index, and related
    // data is now relayed to
    // the appropriate methods
    for (Map.Entry<OutputTypeEnum, Integer> entry : outputTypeIterationInformation.entrySet()) {
      OutputTypeEnum currentOutputTypeEnum = entry.getKey();
      int iterationIndex = entry.getValue();
      switch (outputType) {
        case GENERAL:
          writeGeneralResultsForCurrentTimePeriod(outputConfiguration, outputTypeConfiguration, currentOutputTypeEnum, outputAdapter, modes,
              timePeriod, iterationIndex);
          break;
        case LINK:
          writeLinkResultsForCurrentTimePeriod(outputConfiguration, outputTypeConfiguration, currentOutputTypeEnum, outputAdapter, modes,
              timePeriod, iterationIndex);
          break;
        case OD:
          writeOdResultsForCurrentTimePeriod(outputConfiguration, outputTypeConfiguration, currentOutputTypeEnum, outputAdapter, modes,
              timePeriod, iterationIndex);
          break;
        case SIMULATION:
          writeSimulationResultsForCurrentTimePeriod(outputConfiguration, outputTypeConfiguration, currentOutputTypeEnum, outputAdapter,
              modes, timePeriod, iterationIndex);
          break;
        case PATH:
          writePathResultsForCurrentTimePeriod(outputConfiguration, outputTypeConfiguration, currentOutputTypeEnum, outputAdapter, modes,
              timePeriod, iterationIndex);
          break;
      }
      lockOutputProperties(outputType);
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

}

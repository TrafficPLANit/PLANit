package org.goplanit.output.formatter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import org.goplanit.output.adapter.OutputAdapter;
import org.goplanit.output.configuration.OutputConfiguration;
import org.goplanit.output.configuration.OutputTypeConfiguration;
import org.goplanit.output.enums.OutputType;
import org.goplanit.output.enums.OutputTypeEnum;
import org.goplanit.output.enums.SubOutputTypeEnum;
import org.goplanit.output.property.OutputProperty;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.time.TimePeriod;
import org.goplanit.utils.unit.Unit;

/**
 * Base class for all formatters of output data, i.e. persistence of certain types of data into a particular format
 * 
 * @author markr
 *
 */
public abstract class BaseOutputFormatter implements OutputFormatter {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(BaseOutputFormatter.class.getCanonicalName());

  /**
   * default time unit used
   */
  private static final Unit DEFAULT_TIME_UNIT = Unit.HOUR;

  /**
   * Map of OutputProperties of keys for each OutputType
   */
  protected Map<OutputType, OutputProperty[]> outputKeyProperties;

  /**
   * Map of OutputProperties for values for each OutputType
   */
  protected Map<OutputType, OutputProperty[]> outputValueProperties;

  /**
   * Map to store whether any data values have been stored for a given output type.
   * 
   * If data have been stored for an output type, it is "locked" so its key and output properties cannot be reset
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
  protected Unit outputTimeUnit;

  /**
   * List of registered OutputTypes
   */
  protected Set<OutputType> outputTypes;

  /**
   * The location of the initial costs files
   */
  protected String initialCostsLocation;

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

    PlanItException.throwIf(outputKeyPropertiesArray == null, "Key properties invalid for OutputType " + outputType.value() + " not correctly defined");
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
   * @param outputConfiguration     output configuration
   * @param outputTypeConfiguration OutputTypeConfiguration for current persistence
   * @param currentOutputType       active OutputTypeEnum of the configuration we are persisting for (can be a SubOutputTypeEnum or an OutputType)
   * @param outputAdapter           OutputAdapter for current persistence
   * @param modes                   Set of modes of travel
   * @param timePeriod              current time period
   * @param iterationIndex          current iteration index
   * @throws PlanItException thrown if there is an error
   */
  protected abstract void writeLinkResultsForCurrentTimePeriod(OutputConfiguration outputConfiguration, OutputTypeConfiguration outputTypeConfiguration,
      OutputTypeEnum currentOutputType, OutputAdapter outputAdapter, Set<Mode> modes, TimePeriod timePeriod, int iterationIndex) throws PlanItException;

  /**
   * Write General results for the current time period to the CSV file
   * 
   * @param outputConfiguration     output configuration
   * @param outputTypeConfiguration OutputTypeConfiguration for current persistence
   * @param currentOutputType       active OutputTypeEnum of the configuration we are persisting for (can be a SubOutputTypeEnum or an OutputType)
   * @param outputAdapter           OutputAdapter for current persistence
   * @param modes                   Set of modes of travel
   * @param timePeriod              current time period
   * @param iterationIndex          current iteration index
   * @throws PlanItException thrown if there is an error
   */
  protected abstract void writeGeneralResultsForCurrentTimePeriod(OutputConfiguration outputConfiguration, OutputTypeConfiguration outputTypeConfiguration,
      OutputTypeEnum currentOutputType, OutputAdapter outputAdapter, Set<Mode> modes, TimePeriod timePeriod, int iterationIndex) throws PlanItException;

  /**
   * Write Origin-Destination results for the time period to the CSV file
   * 
   * @param outputConfiguration     output configuration
   * @param outputTypeConfiguration OutputTypeConfiguration for current persistence
   * @param currentOutputType       active OutputTypeEnum of the configuration we are persisting for (can be a SubOutputTypeEnum or an OutputType)
   * @param outputAdapter           OutputAdapter for current persistence
   * @param modes                   Set of modes of travel
   * @param timePeriod              current time period
   * @param iterationIndex          current iteration index
   * @throws PlanItException thrown if there is an error
   */
  protected abstract void writeOdResultsForCurrentTimePeriod(OutputConfiguration outputConfiguration, OutputTypeConfiguration outputTypeConfiguration,
      OutputTypeEnum currentOutputType, OutputAdapter outputAdapter, Set<Mode> modes, TimePeriod timePeriod, int iterationIndex) throws PlanItException;

  /**
   * Write Simulation results for the current time period to the CSV file
   * 
   * @param outputConfiguration     output configuration
   * @param outputTypeConfiguration OutputTypeConfiguration for current persistence
   * @param currentOutputType       active OutputTypeEnum of the configuration we are persisting for (can be a SubOutputTypeEnum or an OutputType)
   * @param outputAdapter           OutputAdapter for current persistence
   * @param modes                   Set of modes of travel
   * @param timePeriod              current time period
   * @param iterationIndex          current iteration index
   * @throws PlanItException thrown if there is an error
   */
  protected abstract void writeSimulationResultsForCurrentTimePeriod(OutputConfiguration outputConfiguration, OutputTypeConfiguration outputTypeConfiguration,
      OutputTypeEnum currentOutputType, OutputAdapter outputAdapter, Set<Mode> modes, TimePeriod timePeriod, int iterationIndex) throws PlanItException;

  /**
   * Write OD Path results for the time period to the CSV file
   * 
   * @param outputConfiguration     output configuration
   * @param outputTypeConfiguration OutputTypeConfiguration for current persistence
   * @param currentOutputType       active OutputTypeEnum of the configuration we are persisting for (can be a SubOutputTypeEnum or an OutputType)
   * @param outputAdapter           OutputAdapter for current persistence
   * @param modes                   Set of modes of travel
   * @param timePeriod              current time period
   * @param iterationIndex          current iteration index
   * @throws PlanItException thrown if there is an error
   */
  protected abstract void writePathResultsForCurrentTimePeriod(OutputConfiguration outputConfiguration, OutputTypeConfiguration outputTypeConfiguration,
      OutputTypeEnum currentOutputType, OutputAdapter outputAdapter, Set<Mode> modes, TimePeriod timePeriod, int iterationIndex) throws PlanItException;

  /**
   * Constructor
   * 
   * @param groupId, contiguous id generation within this group for instances of this class
   */
  public BaseOutputFormatter(IdGroupingToken groupId) {
    this.id = IdGenerator.generateId(groupId, BaseOutputFormatter.class);
    this.outputKeyProperties = new HashMap<OutputType, OutputProperty[]>();
    this.outputValueProperties = new HashMap<OutputType, OutputProperty[]>();
    this.outputTimeUnit = DEFAULT_TIME_UNIT;
    this.outputTypeValuesLocked = new HashMap<OutputType, Boolean>();
    this.outputTypeKeysLocked = new HashMap<OutputType, Boolean>();
    for (OutputType outputType : OutputType.values()) {
      this.outputTypeValuesLocked.put(outputType, false);
      this.outputTypeKeysLocked.put(outputType, false);
    }
  }

  /**
   * Write data to output file
   * 
   * @param timePeriod              time period for current results
   * @param modes                   Set of modes covered by current results
   * @param outputConfiguration     output configuration
   * @param outputTypeConfiguration output configuration being used
   * @param outputAdapter           output adapter being used
   * @throws PlanItException thrown if there is an error
   */
  @Override
  public void persist(TimePeriod timePeriod, Set<Mode> modes, OutputConfiguration outputConfiguration, OutputTypeConfiguration outputTypeConfiguration, OutputAdapter outputAdapter)
      throws PlanItException {

    OutputType outputType = outputTypeConfiguration.getOutputType();
    OutputProperty[] outputValuePropertyArray = outputTypeConfiguration.getOutputValueProperties();
    if (!outputTypeValuesLocked.get(outputType)) {
      outputValueProperties.put(outputType, outputValuePropertyArray);
    } else {
      OutputProperty[] existingOutputValuePropertyArray = outputValueProperties.get(outputType);

      PlanItException.throwIf(outputValuePropertyArray.length != existingOutputValuePropertyArray.length,
          "An attempt was made to change the output value properties after they had been locked");

      for (int i = 0; i < outputValuePropertyArray.length; i++) {
        PlanItException.throwIf(!existingOutputValuePropertyArray[i].equals(outputValuePropertyArray[i]),
            "An attempt was made to change the output value properties after they had been locked");
      }
    }

    if (!outputTypeKeysLocked.get(outputType)) {
      initializeKeyProperties(outputTypeConfiguration);
    }

    // Each output type configuration can contain multiple suboutputypes (or not). We collect the
    // iteration reference (which might be different from the simulation iteration, and the
    // (sub)outputtype combination before proceeding with the actual persisting
    Map<OutputTypeEnum, Integer> outputTypeIterationInformation = new HashMap<OutputTypeEnum, Integer>();
    if (outputTypeConfiguration.hasActiveSubOutputTypes()) {
      // subdivided in suboutputtypes, each having their own file and possible a different reference
      // iteration index
      Set<SubOutputTypeEnum> subOutputTypes = outputTypeConfiguration.getActiveSubOutputTypes();
      for (SubOutputTypeEnum subOutputTypeEnum : subOutputTypes) {
        Optional<Integer> iterationIndex = outputAdapter.getOutputTypeAdapter(outputType).getIterationIndexForSubOutputType(subOutputTypeEnum);
        if (iterationIndex.isEmpty()) {
          throw new PlanItException("iteration index could not be retrieved when persisting");
        }
        outputTypeIterationInformation.put(subOutputTypeEnum, iterationIndex.get());
      }
    } else {
      // regular approach, single outputtype with single iteration reference
      Optional<Integer> iterationIndex = outputAdapter.getOutputTypeAdapter(outputType).getIterationIndexForSubOutputType(null);
      if (iterationIndex.isEmpty()) {
        throw new PlanItException("iteration index could not be retrieved when persisting");
      }
      outputTypeIterationInformation.put(outputType, iterationIndex.get());
    }

    // Each unique combination of (sub)output type (configuration), its iteration index, and related
    // data is now relayed to the appropriate methods
    for (Map.Entry<OutputTypeEnum, Integer> entry : outputTypeIterationInformation.entrySet()) {
      OutputTypeEnum currentOutputTypeEnum = entry.getKey();
      int iterationIndex = entry.getValue();
      switch (outputType) {
      case GENERAL:
        writeGeneralResultsForCurrentTimePeriod(outputConfiguration, outputTypeConfiguration, currentOutputTypeEnum, outputAdapter, modes, timePeriod, iterationIndex);
        break;
      case LINK:
        writeLinkResultsForCurrentTimePeriod(outputConfiguration, outputTypeConfiguration, currentOutputTypeEnum, outputAdapter, modes, timePeriod, iterationIndex);
        break;
      case OD:
        writeOdResultsForCurrentTimePeriod(outputConfiguration, outputTypeConfiguration, currentOutputTypeEnum, outputAdapter, modes, timePeriod, iterationIndex);
        break;
      case SIMULATION:
        writeSimulationResultsForCurrentTimePeriod(outputConfiguration, outputTypeConfiguration, currentOutputTypeEnum, outputAdapter, modes, timePeriod, iterationIndex);
        break;
      case PATH:
        writePathResultsForCurrentTimePeriod(outputConfiguration, outputTypeConfiguration, currentOutputTypeEnum, outputAdapter, modes, timePeriod, iterationIndex);
        break;
      }
      lockOutputProperties(outputType);
    }
  }

  // getters - setters

  public long getId() {
    return id;
  }

}
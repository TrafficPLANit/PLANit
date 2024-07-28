package org.goplanit.output.formatter;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.data.MultiKeyPlanItData;
import org.goplanit.network.layer.macroscopic.MacroscopicLinkSegmentImpl;
import org.goplanit.od.path.OdMultiPathIterator;
import org.goplanit.od.skim.OdSkimMatrix;
import org.goplanit.od.skim.OdSkimMatrix.OdSkimMatrixIterator;
import org.goplanit.output.adapter.*;
import org.goplanit.output.configuration.OutputConfiguration;
import org.goplanit.output.configuration.OutputTypeConfiguration;
import org.goplanit.output.configuration.PathOutputTypeConfiguration;
import org.goplanit.output.enums.OdSkimSubOutputType;
import org.goplanit.output.enums.OutputType;
import org.goplanit.output.enums.OutputTypeEnum;
import org.goplanit.output.enums.PathOutputIdentificationType;
import org.goplanit.output.enums.SubOutputTypeEnum;
import org.goplanit.output.property.OutputProperty;
import org.goplanit.output.property.OutputPropertyType;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.od.OdDataIterator;
import org.goplanit.utils.path.ManagedDirectedPath;
import org.goplanit.utils.time.TimePeriod;
import org.goplanit.utils.unit.VehiclesUnit;

/**
 * OutputFormatter which stores data in memory, using specified keys and output properties.
 * 
 * @author gman6028
 *
 */
public class MemoryOutputFormatter extends BaseOutputFormatter {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(MacroscopicLinkSegmentImpl.class.getCanonicalName());

  private static final int TIMEPERIOD_KEY_INDEX = 1;

  private static final int ITERATION_KEY_INDEX = 2;

  private static final int OUTPUT_TYPE_KEY_INDEX = 3;

  /**
   * MultiKeyMap of data stores
   */
  private MultiKeyMap<Object, MultiKeyPlanItData> timeModeOutputTypeIterationDataMap;

  /**
   * Iterator to loop over results contained in this formatter
   */
  public MemoryOutputIterator iterator;

  /**
   * Returns an array of values (key or values)
   * 
   * @param outputPropertiesArray OutputProperty array to specify which values are to be returns
   * @param getValueFromAdapter   lambda function to find the output value for each label
   * @return array of output values
   */
  private Object[] getValues(OutputProperty[] outputPropertiesArray, Function<OutputProperty, Object> getValueFromAdapter) {
    Object[] values = new Object[outputPropertiesArray.length];
    for (int i = 0; i < outputPropertiesArray.length; i++) {
      values[i] = getValueFromAdapter.apply(outputPropertiesArray[i]);
      if (values[i] instanceof PlanItException) {
        throw new PlanItRunTimeException((PlanItException) values[i]);
      }
    }
    return values;
  }

  /**
   * Store output and key values for current link or skim matrix
   * 
   * @param multiKeyPlanItData  multikey data object to store values
   * @param outputProperties    OutputProperty array of result types to be recorded
   * @param outputKeys          OutputProperty array of key types to be recorded
   * @param getValueFromAdapter lambda function to get the required values from an output adapter
   */
  private void updateOutputAndKeyValues(
          MultiKeyPlanItData multiKeyPlanItData,
          OutputProperty[] outputProperties,
          OutputProperty[] outputKeys,
          Function<OutputProperty, Object> getValueFromAdapter){

    Object[] outputValues = getValues(outputProperties, getValueFromAdapter);
    Object[] keyValues = getValues(outputKeys, getValueFromAdapter);
    multiKeyPlanItData.putRow(outputValues, keyValues);
  }

  /**
   * Record output and key values for links
   * 
   * @param multiKeyPlanItData    multikey data object to store values
   * @param outputProperties      OutputProperty array of result types to be recorded
   * @param outputKeys            OutputProperty array of key types to be recorded
   * @param linkSegment           the current link segment
   * @param linkOutputTypeAdapter output adapter to provide methods to get the property values
   * @param mode                  the current mode
   * @param timePeriod            the current time period
   */
  private void updateOutputAndKeyValuesForLink(
      MultiKeyPlanItData multiKeyPlanItData, OutputProperty[] outputProperties, OutputProperty[] outputKeys,
      MacroscopicLinkSegment linkSegment, MacroscopicLinkOutputTypeAdapter linkOutputTypeAdapter, Mode mode, TimePeriod timePeriod){

    Optional<Boolean> flowPositive = linkOutputTypeAdapter.isFlowPositive(linkSegment, mode);
    flowPositive.orElseThrow(() -> new PlanItRunTimeException("unable to determine if flow is positive on link segment"));

    if (flowPositive.get()) {
      updateOutputAndKeyValues(multiKeyPlanItData, outputProperties, outputKeys, (label) -> {
        return linkOutputTypeAdapter.getLinkSegmentOutputPropertyValue(label, linkSegment, mode, timePeriod).get();
      });
    }
  }

  /**
   * Update output and key values for Origin-Destination data
   * 
   * @param multiKeyPlanItData  multikey data object to store values
   * @param outputProperties    OutputProperty array of result types to be recorded
   * @param outputKeys          OutputProperty array of key types to be recorded
   * @param odDataIterator      OdDataIterator to iterate over od values
   * @param odOutputTypeAdapter output adapter to provide methods to get the property values
   * @param mode                the current mode
   * @param timePeriod          the current time period
   */
  private void updateOutputAndKeyValuesForOd(
      MultiKeyPlanItData multiKeyPlanItData,
      OutputProperty[] outputProperties,
      OutputProperty[] outputKeys,
      OdDataIterator<?> odDataIterator, OdOutputTypeAdapter odOutputTypeAdapter,
      Mode mode,
      TimePeriod timePeriod) {

    updateOutputAndKeyValues(
        multiKeyPlanItData, outputProperties, outputKeys,
        (label) -> odOutputTypeAdapter.getOdOutputPropertyValue(label, odDataIterator, mode, timePeriod).get());
  }

  /**
   * Update output and key values for Path matrix
   * 
   * @param multiKeyPlanItData    multikey data object to store values
   * @param outputProperties      OutputProperty array of result types to be recorded
   * @param outputKeys            OutputProperty array of key types to be recorded
   * @param odMultiPathIterator   ODMultiPathIterator to iterate through Ods with one or more paths
   * @param pathOutputTypeAdapter PathOutputTypeAdapter to provide methods to get the property values
   * @param mode                  the current mode
   * @param timePeriod            the current time period
   * @param pathIdType            the type of output being stored in the path list
   */
  private void updateOutputAndKeyValuesForPath(
          MultiKeyPlanItData multiKeyPlanItData,
          OutputProperty[] outputProperties,
          OutputProperty[] outputKeys,
          OdMultiPathIterator<? extends ManagedDirectedPath,? extends Collection<? extends ManagedDirectedPath>> odMultiPathIterator,
          PathOutputTypeAdapter pathOutputTypeAdapter,
          Mode mode,
          TimePeriod timePeriod,
          PathOutputIdentificationType pathIdType) {

    var currOdMultiPaths = odMultiPathIterator.getCurrentValue();
    for(int multiPathIndex = 0; multiPathIndex < currOdMultiPaths.size(); ++multiPathIndex){

      final int pathIndex = multiPathIndex;
      updateOutputAndKeyValues(multiKeyPlanItData, outputProperties, outputKeys,
              (label) -> pathOutputTypeAdapter.getPathOutputPropertyValue(label, odMultiPathIterator, pathIndex, mode, timePeriod, pathIdType).get());
    }
  }

  /**
   * Update output and key values for Simulation data (for a single iteration)
   *
   * @param multiKeyPlanItData    multikey data object to store values
   * @param outputProperties      OutputProperty array of result types to be recorded
   * @param outputKeys            OutputProperty array of key types to be recorded
   * @param simulationOutputTypeAdapter PathOutputTypeAdapter to provide methods to get the property values
   * @param mode                  the current mode
   * @param timePeriod            the current time period
   */
  private void updateOutputAndKeyValuesForSimulation(
      MultiKeyPlanItData multiKeyPlanItData,
      OutputProperty[] outputProperties,
      OutputProperty[] outputKeys,
      SimulationOutputTypeAdapter simulationOutputTypeAdapter,
      Mode mode,
      TimePeriod timePeriod) {

    updateOutputAndKeyValues(
        multiKeyPlanItData, outputProperties, outputKeys,
        (label) -> simulationOutputTypeAdapter.getSimulationOutputPropertyValue(label, mode, timePeriod).get());
  }

  /**
   * Write Simulation results for the current time period to the CSV file
   * 
   * @param outputConfiguration     output configuration
   * @param outputTypeConfiguration OutputTypeConfiguration for current persistence
   * @param currentOutputType,      the active output type of the configuration we are persisting for (can be a suboutputtype)
   * @param outputAdapter           OutputAdapter for current persistence
   * @param modes                   Set of modes of travel
   * @param timePeriod              current time period
   * @param iterationIndex          the iterationIndex we are persisting for
   */
  @Override
  protected void writeSimulationResultsForCurrentTimePeriod(
          OutputConfiguration outputConfiguration,
          OutputTypeConfiguration outputTypeConfiguration,
          OutputTypeEnum currentOutputType,
          OutputAdapter outputAdapter,
          Set<Mode> modes,
          TimePeriod timePeriod,
          int iterationIndex) {
    // for simulation data we assume no sub-output types exist (yet), hence this check to make sure we can
    // cast safely
    PlanItRunTimeException.throwIf(!(currentOutputType instanceof OutputType) && currentOutputType == OutputType.SIMULATION,
        "currentOutputTypeEnum is not compatible with outputTypeConfiguration");

    OutputType outputType = (OutputType) currentOutputType;
    OutputProperty[] outputProperties = outputValueProperties.get(outputType);
    OutputProperty[] outputKeys = outputKeyProperties.get(outputType);
    SimulationOutputTypeAdapter simulationOutputTypeAdapter = (SimulationOutputTypeAdapter) outputAdapter.getOutputTypeAdapter(outputType);

    for (Mode mode : modes) {
      MultiKeyPlanItData multiKeyPlanItData = new MultiKeyPlanItData(outputKeys, outputProperties);
      updateOutputAndKeyValuesForSimulation(multiKeyPlanItData, outputProperties, outputKeys, simulationOutputTypeAdapter, mode, timePeriod);
      timeModeOutputTypeIterationDataMap.put(mode, timePeriod, iterationIndex, outputType, multiKeyPlanItData);
    }
  }

  /**
   * Write General results for the current time period to the CSV file
   * 
   * @param outputConfiguration     output configuration
   * @param outputTypeConfiguration OutputTypeConfiguration for current persistence
   * @param currentOutputType,      the active output type of the configuration we are persisting for (can be a suboutputtype)
   * @param outputAdapter           OutputAdapter for current persistence
   * @param modes                   Set of modes of travel
   * @param timePeriod              current time period
   * @param iterationIndex          the iteration index we are persisting for
   */
  @Override
  protected void writeGeneralResultsForCurrentTimePeriod(OutputConfiguration outputConfiguration, OutputTypeConfiguration outputTypeConfiguration, OutputTypeEnum currentOutputType,
      OutputAdapter outputAdapter, Set<Mode> modes, TimePeriod timePeriod, int iterationIndex){
    //LOGGER.warning("memory Output for OutputType GENERAL has not been implemented yet");
  }

  /**
   * Write link results for the current time period to Map in memory
   * 
   * @param outputConfiguration     outputConfiguration
   * @param outputTypeConfiguration OutputTypeConfiguration for current persistence
   * @param currentOutputType,      the active output type of the configuration we are persisting for (can be a suboutputtype)
   * @param outputAdapter           OutputAdapter for current persistence
   * @param modes                   Set of modes of travel
   * @param timePeriod              current time period
   * @param iterationIndex          the iteration index we are persisting for
   */
  @Override
  protected void writeLinkResultsForCurrentTimePeriod(OutputConfiguration outputConfiguration, OutputTypeConfiguration outputTypeConfiguration, OutputTypeEnum currentOutputType,
      OutputAdapter outputAdapter, Set<Mode> modes, TimePeriod timePeriod, int iterationIndex){
    // for links we assume no sub-output types exist (yet), hence this check to make sure we can
    // cast safely
    PlanItRunTimeException.throwIf(!(currentOutputType instanceof OutputType) && currentOutputType == OutputType.LINK,
        "currentOutputTypeEnum is not compatible with outputTypeconfiguration");

    OutputType outputType = (OutputType) currentOutputType;
    OutputProperty[] outputProperties = outputValueProperties.get(outputType);
    OutputProperty[] outputKeys = outputKeyProperties.get(outputType);
    MacroscopicLinkOutputTypeAdapter linkOutputTypeAdapter = (MacroscopicLinkOutputTypeAdapter) outputAdapter.getOutputTypeAdapter(outputType);

    for (Mode mode : modes) {
      // ensure that if vehicles are used as the output unit rather than pcu, the correct conversion factor is applied, namely
      // the current mode's conversion factor
      VehiclesUnit.updatePcuToVehicleFactor(1 / mode.getPcu());

      MultiKeyPlanItData multiKeyPlanItData = new MultiKeyPlanItData(outputKeys, outputProperties);

      Optional<Long> networkLayerId = linkOutputTypeAdapter.getInfrastructureLayerIdForMode(mode);
      networkLayerId.orElseThrow(() -> new PlanItRunTimeException("unable to determine if layer id for mode"));

      for (MacroscopicLinkSegment linkSegment : linkOutputTypeAdapter.getPhysicalLinkSegments(networkLayerId.get())) {
        Optional<Boolean> flowPositive = linkOutputTypeAdapter.isFlowPositive(linkSegment, mode);
        flowPositive.orElseThrow(() -> new PlanItRunTimeException("unable to determine if flow is positive on link segment"));

        if (outputConfiguration.isPersistZeroFlow() || flowPositive.get()) {
          updateOutputAndKeyValuesForLink(multiKeyPlanItData, outputProperties, outputKeys, linkSegment, linkOutputTypeAdapter, mode, timePeriod);
        }
      }
      timeModeOutputTypeIterationDataMap.put(mode, timePeriod, iterationIndex, outputType, multiKeyPlanItData);
    }
  }

  /**
   * Write Origin-Destination results for the time period to the Map in memory
   * 
   * @param outputConfiguration     outputConfiguration
   * @param outputTypeConfiguration OutputTypeConfiguration for current persistence
   * @param currentOutputType,      the active output type of the configuration we are persisting for (can be a suboutputtype)
   * @param outputAdapter           OutputAdapter for current persistence
   * @param modes                   Set of modes of travel
   * @param timePeriod              current time period
   * @param iterationIndex          the iteration index we are persisting for
   */
  @SuppressWarnings("unchecked")
  @Override
  protected void writeOdResultsForCurrentTimePeriod(OutputConfiguration outputConfiguration, OutputTypeConfiguration outputTypeConfiguration, OutputTypeEnum currentOutputType,
      OutputAdapter outputAdapter, Set<Mode> modes, TimePeriod timePeriod, int iterationIndex){

    // for od data we assume all data is classified into sub output types of type
    // OdSkimSubOutputType, hence this check to make sure we can cast safely
    PlanItRunTimeException.throwIf(!(currentOutputType instanceof SubOutputTypeEnum && currentOutputType instanceof OdSkimSubOutputType),
        "currentOutputTypeEnum is not compatible with outputType configuration");

    // current sub output type
    OdSkimSubOutputType subOutputType = (OdSkimSubOutputType) currentOutputType;
    // top level output type
    OutputType outputType = outputTypeConfiguration.getOutputType();
    final OutputProperty OD_COST_PROPERTY = OutputProperty.of(OutputPropertyType.OD_COST);

    OutputProperty[] outputProperties = outputValueProperties.get(outputType);
    OutputProperty[] outputKeys = outputKeyProperties.get(outputType);
    OdOutputTypeAdapter odOutputTypeAdapter = (OdOutputTypeAdapter) outputAdapter.getOutputTypeAdapter(outputType);
    for (Mode mode : modes) {
      // ensure that if vehicles are used as the output unit rather than pcu, the correct conversion factor is applied, namely
      // the current mode's conversion factor
      VehiclesUnit.updatePcuToVehicleFactor(1 / mode.getPcu());

      MultiKeyPlanItData multiKeyPlanItData = new MultiKeyPlanItData(outputKeys, outputProperties);
      Optional<OdSkimMatrix> odSkimMatrix = odOutputTypeAdapter.getOdSkimMatrix(subOutputType, mode);
      odSkimMatrix.orElseThrow(() -> new PlanItRunTimeException("unable to retrieve od skim matrix"));

      for (OdSkimMatrixIterator odIterator = odSkimMatrix.get().iterator(); odIterator.hasNext();) {
        odIterator.next();
        Optional<Double> cost = (Optional<Double>) odOutputTypeAdapter.getOdOutputPropertyValue(OD_COST_PROPERTY, odIterator, mode, timePeriod);
        cost.orElseThrow(() -> new PlanItRunTimeException("cost could not be retrieved when persisting"));

        if (outputConfiguration.isPersistZeroFlow() || cost.get() > Precision.EPSILON_6) {
          updateOutputAndKeyValuesForOd(multiKeyPlanItData, outputProperties, outputKeys, odIterator, odOutputTypeAdapter, mode, timePeriod);
        }
      }
      timeModeOutputTypeIterationDataMap.put(mode, timePeriod, iterationIndex, outputType, multiKeyPlanItData);
    }
  }

  /**
   * Write Path results for the time period to the CSV file
   * 
   * @param outputConfiguration     outputConfiguration
   * @param outputTypeConfiguration OutputTypeConfiguration for current persistence
   * @param currentOutputType       the output type we are persisting for
   * @param outputAdapter           OutputAdapter for the current persistence
   * @param modes                   Set of modes of travel
   * @param timePeriod              current time period
   */
  @Override
  protected void writePathResultsForCurrentTimePeriod(OutputConfiguration outputConfiguration, OutputTypeConfiguration outputTypeConfiguration, OutputTypeEnum currentOutputType,
      OutputAdapter outputAdapter, Set<Mode> modes, TimePeriod timePeriod, int iterationIndex){

    // for links we assume no sub-output types exist (yet), hence this check to make sure we can
    // cast safely
    PlanItRunTimeException.throwIf(!(currentOutputType instanceof OutputType) && ((OutputType) currentOutputType) == OutputType.PATH,
        "currentOutputTypeEnum is not compatible with outputType configuration");

    OutputType outputType = (OutputType) currentOutputType;
    OutputProperty[] outputProperties = outputValueProperties.get(outputType);
    OutputProperty[] outputKeys = outputKeyProperties.get(outputType);
    PathOutputTypeAdapter pathOutputTypeAdapter = (PathOutputTypeAdapter) outputAdapter.getOutputTypeAdapter(outputType);
    PathOutputTypeConfiguration pathOutputTypeConfiguration = (PathOutputTypeConfiguration) outputTypeConfiguration;
    for (Mode mode : modes) {
      // ensure that if vehicles are used as the output unit rather than pcu, the correct conversion factor is applied, namely
      // the current mode's conversion factor
      VehiclesUnit.updatePcuToVehicleFactor(1 / mode.getPcu());

      MultiKeyPlanItData multiKeyPlanItData = new MultiKeyPlanItData(outputKeys, outputProperties);
      var odPaths = pathOutputTypeAdapter.getOdMultiPaths(mode);
      odPaths.orElseThrow(() -> new PlanItRunTimeException("Od paths could not be retrieved when persisting"));

      for (var odMultiPathIterator = odPaths.get().iterator(); odMultiPathIterator.hasNext();) {
        var odPathsEntry = odMultiPathIterator.next();
        if (outputConfiguration.isPersistZeroFlow() || (odPathsEntry != null && !odPathsEntry.isEmpty())) {
          updateOutputAndKeyValuesForPath(
                  multiKeyPlanItData,
                  outputProperties,
                  outputKeys,
                  odMultiPathIterator,
                  pathOutputTypeAdapter,
                  mode,
                  timePeriod,
              pathOutputTypeConfiguration.getPathIdentificationType());
        }
      }
      timeModeOutputTypeIterationDataMap.put(mode, timePeriod, iterationIndex, outputType, multiKeyPlanItData);
    }
  }

  /**
   * Constructor
   * 
   * @param groupId, contiguous id generation within this group for instances of this class
   */
  public MemoryOutputFormatter(IdGroupingToken groupId) {
    super(groupId);
  }

  /**
   * Get a specified data value
   * 
   * @param mode           value of mode key
   * @param timePeriod     value of time period key
   * @param iterationIndex value of iteration index key
   * @param outputType     value of output type key
   * @param outputProperty output property to identify the column
   * @param keyValues      values of keys to identify the row
   * @return data map for the specified keys
   * @throws PlanItException thrown if there is an error
   */
  public Object getOutputDataValue(Mode mode, TimePeriod timePeriod, Integer iterationIndex, OutputType outputType, OutputPropertyType outputProperty, Object[] keyValues)
      throws PlanItException {
    MultiKeyPlanItData multiKeyPlanItData = timeModeOutputTypeIterationDataMap.get(mode, timePeriod, iterationIndex, outputType);
    return multiKeyPlanItData.getRowValue(outputProperty, keyValues);
  }

  /**
   * Opens all resources used in the formatter
   * 
   * @param outputConfiguration OutputTypeConfiguration for the assignment
   * @param runId               the traffic assignments runId
   * @throws PlanItException thrown if there is an error
   */
  @Override
  public void initialiseBeforeSimulation(OutputConfiguration outputConfiguration, long runId) throws PlanItException {
    timeModeOutputTypeIterationDataMap = new MultiKeyMap<Object, MultiKeyPlanItData>();
  }

  /**
   * Close all resources used in this formatter
   * 
   * @param outputConfiguration OutputConfiguration of the assignment
   * @param outputAdapter       the outputAdapter
   * @param timePeriod the last time period used before simulation ended
   * @param iterationIndex the last iteration index of the last time period used before the simulation eneded
   */
  @Override
  public void finaliseAfterSimulation(
          OutputConfiguration outputConfiguration, OutputAdapter outputAdapter, TimePeriod timePeriod, int iterationIndex){
    // do nothing
  }

  /**
   * Returns the array of output properties representing the output types of the keys for the current output type
   * 
   * @param outputType the current output type
   * @return array of output properties of the keys
   */
  public OutputProperty[] getOutputKeyProperties(OutputType outputType) {
    return outputKeyProperties.get(outputType);
  }

  /**
   * Returns the array of output properties representing the output types of the data values for the current output type
   * 
   * @param outputType the current output type
   * @return array of output properties of the data values for the current output type
   */
  public OutputProperty[] getOutputValueProperties(OutputType outputType) {
    return outputValueProperties.get(outputType);
  }

  /**
   * Returns the value of the last iteration of recorded data
   * 
   * @return the last iteration of recorded data
   */
  public int getLastIteration() {

    Set<MultiKey<?>> keySet = timeModeOutputTypeIterationDataMap.keySet();
    int lastIteration = 0;
    for (MultiKey<?> multiKey : keySet) {
      Object[] keys = multiKey.getKeys();
      Integer iteration = (Integer) keys[2];
      lastIteration = Math.max(lastIteration, iteration);
    }
    return lastIteration;
  }

  /**
   * Returns the value of the last iteration of recorded data for a given time period
   *
   * @param timePeriod to consider
   * @return the last iteration of recorded data for the given time period
   */
  public int getLastIteration(TimePeriod timePeriod) {

    Set<MultiKey<?>> keySet = timeModeOutputTypeIterationDataMap.keySet();
    int lastIteration = 0;
    for (MultiKey<?> multiKey : keySet) {
      Object[] keys = multiKey.getKeys();
      if(keys[TIMEPERIOD_KEY_INDEX].equals(timePeriod)) {
        Integer iteration = (Integer) keys[ITERATION_KEY_INDEX];
        lastIteration = Math.max(lastIteration, iteration);
      }
    }
    return lastIteration;
  }

  /**
   * Flag to indicate whether an implementation can handle multiple iterations
   * 
   * If this returns false, acts as though OutputConfiguration.setPersistOnlyFinalIteration() is set to true
   * 
   * @return flag to indicate whether the OutputFormatter can handle multiple iterations
   */
  @Override
  public boolean canHandleMultipleIterations() {
    return true;
  }

  /**
   * Returns a MemoryOutputIterator for the contents of the specified MultiKeyPlanItData map
   * 
   * @param mode           value of mode key
   * @param timePeriod     value of time period key
   * @param iterationIndex value of iteration index key
   * @param outputType     value of output type key
   * @return map iterator storing the keys and values of this map, null when one or more inputs are invalid
   */
  public MemoryOutputIterator getIterator(final Mode mode, final TimePeriod timePeriod, final Integer iterationIndex, final OutputType outputType) {
    if (mode == null) {
      LOGGER.warning("IGNORE: mode null when obtaining memory output iterator");
      return null;
    }
    if (timePeriod == null) {
      LOGGER.warning("IGNORE: time period null when obtaining memory output iterator");
      return null;
    }
    if (iterationIndex == null) {
      LOGGER.warning("IGNORE: iteration null when obtaining memory output iterator");
      return null;
    }
    if (outputType == null) {
      LOGGER.warning("IGNORE: output type null when obtaining memory output iterator");
      return null;
    }
    MultiKeyPlanItData multiKeyPlanItData = timeModeOutputTypeIterationDataMap.get(mode, timePeriod, iterationIndex, outputType);
    MemoryOutputIterator memoryOutputIterator = new MemoryOutputIterator(multiKeyPlanItData);
    return memoryOutputIterator;
  }

  /**
   * Returns the position of a property type in the output values property array
   * 
   * @param outputType          value of output type key
   * @param outputValueProperty the output value property whose position is required
   * @return the position of the output value property
   */
  public int getPositionOfOutputValueProperty(final OutputType outputType, final OutputPropertyType outputValueProperty){
    Set<MultiKey<?>> keySet = timeModeOutputTypeIterationDataMap.keySet();
    for (MultiKey<?> multiKey : keySet) {
      Object[] keys = multiKey.getKeys();
      Mode mode1 = (Mode) keys[0];
      TimePeriod timePeriod1 = (TimePeriod) keys[TIMEPERIOD_KEY_INDEX];
      Integer iterationIndex1 = (Integer) keys[ITERATION_KEY_INDEX];
      MultiKeyPlanItData multiKeyPlanItData = timeModeOutputTypeIterationDataMap.get(mode1, timePeriod1, iterationIndex1, outputType);
      OutputType outputType1 = (OutputType) keys[OUTPUT_TYPE_KEY_INDEX];
      if (outputType1.equals(outputType)) {
        return multiKeyPlanItData.getPositionOfOutputValueProperty(outputValueProperty);
      }
    }
    throw new PlanItRunTimeException("Value property " + outputType.name() + " could not be found in the MemoryOutputFormatter");
  }

  /**
   * Returns the position of a property type in the output key property array
   * 
   * @param outputType        value of output type key
   * @param outputKeyProperty the output key property whose position is required
   * @return the position of the output key property
   */
  public int getPositionOfOutputKeyProperty(final OutputType outputType, final OutputPropertyType outputKeyProperty){
    Set<MultiKey<?>> keySet = timeModeOutputTypeIterationDataMap.keySet();
    for (MultiKey<?> multiKey : keySet) {
      Object[] keys = multiKey.getKeys();
      Mode mode1 = (Mode) keys[0];
      TimePeriod timePeriod1 = (TimePeriod) keys[TIMEPERIOD_KEY_INDEX];
      Integer iterationIndex1 = (Integer) keys[ITERATION_KEY_INDEX];
      MultiKeyPlanItData multiKeyPlanItData = timeModeOutputTypeIterationDataMap.get(mode1, timePeriod1, iterationIndex1, outputType);
      OutputType outputType1 = (OutputType) keys[OUTPUT_TYPE_KEY_INDEX];
      if (outputType1.equals(outputType)) {
        return multiKeyPlanItData.getPositionOfOutputKeyProperty(outputKeyProperty);
      }
    }
    throw new PlanItRunTimeException("Key property " + outputType.name() + " could not be found in the MemoryOutputFormatter");
  }

}
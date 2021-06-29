package org.planit.output.formatter;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.planit.data.MultiKeyPlanItData;
import org.planit.network.layer.macroscopic.MacroscopicLinkSegmentImpl;
import org.planit.od.odmatrix.ODMatrixIterator;
import org.planit.od.odmatrix.skim.ODSkimMatrix;
import org.planit.od.odpath.ODPathIterator;
import org.planit.od.odpath.ODPathMatrix;
import org.planit.output.adapter.MacroscopicLinkOutputTypeAdapter;
import org.planit.output.adapter.ODOutputTypeAdapter;
import org.planit.output.adapter.OutputAdapter;
import org.planit.output.adapter.PathOutputTypeAdapter;
import org.planit.output.configuration.OutputConfiguration;
import org.planit.output.configuration.OutputTypeConfiguration;
import org.planit.output.configuration.PathOutputTypeConfiguration;
import org.planit.output.enums.ODSkimSubOutputType;
import org.planit.output.enums.OutputType;
import org.planit.output.enums.OutputTypeEnum;
import org.planit.output.enums.PathOutputIdentificationType;
import org.planit.output.enums.SubOutputTypeEnum;
import org.planit.output.property.OutputProperty;
import org.planit.utils.time.TimePeriod;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.math.Precision;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegment;

/**
 * OutputFormatter which stores data in memory, using specified keys and output properties.
 * 
 * @author gman6028
 *
 */
public class MemoryOutputFormatter extends BaseOutputFormatter {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(MacroscopicLinkSegmentImpl.class.getCanonicalName());

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
   * @param labels              OutputProperty array to specify which values are to be returns
   * @param getValueFromAdapter lambda function to find the output value for each label
   * @return array of output values
   * @throws PlanItException thrown if there is an error
   */
  private Object[] getValues(OutputProperty[] labels, Function<OutputProperty, Object> getValueFromAdapter) throws PlanItException {
    Object[] values = new Object[labels.length];
    for (int i = 0; i < labels.length; i++) {
      values[i] = getValueFromAdapter.apply(labels[i]);
      if (values[i] instanceof PlanItException) {
        throw (PlanItException) values[i];
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
   * @throws PlanItException thrown if there is an error
   */
  private void updateOutputAndKeyValues(MultiKeyPlanItData multiKeyPlanItData, OutputProperty[] outputProperties, OutputProperty[] outputKeys,
      Function<OutputProperty, Object> getValueFromAdapter) throws PlanItException {
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
   * @throws PlanItException thrown if there is an error
   */
  private void updateOutputAndKeyValuesForLink(MultiKeyPlanItData multiKeyPlanItData, OutputProperty[] outputProperties, OutputProperty[] outputKeys, MacroscopicLinkSegment linkSegment,
      MacroscopicLinkOutputTypeAdapter<MacroscopicLinkSegment> linkOutputTypeAdapter, Mode mode, TimePeriod timePeriod) throws PlanItException {
    
    Optional<Boolean> flowPositive = linkOutputTypeAdapter.isFlowPositive(linkSegment, mode);
    flowPositive.orElseThrow(() -> new PlanItException("unable to determine if flow is positive on link segment"));    
    
    if (flowPositive.get()) {
      updateOutputAndKeyValues(multiKeyPlanItData, outputProperties, outputKeys, (label) -> {
        return linkOutputTypeAdapter.getLinkSegmentOutputPropertyValue(label, linkSegment, mode, timePeriod, outputTimeUnit.getMultiplier()).get();
      });
    }
  }

  /**
   * Update output and key values for Origin-Destination matrix
   * 
   * @param multiKeyPlanItData  multikey data object to store values
   * @param outputProperties    OutputProperty array of result types to be recorded
   * @param outputKeys          OutputProperty array of key types to be recorded
   * @param odMatrixIterator    ODMatrixIterator to iterate through Skim matrix
   * @param odOutputTypeAdapter output adapter to provide methods to get the property values
   * @param mode                the current mode
   * @param timePeriod          the current time period
   * @throws PlanItException thrown if there is an error
   */
  private void updateOutputAndKeyValuesForOD(MultiKeyPlanItData multiKeyPlanItData, OutputProperty[] outputProperties, OutputProperty[] outputKeys,
      ODMatrixIterator odMatrixIterator, ODOutputTypeAdapter odOutputTypeAdapter, Mode mode, TimePeriod timePeriod) throws PlanItException {
    updateOutputAndKeyValues(multiKeyPlanItData, outputProperties, outputKeys, (label) -> {
      return odOutputTypeAdapter.getODOutputPropertyValue(label, odMatrixIterator, mode, timePeriod, outputTimeUnit.getMultiplier()).get();
    });
  }

  /**
   * Update output and key values for Path matrix
   * 
   * @param multiKeyPlanItData    multikey data object to store values
   * @param outputProperties      OutputProperty array of result types to be recorded
   * @param outputKeys            OutputProperty array of key types to be recorded
   * @param odPathIterator        ODPathIterator to iterate through matrix of paths
   * @param pathOutputTypeAdapter PathOutputTypeAdapter to provide methods to get the property values
   * @param mode                  the current mode
   * @param timePeriod            the current time period
   * @param pathIdType            the type of output being stored in the path list
   * @throws PlanItException thrown if there is an error
   */

  private void updateOutputAndKeyValuesForPath(MultiKeyPlanItData multiKeyPlanItData, OutputProperty[] outputProperties, OutputProperty[] outputKeys, ODPathIterator odPathIterator,
      PathOutputTypeAdapter pathOutputTypeAdapter, Mode mode, TimePeriod timePeriod, PathOutputIdentificationType pathIdType) throws PlanItException {
    updateOutputAndKeyValues(multiKeyPlanItData, outputProperties, outputKeys, (label) -> {
      return pathOutputTypeAdapter.getPathOutputPropertyValue(label, odPathIterator, mode, timePeriod, pathIdType).get();
    });
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
   * @throws PlanItException thrown if there is an error
   */
  @Override
  protected void writeSimulationResultsForCurrentTimePeriod(OutputConfiguration outputConfiguration, OutputTypeConfiguration outputTypeConfiguration,
      OutputTypeEnum currentOutputType, OutputAdapter outputAdapter, Set<Mode> modes, TimePeriod timePeriod, int iterationIndex) throws PlanItException {
    LOGGER.warning("memory Output for OutputType SIMULATION has not been implemented yet");
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
   * @throws PlanItException thrown if there is an error
   */
  @Override
  protected void writeGeneralResultsForCurrentTimePeriod(OutputConfiguration outputConfiguration, OutputTypeConfiguration outputTypeConfiguration, OutputTypeEnum currentOutputType,
      OutputAdapter outputAdapter, Set<Mode> modes, TimePeriod timePeriod, int iterationIndex) throws PlanItException {
    LOGGER.warning("memory Output for OutputType GENERAL has not been implemented yet");
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
   * @throws PlanItException thrown if there is an error
   */
  @SuppressWarnings("unchecked")
  @Override
  protected void writeLinkResultsForCurrentTimePeriod(OutputConfiguration outputConfiguration, OutputTypeConfiguration outputTypeConfiguration, OutputTypeEnum currentOutputType,
      OutputAdapter outputAdapter, Set<Mode> modes, TimePeriod timePeriod, int iterationIndex) throws PlanItException {
    // for links we assume no sub-output types exist (yet), hence this check to make sure we can
    // cast safely
    PlanItException.throwIf(!(currentOutputType instanceof OutputType) && ((OutputType) currentOutputType) == OutputType.LINK,
        "currentOutputTypeEnum is not compatible with outputTypeconfiguration");

    OutputType outputType = (OutputType) currentOutputType;
    OutputProperty[] outputProperties = outputValueProperties.get(outputType);
    OutputProperty[] outputKeys = outputKeyProperties.get(outputType);
    MacroscopicLinkOutputTypeAdapter<MacroscopicLinkSegment> linkOutputTypeAdapter = 
        (MacroscopicLinkOutputTypeAdapter<MacroscopicLinkSegment>) outputAdapter.getOutputTypeAdapter(outputType);
    
    for (Mode mode : modes) {
      MultiKeyPlanItData multiKeyPlanItData = new MultiKeyPlanItData(outputKeys, outputProperties);

      Optional<Long> networkLayerId = linkOutputTypeAdapter.getInfrastructureLayerIdForMode(mode);
      networkLayerId.orElseThrow(() -> new PlanItException("unable to determine if layer id for mode"));
      
      for (MacroscopicLinkSegment linkSegment : linkOutputTypeAdapter.getPhysicalLinkSegments(networkLayerId.get())) {
        Optional<Boolean> flowPositive = linkOutputTypeAdapter.isFlowPositive(linkSegment, mode);
        flowPositive.orElseThrow(() -> new PlanItException("unable to determine if flow is positive on link segment"));
        
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
   * @throws PlanItException thrown if there is an error
   */
  @SuppressWarnings("unchecked")
  @Override
  protected void writeOdResultsForCurrentTimePeriod(OutputConfiguration outputConfiguration, OutputTypeConfiguration outputTypeConfiguration, OutputTypeEnum currentOutputType,
      OutputAdapter outputAdapter, Set<Mode> modes, TimePeriod timePeriod, int iterationIndex) throws PlanItException {

    // for od data we assume all data is classified into sub output types of type
    // ODSkimSubOutputType, hence this check to make sure we can cast safely
    PlanItException.throwIf(!(currentOutputType instanceof SubOutputTypeEnum && ((SubOutputTypeEnum) currentOutputType) instanceof ODSkimSubOutputType),
        "currentOutputTypeEnum is not compatible with outputTypeconfiguration");

    // current sub output type
    ODSkimSubOutputType subOutputType = (ODSkimSubOutputType) currentOutputType;
    // top level output type
    OutputType outputType = outputTypeConfiguration.getOutputType();

    OutputProperty[] outputProperties = outputValueProperties.get(outputType);
    OutputProperty[] outputKeys = outputKeyProperties.get(outputType);
    ODOutputTypeAdapter odOutputTypeAdapter = (ODOutputTypeAdapter) outputAdapter.getOutputTypeAdapter(outputType);
    for (Mode mode : modes) {
      MultiKeyPlanItData multiKeyPlanItData = new MultiKeyPlanItData(outputKeys, outputProperties);
      Optional<ODSkimMatrix> odSkimMatrix = odOutputTypeAdapter.getODSkimMatrix(subOutputType, mode);
      odSkimMatrix.orElseThrow(() -> new PlanItException("unable to retrieve od skim matrix"));
      
      for (ODMatrixIterator odMatrixIterator = odSkimMatrix.get().iterator(); odMatrixIterator.hasNext();) {
        odMatrixIterator.next();
        Optional<Double> cost = (Optional<Double>) odOutputTypeAdapter.getODOutputPropertyValue(OutputProperty.OD_COST, odMatrixIterator, mode, timePeriod, outputTimeUnit.getMultiplier());
        cost.orElseThrow(() ->new PlanItException("cost could not be retrieved when persisting"));        
        
        if (outputConfiguration.isPersistZeroFlow() || cost.get() > Precision.EPSILON_6) {
          updateOutputAndKeyValuesForOD(multiKeyPlanItData, outputProperties, outputKeys, odMatrixIterator, odOutputTypeAdapter, mode, timePeriod);
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
   * @throws PlanItException thrown if there is an error
   */
  @Override
  protected void writePathResultsForCurrentTimePeriod(OutputConfiguration outputConfiguration, OutputTypeConfiguration outputTypeConfiguration, OutputTypeEnum currentOutputType,
      OutputAdapter outputAdapter, Set<Mode> modes, TimePeriod timePeriod, int iterationIndex) throws PlanItException {

    // for links we assume no sub-output types exist (yet), hence this check to make sure we can
    // cast safely
    PlanItException.throwIf(!(currentOutputType instanceof OutputType) && ((OutputType) currentOutputType) == OutputType.PATH,
        "currentOutputTypeEnum is not compatible with outputTypeconfiguration");

    OutputType outputType = (OutputType) currentOutputType;
    OutputProperty[] outputProperties = outputValueProperties.get(outputType);
    OutputProperty[] outputKeys = outputKeyProperties.get(outputType);
    PathOutputTypeAdapter pathOutputTypeAdapter = (PathOutputTypeAdapter) outputAdapter.getOutputTypeAdapter(outputType);
    PathOutputTypeConfiguration pathOutputTypeConfiguration = (PathOutputTypeConfiguration) outputTypeConfiguration;
    for (Mode mode : modes) {
      MultiKeyPlanItData multiKeyPlanItData = new MultiKeyPlanItData(outputKeys, outputProperties);
      Optional<ODPathMatrix> odPathMatrix = pathOutputTypeAdapter.getODPathMatrix(mode);
      odPathMatrix.orElseThrow(() -> new PlanItException("od path matrix could not be retrieved when persisting"));
      
      for (ODPathIterator odPathIterator = odPathMatrix.get().iterator(); odPathIterator.hasNext();) {
        odPathIterator.next();
        if (outputConfiguration.isPersistZeroFlow() || (odPathIterator.getCurrentValue() != null)) {
          updateOutputAndKeyValuesForPath(multiKeyPlanItData, outputProperties, outputKeys, odPathIterator, pathOutputTypeAdapter, mode, timePeriod,
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
  public Object getOutputDataValue(Mode mode, TimePeriod timePeriod, Integer iterationIndex, OutputType outputType, OutputProperty outputProperty, Object[] keyValues)
      throws PlanItException {
    MultiKeyPlanItData multiKeyPlanItData = (MultiKeyPlanItData) timeModeOutputTypeIterationDataMap.get(mode, timePeriod, iterationIndex, outputType);
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
   * @throws PlanItException thrown if there is an error
   */
  @Override
  public void finaliseAfterSimulation(OutputConfiguration outputConfiguration, OutputAdapter outputAdapter) throws PlanItException {
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

    Set<MultiKey<? extends Object>> keySet = (Set<MultiKey<? extends Object>>) timeModeOutputTypeIterationDataMap.keySet();
    int lastIteration = 0;
    for (MultiKey<? extends Object> multiKey : keySet) {
      Object[] keys = multiKey.getKeys();
      Integer iteration = (Integer) keys[2];
      lastIteration = Math.max(lastIteration, iteration);
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
   * @return map iterator storing the keys and values of this map
   */
  public MemoryOutputIterator getIterator(final Mode mode, final TimePeriod timePeriod, final Integer iterationIndex, final OutputType outputType) {
    MultiKeyPlanItData multiKeyPlanItData = (MultiKeyPlanItData) timeModeOutputTypeIterationDataMap.get(mode, timePeriod, iterationIndex, outputType);
    MemoryOutputIterator memoryOutputIterator = new MemoryOutputIterator(multiKeyPlanItData);
    return memoryOutputIterator;
  }

  /**
   * Returns the position of a property type in the output values property array
   * 
   * @param outputType          value of output type key
   * @param outputValueProperty the output value property whose position is required
   * @return the position of the output value property
   * @throws PlanItException thrown if the output property type is not in the output value property array
   */
  public int getPositionOfOutputValueProperty(final OutputType outputType, final OutputProperty outputValueProperty) throws PlanItException {
    Set<MultiKey<? extends Object>> keySet = (Set<MultiKey<? extends Object>>) timeModeOutputTypeIterationDataMap.keySet();
    for (MultiKey<? extends Object> multiKey : keySet) {
      Object[] keys = multiKey.getKeys();
      Mode mode1 = (Mode) keys[0];
      TimePeriod timePeriod1 = (TimePeriod) keys[1];
      Integer iterationIndex1 = (Integer) keys[2];
      MultiKeyPlanItData multiKeyPlanItData = (MultiKeyPlanItData) timeModeOutputTypeIterationDataMap.get(mode1, timePeriod1, iterationIndex1, outputType);
      OutputType outputType1 = (OutputType) keys[3];
      if (outputType1.equals(outputType)) {
        return multiKeyPlanItData.getPositionOfOutputValueProperty(outputValueProperty);
      }
    }
    throw new PlanItException("Value property " + outputType.name() + " could not be found in the MemoryOutputFormatter");
  }

  /**
   * Returns the position of a property type in the output key property array
   * 
   * @param outputType        value of output type key
   * @param outputKeyProperty the output key property whose position is required
   * @return the position of the output key property
   * @throws PlanItException thrown if the output property type is not in the output key property array
   */
  public int getPositionOfOutputKeyProperty(final OutputType outputType, final OutputProperty outputKeyProperty) throws PlanItException {
    Set<MultiKey<? extends Object>> keySet = (Set<MultiKey<? extends Object>>) timeModeOutputTypeIterationDataMap.keySet();
    for (MultiKey<? extends Object> multiKey : keySet) {
      Object[] keys = multiKey.getKeys();
      Mode mode1 = (Mode) keys[0];
      TimePeriod timePeriod1 = (TimePeriod) keys[1];
      Integer iterationIndex1 = (Integer) keys[2];
      MultiKeyPlanItData multiKeyPlanItData = (MultiKeyPlanItData) timeModeOutputTypeIterationDataMap.get(mode1, timePeriod1, iterationIndex1, outputType);
      OutputType outputType1 = (OutputType) keys[3];
      if (outputType1.equals(outputType)) {
        return multiKeyPlanItData.getPositionOfOutputKeyProperty(outputKeyProperty);
      }
    }
    throw new PlanItException("Key property " + outputType.name() + " could not be found in the MemoryOutputFormatter");
  }

}
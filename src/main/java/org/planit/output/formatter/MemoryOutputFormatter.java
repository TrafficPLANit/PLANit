package org.planit.output.formatter;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.collections.map.MultiKeyMap;
import org.planit.data.MultiKeyPlanItData;
import org.planit.exceptions.PlanItException;
import org.planit.logging.PlanItLogger;
import org.planit.od.odmatrix.ODMatrixIterator;
import org.planit.od.odmatrix.skim.ODSkimMatrix;
import org.planit.od.odroute.ODRouteIterator;
import org.planit.od.odroute.ODRouteMatrix;
import org.planit.output.adapter.LinkOutputTypeAdapter;
import org.planit.output.adapter.ODOutputTypeAdapter;
import org.planit.output.adapter.RouteOutputTypeAdapter;
import org.planit.output.adapter.OutputAdapter;
import org.planit.output.configuration.OutputTypeConfiguration;
import org.planit.output.configuration.PathOutputTypeConfiguration;
import org.planit.output.enums.ODSkimSubOutputType;
import org.planit.output.enums.OutputType;
import org.planit.output.enums.OutputTypeEnum;
import org.planit.output.enums.RoutIdType;
import org.planit.output.enums.SubOutputTypeEnum;
import org.planit.output.property.OutputProperty;
import org.planit.time.TimePeriod;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Mode;

/**
 * OutputFormatter which stores data in memory, using specified keys and output properties.
 * 
 * @author gman6028
 *
 */
public class MemoryOutputFormatter extends BaseOutputFormatter {

	/**
	 * MultiKeyMap of data stores
	 */
	private MultiKeyMap timeModeOutputTypeIterationDataMap;

	/**
	 * Returns an array of values (key or values)
	 * 
	 * @param labels              OutputProperty array to specify which values are
	 *                            to be returns
	 * @param getValueFromAdapter lambda function to find the output value for each
	 *                            label
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
	 * @param outputProperties    OutputProperty array of result types to be
	 *                            recorded
	 * @param outputKeys          OutputProperty array of key types to be recorded
	 * @param getValueFromAdapter lambda function to get the required values from an
	 *                            output adapter
	 * @throws PlanItException thrown if there is an error
	 */
	private void updateOutputAndKeyValues(MultiKeyPlanItData multiKeyPlanItData, OutputProperty[] outputProperties,
			OutputProperty[] outputKeys, Function<OutputProperty, Object> getValueFromAdapter) throws PlanItException {
		Object[] outputValues = getValues(outputProperties, getValueFromAdapter);
		Object[] keyValues = getValues(outputKeys, getValueFromAdapter);
		multiKeyPlanItData.putRow(outputValues, keyValues);
	}

	/**
	 * Record output and key values for links
	 * 
	 * @param multiKeyPlanItData                           multikey data object to
	 *                                                     store values
	 * @param outputProperties                             OutputProperty array of
	 *                                                     result types to be
	 *                                                     recorded
	 * @param outputKeys                                   OutputProperty array of
	 *                                                     key types to be recorded
	 * @param linkSegment                                  the current link segment
	 * @param linkOutputTypeAdapter  output adapter to provide methods to get the property values
	 * @param mode                                         the current mode
	 * @param timePeriod                                   the current time period
	 * @param recordLinksWithZeroFlow true if links with zero flow are to be recorded
	 * @throws PlanItException thrown if there is an error
	 */
	private void updateOutputAndKeyValuesForLink(MultiKeyPlanItData multiKeyPlanItData,OutputProperty[] outputProperties, OutputProperty[] outputKeys, LinkSegment linkSegment,
			LinkOutputTypeAdapter linkOutputTypeAdapter, Mode mode, TimePeriod timePeriod, boolean recordLinksWithZeroFlow) throws PlanItException {
		if (recordLinksWithZeroFlow || linkOutputTypeAdapter.isFlowPositive(linkSegment, mode)) {
			updateOutputAndKeyValues(multiKeyPlanItData, outputProperties, outputKeys, (label) -> {
 				return linkOutputTypeAdapter.getLinkOutputPropertyValue(label, linkSegment, mode, timePeriod, outputTimeUnit.getMultiplier());
			});
		}
	}

	/**
	 * Update output and key values for Origin-Destination matrix
	 * 
	 * @param multiKeyPlanItData  multikey data object to store values
	 * @param outputProperties      OutputProperty array of result types to be recorded
	 * @param outputKeys               OutputProperty array of key  types to be recorded
	 * @param odMatrixIterator     ODMatrixIterator to iterate through Skim matrix
	 * @param odOutputTypeAdapter output adapter to provide methods to get the property values
	 * @param mode                                       the current mode
	 * @param timePeriod                                 the current time period
	 * @throws PlanItException thrown if there is an error
	 */
	private void updateOutputAndKeyValuesForOD(MultiKeyPlanItData multiKeyPlanItData, OutputProperty[] outputProperties,
			OutputProperty[] outputKeys, ODMatrixIterator odMatrixIterator,
			ODOutputTypeAdapter odOutputTypeAdapter, Mode mode, TimePeriod timePeriod) throws PlanItException {
		odMatrixIterator.next();
		updateOutputAndKeyValues(multiKeyPlanItData, outputProperties, outputKeys, (label) -> {
			return odOutputTypeAdapter.getODOutputPropertyValue(label, odMatrixIterator, mode, timePeriod, outputTimeUnit.getMultiplier());
		});
	}

	/**
	 * Update output and key values for Path matrix
	 * 
	 * @param multiKeyPlanItData  multikey data object to store values
	 * @param outputProperties      OutputProperty array of result types to be recorded
	 * @param outputKeys               OutputProperty array of key  types to be recorded
	 * @param odPathIterator         ODPathIterator to iterate through matrix of paths
	 * @param pathOutputTypeAdapter PathOutputTypeAdapter to provide methods to get the property values
	 * @param mode                                       the current mode
	 * @param timePeriod                                 the current time period
	 * @param pathIdType  the type of output being stored in the path list
	 * @throws PlanItException thrown if there is an error
	 */
	
	private void updateOutputAndKeyValuesForPath(MultiKeyPlanItData multiKeyPlanItData, OutputProperty[] outputProperties, OutputProperty[] outputKeys, ODRouteIterator odPathIterator,
			RouteOutputTypeAdapter pathOutputTypeAdapter, Mode mode, TimePeriod timePeriod, RoutIdType pathIdType) throws PlanItException {
		odPathIterator.next();
		updateOutputAndKeyValues(multiKeyPlanItData, outputProperties, outputKeys, (label) -> {
			return pathOutputTypeAdapter.getRouteOutputPropertyValue(label, odPathIterator, mode, timePeriod, pathIdType);
		});
	}
	
	/**
	 * Write Simulation results for the current time period to the CSV file
	 * 
     * @param outputTypeConfiguration OutputTypeConfiguration for current  persistence
     * @param currentOutputType, the active output type of the configuration we are persisting for (can be a suboutputtype)
     * @param outputAdapter OutputAdapter for current persistence
     * @param modes                   Set of modes of travel
     * @param timePeriod              current time period
     * @param iterationIndex the iterationIndex we are persisting for
     * @throws PlanItException thrown if there is an error
	 */
	@Override
	protected void writeSimulationResultsForCurrentTimePeriod(
            OutputTypeConfiguration outputTypeConfiguration, OutputTypeEnum currentOutputType, OutputAdapter outputAdapter, Set<Mode> modes, TimePeriod timePeriod, int iterationIndex) throws PlanItException {
		PlanItLogger.info("Memory Output for OutputType SIMULATION has not been implemented yet.");
	}

	/**
	 * Write General results for the current time period to the CSV file
	 * 
     * @param outputTypeConfiguration OutputTypeConfiguration for current  persistence
     * @param currentOutputType, the active output type of the configuration we are persisting for (can be a suboutputtype)
     * @param outputAdapter OutputAdapter for current persistence
     * @param modes                   Set of modes of travel
     * @param timePeriod              current time period
     * @param iterationIndex		the iteration index we are persisting for
     * @throws PlanItException thrown if there is an error
	 */
	@Override
	protected void writeGeneralResultsForCurrentTimePeriod(
            OutputTypeConfiguration outputTypeConfiguration, OutputTypeEnum currentOutputType, OutputAdapter outputAdapter, Set<Mode> modes, TimePeriod timePeriod, int iterationIndex) throws PlanItException {
		PlanItLogger.info("Memory Output for OutputType GENERAL has not been implemented yet.");
	}

	/**
	 * Write link results for the current time period to Map in memory
	 * 
     * @param outputTypeConfiguration OutputTypeConfiguration for current  persistence
     * @param currentOutputType, the active output type of the configuration we are persisting for (can be a suboutputtype)
     * @param outputAdapter OutputAdapter for current persistence
     * @param modes                   Set of modes of travel
     * @param timePeriod              current time period
     * @param iterationIndex	the iteration index we are persisting for
     * @throws PlanItException thrown if there is an error
	 */
	@Override
	protected void writeLinkResultsForCurrentTimePeriod(
            OutputTypeConfiguration outputTypeConfiguration, OutputTypeEnum currentOutputType, OutputAdapter outputAdapter, Set<Mode> modes, TimePeriod timePeriod, int iterationIndex) throws PlanItException {
	    if(!(currentOutputType instanceof OutputType) && ((OutputType)currentOutputType)==OutputType.LINK)
	    {  // for links we assume no sub-output types exist (yet), hence this check to make sure we can cast safely 
	        throw new PlanItException("currentOutputTypeEnum is not compatible with outputTypeconfiguration");
	    }
	    OutputType outputType = (OutputType)currentOutputType;
		OutputProperty[] outputProperties = outputValueProperties.get(outputType);
		OutputProperty[] outputKeys = outputKeyProperties.get(outputType);
		LinkOutputTypeAdapter linkOutputTypeAdapter = (LinkOutputTypeAdapter) outputAdapter.getOutputTypeAdapter(outputType);
		for (Mode mode : modes) {
			MultiKeyPlanItData multiKeyPlanItData = new MultiKeyPlanItData(outputKeys, outputProperties);
			for (LinkSegment linkSegment : linkOutputTypeAdapter.getLinkSegments()) {
				updateOutputAndKeyValuesForLink(multiKeyPlanItData, outputProperties, outputKeys, linkSegment, linkOutputTypeAdapter, mode, timePeriod, outputTypeConfiguration.isRecordLinksWithZeroFlow());
			}
			timeModeOutputTypeIterationDataMap.put(mode, timePeriod, iterationIndex, outputType, multiKeyPlanItData);
		}
	}
	
	/**
	 * Write Origin-Destination results for the time period to the Map in memory
	 * 
     * @param outputTypeConfiguration OutputTypeConfiguration for current  persistence
     * @param currentOutputType, the active output type of the configuration we are persisting for (can be a suboutputtype)
     * @param outputAdapter OutputAdapter for current persistence
     * @param modes                   Set of modes of travel
     * @param timePeriod              current time period
     * @param iterationIndex 		  the iteration index we are persisting for
     * @throws PlanItException thrown if there is an error
	 */
	@Override
	protected void writeOdResultsForCurrentTimePeriod(
	        OutputTypeConfiguration outputTypeConfiguration, OutputTypeEnum currentOutputType, OutputAdapter outputAdapter, Set<Mode> modes, TimePeriod timePeriod, int iterationIndex) throws PlanItException {
        if(!(currentOutputType instanceof SubOutputTypeEnum && ((SubOutputTypeEnum)currentOutputType) instanceof ODSkimSubOutputType))
        {  // for od data we assume all data is classified into sub output types of type ODSkimSubOutputType, hence this check to make sure we can cast safely 
            throw new PlanItException("currentOutputTypeEnum is not compatible with outputTypeconfiguration");
        }	    
        //current sub output type
        ODSkimSubOutputType subOutputType = (ODSkimSubOutputType) currentOutputType;
        // top level output type
        OutputType outputType = outputTypeConfiguration.getOutputType();
        
		OutputProperty[] outputProperties = outputValueProperties.get(outputType);
		OutputProperty[] outputKeys = outputKeyProperties.get(outputType);
		ODOutputTypeAdapter odOutputTypeAdapter = (ODOutputTypeAdapter) outputAdapter.getOutputTypeAdapter(outputType);
		for (Mode mode : modes) {
			MultiKeyPlanItData multiKeyPlanItData = new MultiKeyPlanItData(outputKeys, outputProperties);
			ODSkimMatrix odSkimMatrix = odOutputTypeAdapter.getODSkimMatrix(subOutputType, mode);
			for (ODMatrixIterator odMatrixIterator = odSkimMatrix.iterator(); odMatrixIterator.hasNext();) {
				updateOutputAndKeyValuesForOD(multiKeyPlanItData, outputProperties, outputKeys, odMatrixIterator, odOutputTypeAdapter, mode, timePeriod);
			}
			timeModeOutputTypeIterationDataMap.put(mode, timePeriod, iterationIndex, outputType,	multiKeyPlanItData);
		}
	}
	
	/**
	 * Write Path results for the time period to the CSV file
	 * 
	 * @param outputTypeConfiguration OutputTypeConfiguration for current persistence
	 * @param currentOutputType the output type we are persisting for
	 * @param outputAdapter OutputAdapter for the current persistence
	 * @param modes                   Set of modes of travel
	 * @param timePeriod              current time period
	 * @throws PlanItException thrown if there is an error
	 */
	@Override
	protected void writePathResultsForCurrentTimePeriod(
            OutputTypeConfiguration outputTypeConfiguration, OutputTypeEnum currentOutputType, OutputAdapter outputAdapter, Set<Mode> modes, TimePeriod timePeriod, int iterationIndex) throws PlanItException {
        if(!(currentOutputType instanceof OutputType) && ((OutputType)currentOutputType)==OutputType.PATH)
        {  // for links we assume no sub-output types exist (yet), hence this check to make sure we can cast safely 
            throw new PlanItException("currentOutputTypeEnum is not compatible with outputTypeconfiguration");
        }
        OutputType outputType = (OutputType)currentOutputType;	    
		OutputProperty[] outputProperties = outputValueProperties.get(outputType);
		OutputProperty[] outputKeys = outputKeyProperties.get(outputType);
		RouteOutputTypeAdapter pathOutputTypeAdapter = (RouteOutputTypeAdapter) outputAdapter.getOutputTypeAdapter(outputType);
		PathOutputTypeConfiguration pathOutputTypeConfiguration = (PathOutputTypeConfiguration) outputTypeConfiguration;
		for (Mode mode : modes) {
			MultiKeyPlanItData multiKeyPlanItData = new MultiKeyPlanItData(outputKeys, outputProperties);
			ODRouteMatrix odPathMatrix = pathOutputTypeAdapter.getODPathMatrix(mode);
			for (ODRouteIterator odPathIterator = odPathMatrix.iterator(); odPathIterator.hasNext();) {
				updateOutputAndKeyValuesForPath(multiKeyPlanItData, outputProperties, outputKeys, odPathIterator, pathOutputTypeAdapter, mode, timePeriod, pathOutputTypeConfiguration.getPathIdType());
			}
			timeModeOutputTypeIterationDataMap.put(mode, timePeriod, iterationIndex, outputType,	multiKeyPlanItData);
		}
	}

	/**
	 * Constructor
	 */
	public MemoryOutputFormatter() {
		super();
	}

	/**
	 * Get a specified data value
	 * 
	 * @param mode           value of mode key
	 * @param timePeriod     value of time period key
	 * @param iterationIndex value of iteration index key
	 * @param outputType     value of output type key
	 * @param outputProperty output property to identify the column
	 * @param keyValues values of keys to identify the row
	 * @return data map for the specified keys
	 * @throws PlanItException thrown if there is an error
	 */
	public Object getOutputDataValue(Mode mode, TimePeriod timePeriod, Integer iterationIndex, OutputType outputType, OutputProperty outputProperty,  Object[] keyValues) throws PlanItException {
		return ((MultiKeyPlanItData)  timeModeOutputTypeIterationDataMap.get(mode, timePeriod, iterationIndex, outputType)).getRowValue(outputProperty, keyValues);
	}

	/**
	 * Opens all resources used in the formatter
	 * 
	 * @param outputTypeConfigurations OutputTypeConfigurations for the assignment that have been activated
	 * @param runId the traffic assignments runId
	 * @throws PlanItException thrown if there is an error
	 */
	@Override
	public void initialiseBeforeSimulation(Map<OutputType, OutputTypeConfiguration> outputTypeConfigurations, long runId) throws PlanItException {
		timeModeOutputTypeIterationDataMap = MultiKeyMap.decorate(new HashedMap());
	}

	/**
	 * Close all resources used in this formatter
	 * 
	 * @param outputTypeConfigurations OutputTypeConfigurations for the assignment that have been activated
	 * @throws PlanItException thrown if there is an error
	 */
	@Override
	public void finaliseAfterSimulation(Map<OutputType, OutputTypeConfiguration> outputTypeConfigurations) throws PlanItException {    
	}

	/**
	 * Returns the array of output properties representing the output types of the
	 * keys for the current output type
	 * 
	 * @param outputType the current output type
	 * @return array of output properties of the keys
	 */
	public OutputProperty[] getOutputKeyProperties(OutputType outputType) {
		return outputKeyProperties.get(outputType);
	}

	/**
	 * Returns the array of output properties representing the output types of the
	 * data values for the current output type
	 * 
	 * @param outputType the current output type
	 * @return array of output properties of the data values for the current output
	 *         type
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
		Set<MultiKey> keySet = (Set<MultiKey>) timeModeOutputTypeIterationDataMap.keySet();
		int lastIteration = 0;
		for (MultiKey multiKey : keySet) {
			Object[] keys = multiKey.getKeys();
			Integer iteration = (Integer) keys[2];
			lastIteration = Math.max(lastIteration, iteration);
		}
		return lastIteration;
	}

	/**
	 * Flag to indicate whether an implementation can handle multiple iterations
	 * 
	 * If this returns false, acts as though
	 * OutputConfiguration.setPersistOnlyFinalIteration() is set to true
	 * 
	 * @return flag to indicate whether the OutputFormatter can handle multiple
	 *         iterations
	 */
	@Override
	public boolean canHandleMultipleIterations() {
		return true;
	}

}
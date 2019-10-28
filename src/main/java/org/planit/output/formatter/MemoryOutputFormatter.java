package org.planit.output.formatter;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.collections.map.MultiKeyMap;
import org.planit.data.MultiKeyPlanItData;
import org.planit.data.TraditionalStaticAssignmentSimulationData;
import org.planit.exceptions.PlanItException;
import org.planit.logging.PlanItLogger;
import org.planit.network.physical.LinkSegment;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.network.transport.TransportNetwork;
import org.planit.od.odmatrix.ODMatrixIterator;
import org.planit.output.adapter.TraditionalStaticAssignmentLinkOutputAdapter;
import org.planit.output.adapter.TraditionalStaticAssignmentODOutputAdapter;
import org.planit.output.configuration.OutputTypeConfiguration;
import org.planit.output.enums.ODSkimOutputType;
import org.planit.output.enums.OutputType;
import org.planit.output.property.OutputProperty;
import org.planit.time.TimePeriod;
import org.planit.userclass.Mode;
import org.planit.utils.TriConsumer;

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
	 */
	private Object[] getValues(OutputProperty[] labels, Function<OutputProperty, Object> getValueFromAdapter) {
		Object[] values = new Object[labels.length];
		for (int i = 0; i < labels.length; i++) {
			values[i] = getValueFromAdapter.apply(labels[i]);
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
	 * @param iterator                                     iterator to iterator
	 *                                                     through link segments
	 * @param traditionalStaticAssignmentLinkOutputAdapter output adapter to provide
	 *                                                     methods to get the
	 *                                                     property values
	 * @param mode                                         the current mode
	 * @param timePeriod                                   the current time period
	 * @throws PlanItException thrown if there is an error
	 */
	private void updateOutputAndKeyValuesForLinks(MultiKeyPlanItData multiKeyPlanItData,
			OutputProperty[] outputProperties, OutputProperty[] outputKeys, Iterator<LinkSegment> iterator,
			TraditionalStaticAssignmentLinkOutputAdapter traditionalStaticAssignmentLinkOutputAdapter, Mode mode,
			TimePeriod timePeriod) throws PlanItException {
		MacroscopicLinkSegment macroscopicLinkSegment = (MacroscopicLinkSegment) iterator.next();
		if (traditionalStaticAssignmentLinkOutputAdapter.isFlowPositive(macroscopicLinkSegment, mode)) {
			updateOutputAndKeyValues(multiKeyPlanItData, outputProperties, outputKeys, (label) -> {
				return traditionalStaticAssignmentLinkOutputAdapter.getLinkPropertyValue(label, macroscopicLinkSegment,
						mode, timePeriod, outputTimeUnit.getMultiplier());
			});
		}
	}

	/**
	 * Record output and key values for Origin-Destination matrix
	 * 
	 * @param multiKeyPlanItData                         multikey data object to
	 *                                                   store values
	 * @param outputProperties                           OutputProperty array of
	 *                                                   result types to be recorded
	 * @param outputKeys                                 OutputProperty array of key
	 *                                                   types to be recorded
	 * @param odMatrixIterator                           ODMatrixIterator to iterate
	 *                                                   through Skim matrix
	 * @param traditionalStaticAssignmentODOutputAdapter output adapter to provide
	 *                                                   methods to get the property
	 *                                                   values
	 * @param mode                                       the current mode
	 * @param timePeriod                                 the current time period
	 * @throws PlanItException thrown if there is an error
	 */
	private void updateOutputAndKeyValuesForOD(MultiKeyPlanItData multiKeyPlanItData, OutputProperty[] outputProperties,
			OutputProperty[] outputKeys, ODMatrixIterator odMatrixIterator,
			TraditionalStaticAssignmentODOutputAdapter traditionalStaticAssignmentODOutputAdapter, Mode mode,
			TimePeriod timePeriod) throws PlanItException {
		odMatrixIterator.next();
		updateOutputAndKeyValues(multiKeyPlanItData, outputProperties, outputKeys, (label) -> {
			return traditionalStaticAssignmentODOutputAdapter.getOdPropertyValue(label, odMatrixIterator, mode,
					timePeriod, outputTimeUnit.getMultiplier());
		});
	}

	/**
	 * Store results for current mode, time period and output type
	 * 
	 * @param outputTypeConfiguration  the current output type configuration
	 * @param iterator                 iterator through data to be recorded
	 * @param iterationIndex           index of current iteration
	 * @param mode                     current mode
	 * @param timePeriod               current time period
	 * @param updateOutputAndKeyValues lambda function to update and store output
	 *                                 and key values
	 * @throws PlanItException thrown if there is an error
	 */
	private void writeResultsForCurrentModeAndTimePeriod(OutputTypeConfiguration outputTypeConfiguration,
			Iterator iterator, int iterationIndex, Mode mode, TimePeriod timePeriod,
			TriConsumer<MultiKeyPlanItData, OutputProperty[], OutputProperty[]> updateOutputAndKeyValues)
			throws PlanItException {
		try {
			OutputType outputType = outputTypeConfiguration.getOutputType();
			MultiKeyPlanItData multiKeyPlanItData = new MultiKeyPlanItData(outputKeyProperties.get(outputType),
					outputValueProperties.get(outputType));
			OutputProperty[] outputProperties = outputValueProperties.get(outputType);
			OutputProperty[] outputKeys = outputKeyProperties.get(outputType);
			while (iterator.hasNext()) {
				updateOutputAndKeyValues.accept(multiKeyPlanItData, outputProperties, outputKeys);
			}
			timeModeOutputTypeIterationDataMap.put(mode, timePeriod, iterationIndex, outputType, multiKeyPlanItData);
		} catch (Exception e) {
			throw new PlanItException(e);
		}
	}

	/**
	 * Write Simulation results for the current time period to the CSV file
	 * 
	 * @param outputTypeConfiguration OutputTypeConfiguration for current
	 *                                persistence
	 * @param modes                   Set of modes of travel
	 * @param timePeriod              current time period
	 * @throws PlanItException thrown if there is an error
	 */
	@Override
	protected void writeSimulationResultsForCurrentTimePeriod(OutputTypeConfiguration outputTypeConfiguration,
			Set<Mode> modes, TimePeriod timePeriod) throws PlanItException {
		PlanItLogger.info("Memory Output for OutputType SIMULATION has not been implemented yet.");
	}

	/**
	 * Write General results for the current time period to the CSV file
	 * 
	 * @param outputTypeConfiguration OutputTypeConfiguration for current
	 *                                persistence
	 * @param modes                   Set of current modes of travel
	 * @param timePeriod              current time period
	 * @throws PlanItException thrown if there is an error
	 */
	@Override
	protected void writeGeneralResultsForCurrentTimePeriod(OutputTypeConfiguration outputTypeConfiguration,
			Set<Mode> modes, TimePeriod timePeriod) throws PlanItException {
		PlanItLogger.info("Memory Output for OutputType GENERAL has not been implemented yet.");
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
	@Override
	protected void writeLinkResultsForCurrentTimePeriod(OutputTypeConfiguration outputTypeConfiguration,
			Set<Mode> modes, TimePeriod timePeriod) throws PlanItException {
		TraditionalStaticAssignmentLinkOutputAdapter traditionalStaticAssignmentLinkOutputAdapter = (TraditionalStaticAssignmentLinkOutputAdapter) outputTypeConfiguration
				.getOutputAdapter();
		TransportNetwork transportNetwork = traditionalStaticAssignmentLinkOutputAdapter.getTransportNetwork();
		for (Mode mode : modes) {
			Iterator<LinkSegment> iterator = transportNetwork.linkSegments.iterator();
			writeResultsForCurrentModeAndTimePeriod(outputTypeConfiguration, iterator,
					traditionalStaticAssignmentLinkOutputAdapter.getIterationIndex(), mode, timePeriod,
					(multiKeyPlanItData, outputProperties, outputKeys) -> {
						updateOutputAndKeyValuesForLinks(multiKeyPlanItData, outputProperties, outputKeys, iterator,
								traditionalStaticAssignmentLinkOutputAdapter, mode, timePeriod);
					});
		}
	}

	/**
	 * Write Origin-Destination results for the time period to the CSV file
	 * 
	 * @param outputTypeConfiguration OutputTypeConfiguration for current
	 *                                persistence
	 * @param modes                   Set of modes of travel
	 * @param timePeriod              current time period
	 * @throws PlanItException thrown if there is an error
	 */
	@Override
	protected void writeOdResultsForCurrentTimePeriod(OutputTypeConfiguration outputTypeConfiguration, Set<Mode> modes,
			TimePeriod timePeriod) throws PlanItException {
		try {
			TraditionalStaticAssignmentODOutputAdapter traditionalStaticAssignmentODOutputAdapter = (TraditionalStaticAssignmentODOutputAdapter) outputTypeConfiguration
					.getOutputAdapter();
			TraditionalStaticAssignmentSimulationData traditionalStaticAssignmentSimulationData = (TraditionalStaticAssignmentSimulationData) traditionalStaticAssignmentODOutputAdapter
					.getSimulationData();
			for (ODSkimOutputType odSkimOutputType : traditionalStaticAssignmentSimulationData
					.getActiveSkimOutputTypes()) {
				for (Mode mode : modes) {
					ODMatrixIterator odMatrixIterator = traditionalStaticAssignmentSimulationData
							.getODSkimMatrix(odSkimOutputType, mode).iterator();
					writeResultsForCurrentModeAndTimePeriod(outputTypeConfiguration, odMatrixIterator,
							traditionalStaticAssignmentODOutputAdapter.getIterationIndex(), mode, timePeriod,
							(multiKeyPlanItData, outputProperties, outputKeys) -> {
								updateOutputAndKeyValuesForOD(multiKeyPlanItData, outputProperties, outputKeys,
										odMatrixIterator, traditionalStaticAssignmentODOutputAdapter, mode, timePeriod);
							});
				}
			}
		} catch (Exception e) {
			throw new PlanItException(e);
		}
	}

	/**
	 * Constructor
	 */
	public MemoryOutputFormatter() {
		super();
	}

	/**
	 * Get the data map for the specified key values
	 * 
	 * @param mode           value of mode key
	 * @param timePeriod     value of time period key
	 * @param iterationIndex value of iteration index key
	 * @param outputType     value of output type key
	 * @return data map for the specified keys
	 */
	public MultiKeyPlanItData getOutputData(Mode mode, TimePeriod timePeriod, Integer iterationIndex,
			OutputType outputType) {
		return (MultiKeyPlanItData) timeModeOutputTypeIterationDataMap.get(mode, timePeriod, iterationIndex,
				outputType);
	}

	/**
	 * Opens all resources used in the formatter
	 * 
	 * @param outputTypeConfiguration OutputTypeConfiguration for the assignment to
	 *                                be saved
	 * @param runId                   the id of the traffic assignment to be saved
	 * @throws PlanItException thrown if there is an error
	 */
	@Override
	public void open(OutputTypeConfiguration outputTypeConfiguration, long runId) throws PlanItException {
		timeModeOutputTypeIterationDataMap = MultiKeyMap.decorate(new HashedMap());
	}

	/**
	 * Close all resources used in this formatter
	 * 
	 * @param outputTypeConfiguration OutputTypeConfiguration for the assignment to
	 *                                be saved
	 * @throws PlanItException thrown if there is an error
	 */
	@Override
	public void close(OutputTypeConfiguration outputTypeConfiguration) throws PlanItException {
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

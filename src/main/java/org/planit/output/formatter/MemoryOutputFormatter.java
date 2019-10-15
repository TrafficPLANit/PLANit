package org.planit.output.formatter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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
import org.planit.output.OutputType;
import org.planit.output.adapter.TraditionalStaticAssignmentLinkOutputAdapter;
import org.planit.output.configuration.OutputTypeConfiguration;
import org.planit.output.property.OutputProperty;
import org.planit.time.TimePeriod;
import org.planit.userclass.Mode;

public class MemoryOutputFormatter extends BaseOutputFormatter {

	/**
	 * MultiKeyMap of data stores
	 */
	private MultiKeyMap timeModeOutputTypeIterationDataMap;

	/**
	 * Map to store whether any data values have been stored for a given output
	 * type.
	 * 
	 * If data have been stored for an output type, it is "locked" so its key and
	 * output properties cannot be reset
	 */
	private Map<OutputType, Boolean> outputTypeValuesLocked;

	/**
	 * Map to store which output types are already in use as keys
	 */
	private Map<OutputType, Boolean> outputTypeKeysLocked;

	/**
	 * Save the data for the current time period, mode, iteration and output type
	 * 
	 * @param timePeriod               the specified time period
	 * @param mode                     the specified mode
	 * @param iterationIndex           the current iteration index
	 * @param outputType               the current output type
	 * @param multiKeyPlanItData       the data map
	 * @param modalNetworkSegmentCosts calculated segment costs for the physical
	 *                                 network
	 * @param modalNetworkSegmentFlows calculated flows for the network
	 * @param linkSegment              the current link segment
	 * @throws PlanItException thrown if there is an error
	 */
	private void saveRecordForLinkSegment(TraditionalStaticAssignmentLinkOutputAdapter outputAdapter, TimePeriod timePeriod, Mode mode, int iterationIndex, OutputType outputType,
			MultiKeyPlanItData multiKeyPlanItData, double[] modalNetworkSegmentCosts, double[] modalNetworkSegmentFlows,
			MacroscopicLinkSegment linkSegment) throws PlanItException {
		OutputProperty[] outputProperties = outputValueProperties.get(outputType);
		Object[] outputValues = new Object[outputProperties.length];
		OutputProperty[] outputKeys = outputKeyProperties.get(outputType);
		Object[] keyValues = new Object[outputKeys.length];
		int id = (int) linkSegment.getId();
		double flow = modalNetworkSegmentFlows[id];
		if (flow > 0.0) {
			double cost = modalNetworkSegmentCosts[id] * getTimeUnitMultiplier();
			for (int i = 0; i < outputValues.length; i++) {
				switch (outputProperties[i]) {
				case FLOW:
					outputValues[i] = Double.valueOf(flow);
					break;
				case COST:
					outputValues[i] = Double.valueOf(cost);
					break;
				default:
					outputValues[i] = outputAdapter.getPropertyValue(outputProperties[i], linkSegment, mode, timePeriod);
				}
				if (outputValues[i] == null) {
					outputValues[i] = NOT_SPECIFIED;
				}
			}
			for (int i = 0; i < outputKeys.length; i++) {
				keyValues[i] = linkSegment.getKeyValue(outputKeys[i]);
			}
			multiKeyPlanItData.putRow(outputValues, keyValues);
		}
	}

	/**
	 * Save results for the current mode and time period
	 * 
	 * @param outputType               the current output type
	 * @param outputAdapter            TraditionalStaticAssignmentLinkOutputAdapter
	 * @param mode                     current mode of travel
	 * @param timePeriod               current time period
	 * @param modalNetworkSegmentCosts calculated segment costs for the physical
	 *                                 network
	 * @param modalNetworkSegmentFlows calculated flows for the network
	 * @param transportNetwork         the transport network
	 * @throws PlanItException thrown if there is an error
	 */
	private void writeLinkResultsForCurrentModeAndTimePeriod(OutputType outputType,
			TraditionalStaticAssignmentLinkOutputAdapter outputAdapter, Mode mode, TimePeriod timePeriod,
			double[] modalNetworkSegmentCosts, double[] modalNetworkSegmentFlows, TransportNetwork transportNetwork)
			throws PlanItException {
		MultiKeyPlanItData multiKeyPlanItData = new MultiKeyPlanItData(outputKeyProperties.get(outputType),
				outputValueProperties.get(outputType));
		int iterationIndex = outputAdapter.getIterationIndex();
		try {
			if (outputType.equals(OutputType.LINK)) {
				Iterator<LinkSegment> linkSegmentIter = transportNetwork.linkSegments.iterator();
				while (linkSegmentIter.hasNext()) {
					MacroscopicLinkSegment linkSegment = (MacroscopicLinkSegment) linkSegmentIter.next();
					saveRecordForLinkSegment(outputAdapter, timePeriod, mode, iterationIndex, outputType, multiKeyPlanItData,
							modalNetworkSegmentCosts, modalNetworkSegmentFlows, linkSegment);
				}
				timeModeOutputTypeIterationDataMap.put(mode, timePeriod, iterationIndex, outputType,
						multiKeyPlanItData);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new PlanItException(e);
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
	protected void writeOdResultsForCurrentModeAndTimePeriod(OutputTypeConfiguration outputTypeConfiguration,
			Set<Mode> modes, TimePeriod timePeriod) throws PlanItException {
		PlanItLogger.info("Memory Output for OutputType OD has not been implemented yet.");
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
	protected void writeSimulationResultsForCurrentModeAndTimePeriod(OutputTypeConfiguration outputTypeConfiguration,
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
	protected void writeGeneralResultsForCurrentModeAndTimePeriod(OutputTypeConfiguration outputTypeConfiguration,
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
	protected void writeLinkResultsForCurrentModeAndTimePeriod(OutputTypeConfiguration outputTypeConfiguration,
			Set<Mode> modes, TimePeriod timePeriod) throws PlanItException {
		TraditionalStaticAssignmentLinkOutputAdapter outputAdapter = (TraditionalStaticAssignmentLinkOutputAdapter) outputTypeConfiguration.getOutputAdapter();
		TraditionalStaticAssignmentSimulationData simulationData = (TraditionalStaticAssignmentSimulationData) outputAdapter.getSimulationData();
		TransportNetwork transportNetwork = outputAdapter.getTransportNetwork();
		OutputType outputType = outputTypeConfiguration.getOutputType();
		for (Mode mode : modes) {
			double[] modalNetworkSegmentCosts = simulationData.getModalNetworkSegmentCosts(mode);
			double[] modalNetworkSegmentFlows = simulationData.getModalNetworkSegmentFlows(mode);
			writeLinkResultsForCurrentModeAndTimePeriod(outputType, outputAdapter, mode, timePeriod,
					modalNetworkSegmentCosts, modalNetworkSegmentFlows, transportNetwork);
		}
		// lock configuration properties for this output type
		outputTypeValuesLocked.put(outputType, true);
		outputTypeKeysLocked.put(outputType, true);
	}

	/**
	 * Constructor
	 */
	public MemoryOutputFormatter() {
		super();
		outputTypeValuesLocked = new HashMap<OutputType, Boolean>();
		outputTypeKeysLocked = new HashMap<OutputType, Boolean>();
		for (OutputType outputType : OutputType.values()) {
			outputTypeValuesLocked.put(outputType, false);
			outputTypeKeysLocked.put(outputType, false);
		}

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
	 * @param outputTypeConfiguration OutputTypeConfiguration for the assignment to  be saved
	 * @param runId the id of the traffic assignment to be saved
	 * @throws PlanItException thrown if there is an error
	 */
	@Override
	public void open(OutputTypeConfiguration outputTypeConfiguration, long runId) throws PlanItException {
		timeModeOutputTypeIterationDataMap = MultiKeyMap.decorate(new HashedMap());
	}

	/**
	 * Close all resources used in this formatter
	 * 
	 * @param outputTypeConfiguration OutputTypeConfiguration for the assignment to  be saved
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
	 * If this returns false, acts as though OutputConfiguration.setPersistOnlyFinalIteration() is set to true
	 * 
	 * @return flag to indicate whether the OutputFormatter can handle multiple iterations
	 */
	@Override
	public boolean canHandleMultipleIterations() {
		// TODO Auto-generated method stub
		return true;
	}

}

package org.planit.output.formatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.collections.map.MultiKeyMap;
import org.planit.data.MultiKeyPlanItData;
import org.planit.data.TraditionalStaticAssignmentSimulationData;
import org.planit.exceptions.PlanItException;
import org.planit.network.physical.LinkSegment;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.network.transport.TransportNetwork;
import org.planit.output.OutputType;
import org.planit.output.adapter.OutputAdapter;
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
	 * Map of OutputProperty types of keys for each OutputType
	 */
	private Map<OutputType, OutputProperty[]> outputKeyProperties;

	/**
	 * Map of OutputProperty types for values for each OutputType
	 */
	private Map<OutputType, OutputProperty[]> outputValueProperties;

	/**
	 * Map to store whether any data values have been stored for a given output type.
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
	private void saveRecordForLinkSegment(TimePeriod timePeriod, Mode mode, int iterationIndex, OutputType outputType,
			MultiKeyPlanItData multiKeyPlanItData, double[] modalNetworkSegmentCosts, double[] modalNetworkSegmentFlows,
			MacroscopicLinkSegment linkSegment) throws PlanItException {
		OutputProperty[] outputProperties = outputValueProperties.get(outputType);
		Object[] outputValues = new Object[outputProperties.length];
		OutputProperty[] outputKeys = outputKeyProperties.get(outputType);
		Object[] keyValues = new Object[outputKeys.length];
		int id = (int) linkSegment.getId();
		double flow = modalNetworkSegmentFlows[id];
		if (flow > 0.0) {
			double cost = modalNetworkSegmentCosts[id]  * getTimeUnitMultiplier();
			for (int i = 0; i < outputValues.length; i++) {
				switch (outputProperties[i]) {
				case FLOW:
					outputValues[i] = Double.valueOf(flow);
					break;
				case COST:
					outputValues[i] = Double.valueOf(cost);
					break;
				default:
					outputValues[i] = linkSegment.getPropertyValue(outputProperties[i], mode);
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
	private void writeResultsForCurrentModeAndTimePeriod(OutputType outputType,
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
					saveRecordForLinkSegment(timePeriod, mode, iterationIndex, outputType, multiKeyPlanItData,
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
	 * Save the results for the current time period
	 * 
	 * @param outputType     the current output type
	 * @param outputAdapter  TraditionalStaticAssignmentLinkOutputAdapter used to
	 *                       retrieve the results of the assignment
	 * @param simulationData simulation data for the current iteration
	 * @param modes          Set of modes of travel
	 * @param timePeriod     the current time period
	 * @throws PlanItException thrown if there is an error
	 */
	private void writeResultsForCurrentTimePeriod(OutputType outputType,
			TraditionalStaticAssignmentLinkOutputAdapter outputAdapter,
			TraditionalStaticAssignmentSimulationData simulationData, Set<Mode> modes, TimePeriod timePeriod)
			throws PlanItException {
		TransportNetwork transportNetwork = outputAdapter.getTransportNetwork();
		for (Mode mode : modes) {
			double[] modalNetworkSegmentCosts = simulationData.getModalNetworkSegmentCosts(mode);
			double[] modalNetworkSegmentFlows = simulationData.getModalNetworkSegmentFlows(mode);
			writeResultsForCurrentModeAndTimePeriod(outputType, outputAdapter, mode, timePeriod,
					modalNetworkSegmentCosts, modalNetworkSegmentFlows, transportNetwork);
		}
		// lock configuration properties for this output type
		outputTypeValuesLocked.put(outputType, true);
		outputTypeKeysLocked.put(outputType, true);
	}

	/**
	 * Tests whether the current output type is appropriate for the current key
	 * output types
	 * 
	 * @param outputType the current output type
	 * @return true if the current output type is valid, false otherwise
	 */
	private boolean isOutputKeysValid(OutputType outputType) {
		
		switch (outputType) {
		case GENERAL:
			return true;
		case LINK:

			List<OutputProperty> outputKeyPropertyList = new ArrayList<OutputProperty>();
			OutputProperty [] outputKeyPropertiesArray = outputKeyProperties.get(outputType);
			for (int i=0; i<outputKeyPropertiesArray.length; i++) {
				OutputProperty outputKeyProperty = outputKeyPropertiesArray[i];
				if (outputKeyProperty.equals(OutputProperty.LINK_SEGMENT_ID) ||
					 outputKeyProperty.equals(OutputProperty.LINK_SEGMENT_EXTERNAL_ID) ||
					 outputKeyProperty.equals(OutputProperty.DOWNSTREAM_NODE_EXTERNAL_ID) || 
					 outputKeyProperty.equals(OutputProperty.UPSTREAM_NODE_EXTERNAL_ID)) {
					outputKeyPropertyList.add(outputKeyProperty);
				}
			}
			if (outputKeyPropertyList.contains(OutputProperty.DOWNSTREAM_NODE_EXTERNAL_ID) && outputKeyPropertyList.contains(OutputProperty.UPSTREAM_NODE_EXTERNAL_ID)) {
				outputKeyPropertiesArray = new OutputProperty[2];
				outputKeyPropertiesArray[0] = OutputProperty.DOWNSTREAM_NODE_EXTERNAL_ID;
				outputKeyPropertiesArray[1] = OutputProperty.UPSTREAM_NODE_EXTERNAL_ID;
				outputKeyProperties.put(outputType, outputKeyPropertiesArray);
				return true;
			}

			if (outputKeyPropertyList.contains(OutputProperty.LINK_SEGMENT_ID)) {
				outputKeyPropertiesArray = new OutputProperty[1];
				outputKeyPropertiesArray[0] = OutputProperty.LINK_SEGMENT_ID;
				outputKeyProperties.put(outputType, outputKeyPropertiesArray);
				return true;
			}

			if (outputKeyPropertyList.contains(OutputProperty.LINK_SEGMENT_EXTERNAL_ID)) {
				outputKeyPropertiesArray = new OutputProperty[1];
				outputKeyPropertiesArray[0] = OutputProperty.LINK_SEGMENT_EXTERNAL_ID;
				outputKeyProperties.put(outputType, outputKeyPropertiesArray);
				return true;
			}
			return false;
		case SIMULATION:
			return true;
		case OD:
			return true;
		}
		return true;
	}

	/**
	 * Constructor
	 */
	public MemoryOutputFormatter() {
		outputKeyProperties = new HashMap<OutputType, OutputProperty[]>();
		outputValueProperties = new HashMap<OutputType, OutputProperty[]>();
		outputTypeValuesLocked = new HashMap<OutputType, Boolean>();
		outputTypeKeysLocked = new HashMap<OutputType, Boolean>();
		for (OutputType outputType : OutputType.values()) {
			outputTypeValuesLocked.put(outputType, false);
			outputTypeKeysLocked.put(outputType, false);
		}

	}

	/**
	 * Save the data for the current iteration
	 * 
	 * @param timePeriod              the current time period
	 * @param modes                   the Set of modes
	 * @param outputTypeConfiguration the current output type configuration
	 */
	@Override
	public void persist(TimePeriod timePeriod, Set<Mode> modes, OutputTypeConfiguration outputTypeConfiguration)
			throws PlanItException {
		OutputType outputType = outputTypeConfiguration.getOutputType();
		if (!outputTypeValuesLocked.get(outputType)) {
			OutputProperty[] outputValueProperties = outputTypeConfiguration.getOutputValueProperties();
			setOutputValueProperties(outputType, outputValueProperties);
		}
		if (!outputTypeKeysLocked.get(outputType)) {
			OutputProperty[] outputKeyProperties = outputTypeConfiguration.getOutputKeyProperties();
			setOutputKeyProperties(outputType, outputKeyProperties);
		}
		if (!isOutputKeysValid(outputType)) {
			throw new PlanItException("Invalid output keys defined for output type.");
		}
		OutputAdapter outputAdapter = outputTypeConfiguration.getOutputAdapter();
		if (outputAdapter instanceof TraditionalStaticAssignmentLinkOutputAdapter) {
			TraditionalStaticAssignmentLinkOutputAdapter traditionalStaticAssignmentLinkOutputAdapter = (TraditionalStaticAssignmentLinkOutputAdapter) outputAdapter;
			TraditionalStaticAssignmentSimulationData traditionalStaticAssignmentSimulationData = (TraditionalStaticAssignmentSimulationData) outputAdapter
					.getSimulationData();
			writeResultsForCurrentTimePeriod(outputType, traditionalStaticAssignmentLinkOutputAdapter,
					traditionalStaticAssignmentSimulationData, modes, timePeriod);
		} else {
			throw new PlanItException("OutputAdapter is of class " + outputAdapter.getClass().getCanonicalName()
					+ " which has not been defined yet");
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
	 * @throws PlanItException thrown if there is an error
	 */
	@Override
	public void open() throws PlanItException {
		timeModeOutputTypeIterationDataMap = MultiKeyMap.decorate(new HashedMap());
	}

	/**
	 * Close all resources used in this formatter
	 * 
	 * @throws PlanItException thrown if there is an error
	 */
	@Override
	public void close() throws PlanItException {
		// TODO Auto-generated method stub
	}

	/**
	 * Set the output properties of the key values for the current output type
	 * 
	 * @param outputType          the current output type
	 * @param outputKeyProperties output properties of the keys
	 * @throws PlanItException throw if the key properties for this output type are
	 *                         already in use
	 */
	public void setOutputKeyProperties(OutputType outputType, OutputProperty... outputKeyProperties)
			throws PlanItException {
		if (outputTypeKeysLocked.get(outputType)) {
			throw new PlanItException("A call to setOutputKeyProperties() was made after the outputType "
					+ outputType.value() + " was locked");
		}
		this.outputKeyProperties.put(outputType, outputKeyProperties);
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
	 * Sets the output properties of the data values for the current output type
	 * 
	 * @param outputType            the current output type
	 * @param outputValueProperties array containing the output property types of
	 *                              the data values
	 * @throws PlanItException throw if the key properties for this output type are
	 *                         already in use
	 */
	public void setOutputValueProperties(OutputType outputType, OutputProperty... outputValueProperties)
			throws PlanItException {
		if (outputTypeValuesLocked.get(outputType)) {
			throw new PlanItException("A call to setOutputKeyProperties() was made after the outputType "
					+ outputType.value() + " was locked");
		}
		this.outputValueProperties.put(outputType, outputValueProperties);
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
	
}

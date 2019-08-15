package org.planit.output.formatter;

import java.util.Iterator;
import java.util.Set;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.collections.map.MultiKeyMap;
import org.planit.data.MultiKeyPlanItData;
import org.planit.data.TraditionalStaticAssignmentSimulationData;
import org.planit.exceptions.PlanItException;
import org.planit.network.physical.LinkSegment;
import org.planit.network.physical.Node;
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

	private MultiKeyMap timePeriodModeOutputTypeMultiKeyMap;
	private OutputProperty[] outputKeyProperties;
	private OutputProperty[] outputValueProperties;

	/**
	 * Save the data for the current time period, mode, iteration and output type
	 * 
	 * @param timePeriodId          the id of the specified time period
	 * @param modeId                the id of the specified mode
	 * @param outputType            the current output type
	 * @param multiKeyPlanItData    the data map
	 * @param linkSegmentId         the id of the link segment
	 * @param linkSegmentExternalId the external Id of the link segment
	 * @param startNodeId           the external Id of the start node of this link
	 *                              segment
	 * @param endNodeId             the external Id of the end node of this link
	 *                              segment
	 * @param flow                  the flow through this link segment
	 * @param length                the length of this link segment
	 * @param maximumSpeed          the maximum speed parameter along this link
	 *                              segment
	 * @param capacityPerLane       the specified capacity per lane of this link
	 *                              segment
	 * @param numberOfLanes         the number of lanes of this link segment
	 * @param cost                  the computed travel cost for this link segment
	 * @throws PlanItException thrown if there is an error
	 */
	private void saveRecord(long timePeriodId, long modeId, int iterationIndex, OutputType outputType,
			MultiKeyPlanItData multiKeyPlanItData, long linkSegmentId, long linkSegmentExternalId, long startNodeId,
			long endNodeId, double flow, double length, double maximumSpeed, double capacityPerLane, int numberOfLanes,
			double cost) throws PlanItException {
		Object[] outputValues = new Object[outputValueProperties.length];
		for (int i = 0; i < outputValueProperties.length; i++) {
			switch (outputValueProperties[i]) {
			case LENGTH:
				outputValues[i] = Double.valueOf(length);
				break;
			case FLOW:
				outputValues[i] = Double.valueOf(flow);
				break;
			case SPEED:
				outputValues[i] = Double.valueOf(maximumSpeed);
				break;
			case CAPACITY_PER_LANE:
				outputValues[i] = Double.valueOf(capacityPerLane);
				break;
			case NUMBER_OF_LANES:
				outputValues[i] = Integer.valueOf(numberOfLanes);
				break;
			case COST:
				outputValues[i] = Double.valueOf(cost);
				break;
			}
		}
		Object[] keyValues = new Object[outputKeyProperties.length];
		for (int i = 0; i < outputKeyProperties.length; i++) {
			switch (outputKeyProperties[i]) {
			case LINK_SEGMENT_ID:
				keyValues[i] = Integer.valueOf((int) linkSegmentId);
				break;
			case LINK_SEGMENT_EXTERNAL_ID:
				keyValues[i] = Integer.valueOf((int) linkSegmentExternalId);
				break;
			case DOWNSTREAM_NODE_EXTERNAL_ID:
				keyValues[i] = Integer.valueOf((int) startNodeId);
				break;
			case UPSTREAM_NODE_EXTERNAL_ID:
				keyValues[i] = Integer.valueOf((int) endNodeId);
				break;
			}
		}
		multiKeyPlanItData.setRowValues(outputValues, keyValues);
		timePeriodModeOutputTypeMultiKeyMap.put(modeId, timePeriodId, iterationIndex, outputType, multiKeyPlanItData);
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
		MultiKeyPlanItData multiKeyPlanItData = new MultiKeyPlanItData();
		multiKeyPlanItData.registerOutputDataKeys(outputKeyProperties);
		multiKeyPlanItData.registerOutputValueProperties(outputValueProperties);
		int iterationIndex = outputAdapter.getIterationIndex();
		try {
			if (outputType.equals(OutputType.LINK)) {
				Iterator<LinkSegment> linkSegmentIter = transportNetwork.linkSegments.iterator();
				while (linkSegmentIter.hasNext()) {
					MacroscopicLinkSegment linkSegment = (MacroscopicLinkSegment) linkSegmentIter.next();
					int id = (int) linkSegment.getId();
					double flow = modalNetworkSegmentFlows[id];
					if (flow > 0.0) {
						double cost = modalNetworkSegmentCosts[id];
						Node startNode = (Node) linkSegment.getUpstreamVertex();
						Node endNode = (Node) linkSegment.getDownstreamVertex();
						saveRecord(timePeriod.getId(), mode.getExternalId(), iterationIndex, outputType,
								multiKeyPlanItData, linkSegment.getId(), linkSegment.getExternalId(),
								startNode.getExternalId(), endNode.getExternalId(), flow,
								linkSegment.getParentLink().getLength(),
								linkSegment.getMaximumSpeed(mode.getExternalId()),
								linkSegment.getLinkSegmentType().getCapacityPerLane(), linkSegment.getNumberOfLanes(),
								cost);
					}
				}
			}
		} catch (Exception e) {
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
			if ((outputKeyProperties.length == 1) && (outputKeyProperties[0].equals(OutputProperty.LINK_SEGMENT_ID))) {
				return true;
			}
			if ((outputKeyProperties.length == 1)
					&& (outputKeyProperties[0].equals(OutputProperty.LINK_SEGMENT_EXTERNAL_ID))) {
				return true;
			}
			if ((outputKeyProperties.length == 2)
					&& (outputKeyProperties[0].equals(OutputProperty.DOWNSTREAM_NODE_EXTERNAL_ID))
					&& (outputKeyProperties[1].equals(OutputProperty.UPSTREAM_NODE_EXTERNAL_ID))) {
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
	 * Save the data for the current iteration
	 * 
	 * @param timePeriod              the current time period
	 * @param modes                   the Set of modes
	 * @param outputTypeConfiguration the current output type configuration
	 * @param outputType              the current output type
	 */
	@Override
	public void persist(TimePeriod timePeriod, Set<Mode> modes, OutputTypeConfiguration outputTypeConfiguration,
			OutputType outputType) throws PlanItException {
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
	public MultiKeyPlanItData getMultiKeyPlanItData(Mode mode, TimePeriod timePeriod, Integer iterationIndex,
			OutputType outputType) {
		return (MultiKeyPlanItData) timePeriodModeOutputTypeMultiKeyMap.get(mode.getExternalId(), timePeriod.getId(),
				iterationIndex, outputType);
	}

	/**
	 * Opens all resources used in the formatter
	 * 
	 * @throws PlanItException thrown if there is an error
	 */
	@Override
	public void open() throws PlanItException {
		timePeriodModeOutputTypeMultiKeyMap = MultiKeyMap.decorate(new HashedMap());
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
	 * Set the output properties of the key values
	 * 
	 * @param outputKeyProperties output properties of the keys
	 */
	public void setOutputKeyProperties(OutputProperty... outputKeyProperties) {
		this.outputKeyProperties = outputKeyProperties;
	}

	/**
	 * Returns the array of output properties representing the output types of the
	 * keys
	 * 
	 * @return array of output properties of the keys
	 */
	public OutputProperty[] getOutputKeyProperties() {
		return outputKeyProperties;
	}

	/**
	 * Sets the output properties of the data values
	 * 
	 * @param outputValueProperties array containing the output property types of
	 *                              the data values
	 */
	public void setOutputValueProperties(OutputProperty... outputValueProperties) {
		this.outputValueProperties = outputValueProperties;
	}

	/**
	 * Returns the array of output properties representing the output types of the
	 * data values
	 * 
	 * @return array of output properties of the data values
	 */
	public OutputProperty[] getOutputValueProperties() {
		return outputValueProperties;
	}

	/**
	 * Returns the value of the last iteration of recorded data
	 * 
	 * @return the last iteration of recorded data
	 */
	public int getLastIteration() {
		Set<MultiKey> keySet = (Set<MultiKey>) timePeriodModeOutputTypeMultiKeyMap.keySet();
		int lastIteration = 0;
		for (MultiKey multiKey : keySet) {
			Object[] keys = multiKey.getKeys();
			Integer iteration = (Integer) keys[2];
			lastIteration = Math.max(lastIteration, iteration);
		}
		return lastIteration;
	}

}

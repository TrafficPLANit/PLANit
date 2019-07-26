package org.planit.output.formatter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Map;
import java.util.logging.Logger;

import org.planit.data.TraditionalStaticAssignmentSimulationData;
import org.planit.exceptions.PlanItException;
import org.planit.network.physical.LinkSegment;
import org.planit.network.physical.Node;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.network.transport.TransportNetwork;
import org.planit.output.adapter.OutputAdapter;
import org.planit.output.adapter.TraditionalStaticAssignmentLinkOutputAdapter;
import org.planit.output.configuration.OutputTypeConfiguration;
import org.planit.output.property.BaseOutputProperty;
import org.planit.output.property.OutputProperty;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.TraditionalStaticAssignment;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.userclass.Mode;

public class MemoryOutputFormatter extends BaseOutputFormatter {

	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = Logger.getLogger(MemoryOutputFormatter.class.getName());

	private Map<Long, Map<Long, Map<Long, Map<Long, Map<Long, Map<OutputProperty, Object>>>>>> memoryTable;

	private void saveRecord(long trafficAssignmentId, long timePeriodId, long modeId, long startNodeId, long endNodeId,
			double flow, double length, double maximumSpeed, double capacityPerLane, int numberOfLanes, double cost)
			throws PlanItException {
		if (!memoryTable.containsKey(trafficAssignmentId)) {
			memoryTable.put(trafficAssignmentId,
					new HashMap<Long, Map<Long, Map<Long, Map<Long, Map<OutputProperty, Object>>>>>());
		}
		if (!memoryTable.get(trafficAssignmentId).containsKey(timePeriodId)) {
			memoryTable.get(trafficAssignmentId).put(timePeriodId,
					new HashMap<Long, Map<Long, Map<Long, Map<OutputProperty, Object>>>>());
		}
		if (!memoryTable.get(trafficAssignmentId).get(timePeriodId).containsKey(modeId)) {
			memoryTable.get(trafficAssignmentId).get(timePeriodId).put(modeId,
					new HashMap<Long, Map<Long, Map<OutputProperty, Object>>>());
		}
		if (!memoryTable.get(trafficAssignmentId).get(timePeriodId).get(modeId).containsKey(startNodeId)) {
			memoryTable.get(trafficAssignmentId).get(timePeriodId).get(modeId).put(startNodeId,
					new HashMap<Long, Map<OutputProperty, Object>>());
		}
		if (!memoryTable.get(trafficAssignmentId).get(timePeriodId).get(modeId).get(startNodeId)
				.containsKey(endNodeId)) {
			memoryTable.get(trafficAssignmentId).get(timePeriodId).get(modeId).get(startNodeId).put(endNodeId,
					new HashMap<OutputProperty, Object>());
		}
		memoryTable.get(trafficAssignmentId).get(timePeriodId).get(modeId).get(startNodeId).get(endNodeId)
				.put(OutputProperty.FLOW, flow);
		memoryTable.get(trafficAssignmentId).get(timePeriodId).get(modeId).get(startNodeId).get(endNodeId)
				.put(OutputProperty.LENGTH, length);
		memoryTable.get(trafficAssignmentId).get(timePeriodId).get(modeId).get(startNodeId).get(endNodeId)
				.put(OutputProperty.SPEED, maximumSpeed);
		memoryTable.get(trafficAssignmentId).get(timePeriodId).get(modeId).get(startNodeId).get(endNodeId)
				.put(OutputProperty.CAPACITY_PER_LANE, capacityPerLane);
		memoryTable.get(trafficAssignmentId).get(timePeriodId).get(modeId).get(startNodeId).get(endNodeId)
				.put(OutputProperty.NUMBER_OF_LANES, numberOfLanes);
		memoryTable.get(trafficAssignmentId).get(timePeriodId).get(modeId).get(startNodeId).get(endNodeId)
				.put(OutputProperty.COST, cost);
	}

	/**
	 * Save results for the current mode and time period
	 * 
	 * @param outputAdapter            TraditionalStaticAssignmentLinkOutputAdapter
	 * @param mode                     current mode of travel
	 * @param timePeriod               current time period
	 * @param modalNetworkSegmentCosts calculated segment costs for the physical
	 *                                 network
	 * @param modalNetworkSegmentFlows calculated flows for the network
	 * @param transportNetwork         the transport network
	 * @throws PlanItException thrown if there is an error
	 */
	private void writeResultsForCurrentModeAndTimePeriod(TraditionalStaticAssignmentLinkOutputAdapter outputAdapter,
			Mode mode, TimePeriod timePeriod, double[] modalNetworkSegmentCosts, double[] modalNetworkSegmentFlows,
			TransportNetwork transportNetwork) throws PlanItException {
		try {
			Iterator<LinkSegment> linkSegmentIter = transportNetwork.linkSegments.iterator();
			while (linkSegmentIter.hasNext()) {
				MacroscopicLinkSegment linkSegment = (MacroscopicLinkSegment) linkSegmentIter.next();
				int id = (int) linkSegment.getId();
				double flow = modalNetworkSegmentFlows[id];
				if (flow > 0.0) {
					double cost = modalNetworkSegmentCosts[id];
					long trafficAssignmentId = outputAdapter.getTrafficAssignmentId();
					Node startNode = (Node) linkSegment.getUpstreamVertex();
					Node endNode = (Node) linkSegment.getDownstreamVertex();
					saveRecord(trafficAssignmentId, timePeriod.getId(), mode.getExternalId(), startNode.getExternalId(),
							endNode.getExternalId(), flow, linkSegment.getParentLink().getLength(),
							linkSegment.getMaximumSpeed(mode.getExternalId()), 
							linkSegment.getLinkSegmentType().getCapacityPerLane(),
							linkSegment.getNumberOfLanes(),
							cost);
				}
			}
		} catch (Exception e) {
			throw new PlanItException(e);
		}
	}

	/**
	 * Save the results for the current time period
	 * 
	 * @param outputAdapter TraditionalStaticAssignmentLinkOutputAdapter used to
	 *                      retrieve the results of the assignment
	 * @param modes         Set of modes of travel
	 * @param timePeriod    the current time period
	 * @throws PlanItException thrown if there is an error
	 */
	private void writeResultsForCurrentTimePeriod(TraditionalStaticAssignmentLinkOutputAdapter outputAdapter,
			TraditionalStaticAssignmentSimulationData simulationData, Set<Mode> modes, TimePeriod timePeriod)
			throws PlanItException {
		TransportNetwork transportNetwork = outputAdapter.getTransportNetwork();
		for (Mode mode : modes) {
			double[] modalNetworkSegmentCosts = simulationData.getModalNetworkSegmentCosts(mode);
			double[] modalNetworkSegmentFlows = simulationData.getModalNetworkSegmentFlows(mode);
			writeResultsForCurrentModeAndTimePeriod(outputAdapter, mode, timePeriod, modalNetworkSegmentCosts,
					modalNetworkSegmentFlows, transportNetwork);
		}
	}

	/**
	 * Save data
	 * 
	 * @param timePeriod              time period for current results
	 * @param modes                   Set of modes covered by current results
	 * @param outputTypeConfiguration output configuration being used
	 * @throws PlanItException thrown if there is an error
	 */
	public void persist(TimePeriod timePeriod, Set<Mode> modes, OutputTypeConfiguration outputTypeConfiguration)
			throws PlanItException {
		try {
			OutputAdapter outputAdapter = outputTypeConfiguration.getOutputAdapter();
			if (outputAdapter instanceof TraditionalStaticAssignmentLinkOutputAdapter) {
				TraditionalStaticAssignmentLinkOutputAdapter traditionalStaticAssignmentLinkOutputAdapter = (TraditionalStaticAssignmentLinkOutputAdapter) outputAdapter;
				TrafficAssignment trafficAssignment = traditionalStaticAssignmentLinkOutputAdapter
						.getTrafficAssignment();
				TraditionalStaticAssignment traditionalStaticAssignment = (TraditionalStaticAssignment) trafficAssignment;
				TraditionalStaticAssignmentSimulationData traditionalStaticAssignmentSimulationData = (TraditionalStaticAssignmentSimulationData) traditionalStaticAssignment
						.getSimulationData();
				writeResultsForCurrentTimePeriod(traditionalStaticAssignmentLinkOutputAdapter,
						traditionalStaticAssignmentSimulationData, modes, timePeriod);
			} else {
				throw new PlanItException("OutputAdapter is of class " + outputAdapter.getClass().getCanonicalName()
						+ " which has not been defined yet");
			}
		} catch (Exception e) {
			throw new PlanItException(e);
		}
	}

	@Override
	public void open() throws PlanItException {
		memoryTable = new HashMap<Long, Map<Long, Map<Long, Map<Long, Map<Long, Map<OutputProperty, Object>>>>>>();
	}

	public Object getLinkSegmentOutput(long trafficAssignmentId, long timePeriodId, long modeId, long startNodeId,
			long endNodeId, OutputProperty outputProperty) throws PlanItException {
		if (!memoryTable.get(trafficAssignmentId).get(timePeriodId).get(modeId).get(startNodeId).get(endNodeId)
				.containsKey(outputProperty)) {
			throw new PlanItException("Trying to retrieve output property "
					+ BaseOutputProperty.convertToBaseOutputProperty(outputProperty)
					+ " which has not been saved in MemoryOutputFormatter.");
		}
		return memoryTable.get(trafficAssignmentId).get(timePeriodId).get(modeId).get(startNodeId).get(endNodeId)
				.get(outputProperty);
	}

	@Override
	public void close() throws PlanItException {
		// TODO Auto-generated method stub

	}

}

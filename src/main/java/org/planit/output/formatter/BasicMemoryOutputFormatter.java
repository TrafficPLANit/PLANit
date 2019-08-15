package org.planit.output.formatter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.collections.map.MultiKeyMap;
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
import org.planit.output.property.BaseOutputProperty;
import org.planit.output.property.OutputProperty;
import org.planit.time.TimePeriod;
import org.planit.userclass.Mode;

/**
 * Formatter which saves the results from assignment runs in memory
 * 
 * @author gman6028
 *
 */
public class BasicMemoryOutputFormatter extends BaseOutputFormatter {

	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = Logger.getLogger(BasicMemoryOutputFormatter.class.getName());

	private MultiKeyMap multiKeyMap;

/**
 * Saves a cost value for a specified link, specified by several input parameters
 * 
 * @param trafficAssignmentId the id of this traffic assignment run
 * @param timePeriodId the id of the specified time period
 * @param modeId the id of the specified mode
 * @param startNodeId the external Id of the start node of this link segment
 * @param endNodeId the external Id of the end node of this link segment
 * @param flow the flow through this link segment
 * @param length the length of this link segment
 * @param maximumSpeed the maximum speed parameter along this link segment
 * @param capacityPerLane the specified capacity per lane of this link segment
 * @param numberOfLanes the number of lanes of this link segment
 * @param cost the computed travel cost for this link segment
 * @throws PlanItException thrown if there is an error
 */
	private void saveRecord(long trafficAssignmentId, long timePeriodId, long modeId, long startNodeId, long endNodeId,
			double flow, double length, double maximumSpeed, double capacityPerLane, int numberOfLanes, double cost)
			throws PlanItException {
		Map<OutputProperty, Object> map = new HashMap<OutputProperty, Object>();
		map.put(OutputProperty.LENGTH, Double.valueOf(length));
		map.put(OutputProperty.FLOW, Double.valueOf(flow));
		map.put(OutputProperty.SPEED, Double.valueOf(maximumSpeed));
		map.put(OutputProperty.CAPACITY_PER_LANE, Double.valueOf(capacityPerLane));
		map.put(OutputProperty.NUMBER_OF_LANES, Integer.valueOf(numberOfLanes));
		map.put(OutputProperty.COST, Double.valueOf(cost));
		multiKeyMap.put(trafficAssignmentId, timePeriodId, modeId, startNodeId, endNodeId, map);
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
	 * @param simulationData simulation data for the current iteration
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
	 * @param outputType OutputType for the current persistence
	 * @throws PlanItException thrown if there is an error
	 */
	public void persist(TimePeriod timePeriod, Set<Mode> modes, OutputTypeConfiguration outputTypeConfiguration, OutputType outputType)
			throws PlanItException {
		try {
			OutputAdapter outputAdapter = outputTypeConfiguration.getOutputAdapter();
			if (outputAdapter instanceof TraditionalStaticAssignmentLinkOutputAdapter) {
				TraditionalStaticAssignmentLinkOutputAdapter traditionalStaticAssignmentLinkOutputAdapter = 
						(TraditionalStaticAssignmentLinkOutputAdapter) outputAdapter;
				TraditionalStaticAssignmentSimulationData traditionalStaticAssignmentSimulationData = 
						(TraditionalStaticAssignmentSimulationData) outputAdapter.getSimulationData();
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
		multiKeyMap = MultiKeyMap.decorate(new HashedMap());
	}

/**
 * Retrieves a specified output for the current link segment
 * 
 * @param trafficAssignmentId the id of this traffic assignment run
 * @param timePeriodId the id of the specified time period
 * @param modeId the id of the specified mode
 * @param startNodeId the external Id of the start node of this link segment
 * @param endNodeId the external Id of the end node of this link segment
 * @param outputProperty enumeration value used to specify which output is required
 * @return the value of the specified output
 * @throws PlanItException thrown if there is an error
 */
	public Object getLinkSegmentOutput(long trafficAssignmentId, long timePeriodId, long modeId, long startNodeId,
			long endNodeId, OutputProperty outputProperty) throws PlanItException {
		Map<OutputProperty, Object> map = (Map<OutputProperty, Object>) multiKeyMap.get(trafficAssignmentId, timePeriodId, modeId, startNodeId, endNodeId);
		if (!map.containsKey(outputProperty)) {
			throw new PlanItException("Trying to retrieve output property "
					+ BaseOutputProperty.convertToBaseOutputProperty(outputProperty)
					+ " which has not been saved in MemoryOutputFormatter.");
		}
		return map.get(outputProperty);
	}

	@Override
	public void close() throws PlanItException {
	}

}

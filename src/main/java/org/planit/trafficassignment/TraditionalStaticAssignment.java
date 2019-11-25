package org.planit.trafficassignment;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.planit.algorithms.shortestpath.DijkstraShortestPathAlgorithm;
import org.planit.algorithms.shortestpath.ShortestPathAlgorithm;
import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.data.ModeData;
import org.planit.data.SimulationData;
import org.planit.data.TraditionalStaticAssignmentSimulationData;
import org.planit.event.RequestAccesseeEvent;
import org.planit.event.listener.InteractorListener;
import org.planit.exceptions.PlanItException;
import org.planit.gap.GapFunction;
import org.planit.gap.LinkBasedRelativeDualityGapFunction;
import org.planit.gap.StopCriterion;
import org.planit.interactor.LinkVolumeAccessee;
import org.planit.logging.PlanItLogger;
import org.planit.network.EdgeSegment;
import org.planit.network.Vertex;
import org.planit.network.physical.LinkSegment;
import org.planit.network.physical.Node;
import org.planit.network.virtual.Centroid;
import org.planit.network.virtual.ConnectoidSegment;
import org.planit.od.odmatrix.ODMatrixIterator;
import org.planit.od.odmatrix.demand.ODDemandMatrix;
import org.planit.od.odmatrix.skim.ODSkimMatrix;
import org.planit.od.odpath.ODPath;
import org.planit.output.adapter.OutputTypeAdapter;
import org.planit.output.adapter.TraditionalStaticAssignmentLinkOutputTypeAdapter;
import org.planit.output.adapter.TraditionalStaticAssignmentODOutputTypeAdapter;
import org.planit.output.adapter.TraditionalStaticODPathOutputTypeAdapter;
import org.planit.output.configuration.OutputConfiguration;
import org.planit.output.configuration.OutputTypeConfiguration;
import org.planit.output.enums.ODSkimOutputType;
import org.planit.output.enums.OutputType;
import org.planit.output.formatter.OutputFormatter;
import org.planit.time.TimePeriod;
import org.planit.userclass.Mode;
import org.planit.utils.ArrayOperations;
import org.planit.utils.Pair;
import org.planit.zoning.Zone;

/**
 * Traditional static assignment traffic component
 * 
 * @author markr, gman6028
 *
 */
public class TraditionalStaticAssignment extends CapacityRestrainedAssignment
		implements LinkVolumeAccessee, InteractorListener {

	/**
	 * Formatter to be used to output demand values
	 */
	private static DecimalFormat df5 = new DecimalFormat("#.#####");

	/**
	 * Formatter to be used to output duality gap
	 */
	private static DecimalFormat df6 = new DecimalFormat("#.######");

	/**
	 * Epsilon margin when comparing flow rates (veh/h)
	 */
	private static final double DEFAULT_FLOW_EPSILON = 0.000001;

	/**
	 * holds the count of all vertices in the transport network
	 */
	private int numberOfNetworkVertices;

	/**
	 * Holds the running simulation data for the assignment
	 */
	private TraditionalStaticAssignmentSimulationData simulationData;
	
	/**
	 * Initialize running simulation variables for the time period
	 * 
	 * @param modes set of modes covered by this assignment
	 * @throws PlanItException thrown if there is an error
	 */
	private void initialiseTimePeriod(Set<Mode> modes) throws PlanItException {
		OutputConfiguration outputConfiguration = outputManager.getOutputConfiguration();
		simulationData = new TraditionalStaticAssignmentSimulationData(outputConfiguration);
		simulationData.setIterationIndex(0);
		simulationData.getModeSpecificData().clear();
		for (Mode mode : modes) {
			simulationData.resetModalNetworkSegmentFlows(mode, numberOfNetworkSegments);
			simulationData.getModeSpecificData().put(mode, new ModeData(new double[numberOfNetworkSegments]));
		}
	}

	/**
	 * Apply smoothing based on current and previous flows and the adopted smoothing
	 * method. The smoothed results are registered as the current segment flows
	 * while the current segment flows are assigned to the previous segment flows
	 * (which are discarded).
	 * 
	 * @param modeData data for the current mode
	 */
	private void applySmoothing(ModeData modeData) {
		double[] smoothedSegmentFlows = smoothing.applySmoothing(modeData.currentNetworkSegmentFlows,
				modeData.nextNetworkSegmentFlows, numberOfNetworkSegments);
		// update flow arrays for next iteration
		modeData.currentNetworkSegmentFlows = smoothedSegmentFlows;
	}

	/**
	 * Perform assignment for a given time period, mode and costs imposed on
	 * Dijkstra shortest path
	 * 
	 * @param mode                     the current mode
	 * @param odDemandMatrix                origin-demand matrix
	 * @param currentModeData          data for the current mode
	 * @param modalNetworkSegmentCosts segment costs for the network
	 * @param shortestPathAlgorithm    shortest path algorithm to be used
	 * @return Map of vertex path arrays between each origin and destination zone
	 * @throws PlanItException thrown if there is an error
	 */
	private Map<Zone, Map<Zone, Pair<Double, EdgeSegment>[]>> executeModeTimePeriod(Mode mode, ODDemandMatrix odDemandMatrix, ModeData currentModeData, double[] modalNetworkSegmentCosts, ShortestPathAlgorithm shortestPathAlgorithm) throws PlanItException {
 		LinkBasedRelativeDualityGapFunction dualityGapFunction = ((LinkBasedRelativeDualityGapFunction) getGapFunction());
		ODPath odPath = simulationData.getODPath(mode);
		Map<Zone, Map<Zone, Pair<Double, EdgeSegment>[]>> vertexPathMap = new HashMap<Zone, Map<Zone, Pair<Double, EdgeSegment>[]>>();
		
		// loop over all available OD demands
		long previousOriginZoneId =  -1;
		Pair<Double, EdgeSegment>[] vertexPathAndCost = null;
		for (ODMatrixIterator odDemandMatrixIter = odDemandMatrix.iterator(); odDemandMatrixIter.hasNext(); ) {
		    double odDemand = odDemandMatrixIter.next();
			Zone currentOriginZone = odDemandMatrixIter.getCurrentOrigin();
			if (!vertexPathMap.containsKey(currentOriginZone)) {
				vertexPathMap.put(currentOriginZone, new HashMap<Zone, Pair<Double, EdgeSegment>[]>());
			}
			Zone currentDestinationZone = odDemandMatrixIter.getCurrentDestination();
			
			if (((odDemand - DEFAULT_FLOW_EPSILON) > 0.0) && (currentOriginZone.getId() != currentDestinationZone.getId())) {

				PlanItLogger
				        .info("Calculating flow from origin zone " + currentOriginZone.getExternalId()
				        		+ " to destination zone " + currentDestinationZone.getExternalId()
								+ " which has demand of " + df5.format(odDemand));
				vertexPathAndCost = updateVertexPathAndCostUsingShortestPathAlgorithm(previousOriginZoneId, currentOriginZone, vertexPathAndCost, shortestPathAlgorithm);
				double shortestPathCost = getShortestPathCost(vertexPathAndCost, currentOriginZone, currentDestinationZone, modalNetworkSegmentCosts, odDemand, currentModeData);
				dualityGapFunction.increaseConvexityBound(odDemand * shortestPathCost);
				previousOriginZoneId = currentOriginZone.getId();
				
				updateODPath(odPath, currentOriginZone, currentDestinationZone, vertexPathAndCost);
				vertexPathMap.get(currentOriginZone).put(currentDestinationZone, vertexPathAndCost);
			} else {
				vertexPathMap.get(currentOriginZone).put(currentDestinationZone, null);
			}
		}
		return vertexPathMap;
	}
	
	/**
	 * Calculate the route cost for the calculated minimum cost path from a specified origin to a specified destination
	 * 
	 * @param vertexPathAndCost array of Pairs containing the current vertex path and cost
	 * @param currentOriginZone current origin zone
	 * @param currentDestinationZone current destination zone
	 * @param modalNetworkSegmentCosts segment costs for the network
	 * @param odDemand the demands from the specified origin to the specified destination
	 * @param currentModeData          data for the current mode
	 * @return the route cost for the calculated minimum cost path
	 * @throws PlanItException thrown if there is an error
	 */
	private double getShortestPathCost(Pair<Double, EdgeSegment>[] vertexPathAndCost, Zone currentOriginZone, Zone currentDestinationZone, double[] modalNetworkSegmentCosts, double odDemand, ModeData currentModeData) throws PlanItException {
		double shortestPathCost = 0;
		EdgeSegment currentEdgeSegment = null;
		for (Vertex currentPathStartVertex = currentDestinationZone.getCentroid(); currentPathStartVertex.getId() != currentOriginZone.getCentroid().getId(); currentPathStartVertex = currentEdgeSegment.getUpstreamVertex()) {
			int startVertexId = (int) currentPathStartVertex.getId();
			currentEdgeSegment = vertexPathAndCost[startVertexId].getSecond();
			if (currentEdgeSegment == null) {
				if (currentPathStartVertex instanceof Centroid) {
					throw new PlanItException(
							"The solution could not find an Edge Segment for the connectoid for zone "
									+ ((Centroid) currentPathStartVertex).getParentZone().getExternalId());
				} else {
					throw new PlanItException("The solution could not find an Edge Segment for node "
							+ ((Node) currentPathStartVertex).getExternalId());
				}
			}
			int edgeSegmentId = (int) currentEdgeSegment.getId();
			shortestPathCost += modalNetworkSegmentCosts[edgeSegmentId];
			currentModeData.nextNetworkSegmentFlows[edgeSegmentId] += odDemand;
		}
		return shortestPathCost;
	}
	
	/**
	 * Update the OD skim matrix for all active output types 
	 * 
	 * @param skimMatrixMap Map of OD skim matrices for each active output type
	 * @param currentOriginZone current origin zone
	 * @param currentDestinationZone current destination zone
	 * @param vertexPathAndCost array of Pairs containing the current vertex path and cost
	 * @param shortestPathCost 
	 */
//TODO  - vertexPathAndCost not used yet, but may be required for more OD skim matrices for types we have not yet implemented e.g. length, toll etc
/*
	private void updateSkimMatrixMap(Map<ODSkimOutputType, ODSkimMatrix> skimMatrixMap, Zone currentOriginZone, Zone currentDestinationZone, Pair<Double, EdgeSegment>[] vertexPathAndCost, double shortestPathCost) {
		for (ODSkimOutputType odSkimOutputType : simulationData.getActiveSkimOutputTypes()) {
			ODSkimMatrix odSkimMatrix = skimMatrixMap.get(odSkimOutputType);
			switch (odSkimOutputType) {
			case COST:
				odSkimMatrix.setValue(currentOriginZone, currentDestinationZone, shortestPathCost);
				break;
			case NONE:
				odSkimMatrix.setValue(currentOriginZone, currentDestinationZone, shortestPathCost);
				break;
			default:
				break;			
			}
		}
	}
*/
	
	/**
	 * Update the OD skim matrix for all active output types 
	 * 
	 * @param skimMatrixMap Map of OD skim matrices for each active output type
	 * @param odDemandMatrix                origin-demand matrix
	 * @param vertexPathMap    Map of vertex path arrays for each origin and destination zone
	 * @param modalLinkSegmentCosts array of costs for the specified mode
	 */
	private void updateSkimMatrixMap(Map<ODSkimOutputType, ODSkimMatrix> skimMatrixMap, ODDemandMatrix odDemandMatrix, Map<Zone, Map<Zone, Pair<Double, EdgeSegment>[]>> vertexPathMap, double[] modalLinkSegmentCosts) {
		for (ODMatrixIterator odDemandMatrixIter = odDemandMatrix.iterator(); odDemandMatrixIter.hasNext(); ) {
		    double odDemand = odDemandMatrixIter.next();
			Zone currentOriginZone = odDemandMatrixIter.getCurrentOrigin();
			Zone currentDestinationZone = odDemandMatrixIter.getCurrentDestination();
			double routeCost = 0.0;
			if (odDemand > 0.0) {
				Pair<Double, EdgeSegment>[] vertexPath = vertexPathMap.get(currentOriginZone).get(currentDestinationZone);
				routeCost = getRouteCost(currentDestinationZone, vertexPath, modalLinkSegmentCosts);
			}
			for (ODSkimOutputType odSkimOutputType : simulationData.getActiveSkimOutputTypes()) {
				ODSkimMatrix odSkimMatrix = skimMatrixMap.get(odSkimOutputType);
				switch (odSkimOutputType) {
				case COST:
					odSkimMatrix.setValue(currentOriginZone, currentDestinationZone, routeCost);
					break;
				case NONE:
					odSkimMatrix.setValue(currentOriginZone, currentDestinationZone, routeCost);
					break;
				default:
					break;			
				}
			}
		}
	}
	
	/**
	 * Get the cost of a route defined by a vertexPathAndCost object to a specified destination zone
	 * 
	 * @param destinationZone the specified destination zone
	 * @param vertexPathAndCost vertex path defining the route
	 * @param modalLinkSegmentCosts array of costs for the specified mode
	 * @return the cost of the route
	 */
	public double getRouteCost(Zone destinationZone, Pair<Double, EdgeSegment>[] vertexPathAndCost, double[] modalLinkSegmentCosts) {
		double routeCost = 0.0;
		Centroid destinationCentroid = destinationZone.getCentroid();
		EdgeSegment nextSegment = getNextEdgeSegment(vertexPathAndCost, destinationCentroid);
		for (Vertex vertex = destinationCentroid; nextSegment != null; nextSegment = getNextEdgeSegment(vertexPathAndCost, vertex)) {
			vertex = nextSegment.getUpstreamVertex();
			if (vertex instanceof Node) {
				int id = (int) nextSegment.getId();
				routeCost += modalLinkSegmentCosts[id];
			}
		}
		return routeCost;
	}
	
	/**
	 * Get the next edge segment in the route
	 * 
	 * @param vertexPathAndCost vertex path defining the route
	 * @param vertex vertex which will be the downstream vertex of the required edge segment
	 * @return the next edge segment
	 */
	private EdgeSegment getNextEdgeSegment(Pair<Double, EdgeSegment>[] vertexPathAndCost, Vertex vertex) {
		for (int i=0; i<vertexPathAndCost.length; i++) {
			if (vertexPathAndCost[i] != null) {
				if (vertexPathAndCost[i].getSecond() != null) {
					if (vertexPathAndCost[i].getSecond().getDownstreamVertex().getId() == vertex.getId()) {
						return vertexPathAndCost[i].getSecond();
					}
				}
			}
		}
		return null;
	}

	/**
	 * Update the OD path for the specified origin and destination
	 * 
	 * @param odPath the OD path object
	 * @param currentOriginZone the current origin
 	 * @param currentDestinationZone the current destination
	 * @param vertexPathAndCost the vertexPathAndCost object calculated from the traffic assignment
	 */
	private void updateODPath(ODPath odPath, Zone currentOriginZone, Zone currentDestinationZone, Pair<Double, EdgeSegment>[] vertexPathAndCost) {
		odPath.setValue(currentOriginZone, currentDestinationZone, vertexPathAndCost);
	}
	
	/**
	 * Update the current vertex path cost
	 * 
	 * @param previousOriginZoneId the Id of the previous origin zone
	 * @param currentOriginZone the current origin zone
	 * @param vertexPathAndCost array of Pairs containing the current vertex path and cost
	 * @param shortestPathAlgorithm the shortest path algorithm to be used 
	 * @return  the updated array of Pairs containing the current vertex path and cost
	 * @throws PlanItException thrown if edge segments have not been assigned to a zone
	 */
	private Pair<Double, EdgeSegment>[] updateVertexPathAndCostUsingShortestPathAlgorithm(long previousOriginZoneId, Zone currentOriginZone, Pair<Double, EdgeSegment>[] vertexPathAndCost, ShortestPathAlgorithm shortestPathAlgorithm) throws PlanItException {
		if (previousOriginZoneId != currentOriginZone.getId()) {
			Centroid originCentroid = currentOriginZone.getCentroid();
			if (originCentroid.exitEdgeSegments.isEmpty()) {
				throw new PlanItException("Edge segments have not been assigned to Centroid for Zone " + (currentOriginZone.getExternalId()));
			}
			vertexPathAndCost = shortestPathAlgorithm.executeOneToAll(originCentroid);
		}
		return vertexPathAndCost;
	}
	
	/**
	 * Perform assignment for a given time period using Dijkstra's algorithm
	 * 
	 * @param timePeriod the time period for the current assignment
	 * @throws PlanItException thrown if there is an error
	 */
	private void executeTimePeriod(TimePeriod timePeriod) throws PlanItException {
		PlanItLogger.info("Running Traditional Static Assigment over all modes for Time Period " + timePeriod.getDescription());
		Set<Mode> modes = demands.getRegisteredModesForTimePeriod(timePeriod);
		initialiseTimePeriod(modes);
		LinkBasedRelativeDualityGapFunction dualityGapFunction = ((LinkBasedRelativeDualityGapFunction) getGapFunction());
		Calendar startTime = Calendar.getInstance();
		Calendar initialStartTime = startTime;
		for (Mode mode : modes) {
			double[] modalLinkSegmentCosts = initializeModalLinkSegmentCosts(mode, timePeriod);
			simulationData.setModalLinkSegmentCosts(mode, modalLinkSegmentCosts);
		}
		Map<Mode, Map<Zone, Map<Zone, Pair<Double, EdgeSegment>[]>>> vertexPathMapPerMode = new HashMap<Mode, Map<Zone, Map<Zone, Pair<Double, EdgeSegment>[]>>>();
		boolean converged = false;
		while (!converged) {
			dualityGapFunction.reset();
			smoothing.update(simulationData.getIterationIndex());
			
			// NETWORK LOADING - PER MODE
			for (Mode mode : modes) {
				simulationData.resetSkimMatrix(mode, getTransportNetwork().zones);
				double[] modalLinkSegmentCosts = simulationData.getModalLinkSegmentCosts(mode);
				simulationData.resetModalNetworkSegmentFlows(mode, numberOfNetworkSegments);
				Map<Zone, Map<Zone, Pair<Double, EdgeSegment>[]>> vertexPathMap = executeAndSmoothTimePeriodAndMode(timePeriod, mode, modalLinkSegmentCosts);
				vertexPathMapPerMode.put(mode, vertexPathMap);
			}

			dualityGapFunction.computeGap();
			simulationData.incrementIterationIndex();
			startTime = recordTime(startTime, dualityGapFunction.getGap());
			converged = dualityGapFunction.hasConverged(simulationData.getIterationIndex());
			for (Mode mode : modes) {
				double[] modalLinkSegmentCosts = recalculateModalLinkSegmentCosts(mode, timePeriod);
				simulationData.setModalLinkSegmentCosts(mode, modalLinkSegmentCosts);
				ODDemandMatrix odDemandMatrix = demands.get(mode, timePeriod);
				Map<ODSkimOutputType, ODSkimMatrix> skimMatrixMap = simulationData.getSkimMatrixMap(mode);
				updateSkimMatrixMap(skimMatrixMap, odDemandMatrix, vertexPathMapPerMode.get(mode), modalLinkSegmentCosts);
			}		
			outputManager.persistOutputData(timePeriod, modes, converged);
		}
		long timeDiff = startTime.getTimeInMillis() - initialStartTime.getTimeInMillis();
		PlanItLogger.info("Assignment took " + timeDiff + " milliseconds");
	}
	
	/**
	 * Record the time an iteration took
	 * 
	 * @param startTime the original start time of the iteration
	 * @param dualityGap the duality gap at the end of the iteration
	 * @return the time at the end of the iteration
	 */
	private Calendar recordTime(Calendar startTime, double dualityGap) {
		Calendar currentTime = Calendar.getInstance();
		long timeDiff = currentTime.getTimeInMillis() - startTime.getTimeInMillis();
		PlanItLogger.info("Iteration " + simulationData.getIterationIndex() + ": Duality gap = "
				+ df6.format(dualityGap) + ": Iteration duration " + timeDiff + " milliseconds");
		return currentTime;
	}

	/**
	 * Execute the assignment for the current time period and mode and apply
	 * smoothing to the result
	 * 
	 * @param timePeriod               the current time period
	 * @param mode                     the current mode
	 * @param modalNetworkSegmentCosts the current network segment costs
	 * @return Map of array of Pairs containing the current vertex path and cost for each origin and destination
	 * @throws PlanItException thrown if there is an error
	 */
	private Map<Zone, Map<Zone, Pair<Double, EdgeSegment>[]>> executeAndSmoothTimePeriodAndMode(TimePeriod timePeriod, Mode mode, double[] modalNetworkSegmentCosts)	throws PlanItException {
		PlanItLogger.info("Running Traditional Static Assignment for Mode " + mode.getName());
		// mode specific data
		ModeData currentModeData = simulationData.getModeSpecificData().get(mode);
		currentModeData.resetNextNetworkSegmentFlows();
		LinkBasedRelativeDualityGapFunction dualityGapFunction = ((LinkBasedRelativeDualityGapFunction) getGapFunction());
		// AON based network loading
		ShortestPathAlgorithm shortestPathAlgorithm = new DijkstraShortestPathAlgorithm(modalNetworkSegmentCosts, numberOfNetworkSegments, numberOfNetworkVertices);
		ODDemandMatrix odDemandMatrix = demands.get(mode, timePeriod);
		Map<Zone, Map<Zone, Pair<Double, EdgeSegment>[]>> vertexPathMap = executeModeTimePeriod(mode, odDemandMatrix, currentModeData, modalNetworkSegmentCosts, shortestPathAlgorithm);
		double totalModeSystemTravelTime = ArrayOperations.dotProduct(currentModeData.currentNetworkSegmentFlows,	modalNetworkSegmentCosts, numberOfNetworkSegments);
		dualityGapFunction.increaseActualSystemTravelTime(totalModeSystemTravelTime);
		applySmoothing(currentModeData);
		// aggregate smoothed mode specific flows - for cost computation
		ArrayOperations.addTo(simulationData.getModalNetworkSegmentFlows(mode), currentModeData.currentNetworkSegmentFlows, numberOfNetworkSegments);
		simulationData.getModeSpecificData().put(mode, currentModeData);
		return vertexPathMap;
	}

	/**
	 * Populate the current segment costs for connectoids
	 * 
	 * @param mode                current mode of travel
	 * @param currentSegmentCosts array to store the current segment costs
	 * @throws PlanItException thrown if there is an error
	 */
	private void populateModalConnectoidCosts(Mode mode, double[] currentSegmentCosts) throws PlanItException {
		for (ConnectoidSegment currentSegment : transportNetwork.connectoidSegments.toList()) {
			currentSegmentCosts[(int) currentSegment.getId()] = virtualCost.getSegmentCost(mode, currentSegment);
		}
	}

	/**
	 * Calculate and store the link segment costs for links for the current iteration
	 * 
	 * This method is called during the second and subsequent iterations of the
	 * simulation.
	 * 
	 * @param mode                current mode of travel
	 * @param currentSegmentCosts array to store the current segment costs
	 * @throws PlanItException thrown if there is an error
	 */
	private void calculateModalLinkSegmentCosts(Mode mode, double[] currentSegmentCosts) throws PlanItException {
		setModalLinkSegmentCosts(mode, currentSegmentCosts, (linkSegment) -> {
			return physicalCost.getSegmentCost(mode, linkSegment);
		});
	}

	/**
	 * Initialize the link segment costs from the InitialLinkSegmentCost object for
	 * all time periods
	 * 
	 * This method is called during the first iteration of the simulation.
	 * 
	 * @param mode                current mode of travel
	 * @param currentSegmentCosts array to store the current segment costs
	 * @return false if the initial costs cannot be set for this mode, true
	 *         otherwise
	 * @throws PlanItException thrown if there is an error
	 */
	private boolean initializeModalLinkSegmentCostsForAllTimePeriods(Mode mode, double[] currentSegmentCosts) throws PlanItException {
		if (!initialLinkSegmentCost.isSegmentCostsSetForMode(mode)) {
			return false;
		}
		setModalLinkSegmentCosts(mode, currentSegmentCosts, (linkSegment) -> {
			return initialLinkSegmentCost.getSegmentCost(mode, linkSegment);
		});
		return true;
	}

	/**
	 * Initialize the link segment costs from the InitialLinkSegmentCost object for
	 * each current time period
	 * 
	 * This method is called during the first iteration of the simulation.
	 * 
	 * @param mode                current mode of travel
	 * @param timePeriod          current time period
	 * @param currentSegmentCosts array to store the current segment costs
	 * @return false if the initial costs cannot be set for this mode and time
	 *         period, true otherwise
	 * @throws PlanItException thrown if there is an error
	 */
	private boolean initializeModalLinkSegmentCostsByTimePeriod(Mode mode, TimePeriod timePeriod, double[] currentSegmentCosts)
			throws PlanItException {
		InitialLinkSegmentCost initialLinkSegmentCostForTimePeriod = initialLinkSegmentCostByTimePeriod
				.get(timePeriod.getId());
		if (!initialLinkSegmentCostForTimePeriod.isSegmentCostsSetForMode(mode)) {
			return false;
		}
		setModalLinkSegmentCosts(mode, currentSegmentCosts, (linkSegment) -> {
			InitialLinkSegmentCost initialLinkSegmentCostLocal = initialLinkSegmentCostByTimePeriod
					.get(timePeriod.getId());
			return initialLinkSegmentCostLocal.getSegmentCost(mode, linkSegment);
		});
		return true;
	}

	/**
	 * Set the link segment costs
	 * 
	 * @param mode                 current mode of travel
	 * @param currentSegmentCosts  array to store the current segment costs
	 * @param calculateSegmentCost lambda function to specify how the costs should
	 *                             be calculated
	 * @throws PlanItException thrown if there is an error
	 */
	private void setModalLinkSegmentCosts(Mode mode, double[] currentSegmentCosts,
			Function<LinkSegment, Double> calculateSegmentCost) throws PlanItException {
		for (LinkSegment linkSegment : transportNetwork.linkSegments.toList()) {
			if (linkSegment.getMaximumSpeed(mode.getExternalId()) == 0.0) {
				currentSegmentCosts[(int) linkSegment.getId()] = Double.POSITIVE_INFINITY;
			} else {
				double currentSegmentCost = calculateSegmentCost.apply(linkSegment);
				if (currentSegmentCost < 0.0) {
					throw new PlanItException("Error during calculation of link segment costs");
				}
				currentSegmentCosts[(int) linkSegment.getId()] = currentSegmentCost;
			}
		}
	}
	
	/**
	 * Open or close output formatters
	 * 
	 * @param action lambda function to open or close a formatter
	 * @throws PlanItException thrown if there is an error opening or closing the file
	 */
	private void openOrClose(BiFunction<OutputFormatter, OutputTypeConfiguration, PlanItException> action)	throws PlanItException {
		List<OutputTypeConfiguration> outputTypeConfigurations = outputManager.getRegisteredOutputTypeConfigurations();
		for (OutputTypeConfiguration outputTypeConfiguration : outputTypeConfigurations) {
			List<OutputFormatter> outputFormatters = outputManager.getOutputFormatters();
			for (OutputFormatter outputFormatter : outputFormatters) {
				PlanItException e = action.apply(outputFormatter, outputTypeConfiguration);
				if (e != null) {
					throw e;
				}
			}
		}
	}

	/**
	 * Initialize members and output resources before equilibration
	 * 
	 * @throws PlanItException thrown if there is an error
	 * 
	 */
	@Override
	protected void initialiseTrafficAssignmentBeforeEquilibration() throws PlanItException {
		// initialize members that are used throughout the assignment
		this.numberOfNetworkSegments = getTransportNetwork().getTotalNumberOfEdgeSegments();
		this.numberOfNetworkVertices = getTransportNetwork().getTotalNumberOfVertices();
		physicalCost.initialiseCostsBeforeEquilibration(physicalNetwork);
		openOrClose((outputFormatter, outputTypeConfiguration) -> {
			try {
				outputFormatter.open(outputTypeConfiguration, getId());
			} catch (Exception e) {
				return new PlanItException(e);
			}
			return null;
		});
	}

	/**
	 * Close output resources after equilibration
	 * 
	 * @throws PlanItException thrown if there is an error closing resources
	 * 
	 */
	@Override
	protected void finalizeAfterEquilibration() throws PlanItException {
		openOrClose((outputFormatter, outputTypeConfiguration) -> {
			try {
				outputFormatter.close(outputTypeConfiguration);
			} catch (Exception e) {
				return new PlanItException(e);
			}
			return null;
		});
	}

	/**
	 * Create the Gap Function used by this Traffic Assignment
	 * 
	 * @return GapFunction created
	 */
	protected GapFunction createGapFunction() {
		return new LinkBasedRelativeDualityGapFunction(new StopCriterion());
	}

	/**
	 * Base Constructor
	 */
	public TraditionalStaticAssignment() {
		super();
		simulationData = null;
	}

	/**
	 * Collect the updated edge segment costs for the given mode and time period
	 * 
	 * @param mode           the current mode
	 * @param timePeriod     the current time period
	 * @param iterationIndex index of the current iteration
	 * @return array of updated edge segment costs
	 * @throws PlanItException thrown if there is an error
	 */
	public double[] getModalLinkSegmentCosts(Mode mode, TimePeriod timePeriod, int iterationIndex)
			throws PlanItException {
		double[] currentSegmentCosts = new double[transportNetwork.getTotalNumberOfEdgeSegments()];
		populateModalConnectoidCosts(mode, currentSegmentCosts);
		if (iterationIndex == 0) {
			if (initialLinkSegmentCostByTimePeriod.containsKey(timePeriod.getId())) {
				if (initializeModalLinkSegmentCostsByTimePeriod(mode, timePeriod, currentSegmentCosts)) {
					return currentSegmentCosts;
				}
			} else if (initialLinkSegmentCost != null) {
				if (initializeModalLinkSegmentCostsForAllTimePeriods(mode, currentSegmentCosts)) {
					return currentSegmentCosts;
				}
			}
		}
		calculateModalLinkSegmentCosts(mode, currentSegmentCosts);
		return currentSegmentCosts;
	}
	
	/**
	 * Recalculate the modal link segment costs after each iteration
	 * 
	 * @param mode current mode
	 * @param timePeriod current time period
	 * @return array containing link costs for each link segment
	 * @throws PlanItException
	 */
	public double[] recalculateModalLinkSegmentCosts(Mode mode, TimePeriod timePeriod) throws PlanItException {
		double[] currentSegmentCosts = new double[transportNetwork.getTotalNumberOfEdgeSegments()];
		populateModalConnectoidCosts(mode, currentSegmentCosts);
		calculateModalLinkSegmentCosts(mode, currentSegmentCosts);
		return currentSegmentCosts;
	}
		
	/**
	 * Initialize the modal link segment costs before the first iteration.
	 * 
	 * This method uses initial link segment costs if they have been input, otherwise these are calculated from zero start values
	 * 
	 * @param mode current mode
	 * @param timePeriod current time period
	 * @return array containing link costs for each link segment
	 * @throws PlanItException
	 */
	public double[] initializeModalLinkSegmentCosts(Mode mode, TimePeriod timePeriod) throws PlanItException {
		double[] currentSegmentCosts = new double[transportNetwork.getTotalNumberOfEdgeSegments()];
		populateModalConnectoidCosts(mode, currentSegmentCosts);
		if (initialLinkSegmentCostByTimePeriod.containsKey(timePeriod.getId())) {
			if (initializeModalLinkSegmentCostsByTimePeriod(mode, timePeriod, currentSegmentCosts)) {
				return currentSegmentCosts;
			}
		} else if (initialLinkSegmentCost != null) {
			if (initializeModalLinkSegmentCostsForAllTimePeriods(mode, currentSegmentCosts)) {
				return currentSegmentCosts;
			}
		}
		calculateModalLinkSegmentCosts(mode, currentSegmentCosts);
		return currentSegmentCosts;
	}
	
	/**
	 * Execute equilibration over all time periods and modes
	 * 
	 * @throws PlanItException thrown if there is an error
	 */
	@Override
	public void executeEquilibration() throws PlanItException {
		// perform assignment per period - per mode
		Set<TimePeriod> timePeriods = demands.getRegisteredTimePeriods();
		PlanItLogger.info("There are " + timePeriods.size() + " time periods to loop through.");
		for (TimePeriod timePeriod : timePeriods) {
			PlanItLogger.info("Equilibrating time period " + timePeriod.toString());
			executeTimePeriod(timePeriod);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.planit.interactor.LinkVolumeInteractor.LinkVolumeAccessee#
	 * getTotalNetworkSegmentFlows()
	 */

	@Override
	public double getTotalNetworkSegmentFlow(LinkSegment linkSegment) {
		return simulationData.getTotalNetworkSegmentFlow(linkSegment);
	}

	@Override
	public double[] getModalNetworkSegmentFlows(Mode mode) {
		return simulationData.getModalNetworkSegmentFlows(mode);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.planit.interactor.LinkVolumeInteractor.LinkVolumeAccessee#
	 * getNumberOfLinkSegments()
	 */
	@Override
	public int getNumberOfLinkSegments() {
		return getTransportNetwork().getTotalNumberOfLinkSegments();
	}

	@Override
	public void onRequestInteractorEvent(RequestAccesseeEvent event) {
		if (event.getSourceAccessor().getRequestedAccessee().equals(LinkVolumeAccessee.class)) {
			event.getSourceAccessor().setAccessee(this);
		}
	}
	
	/**
	 * Create the output type adapter for the current output type
	 * 
	 * @param outputType the current output type
	 * @return the output type adapter corresponding to the current traffic assignment and output type
	 */
	@Override
	public OutputTypeAdapter createOutputTypeAdapter(OutputType outputType) {
		OutputTypeAdapter outputTypeAdapter = null;
		switch (outputType) {
        case LINK: 
         	outputTypeAdapter = new TraditionalStaticAssignmentLinkOutputTypeAdapter(outputType, this);
        break;
        case OD: 
        	outputTypeAdapter = new TraditionalStaticAssignmentODOutputTypeAdapter(outputType, this);
        break;
        case OD_PATH:
        	outputTypeAdapter = new TraditionalStaticODPathOutputTypeAdapter(outputType, this);
        break;
        default: PlanItLogger.warning(outputType.value() + " has not been defined yet.");
		}
		return outputTypeAdapter;
	}

	/**
	 * Return the simulation data for the current iteration
	 * 
	 * @return simulation data
	 */
	public SimulationData getSimulationData() {
		return simulationData;
	}

}
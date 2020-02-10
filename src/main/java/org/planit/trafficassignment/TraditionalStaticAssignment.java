package org.planit.trafficassignment;

import java.util.Calendar;
import java.util.Map;
import java.util.Set;
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
import org.planit.od.odpath.ODPathMatrix;
import org.planit.output.adapter.OutputTypeAdapter;
import org.planit.output.adapter.TraditionalStaticAssignmentLinkOutputTypeAdapter;
import org.planit.output.adapter.TraditionalStaticAssignmentODOutputTypeAdapter;
import org.planit.output.adapter.TraditionalStaticPathOutputTypeAdapter;
import org.planit.output.configuration.OutputConfiguration;
import org.planit.output.enums.ODSkimSubOutputType;
import org.planit.output.enums.OutputType;
import org.planit.time.TimePeriod;
import org.planit.userclass.Mode;
import org.planit.utils.ArrayOperations;
import org.planit.utils.FormatUtils;
import org.planit.utils.Pair;
import org.planit.zoning.Zone;

/**
 * Traditional static assignment traffic component. 
 * Provides configuration access via the CapacityRestrainedTrafficAssignmentBuilder it instantiates
 * 
 * @author markr, gman6028
 *
 */
public class TraditionalStaticAssignment extends CapacityRestrainedAssignment
		implements LinkVolumeAccessee, InteractorListener {

	/**
	 * Epsilon margin when comparing flow rates (veh/h)
	 */
	private static final double DEFAULT_FLOW_EPSILON = 0.000001;

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
	 * @param odDemandMatrix           origin-demand matrix
	 * @param currentModeData          data for the current mode
	 * @param modalNetworkSegmentCosts segment costs for the network
	 * @param shortestPathAlgorithm    shortest path algorithm to be used
	 * @throws PlanItException thrown if there is an error
	 */
	private void executeModeTimePeriod(Mode mode, ODDemandMatrix odDemandMatrix, ModeData currentModeData,
			double[] modalNetworkSegmentCosts, ShortestPathAlgorithm shortestPathAlgorithm) throws PlanItException {
		LinkBasedRelativeDualityGapFunction dualityGapFunction = ((LinkBasedRelativeDualityGapFunction) getGapFunction());
		ODPathMatrix odPathMatrix = simulationData.getODPathMatrix(mode);
		Map<ODSkimSubOutputType, ODSkimMatrix> skimMatrixMap = simulationData.getSkimMatrixMap(mode);

		// loop over all available OD demands
		long previousOriginZoneId = -1;
		// track the cost to reach each vertex in the network and the shortest path
		// segment used to get there
		Pair<Double, EdgeSegment>[] vertexPathCosts = null;
		for (ODMatrixIterator odDemandMatrixIter = odDemandMatrix.iterator(); odDemandMatrixIter.hasNext();) {
			double odDemand = odDemandMatrixIter.next();
			Zone currentOriginZone = odDemandMatrixIter.getCurrentOrigin();
			Zone currentDestinationZone = odDemandMatrixIter.getCurrentDestination();

			if (((odDemand - DEFAULT_FLOW_EPSILON) > 0.0)
					&& (currentOriginZone.getId() != currentDestinationZone.getId())) {

				PlanItLogger.fine("Calculating flow from origin zone " + currentOriginZone.getExternalId()
						+ " to destination zone " + currentDestinationZone.getExternalId() + " which has demand of "
						+ FormatUtils.format5(odDemand) +  " for mode " + mode.getExternalId());

				// MARK 6-1-2020
				// extracted this from separated method (method removed);
				// we do to many things here (path search, path storage), better to leave it
				// exposed at this level so we know where this functionality occurs
				// when origin is updated we conduct ONE-TO-ALL shortest path search
				if (previousOriginZoneId != currentOriginZone.getId()) {

					Centroid originCentroid = currentOriginZone.getCentroid();
					if (originCentroid.exitEdgeSegments.isEmpty()) {
						throw new PlanItException("Edge segments have not been assigned to Centroid for Zone "
								+ (currentOriginZone.getExternalId()));
					}
					// UPDATE SHORTEST PATHS
					vertexPathCosts = shortestPathAlgorithm.executeOneToAll(originCentroid);
				}

				// MARK 6-1-2020
				// store path to current destination when needed
				if (outputManager.isOutputTypeActive(OutputType.PATH)) {
					odPathMatrix.createAndSavePath(currentOriginZone, currentDestinationZone, vertexPathCosts);
				}

				double odShortestPathCost = getShortestPathCost(vertexPathCosts, currentOriginZone,
						currentDestinationZone, modalNetworkSegmentCosts, odDemand, currentModeData);
				dualityGapFunction.increaseConvexityBound(odDemand * odShortestPathCost);
				previousOriginZoneId = currentOriginZone.getId();

				// MARK 6-1-2020
				// Here we should store the skim matrix information for travel time (generalized path cost) for i-1
				// So when we are in the first iteration (i=1), the current vertexPathCosts
				// array provides us the path cost information to store
				// the row of the current origin for all destinations for i=(1-1)=0
				//
				// See how this obviates the need to store the paths (if we are not interested
				// in paths)
				//
				// NOTE: do NOT use the vertex path map to store costs! Only use it to store
				// paths, i.e., edge segments,

				updateSkimMatrixMap(skimMatrixMap, currentOriginZone, currentDestinationZone, odDemand, vertexPathCosts);
			}
		}
	}

	/**
	 * Calculate the route cost for the calculated minimum cost path from a
	 * specified origin to a specified destination
	 * 
	 * @param vertexPathAndCost        array of Pairs containing the current vertex
	 *                                 path and cost
	 * @param currentOriginZone        current origin zone
	 * @param currentDestinationZone   current destination zone
	 * @param modalNetworkSegmentCosts segment costs for the network
	 * @param odDemand                 the demands from the specified origin to the
	 *                                 specified destination
	 * @param currentModeData          data for the current mode
	 * @return the route cost for the calculated minimum cost path
	 * @throws PlanItException thrown if there is an error
	 */
	private double getShortestPathCost(Pair<Double, EdgeSegment>[] vertexPathAndCost, Zone currentOriginZone,
			Zone currentDestinationZone, double[] modalNetworkSegmentCosts, double odDemand, ModeData currentModeData)
			throws PlanItException {
		double shortestPathCost = 0;
		EdgeSegment currentEdgeSegment = null;
		for (Vertex currentPathStartVertex = currentDestinationZone.getCentroid(); currentPathStartVertex
				.getId() != currentOriginZone.getCentroid()
						.getId(); currentPathStartVertex = currentEdgeSegment.getUpstreamVertex()) {
			int startVertexId = (int) currentPathStartVertex.getId();
			currentEdgeSegment = vertexPathAndCost[startVertexId].getSecond();
			if (currentEdgeSegment == null) {
				if (currentPathStartVertex instanceof Centroid) {
					throw new PlanItException("The solution could not find an Edge Segment for the connectoid for zone "
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
	 * @param skimMatrixMap          Map of OD skim matrices for each active output
	 *                               type
	 * @param currentOriginZone      current origin zone
	 * @param currentDestinationZone current destination zone
	 * @param vertexPathCosts        array of costs for the specified mode
	 */
	private void updateSkimMatrixMap(Map<ODSkimSubOutputType, ODSkimMatrix> skimMatrixMap, Zone currentOriginZone,
			Zone currentDestinationZone, double odDemand, Pair<Double, EdgeSegment>[] vertexPathCosts) {
		for (ODSkimSubOutputType odSkimOutputType : simulationData.getActiveSkimOutputTypes()) {
			if (odSkimOutputType.equals(ODSkimSubOutputType.COST)) {
				double odGeneralisedCost = -1;
				if (odDemand > 0.0) {
					// Collect cost to get to vertex from shortest path ONE-TO-ALL information
					// directly
					long destinationVertexId = currentDestinationZone.getCentroid().getId();
					Pair<Double, EdgeSegment> vertexPathCost = vertexPathCosts[(int) destinationVertexId];
					odGeneralisedCost = vertexPathCost.getFirst();
				}
				ODSkimMatrix odSkimMatrix = skimMatrixMap.get(odSkimOutputType);
				odSkimMatrix.setValue(currentOriginZone, currentDestinationZone, odGeneralisedCost);
			}
		}
	}

	/**
	 * Perform assignment for a given time period using Dijkstra's algorithm
	 * 
	 * @param timePeriod the time period for the current assignment
	 * @throws PlanItException thrown if there is an error
	 */
	private void executeTimePeriod(TimePeriod timePeriod) throws PlanItException {
		PlanItLogger.info(
				"Running Traditional Static Assignment over all modes for Time Period " + timePeriod.getDescription());
		Set<Mode> modes = demands.getRegisteredModesForTimePeriod(timePeriod);
		initialiseTimePeriod(modes);
		LinkBasedRelativeDualityGapFunction dualityGapFunction = ((LinkBasedRelativeDualityGapFunction) getGapFunction());
		Calendar startTime = Calendar.getInstance();
		Calendar initialStartTime = startTime;
		for (Mode mode : modes) {
			double[] modalLinkSegmentCosts = initializeModalLinkSegmentCosts(mode, timePeriod);
			simulationData.setModalLinkSegmentCosts(mode, modalLinkSegmentCosts);
		}

		boolean converged = false;
		while (!converged) {
			dualityGapFunction.reset();
			smoothing.update(simulationData.getIterationIndex());

			// NETWORK LOADING - PER MODE
			for (Mode mode : modes) {
				simulationData.resetSkimMatrix(mode, getTransportNetwork().zones);
				simulationData.resetPathMatrix(mode, getTransportNetwork().zones);
				simulationData.resetModalNetworkSegmentFlows(mode, numberOfNetworkSegments);

				double[] modalLinkSegmentCosts = simulationData.getModalLinkSegmentCosts(mode);
				executeAndSmoothTimePeriodAndMode(timePeriod, mode, modalLinkSegmentCosts);
			}

			dualityGapFunction.computeGap();
			simulationData.incrementIterationIndex();
			PlanItLogger.info("The total system travel time after iteration " + simulationData.getIterationIndex() + " for time period " +  timePeriod.getExternalId() + " is " + dualityGapFunction.getActualSystemTravelTime() + ".");
			startTime = recordTime(startTime, dualityGapFunction.getGap());
			for (Mode mode : modes) {
				double[] modalLinkSegmentCosts = recalculateModalLinkSegmentCosts(mode, timePeriod);
				simulationData.setModalLinkSegmentCosts(mode, modalLinkSegmentCosts);
			}
			converged = dualityGapFunction.hasConverged(simulationData.getIterationIndex());
			outputManager.persistOutputData(timePeriod, modes, converged);
		}
		long timeDiff = startTime.getTimeInMillis() - initialStartTime.getTimeInMillis();
		PlanItLogger.info("Assignment took " + timeDiff + " milliseconds");
	}

	/**
	 * Record the time an iteration took
	 * 
	 * @param startTime  the original start time of the iteration
	 * @param dualityGap the duality gap at the end of the iteration
	 * @return the time at the end of the iteration
	 */
	private Calendar recordTime(Calendar startTime, double dualityGap) {
		Calendar currentTime = Calendar.getInstance();
		long timeDiff = currentTime.getTimeInMillis() - startTime.getTimeInMillis();
		PlanItLogger.info("Iteration " + simulationData.getIterationIndex() + ": Duality gap = "
				+ FormatUtils.format6(dualityGap) + ": Iteration duration " + timeDiff + " milliseconds");
		return currentTime;
	}

	/**
	 * Execute the assignment for the current time period and mode and apply
	 * smoothing to the result
	 * 
	 * @param timePeriod               the current time period
	 * @param mode                     the current mode
	 * @param modalNetworkSegmentCosts the current network segment costs
	 * @throws PlanItException thrown if there is an error
	 */
	private void executeAndSmoothTimePeriodAndMode(TimePeriod timePeriod, Mode mode, double[] modalNetworkSegmentCosts)
			throws PlanItException {
		PlanItLogger.info("Running Traditional Static Assignment for Mode " + mode.getName());
		// mode specific data
		ModeData currentModeData = simulationData.getModeSpecificData().get(mode);
		currentModeData.resetNextNetworkSegmentFlows();
		LinkBasedRelativeDualityGapFunction dualityGapFunction = ((LinkBasedRelativeDualityGapFunction) getGapFunction());

		// AON based network loading
		ShortestPathAlgorithm shortestPathAlgorithm = new DijkstraShortestPathAlgorithm(modalNetworkSegmentCosts,
				numberOfNetworkSegments, numberOfNetworkVertices);
		ODDemandMatrix odDemandMatrix = demands.get(mode, timePeriod);
		executeModeTimePeriod(mode, odDemandMatrix, currentModeData, modalNetworkSegmentCosts, shortestPathAlgorithm);

		double totalModeSystemTravelTime = ArrayOperations.dotProduct(currentModeData.currentNetworkSegmentFlows,
				modalNetworkSegmentCosts, numberOfNetworkSegments);
		dualityGapFunction.increaseActualSystemTravelTime(totalModeSystemTravelTime);
		applySmoothing(currentModeData);

		// aggregate smoothed mode specific flows - for cost computation
		ArrayOperations.addTo(simulationData.getModalNetworkSegmentFlows(mode),
				currentModeData.currentNetworkSegmentFlows, numberOfNetworkSegments);
		simulationData.getModeSpecificData().put(mode, currentModeData);
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
	 * Calculate and store the link segment costs for links for the current
	 * iteration
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
	private boolean initializeModalLinkSegmentCostsForAllTimePeriods(Mode mode, double[] currentSegmentCosts)
			throws PlanItException {
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
	private boolean initializeModalLinkSegmentCostsByTimePeriod(Mode mode, TimePeriod timePeriod,
			double[] currentSegmentCosts) throws PlanItException {
		InitialLinkSegmentCost initialLinkSegmentCostForTimePeriod = initialLinkSegmentCostByTimePeriod.get(timePeriod.getId());
		if (!initialLinkSegmentCostForTimePeriod.isSegmentCostsSetForMode(mode)) {
			return false;
		}
		setModalLinkSegmentCosts(mode, currentSegmentCosts, (linkSegment) -> {
			InitialLinkSegmentCost initialLinkSegmentCostLocal = initialLinkSegmentCostByTimePeriod.get(timePeriod.getId());
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
			//if (linkSegment.getMaximumSpeed(mode) == 0.0) {
			//	currentSegmentCosts[(int) linkSegment.getId()] = Double.POSITIVE_INFINITY;
			//} else {
				double currentSegmentCost = calculateSegmentCost.apply(linkSegment);
				if (currentSegmentCost < 0.0) {
					throw new PlanItException("Error during calculation of link segment costs");
				}
				currentSegmentCosts[(int) linkSegment.getId()] = currentSegmentCost;
		//	}
		}
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
	 * @param mode       current mode
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
	 * This method uses initial link segment costs if they have been input,
	 * otherwise these are calculated from zero start values
	 * 
	 * @param mode       current mode
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
	 * @return the output type adapter corresponding to the current traffic
	 *         assignment and output type
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
		case PATH:
			outputTypeAdapter = new TraditionalStaticPathOutputTypeAdapter(outputType, this);
			break;
		default:
			PlanItLogger.warning(outputType.value() + " has not been defined yet.");
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
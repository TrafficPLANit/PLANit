package org.planit.trafficassignment;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
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
import org.planit.network.transport.TransportNetwork;
import org.planit.network.virtual.Centroid;
import org.planit.network.virtual.ConnectoidSegment;
import org.planit.od.odmatrix.ODMatrixIterator;
import org.planit.od.odmatrix.demand.ODDemandMatrix;
import org.planit.output.adapter.OutputAdapter;
import org.planit.output.adapter.TraditionalStaticAssignmentLinkOutputAdapter;
import org.planit.output.adapter.TraditionalStaticAssignmentODOutputAdapter;
import org.planit.output.configuration.OutputConfiguration;
import org.planit.output.configuration.OutputTypeConfiguration;
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
	 */
	private void initialiseTimePeriod(Set<Mode> modes) {
		OutputConfiguration outputConfiguration = outputManager.getOutputConfiguration();
		simulationData = new TraditionalStaticAssignmentSimulationData(outputConfiguration);
		simulationData.setEmptySegmentArray(new double[numberOfNetworkSegments]);
		simulationData.setConverged(false);
		simulationData.setIterationIndex(0);
		simulationData.getModeSpecificData().clear();
		for (Mode mode : modes) {
			simulationData.resetModalNetworkSegmentFlows(mode);
			simulationData.getModeSpecificData().put(mode, new ModeData(simulationData.getEmptySegmentArray()));
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
	 * @param odDemands                origin-demand store
	 * @param currentModeData          data for the current mode
	 * @param modalNetworkSegmentCosts segment costs for the network
	 * @param shortestPathAlgorithm    shortest path algorithm to be used
	 * @throws PlanItException thrown if there is an error
	 */
	private void executeModeTimePeriod(Mode mode, ODDemandMatrix odDemandMatrix, ModeData currentModeData,
			double[] modalNetworkSegmentCosts, ShortestPathAlgorithm shortestPathAlgorithm) throws PlanItException {
		ODMatrixIterator odDemandMatrixIter = odDemandMatrix.iterator();
		TransportNetwork network = getTransportNetwork();
		LinkBasedRelativeDualityGapFunction dualityGapFunction = ((LinkBasedRelativeDualityGapFunction) getGapFunction());

		// loop over all available OD demands
		long previousOriginZoneId = -1;
		Pair<Double, EdgeSegment>[] vertexPathCost = null;
		while (odDemandMatrixIter.hasNext()) {
			double odDemand = odDemandMatrixIter.next();
			Zone currentOriginZone = odDemandMatrixIter.getCurrentOrigin();
			long originZoneId = currentOriginZone.getId();
			Zone currentDestinationZone = odDemandMatrixIter.getCurrentDestination();
			long destinationZoneId = currentDestinationZone.getId();

			if (((odDemand - DEFAULT_FLOW_EPSILON) > 0.0) && (originZoneId != destinationZoneId)) {

				PlanItLogger
						.fine("Calculating flow from origin zone " + network.zones.getZone(originZoneId).getExternalId()
								+ " to destination zone " + network.zones.getZone(destinationZoneId).getExternalId()
								+ " which has demand of " + df5.format(odDemand));

				// UPDATE ORIGIN BASED: SHORTEST PATHS - ONE-TO-ALL
				if (previousOriginZoneId != originZoneId) {
					Centroid originCentroid = currentOriginZone.getCentroid();

					if (originCentroid.exitEdgeSegments.isEmpty()) {
						throw new PlanItException("Edge segments have not been assigned to Centroid for Zone "
								+ (originCentroid.getParentZone().getExternalId()));
					}
					vertexPathCost = shortestPathAlgorithm.executeOneToAll(originCentroid);
				}

				// UPDATE DESTINATION ZONE
				// TODO: Costly to lookup destination zone via map whereas we know it is the
				// next (non-zero demand) id compared to the previous)
				// OD-SHORTEST PATH LOADING
				double shortestPathCost = 0;
				Vertex currentPathStartVertex = currentDestinationZone.getCentroid();

				while (currentPathStartVertex.getId() != currentOriginZone.getCentroid().getId()) {
					int startVertexId = (int) currentPathStartVertex.getId();
					if (vertexPathCost[startVertexId].getSecond() == null) {
						if (currentPathStartVertex instanceof Centroid) {
							throw new PlanItException(
									"The solution could not find an Edge Segment for the connectoid for zone "
											+ ((Centroid) currentPathStartVertex).getParentZone().getExternalId());
						} else {
							throw new PlanItException("The solution could not find an Edge Segment for node "
									+ ((Node) currentPathStartVertex).getExternalId());
						}
					}
					EdgeSegment currentEdgeSegment = vertexPathCost[startVertexId].getSecond();
					double edgeSegmentCost = modalNetworkSegmentCosts[(int) currentEdgeSegment.getId()];
					shortestPathCost += edgeSegmentCost;
					currentModeData.nextNetworkSegmentFlows[(int) currentEdgeSegment.getId()] += odDemand;
					currentPathStartVertex = currentEdgeSegment.getUpstreamVertex();
				}
				dualityGapFunction.increaseConvexityBound(odDemand * shortestPathCost);
				simulationData.setSkimMatrixValue(mode, currentOriginZone, currentDestinationZone, shortestPathCost);
				previousOriginZoneId = originZoneId;
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
				"Running Traditional Static Assigment over all modes for Time Period " + timePeriod.getDescription());
		Set<Mode> modes = demands.getRegisteredModesForTimePeriod(timePeriod);
		initialiseTimePeriod(modes);
		LinkBasedRelativeDualityGapFunction dualityGapFunction = ((LinkBasedRelativeDualityGapFunction) getGapFunction());
		Calendar startTime = Calendar.getInstance();
		Calendar initialStartTime = startTime;
		while (!simulationData.isConverged()) {
			dualityGapFunction.reset();
			smoothing.update(simulationData.getIterationIndex());

			// NETWORK LOADING - PER MODE
			for (Mode mode : modes) {
				simulationData.resetSkimMatrix(mode, getTransportNetwork().zones);
				double[] modalNetworkSegmentCosts = getModalNetworkSegmentCosts(mode, timePeriod,
						simulationData.getIterationIndex());
				simulationData.resetModalNetworkSegmentFlows(mode);
				executeAndSmoothTimePeriodAndMode(timePeriod, mode, modalNetworkSegmentCosts);
				simulationData.setModalNetworkSegmentCosts(mode, modalNetworkSegmentCosts);
			}

			dualityGapFunction.computeGap();
			simulationData.incrementIterationIndex();
			Calendar currentTime = Calendar.getInstance();
			long timeDiff = currentTime.getTimeInMillis() - startTime.getTimeInMillis();
			PlanItLogger.info("Iteration " + simulationData.getIterationIndex() + ": Duality gap = "
					+ df6.format(dualityGapFunction.getGap()) + ": Iteration duration " + timeDiff + " milliseconds");
			startTime = currentTime;
			simulationData.setConverged(dualityGapFunction.hasConverged(simulationData.getIterationIndex()));
			outputManager.persistOutputData(timePeriod, modes, getOutputConfiguration());
		}
		long timeDiff = startTime.getTimeInMillis() - initialStartTime.getTimeInMillis();
		PlanItLogger.info("Assignment took " + timeDiff + " milliseconds");
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
		Iterator<ConnectoidSegment> connectoidSegmentIter = transportNetwork.connectoidSegments.iterator();
		while (connectoidSegmentIter.hasNext()) {
			ConnectoidSegment currentSegment = connectoidSegmentIter.next();
			currentSegmentCosts[(int) currentSegment.getId()] = virtualCost.getSegmentCost(mode, currentSegment);
		}
	}

	/**
	 * Store the link segment costs for links for the current iteration
	 * 
	 * This method is called during the second and subsequent iterations of the
	 * simulation.
	 * 
	 * @param mode                current mode of travel
	 * @param currentSegmentCosts array to store the current segment costs
	 * @throws PlanItException thrown if there is an error
	 */
	private void populateModalLinkSegmentCosts(Mode mode, double[] currentSegmentCosts) throws PlanItException {
		setModalLinkSegmentCosts(mode, currentSegmentCosts, (linkSegment) -> {
			return physicalCost.getSegmentCost(mode, linkSegment);
		});
	}

	/**
	 * Initialize the link segment costs from the InitialLinkSegmentCost object (for
	 * all time periods)
	 * 
	 * This method is called during the first iteration of the simulation.
	 * 
	 * @param mode                current mode of travel
	 * @param currentSegmentCosts array to store the current segment costs
	 * @return false if the initial costs cannot be set for this mode, true
	 *         otherwise
	 * @throws PlanItException thrown if there is an error
	 */
	private boolean initializeModalLinkSegmentCosts(Mode mode, double[] currentSegmentCosts) throws PlanItException {
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
	private boolean initializeModalLinkSegmentCosts(Mode mode, TimePeriod timePeriod, double[] currentSegmentCosts)
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
		Iterator<LinkSegment> linkSegmentIter = transportNetwork.linkSegments.iterator();
		while (linkSegmentIter.hasNext()) {
			LinkSegment linkSegment = linkSegmentIter.next();
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
	private void openOrClose(BiFunction<OutputFormatter, OutputTypeConfiguration, PlanItException> action)
			throws PlanItException {
		List<OutputTypeConfiguration> outputTypeConfigurations = outputManager.getOutputConfiguration()
				.getRegisteredOutputTypeConfigurations();

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
	 * Create Traditional Static Assignment output adapter that allows selective
	 * access to all data required for different output types
	 * 
	 * @param outputType the output type for the new output adapter
	 * @return the new output adapter
	 * @see org.planit.trafficassignment.TrafficAssignment#createOutputAdapter(org.planit.output.OutputType)
	 */
	@Override
	public OutputAdapter createOutputAdapter(OutputType outputType) throws PlanItException {
		OutputAdapter outputAdapter = null;
		switch (outputType) {
		case LINK:
			outputAdapter = new TraditionalStaticAssignmentLinkOutputAdapter(this);
			break;
		case OD:
			outputAdapter = new TraditionalStaticAssignmentODOutputAdapter(this);
			break;
		default:
			throw new PlanItException("No Output adapter exists for output type " + outputType.toString() + " on "
					+ this.getClass().getName());
		}
		return outputAdapter;
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
	public double[] getModalNetworkSegmentCosts(Mode mode, TimePeriod timePeriod, int iterationIndex)
			throws PlanItException {
		double[] currentSegmentCosts = new double[transportNetwork.getTotalNumberOfEdgeSegments()];
		populateModalConnectoidCosts(mode, currentSegmentCosts);
		if (iterationIndex == 0) {
			if (initialLinkSegmentCostByTimePeriod.containsKey(timePeriod.getId())) {
				if (initializeModalLinkSegmentCosts(mode, timePeriod, currentSegmentCosts)) {
					return currentSegmentCosts;
				}
			} else if (initialLinkSegmentCost != null) {
				if (initializeModalLinkSegmentCosts(mode, currentSegmentCosts)) {
					return currentSegmentCosts;
				}
			}
		}
		populateModalLinkSegmentCosts(mode, currentSegmentCosts);
		return currentSegmentCosts;
	}

	/**
	 * Collect the updated edge segment costs for the given mode
	 * 
	 * @param mode           the current mode
	 * @param iterationIndex index of the current iteration
	 * @return array of updated edge segment costs
	 * @throws PlanItException thrown if there is an error
	 */
	public double[] getModalNetworkSegmentCosts(Mode mode, int iterationIndex) throws PlanItException {
		double[] currentSegmentCosts = new double[transportNetwork.getTotalNumberOfEdgeSegments()];
		populateModalConnectoidCosts(mode, currentSegmentCosts);
		if ((iterationIndex == 0) && (initialLinkSegmentCost != null)
				&& (initializeModalLinkSegmentCosts(mode, currentSegmentCosts))) {
			return currentSegmentCosts;
		}
		populateModalLinkSegmentCosts(mode, currentSegmentCosts);
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
	 * Return the simulation data for the current iteration
	 * 
	 * @return simulation data
	 */
	public SimulationData getSimulationData() {
		return simulationData;
	}

}
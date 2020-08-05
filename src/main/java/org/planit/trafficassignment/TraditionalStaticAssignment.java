package org.planit.trafficassignment;

import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.djutils.event.Event;
import org.djutils.event.EventInterface;
import org.djutils.event.EventType;
import org.planit.algorithms.shortestpath.DijkstraShortestPathAlgorithm;
import org.planit.algorithms.shortestpath.OneToAllShortestPathAlgorithm;
import org.planit.cost.Cost;
import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.physical.initial.InitialPhysicalCost;
import org.planit.data.ModeData;
import org.planit.data.SimulationData;
import org.planit.data.TraditionalStaticAssignmentSimulationData;
import org.planit.demands.Demands;
import org.planit.gap.LinkBasedRelativeDualityGapFunction;
import org.planit.input.InputBuilderListener;
import org.planit.interactor.LinkVolumeAccessee;
import org.planit.interactor.LinkVolumeAccessor;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.od.odmatrix.ODMatrixIterator;
import org.planit.od.odmatrix.demand.ODDemandMatrix;
import org.planit.od.odmatrix.skim.ODSkimMatrix;
import org.planit.od.odroute.ODRouteMatrix;
import org.planit.output.adapter.OutputTypeAdapter;
import org.planit.output.adapter.TraditionalStaticAssignmentLinkOutputTypeAdapter;
import org.planit.output.adapter.TraditionalStaticAssignmentODOutputTypeAdapter;
import org.planit.output.adapter.TraditionalStaticRouteOutputTypeAdapter;
import org.planit.output.enums.ODSkimSubOutputType;
import org.planit.output.enums.OutputType;
import org.planit.route.Route;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.builder.TraditionalStaticAssignmentBuilder;
import org.planit.trafficassignment.builder.TrafficAssignmentBuilder;
import org.planit.utils.arrays.ArrayOperations;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.Vertex;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.misc.LoggingUtils;
import org.planit.utils.misc.Pair;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Mode;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.virtual.Centroid;
import org.planit.utils.network.virtual.ConnectoidSegment;
import org.planit.utils.network.virtual.Zone;

/**
 * Traditional static assignment traffic component. Provides configuration access via the CapacityRestrainedTrafficAssignmentBuilder it instantiates
 *
 * @author markr, gman6028
 *
 */
public class TraditionalStaticAssignment extends TrafficAssignment implements LinkVolumeAccessee {

  /** Generated UID */
  private static final long serialVersionUID = -4610905345414397908L;

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(TraditionalStaticAssignment.class.getCanonicalName());

  /**
   * Epsilon margin when comparing flow rates (veh/h)
   */
  private static final double DEFAULT_FLOW_EPSILON = 0.000001;

  /**
   * Holds the running simulation data for the assignment
   */
  private TraditionalStaticAssignmentSimulationData simulationData;
  
  /** create the logging prefix for logging statements during equilibration
   * @return prefix
   */
  protected String createLoggingPrefix() {    
    return super.createLoggingPrefix(simulationData.getIterationIndex()); 
  }

  /**
   * Initialize running simulation variables for the time period
   *
   * @param modes set of modes covered by this assignment
   * @throws PlanItException thrown if there is an error
   */
  private void initialiseTimePeriod(final Set<Mode> modes) throws PlanItException {
    simulationData = new TraditionalStaticAssignmentSimulationData(groupId, outputManager);
    simulationData.setIterationIndex(0);
    simulationData.getModeSpecificData().clear();
    for (final Mode mode : modes) {
      simulationData.resetModalNetworkSegmentFlows(mode, numberOfNetworkSegments);
      simulationData.getModeSpecificData().put(mode, new ModeData(new double[numberOfNetworkSegments]));
    }
  }

  /**
   * Apply smoothing based on current and previous flows and the adopted smoothing method. The smoothed results are registered as the current segment flows while the current
   * segment flows are assigned to the previous segment flows (which are discarded).
   *
   * @param modeData data for the current mode
   */
  private void applySmoothing(final ModeData modeData) {
    final double[] smoothedSegmentFlows = smoothing.applySmoothing(modeData.currentNetworkSegmentFlows, modeData.nextNetworkSegmentFlows, numberOfNetworkSegments);
    // update flow arrays for next iteration
    modeData.currentNetworkSegmentFlows = smoothedSegmentFlows;
  }

  /**
   * Perform assignment for a given time period, mode and costs imposed on Dijkstra shortest path
   *
   * @param mode                     the current mode
   * @param odDemandMatrix           origin-demand matrix
   * @param currentModeData          data for the current mode
   * @param modalNetworkSegmentCosts segment costs for the network
   * @param shortestPathAlgorithm    shortest path algorithm to be used
   * @throws PlanItException thrown if there is an error
   */
  private void executeModeTimePeriod(final Mode mode, final ODDemandMatrix odDemandMatrix, final ModeData currentModeData, final double[] modalNetworkSegmentCosts,
      final OneToAllShortestPathAlgorithm shortestPathAlgorithm) throws PlanItException {

    final LinkBasedRelativeDualityGapFunction dualityGapFunction = ((LinkBasedRelativeDualityGapFunction) getGapFunction());
    final ODRouteMatrix odRouteMatrix = simulationData.getODPathMatrix(mode);
    final Map<ODSkimSubOutputType, ODSkimMatrix> skimMatrixMap = simulationData.getSkimMatrixMap(mode);

    // loop over all available OD demands
    long previousOriginZoneId = -1;
    // track the cost to reach each vertex in the network and the shortest path
    // segment used to get there
    Pair<Double, EdgeSegment>[] vertexPathAndCosts = null;
    for (final ODMatrixIterator odDemandMatrixIter = odDemandMatrix.iterator(); odDemandMatrixIter.hasNext();) {
      final double odDemand = odDemandMatrixIter.next();
      final Zone currentOriginZone = odDemandMatrixIter.getCurrentOrigin();
      final Zone currentDestinationZone = odDemandMatrixIter.getCurrentDestination();

      if (currentOriginZone.getId() != currentDestinationZone.getId()) {
        if (outputManager.getOutputConfiguration().isPersistZeroFlow() || ((odDemand - DEFAULT_FLOW_EPSILON) > 0.0)) {

          if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(LoggingUtils.createRunIdPrefix(getId()) + String.format("(O,D)=(%d,%d) --> demand (pcu/h): %f (mode: %d)", currentOriginZone.getExternalId(),
                currentDestinationZone.getExternalId(), odDemand, mode.getExternalId()));
          }

          // MARK 6-1-2020
          // extracted this from separated method (method removed);
          // we do to many things here (path search, path storage), better to leave it
          // exposed at this level so we know where this functionality occurs
          // when origin is updated we conduct ONE-TO-ALL shortest path search
          if (previousOriginZoneId != currentOriginZone.getId()) {

            final Centroid originCentroid = currentOriginZone.getCentroid();
            PlanItException.throwIf(originCentroid.getExitEdgeSegments().isEmpty(),
                String.format("edge segments have not been assigned to Centroid for Zone %d", currentOriginZone.getExternalId()));

            // UPDATE SHORTEST PATHS
            vertexPathAndCosts = shortestPathAlgorithm.executeOneToAll(originCentroid);
          }

          // TODO: we are now creating a route separate from finding shortest path. This makes no sense as it is very costly when switched on
          if (outputManager.isOutputTypeActive(OutputType.PATH)) {
            final Route route = Route.createRoute(groupId, currentDestinationZone.getCentroid(), vertexPathAndCosts);
            odRouteMatrix.setValue(currentOriginZone, currentDestinationZone, route);
          }

          double odShortestPathCost = 0.0;
          if ((odDemand - DEFAULT_FLOW_EPSILON) > 0.0) {
            try {
              odShortestPathCost = getShortestPathCost(vertexPathAndCosts, currentOriginZone, currentDestinationZone, modalNetworkSegmentCosts, odDemand, currentModeData);
              dualityGapFunction.increaseConvexityBound(odDemand * odShortestPathCost);
            } catch (PlanItException e) {
              LOGGER.warning(e.getMessage());
              LOGGER.info(createLoggingPrefix() + "impossible path from origin zone " + currentOriginZone.getExternalId() + " to destination zone "
                  + currentDestinationZone.getExternalId() + " (mode " + mode.getExternalId() + ")");
            }
          }
          previousOriginZoneId = currentOriginZone.getId();

          updateSkimMatrixMap(skimMatrixMap, currentOriginZone, currentDestinationZone, odDemand, vertexPathAndCosts);
        }
      }
    }
  }

  /**
   * Calculate the route cost for the calculated minimum cost path from a specified origin to a specified destination
   *
   * @param vertexPathAndCost        array of Pairs containing the current vertex path and cost
   * @param currentOriginZone        current origin zone
   * @param currentDestinationZone   current destination zone
   * @param modalNetworkSegmentCosts segment costs for the network
   * @param odDemand                 the demands from the specified origin to the specified destination
   * @param currentModeData          data for the current mode
   * @return the route cost for the calculated minimum cost path
   * @throws PlanItException thrown if there is an error
   */
  private double getShortestPathCost(final Pair<Double, EdgeSegment>[] vertexPathAndCost, final Zone currentOriginZone, final Zone currentDestinationZone,
      final double[] modalNetworkSegmentCosts, final double odDemand, final ModeData currentModeData) throws PlanItException {
    double shortestPathCost = 0;
    EdgeSegment currentEdgeSegment = null;
    for (Vertex currentPathStartVertex = currentDestinationZone.getCentroid(); currentPathStartVertex.getId() != currentOriginZone.getCentroid()
        .getId(); currentPathStartVertex = currentEdgeSegment.getUpstreamVertex()) {

      final int startVertexId = (int) currentPathStartVertex.getId();
      currentEdgeSegment = vertexPathAndCost[startVertexId].getSecond();
      if (currentEdgeSegment == null) {
        PlanItException.throwIf(currentPathStartVertex instanceof Centroid,
            "The solution could not find an Edge Segment for the connectoid for zone " + ((Centroid) currentPathStartVertex).getParentZone().getExternalId());
        throw new PlanItException("The solution could not find an Edge Segment for node " + ((Node) currentPathStartVertex).getId());
      }
      final int edgeSegmentId = (int) currentEdgeSegment.getId();
      shortestPathCost += modalNetworkSegmentCosts[edgeSegmentId];

      // TODO: this should not be part of the shortest path cost
      currentModeData.nextNetworkSegmentFlows[edgeSegmentId] += odDemand;
    }
    return shortestPathCost;
  }

  /**
   * Update the OD skim matrix for all active output types
   *
   * @param skimMatrixMap          Map of OD skim matrices for each active output type
   * @param currentOriginZone      current origin zone
   * @param currentDestinationZone current destination zone
   * @param odDemand               the odDemand
   * @param vertexPathAndCosts     array of costs for the specified mode
   */
  private void updateSkimMatrixMap(final Map<ODSkimSubOutputType, ODSkimMatrix> skimMatrixMap, final Zone currentOriginZone, final Zone currentDestinationZone,
      final double odDemand, final Pair<Double, EdgeSegment>[] vertexPathAndCosts) {
    for (final ODSkimSubOutputType odSkimOutputType : simulationData.getActiveSkimOutputTypes()) {
      if (odSkimOutputType.equals(ODSkimSubOutputType.COST)) {

        // Collect cost to get to vertex from shortest path ONE-TO-ALL information
        // directly
        final long destinationVertexId = currentDestinationZone.getCentroid().getId();
        final Pair<Double, EdgeSegment> vertexPathCost = vertexPathAndCosts[(int) destinationVertexId];
        double odGeneralisedCost = vertexPathCost.getFirst();

        final ODSkimMatrix odSkimMatrix = skimMatrixMap.get(odSkimOutputType);
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
  private void executeTimePeriod(final TimePeriod timePeriod) throws PlanItException {
    final Set<Mode> modes = demands.getRegisteredModesForTimePeriod(timePeriod);
    initialiseTimePeriod(modes);
    final LinkBasedRelativeDualityGapFunction dualityGapFunction = ((LinkBasedRelativeDualityGapFunction) getGapFunction());
    Calendar startTime = Calendar.getInstance();
    final Calendar initialStartTime = startTime;
    for (final Mode mode : modes) {
      final double[] modalLinkSegmentCosts = initializeModalLinkSegmentCosts(mode, timePeriod);
      simulationData.setModalLinkSegmentCosts(mode, modalLinkSegmentCosts);
    }

    boolean converged = false;
    while (!converged) {
      dualityGapFunction.reset();
      smoothing.update(simulationData.getIterationIndex());

      // NETWORK LOADING - PER MODE
      for (final Mode mode : modes) {
        // :TODO ugly -> you are not resetting 1 matrix but multiple NAMES ARE WRONG
        // :TODO: slow -> only reset or do something when it is stored in the first place, this is
        // not checked
        simulationData.resetSkimMatrix(mode, getTransportNetwork().getZoning().zones);
        simulationData.resetPathMatrix(mode, getTransportNetwork().getZoning().zones);
        simulationData.resetModalNetworkSegmentFlows(mode, numberOfNetworkSegments);

        final double[] modalLinkSegmentCosts = simulationData.getModalLinkSegmentCosts(mode);
        executeAndSmoothTimePeriodAndMode(timePeriod, mode, modalLinkSegmentCosts);
      }

      dualityGapFunction.computeGap();
      simulationData.incrementIterationIndex();
      LOGGER.info(createLoggingPrefix() + String.format("Network travel time: %f", dualityGapFunction.getActualSystemTravelTime()));
      startTime = recordTime(startTime, dualityGapFunction.getGap());
      for (final Mode mode : modes) {
        final double[] modalLinkSegmentCosts = recalculateModalLinkSegmentCosts(mode, timePeriod);
        simulationData.setModalLinkSegmentCosts(mode, modalLinkSegmentCosts);
      }
      converged = dualityGapFunction.hasConverged(simulationData.getIterationIndex());
      outputManager.persistOutputData(timePeriod, modes, converged);
    }
    LOGGER.info(LoggingUtils.createRunIdPrefix(getId()) + String.format("run time: %d milliseconds", startTime.getTimeInMillis() - initialStartTime.getTimeInMillis()));
  }

  /**
   * Record the time an iteration took
   *
   * @param startTime  the original start time of the iteration
   * @param dualityGap the duality gap at the end of the iteration
   * @return the time at the end of the iteration
   */
  private Calendar recordTime(final Calendar startTime, final double dualityGap) {
    final Calendar currentTime = Calendar.getInstance();
    LOGGER.info(createLoggingPrefix() + String.format("duality gap: %.6f (%d ms)", dualityGap, currentTime.getTimeInMillis() - startTime.getTimeInMillis()));
    return currentTime;
  }

  /**
   * Execute the assignment for the current time period and mode and apply smoothing to the result
   *
   * @param timePeriod               the current time period
   * @param mode                     the current mode
   * @param modalNetworkSegmentCosts the current network segment costs
   * @throws PlanItException thrown if there is an error
   */
  private void executeAndSmoothTimePeriodAndMode(final TimePeriod timePeriod, final Mode mode, final double[] modalNetworkSegmentCosts) throws PlanItException {
    LOGGER.fine(LoggingUtils.createRunIdPrefix(getId()) + String.format("[mode %d (id:%d)]", mode.getExternalId(), mode.getId()));
    // mode specific data
    final ModeData currentModeData = simulationData.getModeSpecificData().get(mode);
    currentModeData.resetNextNetworkSegmentFlows();
    final LinkBasedRelativeDualityGapFunction dualityGapFunction = ((LinkBasedRelativeDualityGapFunction) getGapFunction());

    // AON based network loading
    final OneToAllShortestPathAlgorithm shortestPathAlgorithm = new DijkstraShortestPathAlgorithm(modalNetworkSegmentCosts, numberOfNetworkSegments, numberOfNetworkVertices);
    final ODDemandMatrix odDemandMatrix = demands.get(mode, timePeriod);
    executeModeTimePeriod(mode, odDemandMatrix, currentModeData, modalNetworkSegmentCosts, shortestPathAlgorithm);

    final double totalModeSystemTravelTime = ArrayOperations.dotProduct(currentModeData.currentNetworkSegmentFlows, modalNetworkSegmentCosts, numberOfNetworkSegments);
    dualityGapFunction.increaseActualSystemTravelTime(totalModeSystemTravelTime);
    applySmoothing(currentModeData);

    // aggregate smoothed mode specific flows - for cost computation
    ArrayOperations.addTo(simulationData.getModalNetworkSegmentFlows(mode), currentModeData.currentNetworkSegmentFlows, numberOfNetworkSegments);
    simulationData.getModeSpecificData().put(mode, currentModeData);
  }

  /**
   * Populate the current segment costs for connectoids
   *
   * @param mode                current mode of travel
   * @param currentSegmentCosts array to store the current segment costs
   * @throws PlanItException thrown if there is an error
   */
  private void populateModalConnectoidCosts(final Mode mode, final double[] currentSegmentCosts) throws PlanItException {
    for (final ConnectoidSegment currentSegment : transportNetwork.getVirtualNetwork().connectoidSegments) {
      currentSegmentCosts[(int) currentSegment.getId()] = virtualCost.getSegmentCost(mode, currentSegment);
    }
  }

  /**
   * Calculate and store the link segment costs for links for the current iteration
   *
   * This method is called during the second and subsequent iterations of the simulation.
   *
   * @param mode                current mode of travel
   * @param currentSegmentCosts array to store the current segment costs
   * @throws PlanItException thrown if there is an error
   */
  private void calculateModalLinkSegmentCosts(final Mode mode, final double[] currentSegmentCosts) throws PlanItException {
    setModalLinkSegmentCosts(mode, currentSegmentCosts, physicalCost);
  }

  /**
   * Initialize the link segment costs from the InitialLinkSegmentCost object for all time periods
   *
   * This method is called during the first iteration of the simulation.
   *
   * @param mode                current mode of travel
   * @param currentSegmentCosts array to store the current segment costs
   * @return false if the initial costs cannot be set for this mode, true otherwise
   * @throws PlanItException thrown if there is an error
   */
  private boolean initializeModalLinkSegmentCostsForAllTimePeriods(final Mode mode, final double[] currentSegmentCosts) throws PlanItException {
    if (!initialLinkSegmentCost.isSegmentCostsSetForMode(mode)) {
      return false;
    }
    setModalLinkSegmentCosts(mode, currentSegmentCosts, initialLinkSegmentCost);
    return true;
  }

  /**
   * Initialize the link segment costs from the InitialLinkSegmentCost object for each current time period
   *
   * This method is called during the first iteration of the simulation.
   *
   * @param mode                current mode of travel
   * @param timePeriod          current time period
   * @param currentSegmentCosts array to store the current segment costs
   * @return false if the initial costs cannot be set for this mode and time period, true otherwise
   * @throws PlanItException thrown if there is an error
   */
  private boolean initializeModalLinkSegmentCostsByTimePeriod(final Mode mode, final TimePeriod timePeriod, final double[] currentSegmentCosts) throws PlanItException {
    final InitialLinkSegmentCost initialLinkSegmentCostForTimePeriod = initialLinkSegmentCostByTimePeriod.get(timePeriod);
    if (!initialLinkSegmentCostForTimePeriod.isSegmentCostsSetForMode(mode)) {
      return false;
    }
    InitialPhysicalCost initialTimePeriodCost = initialLinkSegmentCostByTimePeriod.get(timePeriod);
    setModalLinkSegmentCosts(mode, currentSegmentCosts, initialTimePeriodCost);
    return true;
  }

  /**
   * Initialize the modal link segment costs before the first iteration.
   *
   * This method uses initial link segment costs if they have been input, otherwise these are calculated from zero start values
   *
   * @param mode       current mode
   * @param timePeriod current time period
   * @return array containing link costs for each link segment
   * @throws PlanItException thrown if there is an error
   */
  private double[] initializeModalLinkSegmentCosts(final Mode mode, final TimePeriod timePeriod) throws PlanItException {
    final double[] currentSegmentCosts = new double[transportNetwork.getTotalNumberOfEdgeSegments()];
    populateModalConnectoidCosts(mode, currentSegmentCosts);
    if (initialLinkSegmentCostByTimePeriod.containsKey(timePeriod)) {
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
   * Set the link segment costs
   * 
   * Cost set to POSITIVE_INFINITY for any mode which is forbidden along a link segment
   *
   * @param mode                current mode of travel
   * @param currentSegmentCosts array to store the current segment costs
   * @param cost                Cost object used to calculate the cost
   * @throws PlanItException thrown if there is an error
   */
  private void setModalLinkSegmentCosts(final Mode mode, final double[] currentSegmentCosts, Cost<LinkSegment> cost) throws PlanItException {
    for (final LinkSegment linkSegment : transportNetwork.getPhysicalNetwork().linkSegments) {
      double currentSegmentCost = Double.POSITIVE_INFINITY;
      if (linkSegment.isModeAllowedThroughLink(mode)) {
        currentSegmentCost = cost.getSegmentCost(mode, linkSegment);
        if (currentSegmentCost < 0.0) {
          throw new PlanItException("Error during calculation of link segment costs");
        }
      }
      currentSegmentCosts[(int) linkSegment.getId()] = currentSegmentCost;
    }
  }

  /**
   * Recalculate the modal link segment costs after each iteration
   *
   * @param mode       current mode
   * @param timePeriod current time period
   * @return array containing link costs for each link segment
   * @throws PlanItException thrown if there is an error
   */
  private double[] recalculateModalLinkSegmentCosts(final Mode mode, final TimePeriod timePeriod) throws PlanItException {
    final double[] currentSegmentCosts = new double[transportNetwork.getTotalNumberOfEdgeSegments()];
    populateModalConnectoidCosts(mode, currentSegmentCosts);
    calculateModalLinkSegmentCosts(mode, currentSegmentCosts);
    return currentSegmentCosts;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected TrafficAssignmentBuilder createTrafficAssignmentBuilder(final InputBuilderListener trafficComponentCreateListener, final Demands demands, final Zoning zoning,
      final PhysicalNetwork physicalNetwork) throws PlanItException {
    return new TraditionalStaticAssignmentBuilder(this, trafficComponentCreateListener, demands, zoning, physicalNetwork);
  }

  /** {@inheritDoc} */
  @Override
  protected void addRegisteredEventTypeListeners(final EventType eventType) {
    // in case of traditional static assignment, the assignment provides access to the link volumes
    // so we register ourselves as a listener for this event type
    if (eventType.equals(LinkVolumeAccessee.INTERACTOR_PROVIDE_LINKVOLUMEACCESSEE)) {
      addListener(this, eventType);
    }
  }

  /**
   * Base Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public TraditionalStaticAssignment(IdGroupingToken groupId) {
    super(groupId);
    simulationData = null;
  }

  /**
   * Execute equilibration over all time periods and modes
   *
   * @throws PlanItException thrown if there is an error
   */
  @Override
  public void executeEquilibration() throws PlanItException {
    // perform assignment per period - per mode
    final Collection<TimePeriod> timePeriods = demands.timePeriods.asSortedSetByStartTime();
    LOGGER.info(LoggingUtils.createRunIdPrefix(getId()) + "total time periods: " + timePeriods.size());
    for (final TimePeriod timePeriod : timePeriods) {
      LOGGER.info(LoggingUtils.createRunIdPrefix(getId()) +  LoggingUtils.createTimePeriodPrefix(timePeriod.getExternalId(),timePeriod.getId()) + timePeriod.toString());
      executeTimePeriod(timePeriod);
    }
  }

  /**
   * #{@inheritDoc}
   */
  @Override
  public double getTotalNetworkSegmentFlow(final LinkSegment linkSegment) {
    return simulationData.getTotalNetworkSegmentFlow(linkSegment);
  }

  /**
   * #{@inheritDoc}
   */
  @Override
  public double[] getModalNetworkSegmentFlows(final Mode mode) {
    return simulationData.getModalNetworkSegmentFlows(mode);
  }

  /**
   * #{@inheritDoc}
   */
  @Override
  public int getNumberOfLinkSegments() {
    return getTransportNetwork().getTotalNumberOfPhysicalLinkSegments();
  }

  /**
   * Deal with requests for link volume accessees since we are one. Whenever such a request arrives, we provide ourselves as a candidate and fire a response event of type
   * LinkVolumeAccessee.INTERACTOR_PROVIDE_LINKVOLUMEACCESSEE
   *
   * @param event to process
   */
  @Override
  public void notify(final EventInterface event) throws RemoteException {
    if (event.getType().equals(LinkVolumeAccessor.INTERACTOR_REQUEST_LINKVOLUMEACCESSEE_TYPE)) {
      if (event.getContent() instanceof LinkVolumeAccessor) {
        // source is accessor, so we provide ourselves as the accessee
        final LinkVolumeAccessor theLinkVolumeAccessor = (LinkVolumeAccessor) event.getContent();
        addListener(theLinkVolumeAccessor, INTERACTOR_PROVIDE_LINKVOLUMEACCESSEE);
        // fire event where we signal that an accessee is available (us) for this request
        fireEvent(new Event(INTERACTOR_PROVIDE_LINKVOLUMEACCESSEE, this, this));
      }
    }
  }

  /**
   * Create the output type adapter for the current output type
   *
   * @param outputType the current output type
   * @return the output type adapter corresponding to the current traffic assignment and output type
   */
  @Override
  public OutputTypeAdapter createOutputTypeAdapter(final OutputType outputType) {
    OutputTypeAdapter outputTypeAdapter = null;
    switch (outputType) {
    case LINK:
      outputTypeAdapter = new TraditionalStaticAssignmentLinkOutputTypeAdapter(outputType, this);
      break;
    case OD:
      outputTypeAdapter = new TraditionalStaticAssignmentODOutputTypeAdapter(outputType, this);
      break;
    case PATH:
      outputTypeAdapter = new TraditionalStaticRouteOutputTypeAdapter(outputType, this);
      break;
    default:
      LOGGER.warning(LoggingUtils.createRunIdPrefix(getId()) + outputType.value() + " has not been defined yet.");
    }
    return outputTypeAdapter;
  }

  /**
   * Return the simulation data for the current iteration
   *
   * @return simulation data
   */
  @Override
  public SimulationData getSimulationData() {
    return simulationData;
  }

}

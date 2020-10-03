package org.planit.assignment.traditionalstatic;

import java.util.Calendar;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.planit.algorithms.shortestpath.DijkstraShortestPathAlgorithm;
import org.planit.algorithms.shortestpath.OneToAllShortestPathAlgorithm;
import org.planit.algorithms.shortestpath.ShortestPathResult;
import org.planit.assignment.StaticTrafficAssignment;
import org.planit.cost.Cost;
import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.physical.initial.InitialPhysicalCost;
import org.planit.gap.LinkBasedRelativeDualityGapFunction;
import org.planit.interactor.LinkVolumeAccessee;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.od.odmatrix.ODMatrixIterator;
import org.planit.od.odmatrix.demand.ODDemandMatrix;
import org.planit.od.odmatrix.skim.ODSkimMatrix;
import org.planit.od.odpath.ODPathMatrix;
import org.planit.output.adapter.OutputTypeAdapter;
import org.planit.output.adapter.TraditionalStaticAssignmentLinkOutputTypeAdapter;
import org.planit.output.adapter.TraditionalStaticAssignmentODOutputTypeAdapter;
import org.planit.output.adapter.TraditionalStaticPathOutputTypeAdapter;
import org.planit.output.configuration.ODOutputTypeConfiguration;
import org.planit.output.enums.ODSkimSubOutputType;
import org.planit.output.enums.OutputType;
import org.planit.output.enums.SubOutputTypeEnum;
import org.planit.path.Path;
import org.planit.time.TimePeriod;
import org.planit.utils.arrays.ArrayUtils;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.Vertex;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.misc.LoggingUtils;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.virtual.Centroid;
import org.planit.utils.network.virtual.ConnectoidSegment;
import org.planit.utils.network.virtual.Zone;

/**
 * Traditional static assignment traffic component. Provides configuration access via the CapacityRestrainedTrafficAssignmentBuilder it instantiates
 *
 * @author markr, gman6028
 *
 */
public class TraditionalStaticAssignment extends StaticTrafficAssignment implements LinkVolumeAccessee {

  /** Generated UID */
  private static final long serialVersionUID = -4610905345414397908L;

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(TraditionalStaticAssignment.class.getCanonicalName());

  /**
   * Epsilon margin when comparing flow rates (veh/h)
   */
  private static final double DEFAULT_FLOW_EPSILON = 0.000001;

  /**
   * Simulation data for this equilibration process
   */
  private TraditionalStaticAssignmentSimulationData simulationData;

  /**
   * create the logging prefix for logging statements during equilibration
   * 
   * @return prefix
   */
  protected String createLoggingPrefix() {
    return super.createLoggingPrefix(simulationData.getIterationIndex());
  }

  /**
   * Initialize running simulation variables for the time period
   *
   * @param timePeriod the time period
   * @param modes      set of modes covered by this assignment
   * @throws PlanItException thrown if there is an error
   */
  private void initialiseTimePeriod(TimePeriod timePeriod, final Set<Mode> modes) throws PlanItException {
    simulationData = new TraditionalStaticAssignmentSimulationData(groupId);
    simulationData.setIterationIndex(0);
    simulationData.getModeSpecificData().clear();
    for (final Mode mode : modes) {
      // flow initialisation
      simulationData.getModeSpecificData().put(mode, new ModeData(new double[numberOfNetworkSegments]));
      // cost initialisation
      final double[] modalLinkSegmentCosts = initialiseLinkSegmentCosts(mode, timePeriod);
      simulationData.setModalLinkSegmentCosts(mode, modalLinkSegmentCosts);
    }
  }

  /**
   * Apply smoothing based on current and previous flows and the adopted smoothing method. The smoothed results are registered as the current segment flows while the current
   * segment flows are assigned to the previous segment flows (which are discarded).
   *
   * @param mode     the current mode
   * @param modeData data for the current mode
   */
  private void applySmoothing(Mode mode, final ModeData modeData) {
    final double[] smoothedSegmentFlows = smoothing.applySmoothing(modeData.getCurrentSegmentFlows(), modeData.getNextSegmentFlows(), numberOfNetworkSegments);
    // update flow arrays for next iteration
    modeData.setCurrentSegmentFlows(smoothedSegmentFlows);
    simulationData.getModeSpecificData().put(mode, modeData);
  }

  /**
   * Perform assignment for a given time period, mode and costs imposed on Dijkstra shortest path
   *
   * @param mode                     the current mode
   * @param timePeriod               the current time period
   * @param currentModeData          data for the current mode
   * @param modalNetworkSegmentCosts segment costs for the network
   * @throws PlanItException thrown if there is an error
   */
  private void executeTimePeriodAndMode(final Mode mode, final TimePeriod timePeriod, final ModeData currentModeData, final double[] modalNetworkSegmentCosts)
      throws PlanItException {

    final OneToAllShortestPathAlgorithm shortestPathAlgorithm = new DijkstraShortestPathAlgorithm(modalNetworkSegmentCosts, numberOfNetworkSegments, numberOfNetworkVertices);
    final ODDemandMatrix odDemandMatrix = demands.get(mode, timePeriod);

    final LinkBasedRelativeDualityGapFunction dualityGapFunction = ((LinkBasedRelativeDualityGapFunction) getGapFunction());
    final ODPathMatrix odpathMatrix = simulationData.getODPathMatrix(mode);
    final Map<ODSkimSubOutputType, ODSkimMatrix> skimMatrixMap = simulationData.getSkimMatrixMap(mode);

    // loop over all available OD demands
    long previousOriginZoneId = -1;
    // track the cost to reach each vertex in the network and the shortest path
    // segment used to get there
    ShortestPathResult shortestPathResult = null;
    for (final ODMatrixIterator odDemandMatrixIter = odDemandMatrix.iterator(); odDemandMatrixIter.hasNext();) {
      final double odDemand = odDemandMatrixIter.next();
      final Zone currentOriginZone = odDemandMatrixIter.getCurrentOrigin();
      final Zone currentDestinationZone = odDemandMatrixIter.getCurrentDestination();

      if (currentOriginZone.getId() != currentDestinationZone.getId()) {
        if (getOutputManager().getOutputConfiguration().isPersistZeroFlow() || ((odDemand - DEFAULT_FLOW_EPSILON) > 0.0)) {

          if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(LoggingUtils.createRunIdPrefix(getId()) + String.format("(O,D)=(%d,%d) --> demand (pcu/h): %f (mode: %d)", currentOriginZone.getExternalId(),
                currentDestinationZone.getExternalId(), odDemand, mode.getExternalId()));
          }

          /* new shortest path tree for current origin */
          if (previousOriginZoneId != currentOriginZone.getId()) {

            final Centroid originCentroid = currentOriginZone.getCentroid();
            if (originCentroid.getExitEdgeSegments().isEmpty()) {
              throw new PlanItException(String.format("edge segments have not been assigned to Centroid for Zone %d", currentOriginZone.getExternalId()));
            }

            // UPDATE SHORTEST PATHS
            shortestPathResult = shortestPathAlgorithm.executeOneToAll(originCentroid);
          }

          if ((odDemand - DEFAULT_FLOW_EPSILON) > 0.0) {
            double odShortestPathCost = shortestPathResult.getCostToReach(currentDestinationZone.getCentroid());
            if (odShortestPathCost == Double.POSITIVE_INFINITY) {
              LOGGER.warning(createLoggingPrefix() + "impossible path from origin zone " + currentOriginZone.getExternalId() + " to destination zone "
                  + currentDestinationZone.getExternalId() + " (mode " + mode.getExternalId() + ")");
            } else {
              updateNetworkFlowsForPath(shortestPathResult, currentOriginZone, currentDestinationZone, odDemand, currentModeData);
              dualityGapFunction.increaseConvexityBound(odDemand * odShortestPathCost);
            }
          }
          previousOriginZoneId = currentOriginZone.getId();

          /* update skim and path data if needed */

          updateODOutputData(skimMatrixMap, currentOriginZone, currentDestinationZone, odDemand, shortestPathResult);
          updatePathOutputData(odpathMatrix, currentOriginZone, currentDestinationZone, shortestPathResult);
        }
      }
    }
  }

  /**
   * apply smoothing for the current time period and mode results (to be called after executeTimePeriodAndMode)
   * 
   * @param mode                     the current mode
   * @param timePeriod               the current time period
   * @param currentModeData          data for the current mode
   * @param modalNetworkSegmentCosts segment costs for the network
   */
  private void smoothTimePeriodAndMode(final Mode mode, final TimePeriod timePeriod, final ModeData currentModeData, final double[] modalNetworkSegmentCosts) {
    final double totalModeSystemTravelTime = ArrayUtils.dotProduct(currentModeData.getCurrentSegmentFlows(), modalNetworkSegmentCosts, numberOfNetworkSegments);

    final LinkBasedRelativeDualityGapFunction dualityGapFunction = ((LinkBasedRelativeDualityGapFunction) getGapFunction());
    dualityGapFunction.increaseMeasuredNetworkCost(totalModeSystemTravelTime);
    applySmoothing(mode, currentModeData);
  }

  /**
   * Execute the assignment for the current time period and mode and apply smoothing to the result
   *
   * @param timePeriod the current time period
   * @param mode       the current mode
   * @throws PlanItException thrown if there is an error
   */
  private void executeAndSmoothTimePeriodAndMode(final TimePeriod timePeriod, final Mode mode) throws PlanItException {
    LOGGER.fine(LoggingUtils.createRunIdPrefix(getId()) + String.format("[mode %d (id:%d)]", mode.getExternalId(), mode.getId()));

    // mode specific data
    final double[] modalLinkSegmentCosts = simulationData.getModalLinkSegmentCosts(mode);
    final ModeData currentModeData = simulationData.getModeSpecificData().get(mode);
    currentModeData.resetNextNetworkSegmentFlows();

    // AON based network loading
    executeTimePeriodAndMode(mode, timePeriod, currentModeData, modalLinkSegmentCosts);

    // smoothing
    smoothTimePeriodAndMode(mode, timePeriod, currentModeData, modalLinkSegmentCosts);
  }

  /**
   * update the network flows based on the shortest path between origin and destination
   *
   * @param shortestPathResult result containing the costs to reach path vertices
   * @param origin             current origin zone
   * @param destination        current destination zone
   * @param odDemand           the demands from the specified origin to the specified destination
   * @param currentModeData    data for the current mode
   * @return the path cost for the calculated minimum cost path
   * @throws PlanItException thrown if there is an error
   */
  private void updateNetworkFlowsForPath(final ShortestPathResult shortestPathResult, final Zone origin, final Zone destination, final double odDemand,
      final ModeData currentModeData) throws PlanItException {

    // prep
    EdgeSegment currentEdgeSegment = null;
    Vertex currentVertex = destination.getCentroid();

    while (currentVertex.getId() != origin.getCentroid().getId()) {

      currentEdgeSegment = shortestPathResult.getIncomingEdgeSegmentForVertex(currentVertex);

      if (currentEdgeSegment == null) {
        PlanItException.throwIf(currentVertex instanceof Centroid,
            "The solution could not find an Edge Segment for the connectoid for zone " + ((Centroid) currentVertex).getParentZone().getExternalId());
      }

      currentModeData.addToNextSegmentFlows(currentEdgeSegment.getId(), odDemand);
      currentVertex = currentEdgeSegment.getUpstreamVertex();
    }
  }

  /**
   * Update the OD skim matrix for all active output types
   *
   * @param skimMatrixMap          Map of OD skim matrices for each active output type
   * @param currentOriginZone      current origin zone
   * @param currentDestinationZone current destination zone
   * @param odDemand               the odDemand
   * @param shortestPathResult     costs for the shortest path results for the specified mode and origin-any destination
   */
  private void updateODOutputData(final Map<ODSkimSubOutputType, ODSkimMatrix> skimMatrixMap, final Zone currentOriginZone, final Zone currentDestinationZone,
      final double odDemand, final ShortestPathResult shortestPathResult) {

    if (getOutputManager().isOutputTypeActive(OutputType.OD)) {
      Set<SubOutputTypeEnum> activeSubOutputTypes = ((ODOutputTypeConfiguration) getOutputManager().getOutputTypeConfiguration(OutputType.OD)).getActiveSubOutputTypes();
      for (final SubOutputTypeEnum odSkimOutputType : activeSubOutputTypes) {
        if (odSkimOutputType.equals(ODSkimSubOutputType.COST)) {

          // Collect cost to get to vertex from shortest path ONE-TO-ALL information directly
          final double odGeneralisedCost = shortestPathResult.getCostToReach(currentDestinationZone.getCentroid());
          final ODSkimMatrix odSkimMatrix = skimMatrixMap.get(odSkimOutputType);
          odSkimMatrix.setValue(currentOriginZone, currentDestinationZone, odGeneralisedCost);
        }
      }
    }
  }

  /**
   * Update the OD path matrix
   *
   * @param odpathMatrix           OD path matrix to add to
   * @param currentOriginZone      current origin zone
   * @param currentDestinationZone current destination zone
   * @param shortestPathResult     shortest path tree for given origin
   */
  private void updatePathOutputData(ODPathMatrix odpathMatrix, Zone currentOriginZone, Zone currentDestinationZone, ShortestPathResult shortestPathResult) {

    // TODO: we are now creating a path separate from finding shortest path. This makes no sense as it is very costly when switched on
    if (getOutputManager().isOutputTypeActive(OutputType.PATH)) {
      final Path path = shortestPathResult.createPath(groupId, currentOriginZone.getCentroid(), currentDestinationZone.getCentroid());
      odpathMatrix.setValue(currentOriginZone, currentDestinationZone, path);
    }
  }

  /**
   * Record the time an iteration took
   *
   * @param startTime           the original start time of the iteration
   * @param measuredNetworkCost the measured system wirde cost
   * @param dualityGap          the duality gap at the end of the iteration
   * @return the time (in ms) at the end of the iteration for profiling purposes only
   */
  private Calendar logIterationInformation(final Calendar startTime, final double measuredNetworkCost, final double dualityGap) {
    final Calendar currentTime = Calendar.getInstance();
    LOGGER.info(createLoggingPrefix() + String.format("network travel time: %f", measuredNetworkCost));
    LOGGER.info(createLoggingPrefix() + String.format("duality gap: %.6f (%d ms)", dualityGap, currentTime.getTimeInMillis() - startTime.getTimeInMillis()));
    return currentTime;
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
    getPhysicalCost().populateWithCost(mode, currentSegmentCosts);
  }

  /**
   * Initialize the link segment costs from the InitialLinkSegmentCost that is not time period specific
   *
   * This method is called during the first iteration of the simulation.
   *
   * @param mode                  current mode of travel
   * @param segmentCostToPopulate array to store the costs in
   * @return false if the initial costs cannot be set for this mode, true otherwise
   * @throws PlanItException thrown if there is an error
   */
  private boolean populateToInitialCost(final Mode mode, final double[] segmentCostToPopulate) throws PlanItException {
    if (initialLinkSegmentCost == null || !initialLinkSegmentCost.isSegmentCostsSetForMode(mode)) {
      return false;
    }
    populateCost(initialLinkSegmentCost, mode, segmentCostToPopulate);
    return true;
  }

  /**
   * Initialize the link segment costs from the InitialLinkSegmentCost of passed in time period. If there is no initial cost available for the timp eriod we set the default initial
   * cost if it is present.
   *
   * This method is called during the first iteration of the simulation.
   *
   * @param mode                  current mode of travel
   * @param timePeriod            current time period
   * @param segmentCostToPopulate array to store the current segment costs
   * @return false if the initial costs cannot be set for this mode, true otherwise
   * @throws PlanItException thrown if there is an error
   */
  private boolean populateToInitialCost(final Mode mode, final TimePeriod timePeriod, final double[] segmentCostToPopulate) throws PlanItException {
    final InitialLinkSegmentCost initialLinkSegmentCostForTimePeriod = initialLinkSegmentCostByTimePeriod.get(timePeriod);
    if (initialLinkSegmentCostForTimePeriod == null || !initialLinkSegmentCostForTimePeriod.isSegmentCostsSetForMode(mode)) {
      return populateToInitialCost(mode, segmentCostToPopulate);
    }
    InitialPhysicalCost initialTimePeriodCost = initialLinkSegmentCostByTimePeriod.get(timePeriod);
    populateCost(initialTimePeriodCost, mode, segmentCostToPopulate);
    return true;
  }

  /**
   * Set the link segment costs
   * 
   * Cost set to POSITIVE_INFINITY for any mode which is forbidden along a link segment
   *
   * @param cost            Cost object used to calculate the cost*
   * @param mode            current mode of travel
   * @param costsToPopulate array to store the current segment costs
   * @throws PlanItException thrown if there is an error
   */
  private void populateCost(Cost<MacroscopicLinkSegment> cost, final Mode mode, final double[] costsToPopulate) throws PlanItException {
    MacroscopicNetwork macroscopicNetwork = (MacroscopicNetwork) transportNetwork.getPhysicalNetwork();
    for (final MacroscopicLinkSegment linkSegment : macroscopicNetwork.linkSegments) {
      double currentSegmentCost = cost.getSegmentCost(mode, linkSegment);
      if (currentSegmentCost < 0.0) {
        throw new PlanItException(String.format("link segment cost is negative for link segment %d (id: %d)", linkSegment.getExternalId(), linkSegment.getId()));
      }
      costsToPopulate[(int) linkSegment.getId()] = currentSegmentCost;
    }
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
  private double[] initialiseLinkSegmentCosts(final Mode mode, final TimePeriod timePeriod) throws PlanItException {
    final double[] currentSegmentCosts = new double[transportNetwork.getTotalNumberOfEdgeSegments()];

    /* virtual component */
    populateModalConnectoidCosts(mode, currentSegmentCosts);

    /* physical component */
    if (populateToInitialCost(mode, timePeriod, currentSegmentCosts)) {
      return currentSegmentCosts;
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
   * @throws PlanItException thrown if there is an error
   */
  private double[] recalculateModalLinkSegmentCosts(final Mode mode, final TimePeriod timePeriod) throws PlanItException {
    final double[] currentSegmentCosts = new double[transportNetwork.getTotalNumberOfEdgeSegments()];
    populateModalConnectoidCosts(mode, currentSegmentCosts);
    calculateModalLinkSegmentCosts(mode, currentSegmentCosts);
    return currentSegmentCosts;
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
      outputTypeAdapter = new TraditionalStaticPathOutputTypeAdapter(outputType, this);
      break;
    default:
      LOGGER.warning(LoggingUtils.createRunIdPrefix(getId()) + outputType.value() + " has not been defined yet.");
    }
    return outputTypeAdapter;
  }

  /**
   * Perform assignment for a given time period using Dijkstra's algorithm
   *
   * @param timePeriod the time period for the current assignment
   * @param modes      modes for the time period
   * @throws PlanItException thrown if there is an error
   */
  @Override
  protected void executeTimePeriod(final TimePeriod timePeriod, Set<Mode> modes) throws PlanItException {

    initialiseTimePeriod(timePeriod, modes);

    final LinkBasedRelativeDualityGapFunction dualityGapFunction = ((LinkBasedRelativeDualityGapFunction) getGapFunction());
    boolean converged = false;
    Calendar iterationStartTime = Calendar.getInstance();

    while (!converged) {
      dualityGapFunction.reset();
      smoothing.update(simulationData.getIterationIndex());

      // NETWORK LOADING - PER MODE
      for (final Mode mode : modes) {

        // :TODO ugly -> you are not resetting 1 matrix but multiple, NAMES ARE WRONG
        // :TODO: slow -> only reset or do something when it is stored in the first place, this is not checked
        if (getOutputManager().isOutputTypeActive(OutputType.OD)) {
          simulationData.resetSkimMatrix(mode, getTransportNetwork().getZoning().zones, (ODOutputTypeConfiguration) getOutputManager().getOutputTypeConfiguration(OutputType.OD));
        }
        if (getOutputManager().isOutputTypeActive(OutputType.PATH)) {
          ;
          simulationData.resetPathMatrix(mode, getTransportNetwork().getZoning().zones);
        }

        /* execute */

        executeAndSmoothTimePeriodAndMode(timePeriod, mode);
      }

      dualityGapFunction.computeGap();
      simulationData.incrementIterationIndex();
      iterationStartTime = logIterationInformation(iterationStartTime, dualityGapFunction.getMeasuredNetworkCost(), dualityGapFunction.getGap());
      for (final Mode mode : modes) {
        final double[] modalLinkSegmentCosts = recalculateModalLinkSegmentCosts(mode, timePeriod);
        simulationData.setModalLinkSegmentCosts(mode, modalLinkSegmentCosts);
      }
      converged = dualityGapFunction.hasConverged(simulationData.getIterationIndex());
      getOutputManager().persistOutputData(timePeriod, modes, converged);
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
   * {@inheritDoc}
   */
  @Override
  public int getIterationIndex() {
    return getIterationData() == null ? 0 : getIterationData().getIterationIndex();
  }

  /**
   * #{@inheritDoc}
   */
  @Override
  public double getLinkSegmentFlow(final LinkSegment linkSegment) {
    return simulationData.collectTotalNetworkSegmentFlow(linkSegment);
  }

  /**
   * #{@inheritDoc}
   */
  @Override
  public double[] getLinkSegmentFlows() {
    return simulationData.collectTotalNetworkSegmentFlows();
  }

  /**
   * Return the simulation data for the current iteration
   *
   * @return simulation data
   */
  public TraditionalStaticAssignmentSimulationData getIterationData() {
    return simulationData;
  }

}

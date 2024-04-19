package org.goplanit.assignment.traditionalstatic;

import java.util.Calendar;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.goplanit.algorithms.shortest.ShortestPathDijkstra;
import org.goplanit.algorithms.shortest.ShortestPathResult;
import org.goplanit.assignment.StaticTrafficAssignment;
import org.goplanit.cost.Cost;
import org.goplanit.cost.CostUtils;
import org.goplanit.gap.LinkBasedRelativeDualityGapFunction;
import org.goplanit.interactor.LinkVolumeAccessee;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.layer.macroscopic.MacroscopicNetworkLayerImpl;
import org.goplanit.od.demand.OdDemands;
import org.goplanit.od.path.OdPathMatrix;
import org.goplanit.od.skim.OdSkimMatrix;
import org.goplanit.output.adapter.OutputTypeAdapter;
import org.goplanit.output.configuration.OdOutputTypeConfiguration;
import org.goplanit.output.enums.OdSkimSubOutputType;
import org.goplanit.output.enums.OutputType;
import org.goplanit.path.ManagedDirectedPathFactoryImpl;
import org.goplanit.sdinteraction.smoothing.IterationBasedSmoothing;
import org.goplanit.utils.arrays.ArrayUtils;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.network.virtual.CentroidVertex;
import org.goplanit.utils.path.ManagedDirectedPath;
import org.goplanit.utils.path.ManagedDirectedPathFactory;
import org.goplanit.utils.time.TimePeriod;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.utils.zoning.Zone;

/**
 * Traditional static assignment traffic component.This is the class that conducts the actual assignment.
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
   * Simulation data for this equilibration process
   */
  private TraditionalStaticAssignmentSimulationData simulationData;

  /**
   * the layer used for this assignment
   */
  private MacroscopicNetworkLayerImpl networkLayer;

  /** to generate paths we use a path factory that is configured to generate appropriate ids */
  private ManagedDirectedPathFactory localPathFactory;

  /** have a mapping between zone and connectoid to the layer by means of its centroid vertex */
  private Map<OdZone, CentroidVertex> zone2VertexMapping;

  /**
   * create the logging prefix for logging statements during equilibration
   * 
   * @return prefix
   */
  protected String createLoggingPrefix() {
    return super.createLoggingPrefix(simulationData.getIterationIndex());
  }

  /**
   * Verify if the network contains a single compatible infrastructure layer as traditional static assignment does not support intermodal network layers
   *
   * @throws PlanItException thrown if the components are not compatible
   */
  @Override
  protected void verifyNetworkDemandZoningCompatibility() throws PlanItException {
    /* network compatibility */
    PlanItException.throwIf(!(getInfrastructureNetwork() instanceof MacroscopicNetwork), "Traditional static assignment is only compatible with macroscopic networks");
    var macroscopicNetwork = (MacroscopicNetwork) getInfrastructureNetwork();
    PlanItException.throwIf(macroscopicNetwork.getTransportLayers().size() != 1,
        "Traditional static assignment  is currently only compatible with networks using a single infrastructure layer");
    var infrastructureLayer = macroscopicNetwork.getTransportLayers().getFirst();
    if (getInfrastructureNetwork().getModes().size() != infrastructureLayer.getSupportedModes().size()) {
      LOGGER.warning("network wide modes do not match modes supported by the single available layer, consider removing unused modes");
    }

    /* register the layer */
    this.networkLayer = (MacroscopicNetworkLayerImpl) infrastructureLayer;
  }

  /**
   * Verify if a supported gap function is used
   */
  @Override
  protected void verifyComponentCompatibility() {

    /* gap function check */
    PlanItRunTimeException.throwIf(!(getGapFunction() instanceof LinkBasedRelativeDualityGapFunction),
        "Traditional static assignment only supports link based relative duality gap function at the moment, but found %s", getGapFunction().getClass().getCanonicalName());

  }

  /**
   * Initialize running simulation variables for the time period
   *
   * @param timePeriod the time period
   * @param modes      set of modes covered by this assignment
   * @throws PlanItException thrown if there is an error
   */
  private void initialiseTimePeriod(TimePeriod timePeriod, final Set<Mode> modes) throws PlanItException {
    simulationData = new TraditionalStaticAssignmentSimulationData(getIdGroupingToken());
    simulationData.setIterationIndex(0);
    simulationData.getModeSpecificData().clear();
    for (var mode : modes) {
      // flow initialisation
      simulationData.getModeSpecificData().put(mode, new ModeData(new double[getTotalNumberOfNetworkSegments()]));
      // cost initialisation
      final double[] modalLinkSegmentCosts = initialiseLinkSegmentCosts(mode, timePeriod);
      simulationData.setModalLinkSegmentCosts(mode, modalLinkSegmentCosts);
    }

    /*
     * paths ought to have unique ids (at least their XML ids) within the context of the network layer where they are used, so we must use the network layer id grouping token to
     * ensure this when creating paths based on the shortest path algorithm used
     */
    if (this.localPathFactory == null) {
      this.localPathFactory = new ManagedDirectedPathFactoryImpl(networkLayer.getLayerIdGroupingToken());
    }

    /* construct mapping from OdZone to centroidVertex which is needed for path finding among other things, where we get an OD but need to find a path from
     * centroid vertex to centroid vertex */
    this.zone2VertexMapping = getZoning().getVirtualNetwork().getCentroidVertices().stream().filter(
        cVertex -> (cVertex.getParent().getParentZone() instanceof OdZone)).collect( // filter OdZones
            Collectors.toMap(cVertex -> (OdZone) cVertex.getParent().getParentZone(), cVertex -> cVertex)); // map to zone

    /* register new time period on costs */
    getPhysicalCost().updateTimePeriod(timePeriod);
    getVirtualCost().updateTimePeriod(timePeriod);
  }

  /**
   * Apply smoothing based on current and previous flows and the adopted smoothing method. The smoothed results are registered as the current segment flows while the current
   * segment flows are assigned to the previous segment flows (which are discarded).
   *
   * @param mode     the current mode
   * @param modeData data for the current mode
   */
  private void applySmoothing(Mode mode, final ModeData modeData) {
    final double[] smoothedSegmentFlows = getSmoothing().execute(modeData.getCurrentSegmentFlows(), modeData.getNextSegmentFlows(), getTotalNumberOfNetworkSegments());
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

    final var shortestPathAlgorithm = new ShortestPathDijkstra(modalNetworkSegmentCosts, getTotalNumberOfNetworkVertices());
    final OdDemands odDemands = getDemands().get(mode, timePeriod);

    final var dualityGapFunction = ((LinkBasedRelativeDualityGapFunction) getGapFunction());
    final var odPaths = simulationData.getOdPaths(mode);
    final Map<OdSkimSubOutputType, OdSkimMatrix> skimMatrixMap = simulationData.getSkimMatrixMap(mode);

    // loop over all available OD demands
    long previousOriginZoneId = -1;
    // track the cost to reach each vertex in the network and the shortest path
    // segment used to get there
    ShortestPathResult shortestPathResult = null;
    for (final var odDemandMatrixIter = odDemands.iterator(); odDemandMatrixIter.hasNext();) {
      final double odDemand = odDemandMatrixIter.next();
      final Zone currentOriginZone = odDemandMatrixIter.getCurrentOrigin();
      final Zone currentDestinationZone = odDemandMatrixIter.getCurrentDestination();

      if (currentOriginZone.getId() != currentDestinationZone.getId()) {
        if (getOutputManager().getOutputConfiguration().isPersistZeroFlow() || Precision.positive(odDemand)) {

          if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(LoggingUtils.runIdPrefix(getId()) + String.format("(O,D)=(%d,%d) --> demand (pcu/h): %f (mode: %d)", currentOriginZone.getExternalId(),
                currentDestinationZone.getExternalId(), odDemand, mode.getExternalId()));
          }

          /* new shortest path tree for current origin */
          if (previousOriginZoneId != currentOriginZone.getId()) {
            var originCentroidVertex = zone2VertexMapping.get(currentOriginZone);
            if (!originCentroidVertex.hasExitEdgeSegments()) {
              throw new PlanItException(String.format("Edge segments have not been assigned to Centroid for Zone %d", currentOriginZone.getExternalId()));
            }

            // UPDATE SHORTEST PATHS
            shortestPathResult = shortestPathAlgorithm.executeOneToAll(originCentroidVertex);
          }

          if (Precision.positive(odDemand)) {
            var destinationCentroidVertex = zone2VertexMapping.get(currentDestinationZone);
            double odShortestPathCost = shortestPathResult.getCostToReach(destinationCentroidVertex);
            if (odShortestPathCost == Double.POSITIVE_INFINITY || odShortestPathCost == Double.MAX_VALUE) {
              LOGGER.warning(String.format("%s impossible path from origin zone %s (id:%d) to destination zone %s (id:%d) for mode %s (id:%d)", createLoggingPrefix(),
                  currentOriginZone.getXmlId(), currentOriginZone.getId(), currentDestinationZone.getXmlId(), currentDestinationZone.getId(), mode.getXmlId(), mode.getId()));
            } else {
              updateNetworkFlowsForPath(shortestPathResult, zone2VertexMapping.get(currentOriginZone), destinationCentroidVertex, odDemand, currentModeData);
              dualityGapFunction.increaseConvexityBound(odDemand * odShortestPathCost);
            }
          }
          previousOriginZoneId = currentOriginZone.getId();

          /* update skim and path data if needed */

          updateODOutputData(skimMatrixMap, currentOriginZone, currentDestinationZone, shortestPathResult);
          updatePathOutputData(mode, odPaths, currentOriginZone, currentDestinationZone, shortestPathResult);
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
    final double totalModeSystemTravelTime = ArrayUtils.dotProduct(currentModeData.getCurrentSegmentFlows(), modalNetworkSegmentCosts, getTotalNumberOfNetworkSegments());

    final LinkBasedRelativeDualityGapFunction dualityGapFunction = ((LinkBasedRelativeDualityGapFunction) getGapFunction());
    dualityGapFunction.increaseMeasuredCost(totalModeSystemTravelTime);
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
    LOGGER.fine(LoggingUtils.runIdPrefix(getId()) + String.format("[mode %s (id:%d)]", mode.getExternalId(), mode.getId()));

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
  private void updateNetworkFlowsForPath(final ShortestPathResult shortestPathResult, final CentroidVertex origin, final CentroidVertex destination, final double odDemand,
      final ModeData currentModeData) throws PlanItException {

    // prep
    EdgeSegment currentEdgeSegment = null;
    Vertex currentVertex = destination;

    while (currentVertex.getId() != origin.getId()) {

      currentEdgeSegment = shortestPathResult.getNextEdgeSegmentForVertex(currentVertex);

      if (currentEdgeSegment == null) {
        PlanItException.throwIf(currentVertex instanceof CentroidVertex,
            "The solution could not find an Edge Segment for the connectoid for zone " + ((CentroidVertex) currentVertex).getParent().getParentZone().getExternalId());
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
   * @param shortestPathResult     costs for the shortest path results for the specified mode and origin-any destination
   */
  private void updateODOutputData(
      final Map<OdSkimSubOutputType, OdSkimMatrix> skimMatrixMap, final Zone currentOriginZone, final Zone currentDestinationZone, final ShortestPathResult shortestPathResult) {

    if (getOutputManager().isOutputTypeActive(OutputType.OD)) {
      var activeSubOutputTypes = getOutputManager().getOutputTypeConfiguration(OutputType.OD).getActiveSubOutputTypes();
      for (final var odSkimOutputType : activeSubOutputTypes) {
        if (odSkimOutputType.equals(OdSkimSubOutputType.COST)) {

          // Collect cost to get to vertex from shortest path ONE-TO-ALL information directly
          final double odGeneralisedCost = shortestPathResult.getCostToReach(zone2VertexMapping.get(currentDestinationZone));
          final OdSkimMatrix odSkimMatrix = skimMatrixMap.get(odSkimOutputType);
          odSkimMatrix.setValue(currentOriginZone, currentDestinationZone, odGeneralisedCost);
        }
      }
    }
  }

  /**
   * Update the OD path matrix
   *
   * @param mode               the path is to be stored for (logging purposes only)
   * @param odpathMatrix       OD path matrix to add to
   * @param origin             origin zone
   * @param destination        destination zone
   * @param shortestPathResult shortest path tree for given origin
   */
  private void updatePathOutputData(Mode mode, OdPathMatrix odpathMatrix, Zone origin, Zone destination, ShortestPathResult shortestPathResult) {

    // TODO: we are now creating a path separate from finding shortest path. This makes no sense as it is very costly when switched on
    if (getOutputManager().isOutputTypeActive(OutputType.PATH)) {
      final ManagedDirectedPath path = shortestPathResult.createPath(
          localPathFactory, zone2VertexMapping.get(origin), zone2VertexMapping.get(destination));
      if (path == null) {
        LOGGER.fine(String.format("Unable to create path from origin %s (id:%d) to destination %s (id:%d) for mode %s (id:%d)", origin.getXmlId(), origin.getId(),
            destination.getXmlId(), destination.getId(), mode.getXmlId(), mode.getId()));
      }
      odpathMatrix.setValue(origin, destination, path);
    }
  }

  /**
   * Record the time an iteration took
   *
   * @param startTime           the original start time of the iteration
   * @param measuredNetworkCost the measured system wide cost
   * @param dualityGap          the duality gap at the end of the iteration
   * @return the time (in ms) at the end of the iteration for profiling purposes only
   */
  private Calendar logBasicIterationInformation(final Calendar startTime, final double measuredNetworkCost, final double dualityGap) {
    final Calendar currentTime = Calendar.getInstance();
    LOGGER.info(createLoggingPrefix() + String.format("Network cost: %f", measuredNetworkCost));
    LOGGER.info(createLoggingPrefix() + String.format("Gap: %.10f (%d ms)", dualityGap, currentTime.getTimeInMillis() - startTime.getTimeInMillis()));
    return currentTime;
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
    if (this.initialLinkSegmentCostTimePeriodAgnostic == null || !this.initialLinkSegmentCostTimePeriodAgnostic.isSegmentCostsSetForMode(mode)) {
      return false;
    }
    populateCost(this.initialLinkSegmentCostTimePeriodAgnostic, mode, segmentCostToPopulate);
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
    final var initialLinkSegmentCostForTimePeriod = initialLinkSegmentCostByTimePeriod.get(timePeriod);
    if (initialLinkSegmentCostForTimePeriod == null || !initialLinkSegmentCostForTimePeriod.isSegmentCostsSetForMode(mode)) {
      return populateToInitialCost(mode, segmentCostToPopulate);
    }
    populateCost(initialLinkSegmentCostForTimePeriod, mode, segmentCostToPopulate);
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
   */
  private void populateCost(Cost<MacroscopicLinkSegment> cost, final Mode mode, final double[] costsToPopulate) {
    for (var linkSegment : networkLayer.getLinkSegments()) {
      double currentSegmentCost = cost.getGeneralisedCost(mode, linkSegment);
      if (currentSegmentCost < 0.0) {
        throw new PlanItRunTimeException(String.format("link segment cost is negative for link segment %d (id: %d)", linkSegment.getExternalId(), linkSegment.getId()));
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
    final double[] currentSegmentCosts = CostUtils.createEmptyLinkSegmentCostArray(getInfrastructureNetwork(), getZoning());

    /* virtual component */
    CostUtils.populateModalVirtualLinkSegmentCosts(mode, getVirtualCost(), getZoning().getVirtualNetwork(), currentSegmentCosts);

    /* physical component */
    if(populateToInitialCost(mode, timePeriod, currentSegmentCosts)) {
      return currentSegmentCosts;
    } else {
      CostUtils.populateModalPhysicalLinkSegmentCosts(mode, getPhysicalCost(), getInfrastructureNetwork(), currentSegmentCosts);
      return currentSegmentCosts;
    }
  }

  /**
   * Collect the modal link and connectoid segment costs based on the current state of the cost components
   *
   * @param mode       current mode
   * @return array containing costs for each link segment
   */
  private double[] collectModalLinkSegmentCosts(final Mode mode) {
    return CostUtils.createAndPopulateModalSegmentCost(mode, getVirtualCost(), getPhysicalCost(), getInfrastructureNetwork(), getZoning());
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

    // full reset
    final LinkBasedRelativeDualityGapFunction dualityGapFunction = ((LinkBasedRelativeDualityGapFunction) getGapFunction());
    dualityGapFunction.reset();

    boolean converged = false;
    Calendar iterationStartTime = Calendar.getInstance();

    while (!converged) {
      // iteration reset
      dualityGapFunction.resetIteration();

      // prep smoothing for this iteration
      var smoothing = getSmoothing();
      if(smoothing instanceof IterationBasedSmoothing) {
        ((IterationBasedSmoothing) getSmoothing()).updateIteration(simulationData.getIterationIndex());
      }else{
        throw new PlanItRunTimeException("Currently traditional static assignment only supports iteration based smoothing options such as MSA");
      }
      getSmoothing().updateStepSize();

      // NETWORK LOADING - PER MODE
      for (final Mode mode : modes) {

        // :TODO ugly -> you are not resetting 1 matrix but multiple, NAMES ARE WRONG
        // :TODO: slow -> only reset or do something when it is stored in the first place, this is not checked
        if (getOutputManager().isOutputTypeActive(OutputType.OD)) {
          simulationData.resetSkimMatrix(mode, getTransportNetwork().getZoning().getOdZones(),
              (OdOutputTypeConfiguration) getOutputManager().getOutputTypeConfiguration(OutputType.OD));
        }
        if (getOutputManager().isOutputTypeActive(OutputType.PATH)) {
          simulationData.resetPathMatrix(mode, getTransportNetwork().getZoning().getOdZones());
        }

        /* execute */
        executeAndSmoothTimePeriodAndMode(timePeriod, mode);
      }

      // TODO: questionable if we should update iteration index before logging/persistence/convergence check... see if we can change this
      // which would seem more logical
      dualityGapFunction.computeGap();
      simulationData.incrementIterationIndex();
      iterationStartTime = logBasicIterationInformation(iterationStartTime, dualityGapFunction.getMeasuredNetworkCost(), dualityGapFunction.getGap());

      for (var mode : modes) {
        final double[] modalLinkSegmentCosts = collectModalLinkSegmentCosts(mode);
        simulationData.setModalLinkSegmentCosts(mode, modalLinkSegmentCosts);
      }
      converged = dualityGapFunction.hasConverged(simulationData.getIterationIndex());
      getOutputManager().persistOutputData(timePeriod, modes, converged);
    }
  }

  /**
   * Return the simulation data for the current iteration
   *
   * @return simulation data
   */
  protected TraditionalStaticAssignmentSimulationData getIterationData() {
    return simulationData;
  }

  /**
   * Base Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public TraditionalStaticAssignment(IdGroupingToken groupId) {
    super(groupId);
    this.simulationData = null;
    this.localPathFactory = null;
  }

  /**
   * Copy Constructor
   * 
   * @param traditionalStaticAssignment to copy
   */
  public TraditionalStaticAssignment(TraditionalStaticAssignment traditionalStaticAssignment) {
    super(traditionalStaticAssignment, false);

    this.localPathFactory = traditionalStaticAssignment.localPathFactory;
    this.networkLayer = traditionalStaticAssignment.networkLayer;

    //todo: even shallow clones should copy simulation data given that it is essentially an extension of this class
    //      with containers
    this.simulationData = traditionalStaticAssignment.simulationData;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicNetwork getInfrastructureNetwork() {
    return (MacroscopicNetwork) super.getInfrastructureNetwork();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputTypeAdapter createOutputTypeAdapter(final OutputType outputType) {
    OutputTypeAdapter outputTypeAdapter = null;
    switch (outputType) {
    case LINK:
      outputTypeAdapter = new TraditionalStaticAssignmentLinkOutputTypeAdapter(outputType, this);
      break;
    case OD:
      outputTypeAdapter = new TraditionalStaticAssignmentOdOutputTypeAdapter(outputType, this);
      break;
    case PATH:
      outputTypeAdapter = new TraditionalStaticPathOutputTypeAdapter(outputType, this);
      break;
    default:
      LOGGER.warning(LoggingUtils.runIdPrefix(getId()) + outputType.value() + " has not been defined yet.");
    }
    return outputTypeAdapter;
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
  public double getLinkSegmentVolume(final LinkSegment linkSegment) {
    return simulationData.collectTotalNetworkSegmentFlow(linkSegment);
  }

  /**
   * #{@inheritDoc}
   */
  @Override
  public double[] getLinkSegmentVolumes() {
    return simulationData.collectTotalNetworkSegmentFlows();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TraditionalStaticAssignment shallowClone() {
    return new TraditionalStaticAssignment(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TraditionalStaticAssignment deepClone() {
    throw new PlanItRunTimeException("Deep clone not yet implemented");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> collectSettingsAsKeyValueMap() {
    return null;
  }

}

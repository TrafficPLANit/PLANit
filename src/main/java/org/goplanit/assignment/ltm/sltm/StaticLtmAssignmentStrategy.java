package org.goplanit.assignment.ltm.sltm;

import java.util.BitSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.goplanit.assignment.ltm.sltm.loading.SplittingRateData;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmNetworkLoading;
import org.goplanit.cost.physical.AbstractPhysicalCost;
import org.goplanit.cost.virtual.AbstractVirtualCost;
import org.goplanit.demands.Demands;
import org.goplanit.gap.GapFunction;
import org.goplanit.interactor.TrafficAssignmentComponentAccessee;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.transport.TransportModelNetwork;
import org.goplanit.od.demand.OdDemands;
import org.goplanit.sdinteraction.smoothing.Smoothing;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.Modes;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.virtual.CentroidVertex;
import org.goplanit.utils.network.virtual.ConnectoidSegment;
import org.goplanit.utils.network.virtual.VirtualNetwork;
import org.goplanit.utils.time.TimePeriod;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.utils.zoning.OdZones;
import org.goplanit.utils.zoning.Zone;

/**
 * Base class for dealing with different assignment solution methods within sLTM. These solution methods differ regarding their approach to representing path choices, e.g. bush
 * based, or path based.
 * 
 * @author markr
 *
 */
public abstract class StaticLtmAssignmentStrategy {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(StaticLtmAssignmentStrategy.class.getCanonicalName());

  // INPUTS

  /** token to use for underlying instances */
  private final IdGroupingToken idGroupingToken;

  /** parent assignment id */
  private final long assignmentId;

  /**
   * Transport model network used
   */
  private final TransportModelNetwork transportModelNetwork;

  /**
   * Network loading to use
   */
  private StaticLtmNetworkLoading networkLoading;

  /** static LTM specific settings to use */
  private final StaticLtmSettings settings;

  /** the user configured traffic assignment components used */
  private final TrafficAssignmentComponentAccessee taComponents;

  /**
   * Track which nodes were potentially blocking in previous iteration to ensure costs are updated for these nodes even when they are no longer blocking in the current iteration
   */
  private final BitSet prevIterationPotentiallyBlocking;

  /** have a mapping between zone and connectoid to the layer by means of its centroid vertex */
  private Map<Zone, CentroidVertex> zone2VertexMapping;

  /**
   * The transport model network used
   * 
   * @return transport model network
   */
  protected TransportModelNetwork getTransportNetwork() {
    return transportModelNetwork;
  }

  /**
   * The physical network used
   * 
   * @return the infrastructure network
   */
  protected MacroscopicNetwork getInfrastructureNetwork() {
    return (MacroscopicNetwork) getTransportNetwork().getInfrastructureNetwork();
  }

  /**
   * Token to use
   * 
   * @return token
   */
  protected IdGroupingToken getIdGroupingToken() {
    return idGroupingToken;
  }

  /**
   * Collect parent assignment id
   * 
   * @return parent assignment id
   */
  protected long getAssignmentId() {
    return assignmentId;
  }

  /**
   * The network loading to apply
   * 
   * @return network loading
   */
  protected StaticLtmNetworkLoading getLoading() {
    return networkLoading;
  }

  /**
   * The settings
   * 
   * @return settings
   */
  protected StaticLtmSettings getSettings() {
    return settings;
  }

  /**
   * Get od demands used for a given mode in the current time period
   *
   * @param mode to use
   * @return odDemands used
   */
  protected OdDemands getOdDemands(Mode mode) {
    return getLoading().getOdDemands(mode);
  }

  /**
   * collect a component based on its class component signature key
   * 
   * @param <T>                 key type of component
   * @param taComponentClassKey class of component
   * @return found component, if any
   */
  protected <T> T getTrafficAssignmentComponent(final Class<T> taComponentClassKey) {
    return taComponents.getTrafficAssignmentComponent(taComponentClassKey);
  }

  /**
   * Convenience access to smoothing component
   * @return smoothing component
   */
  protected Smoothing getSmoothing(){
    var smoothing = (Smoothing) getTrafficAssignmentComponent(Smoothing.class);
    return smoothing;
  }

  /**
   * Verify existence of a component based on its class component signature key
   *
   * @param <T>                 key type of component
   * @param taComponentClassKey class of component
   * @return found component, if any
   */
  protected <T> boolean hasTrafficAssignmentComponent(final Class<T> taComponentClassKey) {
    return taComponents.hasTrafficAssignmentComponent(taComponentClassKey);
  }

  /** map zone to centroid vertex
   *
   * @param zone to map
   * @return vertex found
   */
  protected CentroidVertex findCentroidVertex(OdZone zone){
    return zone2VertexMapping.get(zone);
  }

  /**
   * Verify convergence progress and if insufficient attempt to activate one or more extensions to overcome convergence difficulties
   *
   * @param mode to use
   * @param networkLoading               to verify progress on
   * @param networkLoadingIterationIndex we are at
   */
  protected void verifyNetworkLoadingConvergenceProgress(Mode mode, StaticLtmNetworkLoading networkLoading, int networkLoadingIterationIndex) {
    /*
     * whenever the current form of the solution method does not suffice, we move to the next extension which attempts to be more cautious and has a higher likelihood of finding a
     * solution at the cost of slower convergence, so whenever we are not yet stuck, we try to avoid activating these extensions.
     */
    if (!networkLoading.isConverging()) {
      // dependent on whether we are modelling physical queues or not and where we started with settings
      boolean changedScheme = networkLoading.activateNextExtension(mode,true);
      if (!changedScheme) {
        LOGGER.warning(
            String.format("%sDetected network loading is not converging as expected (internal loading iteration %d) - unable to activate further extensions, consider aborting",
                LoggingUtils.runIdPrefix(getAssignmentId()), networkLoadingIterationIndex));
      }
    }
  }

  /**
   * Perform a network loading based on the current assignment state
   *
   * @param mode to use
   */
  protected void executeNetworkLoading(Mode mode) {

    /* for now, we do not consider path choice, we conduct a one-shot all-or-nothing network loading */
    int networkLoadingIterationIndex = 0;
    getLoading().stepZeroIterationInitialisation(mode, getSettings().isDetailedLogging());
    do {

      /* verify if progress is being made and if not activate extensions as deemed adequate */
      verifyNetworkLoadingConvergenceProgress(mode, getLoading(), networkLoadingIterationIndex);

      /* STEP 1 - Splitting rates update before sending flow update */
      getLoading().stepOneSplittingRatesUpdate(mode);

      /* STEP 2 - Sending flow update (including node model update) */
      getLoading().stepTwoInflowSendingFlowUpdate(mode);

      /* STEP 3 - Splitting rates update before receiving flow update */
      getLoading().stepThreeSplittingRateUpdate(mode);

      /* STEP 4 - Receiving flow update */
      getLoading().stepFourOutflowReceivingFlowUpdate(mode);

      /* STEP 5 - Network loading convergence */
    } while (!getLoading().stepFiveCheckNetworkLoadingConvergence(networkLoadingIterationIndex++));
  }

  /**
   * Factory method to create the desired network loading
   * 
   * @return network loading for this solution approach
   */
  protected abstract StaticLtmNetworkLoading createNetworkLoading();

  /**
   * Perform an update of the network wide costs where a partial update is applied in case only potentially blocking nodes are updated during the loading
   * 
   * @param theMode                                to perform the update for
   * @param updateOnlyPotentiallyBlockingNodeCosts flag indicating if only the costs of the entry link segments of potentially blocking nodes are to be updated, or all link segment
   *                                               costs are to be updated
   * @param costsToUpdate                          the network wide costs to update (fully or partially), this is an output
   */
  protected void executeNetworkCostsUpdate(Mode theMode, boolean updateOnlyPotentiallyBlockingNodeCosts, double[] costsToUpdate){

    final AbstractPhysicalCost physicalCost = getTrafficAssignmentComponent(AbstractPhysicalCost.class);
    final AbstractVirtualCost virtualCost = getTrafficAssignmentComponent(AbstractVirtualCost.class);
    SplittingRateData splittingRateData = getLoading().getSplittingRateData();
    if (updateOnlyPotentiallyBlockingNodeCosts) {

      MacroscopicNetworkLayer networkLayer = getInfrastructureNetwork().getLayerByMode(theMode);
      VirtualNetwork virtualLayer = getTransportNetwork().getZoning().getVirtualNetwork();

      /* only update when node is both (flow) tracked as well as potentially blocking */
      boolean currentlyPotentiallyBlocking = false;
      for (var trackedFlowNode : splittingRateData.getTrackedNodes()) {
        currentlyPotentiallyBlocking = splittingRateData.isPotentiallyBlocking(trackedFlowNode);
        if (!currentlyPotentiallyBlocking && !prevIterationPotentiallyBlocking.get((int) trackedFlowNode.getId())) {
          continue;
        }

        /* entry segments */
        final var layerSegments = networkLayer.getLinkSegments();
        final var virtualLayerSegments = virtualLayer.getConnectoidSegments();
        trackedFlowNode.getEntryEdgeSegments().forEach(es -> {
          if(layerSegments.containsKey(es.getId())){
            costsToUpdate[(int) es.getId()] = physicalCost.getGeneralisedCost(theMode, (MacroscopicLinkSegment) es);
          }else if(virtualLayerSegments.containsKey(es.getId())){
            costsToUpdate[(int) es.getId()] = virtualCost.getGeneralisedCost(theMode, (ConnectoidSegment) es);
          }
        });
        prevIterationPotentiallyBlocking.set((int) trackedFlowNode.getId(), currentlyPotentiallyBlocking);
      }
    }
    /* OTHER -> all nodes (and attached links) are updated, update all costs */
    else {

      /* virtual cost */
      virtualCost.populateWithCost(getTransportNetwork().getVirtualNetwork(), theMode, costsToUpdate);

      /* physical cost */
      physicalCost.populateWithCost(getInfrastructureNetwork().getLayerByMode(theMode), theMode, costsToUpdate);

    }
  }

  /**
   * Construct the dCost/dFlow per link segment for the purpose of determining steps towards equilibrium. Derivatives are
   * calculated base don the adopted physical and virtual cost components and existing flows in the underlying network loading
   * iteration
   *
   * @param theMode mode to populate for
   * @param updateOnlyPotentiallyBlockingNodeCosts when true we only consider incoming links into potentiall blocking nodes (assuming
   *                                               dCost/dFlow is zero for all non-blocking nodes which is only valid if a linear free flow
   *                                               branch for the Fundamental diagram is adopted.
   * @return resulting dCostDFlow per link segment involved (zero for non-touched link segments)
   */
  protected double[] constructLinkBasedDCostDFlow(Mode theMode, boolean updateOnlyPotentiallyBlockingNodeCosts){
    double[] linkBasedDCostDFlow = new double[getTransportNetwork().getNumberOfEdgeSegmentsAllLayers()];

    final AbstractPhysicalCost physicalCost = getTrafficAssignmentComponent(AbstractPhysicalCost.class);
    final AbstractVirtualCost virtualCost = getTrafficAssignmentComponent(AbstractVirtualCost.class);
    final double[] acceptanceFactors = this.networkLoading.getCurrentFlowAcceptanceFactors();
    final SplittingRateData splittingRateData = getLoading().getSplittingRateData();

    MacroscopicNetworkLayer networkLayer = getInfrastructureNetwork().getLayerByMode(theMode);
    final var physicalLayerSegments = networkLayer.getLinkSegments();
    VirtualNetwork virtualLayer = getTransportNetwork().getZoning().getVirtualNetwork();
    final var virtualLayerSegments = virtualLayer.getConnectoidSegments();

    if (updateOnlyPotentiallyBlockingNodeCosts) {

      /* only update when node is both (flow) tracked as well as potentially blocking */
      boolean currentlyPotentiallyBlocking = false;
      for (var trackedFlowNode : splittingRateData.getTrackedNodes()) {
        currentlyPotentiallyBlocking = splittingRateData.isPotentiallyBlocking(trackedFlowNode);
        if (!currentlyPotentiallyBlocking && !prevIterationPotentiallyBlocking.get((int) trackedFlowNode.getId())) {
          continue;
        }

        /* entry segments */
        trackedFlowNode.getEntryEdgeSegments().forEach(es -> {
          boolean congested = acceptanceFactors[(int) es.getId()] < 1;
          if(physicalLayerSegments.containsKey(es.getId())){
            linkBasedDCostDFlow[(int) es.getId()] = physicalCost.getDTravelTimeDFlow(!congested, theMode, (MacroscopicLinkSegment) es);
          }else if(virtualLayerSegments.containsKey(es.getId())){
            linkBasedDCostDFlow[(int) es.getId()] = virtualCost.getDTravelTimeDFlow(!congested, theMode, (ConnectoidSegment) es);
          }
        });
      }
    }
    /* OTHER -> all nodes (and attached links) are updated, update all costs */
    else {

      /* virtual cost */
      for (var linkSegment : virtualLayerSegments) {
        boolean congested = acceptanceFactors[(int) linkSegment.getId()] < 1;
        linkBasedDCostDFlow[(int) linkSegment.getId()] = virtualCost.getDTravelTimeDFlow(congested, theMode, linkSegment);
      }

      /* physical cost */
      for (var linkSegment : physicalLayerSegments) {
        boolean congested = acceptanceFactors[(int) linkSegment.getId()] < 1;
        linkBasedDCostDFlow[(int) linkSegment.getId()] = physicalCost.getDTravelTimeDFlow(congested, theMode, linkSegment);
      }
    }

    return linkBasedDCostDFlow;
  }

  /**
   * Constructor
   * 
   * @param idGroupingToken       to use for id generation
   * @param assignmentId          id of the parent assignment
   * @param transportModelNetwork the transport model network
   * @param settings              the sLTM settings to use
   * @param taComponents          to use
   */
  public StaticLtmAssignmentStrategy(final IdGroupingToken idGroupingToken, long assignmentId, final TransportModelNetwork transportModelNetwork, final StaticLtmSettings settings,
      final TrafficAssignmentComponentAccessee taComponents) {
    this.transportModelNetwork = transportModelNetwork;
    this.assignmentId = assignmentId;
    this.idGroupingToken = idGroupingToken;
    this.settings = settings;
    this.taComponents = taComponents;
    this.prevIterationPotentiallyBlocking = new BitSet(transportModelNetwork.getNumberOfVerticesAllLayers());

    /* construct mapping from OdZone to centroidVertex which is needed for path finding among other things, where we get an OD but need to find a path from
     * centroid vertex to centroid vertex */
    this.zone2VertexMapping = transportModelNetwork.createZoneToCentroidVertexMapping(true /*include OdZones */, false /* exclude transfer zones */);
  }

  /**
   * Invoked before start of equilibrating a new time period
   * 
   * @param timePeriod  to initialise for
   * @param modes       to initialise for
   * @param demands     to use
   */
  public void updateTimePeriod(final TimePeriod timePeriod, final Set<Mode> modes, final Demands demands) {
    this.networkLoading = createNetworkLoading();
    this.networkLoading.initialiseInputs(timePeriod, modes, demands, getTransportNetwork());
  }

  /**
   * Verify if assignment has converged, which, means computing ths gap and determining if it has converged in this iteration in this default setup
   * 
   * @param gapFunction    to use
   * @param iterationIndex to use
   * @return true when considered converged, false otherwise
   */
  public boolean hasConverged(GapFunction gapFunction, int iterationIndex) {
    gapFunction.computeGap();
    return gapFunction.hasConverged(iterationIndex);
  }

  /**
   * Create the initial solution to start the equilibration process with
   *
   * @param mode the mode to create initialise solution for
   * @param odZones odZones of the time period and assignment
   * @param initialLinkSegmentCosts to use for this mode
   * @param iterationIndex          to use
   */
  public abstract void createInitialSolution(Mode mode, OdZones odZones, double[] initialLinkSegmentCosts, int iterationIndex);

  /**
   * Perform a single iteration where we perform a loading and then an equilibration step resulting in updated costs
   *
   * @param theMode        to use
   * @param prevCosts  the link segment costs we experienced during the previous iteration (for all link segments considered in the loading)
   * @param costsToUpdate  the link segment costs we are updating (possibly partially for all link segments that might have been affected by a loading)
   * @param simulationData tracking relevant simulation information for the strategy
   * @return true when iteration could be successfully completed, false otherwise
   */
  public abstract boolean performIteration(
          final Mode theMode, final double[] prevCosts, final double[] costsToUpdate, final StaticLtmSimulationData simulationData);

  /**
   * Description of the chosen sLTM strategy for equilibration
   * 
   * @return String
   */
  public abstract String getDescription();

  /**
   * Verify if strategy used has certain dependencies that require checking.
   * To be automatically invoked as part of assignment component compatibility verification
   */
  public void verifyComponentCompatibility() {
    // optional for derived classes
  }
}

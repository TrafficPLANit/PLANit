package org.goplanit.assignment.ltm.sltm.loading;

import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.goplanit.algorithms.nodemodel.TampereNodeModel;
import org.goplanit.algorithms.nodemodel.TampereNodeModelUtils;
import org.goplanit.assignment.ltm.sltm.common.LinkSegmentData;
import org.goplanit.assignment.ltm.sltm.input.StaticLtmSettings;
import org.goplanit.assignment.ltm.sltm.consumer.nodemodel.NodeModelRunResultConsumer;
import org.goplanit.assignment.ltm.sltm.consumer.nodemodel.NMRUpdateEntryLinksOutflowConsumer;
import org.goplanit.assignment.ltm.sltm.consumer.nodemodel.NMRUpdateExitLinkInflowsConsumer;
import org.goplanit.assignment.ltm.sltm.consumer.nodemodel.NodeModelRunTurnBasedResultConsumer;
import org.goplanit.demands.Demands;
import org.goplanit.gap.NormBasedGapFunction;
import org.goplanit.gap.StopCriterion;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.transport.TransportModelNetwork;
import org.goplanit.utils.network.layer.physical.CompiledRelationIndex;
import org.goplanit.utils.network.layer.physical.CompiledRelationMapping;
import org.goplanit.zoning.zonetozone.OdDemands;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.CollectionUtils;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.virtual.graph.CentroidVertex;
import org.goplanit.utils.network.virtual.physical.ConnectoidSegment;
import org.goplanit.utils.network.virtual.VirtualNetwork;
import org.goplanit.utils.pcu.PcuCapacitated;
import org.goplanit.utils.time.TimePeriod;
import org.ojalgo.array.Array1D;
import org.ojalgo.array.Array2D;
import org.ojalgo.function.PrimitiveFunction;
import org.ojalgo.function.aggregator.Aggregator;

/**
 * Class exposing the various sLTM network loading solution method components of sLTM (not considering path choice,
 * this is assumed to be given). Network loading solution method based on Raadsen and Bliemer (2023) General
 * solution scheme for the Static Link Transmission Model .
 * 
 * @author markr
 *
 */
public abstract class StaticLtmNetworkLoading {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(StaticLtmNetworkLoading.class.getCanonicalName());

  // INPUTS //

  /** to use */
  private final IdGroupingToken idToken;

  /**
   * run id of the loading's parent assignment
   */
  private final long runId;

  /** transport network used */
  private TransportModelNetwork<MacroscopicNetwork, VirtualNetwork> transportNetwork;

  /** the network layer used, currently only a single layer is supported */
  private MacroscopicNetworkLayer networkLayer;

  /** Access to odDemands (by mode) to load */
  private Demands demands;

  /** current time period at hand */
  private TimePeriod timePeriod;

  // SIMULATION DATA //

  /**
   * initialise the way the solution scheme is applied. Which in case of storage constraints is the basic decomposition
   * scheme described in Raadsen and Bliemer (2021), and if storage constraints are disabled, it the basic point queue
   * model described in Bliemer et al (2014). Both solution schemes can be altered in case they do not converge by
   * activating various extensions see also {@link #activateNextExtension(Mode, boolean)}
   * 
   * @param logSolutionScheme when true log the set solution scheme, when false do not
   */
  private void initialiseStaticLtmSolutionSchemeApproach(boolean logSolutionScheme) {
    if (getSettings().isDisableStorageConstraints()) {
      solutionScheme = StaticLtmLoadingScheme.POINT_QUEUE_BASIC;
      if(getSettings().getNetworkLoadingInitialScheme() != StaticLtmLoadingScheme.NONE){
        var invalid = LoggingUtils.logWarningIf(LOGGER, "chosen initial solution scheme incompatible with " +
                        "point queue based loading",
                getSettings().getNetworkLoadingInitialScheme(), s -> !s.isPointQueue());
        solutionScheme = invalid ? solutionScheme : getSettings().getNetworkLoadingInitialScheme();
      }
    } else {
      solutionScheme = StaticLtmLoadingScheme.PHYSICAL_QUEUE_BASIC;
      if(getSettings().getNetworkLoadingInitialScheme() != StaticLtmLoadingScheme.NONE){
        var invalid = LoggingUtils.logWarningIf(LOGGER, "chosen initial solution scheme incompatible with " +
                        "physical queue based loading",
                getSettings().getNetworkLoadingInitialScheme(), s -> !s.isPhysicalQueue());
        solutionScheme = invalid ? solutionScheme : getSettings().getNetworkLoadingInitialScheme();
      }
    }

    if (logSolutionScheme) {
      LOGGER.info(String.format("sLTM network loading scheme set to %s", solutionScheme.getValue()));
    }
  }

  /**
   * Whenever we want to activate or track all node turn flows, either because of a change in solution approach, or to
   * be able to persist the results network wide this method ensures that the link segment sending flows are
   * re-initialised (across the whole network) and that for each node the splitting rate data is ready to be updated
   *
   * @param mode to use
   */
  private void initialiseTrackAllNodeTurnFlows(Mode mode) {
    /*
     * sending flows must be re-initialised since otherwise the sending flows of earlier non-tracked nodes have been
     * reset to zero during earlier loading iterations, now they must be available for all tracked nodes, so we
     * reinitialise by conducting a full initialisation based on paths/bushes and most recent flow acceptance factors
     */
    initialiseSendingFlows(mode, true);

    /*
     * Splitting rates must be re-initialised in this approach as well, a different splitting rate data is
     * created based on updated solution scheme Change from using only a small subset of nodes with splitting rates
     * to tracking splitting rates for all used nodes
     */
    initialiseNodeSplittingRateStatus();
  }

  /**
   * Initialise sending flows via network loading Eq. (3)-(4) in paper Initial sending flows: s_a=u_a for all link
   * segments. Optionally also update the unconstrained flow rates
   *
   * @param mode to use
   */
  private void initialiseSendingFlows(Mode mode, boolean updateUnconstrainedFlows) {
    this.nlSendingFlowData.resetCurrentSendingFlows();
    if(updateUnconstrainedFlows){
      this.unconstrainedFlowData.reset();
    }
    networkLoadingLinkSegmentSendingFlowUpdate(mode, updateUnconstrainedFlows);
    LinkSegmentData.copyTo(
        this.nlSendingFlowData.getCurrentSendingFlows(), this.nlSendingFlowData.getNextSendingFlows());
  }

  /**
   * Initialise receiving flows:
   * <p>
   * POINT QUEUE: r_a=C_a for all link segments
   * <p>
   * PHYSICAL QUEUE: r= storage capacity (not yet implemented)
   */
  private void initialiseReceivingFlows() {
    /* POINT QUEUE: */
    if (this.solutionScheme.isPointQueue()) {

      /* r_a = q_a */
      double[] currReceivingFlows = this.nlReceivingFlowData.getCurrentReceivingFlows();
      for (var linkSegment : networkLayer.getLinkSegments()) {
        currReceivingFlows[(int) linkSegment.getId()] = linkSegment.getCapacityOrDefaultPcuH();
      }
      for (var connectoidSegment : transportNetwork.getVirtualNetwork().getLayer().getConnectoidSegments()) {
        currReceivingFlows[(int) connectoidSegment.getId()] = connectoidSegment.getCapacityOrDefaultPcuH();
      }
      LinkSegmentData.copyTo(currReceivingFlows, nlReceivingFlowData.getNextReceivingFlows());

    } else {
      LOGGER.severe("sLTM with physical queues is not yet implemented, please disable storage constraints and " +
              "try again");
    }
  }

  /**
   * Activate all nodes that require tracking during loading. Conduct after initial unconstrained loading is conducted.
   * <p>
   * In case the solution scheme is set the POINTQ_QUEUE_BASIC: Only a subset of all nodes require tracking. This is
   * the least memory intensive approach where only nodes where for any outgoing link b it holds that s_b  greater than
   * c_b (sending flow greater than capacity) is potentially restrictive, i.e., reduces sending flow to meet capacity
   * requirements, needs to be tracked. In this case the splitting rates data is required only for its incoming links
   * during the loading.
   * </p><p>
   * In all other cases, the solution scheme adopts a locally iterative update of sending flows without any loading
   * in between, this means that for flows to reach downstream nodes the entire used network needs to be tracked,
   * otherwise flows cannot propagate. Therefore, in this case we initialise the tracked nodes by considering all
   * paths with non-zero flows and activate the entry links of nodes passed.
   * </p>
   *
   * @return created splittingRateData class
   */
  private NetworkLoadingSplittingRateData createSplittingRateData(
          NetworkLoadingSplittingRateData prevIterationSplittingRateData) {

    int numberOfVertices = getTransportNetwork().getNumberOfVerticesAllLayers();
    /* POINT QUEUE BASIC */
    if (!isTrackAllNodeTurnFlowsDuringLoading()) {
      nlSplittingRateData = new NetworkLoadingSplittingRateDataPartial(numberOfVertices);
    }
    /* OTHER, e.g. physical queues and advanced point queue model */
    else if (!this.solutionScheme.equals(StaticLtmLoadingScheme.NONE)) {
      nlSplittingRateData = new NetworkLoadingSplittingRateDataComplete(
              numberOfVertices, this.nlInFlowOutflowData.getInflows().length);
    }

    if(nlSplittingRateData == null){
      LOGGER.severe("Unable to create correct splitting rate tracking data class");
      return null;
    }

    /* make sure we correctly identify prev iteration potentially blocking nodes, so we know what costs to update when
     * a node switches from blocking to non-blocking (in those cases it is no longer blocked so information may not be
     *  tracked yet costs on its adjacent links will change, so we still need to compute costs for those links
     * TODO: move all potentially blocking information from splitting rate data to its own data class?
     */
    if(prevIterationSplittingRateData != null){
      nlSplittingRateData.initialisePrevIterationData(prevIterationSplittingRateData);
    }

    return nlSplittingRateData;
  }

  /**
   * Initialise the status of the nodes regarding (tracked and/or potentially blocking) which determines if network
   * splitting rates will be tracked during loading
   */
  private void initialiseNodeSplittingRateStatus() {
    // TODO -> the way this is configured depends on the assignment strategy and therefore
    // this functionality should probably not be here but in the assignment strategy...

    if (this.solutionScheme.equals(StaticLtmLoadingScheme.NONE)) {
      LOGGER.severe("Unable to initialise node splitting rate data");
      return;
    }

    /*
     * if we changed our approach during the last iteration -> we replaced the splitting rate data as well.
     * When we start the new iteration with another approach, we first recreate the appropriate splitting rate data
     * consistent with the current approach again and activate the correct tracked, potentially blocking nodes in the
     * process
     */
    boolean initialiseTrackedNodes = true;
    this.nlSplittingRateData = createSplittingRateData(nlSplittingRateData);
    if (initialiseTrackedNodes) {
      if (isTrackAllNodeTurnFlowsDuringLoading()) {
        /*
         * OTHER, e.g. physical queues and advanced point queue model all nodes are to be considered so we must
         * activate them all (track and mark potentially blocking)
         */
        activateAllUsedNodeSplittingRates(this.networkLayer, this.nlSendingFlowData.getCurrentSendingFlows());
      } else {
        /*
         * POINT QUEUE BASIC only track nodes that are needed. Eligibility depends on approach, so to be implemented
         * by derived implementation
         */
        activateEligibleSplittingRateTrackedNodes();
      }
    }

    /*
     * When not all nodes are already activated, i.e. marked tracked and potentially blocking, identify which ones
     * of the currently tracked nodes are also potentially blocking (not all tracked nodes need to be
     * potentially blocking)
     */
    if (!isTrackAllNodeTurnFlowsDuringLoading()) {
      updatePotentiallyBlockingNodes(this.networkLayer, this.nlSendingFlowData.getCurrentSendingFlows());
    }
  }

  /**
   * Update the splitting rates based on the provided accepted turn flows
   *
   * @param acceptedTurnFlows to use to determine splitting rates (movement id indexed array)
   * @param entrySegmentsToUpdate the segments to update (if null all are updated)
   */
  protected void updateNextSplittingRates(
      final TurnFlowAccessor acceptedTurnFlows, TreeSet<EdgeSegment> entrySegmentsToUpdate){
    var nodesToUpdate = nlSplittingRateData.getTrackedNodes();
    if(nodesToUpdate==null){
      return;
    }
    for (var node : nodesToUpdate) {
      for (var entrySegment : node.getEntryEdgeSegments()) {
        if(!CollectionUtils.nullOrEmpty(entrySegmentsToUpdate) && !entrySegmentsToUpdate.contains(entrySegment)){
          continue;
        }

        /* construct splitting rates by first imposing absolute turn flows */
        Array1D<Double> nextSplittingRates = nlSplittingRateData.getSplittingRates(entrySegment);
        nextSplittingRates.reset();
        int index = 0;
        for (var exitSegment : node.getExitEdgeSegments()) {
          /* assume no u-turn flow allowed */
          if (entrySegment.getParent().idEquals(exitSegment.getParent())) {
            index++;
            continue;
          }

          Double acceptedTurnFlow = acceptedTurnFlows.getTurnFlow(entrySegment, exitSegment);
          if (acceptedTurnFlow == null) {
            acceptedTurnFlow = 0.0;
          }
          nextSplittingRates.set(index++, acceptedTurnFlow);
        }

        /* sum all flows and then divide by this sum to obtain splitting rates */
        double totalEntryFlow = nextSplittingRates.aggregateAll(Aggregator.SUM);
        if (totalEntryFlow > 0) {
          nextSplittingRates.modifyAll(PrimitiveFunction.DIVIDE.by(totalEntryFlow));
        } else {
          nextSplittingRates.fillAll(1.0/nextSplittingRates.size());
        }
      }
    }

  }

  /**
   * Update the splitting rates based on the provided accepted turn flows
   * 
   * @param acceptedTurnFlows to use to determine splitting rates (movement id indexed array)
   */
  protected void updateNextSplittingRates(final TurnFlowAccessor acceptedTurnFlows) {
    updateNextSplittingRates(acceptedTurnFlows, null);
  }

  /**
   * For all potentially blocking nodes: perform a node model update based on: 1) sending flows, 2) receiving flows,
   * 3) splitting rates resulting in newly accepted local outflows and inflows.
   *
   * @param nodesToUpdate the nodes to update
   * @param consumer to apply to the result of each node model update of the considered nodes, may be null then ignored
   */
  private void performNodeModelUpdate(TreeSet<DirectedVertex> nodesToUpdate, final NodeModelRunResultConsumer consumer) {
    /* For each tracked node */
    for (var currNode : nodesToUpdate) {
      StaticLtmNetworkLoading.performNodeModelUpdate(currNode, consumer, this);
    }
  }

  /**
   * Update (next) storage capacity factors, Eq. (11) using the next sending flows (representing the current inflows)
   * and the current receiving flows.
   * <p>
   * We only update the factors for incoming links of potentially blocking nodes, because if the node is not
   * potentially blocking the storage capacity factor multiplied by the flow capacity factor results in inflow
   * divided by outflow which always equals to one, so no need to actively track it (do note that this requires to
   * also apply this to the updates of flow capacity and flow acceptance factors, otherwise the combined result is
   * inconsistent and can lead to serious issues in the outcomes)
   * </p>
   */
  private void updateNextStorageCapacityFactors() {
    this.networkLoadingFactorData.resetNextStorageCapacityFactors();
    double[] nextStorageCapacityFactor = this.networkLoadingFactorData.getNextStorageCapacityFactors();
    double[] inflows = this.nlInFlowOutflowData.getInflows();
    double[] receivingFlows = this.nlReceivingFlowData.getCurrentReceivingFlows();

    int currentLinkSegmentId = -1;
    for (DirectedVertex trackedNode : this.nlSplittingRateData.getTrackedNodes()) {
      if (!this.nlSplittingRateData.isPotentiallyBlocking(trackedNode)) {
        continue;
      }

      for (EdgeSegment entryEdgeSegment : trackedNode.getEntryEdgeSegments()) {
        currentLinkSegmentId = (int) entryEdgeSegment.getId();
        /* gamma_a = u_a/r_a */
        nextStorageCapacityFactor[currentLinkSegmentId] =
                inflows[currentLinkSegmentId] / receivingFlows[currentLinkSegmentId];
      }
    }
  }

  /**
   * Update (next) flow acceptance factors, Eq. (9) using the current storage capacity and current flow capacity
   * factors.
   * <p>
   * We only update the factors for incoming links of potentially blocking nodes, because if the node is not
   * potentially blocking the flow acceptance factor is known to be 1 and won't change throughout the loading
   * </p>
   */
  private void updateNextFlowAcceptanceFactors() {
    this.networkLoadingFactorData.resetNextFlowAcceptanceFactors();
    double[] inflows = this.nlInFlowOutflowData.getInflows();
    double[] nextFlowAcceptanceFactors = this.networkLoadingFactorData.getNextFlowAcceptanceFactors();
    double[] currentFlowCapacityFactors = this.networkLoadingFactorData.getCurrentFlowCapacityFactors();
    double[] currentStorageCapacityFactors = this.networkLoadingFactorData.getCurrentStorageCapacityFactors();

    int currentLinkSegmentId = -1;
    for (DirectedVertex trackedNode : this.nlSplittingRateData.getTrackedNodes()) {
      if (!this.nlSplittingRateData.isPotentiallyBlocking(trackedNode)) {
        continue;
      }

      for (EdgeSegment entryEdgeSegment : trackedNode.getEntryEdgeSegments()) {
        currentLinkSegmentId = (int) entryEdgeSegment.getId();
        /* alpha_a = beta_a^i-1 / gamma_a^i */
        if (inflows[currentLinkSegmentId] <= Precision.EPSILON_6) {
          /* special case: no inflow -> no restriction, set to 1 */
          nextFlowAcceptanceFactors[currentLinkSegmentId] = 1;
        } else {
          nextFlowAcceptanceFactors[currentLinkSegmentId] =
                  currentFlowCapacityFactors[currentLinkSegmentId] / currentStorageCapacityFactors[currentLinkSegmentId];
        }
      }
    }
  }

  /**
   * Update (next) flow capacity factors, Eq. (10) using the next receiving flows and the current accepted outflows.
   * <p>
   * We only update the factors for incoming links of potentially blocking nodes, because if the node is not
   * potentially blocking the storage capacity factor multiplied by the flow capacity factor results in inflow
   * divided by outflow which always equals to one, so no need to actively track it (do note that this requires to
   * also apply this to the updates of storage capacity and flow acceptance factors, otherwise the combined result
   * is inconsistent and can lead to serious issues in the outcomes)
   </p>
   *
   */
  private void updateNextFlowCapacityFactors() {
    this.networkLoadingFactorData.resetNextFlowCapacityFactors();
    double[] nextFlowCapacityFactors = this.networkLoadingFactorData.getNextFlowCapacityFactors();
    double[] outflows = this.nlInFlowOutflowData.getOutflows();
    double[] receivingFlows = this.nlReceivingFlowData.getCurrentReceivingFlows();

    int currentLinkSegmentId = -1;
    for (DirectedVertex trackedNode : this.nlSplittingRateData.getTrackedNodes()) {
      if (!this.nlSplittingRateData.isPotentiallyBlocking(trackedNode)) {
        continue;
      }

      for (EdgeSegment entryEdgeSegment : trackedNode.getEntryEdgeSegments()) {
        currentLinkSegmentId = (int) entryEdgeSegment.getId();
        /* beta_a = v_a/r_a */
        nextFlowCapacityFactors[currentLinkSegmentId] =
                Math.min(1, outflows[currentLinkSegmentId] / receivingFlows[currentLinkSegmentId]);
      }
    }
  }

  // SIMULATION DATA

  /** variables tracked for sending flow update step **/
  protected UnconstrainedFlowData unconstrainedFlowData;

  /** variables tracked for sending flow update step **/
  protected NetworkLoadingSendingFlowData nlSendingFlowData;

  /** variables tracked for receiving flow update step **/
  protected NetworkLoadingReceivingFlowData nlReceivingFlowData;

  /** variables tracked for splitting rate update step **/
  protected NetworkLoadingSplittingRateData nlSplittingRateData;

  /** tracks flow acceptance factors as well as its two related other factors, storage and capacity factors */
  protected NetworkLoadingFactorData networkLoadingFactorData;

  /** variables tracked for temporary inflow outflow tracking within sub algorithms **/
  protected InflowOutflowData nlInFlowOutflowData;

  /** the gap function to apply on global convergence update */
  protected NormBasedGapFunction flowAcceptanceGapFunction;

  /** the gap function to apply on sending flow update step */
  protected NormBasedGapFunction sendingFlowGapFunction;

  /** the gap function to apply on receiving flow update step */
  protected NormBasedGapFunction receivingFlowGapFunction;

  // SETTINGS //

  /** user settings used regarding how to run the loading */
  protected final StaticLtmSettings settings;

  /** analyser to track if loading is converging as expected based on its settings */
  protected final StaticLtmNetworkLoadingConvergenceAnalyser convergenceAnalyser;

  /** track the approach of how the solution scheme is applied based on this type */
  protected StaticLtmLoadingScheme solutionScheme;

  /** track the solution scheme applied before the current */
  protected StaticLtmLoadingScheme prevIterationFinalSolutionScheme;

  /**
   * Validate all constructor parameters
   *
   * @param modes to use
   * @return true when positively validated, false when failed
   */
  protected boolean validateInputs(Set<Mode> modes) {
    if (!getSettings().validate()) {
      LOGGER.severe(String.format("%sUnable to use sLTM settings, aborting initialisation of sLTM",
              LoggingUtils.runIdPrefix(runId)));
      return false;
    }

    if (modes == null || modes.isEmpty()) {
      LOGGER.severe("Modes for sLTM network loading are null or empty");
      return false;
    }

    if (modes.size()> 1) {
      LOGGER.severe("Only single mode for sLTM network loading supported, but found more than one");
      return false;
    }
    var theMode = modes.iterator().next();

    if (transportNetwork == null || transportNetwork.getInfrastructureNetwork() == null ||
        modes.stream().anyMatch( m -> transportNetwork.getInfrastructureNetwork().getLayerByMode(m) == null)) {
      LOGGER.severe("Network or network layer or mode of network layer not available for static " +
              "LTM network loading");
      return false;
    }

    if (!modes.stream().allMatch(
        m -> transportNetwork.getInfrastructureNetwork().getLayerByMode(m) instanceof MacroscopicNetworkLayer)) {
      LOGGER.severe("One or more network layers for mode not compatible, expected MacroscopicNetworkLayer");
      return false;
    }

    if (demands == null || demands.get(theMode, timePeriod) == null) {
      LOGGER.severe(String.format(
          "OdDemands for sLTM network loading (for given time period (%s) and mode (%s) ) are null",
              timePeriod.toString(), theMode.toString()));
      return false;
    }

    return true;
  }

  /**
   * Get the transport model network
   * 
   * @return transport model network
   */
  protected TransportModelNetwork<MacroscopicNetwork, VirtualNetwork> getTransportNetwork() {
    return transportNetwork;
  }

  /**
   * Verify if the sending flows are updated iteratively and locally in the Step 2 sending flow update. when not
   * updated iteratively, only a single update is performed before doing another loading consistent with
   * Bliemer et al. (2014). When updated iteratively, the solution scheme presented in Raadsen and Bliemer (2021)
   * is active.
   * 
   * @return true when not in POINT_QUEUE_BASIC scheme, false otherwise
   */
  protected boolean isIterativeSendingFlowUpdateActivated() {
    return !solutionScheme.equals(StaticLtmLoadingScheme.POINT_QUEUE_BASIC);
  }

  /**
   * For all nodes that have downstream link segments with sending flows that exceed their capacity,
   * ensure they are registered as potentially blocking (if not already)
   *
   * @param layer to update
   * @param sendingFlowsPcuH to use
   */
  protected void updatePotentiallyBlockingNodes(MacroscopicNetworkLayer layer, final double[] sendingFlowsPcuH) {
    NetworkLoadingSplittingRateDataPartial pointQueueBasicSplittingRates =
        (NetworkLoadingSplittingRateDataPartial) this.nlSplittingRateData;
    pointQueueBasicSplittingRates.resetPotentiallyBlockingNodes();

    for (MacroscopicLinkSegment linkSegment : layer.getLinkSegments()) {
      if (!pointQueueBasicSplittingRates.isPotentiallyBlocking(linkSegment.getUpstreamNode())) {
        double capacity = linkSegment.getCapacityOrDefaultPcuH();
        /* register if unconstrained flow exceeds capacity */
        if (Precision.greater(sendingFlowsPcuH[(int) linkSegment.getId()], capacity)) {
          pointQueueBasicSplittingRates.registerPotentiallyBlockingNode(linkSegment.getUpstreamNode());
        }
      }
    }
  }

  /**
   * For all nodes that have downstream link segments with positive sending flows, ensure they are activated for
   * splitting rates (activation implies tracking and potentially blocking) if not already. To be used when we
   * consider spill back or when we perform iterative local sending flow updates to propagate flows locally.
   *
   * @param layer to use
   * @param sendingFlowsPcuH to use
   */
  protected void activateAllUsedNodeSplittingRates(MacroscopicNetworkLayer layer, double[] sendingFlowsPcuH) {
    for (MacroscopicLinkSegment linkSegment : layer.getLinkSegments()) {
      if (Precision.positive(sendingFlowsPcuH[(int) linkSegment.getId()])) {
        this.nlSplittingRateData.registerTrackedNode(linkSegment.getUpstreamNode());
      }
    }
    /* also add nodes of eligible connectoid segments (when they are not centroids) */
    for (ConnectoidSegment connectoidSegment :
            getTransportNetwork().getZoning().getVirtualNetwork().getLayer().getConnectoidSegments()) {
      if (Precision.positive(sendingFlowsPcuH[(int) connectoidSegment.getId()])) {
        /* activate both nodes, succeeding segments might not be available */
        this.nlSplittingRateData.registerTrackedNode(connectoidSegment.getUpstreamVertex());
        this.nlSplittingRateData.registerTrackedNode(connectoidSegment.getDownstreamVertex());
      }
    }
  }

  //@formatter:off
  /**
   * Conduct a network loading to compute updated turn inflow rates u_ab: Eq. (3)-(4) in paper. We only consider
   * turns on nodes that are tracked or activated to reduce computational overhead.
   *
   * @param mode                    to use
   * @return acceptedTurnFlows (on potentially blocking nodes) where movement id is index and value is the
   * accepted turn flow v_ab in the form of an accessor interface implementation
   */
  protected abstract TurnFlowAccessor networkLoadingTurnFlowUpdate(Mode mode);

  /**
   * Conduct a network loading to compute updated current sending flow rates
   * (without tracking turn flows): Eq. (3)-(4) in paper. Optionally also update unconstrained flows as well
   *
   * @param mode to use
   * @param updateUnconstrainedFlows flag
   */
  protected abstract void networkLoadingLinkSegmentSendingFlowUpdate(Mode mode, boolean updateUnconstrainedFlows);

  /**
   * After convergence of loading, we want all flows to be in sync for the current alphas. We update
   * inflows, sending flows, and outflows all based on current alphas WITHOUT recomputing any node models nor splitting
   * rates.
   *
   * @param mode to use
   */
  protected abstract void networkLoadingSyncFlowsUpdate(Mode mode);

  /**
   * Let derived loading implementation initialise which nodes are to be tracked for network splitting rates, e.g.
   * a bush-based implementation requires all nodes along PASs to be tracked (Regardless if they are
   * potentially blocking), whereas a path based implementation only requires potentially blocking node to be tracked.
   */
  protected abstract void activateEligibleSplittingRateTrackedNodes();

  /**
   * Constructor
   *
   * @param idToken                 for id generation of internal entities
   * @param runId                   run id the loading is applied for
   * @param settings                to use
   */

  protected StaticLtmNetworkLoading(
          final IdGroupingToken idToken,
          long runId,
          StaticLtmSettings settings) {
    this.runId = runId;
    this.idToken = idToken;
    this.settings = settings;
    
    /* state trackers */    
    this.convergenceAnalyser = new StaticLtmNetworkLoadingConvergenceAnalyser();
    this.solutionScheme = StaticLtmLoadingScheme.NONE;    
    this.prevIterationFinalSolutionScheme = solutionScheme;
  }

  /**
   * conduct a node model update sLTM style with
   *
   * @param node                    to compute
   * @param consumer                to apply to the result of each node model update of the considered nodes, may be
   *                                null then ignored
   * @param staticLtmNetworkLoading sLTMloading containing the data to populate node with (using current splitting rates)
   * @param nodeModelSendingFlows   node model sending flows to use (can be inflows, sending flows, or other flows)
   */
  public static void performNodeModelUpdate(
      DirectedVertex node,
      NodeModelRunResultConsumer consumer,
      StaticLtmNetworkLoading staticLtmNetworkLoading,
      double[] nodeModelSendingFlows) {

    var splittingRateData = staticLtmNetworkLoading.getSplittingRateData();

    /* tracked but non-blocking or centroidVertex is notified as non-blocking */
    if (!splittingRateData.isPotentiallyBlocking(node) || node instanceof CentroidVertex) {
      consumer.acceptNonBlockingLinkBasedResult(node, nodeModelSendingFlows);
      return;
    }

    /* For each potentially blocking node */
    // TODO: not computationally efficient, capacities are recomputed every time and construction of
    // TODO: turn sending flows is not ideal it requires a lot of copying of data that potentially could be optimised

    /* C_a : in Array1D form, capped to maximum physical capacity in case we are dealing with connectoid with
     * infinite capacity */
    var inCapacities = TampereNodeModelUtils.createIncomingCapacities(node);
    /* s_ab : turn sending flows in per entrylinksegmentindex: Array1D (turn to outsegment flows) form */
    Array2D<Double> turnSendingFlows = TampereNodeModelUtils.createTurnSendingFlowsUsingSplittingRates(
        node, nodeModelSendingFlows, es -> splittingRateData.getSplittingRates(es).copy());
    /* r_a : in Array1D form */
    var outReceivingFlows = TampereNodeModelUtils.createOutgoingReceivingFlows(node);

    /* Kappa(s,r,phi) : node model update */
    try {
      var nodeModel = TampereNodeModel.of(inCapacities, outReceivingFlows, turnSendingFlows);
      Array1D<Double> localFlowAcceptanceFactors = nodeModel.run();

      /* delegate to consumer */
      consumer.acceptTurnBasedResult(node, localFlowAcceptanceFactors, nodeModel);

    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      LOGGER.severe(String.format("Unable to run Tampere node model on tracked node %s", node.getXmlId()));
    }
  }

  /**
   * conduct a node model update sLTM style but now invoke as runTurnBased() to obtain turn based alphas allowing for
   * identification of disconinuities used in route choice/cost calculation.
   * todo: 99.9% identical to method above, only due to signatures not the same. Refactor to consolidate in general approach
   *  to reduce duplicate code.
   *
   * @param node                    to compute
   * @param consumer                to apply to the result of each node model update of the considered nodes, may be
   *                                null then ignored
   * @param staticLtmNetworkLoading sLTMloading containing the data to populate node with (using current splitting rates)
   * @param nodeModelSendingFlows   node model sending flows to use (can be inflows, sending flows, or other flows)
   */
  public static void performNodeModelTurnBasedUpdate(
      DirectedVertex node,
      NodeModelRunTurnBasedResultConsumer consumer,
      StaticLtmNetworkLoading staticLtmNetworkLoading,
      double[] nodeModelSendingFlows) {

    var splittingRateData = staticLtmNetworkLoading.getSplittingRateData();

    /* tracked but non-blocking or centroidVertex is notified as non-blocking */
    if (!splittingRateData.isPotentiallyBlocking(node) || node instanceof CentroidVertex) {
      consumer.acceptNonBlockingLinkBasedResult(node, nodeModelSendingFlows);
      return;
    }

    /* For each potentially blocking node */
    int numEntrySegments = node.getNumberOfEntryEdgeSegments();
    int numExitSegments = node.getNumberOfExitEdgeSegments();

    // TODO: not computationally efficient, capacities are recomputed every time and construction of
    // TODO: turn sending flows is not ideal it requires a lot of copying of data that potentially could be optimised

    /* C_a : in Array1D form, capped to maximum physical capacity in case we are dealing with connectoid with
     * infinite capacity */
    var inCapacities = TampereNodeModelUtils.createIncomingCapacities(node);
    /* s_ab : turn sending flows in per entrylinksegmentindex: Array1D (turn to outsegment flows) form */
    Array2D<Double> turnSendingFlows = TampereNodeModelUtils.createTurnSendingFlowsUsingSplittingRates(
        node, nodeModelSendingFlows, es -> splittingRateData.getSplittingRates(es).copy());
    /* r_a : in Array1D form */
    var outReceivingFlows = TampereNodeModelUtils.createOutgoingReceivingFlows(node);

    /* Kappa(s,r,phi) : node model update */
    try {
      var nodeModel = TampereNodeModel.of(inCapacities, outReceivingFlows, turnSendingFlows);
      var turnBasedFlowAcceptanceFactors = nodeModel.runTurnBased();
      /* delegate to turn based consumer */
      consumer.acceptTurnBasedResult(node, turnBasedFlowAcceptanceFactors, nodeModel);

    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      LOGGER.severe(String.format("Unable to run Tampere node model on tracked node %s", node.getXmlId()));
    }
  }

  /**
   * conduct a node model update sLTM style with
   * 
   * @param node                    to compute
   * @param consumer                to apply to the result of each node model update of the considered nodes, may be
   *                                null then ignored
   * @param staticLtmNetworkLoading sLTMloading containing the data to populate node with
   *                                (using current sending flows and splitting rates)
   */
  public static void performNodeModelUpdate(
          DirectedVertex node, NodeModelRunResultConsumer consumer, StaticLtmNetworkLoading staticLtmNetworkLoading) {
    performNodeModelUpdate(
        node, consumer, staticLtmNetworkLoading, staticLtmNetworkLoading.nlSendingFlowData.getCurrentSendingFlows());
  }

  /** Initialise the loading with the given inputs
   *
   * @param timePeriod to use
   * @param modes to use
   * @param demands to use
   * @param network to use
   */
  public void initialiseInputs(
      final TimePeriod timePeriod,
      Set<Mode> modes,
      final Demands demands,
      final TransportModelNetwork<MacroscopicNetwork,VirtualNetwork> network) {

    this.timePeriod = timePeriod;
    this.demands = demands;
    this.transportNetwork = network;

    validateInputs(modes);
    
    /* sLTM only uses a single layer and single mode for now */
    this.networkLayer = this.transportNetwork.getInfrastructureNetwork().getLayerByMode(modes.iterator().next());
    double[] referenceEmptyArray = new double[network.getNumberOfEdgeSegmentsAllLayers()];
    
    /* flow data defaults to zero unless explicitly set */
    this.nlSendingFlowData = new NetworkLoadingSendingFlowData(referenceEmptyArray);
    this.unconstrainedFlowData = new UnconstrainedFlowData(referenceEmptyArray);
    this.nlReceivingFlowData = new NetworkLoadingReceivingFlowData(referenceEmptyArray);
    this.nlInFlowOutflowData = new InflowOutflowData(referenceEmptyArray);
    
    this.networkLoadingFactorData = new NetworkLoadingFactorData(network.getNumberOfEdgeSegmentsAllLayers());

    /* gap functions used */
    this.flowAcceptanceGapFunction = new NormBasedGapFunction(idToken, new StopCriterion());
    flowAcceptanceGapFunction.getStopCriterion().setEpsilon(getSettings().getNetworkLoadingFlowAcceptanceGapEpsilon());
    this.sendingFlowGapFunction = new NormBasedGapFunction(idToken, new StopCriterion());
    sendingFlowGapFunction.getStopCriterion().setEpsilon(getSettings().getNetworkLoadingSendingFlowGapEpsilon());
    this.receivingFlowGapFunction = new NormBasedGapFunction(idToken, new StopCriterion());
    receivingFlowGapFunction.getStopCriterion().setEpsilon(getSettings().getNetworkLoadingReceivingFlowGapEpsilon());
  }
  
  //@formatter:off
  /**
   * Perform initialisation of the network loading. This method can only be called once.
   * <p>
   * 1. Initial acceptance flow, capacity, and storage factors, all set to one  
   * 2. Initial in/outflows via network loading Eq. (3)-(4) in paper: unconstrained network loading
   * 3. Initial sending and receiving flows: s_a=u_a, r_a=capacity_a for all link segments a
   * 4. Set iteration number to one (to be done exogenously)
   * (Extension A):  
   * 5. Restrict receiving flows to storage capacity Eq. (8) - only relevant when storage capacity is activated
   *</p>
   * @param mode to use
   * @param logSolutionScheme flag indicating whether to log the solution scheme used
   * @return true when successful, false otherwise 
   */
  public boolean stepZeroIterationInitialisation(Mode mode, boolean logSolutionScheme) {
        
    /* activate the correct configuration of the initial solution scheme */
    initialiseStaticLtmSolutionSchemeApproach(logSolutionScheme); 
                
    /* 2. Initial sending flows via network loading Eq. (3)-(4) in paper: unconstrained network loading */
    initialiseSendingFlows(mode, true);
    
    /* Depending on the solution scheme we either track all used nodes in the network, or a subset. Either way these
    need to be activated/initialized before commencing the loading. This is done here. */
    initialiseNodeSplittingRateStatus();
    
    /* 3. limit flows to capacity s_a=r_a=min(u_a,cap_a) */
    /* reduce sending flows to capacity */
    this.nlSendingFlowData.limitCurrentSendingFlowsToCapacity(networkLayer.getLinkSegments());
    
    /* initialize receiving flows */
    initialiseReceivingFlows();
        
    return true;    
  }
  
  //@formatter:off
  /**
   * Perform splitting rate update (before sending flow update) of the network loading:
   * <p>
   * 1. Update inflows via network loading Eq. (3)
   * 2. Update splitting rates Eq. (6),(4)
   * (Extension B)
   * 3. If not first iteration then update splitting rates, Eq. (13)
   * </p>
   *
   * @param mode                    to use
   */
  public void stepOneSplittingRatesUpdate(Mode mode) {
    if(this.solutionScheme.isPhysicalQueue()) {
      LOGGER.severe(String.format("%ssLTM with physical queues is not yet implemented, please disable storage " +
              "constraints and try again",LoggingUtils.runIdPrefix(runId)));
    }

    /* 1. Update turn inflows via network loading (movement index array provided) Eq. (3) */
    var acceptedTurnFlows = networkLoadingTurnFlowUpdate(mode);
    
    /* update splitting rates Eq. (6),(4) */
    updateNextSplittingRates(acceptedTurnFlows);
    
    /* TODO:
     * in case we do smoothing, it can be applied directly to the splitting rates per node such that
     * there is no need for a full copy of the entire splitting rate data (create per node/entry link local copy
     * of existing splitting rates, then compute new ones, and apply smoothing on the two, before moving to the next
     * entry link -> SEE NOTE IN PAPER ON TRACKING PREVIOUS SPLITTING RATES PER TYPE OF UPDATE*/    
  }

  public void stepTwoInflowSendingFlowUpdate(Mode mode) {
    stepTwoInflowSendingFlowUpdate(mode, getSplittingRateData().getTrackedNodes());
  }
  
  //@formatter:off
  /**
   * Perform inflow and sending flow update:
   * <p>
   * 1. Update node model to compute new inflows, Eq. (5)
   * 2. Update next sending flows via inflows, Eq. (7) (this is local propagation compared to network full
   * loading of previous step)
   * 3. Compute gap,  then update sending flows to next sending flows
   * 4. If converged continue, otherwise continue go back to Step 2-(1).
   * 5. Update storage capacity factors, Eq. (11)
   * (Extension B)
   * 6. Update smoothed storage capacity factors, Eq. (14)
   *
   * @param mode         to use
   * @param nodesToUpdate to update inflows/sending flows for
   */
  public void stepTwoInflowSendingFlowUpdate(Mode mode, TreeSet<DirectedVertex> nodesToUpdate) {
    if(this.solutionScheme.isPhysicalQueue()) {
      LOGGER.severe(String.format("%ssLTM with physical queues is not yet implemented, please disable storage " +
          "constraints and try again",LoggingUtils.runIdPrefix(runId)));
      return;
    }
    
    int sendingFlowIterationIndex = 0;
    double sendingFlowGap;
    
    do {      
      /* 1. Update node model to compute new inflows, Eq. (5)
       * 2. Update next sending flows via inflows, Eq. (7) */
      LinkSegmentData.copyTo(this.nlSendingFlowData.getCurrentSendingFlows(), this.nlInFlowOutflowData.getInflows());
      performNodeModelUpdate(nodesToUpdate, new NMRUpdateExitLinkInflowsConsumer(this.nlInFlowOutflowData.getInflows()));
      /* s_a^tilde = u_a */
      LinkSegmentData.copyTo(this.nlInFlowOutflowData.getInflows(), this.nlSendingFlowData.getNextSendingFlows());
            
      /*3. Compute gap between current and next sending flows, then update sending flows to next sending flows */
      this.sendingFlowGapFunction.reset();
      this.sendingFlowGapFunction.increaseMeasuredValue(
              this.nlSendingFlowData.getNextSendingFlows(), this.nlSendingFlowData.getCurrentSendingFlows());
      sendingFlowGap = this.sendingFlowGapFunction.computeGap();
      
      /* 4a, update current sending flows s_a = s_a^tilde to next sending flows */
      this.nlSendingFlowData.swapCurrentAndNextSendingFlows();
      
      /* Only run as iterative procedure with physical queues or when using advanced point queue */
      if(!isIterativeSendingFlowUpdateActivated()) {
        break;
      }      
      
      /* 4 If converged continue, otherwise go back to Step 2-(1). */
    }while(!this.sendingFlowGapFunction.getStopCriterion().hasConverged(
            sendingFlowGap, sendingFlowIterationIndex++));
    this.sendingFlowGapFunction.reset();
    
    /* Update storage capacity factors, Eq. (11) */
    updateNextStorageCapacityFactors();
    
    /* TODO:
     * in case we do smoothing, it can be applied directly to the capacity factor per node such that
     * there is no need for a full copy of the entire factor data (create per node/entry link local copy)*/
    this.networkLoadingFactorData.swapCurrentAndNextStorageCapacityFactors();    
  }   
  
  //@formatter:off
  /**
   * Perform splitting rate update (before receiving flow update) of the network loading:
   * 
   * 1. Update intermediate flow acceptance factors, Eq. (9)
   * 2. Update inflows via network loading, Eq. (3)
   * 3. Update splitting rates, Eq. (6)
   * (Extension B)
   * 4. If not first iteration then update splitting rates, Eq. (13)
   * (Extension C)
   * 5. Estimate new multiplication factor used in Step 4, Eq. (16),(17)
   *
   * @param mode the mode to use
   */
  public void stepThreeSplittingRateUpdate(Mode mode) {
    if(!this.solutionScheme.isPhysicalQueue()) {
      /* ignored when not considering physical queues */
      return;
    }    
    
    if(this.solutionScheme.isPhysicalQueue()) {
      LOGGER.severe(String.format("%ssLTM with physical queues is not yet implemented, please disable storage " +
              "constraints and try again",LoggingUtils.runIdPrefix(runId)));
      return;
    }    
    
    /* 1. Update intermediate flow acceptance factors, Eq. (9) */
    updateNextFlowAcceptanceFactors();
    
    /* 2. Update inflows via network loading, Eq. (3) */
    var acceptedTurnFlows = networkLoadingTurnFlowUpdate(mode);
    
    /* 3. update splitting rates Eq. (6),(4) */
    updateNextSplittingRates(acceptedTurnFlows);    
    
    /* TODO:
     * in case we do smoothing, it can be applied directly to the splitting rates per node such that
     * there is no need for a full copy of the entire splitting rate data (create per node/entry link local copy
     * of existing splitting rates, then compute new ones, and apply smoothing on the two, before moving to the next
     * entry link  -> SEE NOTE IN PAPER ON TRACKING PREVIOUS SPLITTING RATES PER TYPE OF UPDATE*/    
  }

  public void stepFourOutflowAndReceivingFlowUpdate(Mode mode) {
    stepFourOutflowAndReceivingFlowUpdate(mode, getSplittingRateData().getTrackedNodes());
  }
  
  /**
   * 1. Update node model, to compute outflows Eq. (5)
   * 2. Update receiving flows based on outflows, Eq. (8)
   * (Extension B)
   * 3. Transform to nudged receiving flows using multiplication factor, Eq. (18)
   * (end Extension B)
   * 4. Compute gap then set next receiving flows to current receiving flows   
   * 5. If converged continue, else go back to Step 4-(1).
   * 6. Update flow capacity factors, Eq. (10)
   * (Extension C)
   * 7. Update smoothed flow capacity factors, Eq. (14)
   *
   * @param mode to use
   * @param trackedNodes to use
   */
  public void stepFourOutflowAndReceivingFlowUpdate(Mode mode, TreeSet<DirectedVertex> trackedNodes) {
    /* update the outflows and receiving flows */      

    /* for now */
    if(this.solutionScheme.isPhysicalQueue()) {
      LOGGER.severe(String.format("%ssLTM with physical queues is not yet implemented, please disable storage " +
              "constraints and try again",LoggingUtils.runIdPrefix(runId)));
      return;
    }
     
    int receivingFlowIterationIndex = 0;
    double receivingFlowGap;
    do {
      
      /* 1. Update node model to compute new outflows, Eq. (5) */
      performNodeModelUpdate(
          trackedNodes, new NMRUpdateEntryLinksOutflowConsumer(this.nlInFlowOutflowData.getOutflows()));
      
      /* POINT QUEUE -> only run as iterative procedure with physical queues are present and r can vary, now
       * we only require an update of outflows v to use for updating flow capacity factors */  
      if(this.solutionScheme.isPointQueue()) {
        break;
      }        
      
      /* 2. Update next receiving flows via inflows, Eq. (7)*/
      double[] outflows = this.nlInFlowOutflowData.getOutflows();
      double[] nextReceivingFlows = this.nlReceivingFlowData.getNextReceivingFlows();
      for(DirectedVertex node : this.nlSplittingRateData.getTrackedNodes()) {
        for(EdgeSegment entryEdgeSegment: node.getEntryEdgeSegments()){
          int index = (int)entryEdgeSegment.getId();
        
          /* storage_capacity_a = (L*FD^-1(v_a))/T) */
          double storageCapacity = Double.POSITIVE_INFINITY; // TODO: entryLinkSegment.getParent().getLengthKm() * etc.;
          /* r_a = min(C_a, v_a + storage_Capacity_a) */
          double receivingFlow =
                  Math.min(((PcuCapacitated)entryEdgeSegment).getCapacityOrDefaultPcuH(),
                          outflows[index] + storageCapacity);
          nextReceivingFlows[index] = receivingFlow;
        }
      }
      
      /*3. Compute gap between current and next sending receiving flows, then update receiving flows to next
       * receiving flows */
      this.receivingFlowGapFunction.reset();
      this.receivingFlowGapFunction.increaseMeasuredValue(
              this.nlReceivingFlowData.getNextReceivingFlows(), this.nlReceivingFlowData.getCurrentReceivingFlows());
      receivingFlowGap = this.receivingFlowGapFunction.computeGap();
      
      /* 4a update r^i-1 = r^i */
      this.nlReceivingFlowData.swapCurrentAndNextReceivingFlows();
      
      /* 4b If converged continue, otherwise continue go back to Step 4-(1). */
    }while(!this.receivingFlowGapFunction.getStopCriterion().hasConverged(
            receivingFlowGap, receivingFlowIterationIndex++));
    this.receivingFlowGapFunction.reset();
    
    /* 6. Update flow capacity factors, beta_a = v_a/r_a as per Eq. (10) */
    updateNextFlowCapacityFactors();
    
    /* TODO:
     * in case we do smoothing, it can be applied directly to the capacity factor per node such that
     * there is no need for a full copy of the entire factor data (create per node/entry link local copy)*/
    this.networkLoadingFactorData.swapCurrentAndNextFlowCapacityFactors();  
  }

  /**
   * 1. Update flow acceptance factors, Eq. (9)
   * 2. Compute gap using flow acceptance factors,  
   * 3. Increment iteration index,  (to be done by caller)
   * 4. If converged done, else go back to Step 1. (to be done by caller)
   * 
   * @param networkLoadingIteration at hand
   * @return true when converged, false otherwise
   */
  public boolean stepFiveCheckNetworkLoadingConvergence(int networkLoadingIteration) {
    if(this.solutionScheme.isPhysicalQueue()) {
      LOGGER.severe(String.format("%ssLTM with physical queues is not yet implemented, please disable storage " +
              "constraints and try again",LoggingUtils.runIdPrefix(runId)));
      return true;
    }
    
    /* 1. Update flow acceptance factors, Eq. (9) */
    updateNextFlowAcceptanceFactors();
    
    /*3. Compute gap between current and next flow acceptance factors*/
    this.flowAcceptanceGapFunction.reset();
    this.flowAcceptanceGapFunction.increaseMeasuredValue(
            this.networkLoadingFactorData.getNextFlowAcceptanceFactors(),
            this.networkLoadingFactorData.getCurrentFlowAcceptanceFactors());
    double globalGap = this.flowAcceptanceGapFunction.computeGap();
    this.convergenceAnalyser.registerIterationGap(globalGap);
    
    if(getSettings().isDetailedLogging()) {
      LOGGER.info(String.format("%sNetwork loading gap (i=%d): %.10f",
              LoggingUtils.runIdPrefix(runId), networkLoadingIteration, globalGap));
    }
    
    /* set next to current */
    this.networkLoadingFactorData.swapCurrentAndNextFlowAcceptanceFactors();
    
    boolean converged = this.flowAcceptanceGapFunction.getStopCriterion().hasConverged(
            globalGap, networkLoadingIteration);
    if(converged && networkLoadingIteration < getSettings().getNetworkLoadingMinIterations()){
      converged = false;
    }
    if(converged && getSettings().isDetailedLogging()) {
      LOGGER.info(String.format("%ssLTM network loading converged in %d iterations (remaining gap: %.10f)",
              LoggingUtils.runIdPrefix(runId), networkLoadingIteration, globalGap));
    }
    return converged;
  }
  
  /**
   * When loading has converged, outputs might be persisted or used for cost calculations, i.e., analysis. Since the
   * loading does not always track the entire network for performance reasons, this method can be invoked before
   * this analysis/data is required to fill the gaps (if any) regarding for example link in and outflows that might
   * otherwise not be available, e.g. in the POINT_QUEUE_BASIC loading scheme under a triangular FD as then only
   * potentially blocking nodes and their immediate adjacent links and their data are tracked on the network level.
   * Whereas if we want to see the results of this iteration, we would want the full inflows/outflows on all links
   * in the network.
   * <p>
   * This is somewhat costly since one additional pass over the network loading may be triggered.
   * </p>
   *
   * @param mode to use
   */
  public void stepSixFinaliseForAnalysis(Mode mode) {
    
    /*
     * Persistence requires all network data available, when not tracking entire network during loading
     * we must now switch to full network tracking for persistence purpose. 
     */
    int originalMaxIterations = this.sendingFlowGapFunction.getStopCriterion().getMaxIterations();
    /* prep: force single iteration on sending flow/inflow update */
    this.sendingFlowGapFunction.getStopCriterion().setMaxIterations(1);

    if (!isTrackAllNodeTurnFlowsDuringLoading()) {
      
      /* triggers tracking all node turn flows for loading steps */
      this.solutionScheme = StaticLtmLoadingScheme.POINT_QUEUE_ADVANCED;

      /* update sending flows, inflows, outflows on all links with minimal overhead */
      initialiseTrackAllNodeTurnFlows(mode);
    }

    /* we should do one full loading that DOES NOT update nodes and alphas, but does update
     * ALL flow information. We cannot use normal step approach because that will implicitly use
     * updated node results (alphas), causing inconsistency between inflows and outflows. We prefer
     * to instead have slightly incorrect alphas as long as inflow * alpha = outflow is consistent
     */
    networkLoadingSyncFlowsUpdate(mode);

    /* limit to capacities in case loading + alphas caused slight discrepancies due to local convergence not being
     * exactly 0 */
    // todo: this last bit should probably only be invoked for persisting or final iteration AFTER rotue choice
    //  because it may lead to discrepancies between bush and network flows
    {
      //networkLoadingSendingFlowOutflowUpdate(mode);   // reworked above to trigger full update so not needed anymore
      nlSendingFlowData.limitCurrentSendingFlowsToCapacity(networkLayer.getLinkSegments());
      LinkSegmentData.copyTo(nlSendingFlowData.getCurrentSendingFlows(), nlInFlowOutflowData.getInflows());
      nlInFlowOutflowData.limitOutflowsToCapacity(networkLayer.getLinkSegments());
    }

    /* post */
    this.sendingFlowGapFunction.getStopCriterion().setMaxIterations(originalMaxIterations);
  }

  /**
   * Verify if all turn flows are to be/being tracked during loading.
   *
   * @return false when POINT_QUEUE_BASIC solution scheme is active, true otherwise
   */
  public boolean isTrackAllNodeTurnFlowsDuringLoading() {
    return !solutionScheme.equals(StaticLtmLoadingScheme.POINT_QUEUE_BASIC);
  }

  /** Verify if we are still converging
   * 
   * @return true when potentially still converging, false otherwise
   */
  public boolean isConverging() {
    return convergenceAnalyser.isImproving();
  }

  /**
   * Given the current extension status and type of sLTM that we are conducting, activate the next extension in
   * loading to improve the likelihood of network loading convergence. Each additional extension that is activated
   * will slow down convergence, so only do this when it is clear the current scheme does not suffice.
   *
   * @param mode this applies for
   * @param logRecentGaps when true log all gaps in the period the most recent solution scheme method was active,
   *                      when false do not
   * @return true when scheme changed, false if no longer possible to change any further  
   */
  public boolean activateNextExtension(Mode mode, boolean logRecentGaps) {
    if(logRecentGaps) {
      convergenceAnalyser.logGapsSince(runId, convergenceAnalyser.getIterationOffset());
    }
    convergenceAnalyser.setIterationOffset(convergenceAnalyser.getRegisteredIterations());
    boolean solutionSchemeChanged = true;

    /* POINT - QUEUE */
    if(this.solutionScheme.isPointQueue()) {
      if(this.solutionScheme.equals(StaticLtmLoadingScheme.POINT_QUEUE_BASIC)) {
        /* BASIC -> ADVANCED, e.g., activate local iterative updates of sending flows by tracking entire network */
        this.solutionScheme = StaticLtmLoadingScheme.POINT_QUEUE_ADVANCED;
        
        /* initialise to allow loading to work with all nodes active based on new solution scheme */
        initialiseTrackAllNodeTurnFlows(mode);

      }else {
        /* no other extensions available, so deactivate any further extensions by maximizing the offset */
        convergenceAnalyser.setMinIterationThreshold(Integer.MAX_VALUE);
        solutionSchemeChanged = false;
      }
      
    }
    /* PHYSICAL - QUEUE */
    else {
      LOGGER.warning(String.format("%sNo extensions have yet been implemented for sLTM with physical queues",
              LoggingUtils.runIdPrefix(runId)));
      solutionSchemeChanged = false;
    }    
    
    if(solutionSchemeChanged) {
      LOGGER.info(String.format("%sSwitching network loading scheme to %s",
              LoggingUtils.runIdPrefix(runId), solutionScheme.getValue()));
    }
    
    return solutionSchemeChanged;
  }

  /** Collect the settings. Only make changes before running any of the loading steps, otherwise risk undefined 
   * behaviour by the loading.
   * 
   * @return settings of this loading
   */
  public StaticLtmSettings getSettings() {
    return settings;
  }

  /**
   * Collect the most recently calculated total inflows by the loading
   * 
   * @return inflows in Pcu per hour
   */
  public double[] getCurrentInflowsPcuH() {
    return this.nlInFlowOutflowData.getInflows();
  }

  /**
   * Collect the most recently calculated total sending flows by the loading
   *
   * @return sendingflows in Pcu per hour
   */
  public double[] getCurrentSendingFlowsPcuH() {
    return this.nlSendingFlowData.getCurrentSendingFlows();
  }
  
  /**
   * Collect the most recently calculate total outflows by the loading
   * 
   * @return outflows in Pcu per hour
   */
  public double[] getCurrentOutflowsPcuH() {
    return this.nlInFlowOutflowData.getOutflows();
  }

  /**
   * Access to current od demands for a given mode for the time period registered
   *
   * @param mode mode to use
   * @return od demands registered
   */
  public OdDemands getOdDemands(Mode mode){
    return demands.get(mode, timePeriod);
  }

  /**
   * Reset the network loading
   */
  public void reset() {
    resetIteration();
    this.nlSplittingRateData.reset();
    this.prevIterationFinalSolutionScheme = solutionScheme;
  }
  
  /**
   * Reset the network loading for the next iteration
   */
  public void resetIteration() {
    /* flow data defaults to zero unless explicitly set */
    this.nlSendingFlowData.reset();
    this.unconstrainedFlowData.reset();
    this.nlReceivingFlowData.reset();
    this.nlInFlowOutflowData.reset();
    this.networkLoadingFactorData.reset();

    /* gap functions used */
    this.flowAcceptanceGapFunction.reset();
    this.sendingFlowGapFunction.reset();
    this.receivingFlowGapFunction.reset();
    
    this.convergenceAnalyser.reset();
    
    /* reset of solution scheme means updating the prevIteration solution scheme to the most recent solution scheme */
    this.prevIterationFinalSolutionScheme = this.solutionScheme;
    this.solutionScheme = StaticLtmLoadingScheme.NONE;    
  }  
  
  
  /** Access to most recent flow acceptance factors (alphas)
   * 
   * @return flow acceptance factors indexed by link segment id
   */
  public final double[] getCurrentFlowAcceptanceFactors(){
    return this.networkLoadingFactorData.getCurrentFlowAcceptanceFactors();
  }

  /** Access to most recent unconstrained flows
   *
   * @return unconstrained flows indexed by link segment id
   */
  public double[] getUnconstrainedFlowsPcuHour() {
    return this.unconstrainedFlowData.getUnconstrainedFlows();
  }
  
  /** Collect the network's current splitting rate data
   * 
   * @return splitting rate data
   */
  public final NetworkLoadingSplittingRateData getSplittingRateData(){
    return this.nlSplittingRateData;
  }
  
  /** Currently active sLTM solution scheme
   * 
   * @return active solution scheme
   */
  public StaticLtmLoadingScheme getActivatedSolutionScheme() {
    return this.solutionScheme;
  }  

}

package org.planit.assignment.ltm.sltm;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.ojalgo.array.Array1D;
import org.ojalgo.array.Array2D;
import org.ojalgo.function.PrimitiveFunction;
import org.ojalgo.function.aggregator.Aggregator;
import org.ojalgo.structure.Access1D;
import org.planit.algorithms.nodemodel.TampereNodeModel;
import org.planit.algorithms.nodemodel.TampereNodeModelFixedInput;
import org.planit.algorithms.nodemodel.TampereNodeModelInput;
import org.planit.assignment.ltm.sltm.consumer.ActivateSplittingRatesUsedNodesConsumer;
import org.planit.assignment.ltm.sltm.consumer.PathLinkInflowUpdateConsumer;
import org.planit.assignment.ltm.sltm.consumer.PathTurnFlowUpdateConsumer;
import org.planit.assignment.ltm.sltm.consumer.UpdateEntryLinksOutflowConsumer;
import org.planit.assignment.ltm.sltm.consumer.UpdateExitLinkInflowsConsumer;
import org.planit.gap.NormBasedGapFunction;
import org.planit.gap.StopCriterion;
import org.planit.network.transport.TransportModelNetwork;
import org.planit.od.demand.OdDemands;
import org.planit.od.path.OdPaths;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.directed.DirectedVertex;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.math.Precision;
import org.planit.utils.misc.HashUtils;
import org.planit.utils.misc.LoggingUtils;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.layer.MacroscopicNetworkLayer;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegments;
import org.planit.utils.network.layer.physical.Node;
import org.planit.utils.network.virtual.ConnectoidSegment;
import org.planit.utils.pcu.PcuCapacitated;
import org.planit.utils.zoning.OdZones;

/**
 * Class exposing the various sLTM network loading solution method components of sLTM (not considering path choice, this is assumed to be given). Network loading solution method
 * Based on Raadsen and Bliemer (2021) General solution scheme for the Static Link Transmission Model .
 * 
 * @author markr
 *
 */
public class StaticLtmNetworkLoading {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(StaticLtmNetworkLoading.class.getCanonicalName());

  /**
   * run Id of the loading's parent asignment
   */
  private final long runId;

  // SIMULATION DATA //

  /** variables tracked for sending flow update step **/
  private final SendingFlowData sendingFlowData;

  /** variables tracked for receiving flow update step **/
  private final ReceivingFlowData receivingFlowData;

  /** variables tracked for splitting rate update step **/
  private SplittingRateData splittingRateData;

  /** tracks flow acceptance factors as well as its two related other factors, storage and capacity factors */
  private final NetworkLoadingFactorData networkLoadingFactorData;

  /** variables tracked for temporary inflow outflow tracking within sub algorithms **/
  private final InflowOutflowData inFlowOutflowData;

  /** the gap function to apply on global convergence update */
  private final NormBasedGapFunction flowAcceptanceFapFunction;

  /** the gap function to apply on sending flow update step */
  private final NormBasedGapFunction sendingFlowGapFunction;

  /** the gap function to apply on receiving flow update step */
  private final NormBasedGapFunction receivingFlowGapFunction;

  // INPUTS //

  /** transport network used */
  private final TransportModelNetwork network;

  /** mode used */
  private final Mode mode;

  /** odPaths to load */
  private final OdPaths odPaths;

  /** odDemands to load */
  private final OdDemands odDemands;

  // SETTINGS //

  /** user settings used regarding how to run the loading */
  private final StaticLtmNetworkLoadingSettings settings;

  /** analyser to track if loading is converging as expected based on its settings */
  private final StaticLtmNetworkLoadingConvergenceAnalyser convergenceAnalyser;

  /** track the approach of how the solution scheme is applied based on this type */
  private StaticLtmSolutionScheme solutionScheme;

  /**
   * Validate all constructor parameters
   * 
   */
  private void validateInputs() {
    if (mode == null) {
      throw new IllegalArgumentException("Mode for sLTM network loading is null");
    }

    if (network == null || network.getInfrastructureNetwork() == null || network.getInfrastructureNetwork().getLayerByMode(mode) == null) {
      throw new IllegalArgumentException(" network or network layer or mode of network layer not available for static LTM network loading");
    }

    if (!(network.getInfrastructureNetwork().getLayerByMode(mode) instanceof MacroscopicNetworkLayer)) {
      throw new IllegalArgumentException(String.format("Network layer for mode %s not of compatible type, expected MacroscopicNetworkLayer", mode.getXmlId()));
    }

    if (odDemands == null) {
      throw new IllegalArgumentException("OdDemands for sLTM network loading are null");
    }

    if (odPaths == null) {
      throw new IllegalArgumentException("OdPaths for sLTM network loading are null");
    }
  }

  /**
   * initialise the way the solution scheme is applied. Which in case of storage constraints is the basic decomposition scheme described in Raadsen and Bliemer (2021), and if
   * storage constraints are disabled, it the basic point queue model described in Bliemer et al (2014). Both solution schemes can be altered in case they do not converge by
   * activating various extensions see also {@link #activateNextExtension()}
   */
  private void initialiseStaticLtmSolutionSchemeApproach() {
    if (!solutionScheme.equals(StaticLtmSolutionScheme.NONE)) {
      LOGGER.severe("sLTM solution method can only be initialised once");
      return;
    }
    if (getSettings().isDisableStorageConstraints()) {
      solutionScheme = StaticLtmSolutionScheme.POINT_QUEUE_BASIC;
    } else {
      solutionScheme = StaticLtmSolutionScheme.PHYSICAL_QUEUE_BASIC;
    }

    LOGGER.info(String.format("sLTM network loading scheme set to %s", solutionScheme.getValue()));
  }

  /**
   * Convenience method to collect the used layer for this loading
   * 
   * @return layer used
   */
  private MacroscopicNetworkLayer getUsedNetworkLayer() {
    return ((MacroscopicNetworkLayer) this.network.getInfrastructureNetwork().getLayerByMode(mode));
  }

  /**
   * Conduct a network loading to compute updated inflow rates (without tracking turn flows): Eq. (3)-(4) in paper
   * 
   * @param linkSegmentFlowArrayToFill the inflows (u_a) to update
   */
  private void networkLoadingLinkSegmentInflowUpdate(final double[] linkSegmentFlowArrayToFill) {
    OdZones odZones = network.getZoning().odZones;
    double[] flowAcceptanceFactors = this.networkLoadingFactorData.getCurrentFlowAcceptanceFactors();

    /* update path turn flows (and sending flows if POINT_QUEUE_BASIC) */
    PathLinkInflowUpdateConsumer pathLinkInflowUpdateConsumer = new PathLinkInflowUpdateConsumer(odPaths, flowAcceptanceFactors, linkSegmentFlowArrayToFill);
    odDemands.forEachNonZeroOdDemand(odZones, pathLinkInflowUpdateConsumer);
  }

  /**
   * Initialise sending flows via
   * 
   * update link in/outflows via network loading Eq. (3)-(4) in paper Initial sending flows: s_a=u_a for all link segments a
   * 
   */
  private void initialiseSendingFlows() {
    networkLoadingLinkSegmentInflowUpdate(this.sendingFlowData.getCurrentSendingFlows());
    LinkSegmentData.copyTo(this.sendingFlowData.getCurrentSendingFlows(), this.sendingFlowData.getNextSendingFlows());
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
      double[] currReceivingFlows = this.receivingFlowData.getCurrentReceivingFlows();
      for (MacroscopicLinkSegment linkSegment : getUsedNetworkLayer().getLinkSegments()) {
        currReceivingFlows[(int) linkSegment.getId()] = linkSegment.computeCapacityPcuH();
      }
      for (ConnectoidSegment connectoidSegment : network.getVirtualNetwork().getConnectoidSegments()) {
        currReceivingFlows[(int) connectoidSegment.getId()] = connectoidSegment.computeCapacityPcuH();
      }
      LinkSegmentData.copyTo(currReceivingFlows, receivingFlowData.getNextReceivingFlows());

    } else {
      LOGGER.severe("sLTM with physical queues is not yet implemented, please disable storage constraints and try again");
    }
  }

  /**
   * Activate all nodes that require tracking during loading. Conduct after initial unconstrained loading is conducted.
   * 
   * In case the solution scheme is set the POINTQ_QUEUE_BASIC: Only a subset of all nodes require tracking. This is the least memory intensive approach where only nodes where for
   * any outgoing link b it holds that s_b>c_b (sending flow > capacity) is potentially restrictive, i.e., reduces sending flow to meet capacity requirements, needs to be tracked.
   * In this case the splitting rates data is required only for its incoming links during the loading.
   * <p>
   * In all other cases, the solution scheme adopts a locally iterative update of sending flows without any loading in between, this means that for flows to reach downstream nodes
   * the entire used network needs to be tracked, otherwise flows cannot propagate. Therefore, in this case we initialise the tracked nodes by considering all paths with non-zero
   * flows and activate the entry links of nodes passed.
   *
   * @param linkSegments to extract capacity from
   */
  private void initialiseSplittingRateData(MacroscopicLinkSegments linkSegments) {

    /* POINT QUEUE BASIC */
    if (this.solutionScheme.equals(StaticLtmSolutionScheme.POINT_QUEUE_BASIC)) {
      /* create */
      this.splittingRateData = new SplittingRateDataPartial();
      /* initialise */
      SplittingRateDataPartial pointQueueBasicSplittingRates = (SplittingRateDataPartial) this.splittingRateData;
      double[] sendingFlows = this.sendingFlowData.getCurrentSendingFlows();
      for (MacroscopicLinkSegment linkSegment : linkSegments) {
        double capacity = linkSegment.computeCapacityPcuH();

        /* register if unconstrained flow exceeds capacity */
        if (Precision.isGreater(sendingFlows[(int) linkSegment.getId()], capacity)) {
          pointQueueBasicSplittingRates.registerPotentiallyBlockingNode(linkSegment.getUpstreamNode());
        }
      }
    }
    /* OTHER, e.g. physical queues and advanced point queue model */
    else if (!this.solutionScheme.equals(StaticLtmSolutionScheme.NONE)) {
      /* create */
      this.splittingRateData = new SplittingRateDataComplete(this.inFlowOutflowData.getInflows().length);
      /* initialise */
      SplittingRateDataComplete extendedSplittingRates = (SplittingRateDataComplete) this.splittingRateData;
      ActivateSplittingRatesUsedNodesConsumer activateSplittingRateExtended = new ActivateSplittingRatesUsedNodesConsumer(extendedSplittingRates, odPaths);
      this.odDemands.forEachNonZeroOdDemand(this.network.getZoning().getOdZones(), activateSplittingRateExtended);
    }
  }

  //@formatter:off
  /**
   * Conduct a network loading to compute updated turn inflow rates u_ab: Eq. (3)-(4) in paper. We only consider turns on nodes that are potentially blocking to reduce
   * computational overhead.
   * 
   * @return acceptedTurnFlows (on potentially blocking nodes) where key comprises a combined hash of entry and exit edge segment ids and value is the accepted turn flow v_ab
   */
  private Map<Integer, Double> networkLoadingTurnFlowUpdate() {

    /* update path turn flows (and sending flows if POINT_QUEUE_BASIC)*/
    PathTurnFlowUpdateConsumer pathTurnFlowUpdateConsumer = 
        new PathTurnFlowUpdateConsumer(
            solutionScheme, 
            sendingFlowData, 
            splittingRateData, 
            networkLoadingFactorData, 
            odPaths);
    odDemands.forEachNonZeroOdDemand(network.getZoning().odZones, pathTurnFlowUpdateConsumer);
    return pathTurnFlowUpdateConsumer.getAcceptedTurnFlows();
  }

  /**
   * Update the splitting rates based on the provided accepted turn flows
   * 
   * @param acceptedTurnFlows to use to determine splitting rates
   */
  private void updateNextSplittingRates(final Map<Integer, Double> acceptedTurnFlows) {
    Set<DirectedVertex> trackedNodes = splittingRateData.getTrackedNodes();
    for (DirectedVertex node : trackedNodes) {
      for (EdgeSegment entrySegment : node.getEntryEdgeSegments()) {

        /* construct splitting rates by first imposing absolute turn flows */
        Array1D<Double> nextSplittingRates = splittingRateData.getSplittingRates(node, entrySegment);
        nextSplittingRates.reset();
        int index = 0;
        for (EdgeSegment exitSegment : node.getExitEdgeSegments()) {
          /* assume no uturn flow allowed */
          if (entrySegment.idEquals(exitSegment)) {
            continue;
          }
          nextSplittingRates.set(index++, acceptedTurnFlows.getOrDefault(HashUtils.createCombinedHashCode(entrySegment.getId(), exitSegment.getId()), 0.0));
        }

        /* sum all flows and then divide by this sum to obtain splitting rates */
        double totalEntryFlow = nextSplittingRates.aggregateAll(Aggregator.SUM);
        if (totalEntryFlow > Precision.EPSILON_6) {
          nextSplittingRates.modifyAll(PrimitiveFunction.DIVIDE.by(totalEntryFlow));
        } else {
          nextSplittingRates.fillAll(1.0);
        }
      }
    }
  }

  /**
   * For all potentially blocking nodes: perform a node model update based on: 1) sending flows, 2) receiving flows, 3) splitting rates resulting in newly accepted local outflows
   * and inflows.
   * 
   * @param consumer to apply to the result of each node model update of the considered nodes, may be null then ignored
   */
  private void performNodeModelUpdate(final ApplyToNodeModelResult consumer) {
    double[] sendingFlows = this.sendingFlowData.getCurrentSendingFlows();

    /* For each potentially blocking node */
    for (DirectedVertex potentiallyBlockingNode : splittingRateData.getTrackedNodes()) {
      // TODO: not computationally efficient, capacities are recomputed every time and construction of
      // turn sending flows is not ideal it requires a lot of copying of data that potentially could be optimised

      /* C_a : in Array1D form */
      Array1D<Double> inCapacities = Array1D.PRIMITIVE64.makeZero(potentiallyBlockingNode.sizeOfEntryEdgeSegments());
      int index = 0;
      for (EdgeSegment entryEdgeSegment : potentiallyBlockingNode.getEntryEdgeSegments()) {
        inCapacities.set(index++, ((PcuCapacitated) entryEdgeSegment).computeCapacityPcuH());
      }

      /* r_a : in Array1D form */
      Array1D<Double> outReceivingFlows = Array1D.PRIMITIVE64.makeZero(potentiallyBlockingNode.sizeOfExitEdgeSegments());
      index = 0;
      for (EdgeSegment exitEdgeSegment : potentiallyBlockingNode.getExitEdgeSegments()) {
        outReceivingFlows.set(index++, ((PcuCapacitated) exitEdgeSegment).computeCapacityPcuH());
      }

      /* s_ab : turn sending flows in per entrylinksegmentindex: Array1D (turn to outsegment flows) form */
      @SuppressWarnings("unchecked")
      Access1D<Double>[] tunSendingFlowsByEntryLinkSegment = (Access1D<Double>[]) new Access1D<?>[potentiallyBlockingNode.sizeOfEntryEdgeSegments()];
      int entryIndex = 0;
      for (Iterator<EdgeSegment> iter = potentiallyBlockingNode.getEntryEdgeSegments().iterator(); iter.hasNext(); ++entryIndex) {
        EdgeSegment entryEdgeSegment = iter.next();
        /* s_ab = s_a*phi_ab */
        double sendingFlow = sendingFlows[(int) entryEdgeSegment.getId()];
        Array1D<Double> localTurnSendingFlows = this.splittingRateData.getSplittingRates(potentiallyBlockingNode, entryEdgeSegment).copy();
        localTurnSendingFlows.modifyAll(PrimitiveFunction.MULTIPLY.by(sendingFlow));
        tunSendingFlowsByEntryLinkSegment[entryIndex] = localTurnSendingFlows;
      }
      Array2D<Double> turnSendingFlows = Array2D.PRIMITIVE64.rows(tunSendingFlowsByEntryLinkSegment);

      /* Kappa(s,r,phi) : node model update */
      TampereNodeModelFixedInput nodeModelInputFixed = new TampereNodeModelFixedInput(inCapacities, outReceivingFlows);
      try {
        TampereNodeModel nodeModel = new TampereNodeModel(new TampereNodeModelInput(nodeModelInputFixed, turnSendingFlows));
        Array1D<Double> localFlowAcceptanceFactor = nodeModel.run();

        /* delegate to consumer if any */
        if (consumer != null) {
          consumer.accept((Node) potentiallyBlockingNode, localFlowAcceptanceFactor, nodeModel);
        }

      } catch (Exception e) {
        LOGGER.severe(e.getMessage());
        LOGGER.severe(String.format("Unable to run Tampere node model on potentially blocking node %s", potentiallyBlockingNode.getXmlId()));
      }
    }
  }

  /**
   * Update (next) storage capacity factors, Eq. (11) using the next sending flows (representing the current inflows) and the current receiving flows.
   * 
   * We only update the factors for incoming links of potentially blocking nodes, because if the node is not potentially blocking the storage capacity factor multiplied by the flow
   * capacity factor results in inflow divided by outflow which always equals to one, so no need to actively track it (do note that this requires to also apply this to the updates
   * of flow capacity and flow acceptance factors, otherwise the combined result is inconsistent and can lead to serious issues in the outcomes)
   */
  private void updateNextStorageCapacityFactors() {
    this.networkLoadingFactorData.resetNextStorageCapacityFactors();
    double[] nextStorageCapacityFactor = this.networkLoadingFactorData.getNextStorageCapacityFactors();
    double[] inflows = this.inFlowOutflowData.getInflows();
    double[] receivingFlows = this.receivingFlowData.getCurrentReceivingFlows();

    int currentLinkSegmentId = -1;
    for (DirectedVertex potentiallyBlockingNode : this.splittingRateData.getTrackedNodes()) {
      for (EdgeSegment entryEdgeSegment : potentiallyBlockingNode.getEntryEdgeSegments()) {
        currentLinkSegmentId = (int) entryEdgeSegment.getId();
        /* gamma_a = u_a/r_a */
        nextStorageCapacityFactor[currentLinkSegmentId] = inflows[currentLinkSegmentId] / receivingFlows[currentLinkSegmentId];
      }
    }
  }

  /**
   * Update (next) flow acceptance factors, Eq. (9) using the current storage capacity and current flow capacity factors.
   * 
   * We only update the factors for incoming links of potentially blocking nodes, because if the node is not potentially blocking the flow acceptance factor is known to be 1 and
   * won't change throughout the loading
   */
  private void updateNextFlowAcceptanceFactors() {
    this.networkLoadingFactorData.resetNextFlowAcceptanceFactors();
    double[] inflows = this.inFlowOutflowData.getInflows();
    double[] nextFlowAcceptanceFactors = this.networkLoadingFactorData.getNextFlowAcceptanceFactors();
    double[] currentFlowCapacityFactors = this.networkLoadingFactorData.getCurrentFlowCapacityFactors();
    double[] currentStorageCapacityFactors = this.networkLoadingFactorData.getCurrentStorageCapacityFactors();

    int currentLinkSegmentId = -1;
    for (DirectedVertex potentiallyBlockingNode : this.splittingRateData.getTrackedNodes()) {
      for (EdgeSegment entryEdgeSegment : potentiallyBlockingNode.getEntryEdgeSegments()) {
        currentLinkSegmentId = (int) entryEdgeSegment.getId();
        /* alpha_a = beta_a^i-1 / gamma_a^i */
        if(inflows[currentLinkSegmentId] <= Precision.EPSILON_6) {
          /* special case: no inflow -> no restriction, set to 1 */
          nextFlowAcceptanceFactors[currentLinkSegmentId] = 1;
        }else {
          nextFlowAcceptanceFactors[currentLinkSegmentId] = currentFlowCapacityFactors[currentLinkSegmentId] / currentStorageCapacityFactors[currentLinkSegmentId];
        }
      }
    }
  }

  /**
   * Update (next) flow capacity factors, Eq. (10) using the next receiving flows and the current accepted outflows.
   * 
   * We only update the factors for incoming links of potentially blocking nodes, because if the node is not potentially blocking the storage capacity factor multiplied by the flow
   * capacity factor results in inflow divided by outflow which always equals to one, so no need to actively track it (do note that this requires to also apply this to the updates
   * of storage capacity and flow acceptance factors, otherwise the combined result is inconsistent and can lead to serious issues in the outcomes)
   * 
   * @param linkSegmentOutFlows to use, e.g. v_a.
   */
  private void updateNextFlowCapacityFactors() {
    this.networkLoadingFactorData.resetNextFlowCapacityFactors();
    double[] nextFlowCapacityFactors = this.networkLoadingFactorData.getNextFlowCapacityFactors();
    double[] outflows = this.inFlowOutflowData.getOutflows();
    double[] receivingFlows = this.receivingFlowData.getCurrentReceivingFlows();

    int currentLinkSegmentId = -1;
    for (DirectedVertex trackedNode : this.splittingRateData.getTrackedNodes()) {
      for (EdgeSegment entryEdgeSegment : trackedNode.getEntryEdgeSegments()) {
        currentLinkSegmentId = (int) entryEdgeSegment.getId();
        /* beta_a = v_a/r_a */
        nextFlowCapacityFactors[currentLinkSegmentId] = Math.min(1, outflows[currentLinkSegmentId] / receivingFlows[currentLinkSegmentId]);
      }
    }
  }

  /**
   * Constructor
   * 
   * @param idToken   for id generation of internal entities
   * @param runId run id the loading is applied for
   * @param network   to run on
   * @param mode      to use
   * @param odPaths   that require loading
   * @param odDemands to apply for this loading, reflecting the total desired flows between the ODs for the given mode
   */

  protected StaticLtmNetworkLoading(final IdGroupingToken idToken, long runId, final TransportModelNetwork network, final Mode mode, final OdPaths odPaths, final OdDemands odDemands) {
    this.runId = runId;
    this.network = network;
    this.mode = mode;
    this.odDemands = odDemands;
    this.odPaths = odPaths;    
    validateInputs();

    /* sLTM only uses a single layer */
    double[] referenceEmptyArray = new double[network.getNumberOfEdgeSegmentsAllLayers()];
    
    /* flow data defalts to zero unless explicitly set */
    this.sendingFlowData = new SendingFlowData(referenceEmptyArray);
    this.receivingFlowData = new ReceivingFlowData(referenceEmptyArray);       
    this.inFlowOutflowData = new InflowOutflowData(referenceEmptyArray);
    
    /* factor data defaults to 1 if reset, so provide this as the reference empty array */
    double[] referenceOneArray =  new double[referenceEmptyArray.length];
    Arrays.fill(referenceOneArray, 1.0);
    this.networkLoadingFactorData = new NetworkLoadingFactorData(referenceOneArray);

    /* gap functions used */
    this.flowAcceptanceFapFunction = new NormBasedGapFunction(idToken, new StopCriterion());
    this.sendingFlowGapFunction = new NormBasedGapFunction(idToken, new StopCriterion());
    this.receivingFlowGapFunction = new NormBasedGapFunction(idToken, new StopCriterion());

    /* settings and state trackers */
    this.settings = new StaticLtmNetworkLoadingSettings();
    this.convergenceAnalyser = new StaticLtmNetworkLoadingConvergenceAnalyser();
    this.solutionScheme = StaticLtmSolutionScheme.NONE;
  }

  //@formatter:off
  /**
   * Perform initialisation of the network loading. This method can only be called once.
   * 
   * 1. Initial acceptance flow, capacity, and storage factors, all set to one  
   * 2. Initial in/outflows via network loading Eq. (3)-(4) in paper: unconstrained network loading
   * 3. Initial sending and receiving flows: s_a=u_a, r_a=capacity_a for all link segments a
   * 4. Set iteration number to one (to be done exogneously)
   * (Extension A):  
   * 5. Restrict receiving flows to storage capacity Eq. (8) - only relevant when storage capacity is activated
   *   
   * @return true when successfull, false otherwise 
   */
  public boolean stepZeroInitialisation() {
    if(!getSettings().validate()) {
      LOGGER.severe(String.format("%sUnable to use sLTM settings, aborting initialisation of sLTM",LoggingUtils.createRunIdPrefix(runId)));
      return false;
    }
    /* activate the correct configuration of the initial solution scheme */
    initialiseStaticLtmSolutionSchemeApproach(); 
            
    /* 1. Initial acceptance flow, capacity, and storage factors, all set to one */
    networkLoadingFactorData.initialiseAll(1.0);
    
    /* 2. Initial sending flows via network loading Eq. (3)-(4) in paper: unconstrained network loading */
    initialiseSendingFlows();
    
    /* Depending on the solution scheme we either track all used nodes in the network, or a subset. Either way these need to be 
     * activated/initialised before commencing the loading. This is done here. */
    initialiseSplittingRateData(getUsedNetworkLayer().getLinkSegments());
    
    /* 3. limit flows to capacity s_a=r_a=min(u_a,cap_a) */
    /* reduce sending flows to capacity */
    this.sendingFlowData.limitCurrentSendingFlowsToCapacity(getUsedNetworkLayer().getLinkSegments());
    
    /* initialise receiving flows */
    initialiseReceivingFlows();
    
    return true;
  }
  
  //@formatter:off
  /**
   * Perform splitting rate update (before sending flow update) of the network loading:
   * 
   * 1. Update inflows via network loading Eq. (3)
   * 2. Update splitting rates Eq. (6),(4) 
   * (Extension B)
   * 3. If not first iteration then update splitting rates, Eq. (13)
   */
  public void stepOneSplittingRatesUpdate() {
    if(this.solutionScheme.isPhysicalQueue()) {
      LOGGER.severe(String.format("%ssLTM with physical queues is not yet implemented, please disable storage constraints and try again",LoggingUtils.createRunIdPrefix(runId)));
    }
    
    /* 1. Update turn inflows via network loading Eq. (3) */
    Map<Integer, Double> acceptedTurnFlows = networkLoadingTurnFlowUpdate();
    
    /* update splitting rates Eq. (6),(4) */
    updateNextSplittingRates(acceptedTurnFlows);
    
    /* TODO:
     * in case we do smoothing, it can be applied directly to the splitting rates per node such that
     * there is no need for a full copy of the entire splitting rate data (create per node/entry link local copy
     * of existing splitting rates, then compute new ones, and apply smoothing on the two, before moving to the next
     * entry link -> SEE NOTE IN PAPER ON TRACKING PREVIOUS SPLITTING RATES PER TYPE OF UPDATE*/    
  }  
  
  //@formatter:off
  /**
   * Perform splitting rate update (before sending flow update) of the network loading:
   * 
   * 1. Update node model to compute new inflows, Eq. (5)
   * 2. Update next sending flows via inflows, Eq. (7)
   * 3. Compute gap,  then update sending flows to next sending flows
   * 4. If converged continue, otherwise continue go back to Step 2-(1).
   * 5. Update storage capacity factors, Eq. (11)
   * (Extension B)
   * 6. Update smoothed storage capacity factors, Eq. (14)
   */
  public void stepTwoInflowSendingFlowUpdate() {
    if(this.solutionScheme.isPhysicalQueue()) {
      LOGGER.severe(String.format("%ssLTM with physical queues is not yet implemented, please disable storage constraints and try again",LoggingUtils.createRunIdPrefix(runId)));
      return;
    }
    
    int sendingFlowIterationIndex = 0;
    double sendingFlowGap = this.sendingFlowGapFunction.getGap();
    
    do {      
      /* 1. Update node model to compute new inflows, Eq. (5)
       * 2. Update next sending flows via inflows, Eq. (7) */
      LinkSegmentData.copyTo(this.sendingFlowData.getCurrentSendingFlows(), this.inFlowOutflowData.getInflows());
      performNodeModelUpdate(new UpdateExitLinkInflowsConsumer(this.inFlowOutflowData.getInflows()));
      /* s_a^tilde = u_a */
      LinkSegmentData.copyTo(this.inFlowOutflowData.getInflows(), this.sendingFlowData.getNextSendingFlows());
            
      /*3. Compute gap between current and next sending flows, then update sending flows to next sending flows */
      this.sendingFlowGapFunction.reset();
      this.sendingFlowGapFunction.increaseMeasuredValue(this.sendingFlowData.getNextSendingFlows(), this.sendingFlowData.getCurrentSendingFlows());
      sendingFlowGap = this.sendingFlowGapFunction.computeGap();
      
      /* 4a, update current sending flows s_a = s_a^tilde to next sending flows */
      this.sendingFlowData.swapCurrentAndNextSendingFlows();
      
      /* Only run as iterative procedure with physical queues or when using advanced point queue */
      if(this.solutionScheme.equals(StaticLtmSolutionScheme.POINT_QUEUE_BASIC)) {
        break;
      }      
      
      /* 4 If converged continue, otherwise go back to Step 2-(1). */
    }while(!this.sendingFlowGapFunction.getStopCriterion().hasConverged(sendingFlowGap, sendingFlowIterationIndex++));       
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
   */
  public void stepThreeSplittingRateUpdate() {
    if(!this.solutionScheme.isPhysicalQueue()) {
      /* ignored when not considering physical queues */
      return;
    }    
    
    if(this.solutionScheme.isPhysicalQueue()) {
      LOGGER.severe(String.format("%ssLTM with physical queues is not yet implemented, please disable storage constraints and try again",LoggingUtils.createRunIdPrefix(runId)));
      return;
    }    
    
    /* 1. Update intermediate flow acceptance factors, Eq. (9) */
    updateNextFlowAcceptanceFactors();
    
    /* 2. Update inflows via network loading, Eq. (3) */
    Map<Integer, Double> acceptedTurnFlows = networkLoadingTurnFlowUpdate();
    
    /* 3. update splitting rates Eq. (6),(4) */
    updateNextSplittingRates(acceptedTurnFlows);    
    
    /* TODO:
     * in case we do smoothing, it can be applied directly to the splitting rates per node such that
     * there is no need for a full copy of the entire splitting rate data (create per node/entry link local copy
     * of existing splitting rates, then compute new ones, and apply smoothing on the two, before moving to the next
     * entry link  -> SEE NOTE IN PAPER ON TRACKING PREVIOUS SPLITTING RATES PER TYPE OF UPDATE*/    
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
   */
  public void stepFourOutflowReceivingFlowUpdate() {
    /* update the outflows and receiving flows */      

    /* for now */
    if(!getSettings().isDisableStorageConstraints()) {
      LOGGER.severe(String.format("%ssLTM with physical queues is not yet implemented, please disable storage constraints and try again",LoggingUtils.createRunIdPrefix(runId)));
      return;
    }
     
    int receivingFlowIterationIndex = 0;
    double receivingFlowGap = this.receivingFlowGapFunction.getGap();    
    do {
      
      /* 1. Update node model to compute new outflows, Eq. (5) */
      performNodeModelUpdate(new UpdateEntryLinksOutflowConsumer(this.inFlowOutflowData.getOutflows()));
      
      /* POINT QUEUE -> only run as iterative procedure with physical queues are present and r can vary, now
       * we only require an update of outflows v to use for updating flow capacity factors */  
      if(this.solutionScheme.isPointQueue()) {
        break;
      }        
      
      /* 2. Update next receiving flows via inflows, Eq. (7)*/
      double[] outflows = this.inFlowOutflowData.getOutflows();
      double[] nextReceivingFlows = this.receivingFlowData.getNextReceivingFlows();
      for(DirectedVertex node : this.splittingRateData.getTrackedNodes()) {
        for (Iterator<EdgeSegment> iter = node.getEntryEdgeSegments().iterator(); iter.hasNext();) {
          EdgeSegment entryEdgeSegment = iter.next();
          int index = (int)entryEdgeSegment.getId();
        
          /* storage_capacity_a = (L*FD^-1(v_a))/T) */
          double storageCapacity = Double.POSITIVE_INFINITY; // TODO: entryLinkSegment.getParentLink().getLengthKm() * etc.;
          /* r_a = min(C_a, v_a + storage_Capacity_a) */
          double receivingFlow = Math.min(((PcuCapacitated)entryEdgeSegment).computeCapacityPcuH(), outflows[index] + storageCapacity);
          nextReceivingFlows[index] = receivingFlow;
        }
      }
      
      /*3. Compute gap between current and next sending receiving flows, then update receiving flows to next receiving flows */
      this.receivingFlowGapFunction.reset();
      this.receivingFlowGapFunction.increaseMeasuredValue(this.receivingFlowData.getNextReceivingFlows(), this.receivingFlowData.getCurrentReceivingFlows());
      receivingFlowGap = this.receivingFlowGapFunction.computeGap();
      
      /* 4a update r^i-1 = r^i */
      this.receivingFlowData.swapCurrentAndNextReceivingFlows();      
      
      /* 4b If converged continue, otherwise continue go back to Step 4-(1). */
    }while(!this.receivingFlowGapFunction.getStopCriterion().hasConverged(receivingFlowGap, receivingFlowIterationIndex++));  
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
      LOGGER.severe(String.format("%ssLTM with physical queues is not yet implemented, please disable storage constraints and try again",LoggingUtils.createRunIdPrefix(runId)));
      return true;
    }
    
    /* 1. Update flow acceptance factors, Eq. (9) */
    updateNextFlowAcceptanceFactors();
    
    /*3. Compute gap between current and next flow acceptance factors*/
    this.flowAcceptanceFapFunction.reset();
    this.flowAcceptanceFapFunction.increaseMeasuredValue(this.networkLoadingFactorData.getNextFlowAcceptanceFactors(), this.networkLoadingFactorData.getCurrentFlowAcceptanceFactors());
    double globalGap = this.flowAcceptanceFapFunction.computeGap();
    this.convergenceAnalyser.registerIterationGap(globalGap);
    
    if(getSettings().isDetailedLogging()) {
      LOGGER.info(String.format("%sNetwork loading gap (i=%d): %.10f",LoggingUtils.createRunIdPrefix(runId), networkLoadingIteration, globalGap));
    }
    
    /* set next to current */
    this.networkLoadingFactorData.swapCurrentAndNextFlowAcceptanceFactors();
    
    boolean converged = this.flowAcceptanceFapFunction.getStopCriterion().hasConverged(globalGap, networkLoadingIteration);        
    if(converged) {
      LOGGER.info(String.format("%ssLTM network loading converged in %d iterations (remaining gap: %.10f)",LoggingUtils.createRunIdPrefix(runId), networkLoadingIteration, globalGap));
    }
    return converged;
  }
  
  /** Verify if we are still converging
   * 
   * @return true when potentially still converging, false otherwise
   */
  public boolean isConverging() {
    return convergenceAnalyser.isImproving();
  }

  /**
   * Given the current extension status and type of sLTM that we are conducting, activate the next extension in loading to
   * improve the likelihood of network loading convergence. Each additional extension that is activated will slow down convergence,, so only
   * do this when it is clear the current scheme does not suffice
   * 
   * @param logRecentGaps when true log all gaps in the period the most recent solution scheme method was active, when false do not
   * @return true when scheme changed, false if no longer possible to change any further  
   */
  public boolean activateNextExtension(boolean logRecentGaps) {
    if(logRecentGaps) {
      convergenceAnalyser.logGapsSince(runId, convergenceAnalyser.getIterationOffset());
    }
    convergenceAnalyser.setIterationOffset(convergenceAnalyser.getRegisteredIterations());
    boolean solutionSchemeChanged = true;

    /* POINT - QUEUE */
    if(this.solutionScheme.isPointQueue()) {
      if(this.solutionScheme.equals(StaticLtmSolutionScheme.POINT_QUEUE_BASIC)) {
        /* BASIC -> ADVANCED, e.g., activate local iterative updates of sending flows */
        this.solutionScheme = StaticLtmSolutionScheme.POINT_QUEUE_ADVANCED;
        
        /* sending flows must be re-initialised since otherwise the sending flows of earlier non-tracked nodes have been rest to zero 
         * during earlier loading iterations, now they must be available for all tracked nodes, so we reinitialise by conducting a full initialisation based
         * on paths and most recent flow acceptance factors*/
        initialiseSendingFlows();
                
        /* Splitting rates must be re-initialised in this approach as well (happens automatically in called method via changed solution scheme): 
         * Change from using only a small subset of nodes with splitting rates to tracking splitting rates for all used nodes */
        initialiseSplittingRateData(getUsedNetworkLayer().getLinkSegments());        
      }else {
        /* no other extensions available, so deactivate any further extensions by maximising the offset */
        convergenceAnalyser.setMinIterationThreshold(Integer.MAX_VALUE);
        solutionSchemeChanged = false;
      }
      
    }
    /* PHYSICAL - QUEUE */
    else {
      LOGGER.warning(String.format("%sNo extensions have yet been implemented for sLTM with physical queues",LoggingUtils.createRunIdPrefix(runId)));
      solutionSchemeChanged = false;
    }    
    
    if(solutionSchemeChanged) {
      LOGGER.info(String.format("%sSwitching network loading scheme to %s", LoggingUtils.createRunIdPrefix(runId), solutionScheme.getValue()));
    }
    
    return solutionSchemeChanged;
  }

  /** Collect the settings. Only make changes before running any of the loading steps, otherwise risk undefined 
   * behaviour by the loading.
   * 
   * @return settings of this loading
   */
  public StaticLtmNetworkLoadingSettings getSettings() {
    return settings;
  }


}

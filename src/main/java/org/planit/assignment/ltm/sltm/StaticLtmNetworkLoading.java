package org.planit.assignment.ltm.sltm;

import java.util.HashMap;
import java.util.Iterator;
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
import org.planit.utils.mode.Mode;
import org.planit.utils.network.layer.MacroscopicNetworkLayer;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegments;
import org.planit.utils.path.DirectedPath;
import org.planit.utils.zoning.OdZone;
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

  /** variables tracked for sending flow update step **/
  private final SendingFlowData sendingFlowData;

  /** variables tracked for receiving flow update step **/
  private final ReceivingFlowData receivingFlowData;

  /** variables tracked for splitting rate update step **/
  private final SplittingRateData splittingRateData;

  /** tracks flow acceptance factors as well as its two related other factors, storage and capacity factors */
  private final NetworkLoadingFactorData networkLoadingFactorData;

  /** transport network used */
  private final TransportModelNetwork network;

  /** mode used */
  private final Mode mode;

  /** odPaths to load */
  final OdPaths odPaths;

  /** odDemands to load */
  final OdDemands odDemands;

  /** internal iteration index for network loading */
  int networkLoadingIterationIndex;

  /** the gap function to apply on global convergence update */
  private final NormBasedGapFunction flowAcceptanceFapFunction;

  /** the gap function to apply on sending flow update step */
  private final NormBasedGapFunction sendingFlowGapFunction;

  /** the gap function to apply on receiving flow update step */
  private final NormBasedGapFunction receivingFlowGapFunction;

  /**
   * Validate provided constructor parameters
   * 
   * @param network to validate
   * @param mode    to validate
   */
  private void validate(TransportModelNetwork network, Mode mode) {
    if (network == null || network.getInfrastructureNetwork() == null || network.getInfrastructureNetwork().getLayerByMode(mode) == null) {
      throw new IllegalArgumentException(" network or network layer or mode of network layer not available for static LTM network loading");
    }

    if (!(network.getInfrastructureNetwork().getLayerByMode(mode) instanceof MacroscopicNetworkLayer)) {
      throw new IllegalArgumentException(String.format("Network layer for mode %s not of compatible type, expected MacroscopicNetworkLayer", mode.getXmlId()));
    }
  }

  /**
   * Conduct a network loading to compute updated inflow rates (without tracking turn flows): Eq. (3)-(4) in paper
   */
  private void networkLoadingLinkSegmentInflowUpdate(final double[] linkSegmentFlowArrayToFill) {
    OdZones odZones = network.getZoning().odZones;
    double[] flowAcceptanceFactors = this.networkLoadingFactorData.getCurrentFlowAcceptanceFactors();

    /* origin */
    for (OdZone origin : odZones) {
      /* destination */
      for (OdZone destination : odZones) {
        Double odDemand = odDemands.getValue(origin, destination);
        if (odDemand != null && odDemand > 0) {
          /* path */
          DirectedPath odPath = odPaths.getValue(origin, destination);
          double acceptedPathFlowRate = odDemand;
          for (EdgeSegment edgeSegment : odPath) {
            /* link segment */

            /* u_a: update inflow for link segment */
            linkSegmentFlowArrayToFill[(int) edgeSegment.getId()] += acceptedPathFlowRate;
            acceptedPathFlowRate *= flowAcceptanceFactors[(int) edgeSegment.getId()];
          }
        }
      }
    }
  }

  /**
   * Conduct after unconstrained loading, all nodes where for any outgoing link b it holds that s_b>c_b (sending flow > capacity) is potentially restrictive, i.e., reduces sending
   * flow to meet capacity requirements, therefore it should be registered on the splitting rates data as we require tracking of splitting rates during the loading for these nodes
   * 
   * @param linkSegments to extract capacity from
   */
  private void initialisePotentiallyBlockingNodes(MacroscopicLinkSegments linkSegments) {
    double[] sendingFlows = this.sendingFlowData.getCurrentSendingFlows();
    for (MacroscopicLinkSegment linkSegment : linkSegments) {
      double capacity = linkSegment.computeCapacityPcuH();
      if (Precision.isGreater(sendingFlows[(int) linkSegment.getId()], capacity)) {
        this.splittingRateData.registerPotentiallyBlockingNode(linkSegment.getUpstreamNode());
      }
    }
  }

  /**
   * Conduct a network loading to compute updated turn inflow rates u_ab: Eq. (3)-(4) in paper. We only consider turns on nodes that are potentially blocking to reduce
   * computational overhead.
   * 
   * @return acceptedTurnFlows (on potentially blocking nodes) where key comprises a combined hash of entry and exit edge segment ids and value is the accepted turn flow v_ab
   */
  private HashMap<Integer, Double> networkLoadingTurnFlowUpdate() {
    OdZones odZones = network.getZoning().odZones;
    double[] flowAcceptanceFactors = this.networkLoadingFactorData.getCurrentFlowAcceptanceFactors();

    HashMap<Integer, Double> acceptedTurnFlows = new HashMap<Integer, Double>();
    /* origin */
    for (OdZone origin : odZones) {
      /* destination */
      for (OdZone destination : odZones) {
        Double odDemand = odDemands.getValue(origin, destination);
        if (odDemand != null && odDemand > 0) {
          /* path */
          DirectedPath odPath = odPaths.getValue(origin, destination);
          double acceptedPathFlowRate = odDemand;
          if (odPath.isEmpty()) {
            LOGGER.warning(String.format("IGNORE: encountered empty path %s", odPath.getXmlId()));
            continue;
          }

          /* turn */
          Iterator<EdgeSegment> edgeSegmentIter = odPath.iterator();
          EdgeSegment previousEdgeSegment = edgeSegmentIter.next();
          while (edgeSegmentIter.hasNext()) {
            EdgeSegment currEdgeSegment = edgeSegmentIter.next();

            if (this.splittingRateData.isPotentiallyBlocking(currEdgeSegment.getUpstreamVertex())) {
              /* v_ap = u_bp = alpha_a*...*f_p where we consider all preceding alphas (flow acceptance factors) up to now */
              acceptedPathFlowRate *= flowAcceptanceFactors[(int) previousEdgeSegment.getId()];
              acceptedTurnFlows.put(HashUtils.createCombinedHashCode(previousEdgeSegment.getId(), currEdgeSegment.getId()), acceptedPathFlowRate);
            }

            previousEdgeSegment = currEdgeSegment;
          }
        }
      }
    }
    return acceptedTurnFlows;
  }

  /**
   * Update the splitting rates based on the provided accepted turn flows
   * 
   * @param acceptedTurnFlows to use to determine splitting rates
   */
  private void updateNextSplittingRates(final HashMap<Integer, Double> acceptedTurnFlows) {
    Set<DirectedVertex> potentiallyBlockingNodes = splittingRateData.getPotentiallyBlockingNodes();
    for (DirectedVertex node : potentiallyBlockingNodes) {
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
    for (DirectedVertex potentiallyBlockingNode : splittingRateData.getPotentiallyBlockingNodes()) {
      // TODO: not computationally efficient, capacities are recomputed every time and construction of
      // turn sending flows is not ideal it requires a lot of copying of data that potentially could be optimised

      /* C_a : in Array1D form */
      Array1D<Double> inCapacities = Array1D.PRIMITIVE64.makeZero(potentiallyBlockingNode.sizeOfEntryEdgeSegments());
      int index = 0;
      for (EdgeSegment entryEdgeSegment : potentiallyBlockingNode.getEntryEdgeSegments()) {
        inCapacities.set(index++, ((MacroscopicLinkSegment) entryEdgeSegment).computeCapacityPcuH());
      }

      /* r_a : in Array1D form */
      Array1D<Double> outReceivingFlows = Array1D.PRIMITIVE64.makeZero(potentiallyBlockingNode.sizeOfExitEdgeSegments());
      index = 0;
      for (EdgeSegment exitEdgeSegment : potentiallyBlockingNode.getExitEdgeSegments()) {
        outReceivingFlows.set(index++, ((MacroscopicLinkSegment) exitEdgeSegment).computeCapacityPcuH());
      }

      /* s_ab : turn sending flows in Array2D form */
      @SuppressWarnings("unchecked")
      Access1D<Double>[] tunSendingFlowsByEntryLinkSegment = (Access1D<Double>[]) new Access1D<?>[potentiallyBlockingNode.sizeOfExitEdgeSegments()];
      int entryIndex = 0;
      for (Iterator<EdgeSegment> iter = potentiallyBlockingNode.getEntryEdgeSegments().iterator(); iter.hasNext(); ++index) {
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
          consumer.accept(potentiallyBlockingNode, localFlowAcceptanceFactor, nodeModel);
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
    double[] nextStorageCapacityFactor = this.networkLoadingFactorData.getNextStorageCapacityFactors();
    double[] inflows = this.sendingFlowData.getNextSendingFlows();
    double[] receivingFlows = this.receivingFlowData.getCurrentReceivingFlows();

    int currentLinkSegmentId = -1;
    for (DirectedVertex potentiallyBlockingNode : this.splittingRateData.getPotentiallyBlockingNodes()) {
      for (EdgeSegment entryEdgeSegment : potentiallyBlockingNode.getEntryEdgeSegments()) {
        currentLinkSegmentId = (int) entryEdgeSegment.getId();
        /* gamma_a = u_a/r_a */
        nextStorageCapacityFactor[currentLinkSegmentId] = inflows[currentLinkSegmentId] / receivingFlows[currentLinkSegmentId];
      }
    }
  }

  /**
   * Constructor
   * 
   * @param idToken   for id generation of internal entities
   * @param network   to run on
   * @param mode      to use
   * @param odPaths   that require loading
   * @param odDemands to apply for this loading, reflecting the total desired flows between the ODs for the given mode
   */

  protected StaticLtmNetworkLoading(IdGroupingToken idToken, final TransportModelNetwork network, final Mode mode, final OdPaths odPaths, final OdDemands odDemands) {
    validate(network, mode);
    this.network = network;
    this.mode = mode;
    this.odDemands = odDemands;

    MacroscopicNetworkLayer networkLayer = (MacroscopicNetworkLayer) network.getInfrastructureNetwork().getLayerByMode(mode);
    double[] referenceEmptyArray = new double[networkLayer.getLinkSegments().size()];
    this.sendingFlowData = new SendingFlowData(referenceEmptyArray);
    this.receivingFlowData = new ReceivingFlowData(referenceEmptyArray);
    this.splittingRateData = new SplittingRateData();
    this.networkLoadingFactorData = new NetworkLoadingFactorData(referenceEmptyArray);
    this.odPaths = odPaths;

    this.flowAcceptanceFapFunction = new NormBasedGapFunction(idToken, new StopCriterion());
    this.sendingFlowGapFunction = new NormBasedGapFunction(idToken, new StopCriterion());
    this.receivingFlowGapFunction = new NormBasedGapFunction(idToken, new StopCriterion());
  }

  //@formatter:off
  /**
   * Perform initialisation of the network loading:
   * 
   * 1. Initial acceptance flow, capacity, and storage factors, all set to one  
   * 2. Initial in/outflows via network loading Eq. (3)-(4) in paper: unconstrained network loading
   * 3. Initial sending and receiving flows: s_a=u_a, r_a=capacity_a for all link segments a
   * 4. Set iteration number to one
   * (Extension A):  
   * 5. Restrict receiving flows to storage capacity Eq. (8) - only relevant when storage capacity is activated
   */
  public void stepZeroInitialisation() {
    MacroscopicLinkSegments linkSegments = ((MacroscopicNetworkLayer) this.network.getInfrastructureNetwork().getLayerByMode(mode)).getLinkSegments();
  
    
    /* 1. Initial acceptance flow, capacity, and storage factors, all set to one */
    networkLoadingFactorData.initialiseAll(1.0);
    
    /* 2. Initial sending and receiving outflows via network loading Eq. (3)-(4) in paper: unconstrained network loading */
    networkLoadingLinkSegmentInflowUpdate(this.sendingFlowData.getCurrentSendingFlows());
    
    /* identify all potentially blocking nodes, i.e., all nodes where unconstrained sending flow exceeds link segment capacity */
    initialisePotentiallyBlockingNodes(linkSegments);
    
    /* 3. limit flows to capacity s_a=r_a=min(u_a,cap_a) */
    /* reduce sending flows to capacity */
    this.sendingFlowData.limitCurrentSendingFlowsToCapacity(linkSegments);
    /* s=r */
    LinkSegmentData.copyTo(this.sendingFlowData.getCurrentSendingFlows(), receivingFlowData.getCurrentReceivingFlows());   
    
    /* 4. Initialise iteration index */
    this.networkLoadingIterationIndex = 0;
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
    /* 1. Update turn inflows via network loading Eq. (3) */
    HashMap<Integer, Double> acceptedTurnFlows = networkLoadingTurnFlowUpdate();
    /* update splitting rates Eq. (6),(4) */
    updateNextSplittingRates(acceptedTurnFlows);
    
    /* TODO:
     * in case we do smoothing, it can be applied directly to the splitting rates per node such that
     * there is no need for a full copy of the entire splitting rate data (create per node/entry link local copy
     * of existing splitting rates, then compute new ones, and apply smoothing on the two, before moving to the next
     * entry link */    
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
  public void stepTwoSendingFlowUpdate() {
    int sendingFlowIterationIndex = 0;
    double sendingFlowGap = this.sendingFlowGapFunction.getGap();
    
    do {
      /* 4b, when not converged update current sending flows to next sending flows */
      this.sendingFlowData.setCurrentSendingFlowsToNext();
      this.sendingFlowData.resetNextSendingFlows();
      
      /* 1. Update node model to compute new inflows, Eq. (5)
       * 2. Update next sending flows via inflows, Eq. (7) */
      performNodeModelUpdate(new UpdateNextSendingFlowsConsumer(this.sendingFlowData.getNextSendingFlows()));
      
      /*3. Compute gap between current and next sending flows, then update sending flows to next sending flows */
      this.sendingFlowGapFunction.reset();
      this.sendingFlowGapFunction.increaseMeasuredValue(this.sendingFlowData.getNextSendingFlows(), this.sendingFlowData.getCurrentSendingFlows());
      sendingFlowGap = this.sendingFlowGapFunction.computeGap();
      
      /* 4 If converged continue, otherwise continue go back to Step 2-(1). */
    }while(!this.sendingFlowGapFunction.getStopCriterion().hasConverged(sendingFlowGap, sendingFlowIterationIndex++));
    this.sendingFlowGapFunction.reset();
    
    /* Update storage capacity factors, Eq. (11) */
    updateNextStorageCapacityFactors();
    
    /* TODO:
     * in case we do smoothing, it can be applied directly to the capacity factor per node such that
     * there is no need for a full copy of the entire factor data (create per node/entry link local copy)*/
    this.networkLoadingFactorData.setCurrentStorageCapacityFactorsToNext();
    this.networkLoadingFactorData.resetNextStorageCapacityFactors();    
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
    //TODO
  }  
  
  /**
   * 1. Update node model, to compute outflows Eq. (5)
   * 2. Update receiving flows based on outflows, Eq. (8)
   * (Extension B)
   * 3. Transform to nudged receiving flows using multiplication factor, Eq. (18)
   * (end Extension B)
   * 4. Compute gap then set next receicing flows to current receiving flows   
   * 5. If converged continue, else go back to Step 4-(1).
   * 6. Update link capacity factors, Eq. (10)
   * (Extension C)
   * 7. Update smoothed link capacity factors, Eq. (14)
   */
  public void stepFourReceivingFlowUpdate() {
    //TODO
  }

  /**
   * 1. Update flow acceptance factors, Eq. (9)
   * 2. Compute gap using flow acceptance factors,  
   * 3. Increment iteration index,  
   * 4. If converged done, else go back to Step 1.
   * 
   * @return true when converged, false otherwise
   */
  public boolean stepFiveCheckNetworkLoadingConvergence() {
    //TODO
    return false;
  }
}

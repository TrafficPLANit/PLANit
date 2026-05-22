package org.goplanit.assignment.ltm.sltm.loading;

import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.goplanit.assignment.common.bush.RootedBush;
import org.goplanit.assignment.ltm.sltm.input.StaticLtmSettings;
import org.goplanit.assignment.ltm.sltm.consumer.BushFlowUpdateConsumer;
import org.goplanit.assignment.ltm.sltm.consumer.NetworkFlowUpdateData;
import org.goplanit.assignment.ltm.sltm.consumer.NetworkTurnFlowUpdateData;
import org.goplanit.assignment.common.pas.Pas;
import org.goplanit.assignment.common.pas.PasManager;
import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.physical.CompiledRelationMapping;

/**
 * The bush based network loading scheme for sLTM - base class
 * 
 * @author markr
 *
 */
public abstract class StaticLtmLoadingBushBase<B extends RootedBush<?,?>> extends
        StaticLtmNetworkLoading {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(StaticLtmLoadingBushBase.class.getCanonicalName());

  /** the bushes managed by the bush strategy but provided to be able to conduct a network loading based on the
   * current state (bush splitting rates) of each bush */
  private Set<B> bushes;

  /**
   * the PAS manager with all the currently active PASs, used to determine which nodes to track flows and splitting
   * rates for during network loading, namely all links and nodes
   * present in the active PASs
   */
  private PasManager<?,?> pasManager;

  /**
   * Factory method to create network link flow data container based on coniguration provided. to be used in
   * conjunction with creating network loading bush consumers.
   *
   * @param updateLinkOutflows when true make sure data contains reference to the network loading outflows
   * @param updateUnconstrainedLinkFlows when true make sure data contains reference to the network unconstrained flows
   * @return network flow data created
   */
  public NetworkFlowUpdateData createNetworkLinkFlowData(
          boolean updateLinkOutflows, boolean updateUnconstrainedLinkFlows) {

      if (updateLinkOutflows) {
        /* sending + outflow update only */
        return updateUnconstrainedLinkFlows ?
                new NetworkFlowUpdateData(
                        nlSendingFlowData, nlInFlowOutflowData, networkLoadingFactorData, unconstrainedFlowData) :
                new NetworkFlowUpdateData(nlSendingFlowData, nlInFlowOutflowData, networkLoadingFactorData);
      } else {
        /* sending flow update only */
        return updateUnconstrainedLinkFlows ?
                new NetworkFlowUpdateData(nlSendingFlowData, networkLoadingFactorData, unconstrainedFlowData) :
                new NetworkFlowUpdateData(nlSendingFlowData, networkLoadingFactorData);
      }
    }

  /**
   * Factory method to create network turn flow data container based on foniguration provided. to be used in
   * conjunction with creating network loading bush consumers.
   *
   * @param updateLinkSendingFlows when true make sure data contains reference to the network loading sending flows
   * @param numTurnSegments        number of entries in turn segment related raw data arrays that are created as part of
   *                               the container
   * @return network turn flow data created
   */
  public NetworkTurnFlowUpdateData createNetworkTurnFlowData(
          boolean updateLinkSendingFlows, int numTurnSegments) {
    if (updateLinkSendingFlows) {
      return new NetworkTurnFlowUpdateData(
              isTrackAllNodeTurnFlowsDuringLoading(),
              nlSendingFlowData,
              nlSplittingRateData,
              networkLoadingFactorData,
              numTurnSegments);
    }else {
      return new NetworkTurnFlowUpdateData(
              isTrackAllNodeTurnFlowsDuringLoading(),
              nlSplittingRateData,
              networkLoadingFactorData,
              numTurnSegments);
    }
  }

  /**
   * Factory method to create the right link flow update consumer to use when conducting a bush based flow update.
   * This link based version is to be used for initialisation/finalisation purposes only
   *
   * @param updateLinkOutflows           flag indicating if the link outflows are to be updated by this consumer
   *                                     in addition to sending flows
   * @param updateUnconstrainedLinkFlows flag indicating if the unconstrained link flows are to be tracked/updated
   *                                     by this consumer in addition to sending flows
   * @return created link (sending/outflow/unconsitrained) flow update consumer
   */
  public abstract BushFlowUpdateConsumer<B> createBushLinkSendingFlowUpdateConsumer(
          boolean updateLinkOutflows, boolean updateUnconstrainedLinkFlows);

  /**
   * Factory method to create the right flow update consumer to use when conducting a bush based flow update.
   * This version is for updating turn accepted flows (and possibly also link sending flows)
   *
   * @param updateLinkSendingFlows flag indicating if the link sending flows are to be updated by this consumer
   *                               as well
   * @return created turn (and potentially link) flow update consumer
   */
  public abstract BushFlowUpdateConsumer<B> createBushTurnFlowUpdateConsumer(boolean updateLinkSendingFlows);

  /** bush sync all network flow consumer, to be used only after convergence of internal iterative procedure
   * @return consumer
   */
  public abstract BushFlowUpdateConsumer<B> createSyncAllNetworkFlowUpdateConsumer();

  /**
   * Factory method to create the right flow update consumer to use when conducting a bush based flow update.
   * We either create one that updates turn accepted flows (and possibly also sending flows), or one that
   * only updates (network wide) link sending flows and/or link outflows. The latter is to be used for
   * initialisation/finalisation purposes only. The former is the one used during the iterative loading procedure.
   *
   * @param updateTurnAcceptedFlows  flag indicating if the turn accepted flows are to be updated by this consumer
   * @param updateSendingFlows       flag indicating if the link sending flow are to be updated by this consumer
   * @param updateLinkOutflows       flag indicating if the link outflows are to be updated by this consumer
   * @param updateUnconstrainedFlows flag indicating if the unconstrained link flows are to be tracked/updated by this consumer
   * @return created flow update consumer
   */
  public BushFlowUpdateConsumer<B> createRegularBushLoadingFlowUpdateConsumer(
          boolean updateTurnAcceptedFlows,
          boolean updateSendingFlows,
          boolean updateLinkOutflows,
          boolean updateUnconstrainedFlows){

    if (!updateSendingFlows && !updateTurnAcceptedFlows) {
      LOGGER.warning("Network flow updates using bushes must either updating link sending flows or turn accepted " +
              "flows, neither are selected");
      return null;
    }

    // prep by resetting
    if (updateSendingFlows) {
      nlSendingFlowData.reset();
    }
    if (updateLinkOutflows) {
      this.nlInFlowOutflowData.resetOutflows();
    }
    if(updateUnconstrainedFlows){
      this.unconstrainedFlowData.reset();
    }

    if (!updateTurnAcceptedFlows) {
      /* link based sending flows + potentially link outflows/unconstrained flows */
      return createBushLinkSendingFlowUpdateConsumer(updateLinkOutflows, updateUnconstrainedFlows);
    }else{
      if (updateLinkOutflows) {
        LOGGER.warning("Network outflow updates using bushes can only be combined with link sending flows updates, " +
                "not turn accepted flows");
        return null;
      }
      /* turn based sending flows + potentially link sending flows */
      return createBushTurnFlowUpdateConsumer(updateSendingFlows);
    }
  }

  //@formatter:off
  /**
   * Conduct a network loading to compute updated turn inflow rates u_ab: Eq. (3)-(4) in paper. We only consider turns
   * on nodes that are potentially blocking to reduce computational overhead.
   *
   * @param mode                    unused
   * @return acceptedTurnFlows (on potentially blocking nodes) where key comprises a combined hash of entry and exit
   * edge segment ids and value is the accepted turn flow v_ab
   */
  @Override
  protected TurnFlowAccessor networkLoadingTurnFlowUpdate(Mode mode) {
   
    /* update network turn flows (and sending flows if POINT_QUEUE_BASIC) by performing a network loading
     * on all bushes using the bush-splitting rates (and updating the bush turn sending flows in the process,
     * so they remain consistent with the loading)
     */
    boolean updateTurnAcceptedFlows = true;
    boolean updateSendingFlowDuringLoading = !isIterativeSendingFlowUpdateActivated();
    boolean updateOutflows = false;
    boolean updateUnconstrainedFlows = false;
    var bushTurnFlowUpdateConsumer = createRegularBushLoadingFlowUpdateConsumer(
            updateTurnAcceptedFlows, updateSendingFlowDuringLoading, updateOutflows, updateUnconstrainedFlows);
    
    /* execute */
    executeNetworkLoadingUpdate(bushTurnFlowUpdateConsumer);

    /* result */
    return bushTurnFlowUpdateConsumer.getAcceptedTurnFlows();
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  protected void networkLoadingLinkSegmentSendingFlowUpdate(Mode mode, boolean updateUnconstrainedFlows) {
        
    /* configure to only update all link segment sending flows */
    boolean updateTurnAcceptedFlows = false;
    boolean updateSendingFlowDuringLoading = true;
    boolean updateOutflows = false;
    var bushFlowUpdateConsumer =
        createRegularBushLoadingFlowUpdateConsumer(
                updateTurnAcceptedFlows, updateSendingFlowDuringLoading, updateOutflows, updateUnconstrainedFlows);
    
    /* execute */
    executeNetworkLoadingUpdate(bushFlowUpdateConsumer);
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  protected void networkLoadingSyncFlowsUpdate(Mode mode) {
        
    var syncFlowConsumer = createSyncAllNetworkFlowUpdateConsumer();
    
    /* execute */
    executeNetworkLoadingUpdate(syncFlowConsumer);

    // construct splitting rates
    var turnFlows = syncFlowConsumer.getAcceptedTurnFlows();
    updateNextSplittingRates(turnFlows);
  }

  /**
   * Initialise tracking of splitting rates and network flows on all nodes that are used by any currently
   * active AND inactive PAS. This way we are able to ascertain how much total network flow runs through each PAS
   * which in turn is used to determine how much flow we can shift between segments.
   * <p>
   *   We also activate all inactive PASs here to ensure that if an inactive PAS gets reactivated we have all the
   *   information readily available.
   * </p>
   * todo: we could reduce memory footprint by postponing flow shifts on reactivated PASs by one iteration in which case
   *   we only need to track active PASs.
   */
  @Override
  protected void activateEligibleSplittingRateTrackedNodes() {
    this.pasManager.forEachActivePas( this::activateNodeTrackingFor);
    this.pasManager.forEachInactivePas( this::activateNodeTrackingFor);
  }

  /**
   * Constructor
   * 
   * @param idToken      to use
   * @param assignmentId to use
   * @param compiledMovementIds implicit movementIds used for turn based data indexing
   * @param settings to use
   */
  public StaticLtmLoadingBushBase(
          IdGroupingToken idToken,
          long assignmentId,
          CompiledRelationMapping compiledMovementIds,
          final StaticLtmSettings settings) {
    super(idToken, assignmentId, compiledMovementIds, settings);
  }
  
  /** The bushes to use when a loading update is requested
   * 
   * @param bushes to use
   */
  public void setBushes(final Set<B> bushes) {
    this.bushes = bushes;    
  }
  
  /** The PasManager to use when we must initialise the tracked network nodes (namely all nodes
   * that are part of a PAS, since we need to know the network flow that passes through them)
   * 
   * @param pasManager to use
   */
  public void setPasManager(final PasManager<?,?> pasManager) {
    this.pasManager = pasManager;
  } 

  /** For each PAS we must be able to determine the network level flows along the segments, see computeSubPathSendingFlow(). 
   * This requires knowing the network level splitting rates on the network level as well as the sending flows and
   * acceptance factors, otherwise we cannot determine this. Therefore, for each newly identified PAS we activate
   * node tracking for all (eligible) nodes along the segments of this PAS, if not already done so
   *
   *@param newPas to activate nodes on segments for
   */
  public void activateNodeTrackingFor(final Pas<? extends DirectedVertex,?> newPas) {
    if(newPas==null) {
      LOGGER.severe("Provided PAS is null, unable to activate node tracking for alternative segments");
      return;
    }
    /* only when not all turn flows are tracked, we must expand the tracked nodes, otherwise they are already
    available */
    if(!isTrackAllNodeTurnFlowsDuringLoading()) {
      var pointQueueBasicSplittingRates = (NetworkLoadingSplittingRateDataPartial) this.getSplittingRateData();

      //todo: make better but currently we must take extra measure for conjugate vertices on PASs as these are not
      // directly compatible with network loading. In those cases we find the relevant node for each turn and track it
      // eventually this should be delegated and done via an overridden method but for now this should work
      Consumer<? super DirectedVertex> lambda = (v) -> {
        DirectedVertex candidateVertex = v;
        if(v instanceof ConjugateDirectedVertex && ((ConjugateDirectedVertex)v).hasOriginalEdgeSegment()){
          if(newPas.getMergeVertex() == v){
            // last conjugate vertex we ignore, because downstream original edge is beyond point of interest for tracking
            return;
          }
          candidateVertex = ((ConjugateDirectedVertex)v).getOriginalEdgeSegment().getDownstreamVertex();
        }

        if(!pointQueueBasicSplittingRates.isTracked(candidateVertex))
          pointQueueBasicSplittingRates.registerTrackedNode(candidateVertex);
      };

      boolean lowCostSegment = true;
      newPas.forEachVertex(lowCostSegment, lambda);
      lowCostSegment = false;
      newPas.forEachVertex(lowCostSegment, lambda);
    }
    
  }

  /**
   * Conduct a loading update based on the provided consumer functionality
   *
   * @param bushFlowUpdateConsumer to use
   */
  public void executeNetworkLoadingUpdate(final BushFlowUpdateConsumer<B> bushFlowUpdateConsumer) {
    for (var bush : bushes) {
      if (bush != null) {
        bushFlowUpdateConsumer.accept(bush);
      }
    }
  }

}

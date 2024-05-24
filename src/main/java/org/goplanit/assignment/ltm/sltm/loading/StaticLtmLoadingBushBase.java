package org.goplanit.assignment.ltm.sltm.loading;

import java.util.logging.Logger;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.assignment.ltm.sltm.Bush;
import org.goplanit.assignment.ltm.sltm.Pas;
import org.goplanit.assignment.ltm.sltm.PasManager;
import org.goplanit.assignment.ltm.sltm.StaticLtmSettings;
import org.goplanit.assignment.ltm.sltm.consumer.BushFlowUpdateConsumer;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.physical.Movement;

/**
 * The bush based network loading scheme for sLTM - base class
 * 
 * @author markr
 *
 */
public abstract class StaticLtmLoadingBushBase<B extends Bush> extends StaticLtmNetworkLoading {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(StaticLtmLoadingBushBase.class.getCanonicalName());

  /** the bushes managed by the bush strategy but provided to be able to conduct a network loading based on the current state (bush splitting rates) of each bush */
  private B[] bushes;

  /**
   * the PAS manager with all the currently active PASs, used to determine which nodes to track flows and splitting rates for during network loading, namely all links and nodes
   * present in the active PASs
   */
  private PasManager pasManager;

  /**
   * Conduct a loading update based on the provided consumer functionality
   * 
   * @param bushFlowUpdateConsumer to use
   */
  private void executeNetworkLoadingUpdate(final BushFlowUpdateConsumer<B> bushFlowUpdateConsumer) {
    for (var bush : bushes) {
      if (bush != null) {
        bushFlowUpdateConsumer.accept(bush);
      }
    }
  }

  /**
   * TODO: Create factory class for this
   * <p>
   * Factory method to create the right flow update consumer to use when conducting a bush based flow update. We either create one that updates turn accepted flows (and possibly
   * also sending flows), or one that only updates (network wide) link sending flows and/or link outflows. The latter is to be used for initialisation/finalisation purposes only.
   * The former is the one used during the iterative loading procedure.
   * 
   * @param updateTurnAcceptedFlows flag indicating if the turn accepted flows are to be updated by this consumer
   * @param updateSendingFlows      flag indicating if the link sending flow are to be updated by this consumer
   * @param updateOutflows           flag indicating if the link outflows are to be updated by this consumer
   * @return created flow update consumer
   */
  protected abstract BushFlowUpdateConsumer<B> createBushFlowUpdateConsumer(boolean updateTurnAcceptedFlows, boolean updateSendingFlows, boolean updateOutflows);

  //@formatter:off
  /**
   * Conduct a network loading to compute updated turn inflow rates u_ab: Eq. (3)-(4) in paper. We only consider turns on nodes that are potentially blocking to reduce
   * computational overhead.
   *
   * @param mode                    unused
   * @return acceptedTurnFlows (on potentially blocking nodes) where key comprises a combined hash of entry and exit edge segment ids and value is the accepted turn flow v_ab
   */
  @Override
  protected MultiKeyMap<Object, Double> networkLoadingTurnFlowUpdate(Mode mode) {
   
    /* update network turn flows (and sending flows if POINT_QUEUE_BASIC) by performing a network loading
     * on all bushes using the bush-splitting rates (and updating the bush turn sending flows in the process, so they remain consistent
     * with the loading)
     */
    boolean updateTurnAcceptedFlows = true;
    boolean updateSendingFlowDuringLoading = !isIterativeSendingFlowUpdateActivated();
    boolean updateOutflows = false;
    var bushTurnFlowUpdateConsumer = createBushFlowUpdateConsumer(updateTurnAcceptedFlows, updateSendingFlowDuringLoading, updateOutflows);    
    
    /* execute */
    executeNetworkLoadingUpdate(bushTurnFlowUpdateConsumer);

    /* result */
    return bushTurnFlowUpdateConsumer.getAcceptedTurnFlows();
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  protected void networkLoadingLinkSegmentSendingFlowUpdate(Mode mode) {
        
    /* configure to only update all link segment sending flows */
    boolean updateTurnAcceptedFlows = false;
    boolean updateSendingFlowDuringLoading = true;
    boolean updateOutflows = false;
    var bushFlowUpdateConsumer =
        createBushFlowUpdateConsumer(updateTurnAcceptedFlows, updateSendingFlowDuringLoading, updateOutflows);
    
    /* execute */
    executeNetworkLoadingUpdate(bushFlowUpdateConsumer);
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  protected void networkLoadingLinkSegmentSendingflowOutflowUpdate(Mode mode) {
        
    /* configure to only update all link segment sending flows */
    boolean updateTurnAcceptedFlows = false;
    boolean updateSendingFlow = true;
    boolean updateOutflowFlow= true;
    var bushFlowUpdateConsumer = createBushFlowUpdateConsumer(updateTurnAcceptedFlows, updateSendingFlow, updateOutflowFlow);
    
    /* execute */
    executeNetworkLoadingUpdate(bushFlowUpdateConsumer);    
  }   

  /**
   * Initialise tracking of splitting rates and network flows on all nodes that are used by any currently
   * active PAS. This way we are able to ascertain how much total network flow runs through each PAS which in turn
   * is used to determine how much flow we can shift between segments.
   */
  @Override
  protected void activateEligibleSplittingRateTrackedNodes() {
    this.pasManager.forEachPas( (pas) -> activateNodeTrackingFor(pas));
  }

  /**
   * Constructor
   * 
   * @param idToken      to use
   * @param assignmentId to use
   * @param settings to use
   */
  public StaticLtmLoadingBushBase(IdGroupingToken idToken, long assignmentId, final StaticLtmSettings settings) {
    super(idToken, assignmentId, settings);
  }
  
  /** The bushes to use when a loading update is requested
   * 
   * @param bushes to use
   */
  public void setBushes(final B[] bushes) {
    this.bushes = bushes;    
  }
  
  /** The PasManager to use when we must initialise the tracked network nodes (namely all nodes
   * that are part of a PAS, since we need to know the network flow that passes through them)
   * 
   * @param pasManager to use
   */
  public void setPasManager(final PasManager pasManager) {
    this.pasManager = pasManager;
  } 

  /** For each PAS we must be able to determine the network level flows along the segments, see computeSubPathSendingFlow(). 
   * This requires knowing the network level splitting rates on the network level as well as the sending flows and acceptance factors, otherwise we cannot determine this. 
   * Therefore, for each newly identified PAS we activate node tracking for all (eligible) nodes along the segments of this PAS, if not already done so 
   *
   *@param newPas to activate nodes on segments for
   */
  public void activateNodeTrackingFor(final Pas newPas) {
    if(newPas==null) {
      LOGGER.severe("Provided PAS is null, unable to activate node tracking for alternative segments");
      return;
    }
    /* only when not all turn flows are tracked, we must expand the tracked nodes, otherwise they are already available */
    if(!isTrackAllNodeTurnFlows()) {
      var pointQueueBasicSplittingRates = (SplittingRateDataPartial) this.getSplittingRateData();
      boolean lowCostSegment = true;
      newPas.forEachVertex(lowCostSegment, (v) -> {
        if(!pointQueueBasicSplittingRates.isTracked(v)) 
          pointQueueBasicSplittingRates.registerTrackedNode(v); 
        });
      lowCostSegment = false;
      newPas.forEachVertex(lowCostSegment, (v) -> {
        if(!pointQueueBasicSplittingRates.isTracked(v)) 
          pointQueueBasicSplittingRates.registerTrackedNode(v); 
        });      
    }
    
  }

}

package org.goplanit.assignment.ltm.sltm.loading;

import java.util.logging.Logger;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.assignment.ltm.sltm.Bush;
import org.goplanit.assignment.ltm.sltm.Pas;
import org.goplanit.assignment.ltm.sltm.PasManager;
import org.goplanit.assignment.ltm.sltm.StaticLtmSettings;
import org.goplanit.assignment.ltm.sltm.consumer.BushFlowUpdateConsumer;
import org.goplanit.assignment.ltm.sltm.consumer.BushTurnFlowUpdateConsumer;
import org.goplanit.assignment.ltm.sltm.consumer.NetworkFlowUpdateData;
import org.goplanit.assignment.ltm.sltm.consumer.NetworkTurnFlowUpdateData;
import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;

/**
 * The bush based network loading scheme for sLTM
 * 
 * @author markr
 *
 */
public class StaticLtmLoadingBush extends StaticLtmNetworkLoading {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(StaticLtmLoadingBush.class.getCanonicalName());

  /** the bushes managed by the bush strategy but provided to be able to conduct a network loading based on the current state (bush splitting rates) of each bush */
  private Bush[] originBushes;

  /**
   * the PAS manager with all the currently active PASs, used to determine which nodes to track flows and splitting rates for during network loading, namely all links and nodes
   * present in the active PASs
   */
  private PasManager pasManager;

  /**
   * Factory method to create the right flow update consumer to use when conducting a bush based flow update. We either create one that updates turn accepted flows (and possibly
   * also sending flows), or one that only updates link sending flows. The latter is to be used for initialisation purposes only where the former is the one used during the
   * iterative loading procedure.
   * 
   * @param updateturnAcceptedFlows flag indicating if the turn accepted flows are to be updated by this consumer
   * @param updateLinkSendingFlows  flag indicating if the link sending flow are to be updated by this consumer
   * @return created flow update consumer
   */
  private BushFlowUpdateConsumer<?> createBushFlowUpdateconsumer(boolean updateTurnAcceptedFlows, boolean updateLinkSendingFlows) {

    if (updateTurnAcceptedFlows) {
      NetworkTurnFlowUpdateData dataConfig = null;
      if (updateLinkSendingFlows) {
        sendingFlowData.reset();
        dataConfig = new NetworkTurnFlowUpdateData(isTrackAllNodeTurnFlows(), sendingFlowData, splittingRateData, networkLoadingFactorData);
      } else {
        dataConfig = new NetworkTurnFlowUpdateData(isTrackAllNodeTurnFlows(), splittingRateData, networkLoadingFactorData);
      }
      return new BushTurnFlowUpdateConsumer(dataConfig);
    } else {
      if (!updateLinkSendingFlows) {
        LOGGER.warning("network flow updates using bushes must either updating link sending flows or turn accepted flows, neither are selected");
        return null;
      }
      return new BushFlowUpdateConsumer<NetworkFlowUpdateData>(new NetworkFlowUpdateData(sendingFlowData, networkLoadingFactorData));
    }
  }

  /**
   * Conduct a loading update based on the provided consumer functionality
   * 
   * @param bushFlowUpdateConsumer to use
   */
  private void executeNetworkLoadingUpdate(final BushFlowUpdateConsumer<?> bushFlowUpdateConsumer) {
    Bush originBush = null;
    for (int index = 0; index < originBushes.length; ++index) {
      originBush = originBushes[index];
      if (originBush != null) {
        bushFlowUpdateConsumer.accept(originBush);
      }
    }
  }

  //@formatter:off
  /**
   * Conduct a network loading to compute updated turn inflow rates u_ab: Eq. (3)-(4) in paper. We only consider turns on nodes that are potentially blocking to reduce
   * computational overhead.
   * 
   * @return acceptedTurnFlows (on potentially blocking nodes) where key comprises a combined hash of entry and exit edge segment ids and value is the accepted turn flow v_ab
   */
  @Override
  protected MultiKeyMap<Object, Double> networkLoadingTurnFlowUpdate() {
   
    /* update network turn flows (and sending flows if POINT_QUEUE_BASIC) by performing a network loading
     * on all bushes using the bush-splitting rates (and updating the bush turn sending flows in the process so they remain consistent
     * with the loading)
     */
    boolean updateTurnAcceptedFlows = true;
    boolean updateSendingFlowDuringLoading = !isIterativeSendingFlowUpdateActivated();     
    BushTurnFlowUpdateConsumer bushTurnFlowUpdateConsumer = (BushTurnFlowUpdateConsumer) createBushFlowUpdateconsumer(updateTurnAcceptedFlows, updateSendingFlowDuringLoading);    
    
    /* execute */
    executeNetworkLoadingUpdate(bushTurnFlowUpdateConsumer);

    /* result */
    return bushTurnFlowUpdateConsumer.getAcceptedTurnFlows();
  }
  
  /**
   * Conduct a network loading to compute updated inflow rates (without tracking turn flows): Eq. (3)-(4) in paper
   * 
   * @param linkSegmentFlowArrayToFill the inflows (u_a) to update
   */
  @Override
  protected void networkLoadingLinkSegmentInflowUpdate(final double[] linkSegmentFlowArrayToFill) {
        
    /* configure to only update all link segment sending flows */
    boolean updateTurnAcceptedFlows = false;
    boolean updateSendingFlowDuringLoading = true;     
    BushFlowUpdateConsumer<?> bushFlowUpdateConsumer = createBushFlowUpdateconsumer(updateTurnAcceptedFlows, updateSendingFlowDuringLoading);
    
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
  public StaticLtmLoadingBush(IdGroupingToken idToken, long assignmentId, final StaticLtmSettings settings) {
    super(idToken, assignmentId, settings);
  }
  
  /**
   * Determine the sending flow between origin,destination vertex using the subpath given by the subPathArray in order from start to finish. We utilise the initial sending flow on
   * the first segment as the base flow which is then followed along the subpath through the network splitting rates up to the final link segment
   * 
   * @param startVertex  to use
   * @param endVertex    to use
   * @param subPathArray to extract path from
   * @return sendingFlowPcuH between start and end vertex following the sub-path
   */
  public double computeSubPathSendingFlow(final DirectedVertex startVertex, final DirectedVertex endVertex, final EdgeSegment[] subPathArray) {

    int index = 0;
    EdgeSegment currEdgeSegment = subPathArray[index++];
    double subPathSendingFlow = getCurrentInflowsPcuH()[(int)currEdgeSegment.getId()];

    EdgeSegment nextEdgeSegment = currEdgeSegment;
    while (index < subPathArray.length && Precision.isPositive(subPathSendingFlow)) {
      currEdgeSegment = nextEdgeSegment;
      nextEdgeSegment = subPathArray[index++];
      subPathSendingFlow *= this.splittingRateData.getSplittingRate(currEdgeSegment, nextEdgeSegment);
    }

    return subPathSendingFlow;
  }

  /** The bushes to use when a loading update is requested
   * 
   * @param originBushes to use
   */
  public void setBushes(final Bush[] originBushes) {
    this.originBushes = originBushes;    
  }
  
  /** The PasManager to use when we must initialise the tracked network nodes (namely all nodes
   * that are part of a PAS, since we need to know the network flow that passes through them)
   * 
   * @param pasManager to use
   */
  public void setPasManager(final PasManager pasManager) {
    this.pasManager = pasManager;
  } 

  /** For each PAS we must be able to determine the network level flows along the segments, see {@link #computeSubPathSendingFlow(DirectedVertex, DirectedVertex, EdgeSegment[])}. 
   * This requires knowing the network level splitting rates on the network level as well as the sending flows and acceptance factors, otherwise we cannot determine this. 
   * Therefore, for each newly identified PAS we activate node tracking for all (eligible) nodes along the segments of this PAS, if not already done so 
   *
   *@param newPas to activate nodes on segments for
   */
  public void activateNodeTrackingFor(final Pas newPas) {
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

package org.goplanit.assignment.ltm.sltm.loading;

import java.util.logging.Logger;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.assignment.ltm.sltm.StaticLtmSettings;
import org.goplanit.assignment.ltm.sltm.consumer.NetworkFlowUpdateData;
import org.goplanit.assignment.ltm.sltm.consumer.NetworkTurnFlowUpdateData;
import org.goplanit.assignment.ltm.sltm.consumer.PathFlowUpdateConsumer;
import org.goplanit.assignment.ltm.sltm.consumer.PathLinkFlowUpdateConsumer;
import org.goplanit.assignment.ltm.sltm.consumer.PathTurnFlowUpdateConsumer;
import org.goplanit.od.path.OdPaths;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * The path based network loading scheme for sLTM
 * 
 * @author markr
 *
 */
public class StaticLtmLoadingPath extends StaticLtmNetworkLoading {

  /** logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(StaticLtmLoadingPath.class.getCanonicalName());

  /**
   * Od Paths to use
   */
  private OdPaths odPaths;

  //@formatter:off

  /** Factory method to create the right flow update consumer to use when conducting a path based flow update. We either create one that updates
   * turn accepted flows (and possibly also sending flows), or one that only updates link sending flows. The latter is to be used for initialisation purposes only where
   * the former is the one used during the iterative loading procedure.
   * 
   * @param updateTurnAcceptedFlows flag indicating if the turn accepted flows are to be updated by this consumer
   * @param updateSendingFlows flag indicating if the link sending flow are to be updated by this consumer
   * @param updateOutflows           flag indicating if the link outflows are to be updated by this consumer
   * @return created flow update consumer
   */
  private PathFlowUpdateConsumer<?> createPathFlowUpdateConsumer(boolean updateTurnAcceptedFlows, boolean updateSendingFlows, boolean updateOutflows) {
    if (!updateSendingFlows && !updateTurnAcceptedFlows) {
      LOGGER.warning("Network flow updates using paths must either updating link sending flows or turn accepted flows, neither are selected");
      return null;
    }
    
    if (updateSendingFlows) {
      sendingFlowData.reset();
    }
    if (updateOutflows) {
      this.inFlowOutflowData.resetOutflows();
    }    
    
    /* link update only */
    if (!updateTurnAcceptedFlows) {
      NetworkFlowUpdateData dataConfig = null;
      if (updateOutflows) {
        /* sending + outflow update only */
        dataConfig = new NetworkFlowUpdateData(sendingFlowData, inFlowOutflowData, networkLoadingFactorData);
      } else {
        /* sending flow update only */
        dataConfig = new NetworkFlowUpdateData(sendingFlowData, networkLoadingFactorData); 
      }
      return new PathLinkFlowUpdateConsumer(dataConfig, odPaths);
    }    
        
    /* turns + optional links update */
    if(updateTurnAcceptedFlows) {
      NetworkTurnFlowUpdateData dataConfig = null;
      if (updateSendingFlows) {           
        if (updateOutflows) {
          LOGGER.warning("Network flow updates using paths cannot update turn accepted flows and outflows, this is not yet supported");
          return null;
        } else {        
          dataConfig = new NetworkTurnFlowUpdateData(isTrackAllNodeTurnFlows(), sendingFlowData, splittingRateData, networkLoadingFactorData);
        }
      }else {
        dataConfig = new NetworkTurnFlowUpdateData(isTrackAllNodeTurnFlows(), splittingRateData, networkLoadingFactorData);
      }
      return new PathTurnFlowUpdateConsumer(dataConfig, odPaths);
    }

    LOGGER.warning("Invalid network flow update requested for path based loading");
    return null;    
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected MultiKeyMap<Object, Double> networkLoadingTurnFlowUpdate() {
    
    /* when one-shot sending flow update in step-2 of the algorithm is active, the sending flows are to be updated during the update here, 
     * otherwise not. In the latter case it is taken care of by step-2 in the solution algorithm via the iterative procedure */
    boolean updateTurnAcceptedFlows = true;
    boolean updateSendingFlows = !isIterativeSendingFlowUpdateActivated();
    boolean updateOutflows = false;
    PathTurnFlowUpdateConsumer pathTurnFlowUpdateConsumer = (PathTurnFlowUpdateConsumer) createPathFlowUpdateConsumer(updateTurnAcceptedFlows, updateSendingFlows, updateOutflows);
    
    /* execute */
    getOdDemands().forEachNonZeroOdDemand(getTransportNetwork().getZoning().odZones, pathTurnFlowUpdateConsumer);
    return pathTurnFlowUpdateConsumer.getAcceptedTurnFlows();
  }
  
  /**
   * {@inheritDoc}
   */ 
  @Override
  protected void networkLoadingLinkSegmentSendingFlowUpdate() {
    /* only update link sending flows */
    boolean updateTurnAcceptedFlows = false, updateOutflows = false;
    boolean updateSendingFlows = true;   
    var pathLinkFlowUpdateConsumer = (PathLinkFlowUpdateConsumer) createPathFlowUpdateConsumer(updateTurnAcceptedFlows, updateSendingFlows, updateOutflows); 
    
    /* execute */
    getOdDemands().forEachNonZeroOdDemand(getTransportNetwork().getZoning().odZones, pathLinkFlowUpdateConsumer);
  }

  /**
   * {@inheritDoc}
   */ 
  @Override
  protected void networkLoadingLinkSegmentSendingflowOutflowUpdate() {
    /* update link sending flows and outflows */
    boolean updateTurnAcceptedFlows = false;
    boolean updateSendingFlows = true, updateOutflows = true;
    var pathLinkFlowUpdateConsumer = (PathLinkFlowUpdateConsumer) createPathFlowUpdateConsumer(updateTurnAcceptedFlows, updateSendingFlows, updateOutflows); 
    
    /* execute */
    getOdDemands().forEachNonZeroOdDemand(getTransportNetwork().getZoning().odZones, pathLinkFlowUpdateConsumer);   
  }  

  /** In a path based implementation, tracked nodes overlap with potentially blocking nodes. Since potentially blocking nodes
   * are identified by the base class, there is no need for additional work in this implementation. Empty implementation
   */
  @Override
  protected void activateEligibleSplittingRateTrackedNodes() {
    // do nothing
  }

  /**
   * constructor
   * 
   * @param idToken      to use
   * @param assignmentId to use
   * @param settings to use
   */
  public StaticLtmLoadingPath(IdGroupingToken idToken, long assignmentId, final StaticLtmSettings settings) {
    super(idToken, assignmentId, settings);
    this.odPaths = null;
  }

  /** Update the od paths to use in the loading
   * 
   * @param odPaths to use
   */
  public void updateOdPaths(final OdPaths odPaths) {
    this.odPaths = odPaths;
  }




}

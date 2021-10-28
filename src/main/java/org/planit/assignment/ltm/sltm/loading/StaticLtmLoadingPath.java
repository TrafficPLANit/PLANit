package org.planit.assignment.ltm.sltm.loading;

import java.util.Map;
import java.util.logging.Logger;

import org.planit.assignment.ltm.sltm.StaticLtmSettings;
import org.planit.assignment.ltm.sltm.consumer.NetworkFlowUpdateData;
import org.planit.assignment.ltm.sltm.consumer.NetworkTurnFlowUpdateData;
import org.planit.assignment.ltm.sltm.consumer.PathFlowUpdateConsumer;
import org.planit.assignment.ltm.sltm.consumer.PathLinkSendingFlowUpdateConsumer;
import org.planit.assignment.ltm.sltm.consumer.PathTurnFlowUpdateConsumer;
import org.planit.od.path.OdPaths;
import org.planit.utils.id.IdGroupingToken;

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
   * @param updateturnAcceptedFlows flag indicating if the turn accepted flows are to be updated by this consumer
   * @param updateLinkSendingFlows flag indicating if the link sending flow are to be updated by this consumer
   * @return created flow update consumer
   */
  private PathFlowUpdateConsumer<?> createPathFlowUpdateconsumer(boolean updateTurnAcceptedFlows, boolean updateLinkSendingFlows) {
    
    if(updateTurnAcceptedFlows) {
      if (updateLinkSendingFlows) {
        sendingFlowData.reset();                
        return new PathTurnFlowUpdateConsumer(
            new NetworkTurnFlowUpdateData(isTrackAllNodeTurnFlows(), sendingFlowData, splittingRateData, networkLoadingFactorData),
            odPaths);
      }else {
        return new PathTurnFlowUpdateConsumer(
            new NetworkTurnFlowUpdateData(isTrackAllNodeTurnFlows(), splittingRateData, networkLoadingFactorData), 
            odPaths);
      }
    }else {
      if(!updateLinkSendingFlows) {
        return null;
      }else {
        return new PathLinkSendingFlowUpdateConsumer(
            new NetworkFlowUpdateData(sendingFlowData, networkLoadingFactorData), 
            odPaths);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Map<Integer, Double> networkLoadingTurnFlowUpdate() {
    
    /* when one-shot sending flow update in step-2 of the algorithm is active, the sending flows are to be updated during the update here, 
     * otherwise not. In the latter case it is taken care of by step-2 in the solution algorithm via the iterative procedure */
    boolean updateTurnAcceptedFlows = true;
    boolean updateSendingFlows = !isIterativeSendingFlowUpdateActivated();    
    PathTurnFlowUpdateConsumer pathTurnFlowUpdateConsumer = (PathTurnFlowUpdateConsumer) createPathFlowUpdateconsumer(updateTurnAcceptedFlows, updateSendingFlows);
    
    /* execute */
    getOdDemands().forEachNonZeroOdDemand(getTransportNetwork().getZoning().odZones, pathTurnFlowUpdateConsumer);
    return pathTurnFlowUpdateConsumer.getAcceptedTurnFlows();
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  protected void networkLoadingLinkSegmentInflowUpdate(final double[] linkSegmentFlowArrayToFill) {

    /* only update link sending flows */
    boolean updateTurnAcceptedFlows = false;
    boolean updateSendingFlows = true;    
    PathLinkSendingFlowUpdateConsumer pathLinkFlowUpdateConsumer = (PathLinkSendingFlowUpdateConsumer) createPathFlowUpdateconsumer(updateTurnAcceptedFlows, updateSendingFlows); 
    
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

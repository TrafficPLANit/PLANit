package org.goplanit.assignment.ltm.sltm.loading;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.assignment.ltm.sltm.util.StaticLtmDirectedPath;
import org.goplanit.assignment.ltm.sltm.input.StaticLtmSettings;
import org.goplanit.assignment.ltm.sltm.consumer.*;
import org.goplanit.utils.network.layer.physical.CompiledRelationIndex;
import org.goplanit.utils.network.layer.physical.CompiledRelationMapping;
import org.goplanit.zoning.od.path.OdMultiPaths;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.physical.Movement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

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

  /** Od Paths registered by mode */
  private final Map<Mode, OdMultiPaths<StaticLtmDirectedPath, ? extends List<StaticLtmDirectedPath>>> odMultiPathsByMode;

  private final CompiledRelationIndex compiledMovementIds;

  //@formatter:off

  /** Factory method to create the right flow update consumer to use when conducting a path based flow update.
   * We either create one that updates turn accepted flows (and possibly also sending flows), or one that only
   * updates link sending flows. The latter is to be used for initialisation purposes only where
   * the former is the one used during the iterative loading procedure.
   *
   * @param mode                      to use
   * @param updateTurnAcceptedFlows   flag indicating if the turn accepted flows are to be updated by this consumer
   * @param updateSendingFlows        flag indicating if the link sending flow are to be updated by this consumer
   * @param updateOutflows            flag indicating if the link outflows are to be updated by this consumer
   * @param updateUnconstrainedFlows  flag indicating if the link unconstrained flows are to be updated by this consumer
   * @return created flow update consumer
   */
  private PathFlowUpdateConsumer<?> createPathFlowUpdateConsumer(
          Mode mode,
          boolean updateTurnAcceptedFlows,
          boolean updateSendingFlows,
          boolean updateOutflows,
          boolean updateUnconstrainedFlows) {
    if (!updateSendingFlows && !updateTurnAcceptedFlows) {
      LOGGER.warning("Network flow updates using paths must either updating link sending flows or turn " +
          "accepted flows, neither are selected");
      return null;
    }
    
    if (updateSendingFlows) {
      nlSendingFlowData.reset();
    }
    if (updateOutflows) {
      this.nlInFlowOutflowData.resetOutflows();
    }
    if(updateUnconstrainedFlows){
      this.unconstrainedFlowData.reset();
    }

    //todo: spaghetti --> replace by one part checking on validity and then second part having a factory method that maps
    // the flags to the data 1:1 and then creates the data object

    /* link update only */
    if (!updateTurnAcceptedFlows) {
      NetworkFlowUpdateData dataConfig = null;
      if (updateOutflows) {
        /* sending + outflow update only (potentially unconstrained flows as well) */
        dataConfig = updateUnconstrainedFlows ?
                new NetworkFlowUpdateData(
                        nlSendingFlowData, nlInFlowOutflowData, networkLoadingFactorData, unconstrainedFlowData):
                new NetworkFlowUpdateData(nlSendingFlowData, nlInFlowOutflowData, networkLoadingFactorData);
      } else {
        /* sending flow update only (potentially unconstrained flows as well) */
        dataConfig =  updateUnconstrainedFlows ?
                new NetworkFlowUpdateData(nlSendingFlowData, networkLoadingFactorData, unconstrainedFlowData):
                new NetworkFlowUpdateData(nlSendingFlowData, networkLoadingFactorData);
      }

      return new PathLinkFlowUpdateConsumer(dataConfig, odMultiPathsByMode.get(mode));
    }    
        
    /* turns + optional links update */
    if(updateTurnAcceptedFlows) {
      long numPermissableMovements = compiledMovementIds.size();
      NetworkTurnFlowUpdateData dataConfig = null;

      if (updateUnconstrainedFlows) {
        LOGGER.warning("Network flow updates using paths cannot update turn accepted flows and unconstrained flows, " +
                "this is not yet supported when creating the NetworkTurnFlowUpdateData class (functionally not " +
            "an issue though)");
        return null;
      }

      if (updateSendingFlows) {
        if (updateOutflows) {
          LOGGER.warning("Network flow updates using paths cannot update turn accepted flows and outflows, this " +
              "is not yet supported");
          return null;
        } else {        
          dataConfig = new NetworkTurnFlowUpdateData(
                  isTrackAllNodeTurnFlowsDuringLoading(),
                  nlSendingFlowData,
                  nlSplittingRateData,
                  networkLoadingFactorData,
                  numPermissableMovements);
        }
      }else {
        dataConfig = new NetworkTurnFlowUpdateData(
            isTrackAllNodeTurnFlowsDuringLoading(),
            nlSplittingRateData,
            networkLoadingFactorData,
            numPermissableMovements);
      }
      return new PathTurnFlowUpdateConsumer(dataConfig, odMultiPathsByMode.get(mode), compiledMovementIds);
    }

    LOGGER.warning("Invalid network flow update requested for path based loading");
    return null;    
  }

  private PathFlowUpdateConsumer<?> createSyncAllNetworkFlowUpdateConsumer(Mode theMode){
    nlSendingFlowData.reset();
    nlInFlowOutflowData.resetInflows();
    nlInFlowOutflowData.resetOutflows();
    unconstrainedFlowData.reset();

    return new PathLinkFlowUpdateConsumer(
        new NetworkFlowUpdateData(
            nlSendingFlowData,
            networkLoadingFactorData.getCurrentFlowAcceptanceFactors(),
            nlInFlowOutflowData.getInflows(),
            nlInFlowOutflowData.getOutflows(),
            unconstrainedFlowData),
        odMultiPathsByMode.get(theMode));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected TurnFlowAccessor networkLoadingTurnFlowUpdate(Mode mode) {
    
    /* when one-shot sending flow update in step-2 of the algorithm is active, the sending flows are to be updated
    during the update here, otherwise not. In the latter case it is taken care of by step-2 in the solution algorithm
    via the iterative procedure */
    boolean updateTurnAcceptedFlows = true;
    boolean updateSendingFlows = !isIterativeSendingFlowUpdateActivated();
    boolean updateOutflows = false, updateUnconstrainedFlows = false;
    PathTurnFlowUpdateConsumer pathTurnFlowUpdateConsumer =
            (PathTurnFlowUpdateConsumer) createPathFlowUpdateConsumer(
                    mode, updateTurnAcceptedFlows, updateSendingFlows, updateOutflows, updateUnconstrainedFlows);
    
    /* execute */
    getOdDemands(mode).forEachNonZeroOdDemand(
        getTransportNetwork().getZoning().getOdZones(), pathTurnFlowUpdateConsumer);
    return pathTurnFlowUpdateConsumer.getAcceptedTurnFlows();
  }
  
  /**
   * {@inheritDoc}
   */ 
  @Override
  protected void networkLoadingLinkSegmentSendingFlowUpdate(Mode mode, boolean updateUnconstrainedFlows) {
    /* only update link sending flows */
    boolean updateTurnAcceptedFlows = false, updateOutflows = false;
    boolean updateSendingFlows = true;   
    var pathLinkFlowUpdateConsumer = (PathLinkFlowUpdateConsumer)
            createPathFlowUpdateConsumer(
                    mode, updateTurnAcceptedFlows, updateSendingFlows, updateOutflows, updateUnconstrainedFlows);
    
    /* execute */
    getOdDemands(mode).forEachNonZeroOdDemand(
        getTransportNetwork().getZoning().getOdZones(), pathLinkFlowUpdateConsumer);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void networkLoadingSyncFlowsUpdate(Mode mode) {

    var syncFlowConsumer = createSyncAllNetworkFlowUpdateConsumer(mode);

    /* execute */
    getOdDemands(mode).forEachNonZeroOdDemand(
        getTransportNetwork().getZoning().getOdZones(), syncFlowConsumer);
  }

  /** In a path based implementation, tracked nodes overlap with potentially blocking nodes. Since potentially
   * blocking nodes are identified by the base class, there is no need for additional work in this implementation.
   * Empty implementation
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
   * @param compiledMovementIds to use
   * @param settings to use
   */
  public StaticLtmLoadingPath(
          IdGroupingToken idToken,
          long assignmentId,
          CompiledRelationIndex compiledMovementIds,
          final StaticLtmSettings settings) {
    super(idToken, assignmentId, settings);
    this.odMultiPathsByMode = new HashMap<>();
    this.compiledMovementIds = compiledMovementIds;
  }

  /** Set the od multi paths to use in the loading (by mode). Expected to be set before this class is used
   *
   * @param mode mode of the paths
   * @param odMultiPaths to use
   */
  public void setOdMultiPaths(
      final Mode mode, OdMultiPaths<StaticLtmDirectedPath, ? extends List<StaticLtmDirectedPath>> odMultiPaths) {
    this.odMultiPathsByMode.put(mode, odMultiPaths);
  }


  /** Access to the od multi paths to use in the loading (by mode).
   *
   * @param mode mode of the paths
   * @return odMultiPaths found
   */
  public OdMultiPaths<StaticLtmDirectedPath, ? extends List<StaticLtmDirectedPath>> getOdMultiPaths(final Mode mode) {
    return this.odMultiPathsByMode.get(mode);
  }

}

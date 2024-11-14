package org.goplanit.assignment.ltm.sltm.loading;

import java.util.logging.Logger;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.assignment.ltm.sltm.StaticLtmSettings;
import org.goplanit.assignment.ltm.sltm.conjugate.ConjugateDestinationBush;
import org.goplanit.assignment.ltm.sltm.consumer.*;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.physical.Movement;

/**
 * The conjugate rooted bush based network loading scheme for sLTM
 * 
 * @author markr
 *
 */
public class StaticLtmLoadingBushConjugate extends StaticLtmLoadingBushBase<ConjugateDestinationBush> {

  /** logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(StaticLtmLoadingBushConjugate.class.getCanonicalName());

  /**
   * {@inheritDoc}
   */
  @Override
  protected BushFlowUpdateConsumer<ConjugateDestinationBush> createBushFlowUpdateConsumer(
          boolean updateTurnAcceptedFlows,
          boolean updateSendingFlows,
          boolean updateOutflows,
          boolean updateUnconstrainedFlows) {
//
//    if (!updateSendingFlows && !updateTurnAcceptedFlows) {
//      LOGGER.warning("Network flow updates using bushes must either updating link sending flows or turn accepted " +
//              "flows, neither are selected");
//      return null;
//    }
//
//    if (updateSendingFlows) {
//      sendingFlowData.reset();
//    }
//    if (updateOutflows) {
//      this.inFlowOutflowData.resetOutflows();
//    }
//    if(updateUnconstrainedFlows){
//      this.unconstrainedFlowData.reset();
//    }
//
//    //todo: spaghetti --> replace by one part checking on validity and then second part having a factory method that maps
//    // the flags to the data 1:1 and then creates the data object
//
//    if (!updateTurnAcceptedFlows) {
//
//      /* link based only */
//      NetworkFlowUpdateData dataConfig;
//      if (updateOutflows) {
//        /* sending + outflow update only */
//        dataConfig = updateUnconstrainedFlows ?
//                new NetworkFlowUpdateData(sendingFlowData, inFlowOutflowData, networkLoadingFactorData, unconstrainedFlowData):
//                new NetworkFlowUpdateData(sendingFlowData, inFlowOutflowData, networkLoadingFactorData);
//      } else {
//        /* sending flow update only */
//        dataConfig = updateUnconstrainedFlows ?
//                new NetworkFlowUpdateData(sendingFlowData, networkLoadingFactorData, unconstrainedFlowData):
//                new NetworkFlowUpdateData(sendingFlowData, networkLoadingFactorData);
//      }
//      return new RootedBushFlowUpdateConsumerImpl<>(dataConfig, segmentPair2MovementMap);
//
//    }else{
//
//      if (updateUnconstrainedFlows) {
//        LOGGER.warning("Network flow updates using bushes cannot update turn accepted flows and unconstrained flows, " +
//                "this is not yet supported when creating the NetworkTurnFlowUpdateData class (functionally not an issue though)");
//        return null;
//      }
//
//      /* turn based + optional link based */
//      int numMovements = getTransportNetwork().getMovements().size();
//      NetworkTurnFlowUpdateData dataConfig = null;
//
//      if (updateSendingFlows) {
//        if (updateOutflows) {
//          LOGGER.warning("Network flow updates using bushes cannot update turn accepted flows and outflows, " +
//                  "this is not yet supported");
//          return null;
//        } else {
//          dataConfig = new NetworkTurnFlowUpdateData(
//                  isTrackAllNodeTurnFlowsDuringLoading(), sendingFlowData, splittingRateData, networkLoadingFactorData, numMovements);
//        }
//      } else if (updateOutflows) {
//        LOGGER.warning("Network flow updates using bushes must either updating link sending flows and outflows, " +
//                "or just turn accepted flows, neither are selected");
//        return null;
//      } else {
//        dataConfig = new NetworkTurnFlowUpdateData(
//                isTrackAllNodeTurnFlowsDuringLoading(), splittingRateData, networkLoadingFactorData, numMovements);
//      }
//      return new RootedBushTurnFlowUpdateConsumer(dataConfig, segmentPair2MovementMap);
//    }
    return null;
  }

  /**
   * Constructor
   * 
   * @param idToken      to use
   * @param assignmentId to use
   * @param segmentPair2MovementMap mapping from entry/exit segment (dual key) to movement, use to covert turn flows
   *  to splitting rate data format
   * @param settings     to use
   */
  public StaticLtmLoadingBushConjugate(IdGroupingToken idToken, long assignmentId, MultiKeyMap<Object, Movement> segmentPair2MovementMap, final StaticLtmSettings settings) {
    super(idToken, assignmentId, segmentPair2MovementMap, settings);
  }

}

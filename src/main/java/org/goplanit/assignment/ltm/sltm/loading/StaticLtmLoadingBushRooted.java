package org.goplanit.assignment.ltm.sltm.loading;

import java.util.logging.Logger;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.assignment.ltm.sltm.RootedLabelledBush;
import org.goplanit.assignment.ltm.sltm.StaticLtmSettings;
import org.goplanit.assignment.ltm.sltm.consumer.BushFlowUpdateConsumer;
import org.goplanit.assignment.ltm.sltm.consumer.NetworkFlowUpdateData;
import org.goplanit.assignment.ltm.sltm.consumer.NetworkTurnFlowUpdateData;
import org.goplanit.assignment.ltm.sltm.consumer.RootedBushFlowUpdateConsumerImpl;
import org.goplanit.assignment.ltm.sltm.consumer.RootedBushTurnFlowUpdateConsumer;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.physical.Movement;

/**
 * The rooted bush based network loading scheme for sLTM
 * 
 * @author markr
 *
 */
public class StaticLtmLoadingBushRooted extends StaticLtmLoadingBushBase<RootedLabelledBush> {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(StaticLtmLoadingBushRooted.class.getCanonicalName());

  /**
   * {@inheritDoc}
   */
  @Override
  protected BushFlowUpdateConsumer<RootedLabelledBush> createBushFlowUpdateConsumer(
          boolean updateTurnAcceptedFlows, boolean updateSendingFlows, boolean updateOutflows) {
    if (!updateSendingFlows && !updateTurnAcceptedFlows) {
      LOGGER.warning("Network flow updates using bushes must either updating link sending flows or turn accepted flows, neither are selected");
      return null;
    }

    if (updateSendingFlows) {
      sendingFlowData.reset();
    }
    if (updateOutflows) {
      this.inFlowOutflowData.resetOutflows();
    }

    if (!updateTurnAcceptedFlows) {

      /* link based only */
      NetworkFlowUpdateData dataConfig = null;
      if (updateOutflows) {
        /* sending + outflow update only */
        dataConfig = new NetworkFlowUpdateData(sendingFlowData, inFlowOutflowData, networkLoadingFactorData);
      } else {
        /* sending flow update only */
        dataConfig = new NetworkFlowUpdateData(sendingFlowData, networkLoadingFactorData);
      }
      return new RootedBushFlowUpdateConsumerImpl<NetworkFlowUpdateData>(dataConfig, segmentPair2MovementMap);

    }else{

      /* turn based + optional link based */
      int numMovements = getTransportNetwork().getMovements().size();
      NetworkTurnFlowUpdateData dataConfig = null;

      if (updateSendingFlows) {
        if (updateOutflows) {
          LOGGER.warning("Network flow updates using bushes cannot update turn accepted flows and outflows, this is not yet supported");
          return null;
        } else {
          dataConfig = new NetworkTurnFlowUpdateData(
                  isTrackAllNodeTurnFlowsDuringLoading(), sendingFlowData, splittingRateData, networkLoadingFactorData, numMovements);
        }
      } else if (updateOutflows) {
        LOGGER.warning("Network flow updates using bushes must either updating link sending flows and otuflows, or just turn accepted flows, neither are selected");
        return null;
      } else {
        dataConfig = new NetworkTurnFlowUpdateData(isTrackAllNodeTurnFlowsDuringLoading(), splittingRateData, networkLoadingFactorData, numMovements);
      }
      return new RootedBushTurnFlowUpdateConsumer(dataConfig, segmentPair2MovementMap);
    }

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
  public StaticLtmLoadingBushRooted(
          IdGroupingToken idToken, long assignmentId, MultiKeyMap<Object, Movement> segmentPair2MovementMap, final StaticLtmSettings settings) {
    super(idToken, assignmentId, segmentPair2MovementMap, settings);
  }

}

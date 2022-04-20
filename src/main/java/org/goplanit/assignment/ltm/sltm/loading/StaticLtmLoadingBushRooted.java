package org.goplanit.assignment.ltm.sltm.loading;

import java.util.logging.Logger;

import org.goplanit.assignment.ltm.sltm.RootedBush;
import org.goplanit.assignment.ltm.sltm.StaticLtmSettings;
import org.goplanit.assignment.ltm.sltm.consumer.BushFlowUpdateConsumer;
import org.goplanit.assignment.ltm.sltm.consumer.BushTurnFlowUpdateConsumer;
import org.goplanit.assignment.ltm.sltm.consumer.NetworkFlowUpdateData;
import org.goplanit.assignment.ltm.sltm.consumer.NetworkTurnFlowUpdateData;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * The rooted bush based network loading scheme for sLTM
 * 
 * @author markr
 *
 */
public class StaticLtmLoadingBushRooted extends StaticLtmLoadingBushBase<RootedBush> {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(StaticLtmLoadingBushRooted.class.getCanonicalName());

  /**
   * {@inheritDoc}
   */
  @Override
  protected BushFlowUpdateConsumer<?> createBushFlowUpdateConsumer(boolean updateTurnAcceptedFlows, boolean updateSendingFlows, boolean updateOutflows) {
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

    /* link based only */
    if (!updateTurnAcceptedFlows) {
      NetworkFlowUpdateData dataConfig = null;
      if (updateOutflows) {
        /* sending + outflow update only */
        dataConfig = new NetworkFlowUpdateData(sendingFlowData, inFlowOutflowData, networkLoadingFactorData);
      } else {
        /* sending flow update only */
        dataConfig = new NetworkFlowUpdateData(sendingFlowData, networkLoadingFactorData);
      }
      return new BushFlowUpdateConsumer<NetworkFlowUpdateData>(dataConfig);
    }

    /* turn based + optional link based */
    if (updateTurnAcceptedFlows) {
      NetworkTurnFlowUpdateData dataConfig = null;
      if (updateSendingFlows) {
        if (updateOutflows) {
          LOGGER.warning("Network flow updates using bushes cannot update turn accepted flows and outflows, this is not yet supported");
          return null;
        } else {
          dataConfig = new NetworkTurnFlowUpdateData(isTrackAllNodeTurnFlows(), sendingFlowData, splittingRateData, networkLoadingFactorData);
        }
      } else if (updateOutflows) {
        LOGGER.warning("Network flow updates using bushes must either updating link sending flows and otuflows, or just turn accepted flows, neither are selected");
        return null;
      } else {
        dataConfig = new NetworkTurnFlowUpdateData(isTrackAllNodeTurnFlows(), splittingRateData, networkLoadingFactorData);
      }
      return new BushTurnFlowUpdateConsumer(dataConfig);
    }

    LOGGER.warning("Invalid network flow update requested for bush absed laoding");
    return null;
  }

  /**
   * Constructor
   * 
   * @param idToken      to use
   * @param assignmentId to use
   * @param settings     to use
   */
  public StaticLtmLoadingBushRooted(IdGroupingToken idToken, long assignmentId, final StaticLtmSettings settings) {
    super(idToken, assignmentId, settings);
  }

}

package org.goplanit.assignment.ltm.sltm.loading;

import java.util.logging.Logger;

import org.goplanit.assignment.ltm.sltm.StaticLtmSettings;
import org.goplanit.assignment.ltm.sltm.conjugate.ConjugateDestinationBush;
import org.goplanit.assignment.ltm.sltm.consumer.BushFlowUpdateConsumer;
import org.goplanit.utils.id.IdGroupingToken;

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
  protected BushFlowUpdateConsumer<ConjugateDestinationBush> createBushFlowUpdateConsumer(boolean updateTurnAcceptedFlows, boolean updateSendingFlows, boolean updateOutflows) {
    // TODO: base on rooted bush loading
//    if (!updateSendingFlows && !updateTurnAcceptedFlows) {
//      LOGGER.warning("Network flow updates using bushes must either updating link sending flows or turn accepted flows, neither are selected");
//      return null;
//    }
//
//    if (updateSendingFlows) {
//      sendingFlowData.reset();
//    }
//    if (updateOutflows) {
//      this.inFlowOutflowData.resetOutflows();
//    }
//
//    /* link based only */
//    if (!updateTurnAcceptedFlows) {
//      NetworkFlowUpdateData dataConfig = null;
//      if (updateOutflows) {
//        /* sending + outflow update only */
//        dataConfig = new NetworkFlowUpdateData(sendingFlowData, inFlowOutflowData, networkLoadingFactorData);
//      } else {
//        /* sending flow update only */
//        dataConfig = new NetworkFlowUpdateData(sendingFlowData, networkLoadingFactorData);
//      }
//      return new BushFlowUpdateConsumer<NetworkFlowUpdateData>(dataConfig);
//    }
//
//    /* turn based + optional link based */
//    if (updateTurnAcceptedFlows) {
//      NetworkTurnFlowUpdateData dataConfig = null;
//      if (updateSendingFlows) {
//        if (updateOutflows) {
//          LOGGER.warning("Network flow updates using bushes cannot update turn accepted flows and outflows, this is not yet supported");
//          return null;
//        } else {
//          dataConfig = new NetworkTurnFlowUpdateData(isTrackAllNodeTurnFlows(), sendingFlowData, splittingRateData, networkLoadingFactorData);
//        }
//      } else if (updateOutflows) {
//        LOGGER.warning("Network flow updates using bushes must either updating link sending flows and otuflows, or just turn accepted flows, neither are selected");
//        return null;
//      } else {
//        dataConfig = new NetworkTurnFlowUpdateData(isTrackAllNodeTurnFlows(), splittingRateData, networkLoadingFactorData);
//      }
//      return new BushTurnFlowUpdateConsumer(dataConfig);
//    }
//
//    LOGGER.warning("Invalid network flow update requested for bush absed laoding");
    return null;
  }

  /**
   * Constructor
   * 
   * @param idToken      to use
   * @param assignmentId to use
   * @param settings     to use
   */
  public StaticLtmLoadingBushConjugate(IdGroupingToken idToken, long assignmentId, final StaticLtmSettings settings) {
    super(idToken, assignmentId, settings);
  }

}

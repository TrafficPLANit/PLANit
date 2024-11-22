package org.goplanit.assignment.ltm.sltm.loading;

import java.util.logging.Logger;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.assignment.ltm.sltm.RootedLabelledBush;
import org.goplanit.assignment.ltm.sltm.StaticLtmSettings;
import org.goplanit.assignment.ltm.sltm.conjugate.ConjugateBushFlowUpdateConsumerImpl;
import org.goplanit.assignment.ltm.sltm.conjugate.ConjugateBushTurnFlowUpdateConsumer;
import org.goplanit.assignment.ltm.sltm.conjugate.ConjugateDestinationBush;
import org.goplanit.assignment.ltm.sltm.consumer.*;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
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
  protected ConjugateBushFlowUpdateConsumerImpl<NetworkFlowUpdateData> createBushLinkSendingFlowUpdateConsumer(
          boolean updateLinkOutflows, boolean updateUnconstrainedLinkFlows){
    return new ConjugateBushFlowUpdateConsumerImpl<>(
            createNetworkLinkFlowData(updateLinkOutflows, updateUnconstrainedLinkFlows));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected ConjugateBushTurnFlowUpdateConsumer createBushTurnFlowUpdateConsumer(
          boolean updateLinkSendingFlows) {
    /* turn based + optional link sending flow based */
    int numMovements = -1;
    throw new PlanItRunTimeException("number of turns should be based on conjugate network segments here, not movements, should be abstracted out from loading - todo");
//    return new ConjugateBushTurnFlowUpdateConsumer(
//            createNetworkTurnFlowData(updateLinkSendingFlows, numMovements));
  }

  /**
   * Constructor
   * 
   * @param idToken      to use
   * @param assignmentId to use
   * @param settings     to use
   */
  public StaticLtmLoadingBushConjugate(
          IdGroupingToken idToken,
          long assignmentId,
          final StaticLtmSettings settings) {
    super(idToken, assignmentId, null, settings);
    throw new PlanItRunTimeException("segmentPair2MovementMap embedded in loading, but should be abstracted out I think - TODO");
  }

}

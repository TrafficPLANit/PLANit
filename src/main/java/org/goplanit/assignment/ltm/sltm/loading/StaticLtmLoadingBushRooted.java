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

  final MultiKeyMap<Object, Movement> segmentPair2MovementMap;

  /**
   * {@inheritDoc}
   */
  @Override
  protected RootedBushFlowUpdateConsumerImpl<NetworkFlowUpdateData> createBushLinkSendingFlowUpdateConsumer(
          boolean updateLinkOutflows, boolean updateUnconstrainedLinkFlows){
    return new RootedBushFlowUpdateConsumerImpl<>(
            createNetworkLinkFlowData(updateLinkOutflows, updateUnconstrainedLinkFlows), segmentPair2MovementMap);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected RootedBushTurnFlowUpdateConsumer createBushTurnFlowUpdateConsumer(
          boolean updateLinkSendingFlows) {
    /* turn based + optional link sending flow based */
    int numMovements = getTransportNetwork().getMovements().size();
    return new RootedBushTurnFlowUpdateConsumer(
            createNetworkTurnFlowData(updateLinkSendingFlows, numMovements), segmentPair2MovementMap);
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
          IdGroupingToken idToken,
          long assignmentId,
          MultiKeyMap<Object, Movement> segmentPair2MovementMap,
          final StaticLtmSettings settings) {
    super(idToken, assignmentId,settings);
    this.segmentPair2MovementMap = segmentPair2MovementMap;
  }

}

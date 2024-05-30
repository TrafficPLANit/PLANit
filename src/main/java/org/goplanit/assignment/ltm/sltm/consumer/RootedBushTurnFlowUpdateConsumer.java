package org.goplanit.assignment.ltm.sltm.consumer;

import java.util.logging.Logger;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.assignment.ltm.sltm.BushFlowLabel;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.network.layer.physical.Movement;

/**
 * Consumer to apply during bush based network loading turn flow update for each non-zero demand bush
 * <p>
 * Depending on the applied solution scheme a slightly different approach is taken to this update where:
 * <p>
 * POINT QUEUE BASIC: Also update the network sending flow. Only during basic point queue solution scheme, sending flows are NOT locally updated in the sending flow update step.
 * Therefore, sending flows of most links are not updated during this sending flow update because it only updates the sending flows of outgoing links of potentially blocking nodes.
 * When an incoming link of any node is not also an outgoing link of another potentially blocking node its sending flow remains the same even if it actually changes due to further
 * upstream changes in restrictions. In this approach this is only identified when we make sure the sending flows are updated during (this) loading on the path level. Hence, we
 * must update sending flows here.
 * <p>
 * ANY OTHER SOLUTION APPROACH: Here we update all used nodes and sending flows are updated iteratively and locally propagated without the need of the loading in the sending flow
 * update. Therefore, there is no need to update the sending flows. On the other hand we now update the turn flows on all used nodes rather than only the potentially blocking ones.
 * 
 * @author markr
 *
 */
public class RootedBushTurnFlowUpdateConsumer extends RootedBushFlowUpdateConsumerImpl<NetworkTurnFlowUpdateData> {

  /** logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(RootedBushTurnFlowUpdateConsumer.class.getCanonicalName());

  /**
   * constructor
   *
   * @param dataConfig to use
   * @param segmentPair2MovementMap mapping from entry/exit segment (dual key) to movement, use to covert turn flows
   *  to splitting rate data format
   */
  public RootedBushTurnFlowUpdateConsumer(
          final NetworkTurnFlowUpdateData dataConfig, MultiKeyMap<Object,Movement> segmentPair2MovementMap) {
    super(dataConfig, segmentPair2MovementMap);
  }

  /**
   * Track the turn accepted flows when they are classified as being tracked during network loading, otherwise do nothing
   * 
   * @param movement          the movement (turn)
   * @param prevLabel            at hand
   * @param currLabel            at hand
   * @param turnAcceptedFlowPcuH to use
   */
  @Override
  protected void applyAcceptedTurnFlowUpdate(
          final Movement movement,
          final BushFlowLabel prevLabel,
          final BushFlowLabel currLabel,
          double turnAcceptedFlowPcuH) {
    if (dataConfig.trackAllNodeTurnFlows || dataConfig.splittingRateData.isTracked(movement.getCentreVertex())) {
      dataConfig.addToAcceptedTurnFlows(movement, turnAcceptedFlowPcuH); // network level
    }
  }

  /**
   * The found accepted turn flows by movement id
   * 
   * @return accepted turn flows
   */
  @Override
  public double[] getAcceptedTurnFlows() {
    return dataConfig.getAcceptedTurnFlows();
  }
}

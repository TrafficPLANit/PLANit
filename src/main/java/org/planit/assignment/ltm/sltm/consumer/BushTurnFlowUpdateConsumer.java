package org.planit.assignment.ltm.sltm.consumer;

import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.planit.assignment.ltm.sltm.Bush;
import org.planit.utils.graph.EdgeSegment;

/**
 * Consumer to apply during bush based turn flow update for each non-zero demand bush
 * <p>
 * Depending on the applied solution scheme a slightly different approach is taken to this update where:
 * <p>
 * POINT QUEUE BASIC: Also update the network sending flow. Only during basic point queue solution scheme, sending flows are NOT locally updated in the sending flow update step.
 * Therefore sending flows of most links are not updated during this sending flow update because it only updates the sending flows of outgoing links of potentially blocking nodes.
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
public class BushTurnFlowUpdateConsumer extends BushFlowUpdateConsumer<NetworkTurnFlowUpdateData> implements Consumer<Bush> {

  /** logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(BushTurnFlowUpdateConsumer.class.getCanonicalName());

  /**
   * constructor
   * 
   * @param dataConfig to use
   */
  public BushTurnFlowUpdateConsumer(final NetworkTurnFlowUpdateData dataConfig) {
    super(dataConfig);
  }

  /**
   * track the turn accepted flows when they are classified as being tracked, otherwise do nothing
   * 
   * @param prevSegmentId        to use
   * @param currentSegment       to use
   * @param turnAcceptedFlowPcuH to use
   */
  @Override
  protected void applyAcceptedTurnFlowUpdate(int prevSegmentId, EdgeSegment currentSegment, double turnAcceptedFlowPcuH) {
    if (dataConfig.trackAllNodeTurnFlows || dataConfig.splittingRateData.isTracked(currentSegment.getUpstreamVertex())) {
      dataConfig.addToAcceptedTurnFlows(NetworkTurnFlowUpdateData.createTurnHashCode(prevSegmentId, (int) currentSegment.getId()), turnAcceptedFlowPcuH); // network level
    }
  }

  /**
   * The found accepted turn flows by the combined entry-exit segment hash code
   * 
   * @return accepted turn flows
   */
  public Map<Integer, Double> getAcceptedTurnFlows() {
    return dataConfig.getAcceptedTurnFlows();
  }
}
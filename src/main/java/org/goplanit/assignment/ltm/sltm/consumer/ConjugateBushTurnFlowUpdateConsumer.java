package org.goplanit.assignment.ltm.sltm.consumer;

import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.assignment.ltm.sltm.conjugate.ConjugateDestinationBush;
import org.goplanit.assignment.ltm.sltm.loading.TurnFlowAccessorConjugateSegments;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;

/**
 * Consumer to apply during bush based network loading turn flow update for each non-zero demand bush
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
public class ConjugateBushTurnFlowUpdateConsumer
        extends ConjugateBushNetworkFlowUpdateConsumerImpl<NetworkTurnFlowUpdateData> {

  /** logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(ConjugateBushTurnFlowUpdateConsumer.class.getCanonicalName());

  /**
   * constructor
   *
   * @param dataConfig to use
   * @param turn2ConjSegmentMapping to use
   * @param edgeSegmentsToUpdate selective edge segments to update (all turn flows of all bushes of these edge segments)
   */
  public ConjugateBushTurnFlowUpdateConsumer(
      final NetworkTurnFlowUpdateData dataConfig,
      final MultiKeyMap<Object, ConjugateEdgeSegment> turn2ConjSegmentMapping,
      Set<EdgeSegment> edgeSegmentsToUpdate) {
    super(dataConfig, turn2ConjSegmentMapping, edgeSegmentsToUpdate);
  }

  /**
   * constructor
   * 
   * @param dataConfig to use
   * @param turn2ConjSegmentMapping to use
   */
  public ConjugateBushTurnFlowUpdateConsumer(final NetworkTurnFlowUpdateData dataConfig,
                                             final MultiKeyMap<Object, ConjugateEdgeSegment> turn2ConjSegmentMapping) {
    super(dataConfig, turn2ConjSegmentMapping);
  }

  /**
   * Track the turn accepted flows when they are classified as being tracked during network loading, otherwise do nothing
   * 
   * @param conjEdgeSegment      the turn ti use
   * @param turnAcceptedFlowPcuH to use
   */
  @Override
  protected void applyAcceptedTurnFlowUpdate(final ConjugateEdgeSegment conjEdgeSegment, double turnAcceptedFlowPcuH) {
    if (dataConfig.isTrackAllNodeTurnFlows() ||
            dataConfig.getNlSplittingRateData().isTracked(conjEdgeSegment.getOriginalCentreVertex())) {
      dataConfig.addToAcceptedTurnFlows((int)conjEdgeSegment.getId(), turnAcceptedFlowPcuH); // network level

//      if(conjEdgeSegment.getOriginalAdjacentEdgeSegments().second()!= null && conjEdgeSegment.getOriginalAdjacentEdgeSegments().second().getId()==41){
//        LOGGER.severe("41 Constrained ("+conjEdgeSegment.getXmlId()+"): add: " + turnAcceptedFlowPcuH );
//      }

    }
  }

  /**
   * The found accepted turn flows by the combined entry-exit segment hash code
   * 
   * @return accepted turn flows
   */
  @Override
  public TurnFlowAccessorConjugateSegments getAcceptedTurnFlows() {
    return TurnFlowAccessorConjugateSegments.of(this.turn2ConjSegmentMapping, dataConfig.getAcceptedTurnFlows());
  }
}

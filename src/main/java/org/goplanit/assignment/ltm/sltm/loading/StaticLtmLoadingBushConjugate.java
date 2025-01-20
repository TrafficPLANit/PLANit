package org.goplanit.assignment.ltm.sltm.loading;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.assignment.ltm.sltm.StaticLtmSettings;
import org.goplanit.assignment.ltm.sltm.consumer.ConjugateBushFlowUpdateConsumerImpl;
import org.goplanit.assignment.ltm.sltm.consumer.ConjugateBushTurnFlowUpdateConsumer;
import org.goplanit.assignment.ltm.sltm.conjugate.ConjugateDestinationBush;
import org.goplanit.assignment.ltm.sltm.consumer.NetworkFlowUpdateData;
import org.goplanit.network.transport.ConjugateTransportModelNetwork;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;

import java.util.logging.Logger;

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

  /** conjugate network to access bush based data structures */
  private final ConjugateTransportModelNetwork conjugateTransportModelNetwork;

  private final MultiKeyMap<Object, ConjugateEdgeSegment> turn2ConjugateSegmentMapping;

  /**
   * {@inheritDoc}
   */
  @Override
  protected ConjugateBushFlowUpdateConsumerImpl<NetworkFlowUpdateData> createBushLinkSendingFlowUpdateConsumer(
          boolean updateLinkOutflows, boolean updateUnconstrainedLinkFlows){
    return new ConjugateBushFlowUpdateConsumerImpl<>(
            createNetworkLinkFlowData(updateLinkOutflows, updateUnconstrainedLinkFlows), turn2ConjugateSegmentMapping);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected ConjugateBushTurnFlowUpdateConsumer createBushTurnFlowUpdateConsumer(
          boolean updateLinkSendingFlows) {
    /* original turn (so conjugate link segment) based + optional original link sending flow based (so conjugate node) */

    // we use conjugate segments because they also contain the dummy (connectoid) turns that have no entry segment
    int numConjugateSegments = conjugateTransportModelNetwork.getNumberOfEdgeSegmentsAllLayers();
    return new ConjugateBushTurnFlowUpdateConsumer(
            createNetworkTurnFlowData(updateLinkSendingFlows, numConjugateSegments), turn2ConjugateSegmentMapping);
  }

  /**
   * Constructor
   * 
   * @param idToken      to use
   * @param assignmentId to use
   * @param turn2ConjugateSegmentMapping to use
   * @param conjugateTransportModelNetwork to use
   * @param settings     to use
   */
  public StaticLtmLoadingBushConjugate(
          IdGroupingToken idToken,
          long assignmentId,
          MultiKeyMap<Object, ConjugateEdgeSegment> turn2ConjugateSegmentMapping,
          ConjugateTransportModelNetwork conjugateTransportModelNetwork,
          final StaticLtmSettings settings) {
    super(idToken, assignmentId, settings);
    this.conjugateTransportModelNetwork = conjugateTransportModelNetwork;
    this.turn2ConjugateSegmentMapping = turn2ConjugateSegmentMapping;
  }

  /** access to the conjugate transport model network
   * @return conjugate version of transport model network
   */
  public ConjugateTransportModelNetwork getConjugateTransportModelNetwork() {
    return conjugateTransportModelNetwork;
  }
}

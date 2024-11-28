package org.goplanit.assignment.ltm.sltm.loading;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.assignment.ltm.sltm.StaticLtmSettings;
import org.goplanit.assignment.ltm.sltm.conjugate.ConjugateBushFlowUpdateConsumerImpl;
import org.goplanit.assignment.ltm.sltm.conjugate.ConjugateBushTurnFlowUpdateConsumer;
import org.goplanit.assignment.ltm.sltm.conjugate.ConjugateDestinationBush;
import org.goplanit.assignment.ltm.sltm.consumer.NetworkFlowUpdateData;
import org.goplanit.network.transport.ConjugateTransportModelNetwork;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.physical.Movement;

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
    /* original turn (so conjugate link segment) based + optional original link sending flow based (so conjugate node) */

    // shouldn't matter if we use movements or conjugate segments
    int numConjugateSegments = conjugateTransportModelNetwork.getNumberOfEdgeSegmentsAllLayers();
    int numMovements = getTransportNetwork().getMovements().size();
    assert(numConjugateSegments==numMovements);
    return new ConjugateBushTurnFlowUpdateConsumer(
            createNetworkTurnFlowData(updateLinkSendingFlows, numMovements/*numConjugateSegments*/));
  }

  /**
   * Constructor
   * 
   * @param idToken      to use
   * @param assignmentId to use
   * @param nlSegmentPair2MovementMap to use
   * @param settings     to use
   */
  public StaticLtmLoadingBushConjugate(
          IdGroupingToken idToken,
          long assignmentId,
          MultiKeyMap<Object, Movement> nlSegmentPair2MovementMap,
          ConjugateTransportModelNetwork conjugateTransportModelNetwork,
          final StaticLtmSettings settings) {
    super(idToken, assignmentId, nlSegmentPair2MovementMap, settings);
    this.conjugateTransportModelNetwork = conjugateTransportModelNetwork;
  }

  /** access to the conjugate transport model network
   * @return conjugate version of transport model network
   */
  public ConjugateTransportModelNetwork getConjugateTransportModelNetwork() {
    return conjugateTransportModelNetwork;
  }
}

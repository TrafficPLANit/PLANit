package org.goplanit.assignment.ltm.sltm.loading;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.assignment.ltm.sltm.StaticLtmSettings;
import org.goplanit.assignment.ltm.sltm.consumer.*;
import org.goplanit.assignment.ltm.sltm.conjugate.ConjugateDestinationBush;
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
  protected ConjugateBushNetworkFlowUpdateConsumerImpl<NetworkFlowUpdateData> createBushLinkSendingFlowUpdateConsumer(
          boolean updateLinkOutflows, boolean updateUnconstrainedLinkFlows){
    return new ConjugateBushNetworkFlowUpdateConsumerImpl<>(
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

  protected ConjugateBushSyncNetworkFlowConsumer createSyncAllNetworkFlowUpdateConsumer(){
    nlSendingFlowData.reset();
    nlInFlowOutflowData.resetInflows();
    nlInFlowOutflowData.resetOutflows();
    unconstrainedFlowData.reset();

    int numConjugateSegments = conjugateTransportModelNetwork.getNumberOfEdgeSegmentsAllLayers();

    // all in one update (except for alphas), splitting rates are not updated, just populated in full
    return new ConjugateBushSyncNetworkFlowConsumer(
        new NetworkTurnFlowUpdateData(
            true,
            nlSendingFlowData,
            nlSplittingRateData,
            networkLoadingFactorData,
            nlInFlowOutflowData.getInflows(),
            nlInFlowOutflowData.getOutflows(),
            unconstrainedFlowData,
            numConjugateSegments),
        turn2ConjugateSegmentMapping);
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

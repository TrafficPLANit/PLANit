package org.goplanit.assignment.ltm.sltm.loading;

import org.goplanit.assignment.common.pas.Pas;
import org.goplanit.assignment.ltm.sltm.input.StaticLtmSettings;
import org.goplanit.assignment.ltm.sltm.consumer.*;
import org.goplanit.assignment.common.bush.ConjugateDestinationBush;
import org.goplanit.network.transport.ConjugateTransportModelNetwork;
import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.physical.CompiledRelationMapping;

import java.util.TreeSet;
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

  private final CompiledRelationMapping<ConjugateEdgeSegment> compiledTurnToConjugateSegmentMapping;

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateBushNetworkFlowUpdateConsumerImpl<NetworkFlowUpdateData> createBushLinkSendingFlowUpdateConsumer(
          boolean updateLinkOutflows, boolean updateUnconstrainedLinkFlows){
    return new ConjugateBushNetworkFlowUpdateConsumerImpl<>(
        createNetworkLinkFlowData(updateLinkOutflows, updateUnconstrainedLinkFlows),
        compiledTurnToConjugateSegmentMapping);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateBushTurnFlowUpdateConsumer createBushTurnFlowUpdateConsumer(
          boolean updateLinkSendingFlows) {
    /* original turn (so conjugate link segment) based + optional original link sending flow based (so conjugate node) */

    // we use conjugate segments because they also contain the dummy (connectoid) turns that have no entry segment
    return new ConjugateBushTurnFlowUpdateConsumer(
        createNetworkTurnFlowData(updateLinkSendingFlows, (int) compiledTurnToConjugateSegmentMapping.size()),
        compiledTurnToConjugateSegmentMapping);
  }

  public ConjugateBushSyncNetworkFlowConsumer createSyncAllNetworkFlowUpdateConsumer(){
    nlSendingFlowData.reset();
    nlInFlowOutflowData.resetInflows();
    nlInFlowOutflowData.resetOutflows();
    unconstrainedFlowData.reset();

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
            (int) compiledTurnToConjugateSegmentMapping.size()),
            compiledTurnToConjugateSegmentMapping);
  }

  /**
   * Constructor
   * 
   * @param idToken      to use
   * @param assignmentId to use
   * @param compiledTurnToConjugateSegmentMapping to use
   * @param settings     to use
   */
  public StaticLtmLoadingBushConjugate(
      IdGroupingToken idToken,
      long assignmentId,
      CompiledRelationMapping<ConjugateEdgeSegment> compiledTurnToConjugateSegmentMapping,
      final StaticLtmSettings settings) {
    super(idToken, assignmentId, settings);
    this.compiledTurnToConjugateSegmentMapping = compiledTurnToConjugateSegmentMapping;
  }

  // special version of network splitting rate loading update where we limit ourselves to propagating PAS flows instead
  // of full bush loading - used in route choice update to get a better estimate of route choice impact for internal
  // iterations
  public void stepOneSplittingRatesUpdateNotBushButPasBased(
      Mode theMode,
      TreeSet<Pas<ConjugateDirectedVertex, ConjugateEdgeSegment>> passToPropagate,
      TreeSet<EdgeSegment> pasTouchedSegments) {

    boolean updateLinkSendingFlows = false;
    var selectiveBushPasNodeTurnFlowUpdateConsumer = new ConjugateBushTurnFlowUpdateConsumer(
        createNetworkTurnFlowData(updateLinkSendingFlows, (int) compiledTurnToConjugateSegmentMapping.size()),
        compiledTurnToConjugateSegmentMapping,
        pasTouchedSegments);

    /* execute loading - for selective bushes with selective nodes - */
    executeNetworkLoadingUpdate(selectiveBushPasNodeTurnFlowUpdateConsumer);

    /* update splitting rates - for selective segments - Eq. (6),(4) */
    updateNextSplittingRates(selectiveBushPasNodeTurnFlowUpdateConsumer.getAcceptedTurnFlows(), pasTouchedSegments);
  }
}



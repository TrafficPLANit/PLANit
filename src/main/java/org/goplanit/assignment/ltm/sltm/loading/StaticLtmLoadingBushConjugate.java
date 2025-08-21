package org.goplanit.assignment.ltm.sltm.loading;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.assignment.ltm.sltm.Pas;
import org.goplanit.assignment.ltm.sltm.RootedBush;
import org.goplanit.assignment.ltm.sltm.StaticLtmSettings;
import org.goplanit.assignment.ltm.sltm.conjugate.ConjugateBushUtils;
import org.goplanit.assignment.ltm.sltm.consumer.*;
import org.goplanit.assignment.ltm.sltm.conjugate.ConjugateDestinationBush;
import org.goplanit.network.transport.ConjugateTransportModelNetwork;
import org.goplanit.utils.arrays.ArrayUtils;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.virtual.physical.ConnectoidNode;
import org.goplanit.utils.network.virtual.physical.ConnectoidSegment;

import java.util.Collection;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
  public ConjugateBushNetworkFlowUpdateConsumerImpl<NetworkFlowUpdateData> createBushLinkSendingFlowUpdateConsumer(
          boolean updateLinkOutflows, boolean updateUnconstrainedLinkFlows){
    return new ConjugateBushNetworkFlowUpdateConsumerImpl<>(
            createNetworkLinkFlowData(updateLinkOutflows, updateUnconstrainedLinkFlows), turn2ConjugateSegmentMapping);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateBushTurnFlowUpdateConsumer createBushTurnFlowUpdateConsumer(
          boolean updateLinkSendingFlows) {
    /* original turn (so conjugate link segment) based + optional original link sending flow based (so conjugate node) */

    // we use conjugate segments because they also contain the dummy (connectoid) turns that have no entry segment
    int numConjugateSegments = conjugateTransportModelNetwork.getNumberOfEdgeSegmentsAllLayers();
    return new ConjugateBushTurnFlowUpdateConsumer(
            createNetworkTurnFlowData(updateLinkSendingFlows, numConjugateSegments), turn2ConjugateSegmentMapping);
  }

  public ConjugateBushSyncNetworkFlowConsumer createSyncAllNetworkFlowUpdateConsumer(){
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

  // special version of network splitting rate loading update where we limit ourselves to propagating PAS flows instead
  // of full bush loading - used in route choice update to get a better estimate of rotue choice impact for internal
  // iterations
  public void stepOneSplittingRatesUpdateNotBushButPasBased(
      Mode theMode,
      TreeSet<Pas<ConjugateDirectedVertex, ConjugateEdgeSegment>> passToPropagate,
      TreeSet<EdgeSegment> pasTouchedSegments) {

    boolean updateLinkSendingFlows = false;
    int numConjugateSegments = conjugateTransportModelNetwork.getNumberOfEdgeSegmentsAllLayers();
    var selectiveBushPasNodeTurnFlowUpdateConsumer = new ConjugateBushTurnFlowUpdateConsumer(
        createNetworkTurnFlowData(updateLinkSendingFlows, numConjugateSegments),
        turn2ConjugateSegmentMapping,
        pasTouchedSegments);

    /* execute loading - for selective bushes with selective nodes - */
    executeNetworkLoadingUpdate(selectiveBushPasNodeTurnFlowUpdateConsumer);

    /* update splitting rates - for selective nodes - Eq. (6),(4) */
    updateNextSplittingRates(selectiveBushPasNodeTurnFlowUpdateConsumer.getAcceptedTurnFlows(), pasTouchedSegments);
  }
}



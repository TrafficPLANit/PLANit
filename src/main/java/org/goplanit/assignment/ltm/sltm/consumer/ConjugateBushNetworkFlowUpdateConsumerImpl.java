package org.goplanit.assignment.ltm.sltm.consumer;

import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.assignment.ltm.sltm.util.ConjugateBushUtils;
import org.goplanit.assignment.common.bush.ConjugateDestinationBush;
import org.goplanit.utils.arrays.ArrayUtils;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.CollectionUtils;
import org.goplanit.utils.network.layer.physical.CompiledMovementIds;
import org.goplanit.utils.network.virtual.physical.ConnectoidNode;
import org.goplanit.utils.network.virtual.physical.ConnectoidSegment;

/**
 * Conjugate Bush consumer to apply during conjugate bush based network loading flow update for each origin bush
 * <p>
 * Derived implementation can apply different changes to each of the (turn/link) flows on the bushes by
 * 
 * @author markr
 *
 */
public class ConjugateBushNetworkFlowUpdateConsumerImpl<T extends NetworkFlowUpdateData>
        implements BushFlowUpdateConsumer<ConjugateDestinationBush> {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(
      ConjugateBushNetworkFlowUpdateConsumerImpl.class.getCanonicalName());

  /** data and configuration used for a flow update by derived classes */
  protected T dataConfig;

  /** mapping from origin turn (segment, segment key) to movement data index */
  protected final CompiledMovementIds compiledMovementIds;

  /** when null all edge segments are processed (Default during regular loading) */
  protected final Set<EdgeSegment> edgeSegmentsToUpdate;

  /**
   * Register the conjugate bush accepted turn flow to the turn if required. Default implementation does nothing
   * but provide a hook for derived classes that do require to do something with turn accepted flows
   * 
   * @param turnSegment          of turn
   * @param turnAcceptedFlowPcuH sending flow rate of turn
   */
  protected void applyAcceptedTurnFlowUpdate(final ConjugateEdgeSegment turnSegment, double turnAcceptedFlowPcuH) {
    // default implementation does nothing but provide a hook for derived classes that do require to do something
    // with turn accepted flows
  }

  /**
   * Constructor
   *
   * @param dataConfig              to use
   * @param compiledMovementIds to use
   */
  public ConjugateBushNetworkFlowUpdateConsumerImpl(
          final T dataConfig, final CompiledMovementIds compiledMovementIds){
    this.dataConfig = dataConfig;
    this.compiledMovementIds = compiledMovementIds;
    this.edgeSegmentsToUpdate = null;   // so all are updated
  }

  /**
   * Constructor for selective bush and node updating, in which case we skip non-selected bushes from updating and
   * only update the selected nodes and skip all others for the selected bushes.
   *
   * @param dataConfig              to use
   * @param compiledMovementIds to use
   * @param edgeSegmentsToUpdate selective edge segments to update (all turn flows of all bushes of these edge segments)
   */
  public ConjugateBushNetworkFlowUpdateConsumerImpl(
      final T dataConfig,
      CompiledMovementIds compiledMovementIds,
      Set<EdgeSegment> edgeSegmentsToUpdate){
    this.dataConfig = dataConfig;
    this.compiledMovementIds = compiledMovementIds;
    this.edgeSegmentsToUpdate = edgeSegmentsToUpdate;
  }

  /**
   * Update(increase) the (network) flows based on the bush at hand as dictated by the data configuration
   *
   * @param bush to apply to
   */
  @Override
  public void accept(final ConjugateDestinationBush bush) {
    /*
     * track bush sending flows propagated from the origin. Note: We cannot use the bush's own turn sending flows
     * because we are performing a network loading based on the most recent bush's splitting rates, we only use
     * the bush's sending flows for bush flow shifts. The bush's sending flows are updated AFTER the network loading
     * is complete (converged) by using the network reduction factors
     */

    /* get topological sorted vertices to process */
    var vertexIter = bush.isInverted() ? bush.getInvertedTopologicalIterator() : bush.getTopologicalIterator();
    if (vertexIter == null) {
      LOGGER.severe(String.format("Topologically sorted conjugate bush (%s) not available, this shouldn't happen",
              bush.getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
      LOGGER.severe(bush.toString());
      return;
    }
    var currConjVertex = vertexIter.next();

    /* initialise origin vertex outgoing edge sending flows */
    var bushSendingFlows =
        ConjugateBushUtils.createOriginExitSegmentSendingFlowsSegmentFiltered(bush, edgeSegmentsToUpdate);
    TreeMap<ConjugateEdgeSegment, Double> bushUnconstrainedFlows =
            dataConfig.isUnconstrainedFlowsUpdate() ? new TreeMap<>(bushSendingFlows) : null;

    /* pass over bush in topological order propagating flow from origin */
    while (vertexIter.hasNext()) {
      currConjVertex = vertexIter.next();

      /* bush splitting rates by [exit segment, exit label] as key
      *  in conjugate setting all conjugate entry segments will have the same splitting rates */
      double[] splittingRates = null;
      double splittingRateTotal = -1;
      for (var conjEntrySegment : currConjVertex.getEntryEdgeSegments()) {
        if (!bush.contains(conjEntrySegment)) {
          continue;
        }

        var originalTurnEntrySegment = conjEntrySegment.getOriginalAdjacentEdgeSegments().first();

        boolean allowSendingFlowAsSeedFlowInsteadOfOrigin = false;
        if(originalTurnEntrySegment!=null && !CollectionUtils.nullOrEmpty(edgeSegmentsToUpdate)){
          if(!edgeSegmentsToUpdate.contains(originalTurnEntrySegment)){
            // skip if not slated for update
            continue;
          }
          allowSendingFlowAsSeedFlowInsteadOfOrigin = true;
        }

        boolean skip = false;
        Double bushConjSegmentSendingFlow = bushSendingFlows.get(conjEntrySegment);
        // can happen in case it is there to maintain spanning tree or when it is a starting point for partial update
        // when the latter is the case we use the current bush sending flow as reference starting point
        if (bushConjSegmentSendingFlow == null ) {
          if(allowSendingFlowAsSeedFlowInsteadOfOrigin){
            if(!bush.containsTurnSendingFlow(conjEntrySegment)) {
              skip = true;
            }else {
              // initialise for partial update as this is expected to be a local point of origin
              bushConjSegmentSendingFlow = bush.getSendingFlowPcuH(conjEntrySegment);
              bushSendingFlows.put(conjEntrySegment, bushConjSegmentSendingFlow);
              double initialAlpha = originalTurnEntrySegment!=null ?
                  dataConfig.getFlowAcceptanceFactors()[(int)originalTurnEntrySegment.getId()] : 1.0;
              applyAcceptedTurnFlowUpdate(conjEntrySegment, bushConjSegmentSendingFlow*initialAlpha);
            }
          }else {
            skip = true;
          }
        }
        if(skip){
          continue;
        }

        double bushConjSegmentAcceptedFlow = bushConjSegmentSendingFlow;

        // ....otherwise propagate with alphas and update "real" network as we go
        if(originalTurnEntrySegment!=null) {
          var originalEntrySegmentId = (int) originalTurnEntrySegment.getId();

          /* v^o_ab = s^o_ab * alpha_a */
          double alpha = dataConfig.getFlowAcceptanceFactors()[originalEntrySegmentId];
          bushConjSegmentAcceptedFlow *= alpha;

          /* s_a = SUM(u^o_a) (only when enabled) */
          if (dataConfig.isSendingFlowsUpdate()) {
            dataConfig.getSendingFlows()[originalEntrySegmentId] += bushConjSegmentSendingFlow;
          }
          if (dataConfig.isInflowsUpdate()) {
            dataConfig.getInFlows()[originalEntrySegmentId] += bushConjSegmentSendingFlow;
          }

          if (dataConfig.isUnconstrainedFlowsUpdate()) {
            double bushLinkDemand = bushUnconstrainedFlows.get(conjEntrySegment);
            dataConfig.getUnconstrainedFlows()[originalEntrySegmentId] += bushLinkDemand;
          }

          /* v_a = SUM(v^o_a) (only when enabled) */
          if (dataConfig.isOutflowsUpdate()) {
            dataConfig.getOutFlows()[originalEntrySegmentId] += bushConjSegmentAcceptedFlow;
          }
        }

        if(splittingRates == null && currConjVertex.hasExitEdgeSegments()){
          splittingRates = bush.getSplittingRates(currConjVertex);
          splittingRateTotal = ArrayUtils.sumOf(splittingRates);

          if(!(currConjVertex instanceof ConnectoidNode)) {
            if (splittingRateTotal <= 0.0 && bushConjSegmentSendingFlow > 0) {
              LOGGER.severe(String.format(
                  "Splitting rates 0%%, but sending flow present (%.4f), for segment %s on bush %s, this shouldn't happen",
                  bushConjSegmentSendingFlow, conjEntrySegment.getIdsAsString(), bush.getRootZone().getIdsAsString()));
              continue;
            }

            if (Precision.smaller(splittingRateTotal, 1, Precision.EPSILON_6)) {
              LOGGER.severe("Splitting rates do not add up to 100%, this shouldn't happen");
            }
          }
        }

        int splittingRateIndex = 0;
        double totalExitAcceptedFlow = 0;
        for (var conjExitSegment : currConjVertex.getExitEdgeSegments()) {
          if (!bush.contains(conjExitSegment)) {
            ++splittingRateIndex;
            continue;
          }

          double splittingRate = splittingRates[splittingRateIndex];
          if (splittingRate > 0) {

            /* v^o_ab = v^o_a * phi_ab */
            double turnAcceptedFlow = bushConjSegmentAcceptedFlow * splittingRate;
            totalExitAcceptedFlow += turnAcceptedFlow;

            double exitFlowToUpdate = bushSendingFlows.getOrDefault(conjExitSegment, 0.0) + turnAcceptedFlow;
            bushSendingFlows.put(conjExitSegment, exitFlowToUpdate);

            if(dataConfig.isUnconstrainedFlowsUpdate()){
              double bushTurnDemand =
                      bushUnconstrainedFlows.getOrDefault(conjEntrySegment, 0.0) * splittingRate;
              bushUnconstrainedFlows.put(conjExitSegment,
                      bushUnconstrainedFlows.getOrDefault(conjExitSegment,0.0) + bushTurnDemand);
            }

            /* update turn accepted flows as per derived class implementation (or do nothing) */
            applyAcceptedTurnFlowUpdate(conjExitSegment, turnAcceptedFlow);
          }
          ++splittingRateIndex;
        }

        if (Precision.notEqual(bushConjSegmentAcceptedFlow, totalExitAcceptedFlow) &&
            !(conjEntrySegment instanceof ConnectoidSegment)) {
          LOGGER.severe(String.format("Accepted out flow %.10f on conjugate edge segment (%s) not equal " +
                  "to flow (%.10f) assigned to turns on bush %s, this shouldn't happen",
                  bushConjSegmentAcceptedFlow,
                  conjEntrySegment.getXmlId(),
                  totalExitAcceptedFlow,
                  bush.getDestination().getParent().getParentZone().getIdsAsString()));
        }
      }
    }
  }
}

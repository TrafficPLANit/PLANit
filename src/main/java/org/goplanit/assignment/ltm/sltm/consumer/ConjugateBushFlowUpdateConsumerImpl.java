package org.goplanit.assignment.ltm.sltm.consumer;

import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.assignment.ltm.sltm.conjugate.ConjugateDestinationBush;
import org.goplanit.utils.arrays.ArrayUtils;
import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.network.virtual.physical.ConnectoidNode;
import org.goplanit.utils.network.virtual.physical.ConnectoidSegment;
import org.goplanit.utils.network.virtual.physical.conjugate.ConjugateConnectoidNode;
import org.goplanit.utils.network.virtual.physical.conjugate.ConjugateConnectoidSegment;

/**
 * Conjugate Bush consumer to apply during conjugate bush based network loading flow update for each origin bush
 * <p>
 * Derived implementation can apply different changes to each of the (turn/link) flows on the bushes by
 * 
 * @author markr
 *
 */
public class ConjugateBushFlowUpdateConsumerImpl<T extends NetworkFlowUpdateData>
        implements BushFlowUpdateConsumer<ConjugateDestinationBush> {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(ConjugateBushFlowUpdateConsumerImpl.class.getCanonicalName());

  /** data and configuration used for a flow update by derived classes */
  protected T dataConfig;

  /** mapping from origin turn (segment, segment key) to conjugate segment */
  protected final MultiKeyMap<Object, ConjugateEdgeSegment> turn2ConjSegmentMapping;

  /**
   * Initialise the bush sending flows for the bush's root exit edge segments to bootstrap the loading for this bush
   * 
   * @param bush             at hand
   * @param bushSendingFlows to populate as a starting point for the bush loading
   */
  private void initialiseOriginExitSegmentSendingFlows(
          final ConjugateDestinationBush bush, final Map<ConjugateEdgeSegment, Double> bushSendingFlows) {

    var originVertices = bush.getOriginVertices();
    for (ConjugateDirectedVertex originVertex : originVertices) {
      double totalOriginsSendingFlow = 0;
      for (var originExit : originVertex.getExitEdgeSegments()) {
        if (bush.contains(originExit)) {
          double sendingFlow = bush.getTurnSendingFlow(originExit);
          bushSendingFlows.put(originExit, sendingFlow);
          totalOriginsSendingFlow += sendingFlow;
        }
      }

      if (Precision.notEqual(totalOriginsSendingFlow, bush.getOriginDemandPcuH(originVertex), Precision.EPSILON_3)) {
        LOGGER.severe(String.format("conjugate bush with root zone (%s) origin's (%s) travel demand (%.8f pcu/h) not equal " +
                        "to total flow (%.8f pcu/h), this shouldn't happen",
                bush.getRootZoneVertex().getParent().getParentZone().getIdsAsString(),
                ((ConjugateConnectoidNode)originVertex).getCentroidVertex().getParent().getParentZone().getXmlId(),
                bush.getOriginDemandPcuH(originVertex), totalOriginsSendingFlow));
      }
    }

  }

  /**
   * Register the conjugate bush accepted turn flow to the turn if required. Default implementation does nothing but provide a hook for derived classes that do require to do
   * something with turn accepted flows
   * 
   * @param turnSegment          of turn
   * @param turnAcceptedFlowPcuH sending flow rate of turn
   */
  protected void applyAcceptedTurnFlowUpdate(final ConjugateEdgeSegment turnSegment, double turnAcceptedFlowPcuH) {
    // default implementation does nothing but provide a hook for derived classes that do require to do something with turn accepted flows
  }

  /**
   * Constructor
   *
   * @param dataConfig              to use
   * @param turn2ConjSegmentMapping to use
   */
  public ConjugateBushFlowUpdateConsumerImpl(
          final T dataConfig, final MultiKeyMap<Object, ConjugateEdgeSegment> turn2ConjSegmentMapping){
    this.dataConfig = dataConfig;
    this.turn2ConjSegmentMapping = turn2ConjSegmentMapping;
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

    /* key is conjugate segment, value is sending flow */
    TreeMap<ConjugateEdgeSegment, Double> bushSendingFlows = new TreeMap<>();

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
    initialiseOriginExitSegmentSendingFlows(bush, bushSendingFlows);
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
        Double bushConjSegmentSendingFlow = bushSendingFlows.get(conjEntrySegment);
        if (bushConjSegmentSendingFlow == null ) {
          // can happen in case it is there to maintain spanning tree
          continue;
        }

        double bushConjSegmentAcceptedFlow = bushConjSegmentSendingFlow;
        var originalTurnEntrySegment = conjEntrySegment.getOriginalAdjacentEdgeSegments().first();

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

        if (Precision.notEqual(bushConjSegmentAcceptedFlow, totalExitAcceptedFlow) && !(conjEntrySegment instanceof ConnectoidSegment)) {
          LOGGER.severe(String.format("Accepted out flow %.10f on conjugate edge segment (%s) not equal to flow (%.10f) assigned " +
                          "to turns on bush %s, this shouldn't happen",
                  bushConjSegmentAcceptedFlow,
                  conjEntrySegment.getXmlId(),
                  totalExitAcceptedFlow,
                  bush.getDestination().getParent().getParentZone().getIdsAsString()));
        }
      }
    }
  }
}

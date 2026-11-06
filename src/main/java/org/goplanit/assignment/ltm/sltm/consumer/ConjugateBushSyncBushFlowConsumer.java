package org.goplanit.assignment.ltm.sltm.consumer;

import org.goplanit.assignment.common.bush.ConjugateDestinationBush;
import org.goplanit.assignment.ltm.sltm.util.ConjugateBushUtils;
import org.goplanit.utils.arrays.ArrayUtils;
import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.CollectionUtils;
import org.goplanit.utils.network.virtual.physical.ConnectoidNode;
import org.goplanit.utils.network.virtual.physical.ConnectoidSegment;

import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Conjugate Bush consumer to use when we sync the bush turn flows to the results of the network
 * loading to ensure the bush turn sending flows reflect the correct level of flow
 * <p>
 *   Reason 1: alphas change due to network loading, causing bush flows to be too high or too low
 *   Reason 2: if inconsistencies were introduced when performing flow shifts in the previous iteration, e.g.
 *     flow shifts violating capacities that are difficult to fix while performing route choice, we ensure
 *     that any dangling turn flows on no longer used turns are wiped out here and we start fresh.
 *     Example: adding flow to uncongested turn (claiming larger fair share) into congested exit, causes total
 *     exceeding capacity. We truncate network flows, but do not do this for all other bushes on other turns that should
 *     get reduced as this would be computationally costly and require loading effectively. this can cause ghost bush
 *     flow downstream when we remove flows in the longer term. For splitting rates this becomes an issue if we do not
 *     detect ghost flows as they should be reduced back to zero based on the loading
 *     result, which is what we do here by propagating from origins.
 * </p>
 * <p>
 *   replace turn sending flows with synced ones propagated from origin, we make sure to update all.
 * </p>
 * 
 * @author markr
 *
 */
public class ConjugateBushSyncBushFlowConsumer implements Consumer<ConjugateDirectedVertex> {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(ConjugateBushSyncBushFlowConsumer.class.getCanonicalName());

  /** bush at hand */
  private final ConjugateDestinationBush conjBush;

  private final double[] originalNetworkFlowAcceptanceFactors;

  /** bush sending flows to track/propagate during syncing */
  private final TreeMap<ConjugateEdgeSegment, Double> bushSendingFlows;

  /**
   * Register the conjugate bush accepted turn flow to the turn if required. Default implementation does nothing but
   * provide a hook for derived classes that do require to do something with turn accepted flows
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
   * @param conjBush to sync
   * @param originalNetworkFlowAcceptanceFactors  to use
   */
  public ConjugateBushSyncBushFlowConsumer(
          final ConjugateDestinationBush conjBush,
          double[] originalNetworkFlowAcceptanceFactors){
    this.conjBush = conjBush;
    this.originalNetworkFlowAcceptanceFactors = originalNetworkFlowAcceptanceFactors;
    this.bushSendingFlows = ConjugateBushUtils.createAllOriginExitSegmentSendingFlows(conjBush);
  }

  /**
   * Update the bush turn sending flows in line with its splitting rates AND origin sending flows AND
   * current network loading acceptance factors
   * todo: I have not bothered with the final link segments before the destination as we only update the turn
   *   sending flows on the entries. Given that the last segment always leads to the destination and nothing else
   *   it should not matter if the flows are not correct as it should always only have a single non-zero flow to the
   *   destination, so leaving that for now.
   *
   * @param currConjVertex to apply to
   */
  @Override
  public void accept(final ConjugateDirectedVertex currConjVertex) {

    // splitting rates for turns on original link (conjugate vertex)
    double[] splittingRates = conjBush.getSplittingRates(currConjVertex);
    if(!currConjVertex.hasExitEdgeSegments()) {
      return;
    }
    double splittingRateTotal = ArrayUtils.sumOf(splittingRates);
    if(!(currConjVertex instanceof ConnectoidNode)) {
      if (splittingRateTotal <= 0.0) {
        LOGGER.severe(String.format(
            "Splitting rates 0%%, for original segment %s on bush %s, this shouldn't happen",
            currConjVertex.getOriginalEdgeSegment().getIdsAsString(), conjBush.getRootZone().getIdsAsString()));
        return;
      }

      if (Precision.smaller(splittingRateTotal, 1, Precision.EPSILON_6)) {
        LOGGER.severe("Splitting rates do not add up to 100%, this shouldn't happen");
      }
    }

    /* bush splitting rates by [exit segment, exit label] as key
    *  in conjugate setting all conjugate entry segments will have the same splitting rates */
    for (var conjEntrySegment : currConjVertex.getEntryEdgeSegments()) {
      if (!conjBush.contains(conjEntrySegment)) {
        if(conjBush.containsTurnSendingFlow(conjEntrySegment)){
          // should not happen, but if it does, we must make sure that its flow is synced to zero
          LOGGER.warning(String.format(
              "Found (node entry) turn (%s) that is not on bush DAG but still has sending flow, removing it",conjEntrySegment.getIdsAsString()));
          conjBush.remove(conjEntrySegment);
        }
        continue;
      }

      Double bushConjSegmentSendingFlow = bushSendingFlows.get(conjEntrySegment);
      if (bushConjSegmentSendingFlow == null) {
        if(conjBush.containsTurnSendingFlow(conjEntrySegment)) {
          // should ideally not happen, but possible due to shifting not being fully consistent with loading as it
          // happens locally, when we find ghost flow, we make sure that it is truncated to zero
          LOGGER.fine(String.format(
              "Found (node entry) turn (%s) with ghost turn flow on bush %s, removing it",
              conjEntrySegment.getIdsAsString(), conjBush.getRootZone().getIdsAsString()));
          conjBush.bushData.removeTurnData(conjEntrySegment);
        }
        // no point in processing exits as there is no flow to propagate
        continue;
      }

      // IMPOSE SENDING FLOW ON BUSH
      conjBush.bushData.setTurnSendingFlow(conjEntrySegment, bushConjSegmentSendingFlow, false);

      // ....otherwise propagate with alphas and update local bush sending flows as we go
      var originalTurnEntrySegment = conjEntrySegment.getOriginalAdjacentEdgeSegments().first();
      double bushConjSegmentAcceptedFlow = bushConjSegmentSendingFlow;
      if (originalTurnEntrySegment != null) {
        var originalEntrySegmentId = (int) originalTurnEntrySegment.getId();
        double alpha = Math.min(1,originalNetworkFlowAcceptanceFactors[originalEntrySegmentId]);
        bushConjSegmentAcceptedFlow *= alpha;
      }

      int splittingRateIndex = 0;
      double totalExitAcceptedFlow = 0;
      for (var conjExitSegment : currConjVertex.getExitEdgeSegments()) {
        double splittingRate = splittingRates[splittingRateIndex++];
        if (!conjBush.contains(conjExitSegment)) {

          if(conjBush.containsTurnSendingFlow(conjExitSegment)){
            // should not happen, but if it does, we must make sure that its flow is synced to zero
            LOGGER.warning(String.format(
                "Found (node exit) turn (%s) that is not on bush DAG but still has turn flow (%.4f), removing it",
                conjEntrySegment.getIdsAsString(), conjBush.getTurnSendingFlow(conjExitSegment)));

            // REMOVE (GHOST) TURN FLOW
            conjBush.remove(conjEntrySegment);
            if(bushConjSegmentAcceptedFlow > 0 && splittingRate>0){
              // removing ghost flow is not problematic but if we have a segment not on the bush but with flow and
              // propagated flow we do have an issue as it limits rearranging/improving the bush structure
              LOGGER.warning(String.format(
                  "removed (node exit) propagated flow was non-zero either (%.4f), bush flow propagation compromised",
                  bushConjSegmentAcceptedFlow * splittingRate));
            }
          }
          continue;
        }

        if (splittingRate > 0) {

          /* v^o_ab = v^o_a * phi_ab */
          double turnAcceptedFlow = bushConjSegmentAcceptedFlow * splittingRate;

          // update the synced local flow propagation
          if(turnAcceptedFlow > 0) {
            double exitFlowToUpdate = bushSendingFlows.getOrDefault(conjExitSegment, 0.0) + turnAcceptedFlow;
            bushSendingFlows.put(conjExitSegment, exitFlowToUpdate);
            totalExitAcceptedFlow += turnAcceptedFlow;
          }
        }
      }

      if (Precision.notEqual(bushConjSegmentAcceptedFlow, totalExitAcceptedFlow) && !(conjEntrySegment instanceof ConnectoidSegment)) {
        LOGGER.severe(String.format("Accepted out flow %.10f on conjugate edge segment (%s) not equal to flow (%.10f) assigned " +
                        "to turns on bush %s, this shouldn't happen",
                bushConjSegmentAcceptedFlow,
                conjEntrySegment.getXmlId(),
                totalExitAcceptedFlow,
                conjBush.getDestination().getParent().getParentZone().getIdsAsString()));
      }
    }
  }
}

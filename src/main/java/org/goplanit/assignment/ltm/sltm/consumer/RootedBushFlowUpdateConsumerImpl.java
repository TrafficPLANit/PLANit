package org.goplanit.assignment.ltm.sltm.consumer;

import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.assignment.ltm.sltm.BushFlowLabel;
import org.goplanit.assignment.ltm.sltm.DestinationBush;
import org.goplanit.assignment.ltm.sltm.RootedLabelledBush;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.network.layer.physical.Movement;
import org.goplanit.utils.network.virtual.CentroidVertex;
import org.goplanit.utils.network.virtual.ConnectoidSegment;

/**
 * Base Consumer to apply during bush based network loading flow update for each origin bush
 * <p>
 * Derived implementation can apply different changes to each of the (turn/link) flows on the bushes by
 * 
 * @author markr
 *
 */
public class RootedBushFlowUpdateConsumerImpl<T extends NetworkFlowUpdateData> implements BushFlowUpdateConsumer<RootedLabelledBush> {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(RootedBushFlowUpdateConsumerImpl.class.getCanonicalName());

  /** data and configuration used for a flow update by derived classes */
  protected T dataConfig;

  /** be able to convert entry/exit segment to their corresponding movement */
  private final MultiKeyMap<Object,Movement> segmentPair2MovementMap;

  /**
   * Initialise the bush sending flows for the bush's origins' exit edge segments to bootstrap the loading for this bush.
   * This is irrespective whether the bush is inverted and runs from destination to origins or the other way around
   * 
   * @param bush             at hand
   * @param bushSendingFlows to populate as a starting point for the bush loading
   */
  private void initialiseOriginExitSegmentSendingFlows(
          final RootedLabelledBush bush, final MultiKeyMap<Object, Double> bushSendingFlows) {
    Set<CentroidVertex> originVertices = bush.getOriginVertices();
    for (var originVertex : originVertices) {
      double totalOriginsSendingFlow = 0;
      for (var originExit : originVertex.getExitEdgeSegments()) {
        if (bush.containsEdgeSegment(originExit)) {
          var usedLabels = bush.getFlowCompositionLabels(originExit);
          if(usedLabels == null){
            LOGGER.severe(String.format("No labels found on exit segment (%s) despite bush containing segment, this shouldn't happen", originExit.getIdsAsString()));
            continue;
          }
          for (var usedLabel : usedLabels) {
            double sendingFlow = bush.getSendingFlowPcuH(originExit, usedLabel);
            bushSendingFlows.put(originExit, usedLabel, sendingFlow);
            totalOriginsSendingFlow += sendingFlow;
          }
        }
      }

      if (Precision.notEqual(totalOriginsSendingFlow, bush.getOriginDemandPcuH(originVertex), Precision.EPSILON_3)) {
        LOGGER.severe(String.format("bush specific origin's (%s) travel demand (%.8f pcu/h) not equal to total flow (%.8f pcu/h) placed on bush root, this shouldn't happen",
            originVertex.getParent().getParentZone().getXmlId(), bush.getOriginDemandPcuH(originVertex), totalOriginsSendingFlow));
      }
    }
  }

  /**
   * Register the bush accepted turn flow to the turn if required. Default implementation does nothing but provide a hook for derived classes that do require to do something with
   * turn accepted flows
   * 
   * @param movement          the movement
   * @param prevLabel            at hand
   * @param currLabel            at hand
   * @param turnAcceptedFlowPcuH sending flow rate of turn
   */
  protected void applyAcceptedTurnFlowUpdate(
          final Movement movement, final BushFlowLabel prevLabel, final BushFlowLabel currLabel, double turnAcceptedFlowPcuH) {
    // default implementation does nothing but provide a hook for derived classes that do require to do something with turn accepted flows
  }

  /**
   * Constructor
   * 
   * @param dataConfig to use
   * @param segmentPair2MovementMap mapping from entry/exit segment (dual key) to movement, use to covert turn flows
   *  to splitting rate data format
   */
  public RootedBushFlowUpdateConsumerImpl(final T dataConfig, MultiKeyMap<Object,Movement> segmentPair2MovementMap) {
    this.dataConfig = dataConfig;
    this.segmentPair2MovementMap = segmentPair2MovementMap;
  }

  /**
   * Update(increase) the (network) flows based on the bush at hand as dictated by the data configuration
   * 
   */
  @Override
  public void accept(final RootedLabelledBush bush) {
    /*
     * track bush sending flows propagated from the origin. Note: We cannot use the bush's own turn sending flows
     * because we are performing a network loading based on the most recent bush's splitting rates, we only use
     * the bush's sending flows for bush flow shifts. The bush's sending flows are updated AFTER the network loading
     * is complete (converged) by using the network reduction factors
     */

    /* key is segment+label, value is sending flow */
    MultiKeyMap<Object, Double> bushSendingFlows = new MultiKeyMap<>();

    /* get topological sorted vertices to process */
    var vertexIter = bush.isInverted() ? bush.getInvertedTopologicalIterator() : bush.getTopologicalIterator();
    if (vertexIter == null) {
      LOGGER.severe(String.format("Topologically sorted bush (%s) not available, this shouldn't happen", ((DestinationBush)bush).getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
      return;
    }
    var currVertex = vertexIter.next();

    /* initialise origin vertex outgoing edge sending flows */
    initialiseOriginExitSegmentSendingFlows(bush, bushSendingFlows);

    /* pass over bush in topological order propagating flow from origin */
    while (vertexIter.hasNext()) {
      currVertex = vertexIter.next();
      for (var entrySegment : currVertex.getEntryEdgeSegments()) {
        if (!bush.containsEdgeSegment(entrySegment)) {
          continue;
        }

        int entrySegmentId = (int) entrySegment.getId();
        var usedLabels = bush.getFlowCompositionLabels(entrySegment);
        if (usedLabels == null) {
          LOGGER.severe(String.format("Edge segment %s on bush (%s), but no flow labels present, this shouldn't happen",
              entrySegment.getXmlId(), ((CentroidVertex)bush.getRootVertex()).getParent().getParentZone().getIdsAsString()));
          continue;
        }

        double totalEntryAcceptedFlow = 0;
        double totalExitAcceptedFlow = 0;
        for (var entrylabel : usedLabels) {

          Double bushLinkLabelSendingFlow = bushSendingFlows.get(entrySegment, entrylabel);
          if (bushLinkLabelSendingFlow == null) {
            LOGGER.severe(String.format(
                "No link sending flow found for segment %s and label %d on bush(%s), this shouldn't happen",
                entrySegment.getXmlId(), entrylabel.getLabelId(),
                ((CentroidVertex)bush.getRootVertex()).getParent().getParentZone().getIdsAsString()));
            continue;
          }

          /* v^o_a = s^o_a * alpha_a */
          double alpha = dataConfig.flowAcceptanceFactors[entrySegmentId];
          double bushEntryAcceptedFlow = bushLinkLabelSendingFlow * alpha;
          totalEntryAcceptedFlow += bushEntryAcceptedFlow;

          /* s_a = SUM(u^o_a) (only when enabled) */
          if (dataConfig.isSendingflowsUpdate()) {
            dataConfig.sendingFlows[entrySegmentId] += bushLinkLabelSendingFlow;
          }

          /* v_a = SUM(v^o_a) (only when enabled) */
          if (dataConfig.isOutflowsUpdate()) {
            dataConfig.outFlows[entrySegmentId] += bushEntryAcceptedFlow;
          }

          /* bush splitting rates by [exit segment, exit label] as key */
          MultiKeyMap<Object, Double> splittingRates = bush.getSplittingRates(entrySegment, entrylabel);
          if (splittingRates == null || splittingRates.isEmpty()) {
            continue;
          }

          for (var exitSegment : currVertex.getExitEdgeSegments()) {
            if (!bush.containsEdgeSegment(exitSegment)) {
              continue;
            }

            var exitLabels = bush.getFlowCompositionLabels(exitSegment);
            if (exitLabels == null) {
              LOGGER.severe(String.format("Edge segment %s on bush (%s), but no flow labels present, this shouldn't happen",
                  exitSegment.getXmlId(),
                  ((CentroidVertex)bush.getRootVertex()).getParent().getParentZone().getIdsAsString()));
              continue;
            }

            for (var exitLabel : exitLabels) {
              Double splittingRate = splittingRates.get(exitSegment, exitLabel);
              if (splittingRate != null && splittingRate > 0) {
                /* v^o_ab = v^o_a * phi_ab */
                double turnAcceptedFlow = bushEntryAcceptedFlow * splittingRate;
                totalExitAcceptedFlow += turnAcceptedFlow;

                Double exitLabelFlowToUpdate = bushSendingFlows.get(exitSegment, exitLabel);
                if (exitLabelFlowToUpdate == null) {
                  exitLabelFlowToUpdate = turnAcceptedFlow;
                } else {
                  exitLabelFlowToUpdate += turnAcceptedFlow;
                }
                bushSendingFlows.put(exitSegment, exitLabel, exitLabelFlowToUpdate);

                /* update turn accepted flows as per derived class implementation (or do nothing) */
                applyAcceptedTurnFlowUpdate(
                        segmentPair2MovementMap.get(entrySegment, exitSegment), entrylabel, exitLabel, turnAcceptedFlow);
              }
            }
          }
        }
        if (Precision.notEqual(totalEntryAcceptedFlow, totalExitAcceptedFlow) && !(entrySegment instanceof ConnectoidSegment)) {
          LOGGER.severe(String.format("Accepted (labelled) out flow %.10f on edge segment (%s) not equal to flow (%.10f) assigned to (labelled) turns, this shouldn't happen",
              totalEntryAcceptedFlow, entrySegment.getXmlId(), totalExitAcceptedFlow));
        }
      }
    }
  }
}

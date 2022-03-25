package org.goplanit.assignment.ltm.sltm.consumer;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.assignment.ltm.sltm.Bush;
import org.goplanit.assignment.ltm.sltm.BushFlowLabel;
import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.math.Precision;

/**
 * Base Consumer to apply during bush based network loading flow update for each origin bush
 * <p>
 * Derived implementation can apply different changes to each of the (turn/link) flows on the bushes by
 * 
 * @author markr
 *
 */
public class BushFlowUpdateConsumer<T extends NetworkFlowUpdateData> implements Consumer<Bush> {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(BushFlowUpdateConsumer.class.getCanonicalName());

  /** data and configuration used for a flow update by derived classes */
  protected T dataConfig;

  /**
   * Initialise the bush sending flows for the bush's root exit edge segments to bootstrap the loading for this bush
   * 
   * @param originBush       at hand
   * @param bushSendingFlows to populate as a starting point for the bush loading
   */
  private void initialiseRootExitSegmentSendingFlows(final Bush originBush, final MultiKeyMap<Object, Double> bushSendingFlows) {
    double totalRootSendingFlow = 0;
    for (var rootExit : originBush.getOrigin().getCentroid().getExitEdgeSegments()) {
      if (originBush.containsEdgeSegment(rootExit)) {
        var usedLabels = originBush.getFlowCompositionLabels(rootExit);
        for (var usedLabel : usedLabels) {
          double sendingFlow = originBush.getSendingFlowPcuH(rootExit, usedLabel);
          bushSendingFlows.put(rootExit, usedLabel, sendingFlow);
          totalRootSendingFlow += sendingFlow;
        }
      }
    }
    if (Precision.notEqual(totalRootSendingFlow, originBush.getTravelDemandPcuH())) {
      LOGGER.severe(String.format("Origin (%s) travel demand not equal to total flow placed on bush root, this shouldn't happen", originBush.getOrigin().getXmlId()));
    }
  }

  /**
   * Register the bush accepted turn flow to the turn if required. Default implementation does nothing but provide a hook for derived classes that do require to do something with
   * turn accepted flows
   * 
   * @param prevSegment          of turn
   * @param prevLabel            at hand
   * @param currentSegment       of turn
   * @param currLabel            at hand
   * @param turnAcceptedFlowPcuH sending flow rate of turn
   */
  protected void applyAcceptedTurnFlowUpdate(final EdgeSegment prevSegment, final BushFlowLabel prevLabel, final EdgeSegment currentSegment, final BushFlowLabel currLabel,
      double turnAcceptedFlowPcuH) {
    // default implementation does nothing but provide a hook for derived classes that do require to do something with turn accepted flows
  }

  /**
   * Constructor
   * 
   * @param dataConfig to use
   */
  public BushFlowUpdateConsumer(final T dataConfig) {
    this.dataConfig = dataConfig;
  }

  /**
   * Update(increase) the (network) flows based on the bush at hand as dictated by the data configuration
   * 
   * {@inheritDoc}
   */
  @Override
  public void accept(final Bush originBush) {
    /*
     * track bush sending flows propagated from the origin. Note: We cannot use the bush's own turn sending flows because we are performing a network loading based on the most
     * recent bush's splitting rates, we only use the bush's sending flows for bush flow shifts. The bush's sending flows are updated AFTER the network loading is complete
     * (converged) by using the network reduction factors
     */

    /* key is segment+label, value is sending flow */
    MultiKeyMap<Object, Double> bushSendingFlows = new MultiKeyMap<>();

    /* get topological sorted vertices to process */
    Collection<DirectedVertex> topSortedVertices = originBush.getTopologicallySortedVertices();
    if (topSortedVertices == null) {
      LOGGER.severe(String.format("Topologically sorted bush rooted at origin %s not available, this shouldn't happen, skip", originBush.getOrigin().getXmlId()));
      return;
    }

    var vertexIter = topSortedVertices.iterator();
    var currVertex = vertexIter.next();
    if (!currVertex.idEquals(originBush.getOrigin().getCentroid())) {
      LOGGER.severe(String.format("Topologically sorted bush rooted at origin %s, does not commence with its root vertex %s", originBush.getOrigin().getXmlId(),
          originBush.getOrigin().getCentroid().getXmlId()));
      return;
    }

    /* initialise root vertex outgoing edge sending flows */
    initialiseRootExitSegmentSendingFlows(originBush, bushSendingFlows);

    /* pass over bush in topological order propagating flow from origin */
    while (vertexIter.hasNext()) {
      currVertex = vertexIter.next();
      for (var entrySegment : currVertex.getEntryEdgeSegments()) {
        if (!originBush.containsEdgeSegment(entrySegment)) {
          continue;
        }

        int entrySegmentId = (int) entrySegment.getId();
        var usedLabels = originBush.getFlowCompositionLabels(entrySegment);
        if (usedLabels == null) {
          LOGGER.severe(String.format("Edge segment %s on bush, but no flow labels present, this shouldn't happen", entrySegment.getXmlId()));
          continue;
        }

        for (var entrylabel : usedLabels) {

          Double bushLinkLabelSendingFlow = bushSendingFlows.get(entrySegment, entrylabel);
          if (bushLinkLabelSendingFlow == null) {
            LOGGER.severe(String.format("Origin (%s): No link sending flow found for segment %s and label %d, this shouldn't happen", originBush.getOrigin().getXmlId(),
                entrySegment.getXmlId(), entrylabel.getLabelId()));
            continue;
          }

          /* v^o_a = s^o_a * alpha_a */
          double alpha = dataConfig.flowAcceptanceFactors[entrySegmentId];
          double bushEntryAcceptedFlow = bushLinkLabelSendingFlow * alpha;

          /* s_a = SUM(u^o_a) (only when enabled) */
          if (dataConfig.isSendingflowsUpdate()) {
            dataConfig.sendingFlows[entrySegmentId] += bushLinkLabelSendingFlow;
          }

          /* v_a = SUM(v^o_a) (only when enabled) */
          if (dataConfig.isOutflowsUpdate()) {
            dataConfig.outFlows[entrySegmentId] += bushEntryAcceptedFlow;
          }

          /* bush splitting rates by [exit segment, exit label] as key */
          MultiKeyMap<Object, Double> splittingRates = originBush.getSplittingRates(entrySegment, entrylabel);
          if (splittingRates == null || splittingRates.isEmpty()) {
            continue;
          }

          for (var exitSegment : currVertex.getExitEdgeSegments()) {
            if (!originBush.containsEdgeSegment(exitSegment)) {
              continue;
            }

            var exitLabels = originBush.getFlowCompositionLabels(exitSegment);
            if (exitLabels == null) {
              LOGGER.severe(String.format("Edge segment %s on bush, but no flow labels present, this shouldn't happen", exitSegment.getXmlId()));
              continue;
            }

            for (var exitLabel : exitLabels) {
              Double splittingRate = splittingRates.get(exitSegment, exitLabel);
              if (splittingRate != null && splittingRate > 0) {

                /* v^o_ab = v^o_a * phi_ab */
                double turnAcceptedFlow = bushEntryAcceptedFlow * splittingRate;
                Double exitLabelFlowToUpdate = bushSendingFlows.get(exitSegment, exitLabel);
                if (exitLabelFlowToUpdate == null) {
                  exitLabelFlowToUpdate = turnAcceptedFlow;
                } else {
                  exitLabelFlowToUpdate += turnAcceptedFlow;
                }
                bushSendingFlows.put(exitSegment, exitLabel, exitLabelFlowToUpdate);

                /* update turn accepted flows as per derived class implementation (or do nothing) */
                applyAcceptedTurnFlowUpdate(entrySegment, entrylabel, exitSegment, exitLabel, turnAcceptedFlow);
              }
            }
          }
        }
      }
    }
  }
}

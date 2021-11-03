package org.goplanit.assignment.ltm.sltm.consumer;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.goplanit.assignment.ltm.sltm.Bush;
import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.math.Precision;

/**
 * Base Consumer to apply during bush based network flow update for each origin bush
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
  private void initialiseRootExitSegmentSendingFlows(final Bush originBush, final double[] bushSendingFlows) {
    int index = 0;
    double[] rootVertexSplittingRates = originBush.getRootVertexSplittingRates();
    for (EdgeSegment rootExit : originBush.getOrigin().getCentroid().getExitEdgeSegments()) {
      if (originBush.containsEdgeSegment(rootExit)) {
        // TODO: if this works well, then we can replace this by simply collecting the rootExit sending flow directly which is less costly (but has no checks)
        bushSendingFlows[(int) rootExit.getId()] = originBush.getTravelDemandPcuH() * rootVertexSplittingRates[index];
      }
      ++index;
    }
  }

  /**
   * Register the bush accepted turn flow to the turn if required. Default implementation does nothing but provide a hook for derived classes that do require to do something with
   * turn accepted flows
   * 
   * @param prevSegmentId       of turn
   * @param currentSegment      of turn
   * @param turnSendingFlowPcuH sending flow rate of turn
   */
  protected void applyAcceptedTurnFlowUpdate(final int prevSegmentId, final EdgeSegment currentSegment, final double turnAcceptedFlow) {
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
   */
  @Override
  public void accept(final Bush originBush) {
    /*
     * track bush sending flows propagated from the origin Note: We cannot use the bush's own turn sending flows because we are performing a network loading based on the most
     * recent bush's splitting rates, we only use the bush's sending flows for bush flow shifts. The bush's sending flows are updated AFTER the network loading is complete
     * (converged) by using the network splitting rates and reduction factors
     */
    double[] bushSendingFlows = new double[dataConfig.sendingFlows.length];

    /* get topological sorted vertices to process */
    Collection<DirectedVertex> topSortedVertices = originBush.getTopologicallySortedVertices();
    Iterator<DirectedVertex> vertexIter = topSortedVertices.iterator();
    DirectedVertex currVertex = vertexIter.next();
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
      for (EdgeSegment entrySegment : currVertex.getEntryEdgeSegments()) {
        if (originBush.containsEdgeSegment(entrySegment)) {
          int entrySegmentId = (int) entrySegment.getId();
          double bushLinkSendingFlow = bushSendingFlows[entrySegmentId];

          /* s_a = u_a */
          if (dataConfig.updateLinkSendingFlows) {
            dataConfig.sendingFlows[(int) entrySegment.getId()] += bushLinkSendingFlow;
          }

          /* v_a = s_a * alpha_a */
          double bushEntryAcceptedFlow = bushLinkSendingFlow * dataConfig.flowAcceptanceFactors[entrySegmentId];
          double[] splittingRates = originBush.getSplittingRates(entrySegment);
          int index = 0;
          for (EdgeSegment exitSegment : currVertex.getExitEdgeSegments()) {
            if (Precision.isPositive(splittingRates[index])) {
              int exitSegmentId = (int) exitSegment.getId();

              /* v_ab = v_a * phi_ab */
              double turnAcceptedFlow = bushEntryAcceptedFlow * splittingRates[index];
              bushSendingFlows[exitSegmentId] += turnAcceptedFlow; // bush level

              /* update turn accepted flows as per derived class implementation (or do nothing) */
              applyAcceptedTurnFlowUpdate(entrySegmentId, exitSegment, turnAcceptedFlow);
            }
            ++index;
          }
        }
      }
    }
  }

}

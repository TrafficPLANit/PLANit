package org.goplanit.assignment.ltm.sltm.conjugate;

import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.graph.directed.acyclic.ACyclicSubGraph;

import java.util.*;
import java.util.logging.Logger;

/**
 * Helps to initialise bushes based on shortest path(s), does not create initial PASs (hence simple)
 */
public class ConjugateBushSimpleInitialiserHelper {

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(ConjugateBushSimpleInitialiserHelper.class.getCanonicalName());

  /** to initialise */
  private final ConjugateDestinationBush bush;

  /** to use to initialise bush */
  private final ACyclicSubGraph rootedDag;

  /**
   * Constructor
   *
   * @param bush       to (further) initialise
   * @param rootedDag      to add to bush as initial supported DAG
   */
  protected ConjugateBushSimpleInitialiserHelper(
          final ConjugateDestinationBush bush, final ACyclicSubGraph rootedDag) {
    this.bush = bush;
    this.rootedDag = rootedDag;
  }

  /**
   * Factory method for bush initialiser
   * 
   * @param bush  conjugate to use
   * @param rootedDag to use
   * @return created helper
   */
  public static ConjugateBushSimpleInitialiserHelper create(
          final ConjugateDestinationBush bush, final ACyclicSubGraph rootedDag) {
    return new ConjugateBushSimpleInitialiserHelper(bush, rootedDag);
  }

  /**
   * Execute the initialisation by ensuring the correct flow is added to the bush for the given od dag and its
   * related demand.
   * <p>
   *   It is assumed we are given an iterator that runs from the origin towards destination(s) at all times here
   * </p>
   * 
   * @param originVertex   to start with, expected to be the centroid of the od's origin. It is expected the
   *                       iterator proceeds in downstream direction until reaching the destination
   * @param odDemandPcuH   to use for the origin vertex
   * @param vertexIter     iterator to use
   */
  public void executeOdBushInitialisation(
          DirectedVertex originVertex,
          final Double odDemandPcuH,
          final Iterator<DirectedVertex> vertexIter) {
    // track local to avoid confounding with other OD bush flows already placed on bush level flows
    Map<EdgeSegment, Double> localDagTurnSendingFlows = new HashMap<>();

    /* initialise starting flows on initial vertex */
    int numUsedOdExitSegments = rootedDag.getNumberOfEdgeSegments(originVertex, true /* exit segments */);
    for (var conjugateExitSegment : originVertex.getExitEdgeSegments()) {
      double exitProportionalOdDemandPcuH = odDemandPcuH / numUsedOdExitSegments;
      localDagTurnSendingFlows.put(conjugateExitSegment,exitProportionalOdDemandPcuH);
      bush.addTurnSendingFlow(
              (ConjugateEdgeSegment) conjugateExitSegment, exitProportionalOdDemandPcuH);
    }

    /* pass using topological ordering to propagate o-d flow and initialising labels from origin
     * NOTE: this pushes from the origin downstream in DAG type of topological way
     */
    DirectedVertex currVertex;
    while (vertexIter.hasNext()) {
      currVertex = vertexIter.next();

      /* aggregate incoming vertex flows */
      double vertexOdSendingFlow = 0;
      for (var conjugateEntryEdgeSegment : currVertex.getEntryEdgeSegments()) {
        if (rootedDag.containsEdgeSegment(conjugateEntryEdgeSegment)) {
          Double entrySegmentSendingFlow = localDagTurnSendingFlows.get(conjugateEntryEdgeSegment);
          if (entrySegmentSendingFlow == null) {
            continue;
          }
          vertexOdSendingFlow += entrySegmentSendingFlow;
        }
      }

      numUsedOdExitSegments = rootedDag.getNumberOfEdgeSegments(currVertex, true /* exit segments */);
      double proportionalOdExitFlow = vertexOdSendingFlow / numUsedOdExitSegments;

      for (var conjugateExitSegment : currVertex.getExitEdgeSegments()) {
        if (rootedDag.containsEdgeSegment(conjugateExitSegment)) {
          localDagTurnSendingFlows.put(conjugateExitSegment, proportionalOdExitFlow);
          // update bush level turn sending flows
          bush.addTurnSendingFlow((ConjugateEdgeSegment) conjugateExitSegment, proportionalOdExitFlow);
        }
      }
    }

  }
}

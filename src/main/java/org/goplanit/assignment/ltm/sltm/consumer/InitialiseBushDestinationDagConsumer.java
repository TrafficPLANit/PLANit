package org.goplanit.assignment.ltm.sltm.consumer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.goplanit.assignment.ltm.sltm.Bush;
import org.goplanit.assignment.ltm.sltm.BushFlowLabel;
import org.goplanit.graph.directed.acyclic.ACyclicSubGraph;
import org.goplanit.utils.functionalinterface.TriConsumer;
import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.zoning.Centroid;

/**
 * Initialise the origin sLTM bush by including the DAGs for each origin-destination. while adding make sure the appropriate labelling is set to be able to separate overalpping
 * merging and diverging flows. Whenever an o-d is not a single path but comprises multiple (implicit) paths, we split the OD demand proportionally
 * <p>
 * Add the edge segments to the bush and update the turn sending flow accordingly.
 * <p>
 * Consumer can be reused for multiple destinations by updating the destination and demand that goes with it.
 * <p>
 * Key principle when creating initial labels (per destination DAG on the origin bush) is that once we merge with an existing label it follows the same bush to the origin by
 * definition, since there is no cheaper bush available (at the moment). Therefore, we can start following the label we have merged with. It does not matter if multiple entry
 * segments are used (it is a bush), in that case we simply recursively follow them all
 * 
 * @author markr
 *
 */
public class InitialiseBushDestinationDagConsumer implements TriConsumer<DirectedVertex, Double, ACyclicSubGraph> {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(InitialiseBushDestinationDagConsumer.class.getCanonicalName());

  /** the bush to initialise */
  private final Bush originBush;

  /**
   * Constructor
   * 
   * @param originBush to use
   */
  public InitialiseBushDestinationDagConsumer(final Bush originBush) {
    this.originBush = originBush;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void accept(final DirectedVertex currentDestination, final Double originDestinationDemandPcuH, final ACyclicSubGraph currentDestinationDag) {

    originBush.addOriginDemandPcuH(originDestinationDemandPcuH);

    Map<EdgeSegment, Double> destinationDagLabelledFlows = new HashMap<>();
    /* composite label to start with at origin */
    BushFlowLabel currentLabel = originBush.createFlowCompositionLabel();

    /* get topological sorted vertices to process */
    Collection<DirectedVertex> topSortedVertices = currentDestinationDag.topologicalSort(true);
    var vertexIter = topSortedVertices.iterator();
    var currVertex = vertexIter.next();
    if (!(currVertex instanceof Centroid)) {
      LOGGER.warning("root vertex is not centroid, should not happen");
      return;
    }

    /* initialise */
    int numUsedOdExitSegments = currentDestinationDag.getNumberOfEdgeSegments(currVertex, true /* exit segments */);
    for (var exitEdgeSegment : currVertex.getExitEdgeSegments()) {
      destinationDagLabelledFlows.put(exitEdgeSegment, originDestinationDemandPcuH / numUsedOdExitSegments);
    }

    /* pass over destination DAG in topological order propagating o-d flow and initialising labels from origin */
    while (vertexIter.hasNext()) {
      currVertex = vertexIter.next();

      /* aggregate incoming vertex flows */
      boolean anyEntryInBush = false;
      double vertexOdSendingFlow = 0;
      for (var entryEdgeSegment : currVertex.getEntryEdgeSegments()) {
        if (currentDestinationDag.containsEdgeSegment(entryEdgeSegment)) {
          Double entrySegmentSendingFlow = destinationDagLabelledFlows.get(entryEdgeSegment);
          vertexOdSendingFlow += entrySegmentSendingFlow != null ? entrySegmentSendingFlow : 0;
        }
        if (!anyEntryInBush && originBush.hasFlowCompositionLabel(entryEdgeSegment)) {
          anyEntryInBush = true;
        }
      }

      numUsedOdExitSegments = currentDestinationDag.getNumberOfEdgeSegments(currVertex, true /* exit segments */);
      double proportionalOdExitFlow = vertexOdSendingFlow / numUsedOdExitSegments;

      for (var entrySegment : currVertex.getEntryEdgeSegments()) {
        if (!currentDestinationDag.containsEdgeSegment(entrySegment)) {
          continue;
        }

        for (var exitSegment : currVertex.getExitEdgeSegments()) {
          if (currentDestinationDag.containsEdgeSegment(exitSegment)) {
            originBush.addTurnSendingFlow(entrySegment, currentLabel, exitSegment, currentLabel, proportionalOdExitFlow);
            destinationDagLabelledFlows.put(exitSegment, proportionalOdExitFlow);
          }
        }
      }
    }
  }

}

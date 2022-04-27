package org.goplanit.algorithms.shortest;

import java.util.function.BiPredicate;
import java.util.function.Consumer;

import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.network.virtual.ConjugateConnectoidNodes;

/**
 * Conjugate version of shortest path algorithm. The only difference is found in that the original network edge segment costs are now obtained on the turn level, where each
 * conjugate edge segment (turn) collects its cost by means of its incoming original edge segment. Note that for the final turn this means the last edge segemnt's cost is missed
 * which is compensated for at the end
 * 
 * In its current form, it assumes a macroscopic network and macroscopic link segments to operate on
 * 
 * @author markr
 *
 */
public class ConjugateShortestPathGeneralised extends ShortestPathGeneralised {

  /** needed to track the costs to the centroids on the final original edge segments */
  protected final ConjugateConnectoidNodes conjugateConnectoidNodes;

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  protected double[] internalExecute(BiPredicate<Double, Double> verifyVertex, Consumer<EdgeSegment> shortestAlternativeEdgeSegmentConsumer) {
    var vertexMeasuredCost = super.internalExecute(verifyVertex, shortestAlternativeEdgeSegmentConsumer);
    /* add costs of final edge segment of final turns */
    for (var conjugateConnectoid : conjugateConnectoidNodes) {
      ConjugateEdgeSegment finalTurn = conjugateConnectoid.getPRecedingTurn()
    }
  }

  /**
   * For a conjugate network we obtain the conjugate edge segment cost by means of the incoming original edge segment of the turn, i.e., conjugate edge segment
   * 
   * @param edgeSegment to use
   * @return cost of traversing edge segment
   */
  @Override
  protected Double getEdgeSegmentCost(final EdgeSegment edgeSegment) {
    // TODO: costly, to collect, when cached it will be quicker
    return edgeSegmentCosts[(int) ((ConjugateEdgeSegment) edgeSegment).getOriginalAdjcentEdgeSegments().first().getId()];
  }

  /**
   * Constructor for an edge cost based Dijkstra algorithm for finding shortest paths.
   * 
   * @param originalEdgeSegmentCosts  original network (non-conjugate) edge segment costs, both physical and virtual
   * @param numberOfConjugateVertices number of conjugate vertices
   * @param conjugateConnectoidNodes  conjugate connectoid nodes needed to extract the cost on the last original edge segments to reach the original centroids
   */
  public ConjugateShortestPathGeneralised(final double[] originalEdgeSegmentCosts, int numberOfConjugateVertices, ConjugateConnectoidNodes conjugateConnectoidNodes) {
    super(originalEdgeSegmentCosts, numberOfConjugateVertices);
    this.conjugateConnectoidNodes = conjugateConnectoidNodes;
  }

}

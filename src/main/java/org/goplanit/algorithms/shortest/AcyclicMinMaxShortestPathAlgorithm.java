package org.goplanit.algorithms.shortest;

import java.util.Arrays;
import java.util.Collection;

import org.goplanit.graph.directed.acyclic.ACyclicSubGraph;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;

/**
 * Build a min/max shortest path tree for a given origin vertex. This implementation requires an acylic network representation such that the vertices can - and already are -
 * topologically sorted. If the provided topological sorted list of vertices is incorrect undefined behaviour will occurs.
 * <p>
 * Obtaining a topologically sorted list of vertices for a given acyclic (sub)graph can be generated via the functionality on the AcyclicSubGraph implementation
 * 
 * @author markr
 *
 */
public class AcyclicMinMaxShortestPathAlgorithm implements OneToAllShortestPathAlgorithm {

  /** topological ordering to use, which is assumed to be based on the given origin vertex */
  private final Collection<? extends DirectedVertex> topologicalOrder;

  /** the acyclic graph to operate on */
  private final ACyclicSubGraph acyclicSubGraph;

  /** costs of all edge segments known, index reflects id of the graph entity */
  private final double[] edgeSegmentCosts;

  /** number of vertices in parent network, required to create raw result array by contiguous vertex id without the need for any mapping */
  private final int numParentNetworkVertices;

  /**
   * Constructor
   * <p>
   * The edge segment costs should be set for all registered segments on the subgraph while the array itself is expected to match the ids of the edge segments which in turn are
   * based on the number of edge segments on the over-arching network.
   * 
   * @param acyclicSubGraph       the subgraph we are conducting this search on
   * @param topologicalOrder      to use for constructing the min max paths
   * @param edgeSegmentCosts      for all edge segments
   * @param parentNetworkVertices number of vertices in parent network, required to create raw result array by contiguous vertex id without the need for any mapping
   */
  public AcyclicMinMaxShortestPathAlgorithm(final ACyclicSubGraph acyclicSubGraph, final Collection<? extends DirectedVertex> topologicalOrder, final double[] edgeSegmentCosts,
      final int parentNetworkVertices) {
    this.acyclicSubGraph = acyclicSubGraph;
    this.topologicalOrder = topologicalOrder;
    this.edgeSegmentCosts = edgeSegmentCosts;
    this.numParentNetworkVertices = parentNetworkVertices;
  }

  /**
   * Perform a one-to-all min-max path search where we construct both the least and most costliest path from the origin vertex provided to all other vertices in the (sub)graph.
   * Since this is conducted on an acyclic graph all vertices only need to be explored once, which makes it computationally more attractive than the same search on a cyclic graph.
   * 
   * @param currentOrigin to conduct search for
   */
  @Override
  public MinMaxPathResult executeOneToAll(final DirectedVertex currentOrigin) throws PlanItException {
    /* prep cost arrays */
    double[] minCost = new double[numParentNetworkVertices];
    double[] maxCost = new double[numParentNetworkVertices];
    Arrays.fill(minCost, Double.POSITIVE_INFINITY);
    Arrays.fill(maxCost, Double.NEGATIVE_INFINITY);

    /* prep backward link reference arrays */
    EdgeSegment[] minBackwardEdgeSegments = new EdgeSegment[numParentNetworkVertices];
    EdgeSegment[] maxBackwardEdgeSegments = new EdgeSegment[numParentNetworkVertices];

    /* prep starting point */
    minCost[(int) currentOrigin.getId()] = 0.0;
    maxCost[(int) currentOrigin.getId()] = 0.0;

    for (DirectedVertex vertex : topologicalOrder) {
      int vertexIndex = (int) vertex.getId();
      for (EdgeSegment exitEdgeSegment : vertex.getExitEdgeSegments()) {
        if (acyclicSubGraph.containsEdgeSegment(exitEdgeSegment)) {
          double edgeCost = edgeSegmentCosts[(int) exitEdgeSegment.getId()];
          int downstreamVertexIndex = (int) exitEdgeSegment.getDownstreamVertex().getId();

          /* min cost update */
          double foundCostToDownstreamVertex = minCost[vertexIndex] + edgeCost;
          if (foundCostToDownstreamVertex < minCost[downstreamVertexIndex]) {
            minCost[downstreamVertexIndex] = foundCostToDownstreamVertex;
            minBackwardEdgeSegments[downstreamVertexIndex] = exitEdgeSegment;
          }

          /* max cost update */
          foundCostToDownstreamVertex = maxCost[vertexIndex] + edgeCost;
          if (foundCostToDownstreamVertex >= maxCost[downstreamVertexIndex]) {
            maxCost[downstreamVertexIndex] = foundCostToDownstreamVertex;
            maxBackwardEdgeSegments[downstreamVertexIndex] = exitEdgeSegment;
          }
        }
      }
    }

    return new MinMaxPathResultImpl(minCost, minBackwardEdgeSegments, maxCost, maxBackwardEdgeSegments);
  }

}

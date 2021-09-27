package org.planit.algorithms.shortestpath;

import java.util.Arrays;
import java.util.List;

import org.planit.graph.directed.acyclic.ACyclicSubGraph;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.directed.DirectedVertex;

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
  private final List<? extends DirectedVertex> topologicalOrder;

  /** the acyclic graph to operate on */
  private final ACyclicSubGraph<?, ?, ?> acyclicSubGraph;

  /** costs of all edge segments known, index reflects id of the graph entity */
  private final double[] edgeSegmentCosts;

  /**
   * Constructor
   * <p>
   * The edge segment costs should be set for all registered segments on the subgraph while the array itself is expected to match the ids of the edge segments which in turn are
   * based on the number of edge segments on the overarching network.
   * 
   * @param acyclicSubGraph  the subgraph we are conducting this search on
   * @param topologicalOrder to use for constructing the min max paths
   * @param edgeSegmentCosts for all edge segments
   */
  public AcyclicMinMaxShortestPathAlgorithm(final ACyclicSubGraph<?, ?, ?> acyclicSubGraph, final List<? extends DirectedVertex> topologicalOrder,
      final double[] edgeSegmentCosts) {
    this.acyclicSubGraph = acyclicSubGraph;
    this.topologicalOrder = topologicalOrder;
    this.edgeSegmentCosts = edgeSegmentCosts;
  }

  /**
   * Perform a one-to-all min-max path search where we construct both the least and most costliest path from the origin vertex provided to all other vertices in the (sub)graph.
   * Since this is conducted on an acyclic graph all vertices only need to be explored once, which makes it computationally more attractive than the same search on a cyclic graph.
   * 
   * @param currentOrigin to conduct search for
   */
  @Override
  public ShortestPathResult executeOneToAll(DirectedVertex currentOrigin) throws PlanItException {
    long numberOfVertices = acyclicSubGraph.getNumberOfVertices();

    /* prep cost arrays */
    double[] minCost = new double[(int) numberOfVertices];
    double[] maxCost = new double[(int) numberOfVertices];
    Arrays.fill(minCost, Double.POSITIVE_INFINITY);

    /* prep backward link reference arrays */
    EdgeSegment[] minBackwardEdgeSegments = new EdgeSegment[(int) numberOfVertices];
    EdgeSegment[] maxBackwardEdgeSegments = new EdgeSegment[(int) numberOfVertices];

    /* prep starting point */
    minCost[(int) currentOrigin.getId()] = 0.0;

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

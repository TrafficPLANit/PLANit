package org.goplanit.algorithms.shortest;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.graph.directed.acyclic.ACyclicSubGraph;

/**
 * Build a min/max shortest path tree for a given start vertex based on the configuration used. This implementation requires an acyclic network representation such that the
 * vertices can - and already are - topologically sorted. If the provided topological sorted list of vertices is incorrect undefined behaviour will occurs.
 * <p>
 * Obtaining a topologically sorted list of vertices for a given acyclic (sub)graph can be generated via the functionality on the AcyclicSubGraph implementation
 * 
 * @author markr
 *
 */
public class ShortestPathAcyclicMinMaxGeneralised implements ShortestPathOneToAll, ShortestPathAllToOne {

  /** topological ordering to use, which is assumed to be based on the given origin vertex */
  private final Collection<? extends DirectedVertex> topologicalOrder;

  /** the acyclic graph to operate on */
  private final ACyclicSubGraph acyclicSubGraph;

  /** costs of all edge segments known, index reflects id of the graph entity */
  private final double[] edgeSegmentCosts;

  /** number of vertices in parent network, required to create raw result array by contiguous vertex id without the need for any mapping */
  private final int numParentNetworkVertices;

  /** depending on configuration this function collects vertex at desired edge segment extremity */
  protected Function<EdgeSegment, DirectedVertex> getVertexAtExtreme;

  /** depending on configuration this function collects edge segments in entry or exit direction of vertex */
  protected Function<DirectedVertex, Iterable<? extends EdgeSegment>> getEdgeSegmentsInDirection;

  /**
   * Constructor
   * <p>
   * The edge segment costs should be set for all registered segments on the subgraph while the array itself is expected to match the ids of the edge segments which in turn are
   * based on the number of edge segments on the over-arching network.
   * 
   * @param acyclicSubGraph        the subgraph we are conducting this search on
   * @param updateTopologicalOrder indicate if current topological order can be used, or it should be updated before use
   * @param edgeSegmentCosts       for all edge segments
   * @param parentNetworkVertices  number of vertices in parent network, required to create raw result array by contiguous vertex id without the need for any mapping
   */
  public ShortestPathAcyclicMinMaxGeneralised(final ACyclicSubGraph acyclicSubGraph, boolean updateTopologicalOrder, final double[] edgeSegmentCosts,
      final int parentNetworkVertices) {
    this.acyclicSubGraph = acyclicSubGraph;
    this.topologicalOrder = this.acyclicSubGraph.topologicalSort(updateTopologicalOrder);
    this.edgeSegmentCosts = edgeSegmentCosts;
    this.numParentNetworkVertices = parentNetworkVertices;
  }

  /**
   * Perform a generalised min-max path search where we construct both the least and most costly path from the start vertex provided to all other vertices in the (sub)graph based
   * on the configuration. Since this is conducted on an acyclic graph all vertices only need to be explored once, which makes it computationally more attractive than the same
   * search on a cyclic graph.
   * 
   * @param startVertex to conduct search for
   * @return created result
   */
  public MinMaxPathResultImpl execute(final DirectedVertex startVertex) {
    /* prep cost arrays */
    double[] minCost = new double[numParentNetworkVertices];
    double[] maxCost = new double[numParentNetworkVertices];
    Arrays.fill(minCost, Double.POSITIVE_INFINITY);
    Arrays.fill(maxCost, Double.NEGATIVE_INFINITY);

    /* prep backward link reference arrays */
    EdgeSegment[] minCostNextEdgeSegments = new EdgeSegment[numParentNetworkVertices];
    EdgeSegment[] maxCostNextEdgeSegments = new EdgeSegment[numParentNetworkVertices];

    /* prep starting point */
    minCost[(int) startVertex.getId()] = 0.0;
    maxCost[(int) startVertex.getId()] = 0.0;

    for (DirectedVertex vertex : topologicalOrder) {
      int vertexIndex = (int) vertex.getId();
      var edgeSegments = this.getEdgeSegmentsInDirection.apply(vertex);
      for (EdgeSegment currEdgeSegment : edgeSegments) {
        if (acyclicSubGraph.containsEdgeSegment(currEdgeSegment)) {
          double edgeCost = edgeSegmentCosts[(int) currEdgeSegment.getId()];
          int nextVertexIndex = (int) this.getVertexAtExtreme.apply(currEdgeSegment).getId();

          /* min cost update */
          double foundCostToNextVertex = minCost[vertexIndex] + edgeCost;
          if (foundCostToNextVertex < minCost[nextVertexIndex]) {
            minCost[nextVertexIndex] = foundCostToNextVertex;
            minCostNextEdgeSegments[nextVertexIndex] = currEdgeSegment;
          }

          /* max cost update */
          foundCostToNextVertex = maxCost[vertexIndex] + edgeCost;
          if (foundCostToNextVertex >= maxCost[nextVertexIndex]) {
            maxCost[nextVertexIndex] = foundCostToNextVertex;
            maxCostNextEdgeSegments[nextVertexIndex] = currEdgeSegment;
          }
        }
      }
    }

    return new MinMaxPathResultImpl(minCost, minCostNextEdgeSegments, maxCost, maxCostNextEdgeSegments);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MinMaxPathResult executeAllToOne(DirectedVertex currentDestination) {
    this.getEdgeSegmentsInDirection = ShortestPathSearchUtils.getEdgeSegmentsInDirectionLambda(ShortestSearchType.ALL_TO_ONE);
    this.getVertexAtExtreme = ShortestPathSearchUtils.getVertexFromEdgeSegmentLambda(ShortestSearchType.ALL_TO_ONE);
    return execute(currentDestination);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MinMaxPathResult executeOneToAll(DirectedVertex currentOrigin) {
    this.getEdgeSegmentsInDirection = ShortestPathSearchUtils.getEdgeSegmentsInDirectionLambda(ShortestSearchType.ONE_TO_ALL);
    this.getVertexAtExtreme = ShortestPathSearchUtils.getVertexFromEdgeSegmentLambda(ShortestSearchType.ONE_TO_ALL);
    return execute(currentOrigin);
  }

}

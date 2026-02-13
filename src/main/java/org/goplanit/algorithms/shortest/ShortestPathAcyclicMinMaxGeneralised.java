package org.goplanit.algorithms.shortest;

import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.graph.directed.acyclic.UntypedACyclicSubGraph;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Build a min/max shortest path tree for a given start vertex based on the configuration used. This implementation
 * requires an acyclic network representation such that the vertices can - and already are - topologically sorted.
 * If the provided topological sorted list of vertices is incorrect undefined behaviour will occur.
 * <p>
 * Obtaining a topologically sorted list of vertices for a given acyclic (sub)graph can be generated via the
 * functionality on the AcyclicSubGraph implementation
 * </p>
 * 
 * @author markr
 *
 */
public class ShortestPathAcyclicMinMaxGeneralised implements ShortestPathOneToAll, ShortestPathAllToOne {

  /** topological ordering to use, which is assumed to be based on the given origin vertex */
  private final Collection<? extends DirectedVertex> topologicalOrder;

  /** the acyclic graph to operate on */
  private final UntypedACyclicSubGraph<DirectedVertex,EdgeSegment> acyclicSubGraph;

  /** costs of all edge segments known, index reflects id of the graph entity */
  private final double[] edgeSegmentCosts;

  /** number of vertices in parent network, required to create raw result array by contiguous vertex id without
   * the need for any mapping */
  private final int numParentNetworkVertices;

  /** depending on configuration this function collects vertex at desired edge segment extremity */
  protected Function<EdgeSegment, DirectedVertex> getVertexAtExtreme;

  /** depending on configuration this function collects edge segments in entry or exit direction of vertex */
  protected Function<DirectedVertex, Iterable<? extends EdgeSegment>> getEdgeSegmentsInDirection;

  /**
   * Constructor
   * <p>
   * The edge segment costs should be set for all registered segments on the subgraph while the array itself is
   * expected to match the ids of the edge segments which in turn are based on the number of edge segments on the
   * over-arching network.
   * 
   * @param acyclicSubGraph        the subgraph we are conducting this search on
   * @param updateTopologicalOrder indicate if current topological order can be used, or it should be updated
   *                               before use
   * @param edgeSegmentCosts       for all edge segments
   * @param parentNetworkVertices  number of vertices in parent network, required to create raw result array by
   *                               contiguous vertex id without the need for any mapping
   */
  @SuppressWarnings("unchecked")
  public ShortestPathAcyclicMinMaxGeneralised(
          final UntypedACyclicSubGraph<?,?> acyclicSubGraph,
          boolean updateTopologicalOrder,
          final double[] edgeSegmentCosts,
          final int parentNetworkVertices) {

    this.acyclicSubGraph = (UntypedACyclicSubGraph<DirectedVertex, EdgeSegment>) acyclicSubGraph;
    this.topologicalOrder = this.acyclicSubGraph.topologicalSort(updateTopologicalOrder);
    this.edgeSegmentCosts = edgeSegmentCosts;
    this.numParentNetworkVertices = parentNetworkVertices;
  }

  /**
   * Perform a generalised min-max path search where we construct both the least and most costly path from the
   * start vertex provided to all other vertices in the (sub)graph based on the configuration. Since this is
   * conducted on an acyclic graph all vertices only need to be explored once, which makes it computationally
   * more attractive than the same search on a cyclic graph.
   * 
   * @param startVertex to conduct search for
   * @param searchType used
   * @param bannedThroughVertices set of vertices that do not allow paths to go through them. They may only serve as
   *                              start and/or end points
   * @param minPathFilter to apply, keep when true, discard when false (may be null)
   * @param maxPathFilter to use, keep when true, discard when false (may be null)
   * @return created result
   */
  public MinMaxPathResultImpl execute(
          final DirectedVertex startVertex,
          ShortestSearchType searchType,
          Set<DirectedVertex> bannedThroughVertices,
          Predicate<EdgeSegment> minPathFilter,
          Predicate<EdgeSegment> maxPathFilter) {

    /* prep cost arrays */
    double[] minCost = new double[numParentNetworkVertices];
    double[] maxCost = new double[numParentNetworkVertices];
    Arrays.fill(minCost, Double.POSITIVE_INFINITY);
    Arrays.fill(maxCost, Double.NEGATIVE_INFINITY);
    if(bannedThroughVertices == null){
      bannedThroughVertices = Collections.emptySet();
    }
    if(minPathFilter == null){
      minPathFilter = e -> true;
    }
    if(maxPathFilter == null){
      maxPathFilter = e -> true;
    }

    /* prep backward link reference arrays */
    EdgeSegment[] minCostNextEdgeSegments = new EdgeSegment[numParentNetworkVertices];
    EdgeSegment[] maxCostNextEdgeSegments = new EdgeSegment[numParentNetworkVertices];

    /* prep starting point */
    minCost[(int) startVertex.getId()] = 0.0;
    maxCost[(int) startVertex.getId()] = 0.0;

    for (DirectedVertex vertex : topologicalOrder) {
      int vertexIndex = (int) vertex.getId();
      var edgeSegments = this.getEdgeSegmentsInDirection.apply(vertex);

      if(bannedThroughVertices.contains(vertex) && vertex != startVertex){
        continue;
      }

      for (EdgeSegment currEdgeSegment : edgeSegments) {
        if (acyclicSubGraph.containsEdgeSegment(currEdgeSegment)) {
          double edgeCost = edgeSegmentCosts[(int) currEdgeSegment.getId()];
          int nextVertexIndex = (int) this.getVertexAtExtreme.apply(currEdgeSegment).getId();

          /* min cost update */
          double minCostCurrVertex = minCost[vertexIndex];
          double foundCostToNextVertex = minCost[vertexIndex] + edgeCost;
          double minCostNextVertex = minCost[nextVertexIndex];
          if (foundCostToNextVertex < minCostNextVertex && minPathFilter.test(currEdgeSegment)) {
            minCost[nextVertexIndex] = foundCostToNextVertex;
            minCostNextEdgeSegments[nextVertexIndex] = currEdgeSegment;
          }

          /* max cost update */
          foundCostToNextVertex = maxCost[vertexIndex] + edgeCost;
          if (foundCostToNextVertex >= maxCost[nextVertexIndex] && maxPathFilter.test(currEdgeSegment)) {
            maxCost[nextVertexIndex] = foundCostToNextVertex;
            maxCostNextEdgeSegments[nextVertexIndex] = currEdgeSegment;
          }
        }
      }
    }

    return new MinMaxPathResultImpl(
        startVertex,
        searchType,
        minCost,
        minCostNextEdgeSegments,
        maxCost,
        maxCostNextEdgeSegments,
        edgeSegmentCosts.length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MinMaxPathResult executeAllToOne(
          DirectedVertex currentDestination, Set<DirectedVertex> bannedThroughVertices) {
    this.getEdgeSegmentsInDirection =
            ShortestPathSearchUtils.getEdgeSegmentsInDirectionLambda(ShortestSearchType.ALL_TO_ONE);
    this.getVertexAtExtreme = ShortestPathSearchUtils.getVertexFromEdgeSegmentLambda(ShortestSearchType.ALL_TO_ONE);
    return execute(
        currentDestination, ShortestSearchType.ALL_TO_ONE, bannedThroughVertices, null, null);
  }

  /**
   * min max variant of running a shortest path with a single filer. Only now applied to min/max path separately
   *
   * @param currentDestination to consider
   * @param minPathFilter to use (may be null)
   * @param maxPathFilter to use (may be null)
   * @return found min/max paths result
   */
  public MinMaxPathResult executeAllToOneWithFilter(
      DirectedVertex currentDestination,
      Predicate<EdgeSegment> minPathFilter,
      Predicate<EdgeSegment> maxPathFilter) {

    this.getEdgeSegmentsInDirection =
        ShortestPathSearchUtils.getEdgeSegmentsInDirectionLambda(ShortestSearchType.ALL_TO_ONE);
    this.getVertexAtExtreme = ShortestPathSearchUtils.getVertexFromEdgeSegmentLambda(ShortestSearchType.ALL_TO_ONE);
    return execute(
        currentDestination, ShortestSearchType.ALL_TO_ONE, null, minPathFilter, maxPathFilter);
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public MinMaxPathResult executeAllToOne(DirectedVertex currentDestination){
    return executeAllToOne(currentDestination, Collections.emptySet());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MinMaxPathResult executeOneToAll(
          DirectedVertex currentOrigin, Set<DirectedVertex> bannedThroughVertices) {
    this.getEdgeSegmentsInDirection =
            ShortestPathSearchUtils.getEdgeSegmentsInDirectionLambda(ShortestSearchType.ONE_TO_ALL);
    this.getVertexAtExtreme = ShortestPathSearchUtils.getVertexFromEdgeSegmentLambda(ShortestSearchType.ONE_TO_ALL);
    return execute(
        currentOrigin, ShortestSearchType.ONE_TO_ALL, bannedThroughVertices, null, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MinMaxPathResult executeOneToAll(DirectedVertex currentOrigin){
    return executeOneToAll(currentOrigin, Collections.emptySet());
  }

}

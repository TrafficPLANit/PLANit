package org.goplanit.algorithms.shortest;

import java.util.function.BiPredicate;

import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;

/**
 * Dijkstra's shortest path algorithm
 * 
 * Dijkstra's shortest path is a one-to-all (or all-to-one) implementation of the shortest path algorithm based on the generalized costs on each link segment (edge). The costs
 * should be provided upon instantiation and are reused whenever an execution conditional on the chosen source/destination node is performed.
 * 
 * In its current form, it assumes a macroscopic network and macroscopic link segments to operate on
 * 
 * @author markr
 *
 */
public class ShortestPathDijkstra extends ShortestPathGeneralised implements ShortestPathOneToAll, ShortestPathAllToOne {

  /**
   * Track incoming edge segment that is shortest for each vertex in this array
   */
  protected EdgeSegment[] shortestEdgeSegmentOfVertex;

  /**
   * predicate for Dijkstra where shortest means less cost than existing cost, so only cheaper paths overwrite an existing shortest path to a node
   */
  protected static final BiPredicate<Double, Double> isShorterPredicate = (currCost, computedCost) -> {
    return currCost > computedCost;
  };

  /**
   * Constructor for an edge cost based Dijkstra algorithm for finding shortest paths.
   * 
   * @param edgeSegmentCosts     Edge segment costs
   * @param numberOfEdgeSegments Edge segments, both physical and connectoid
   * @param numberOfVertices     Vertices, both nodes and centroids
   */
  public ShortestPathDijkstra(final double[] edgeSegmentCosts, int numberOfEdgeSegments, int numberOfVertices) {
    super(edgeSegmentCosts, numberOfEdgeSegments, numberOfVertices);
  }

  /**
   * Construct shortest paths from source node to all other nodes in the network based on directed LinkSegment edges
   * 
   * @param currentOrigin origin vertex of source node
   * @return shortest path result that can be used to extract paths
   */
  @Override
  public ShortestPathOneToAllResult executeOneToAll(DirectedVertex currentOrigin) {
    this.currentSource = currentOrigin;
    this.shortestEdgeSegmentOfVertex = new EdgeSegment[numberOfVertices];

    /*
     * found shortest path costs to each vertex for current origin. When deemed shortest, the incoming edge segment is stored on the array
     */
    double[] vertexMeasuredCost = super.executeOneToAll(isShorterPredicate, es -> shortestEdgeSegmentOfVertex[(int) es.getDownstreamVertex().getId()] = es);

    return new ShortestPathResultGeneralised(vertexMeasuredCost, shortestEdgeSegmentOfVertex, ShortestSearchType.ONE_TO_ALL);
  }

  /**
   * Construct shortest paths from all nodes to a single sink node in the network based on directed Link segment edges
   * 
   * @param currentDestination destination vertex
   * @return shortest path result that can be used to extract paths
   * @throws PlanItException thrown if an error occurs
   */
  @Override
  public ShortestPathAllToOneResult executeAllToOne(DirectedVertex currentDestination) {
    this.currentSource = currentDestination;
    this.shortestEdgeSegmentOfVertex = new EdgeSegment[numberOfVertices];

    /*
     * found shortest path costs from each vertex to current destination. When deemed shortest, the outgoing edge segment is stored on the array
     */
    double[] vertexMeasuredCost = super.executeAllToOne(isShorterPredicate, es -> shortestEdgeSegmentOfVertex[(int) es.getUpstreamVertex().getId()] = es);

    return new ShortestPathResultGeneralised(vertexMeasuredCost, shortestEdgeSegmentOfVertex, ShortestSearchType.ALL_TO_ONE);
  }
}

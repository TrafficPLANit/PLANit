package org.goplanit.algorithms.shortest;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;

import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.misc.Pair;

/**
 * Dijkstra's shortest path algorithm
 * 
 * Dijkstra's shortest path is a one-to-all implementation of the shortest path algorithm based on the generalized costs on each link segment (edge). The costs should be provided
 * upon instantiation and are reused whenever a One-To-All execution conditional on the chosen source node is performed. Note that while it is one-to-all the direction of the
 * search can be inverted such that it effectively becomes an all-to-one search.
 * 
 * In its current form, it assumes a macroscopic network and macroscopic link segments to operate on
 * 
 * @author markr
 *
 */
public class ShortestPathGeneralised {

  /** Comparator to sort based on the second elements minimum value (ascending order) */
  private static final Comparator<Pair<DirectedVertex, Double>> pairSecondComparator = Comparator.comparing(Pair<DirectedVertex, Double>::second, (f1, f2) -> {
    return f1.compareTo(f2);
  });

  /**
   * Generalised shortest-X search
   * 
   * @param verifyVertex                           predicate to test if the new cost to reach vertex is considered shortest compared to existing cost
   * @param shortestAlternativeEdgeSegmentConsumer process the "shortest" alternative edge segment when verified by the predicate
   * @return found shortest costs for vertices, where the most recent found "shortest" cost is the one available in the array
   */
  private double[] internalExecute(BiPredicate<Double, Double> verifyVertex, Consumer<EdgeSegment> shortestAlternativeEdgeSegmentConsumer) {
    boolean[] vertexVisited = new boolean[numberOfVertices];

    // track measured cost for each vertex
    double[] vertexMeasuredCost = new double[numberOfVertices];
    Arrays.fill(vertexMeasuredCost, Double.MAX_VALUE);
    vertexMeasuredCost[(int) currentSource.getId()] = 0.0;
    // precedingVertex for each vertex (used to reconstruct path)
    EdgeSegment[] nextEdgeSegmentByVertex = new EdgeSegment[numberOfVertices];
    Arrays.fill(nextEdgeSegmentByVertex, null);

    PriorityQueue<Pair<DirectedVertex, Double>> openVertices = new PriorityQueue<Pair<DirectedVertex, Double>>(numberOfVertices, pairSecondComparator);
    openVertices.add(Pair.of(currentSource, 0.0)); // cost to reach self is zero

    // collect cheapest cost and expand the vertex if not already visited
    while (!openVertices.isEmpty()) {
      Pair<DirectedVertex, Double> cheapestNextVertex = openVertices.poll();
      DirectedVertex currentVertex = cheapestNextVertex.first();
      int currentVertexId = (int) currentVertex.getId();
      double currentCost = cheapestNextVertex.second();
      if (vertexVisited[currentVertexId]) {
        continue;
      }

      vertexVisited[currentVertexId] = true;

      // vertex has not yet been processed, if it has then a cheaper path which has
      // already been found and we continue with the next entry
      // track all adjacent edge segments for possible improved shortest paths
      var edgeSegments = this.getEdgeSegmentsInDirection.apply(currentVertex);
      for (var adjacentEdgeSegment : edgeSegments) {
        double currentEdgeSegmentCost = edgeSegmentCosts[(int) adjacentEdgeSegment.getId()];
        if (currentEdgeSegmentCost < Double.MAX_VALUE) {

          DirectedVertex adjacentVertex = this.getVertexAtExtreme.apply(adjacentEdgeSegment);
          int adjacentVertexId = (int) adjacentVertex.getId();
          if (!vertexVisited[adjacentVertexId]) {
            double adjacentVertexCost = vertexMeasuredCost[adjacentVertexId];
            double computedCostToReachAdjacentVertex = currentCost + currentEdgeSegmentCost;

            if (verifyVertex.test(adjacentVertexCost, computedCostToReachAdjacentVertex)) {
              vertexMeasuredCost[adjacentVertexId] = computedCostToReachAdjacentVertex; // update cost
              openVertices.add(Pair.of(adjacentVertex, computedCostToReachAdjacentVertex)); // place on queue

              shortestAlternativeEdgeSegmentConsumer.accept(adjacentEdgeSegment); // process "shortest" edge segment
            }
          }
        }
      }
    }

    return vertexMeasuredCost;
  }

  /**
   * Reference to starting point for search for which we collect shortest paths from/to
   */
  protected DirectedVertex currentSource = null;

  /**
   * Track the cost for each edge to determine shortest paths
   */
  protected final double[] edgeSegmentCosts;

  /**
   * The number of edge segments considered
   */
  protected final int numberOfEdgeSegments;

  /**
   * The number of vertices in the network
   */
  protected final int numberOfVertices;

  /** depending on configuration this function collects vertex at desired edge segment extremity */
  protected Function<EdgeSegment, DirectedVertex> getVertexAtExtreme;

  /** depending on configuration this function collects edge segments in entry or exit direction of vertex */
  protected Function<DirectedVertex, Iterable<? extends EdgeSegment>> getEdgeSegmentsInDirection;

  /**
   * Generalised shortest-X search where the search type determines to which of the other methods to delegate, oneToAll or AllToOne.
   * 
   * @param searchType                          to use
   * @param verifyVertex                        predicate to test if the new cost to reach vertex is considered shortest compared to existing cost
   * @param shortestIncomingEdgeSegmentConsumer process the "shortest" incoming edge segment when verified by the predicate
   * @return found shortest costs for vertices, where the most recent found "shortest" cost is the one available in the array
   */
  protected double[] execute(ShortestSearchType searchType, BiPredicate<Double, Double> verifyVertex, Consumer<EdgeSegment> shortestIncomingEdgeSegmentConsumer) {
    this.getEdgeSegmentsInDirection = ShortestPathSearchUtils.getEdgeSegmentsInDirectionLambda(searchType);
    this.getVertexAtExtreme = ShortestPathSearchUtils.getVertexFromEdgeSegmentLambda(searchType);
    return internalExecute(verifyVertex, shortestIncomingEdgeSegmentConsumer);
  }

  /**
   * Generalised one-to-all shortest-X search where the test whether or not an alternative edge segment is shortest is dictated by the provided predicate while the processing of
   * the alternative edge segment when the predicate tests as true is outsourced to the provided consumer. It is however assumed that only a single cost is stored per vertex
   * resulting in the returned vertex measured cost array
   * 
   * @param verifyVertex                        predicate to test if the new cost to reach vertex is considered shortest compared to existing cost
   * @param shortestIncomingEdgeSegmentConsumer process the "shortest" incoming edge segment when verified by the predicate
   * @return found shortest costs for vertices, where the most recent found "shortest" cost is the one available in the array
   */
  protected double[] executeOneToAll(BiPredicate<Double, Double> verifyVertex, Consumer<EdgeSegment> shortestIncomingEdgeSegmentConsumer) {
    return execute(ShortestSearchType.ONE_TO_ALL, verifyVertex, shortestIncomingEdgeSegmentConsumer);
  }

  /**
   * Generalised all-to-one shortest-X search where the test whether or not an alternative edge segment is shortest is dictated by the provided predicate while the processing of
   * the alternative edge segment when the predicate tests as true is outsourced to the provided consumer. It is however assumed that only a single cost is stored per vertex
   * resulting in the returned vertex measured cost array
   * 
   * @param verifyVertex                        predicate to test if the new cost to reach vertex is considered shortest compared to existing cost
   * @param shortestIncomingEdgeSegmentConsumer process the "shortest" incoming edge segment when verified by the predicate
   * @return found shortest costs for vertices, where the most recent found "shortest" cost is the one available in the array
   */
  protected double[] executeAllToOne(BiPredicate<Double, Double> verifyVertex, Consumer<EdgeSegment> shortestIncomingEdgeSegmentConsumer) {
    return execute(ShortestSearchType.ALL_TO_ONE, verifyVertex, shortestIncomingEdgeSegmentConsumer);
  }

  /**
   * Constructor for an edge cost based Dijkstra algorithm for finding shortest paths.
   * 
   * @param edgeSegmentCosts     Edge segment costs
   * @param numberOfEdgeSegments Edge segments, both physical and connectoid
   * @param numberOfVertices     Vertices, both nodes and centroids
   */
  public ShortestPathGeneralised(final double[] edgeSegmentCosts, int numberOfEdgeSegments, int numberOfVertices) {
    this.edgeSegmentCosts = edgeSegmentCosts;
    this.numberOfVertices = numberOfVertices;
    this.numberOfEdgeSegments = numberOfEdgeSegments;
  }

}

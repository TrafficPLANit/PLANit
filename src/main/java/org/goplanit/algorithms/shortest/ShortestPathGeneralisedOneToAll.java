package org.goplanit.algorithms.shortest;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.misc.Pair;

/**
 * Dijkstra's shortest path algorithm
 * 
 * Dijkstra's shortest path is a one-to-all implementation of the shortest path algorithm based on the generalized costs on each link segment (edge). The costs should be provided
 * upon instantiation and are reused whenever a One-To-All execution conditional on the chosen source node is performed.
 * 
 * In its current form, it assumes a macroscopic network and macroscopic link segments to operate on
 * 
 * @author markr
 *
 */
public class ShortestPathGeneralisedOneToAll {

  /**
   * Reference to current origin for which we have collected shortest paths on a ONE-TO-ALL basis
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

  /** Comparator to sort based on the second elements minimum value (ascending order) */
  protected static final Comparator<Pair<DirectedVertex, Double>> pairSecondComparator = Comparator.comparing(Pair<DirectedVertex, Double>::second, (f1, f2) -> {
    return f1.compareTo(f2);
  });

  /**
   * Generalised one-to-all shortest-X search where the test whether or not an incoming edge segment is shortest is dictated by the provided predicate while the processing of the
   * incoming edge segment when the predicate tests as true is outsourced to the provided consumer. It is however assumed that only a single cost is stored per vertex resulting in
   * the returned vertex measured cost array
   * 
   * @param verifyVertex                        predicate to test if the new cost to reach vertex is considered shortest compared to existing cost
   * @param shortestIncomingEdgeSegmentConsumer process the "shortest" incoming edge segment when verified by the predicate
   * @return found shortest costs for vertices, where the most recent found "shortest" cost is the one available in the array
   */
  protected double[] executeOneToAll(BiPredicate<Double, Double> verifyVertex, Consumer<EdgeSegment> shortestIncomingEdgeSegmentConsumer) {
    boolean[] vertexVisited = new boolean[numberOfVertices];

    // track measured cost for each vertex
    double[] vertexMeasuredCost = new double[numberOfVertices];
    Arrays.fill(vertexMeasuredCost, Double.MAX_VALUE);
    vertexMeasuredCost[(int) currentSource.getId()] = 0.0;
    // precedingVertex for each vertex (used to reconstruct path)
    EdgeSegment[] incomingEdgeSegment = new EdgeSegment[numberOfVertices];
    Arrays.fill(incomingEdgeSegment, null);

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

      for (var adjacentEdgeSegment : currentVertex.getExitEdgeSegments()) {
        double currentEdgeSegmentCost = edgeSegmentCosts[(int) adjacentEdgeSegment.getId()];
        if (currentEdgeSegmentCost < Double.MAX_VALUE) {

          DirectedVertex adjacentVertex = adjacentEdgeSegment.getDownstreamVertex();
          int adjacentVertexId = (int) adjacentVertex.getId();
          if (!vertexVisited[adjacentVertexId]) {
            double adjacentVertexCost = vertexMeasuredCost[adjacentVertexId];
            double computedCostToReachAdjacentVertex = currentCost + currentEdgeSegmentCost;

            if (verifyVertex.test(adjacentVertexCost, computedCostToReachAdjacentVertex)) {
              vertexMeasuredCost[adjacentVertexId] = computedCostToReachAdjacentVertex; // update cost
              openVertices.add(Pair.of(adjacentVertex, computedCostToReachAdjacentVertex)); // place on queue

              shortestIncomingEdgeSegmentConsumer.accept(adjacentEdgeSegment); // process "shortest" edge segment
            }
          }
        }
      }
    }

    return vertexMeasuredCost;
  }

  /**
   * Constructor for an edge cost based Dijkstra algorithm for finding shortest paths.
   * 
   * @param edgeSegmentCosts     Edge segment costs
   * @param numberOfEdgeSegments Edge segments, both physical and connectoid
   * @param numberOfVertices     Vertices, both nodes and centroids
   */
  public ShortestPathGeneralisedOneToAll(final double[] edgeSegmentCosts, int numberOfEdgeSegments, int numberOfVertices) {
    this.edgeSegmentCosts = edgeSegmentCosts;
    this.numberOfVertices = numberOfVertices;
    this.numberOfEdgeSegments = numberOfEdgeSegments;
  }

}

package org.planit.algorithms.shortestpath;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.Vertex;
import org.planit.utils.graph.directed.DirectedVertex;
import org.planit.utils.misc.Pair;

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
public class DijkstraShortestPathAlgorithm implements OneToAllShortestPathAlgorithm {

  /**
   * Reference to current origin for which we have collected shortest paths on a ONE-TO-ALL basis
   */
  protected Vertex currentOrigin = null;

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
   * Constructor for an edge cost based Dijkstra algorithm for finding shortest paths.
   * 
   * @param edgeSegmentCosts     Edge segment costs
   * @param numberOfEdgeSegments Edge segments, both physical and connectoid
   * @param numberOfVertices     Vertices, both nodes and centroids
   */
  public DijkstraShortestPathAlgorithm(final double[] edgeSegmentCosts, int numberOfEdgeSegments, int numberOfVertices) {
    this.edgeSegmentCosts = edgeSegmentCosts;
    this.numberOfVertices = numberOfVertices;
    this.numberOfEdgeSegments = numberOfEdgeSegments;
  }

  /**
   * Construct shortest paths from source node to all other nodes in the network based on directed LinkSegment edges
   * 
   * @param currentOrigin origin vertex of source node
   * @return shortest path result that can be used to extract paths
   * @throws PlanItException thrown if an error occurs
   */
  @Override
  public ShortestPathResult executeOneToAll(DirectedVertex currentOrigin) throws PlanItException {
    boolean[] vertexVisited = new boolean[numberOfVertices];
    this.currentOrigin = currentOrigin;

    // track measured cost for each vertex
    double[] vertexMeasuredCost = new double[numberOfVertices];
    Arrays.fill(vertexMeasuredCost, Double.MAX_VALUE);
    vertexMeasuredCost[(int) currentOrigin.getId()] = 0.0;
    // precedingVertex for each vertex (used to reconstruct path)
    EdgeSegment[] incomingEdgeSegment = new EdgeSegment[numberOfVertices];
    Arrays.fill(incomingEdgeSegment, null);

    PriorityQueue<Pair<DirectedVertex, Double>> openVertices = new PriorityQueue<Pair<DirectedVertex, Double>>(numberOfVertices, pairSecondComparator);
    openVertices.add(Pair.of(currentOrigin, 0.0)); // cost to reach self is zero

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

      // vertex has not yet been processed, if it has than a cheaper path which has
      // already been found and we continue with the next entry
      // track all adjacent edge segments for possible improved shortest paths

      for (EdgeSegment adjacentEdgeSegment : currentVertex.getExitEdgeSegments()) {
        double currentEdgeSegmentCost = edgeSegmentCosts[(int) adjacentEdgeSegment.getId()];
        if (currentEdgeSegmentCost < Double.MAX_VALUE) {

          DirectedVertex adjacentVertex = adjacentEdgeSegment.getDownstreamVertex();
          int adjacentVertexId = (int) adjacentVertex.getId();
          if (!vertexVisited[adjacentVertexId]) {
            double adjacentVertexCost = vertexMeasuredCost[adjacentVertexId];
            double computedCostToReachAdjacentVertex = currentCost + currentEdgeSegmentCost;

            // Whenever the adjacent vertex can be reached in less cost than currently is
            // the case, place it on the queue for expanding and update its cost
            if (adjacentVertexCost > computedCostToReachAdjacentVertex) {
              vertexMeasuredCost[adjacentVertexId] = computedCostToReachAdjacentVertex;
              incomingEdgeSegment[adjacentVertexId] = adjacentEdgeSegment;
              openVertices.add(Pair.of(adjacentVertex, computedCostToReachAdjacentVertex)); // place on queue
            }
          }
        }
      }
    }
    return new ShortestPathResultImpl(vertexMeasuredCost, incomingEdgeSegment);
  }
}

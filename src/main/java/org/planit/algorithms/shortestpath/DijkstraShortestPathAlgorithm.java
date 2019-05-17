package org.planit.algorithms.shortestpath;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.planit.exceptions.PlanItException;
import org.planit.network.EdgeSegment;
import org.planit.network.Vertex;
import org.planit.utils.Pair;

/**
 * Dijkstra's shortest path algorithm
 * 
 * Dijkstra's shortest path is a one-to-all implementation of the shortest path
 * algorithm based on the generalized costs on each link segment (edge). The
 * costs should be provided upon instantiation and are reused whenever a
 * One-To-All execution conditional on the chosen source node is performed.
 * 
 * In its current form, it assumes a macroscopic network and macroscopic link
 * segments to operate on
 * 
 * @author markr
 *
 */
public class DijkstraShortestPathAlgorithm implements ShortestPathAlgorithm {

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(DijkstraShortestPathAlgorithm.class.getName());

    /**
     * Reference to current origin for which we have collected shortest paths on a
     * ONE-TO-ALL basis
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

    /**
     * Constructor for an edge cost based Dijkstra algorithm for finding shortest
     * paths.
     * 
     * @param edgeSegmentCosts
     *            Edge segment costs
     * @param numberOfEdgeSegments
     *            Edge segments, both physical and connectoid
     * @param numberOfVertices
     *            Vertices, both nodes and centroids
     */
    public DijkstraShortestPathAlgorithm(final double[] edgeSegmentCosts, int numberOfEdgeSegments,
            int numberOfVertices) {
        this.edgeSegmentCosts = edgeSegmentCosts;
        this.numberOfVertices = numberOfVertices;
        this.numberOfEdgeSegments = numberOfEdgeSegments;
    }

    /**
     * Construct shortest paths from source node to all other nodes in the network
     * based on directed LinkSegment edges
     * 
     * @param currentOrigin
     *            origin vertex of source node
     * @return array of pairs containing, for each vertex (array index), the cost to
     *         reach the vertex and the link segment it is reached from with the
     *         shortest cost.
     * @throws PlanItException
     *             thrown if an error occurss
     */
    public Pair<Double, EdgeSegment>[] executeOneToAll(@Nonnull Vertex currentOrigin) throws PlanItException {
        boolean[] vertexVisited = new boolean[numberOfVertices];
        this.currentOrigin = currentOrigin;
        @SuppressWarnings("unchecked")
        Pair<Double, EdgeSegment>[] vertexCost = new Pair[numberOfVertices];
        Arrays.fill(vertexCost, new Pair<Double, EdgeSegment>(Double.POSITIVE_INFINITY, null));

        // Use priority queue to identify the current cheapest cost (second element) to
        // reach each vertex (first element)
        Comparator<Pair<Vertex, Double>> pairSecondComparator = 
                Comparator.comparing(Pair<Vertex, Double>::getSecond,
                (f1, f2) -> {
                    return f1.compareTo(f2);
                });

        PriorityQueue<Pair<Vertex, Double>> openVertices = new PriorityQueue<Pair<Vertex, Double>>(numberOfVertices,
                pairSecondComparator);
        openVertices.add(new Pair<Vertex, Double>(currentOrigin, 0.0)); // cost to reach self is zero

        // collect cheapest cost and expand the vertex if not already visited
        while (!openVertices.isEmpty()) {
            Pair<Vertex, Double> cheapestNextVertex = openVertices.poll();
            Vertex currentNode = cheapestNextVertex.getFirst();
            double currentCost = cheapestNextVertex.getSecond();
            if (vertexVisited[(int) currentNode.getId()]) {
                continue;
            }
            vertexVisited[(int) currentNode.getId()] = true;

            // vertex has not yet been processed, if it has than a cheaper path which has
            // already been found and we continue with the next entry
            // track all adjacent edge segments for possible improved shortest paths

            for (EdgeSegment adjacentLinkSegment : currentNode.exitEdgeSegments) {
                double currentEdgeSegmentCost = edgeSegmentCosts[(int) adjacentLinkSegment.getId()];
                if (currentEdgeSegmentCost < Double.POSITIVE_INFINITY) {
                    Vertex adjacentVertex = adjacentLinkSegment.getDownstreamVertex();
                    int adjacentVertexId = (int) adjacentVertex.getId();
                    Pair<Double, EdgeSegment> adjacentVertexDataPair = vertexCost[adjacentVertexId];
                    double computedCostToReachAdjacentVertex = currentCost + currentEdgeSegmentCost;
                    // Whenever the adjacent vertex can be reached in less cost than currently is
                    // the case, place it on the queue for expanding and update its cost
                    if (!vertexVisited[adjacentVertexId] && (adjacentVertexId != currentOrigin.getId())
                            && (adjacentVertexDataPair.getFirst() > computedCostToReachAdjacentVertex)) {
                        vertexCost[adjacentVertexId] = 
                                new Pair<Double, EdgeSegment>(computedCostToReachAdjacentVertex,adjacentLinkSegment); // update cost and path
                        openVertices.add(new Pair<Vertex, Double>(adjacentVertex, computedCostToReachAdjacentVertex)); // place
                                                                                                                       // on
                                                                                                                       // queue
                    }
                }
            }

        }
        return vertexCost;
    }
}

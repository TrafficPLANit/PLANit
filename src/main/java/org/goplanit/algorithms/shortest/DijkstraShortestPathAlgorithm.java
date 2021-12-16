package org.goplanit.algorithms.shortest;

import java.util.function.BiPredicate;

import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;

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
public class DijkstraShortestPathAlgorithm extends OneToAllShortestGeneralisedAlgorithm implements OneToAllShortestPathAlgorithm {

  protected EdgeSegment[] incomingEdgeSegment;

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
  public DijkstraShortestPathAlgorithm(final double[] edgeSegmentCosts, int numberOfEdgeSegments, int numberOfVertices) {
    super(edgeSegmentCosts, numberOfEdgeSegments, numberOfVertices);
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
    this.currentOrigin = currentOrigin;
    this.incomingEdgeSegment = new EdgeSegment[numberOfVertices];

    /*
     * found shortest path costs to each vertex for current origin. When deemed shortest, the incoming edge segment is stored on the array
     */
    double[] vertexMeasuredCost = super.executeOneToAll(isShorterPredicate, es -> incomingEdgeSegment[(int) es.getDownstreamVertex().getId()] = es);

    return new ShortestPathResultImpl(vertexMeasuredCost, incomingEdgeSegment);
  }
}

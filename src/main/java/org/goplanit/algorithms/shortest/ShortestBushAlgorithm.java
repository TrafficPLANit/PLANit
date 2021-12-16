package org.goplanit.algorithms.shortest;

import java.util.function.BiPredicate;

import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.math.Precision;

/**
 * Shortest bush algorithm.
 * 
 * Shortest bush algorithm is a one-to-all implementation of all (equal cost) implicit shortest bush comprising of all equal cost paths based on the generalized costs on each link
 * segment (edge). It is identical to Dijkstra's shortest path algorithm except that it creates a bush rooted at the origin towards each node, where each node stores all its equal
 * cheapest predecessor nodes from which the bush can be extracted when traversing it in reverse order (from node back to origin)
 * 
 * In its current form, it assumes a macroscopic network and macroscopic link segments to operate on
 * 
 * @author markr
 *
 */
public class ShortestBushAlgorithm extends OneToAllShortestGeneralisedAlgorithm implements OneToAllShortestBushAlgorithm {

  /**
   * predicate for shortest bush means less or equal cost compared to existing cost, so cheaper and equal cost paths are considered
   */
  protected static final BiPredicate<Double, Double> isShorterOrEqualPredicate = (currCost, computedCost) -> {
    return Precision.greaterEqual(currCost, computedCost, Precision.EPSILON_15);
  };

  private void bla(EdgeSegment es) {

  }

  /**
   * Constructor for an edge cost based algorithm for finding shortest bushes.
   * 
   * @param edgeSegmentCosts     Edge segment costs
   * @param numberOfEdgeSegments Edge segments, both physical and connectoid
   * @param numberOfVertices     Vertices, both nodes and centroids
   */
  public ShortestBushAlgorithm(final double[] edgeSegmentCosts, int numberOfEdgeSegments, int numberOfVertices) {
    super(edgeSegmentCosts, numberOfEdgeSegments, numberOfVertices);
  }

  /**
   * Construct shortest bush result from source node to all other nodes in the network based on directed LinkSegment edges
   * 
   * @param currentOrigin origin vertex of source node
   * @return shortest bush result that can be used to extract bushes
   * @throws PlanItException thrown if an error occurs
   */
  @Override
  public ShortestBushResult executeOneToAll(DirectedVertex currentOrigin) throws PlanItException {
    // TODO:
    // ideally we rewrite paths as being a special resitricted case of a bush, in that situation we can derive it from this which would be nice instead
    // of the separate classes and interfaces

    this.currentOrigin = currentOrigin;

    // this.incomingEdgeSegment = new EdgeSegment[numberOfVertices];

    /*
     * found shortest path costs to each vertex for current origin. When deemed shortest, the incoming edge segment is stored on the array
     */
    double[] vertexMeasuredCost = super.executeOneToAll(isShorterOrEqualPredicate, this::bla);

    return null;
  }
}

package org.goplanit.algorithms.shortest;

import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;

/**
 * Dijkstra's shortest path algorithm
 * <p>
 * Dijkstra's shortest path is a one-to-all (or all-to-one) implementation of the shortest path algorithm based on the generalized costs on each link segment (edge). The costs
 * should be provided upon instantiation and are reused whenever an execution conditional on the chosen source/destination node is performed.
 * </p>
 * <p>
 * In its current form, it assumes a macroscopic network and macroscopic link segments to operate on
 * </p>
 * 
 * @author markr
 *
 */
public class ShortestPathDijkstra extends ShortestPathGeneralised implements ShortestPathOneToAll, ShortestPathAllToOne {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(ShortestPathDijkstra.class.getCanonicalName());

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
   * Execute Dijkstra shortest path search based on search type, start vertex and consumer that deals with finding a new shorter(equally short) alternative edge segment for a given
   * vertex
   * 
   * @param searchType                      to apply
   * @param startVertex                     to use
   * @param shortestNextEdgeSegmentConsumer to apply to a new shortest edge segment found for a given vertex
   * @return shortest path results which, depending on search type, can take on various derived forms of this base result class
   */
  private ShortestPathResult dijkstraExecute(
          ShortestSearchType searchType, DirectedVertex startVertex, Consumer<EdgeSegment> shortestNextEdgeSegmentConsumer) {
    this.currentSource = startVertex;
    this.shortestEdgeSegmentOfVertex = new EdgeSegment[numberOfVertices];

    /* shortest path costs to each vertex for start vertex */
    double[] vertexMeasuredCost = super.execute(searchType, isShorterPredicate, shortestNextEdgeSegmentConsumer);
    /* pass on to result object for user friendly dissemination */
    return new ShortestPathResultGeneralised(vertexMeasuredCost, shortestEdgeSegmentOfVertex, searchType);
  }

  /**
   * Constructor for an edge cost based Dijkstra algorithm for finding shortest paths.
   * 
   * @param edgeSegmentCosts edge segment costs both physical and virtual
   * @param numberOfVertices Vertices, both nodes and centroids
   */
  public ShortestPathDijkstra(final double[] edgeSegmentCosts, int numberOfVertices) {
    super(edgeSegmentCosts, numberOfVertices);
  }

  /**
   * Execute shortest path search based on given search direction and start vertex
   * 
   * @param searchType  to use
   * @param startVertex to use
   * @return results of shortest path search, if something went wrong null is returned
   */
  public ShortestPathResult execute(ShortestSearchType searchType, DirectedVertex startVertex) {
    switch (searchType) {
    case ONE_TO_ALL:
    case ONE_TO_ONE:
      return executeOneToAll(startVertex);
    case ALL_TO_ONE:
      return executeAllToOne(startVertex);
    default:
      LOGGER.severe("Unsupported search type encountered in Dijkstra shortest path execution");
      return null;
    }
  }

  /**
   * Construct shortest paths from source node to all other nodes in the network based on directed LinkSegment edges
   * 
   * @param currentOrigin origin vertex of source node
   * @return shortest path result that can be used to extract paths
   */
  @Override
  public ShortestPathResult executeOneToAll(DirectedVertex currentOrigin) {
    return dijkstraExecute(ShortestSearchType.ONE_TO_ALL, currentOrigin, es -> shortestEdgeSegmentOfVertex[(int) es.getDownstreamVertex().getId()] = es);
  }

  /**
   * Construct shortest paths from all nodes to a single sink node in the network based on directed Link segment edges
   * 
   * @param currentDestination destination vertex
   * @return shortest path result that can be used to extract paths
   */
  @Override
  public ShortestPathResult executeAllToOne(DirectedVertex currentDestination) {
    return dijkstraExecute(
            ShortestSearchType.ALL_TO_ONE,
            currentDestination, es -> shortestEdgeSegmentOfVertex[(int) es.getUpstreamVertex().getId()] = es);
  }
}

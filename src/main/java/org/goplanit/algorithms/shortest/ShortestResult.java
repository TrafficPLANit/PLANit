package org.goplanit.algorithms.shortest;

import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;

/**
 * Base interface that defines how to access results of a shortest X execution allowing one to extract information
 * 
 * @author markr
 *
 */
public interface ShortestResult {

  /**
   * Find the next vertex on the given edge segment extremity based on the underlying search this can be either in upstream or downstream direction
   * 
   * @param edgeSegment to get next vertex for
   * @return next vertex
   */
  public abstract DirectedVertex getNextVertexForEdgeSegment(EdgeSegment edgeSegment);

  /**
   * Collect the cost to reach the given vertex from the reference starting point
   * 
   * @param vertex to collect cost for
   * @return cost found
   */
  public abstract double getCostToReach(Vertex vertex);

  /**
   * Provide the search type that was used to obtain this result
   * 
   * @return shortest path search type used to obtain result
   */
  public abstract ShortestSearchType getSearchType();
  
  /** when search is inverted, result is also inverted, i.e., when search is one-to-x (regular), result is in upstream direction, when inverted, result is in downstream direction
   * @return true when search (and result) is inverted compared to regular one-to-x search, false otherwise
   */
  public default boolean isInverted() {
    return getSearchType().isInverted();
  }

}

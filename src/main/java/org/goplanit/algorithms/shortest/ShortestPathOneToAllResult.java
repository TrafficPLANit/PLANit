package org.goplanit.algorithms.shortest;

import org.goplanit.utils.graph.Vertex;

/**
 * Interfaces that defines how to access results of a shortest path one-to-all execution allowing one to extract paths or cost information
 * 
 * @author markr
 *
 */
public interface ShortestPathOneToAllResult extends ShortestPathResult {

  /**
   * Collect the cost to reach the given vertex from the (single) point of origin
   * 
   * @param vertex to collect cost for
   * @return cost found
   */
  public abstract double getCostToReach(Vertex vertex);

}

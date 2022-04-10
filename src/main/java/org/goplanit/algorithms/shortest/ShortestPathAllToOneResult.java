package org.goplanit.algorithms.shortest;

import org.goplanit.utils.graph.Vertex;

/**
 * Interfaces that defines how to access results of a shortest path all-to-one execution allowing one to extract paths or cost information
 * 
 * @author markr
 *
 */
public interface ShortestPathAllToOneResult extends ShortestPathResult {

  /**
   * Collect the cost to reach the (single) point of destination from a given vertex
   * 
   * @param vertex to collect cost for
   * @return cost found
   */
  public abstract double getCostFrom(Vertex vertex);

}

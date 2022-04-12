package org.goplanit.algorithms.shortest;

import org.goplanit.utils.graph.Vertex;

/**
 * Interfaces that defines how to access results of a shortest bush execution allowing one to extract bushes or cost information for a one-to-all search
 * 
 * @author markr
 *
 */
public interface ShortestBushOneToAllResult extends ShortestBushResult {

  /**
   * Collect the cost to reach the given vertex
   * 
   * @param vertex to collect cost for
   * @return cost found
   */
  public abstract double getCostToReach(Vertex vertex);

}

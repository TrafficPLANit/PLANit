package org.goplanit.algorithms.shortest;

import org.goplanit.utils.graph.Vertex;

/**
 * Interfaces that defines how to access results of a shortest bush execution allowing one to extract bushes or cost information for an all-to-one search
 * 
 * @author markr
 *
 */
public interface ShortestBushAllToOneResult extends ShortestBushResult {

  /**
   * Collect the cost from given vertex to final vertex
   * 
   * @param vertex to collect cost for
   * @return cost found
   */
  public abstract double getCostFrom(Vertex vertex);

}

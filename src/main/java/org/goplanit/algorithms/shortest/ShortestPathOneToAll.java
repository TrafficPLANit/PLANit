package org.goplanit.algorithms.shortest;

import org.goplanit.utils.graph.directed.DirectedVertex;

/**
 * An algorithm which calculates the shortest (a.k.a. lowest cost) path to all vertices from a given start vertex for a directed graph
 * 
 * @author markr
 *
 */
public interface ShortestPathOneToAll {

  /**
   * Construct shortest paths from source node to all other nodes in the network based on directed LinkSegment edges
   * 
   * @param currentOrigin start vertex
   * @return shortest path result that can be used to extract paths
   */
  public ShortestPathOneToAllResult executeOneToAll(DirectedVertex currentOrigin);

}

package org.goplanit.algorithms.shortest;

import org.goplanit.utils.graph.directed.DirectedVertex;

/**
 * An algorithm which calculates the shortest (a.k.a. lowest cost) path to all vertices from a given origin vertex for a directed graph in upstream direction
 * 
 * @author markr
 *
 */
public interface ShortestPathAllToOne {

  /**
   * Construct shortest paths from all nodes to a destination node in the network based on directed LinkSegment edges
   * 
   * @param currentDestination destination vertex to which all paths go
   * @return shortest path result that can be used to extract paths
   */
  public ShortestPathResult executeAllToOne(DirectedVertex currentDestination);

}

package org.goplanit.algorithms.shortest;

import org.goplanit.utils.graph.directed.DirectedVertex;

import java.util.Collections;
import java.util.Set;

/**
 * An algorithm which calculates the shortest (a.k.a. lowest cost) path to all vertices from a given origin vertex
 * for a directed graph in upstream direction.
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
  public default ShortestPathResult executeAllToOne(DirectedVertex currentDestination){
    return executeAllToOne(currentDestination, Collections.emptySet());
  }

  /**
   * Construct shortest paths from all nodes to a destination node in the network based on directed LinkSegment edges
   * 
   * @param currentDestination destination vertex to which all paths go
   * @param bannedsThroughVertices set of vertices that do not allow paths to go through them. They may only serve as
   *                           start and/or end points
   * @return shortest path result that can be used to extract paths
   */
  public ShortestPathResult executeAllToOne(
      DirectedVertex currentDestination, Set<DirectedVertex> bannedsThroughVertices);

}

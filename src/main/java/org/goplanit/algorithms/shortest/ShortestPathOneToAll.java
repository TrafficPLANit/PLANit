package org.goplanit.algorithms.shortest;

import org.goplanit.utils.graph.directed.DirectedVertex;

import java.util.Collections;
import java.util.Set;

/**
 * An algorithm which calculates the shortest (a.k.a. lowest cost) path to all vertices from a given start vertex
 * for a directed graph.
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
  public default ShortestPathResult executeOneToAll(DirectedVertex currentOrigin){
    return executeOneToAll(currentOrigin, Collections.emptySet());
  }


  /**
   * Construct shortest paths from source node to all other nodes in the network based on directed LinkSegment edges
   *
   * @param currentOrigin start vertex
   * @param bannedThroughVertices set of vertices that do not allow paths to go through them. They may only serve as
   *                           start and/or end points
   * @return shortest path result that can be used to extract paths
   */
  public ShortestPathResult executeOneToAll(
          DirectedVertex currentOrigin, Set<DirectedVertex> bannedThroughVertices);

}

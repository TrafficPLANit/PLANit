package org.goplanit.algorithms.shortest;

import org.goplanit.utils.graph.directed.DirectedVertex;

import java.util.Collections;
import java.util.Set;

/**
 * An algorithm which calculates the shortest (a.k.a. lowest cost) bush from all vertices to a given end vertex
 * for a directed graph in upstream direction.
 * 
 * @author markr
 *
 */
public interface ShortestBushAllToOne {

  /**
   * Construct shortest bush result from any node to a sink node based on directed LinkSegment edges
   *
   * @param currentDestination destination vertex
   * @return shortest bush result that can be used to extract bushes
   */
  public default ShortestBushResult executeAllToOne(DirectedVertex currentDestination){
    return executeAllToOne(currentDestination, Collections.emptySet());
  }

  /**
   * Construct shortest bush result from any node to a sink node based on directed LinkSegment edges
   * 
   * @param currentDestination destination vertex
   * @param bannedThroughVertices set of vertices that do not allow paths to go through them. They may only serve as
   *                              start and/or end points
   * @return shortest bush result that can be used to extract bushes
   */
  public ShortestBushResult executeAllToOne(
          DirectedVertex currentDestination, Set<DirectedVertex> bannedThroughVertices);

}

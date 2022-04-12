package org.goplanit.algorithms.shortest;

import org.goplanit.utils.graph.directed.DirectedVertex;

/**
 * An algorithm which calculates the shortest (a.k.a. lowest cost) bush from all vertices to a given end vertex for a directed graph in upstream direction
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
  public ShortestBushAllToOneResult executeAllToOne(DirectedVertex currentDestination);

}

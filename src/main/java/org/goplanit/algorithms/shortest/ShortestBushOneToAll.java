package org.goplanit.algorithms.shortest;

import org.goplanit.utils.graph.directed.DirectedVertex;

/**
 * An algorithm which calculates the shortest (a.k.a. lowest cost) bush to all vertices from a given origin vertex for a directed graph in downstream direction
 * 
 * @author markr
 *
 */
public interface ShortestBushOneToAll {

  /**
   * Construct shortest bush result from source node to all other nodes in the network based on directed LinkSegment edges
   * 
   * @param currentOrigin origin vertex of source node
   * @return shortest bush result that can be used to extract bushes
   */
  public ShortestBushResult executeOneToAll(DirectedVertex currentOrigin);

}

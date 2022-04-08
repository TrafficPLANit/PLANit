package org.goplanit.algorithms.shortest;

import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.directed.DirectedVertex;

/**
 * An algorithm which calculates the shortest (a.k.a. lowest cost) path to all vertices from a given origin vertex for a directed graph in upstream direction
 * 
 * @author markr
 *
 */
public interface ShortestPathAllToOne {

  /**
   * Construct shortest paths from source node to all other nodes in the network based on directed LinkSegment edges
   * 
   * @param currentDestination destination vertex to which all paths go
   * @return shortest path result that can be used to extract paths
   * @throws PlanItException thrown if an error occurs
   */
  public ShortestPathResult executeAllToOne(DirectedVertex currentDestination);

}

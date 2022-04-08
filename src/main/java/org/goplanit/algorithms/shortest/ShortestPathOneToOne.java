package org.goplanit.algorithms.shortest;

import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.directed.DirectedVertex;

/**
 * An algorithm which calculates the shortest (a.k.a. lowest cost) path to all
 * vertices from a given origin vertex for a directed graph
 * 
 * @author markr
 *
 */
public interface ShortestPathOneToOne {

  /**
   * Construct shortest paths from source node to all other nodes in the network
   * based on directed LinkSegment edges
   * 
   * @param origin vertex of source node
   * @param destination vertex of sink node
   * @return shortest path result of the execution
   * @throws PlanItException thrown if path cannot be created
   */
  public ShortestPathResult executeOneToOne(DirectedVertex origin, DirectedVertex destination) throws PlanItException;

}

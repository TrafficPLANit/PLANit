package org.planit.algorithms.shortestpath;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.Vertex;

/**
 * An algorithm which calculates the shortest (a.k.a. lowest cost) path to all
 * vertices from a given origin vertex
 * 
 * @author markr
 *
 */
public interface OneToOneShortestPathAlgorithm {

  /**
   * Construct shortest paths from source node to all other nodes in the network
   * based on directed LinkSegment edges
   * 
   * @param origin vertex of source node
   * @param destination vertex of sink node
   * @return shortest path result of the execution
   * @throws PlanItException thrown if path cannot be created
   */
  public ShortestPathResult executeOneToOne(Vertex origin, Vertex destination) throws PlanItException;

}

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
public interface OneToAllShortestPathAlgorithm {

  /**
   * Construct shortest paths from source node to all other nodes in the network
   * based on directed LinkSegment edges
   * 
   * @param currentOrigin origin vertex of source node
   * @return shortest path result that can be used to extract paths
   * @throws PlanItException thrown if an error occurs
   */
  public ShortestPathResult executeOneToAll(Vertex currentOrigin) throws PlanItException;

}

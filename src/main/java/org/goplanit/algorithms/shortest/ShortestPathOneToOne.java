package org.goplanit.algorithms.shortest;

import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;

import java.util.Set;

/**
 * An algorithm which calculates the shortest (a.k.a. lowest cost) path to all vertices from a given origin vertex for a directed graph
 * 
 * @author markr
 *
 */
public interface ShortestPathOneToOne {

  /**
   * Construct shortest paths from source node to all other nodes in the network based on directed LinkSegment edges
   * 
   * @param origin      vertex of source node
   * @param destination vertex of sink node
   * @return shortest path result of the execution
   */
  public ShortestPathResult executeOneToOne(DirectedVertex origin, DirectedVertex destination);

  /**
   * Construct shortest paths from source node to all other nodes in the network based on directed LinkSegment edges while imposing custom constraints on
   * certain edge segments not being allowed to be used
   *
   * @param origin      vertex of source node
   * @param destination vertex of sink node
   * @return shortest path result of the execution
   */
  public ShortestPathResult executeOneToOne(DirectedVertex origin, DirectedVertex destination, Set<? extends EdgeSegment> bannedSegments);

}

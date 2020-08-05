package org.planit.algorithms.shortestpath;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.Vertex;
import org.planit.utils.misc.Pair;

/**
 * An algorithm which calculates the shortest (a.k.a. lowest cost) route to all
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
   * @return array of pairs containing, for each vertex (array index), the cost to
   *         reach the vertex and the link segment it is reached from with the
   *         shortest cost.
   * @throws PlanItException thrown if an error occurs
   */
  public Pair<Double, EdgeSegment>[] executeOneToOne(Vertex origin, Vertex destination) throws PlanItException;

}

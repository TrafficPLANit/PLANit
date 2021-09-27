package org.planit.algorithms.shortestpath;

import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.Vertex;
import org.planit.utils.path.DirectedPath;
import org.planit.utils.path.DirectedPathFactory;

/**
 * Interfaces that defines how to access results of a shortest path execution allowing one to extract paths or cost information
 * 
 * @author markr
 *
 */
public interface ShortestPathResult {

  /**
   * Create the path from the provided origin to a specified destination vertex, using the results available. The path builder is used to create the instance of the path.
   * 
   * @param pathBuilder to use for creating path instances
   * @param origin      the specified origin vertex
   * @param destination the specified destination vertex
   * @return the path that is created, when no path could be extracted null is returned
   * 
   */
  public abstract DirectedPath createPath(final DirectedPathFactory pathFactory, Vertex origin, Vertex destination);

  /**
   * Find the incoming edge segment for a given vertex
   * 
   * @param vertex to get incoming segment for
   * @return incoming edge segment
   */
  public abstract EdgeSegment getIncomingEdgeSegmentForVertex(Vertex vertex);

  /**
   * Collect the cost to reach the given vertex
   * 
   * @param vertex to collect cost for
   * @return cost found
   */
  public abstract double getCostToReach(Vertex vertex);

}

package org.goplanit.algorithms.shortest;

import java.util.List;

import org.goplanit.graph.directed.acyclic.ACyclicSubGraph;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * Interfaces that defines how to access results of a shortest bush execution allowing one to extract bushes or cost information
 * 
 * @author markr
 *
 */
public interface ShortestBushResult {

  /**
   * Create the bush in the form of a directed acyclic subgraph of its parent network (layer) for a given origin-destination vertex
   * 
   * @param idToken     to use for the Acyclic subgraph's id generation
   * @param origin      to use
   * @param destination to use
   * @return created acyclic graph
   */
  public abstract ACyclicSubGraph createDirectedAcyclicSubGraph(final IdGroupingToken idToken, final DirectedVertex origin, final DirectedVertex destination);

  /**
   * Find the incoming edge segments for a given vertex
   * 
   * @param vertex to get incoming segment for
   * @return incoming edge segments
   */
  public abstract List<EdgeSegment> getNextEdgeSegmentsForVertex(Vertex vertex);

  /**
   * Collect the cost to reach the given vertex
   * 
   * @param vertex to collect cost for
   * @return cost found
   */
  public abstract double getCostOf(Vertex vertex);

}

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
public interface ShortestBushResult extends ShortestResult {

  /**
   * Create a bush in the form of a directed acyclic subgraph of its parent network (layer) for a given origin-destination vertex. The direction of the result is inherited from the
   * search itself, so if the search was inverted, the dag has an inverted root etc.
   * 
   * @param idToken     to use for the Acyclic subgraph's id generation
   * @param origin      to use
   * @param destination to use
   * @return created acyclic graph
   */
  public abstract ACyclicSubGraph<DirectedVertex, EdgeSegment> createDirectedAcyclicSubGraph(final IdGroupingToken idToken, final DirectedVertex origin,
      final DirectedVertex destination);

  /**
   * Find the next edge segment for a given vertex, depending on the underlying search this can be either in upstream or downstream direction
   * 
   * @param vertex to get next segment for
   * @return next edge segment
   */
  public abstract List<EdgeSegment> getNextEdgeSegmentsForVertex(Vertex vertex);

}

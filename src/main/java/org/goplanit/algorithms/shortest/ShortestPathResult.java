package org.goplanit.algorithms.shortest;

import java.util.function.Consumer;

import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.path.DirectedPath;
import org.goplanit.utils.path.DirectedPathFactory;

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
   * @param pathFactory to use for creating path instances
   * @param origin      the specified origin vertex
   * @param destination the specified destination vertex
   * @return the path that is created, when no path could be extracted null is returned
   */
  public abstract DirectedPath createPath(final DirectedPathFactory pathFactory, DirectedVertex origin, DirectedVertex destination);

  /**
   * apply consumer to each edge segment on path. Depending on the thype of shortest path (direction), the next segment is either in the backward direction from destination to
   * origin (one-to-all) or in the forward direction from the origin to the destination (all-to-one). This depends on the implementation of the
   * {@link #getNextEdgeSegmentForVertex(Vertex)}. If path does not lead to origin/destination, the loop terminates when no more next edge segment is found anymore
   * 
   * @param startVertex             to use
   * @param endVertex               to use
   * @param nextEdgeSegmentConsumer to apply to each next segment on the path
   * @return number of edge segments traversed on the path
   */
  public default int forEachNextEdgeSegment(DirectedVertex startVertex, DirectedVertex endVertex, Consumer<EdgeSegment> nextEdgeSegmentConsumer) {
    EdgeSegment backwardEdgeSegment = null;
    Vertex currentVertex = endVertex;
    int count = 0;
    do {
      backwardEdgeSegment = getNextEdgeSegmentForVertex(currentVertex);
      if (backwardEdgeSegment == null) {
        break;
      }
      nextEdgeSegmentConsumer.accept(backwardEdgeSegment);
      currentVertex = backwardEdgeSegment.getUpstreamVertex();
      ++count;
    } while (!currentVertex.idEquals(startVertex));
    return count;
  }

  /**
   * Find the next edge segment for a given vertex, depending on the underlying search this can be either in upstream or diwnstream direction
   * 
   * @param vertex to get next segment for
   * @return next edge segment
   */
  public abstract EdgeSegment getNextEdgeSegmentForVertex(Vertex vertex);

}

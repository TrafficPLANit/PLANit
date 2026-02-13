package org.goplanit.algorithms.shortest;

import java.util.Deque;
import java.util.function.Consumer;

import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.graph.directed.acyclic.ACyclicSubGraph;
import org.goplanit.utils.graph.directed.acyclic.UntypedACyclicSubGraph;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.path.DirectedPathFactory;
import org.goplanit.utils.path.SimpleDirectedPath;

/**
 * Interfaces that defines how to access results of a shortest path execution allowing one to extract paths or cost
 * information.
 * 
 * @author markr
 *
 */
public interface ShortestPathResult extends ShortestResult{

  /**
   * Create the path from the provided origin to a specified destination vertex, using the results available.
   * The path builder is used to create the instance of the path.
   *
   * @param <T> type of path
   * @param pathFactory to use for creating path instances
   * @param origin      the specified origin vertex
   * @param destination the specified destination vertex
   * @return the path that is created, when no path could be extracted null is returned
   */
  public abstract <T extends SimpleDirectedPath> T createPath(
          final DirectedPathFactory<T> pathFactory, DirectedVertex origin, DirectedVertex destination);

  /**
   * Create the path from the provided origin to a specified destination vertex, using the results available.
   * The path builder is used to create the instance of the path.
   *
   * @param origin      the specified origin vertex
   * @param destination the specified destination vertex
   * @return the raw path in the form of an array of edge segments, when no path could be extracted null is returned
   */
  public abstract Deque<EdgeSegment> createRawPath(DirectedVertex origin, DirectedVertex destination);
  
  /**
   * Find the next edge segment for a given vertex, depending on the underlying search this can be either in upstream
   * or downstream direction.
   * 
   * @param vertex to get next segment for
   * @return next edge segment
   */
  public abstract EdgeSegment getNextEdgeSegmentForVertex(Vertex vertex);

  /**
   * Allow to overwrite the result for the next segment to use for a given vertex in the result. Use with caution!
   *
   * @param vertex to overwrite next segment for
   * @param nextSegment to replace existing result
   * @return original next segment that has been replaced
   */
  public abstract EdgeSegment overwriteNextSegmentForVertex(Vertex vertex, EdgeSegment nextSegment);

  /**
   * Extract the shortest path tree as a directed acyclic sub graph to ALL vertices.
   * It is a subgraph because it will be a subset of the network (graph), but it will be a full spanning tree, so all
   * vertices in the network will be connected
   *
   * @param idToken to use
   * @return created graph
   */
  public abstract UntypedACyclicSubGraph<?,?> createAndPopulateDirectedAcyclicSubGraphSpanningTree(
      final IdGroupingToken idToken);

  /**
   * identical to {@link #createAndPopulateDirectedAcyclicSubGraphSpanningTree(IdGroupingToken)} only now dag is
   * provided which will be used to populate (assumed empty)
   *
   * @param <V> vertex type
   * @param <E> edge segment type
   * @param dagToPopulate the dag to populate
   */
  public abstract <V extends DirectedVertex, E extends EdgeSegment> void populateDirectedAcyclicSubGraphSpanningTree(
      UntypedACyclicSubGraph<V,E> dagToPopulate);

  /**
   * apply consumer to each edge segment on path. Depending on the type of shortest path (direction), the next segment
   * is either in the backward direction from destination to origin (one-to-all) or in the forward direction from the
   * origin to the destination (all-to-one). This depends on the implementation of the
   * {@link #getNextEdgeSegmentForVertex(Vertex)}. If path does not lead to origin/destination, the loop terminates
   * when no more next edge segment is found.
   * 
   * @param startVertex             to use
   * @param endVertex               to use
   * @param nextEdgeSegmentConsumer to apply to each next segment on the path
   * @return number of edge segments traversed on the path
   */
  public default int forEachNextEdgeSegment(
          DirectedVertex startVertex, DirectedVertex endVertex, Consumer<EdgeSegment> nextEdgeSegmentConsumer) {
    EdgeSegment backwardEdgeSegment = null;
    Vertex currentVertex = endVertex;
    int count = 0;
    do {
      backwardEdgeSegment = getNextEdgeSegmentForVertex(currentVertex);
      if (backwardEdgeSegment == null) {
        break;
      }
      nextEdgeSegmentConsumer.accept(backwardEdgeSegment);
      currentVertex = getNextVertexForEdgeSegment(backwardEdgeSegment);
      ++count;
    } while (!currentVertex.idEquals(startVertex));
    return count;
  }

}

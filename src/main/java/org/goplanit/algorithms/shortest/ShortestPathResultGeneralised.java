package org.goplanit.algorithms.shortest;

import java.util.Deque;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.path.DirectedPath;
import org.goplanit.utils.path.DirectedPathFactory;

/**
 * Class that stores the result of a shortest path execution allowing one to extract paths or cost information. Can be used for a one-to-all as well as an all-to-one search result.
 * However not both at the same time, so either the internal state reflects a one-to-all result, or an all-to-one result and methods supported by that specific interface have
 * defined behaviour
 * <p>
 * Note that for one-to-all we traverse a path from an origin to a destination in reversed order to extract the path, whereas in all-to-one we can directly extract it as we start
 * at the origin directly.
 * 
 * @author markr
 *
 */
public class ShortestPathResultGeneralised extends ShortestResultGeneralised implements ShortestPathResult {

  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(ShortestPathResultGeneralised.class.getCanonicalName());
  
  /**
   * the next edge segment to reach the vertex with the given measured cost (preceding in one-to-all, succeeding in all-to-one)
   */
  protected final EdgeSegment[] nextEdgeSegmentByVertex;  

  /**
   * Constructor only to be used by shortest path algorithms
   * 
   * @param vertexMeasuredCost      measured costs to get to the vertex (by id)
   * @param nextEdgeSegmentByVertex the next edge segment for each vertex (by id)
   * @param searchType              used (one-to-all, all-to-one, etc)
   */
  protected ShortestPathResultGeneralised(double[] vertexMeasuredCost, EdgeSegment[] nextEdgeSegmentByVertex, ShortestSearchType searchType) {
    super(vertexMeasuredCost, searchType);
    this.nextEdgeSegmentByVertex = nextEdgeSegmentByVertex;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedPath createPath(final DirectedPathFactory pathFactory, DirectedVertex origin, DirectedVertex destination) {
    // path edge segment container
    final Deque<EdgeSegment> pathEdgeSegments = new LinkedList<>();

    /* depending on the search direction the start and end vertex to loop through result could be inverted */
    Pair<DirectedVertex, DirectedVertex> startEndPair = getStartEndVertexForResultTraversal(origin, destination);
    DirectedVertex startVertex = startEndPair.first();
    DirectedVertex endVertex = startEndPair.second();

    boolean invertedTraversal = isInverted();

    int currVertexId = (int) startVertex.getId();
    var nextEdgeSegment = nextEdgeSegmentByVertex[currVertexId];
    final int endVertexId = (int) endVertex.getId();

    // extract path
    while (endVertexId != currVertexId) {
      if (nextEdgeSegment == null) {
        /* unable to create path */
        return null;
      }

      if (invertedTraversal) {
        /* from origin towards destination, add to back */
        pathEdgeSegments.add(nextEdgeSegment);
      } else {
        /* from destination back toorigin, add to front */
        pathEdgeSegments.addFirst(nextEdgeSegment);
      }

      currVertexId = (int) getVertexAtExtreme.apply(nextEdgeSegment).getId();
      nextEdgeSegment = nextEdgeSegmentByVertex[currVertexId];
    }

    // create path
    return pathFactory.createNew(pathEdgeSegments);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegment getNextEdgeSegmentForVertex(Vertex vertex) {
    return nextEdgeSegmentByVertex[(int) vertex.getId()];
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getCostOf(Vertex vertex) {
    return vertexMeasuredCost[(int) vertex.getId()];
  }



}

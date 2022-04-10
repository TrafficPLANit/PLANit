package org.goplanit.algorithms.shortest;

import java.util.Deque;
import java.util.LinkedList;
import java.util.function.Function;
import java.util.logging.Logger;

import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.graph.directed.DirectedVertex;
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
public class ShortestPathResultGeneralisedImpl implements ShortestPathOneToAllResult, ShortestPathAllToOneResult {

  enum ResultType {
    ALL_TO_ONE, ONE_TO_ALL, ONE_TO_ONE;
  }

  private static final Logger LOGGER = Logger.getLogger(ShortestPathOneToAllResult.class.getCanonicalName());

  /**
   * the costs found by a shortest path run
   */
  protected final double[] vertexMeasuredCost;

  /**
   * the next edge segment to reach the vertex with the given measured cost (preceding in one-to-all, succeeding in all-to-one)
   */
  protected final EdgeSegment[] nextEdgeSegment;

  /** reflects the active result type */
  protected final ResultType resultType;

  /** depending on configuration this function collects vertex at desired edge segment extremity */
  protected Function<EdgeSegment, DirectedVertex> getVertexAtExtreme;

  /**
   * Constructor only to be used by shortest path algorithms
   * 
   * @param vertexMeasuredCost  measured costs to get to the vertex (by id)
   * @param incomingEdgeSegment the incoming edge segment for each vertex (by id)
   * @param resultType          used
   */
  protected ShortestPathResultGeneralisedImpl(double[] vertexMeasuredCost, EdgeSegment[] incomingEdgeSegment, ResultType resultType) {
    this.vertexMeasuredCost = vertexMeasuredCost;
    this.nextEdgeSegment = incomingEdgeSegment;
    this.resultType = resultType;
    switch (resultType) {
    case ONE_TO_ALL:
    case ONE_TO_ONE:
      getVertexAtExtreme = ShortestPathGeneralised.getUpstreamVertex;
      break;
    case ALL_TO_ONE:
      getVertexAtExtreme = ShortestPathGeneralised.getDownstreamVertex;
      break;
    default:
      LOGGER.severe(String.format("Result type %s not supported by shortest path result", resultType.toString()));
      break;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedPath createPath(final DirectedPathFactory pathFactory, DirectedVertex origin, DirectedVertex destination) {
    // path edge segment container
    final Deque<EdgeSegment> pathEdgeSegments = new LinkedList<>();

    // prep
    int vertexId = (int) destination.getId();
    var previousEdgeSegmentOnPath = nextEdgeSegment[vertexId];
    final int originVertexId = (int) origin.getId();

    // extract path
    while (originVertexId != vertexId) {
      if (previousEdgeSegmentOnPath == null) {
        /* unable to create path */
        return null;
      }
      pathEdgeSegments.addFirst(previousEdgeSegmentOnPath);
      vertexId = (int) getVertexAtExtreme.apply(previousEdgeSegmentOnPath).getId();
      previousEdgeSegmentOnPath = nextEdgeSegment[vertexId];
    }

    // create path
    return pathFactory.createNew(pathEdgeSegments);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegment getNextEdgeSegmentForVertex(Vertex vertex) {
    return nextEdgeSegment[(int) vertex.getId()];
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getCostToReach(Vertex vertex) {
    return vertexMeasuredCost[(int) vertex.getId()];
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getCostFrom(Vertex vertex) {
    return vertexMeasuredCost[(int) vertex.getId()];
  }

}

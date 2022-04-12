package org.goplanit.algorithms.shortest;

import java.util.Deque;
import java.util.LinkedList;
import java.util.function.Function;
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
public class ShortestPathResultGeneralised implements ShortestPathResult {

  private static final Logger LOGGER = Logger.getLogger(ShortestPathResultGeneralised.class.getCanonicalName());

  /**
   * Determine the start and end vertex to use for constructing the path depending on the search type used in the preceding shortest path search
   * 
   * @param origin      of to be constructed path
   * @param destination of to be constructed path
   * @return order in which origin and destination are to be encountered when traversing search results
   */
  private Pair<DirectedVertex, DirectedVertex> getStartEndVertex(DirectedVertex origin, DirectedVertex destination) {
    Boolean isInverted = isSearchResultInverted();
    if (isInverted == null) {
      LOGGER.severe(String.format("Unsupported search type %s found for shortest path result", searchType));
      return null;
    }

    if (isSearchResultInverted()) {
      return Pair.of(origin, destination);
    } else {
      /* regular direction where results are traversed from destination back to origin */
      return Pair.of(destination, origin);
    }
  }

  /**
   * the costs found by a shortest path run
   */
  protected final double[] vertexMeasuredCost;

  /**
   * the next edge segment to reach the vertex with the given measured cost (preceding in one-to-all, succeeding in all-to-one)
   */
  protected final EdgeSegment[] nextEdgeSegmentByVertex;

  /** reflects the active type */
  protected final ShortestSearchType searchType;

  /** depending on configuration this function collects vertex at desired edge segment extremity */
  protected Function<EdgeSegment, DirectedVertex> getVertexAtExtreme;

  /**
   * Based on the search type of the underlying search we indicate if it is inverted compared to a regular shortest path search that occurs in the one-to-x direction
   * 
   * @return true when inverted, false otherwise
   */
  protected Boolean isSearchResultInverted() {
    switch (searchType) {
    case ONE_TO_ONE:
    case ONE_TO_ALL:
      return false;
    case ALL_TO_ONE:
      return true;
    default:
      LOGGER.severe(String.format("Unsupported search type %s found for shortest path result", searchType));
      return null;
    }
  }

  /**
   * Constructor only to be used by shortest path algorithms
   * 
   * @param vertexMeasuredCost      measured costs to get to the vertex (by id)
   * @param nextEdgeSegmentByVertex the next edge segment for each vertex (by id)
   * @param searchType              used (one-to-all, all-to-one, etc)
   */
  protected ShortestPathResultGeneralised(double[] vertexMeasuredCost, EdgeSegment[] nextEdgeSegmentByVertex, ShortestSearchType searchType) {
    this.vertexMeasuredCost = vertexMeasuredCost;
    this.nextEdgeSegmentByVertex = nextEdgeSegmentByVertex;
    this.searchType = searchType;

    /* search direction for creating paths in opposite direction as compared to shortest path search itself */
    this.getVertexAtExtreme = ShortestPathSearchUtils.getVertexFromEdgeSegmentLambda(searchType, true /* invert */ );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedPath createPath(final DirectedPathFactory pathFactory, DirectedVertex origin, DirectedVertex destination) {
    // path edge segment container
    final Deque<EdgeSegment> pathEdgeSegments = new LinkedList<>();

    /* depending on the search direction the start and end vertex to loop through result could be inverted */
    Pair<DirectedVertex, DirectedVertex> startEndPair = getStartEndVertex(origin, destination);
    DirectedVertex startVertex = startEndPair.first();
    DirectedVertex endVertex = startEndPair.second();

    boolean invertedTraversal = isSearchResultInverted();

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
  public DirectedVertex getNextVertexForEdgeSegment(EdgeSegment edgeSegment) {
    return this.getVertexAtExtreme.apply(edgeSegment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getCostOf(Vertex vertex) {
    return vertexMeasuredCost[(int) vertex.getId()];
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ShortestSearchType getSearchType() {
    return searchType;
  }

}

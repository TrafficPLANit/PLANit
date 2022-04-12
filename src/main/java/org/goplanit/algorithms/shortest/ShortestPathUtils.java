package org.goplanit.algorithms.shortest;

import java.util.function.Function;
import java.util.logging.Logger;

import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;

/**
 * Some common utilities for various shortest path algorithms
 * 
 * @author markr
 *
 */
public final class ShortestPathUtils {

  /** logger ot use */
  private static final Logger LOGGER = Logger.getLogger(ShortestPathUtils.class.getCanonicalName());

  /** Function collecting upstream vertex for edge segment */
  public static final Function<EdgeSegment, DirectedVertex> getUpstreamVertex = e -> e.getUpstreamVertex();

  /** Function collecting downstream vertex for edge segment */
  public static final Function<EdgeSegment, DirectedVertex> getDownstreamVertex = e -> e.getDownstreamVertex();

  /** Function collecting entry edge segments for vertex */
  public static final Function<DirectedVertex, Iterable<EdgeSegment>> getEntryEdgeSegments = v -> v.getEntryEdgeSegments();

  /** Function collecting exit edge segments for vertex */
  public static final Function<DirectedVertex, Iterable<EdgeSegment>> getExitEdgeSegments = v -> v.getExitEdgeSegments();

  /**
   * Identical to {@link #getVertexFromEdgeSegmentLambda(ShortestSearchType, false)}
   * 
   * @param shortestPathSearchType to base lambda on
   * @return lambda collecting vertex based on given edge segment
   */
  public static Function<EdgeSegment, DirectedVertex> getVertexFromEdgeSegmentLambda(ShortestSearchType shortestPathSearchType) {
    return getVertexFromEdgeSegmentLambda(shortestPathSearchType, false);
  }

  /**
   * Based on the search type we collect different vertex direction from edge segment (up or downstream) during shortest path search. This method provides the Lambda that performs
   * this collection.
   * 
   * @param shortestPathSearchType to base lambda on
   * @param invertDirection        flag to invert direction, useful when constructing paths from search result, which might require traversal in opposite direction of the search
   *                               algorithm
   * @return lambda collecting vertex based on given edge segment
   */
  public static Function<EdgeSegment, DirectedVertex> getVertexFromEdgeSegmentLambda(ShortestSearchType shortestPathSearchType, boolean invertDirection) {
    switch (shortestPathSearchType) {
    case ONE_TO_ALL:
    case ONE_TO_ONE:
      return invertDirection ? getUpstreamVertex : getDownstreamVertex;
    case ALL_TO_ONE:
      return invertDirection ? getDownstreamVertex : getUpstreamVertex;
    default:
      LOGGER.severe(String.format("Shortest path search type %s not supported by getVertexFromEdgeSegmentLambda", shortestPathSearchType.toString()));
      return null;
    }
  }

  /**
   * Based on the search type we collect edge segments (up or downstream) for a given vertex. This method provides the Lambda that performs this collection.
   * 
   * @param shortestPathSearchType to base lambda on
   * @return lambda collecting edge segments in given direction based on vertex provided
   */
  public static Function<DirectedVertex, Iterable<EdgeSegment>> getEdgeSegmentsInDirectionLambda(ShortestSearchType shortestPathSearchType) {
    switch (shortestPathSearchType) {
    case ONE_TO_ALL:
    case ONE_TO_ONE:
      return getExitEdgeSegments;
    case ALL_TO_ONE:
      return getEntryEdgeSegments;
    default:
      LOGGER.severe(String.format("Shortest path search type %s not supported by getEdgeSegmentsInDirectionLambda", shortestPathSearchType.toString()));
      return null;
    }
  }
}

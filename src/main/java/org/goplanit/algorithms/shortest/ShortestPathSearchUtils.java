package org.goplanit.algorithms.shortest;

import java.util.function.Function;
import java.util.logging.Logger;

import org.goplanit.assignment.ltm.sltm.RootedLabelledBush;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;

/**
 * Some common utilities for various shortest path algorithms. The lambdas provided base their direction (up/downstream) on the direction used in the shortest path search NOT the
 * results it yields. To traverse the results, the opposite direciton is required which can be obtained by inverting the call via a flag.
 * 
 * 
 * @author markr
 *
 */
public final class ShortestPathSearchUtils {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(ShortestPathSearchUtils.class.getCanonicalName());

  /**
   * Identical to {@link #getVertexFromEdgeSegmentLambda(ShortestSearchType)}
   *
   * @param shortestPathSearchType to base lambda on
   * @return lambda collecting vertex based on given edge segment
   */
  public static Function<EdgeSegment, DirectedVertex> getVertexFromEdgeSegmentLambda(ShortestSearchType shortestPathSearchType) {
    return getVertexFromEdgeSegmentLambda(shortestPathSearchType, false);
  }

  /**
   * Based on the bush configuration we collect different vertex direction from edge segment (up or downstream) compatible with a (shortest) path search. In case the bush is not
   * inverted, i.e., runs from single origin to one or more destinations we collect the downstream vertex, otherwise the upstream vertex. This direction can be swapped if desired
   * with the invertDirection parameter
   * 
   * @param bush            based on the configuration of the bush determine which vertex to collect.
   * @param invertDirection flag to invert direction compared to "regular" result
   * @return lambda collecting vertex based on given edge segment
   */
  public static Function<EdgeSegment, DirectedVertex> getVertexFromEdgeSegmentLambda(RootedLabelledBush bush, boolean invertDirection) {
    if (bush.isInverted()) {
      return getVertexFromEdgeSegmentLambda(ShortestSearchType.ALL_TO_ONE, invertDirection);
    } else {
      return getVertexFromEdgeSegmentLambda(ShortestSearchType.ONE_TO_ALL, invertDirection);
    }
  }

  /**
   * Based on the bush configuration we collect different vertex direction from edge segment (up or downstream) compatible with a (shortest) path search. In case the bush is not
   * inverted, i.e., runs from single origin to one or more destinations we collect the downstream vertex, otherwise the upstream vertex.
   * 
   * @param bush based on the configuration of the bush determine which vertex to collect.
   * @return lambda collecting vertex based on given edge segment
   */
  public static Function<EdgeSegment, DirectedVertex> getVertexFromEdgeSegmentLambda(RootedLabelledBush bush) {
    return getVertexFromEdgeSegmentLambda(bush, false);
  }

  /**
   * Based on the search type we collect different vertex direction from edge segment (up or downstream) during shortest path search. This method provides the Lambda that performs
   * this collection.
   * 
   * @param shortestPathSearchType to base lambda on
   * @param invertDirection        flag to invert direction compared to "regular" result
   * @return lambda collecting vertex based on given edge segment
   */
  public static Function<EdgeSegment, DirectedVertex> getVertexFromEdgeSegmentLambda(ShortestSearchType shortestPathSearchType, boolean invertDirection) {
    switch (shortestPathSearchType) {
    case ONE_TO_ALL:
    case ONE_TO_ONE:
      return EdgeSegment.getVertexForEdgeSegmentLambda(invertDirection); // normally downstream unless inverted
    case ALL_TO_ONE:
      return EdgeSegment.getVertexForEdgeSegmentLambda(!invertDirection); // normally upstream unless inverted
    default:
      LOGGER.severe(String.format("Shortest path search type %s not supported by getVertexFromEdgeSegmentLambda", shortestPathSearchType.toString()));
      return null;
    }
  }

  /**
   * Based on the bush configuration we collect different edge segments from vertex (entry or exit segments) compatible with a (shortest) path search. In case the bush is not
   * inverted, i.e., runs from single origin to one or more destinations we collect the downstream edge segments, otherwise the upstream edge segments. This direction can be
   * swapped if desired with the invertDirection parameter
   * 
   * @param bush            to base lambda on
   * @param invertDirection flag to invert direction compared to "regular" result
   * @return lambda collecting edge segments in given direction based on vertex provided
   */
  public static Function<DirectedVertex, Iterable<? extends EdgeSegment>> getEdgeSegmentsInDirectionLambda(
      RootedLabelledBush bush, boolean invertDirection) {
    if (bush.isInverted()) {
      return getEdgeSegmentsInDirectionLambda(ShortestSearchType.ALL_TO_ONE, invertDirection);
    } else {
      return getEdgeSegmentsInDirectionLambda(ShortestSearchType.ONE_TO_ALL, invertDirection);
    }
  }

  /**
   * Based on the bush configuration we collect different edge segments from vertex (entry or exit segments) compatible with a (shortest) path search. i.e., runs from single origin
   * to one or more destinations we collect the downstream edge segments, otherwise the upstream edge segments.
   * 
   * @param bush to base lambda on
   * @return lambda collecting edge segments in given direction based on vertex provided
   */
  public static Function<DirectedVertex, Iterable<? extends EdgeSegment>> getEdgeSegmentsInDirectionLambda(
      RootedLabelledBush bush) {
    return getEdgeSegmentsInDirectionLambda(bush, false);
  }

  /**
   * Based on the search type we collect edge segments (up or downstream) for a given vertex. This method provides the Lambda that performs this collection.
   * 
   * @param shortestPathSearchType to base lambda on
   * @return lambda collecting edge segments in given direction based on vertex provided
   */
  public static Function<DirectedVertex, Iterable<? extends EdgeSegment>> getEdgeSegmentsInDirectionLambda(ShortestSearchType shortestPathSearchType) {
    return getEdgeSegmentsInDirectionLambda(shortestPathSearchType, false);
  }

  /**
   * Based on the search type we collect edge segments (up or downstream) for a given vertex. This method provides the Lambda that performs this collection.
   * 
   * @param shortestPathSearchType to base lambda on
   * @param invertDirection        flag to invert direction compared to "regular" result
   * @return lambda collecting edge segments in given direction based on vertex provided
   */
  public static Function<DirectedVertex, Iterable<? extends EdgeSegment>> getEdgeSegmentsInDirectionLambda(ShortestSearchType shortestPathSearchType, boolean invertDirection) {
    switch (shortestPathSearchType) {
    case ONE_TO_ALL:
    case ONE_TO_ONE:
      return DirectedVertex.getEdgeSegmentsForVertexLambda(invertDirection); // normally downstream, unless inverted
    case ALL_TO_ONE:
      return DirectedVertex.getEdgeSegmentsForVertexLambda(!invertDirection); // normally upstream, unless inverted
    default:
      LOGGER.severe(String.format("Shortest path search type %s not supported by getEdgeSegmentsInDirectionLambda", shortestPathSearchType.toString()));
      return null;
    }
  }
}

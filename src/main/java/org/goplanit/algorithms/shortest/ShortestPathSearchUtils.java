package org.goplanit.algorithms.shortest;

import java.util.function.Function;
import java.util.logging.Logger;

import org.goplanit.assignment.common.bush.RootedBush;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import java.util.function.Predicate;
import org.goplanit.network.layer.macroscopic.MacroscopicNetworkLayerUtils;
import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.physical.LinkSegment;

/**
 * Some common utilities for various shortest path algorithms. The lambdas provided base their direction
 * (up/downstream) on the direction used in the shortest path search NOT the results it yields. To traverse the
 * results, the opposite direciton is required which can be obtained by inverting the call via a flag.
 * <p>
 * It also holds the ready-made searches built on {@link ShortestPathBreadthFirst}: the shortest connections from a
 * vertex within a distance, and those a mode can make from a link segment respecting its banned turns, so every caller
 * asks the same search rather than setting up its own.
 * </p>
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
  public static Function<EdgeSegment, DirectedVertex> getVertexFromEdgeSegmentLambda(
          ShortestSearchType shortestPathSearchType) {
    return getVertexFromEdgeSegmentLambda(shortestPathSearchType, false);
  }

  /**
   * Based on the bush configuration, collect the vertex of an edge segment (up or downstream) compatible with a
   * (shortest) path search. When the bush is not inverted, i.e., runs from a single origin to one or more
   * destinations, the downstream vertex is collected, otherwise the upstream vertex. The invertDirection parameter
   * swaps this direction.
   *
   * @param bush            based on the configuration of the bush determine which vertex to collect.
   * @param invertDirection flag to invert direction compared to "regular" result
   * @return lambda collecting vertex based on given edge segment
   */
  public static Function<EdgeSegment, DirectedVertex> getVertexFromEdgeSegmentLambda(
      RootedBush<?,?,?> bush, boolean invertDirection) {
    if (bush.isInverted()) {
      return getVertexFromEdgeSegmentLambda(ShortestSearchType.ALL_TO_ONE, invertDirection);
    } else {
      return getVertexFromEdgeSegmentLambda(ShortestSearchType.ONE_TO_ALL, invertDirection);
    }
  }

  /**
   * Based on the bush configuration, collect the vertex of an edge segment (up or downstream) compatible with a
   * (shortest) path search. When the bush is not inverted, i.e., runs from a single origin to one or more
   * destinations, the downstream vertex is collected, otherwise the upstream vertex.
   *
   * @param bush based on the configuration of the bush determine which vertex to collect.
   * @return lambda collecting vertex based on given edge segment
   */
  public static Function<EdgeSegment, DirectedVertex> getVertexFromEdgeSegmentLambda(RootedBush<?,?,?> bush) {
    return getVertexFromEdgeSegmentLambda(bush, false);
  }

  /**
   * Based on the search type we collect different vertex direction from edge segment (up or downstream) during
   * shortest path search. This method provides the Lambda that performs this collection.
   * 
   * @param shortestPathSearchType to base lambda on
   * @param invertDirection        flag to invert direction compared to "regular" result
   * @return lambda collecting vertex based on given edge segment
   */
  public static Function<EdgeSegment, DirectedVertex> getVertexFromEdgeSegmentLambda(
          ShortestSearchType shortestPathSearchType, boolean invertDirection) {
    switch (shortestPathSearchType) {
    case ONE_TO_ALL:
    case ONE_TO_ONE:
      return EdgeSegment.getVertexForEdgeSegmentLambda(invertDirection); // normally downstream unless inverted
    case ALL_TO_ONE:
      return EdgeSegment.getVertexForEdgeSegmentLambda(!invertDirection); // normally upstream unless inverted
    default:
      LOGGER.severe(String.format("Shortest path search type %s not supported by getVertexFromEdgeSegmentLambda",
              shortestPathSearchType));
      return null;
    }
  }

  /**
   * Based on the bush configuration, collect the edge segments of a vertex (entry or exit segments) compatible with a
   * (shortest) path search. When the bush is not inverted, i.e., runs from a single origin to one or more
   * destinations, the downstream edge segments are collected, otherwise the upstream edge segments. The
   * invertDirection parameter swaps this direction.
   * 
   * @param bush            to base lambda on
   * @param invertDirection flag to invert direction compared to "regular" result
   * @return lambda collecting edge segments in given direction based on vertex provided
   */
  public static Function<DirectedVertex, Iterable<? extends EdgeSegment>> getEdgeSegmentsInDirectionLambda(
      RootedBush<?,?,?> bush, boolean invertDirection) {
    if (bush.isInverted()) {
      return getEdgeSegmentsInDirectionLambda(ShortestSearchType.ALL_TO_ONE, invertDirection);
    } else {
      return getEdgeSegmentsInDirectionLambda(ShortestSearchType.ONE_TO_ALL, invertDirection);
    }
  }

  /**
   * Based on the bush configuration, collect the edge segments of a vertex (entry or exit segments) compatible with a
   * (shortest) path search. When the bush is not inverted, i.e., runs from a single origin to one or more
   * destinations, the downstream edge segments are collected, otherwise the upstream edge segments.
   * 
   * @param bush to base lambda on
   * @return lambda collecting edge segments in given direction based on vertex provided
   */
  public static Function<DirectedVertex, Iterable<? extends EdgeSegment>> getEdgeSegmentsInDirectionLambda(
      RootedBush<?,?,?> bush) {
    return getEdgeSegmentsInDirectionLambda(bush, false);
  }

  /**
   * Based on the search type we collect edge segments (up or downstream) for a given vertex. This method provides
   * the Lambda that performs this collection.
   * 
   * @param shortestPathSearchType to base lambda on
   * @return lambda collecting edge segments in given direction based on vertex provided
   */
  public static Function<DirectedVertex, Iterable<? extends EdgeSegment>> getEdgeSegmentsInDirectionLambda(
          ShortestSearchType shortestPathSearchType) {
    return getEdgeSegmentsInDirectionLambda(shortestPathSearchType, false);
  }

  /**
   * Based on the search type we collect edge segments (up or downstream) for a given vertex. This method provides
   * the Lambda that performs this collection.
   * 
   * @param shortestPathSearchType to base lambda on
   * @param invertDirection        flag to invert direction compared to "regular" result
   * @return lambda collecting edge segments in given direction based on vertex provided
   */
  public static Function<DirectedVertex, Iterable<? extends EdgeSegment>> getEdgeSegmentsInDirectionLambda(
          ShortestSearchType shortestPathSearchType, boolean invertDirection) {

    switch (shortestPathSearchType) {
    case ONE_TO_ALL:
    case ONE_TO_ONE:
      return DirectedVertex.getEdgeSegmentsForVertexLambda(invertDirection); // normally downstream, unless inverted
    case ALL_TO_ONE:
      return DirectedVertex.getEdgeSegmentsForVertexLambda(!invertDirection); // normally upstream, unless inverted
    default:
      LOGGER.severe(String.format("Shortest path search type %s not supported by getEdgeSegmentsInDirectionLambda",
              shortestPathSearchType));
      return null;
    }
  }

  /**
   * Find the shortest connections from a vertex to the vertices within a distance, over the edges that may be followed,
   * such as the links a motorised road mode may use, in either direction, one-way or not
   *
   * @param <V> type of the vertices
   * @param source vertex to start from
   * @param canFollow true for an edge the connections may follow
   * @param canReach true for a vertex a connection may reach and pass
   * @param maximumDistanceMeters furthest a vertex reached may be
   * @return the vertices reached, the source included, with their distances in metres and the connections to them
   */
  public static <V extends Vertex> ShortestPathBreadthFirstResult<V> findShortestConnections(
      V source,
      Predicate<? super Edge> canFollow,
      Predicate<V> canReach,
      double maximumDistanceMeters) {
    return ShortestPathBreadthFirst.executeAlongEdges(source,
        edge -> canFollow.test(edge) ? edge.getLengthMeters() : Double.POSITIVE_INFINITY, canReach,
        maximumDistanceMeters);
  }

  /**
   * Find the shortest connections a mode can make from a link segment, in the traffic direction, over the link
   * segments it may continue on, such as the internal segments of a spatial intersection or the link segments of a
   * toll road, to those it may reach, such as the intersection's exits. It runs over link segments rather than nodes,
   * so a turn banned between two link segments is respected even when their node can be reached another way
   * <p>
   * The search never goes back along the link it just came along, nor onto the start's own link. Otherwise a mode
   * could get round a turn banned at a node by driving into a two-way link and straight back, which no driver does, and
   * a connection would count as one the mode can make. For example, from where a toll road is entered, the places it
   * is left that a car can reach along it, with the distance to each.
   * </p>
   *
   * @param layer holding the banned turns
   * @param start link segment to start from
   * @param mode travelling, only over link segments allowing it
   * @param canContinueOn true for a link segment reached that the search may continue beyond; the start is always
   *                      continued on
   * @param canReach true for a link segment the search may reach
   * @param respectBannedTurns when true a turn banned for the mode is not taken, when false banned turns are ignored
   * @return the link segments reached, the start included, each with the shortest connection to it and its distance
   *         from where the start ends, in metres
   */
  public static ShortestPathBreadthFirstResult<LinkSegment> findShortestConnectionsForMode(
      final MacroscopicNetworkLayer layer,
      final LinkSegment start,
      final Mode mode,
      final Predicate<LinkSegment> canContinueOn,
      final Predicate<LinkSegment> canReach,
      boolean respectBannedTurns) {
    return ShortestPathBreadthFirst.<LinkSegment>executeAlongEdgeSegments(start, true,
        (from, onto) -> onto.isModeAllowed(mode) && onto.getParent() != start.getParent() &&
            onto.getParent() != from.getParent() && canReach.test(onto) &&
            !(respectBannedTurns && MacroscopicNetworkLayerUtils.isTurnBannedForMode(layer, from, onto, mode)),
        canContinueOn, segment -> segment.getParent().getLengthMeters(), Double.POSITIVE_INFINITY);
  }
}

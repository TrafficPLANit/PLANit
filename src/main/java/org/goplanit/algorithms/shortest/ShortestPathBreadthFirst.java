package org.goplanit.algorithms.shortest;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.math.Precision;

/**
 * Shortest search over the part of a graph around where it starts, such as the links of one junction. It searches
 * breadth first, and looks at a vertex or edge segment again when it finds a shorter way to it, so the costs it finds
 * are the shortest even when steps differ in cost. Unlike {@link ShortestPathDijkstra} it needs no costs for the whole
 * network and keeps only what it reaches, and it can stop at a maximum cost. Where only reachability matters, the plain
 * breadth-first search of the graph utilities is enough.
 * <p>
 * It searches the graph itself, in one of two ways:
 * </p>
 * <ul>
 *   <li>along edges, in either direction: the vertices within a cost of a vertex, such as the nodes within 25 m along
 *   the road whichever way its links run, see
 *   {@link #executeAlongEdges(Vertex, ToDoubleFunction, Predicate, double)};</li>
 *   <li>along edge segments, in their direction: the edge segments a traveller can reach from an edge segment, where
 *   the turn from one onto the next can be refused, such as by a banned turn, see
 *   {@link #executeAlongEdgeSegments(EdgeSegment, BiPredicate, Predicate, ToDoubleFunction, double)}.</li>
 * </ul>
 * <p>
 * Costs within {@link Precision#EPSILON_9} of each other count as equal; of equal ways, the first found is kept.
 * </p>
 *
 * @author markr
 */
public final class ShortestPathBreadthFirst {

  /**
   * Not instantiable
   */
  private ShortestPathBreadthFirst() {
  }

  /**
   * Search breadth first from a start, looking at an entity again when a shorter way to it is found
   *
   * @param <T> what is searched over
   * @param start to start from, at no cost
   * @param forEachNext for an entity reached, offers each entity that follows it with the cost of the step
   * @param maximumCost highest cost an entity may be reached at
   * @return the entities reached, with their costs and the connections to them
   */
  private static <T> ShortestPathBreadthFirstResult<T> execute(
      T start, BiConsumer<T, BiConsumer<T, Double>> forEachNext, double maximumCost) {
    var costToReach = new LinkedHashMap<T, Double>();
    var previous = new LinkedHashMap<T, T>();
    var open = new ArrayDeque<T>();
    var queued = new HashSet<T>();
    costToReach.put(start, 0.0);
    open.add(start);
    queued.add(start);
    while (!open.isEmpty()) {
      var current = open.poll();
      queued.remove(current);
      double currentCost = costToReach.get(current);
      forEachNext.accept(current, (next, stepCost) -> {
        double nextCost = currentCost + stepCost;
        var known = costToReach.get(next);
        if (nextCost > maximumCost + Precision.EPSILON_9 ||
            (known != null && nextCost >= known - Precision.EPSILON_9)) {
          return;
        }
        costToReach.put(next, nextCost);
        previous.put(next, current);
        if (queued.add(next)) {
          open.add(next);
        }
      });
    }
    return new ShortestPathBreadthFirstResult<>(start, costToReach, previous);
  }

  /**
   * Find the shortest connections from a vertex to the vertices around it, along edges in either direction, whichever
   * way their edge segments run
   *
   * @param <V> vertex type
   * @param source to start from
   * @param edgeCost cost of following an edge; an edge with an infinite cost is not followed
   * @param canReach true for a vertex a connection may reach and pass; the source always counts as reached
   * @param maximumCost highest cost a vertex may be reached at
   * @return the vertices reached, the source included, with their costs and the connections to them
   */
  @SuppressWarnings("unchecked")
  public static <V extends Vertex> ShortestPathBreadthFirstResult<V> executeAlongEdges(
      V source, ToDoubleFunction<Edge> edgeCost, Predicate<V> canReach, double maximumCost) {
    return execute(source, (vertex, next) -> {
      for (var edge : vertex.getEdges()) {
        var otherEnd = (V) edge.getOtherVertex(vertex);
        double cost = edgeCost.applyAsDouble(edge);
        if (otherEnd != vertex && cost != Double.POSITIVE_INFINITY && canReach.test(otherEnd)) {
          next.accept(otherEnd, cost);
        }
      }
    }, maximumCost);
  }

  /**
   * Find the shortest connections from where an edge segment ends to the edge segments a traveller can reach from it,
   * along edge segments in their direction. The cost to reach an edge segment is that of the edge segments passed
   * before it, so the start costs nothing and a reached edge segment's own cost is not part of its cost
   *
   * @param <ES> edge segment type
   * @param start the edge segment to start from, at its downstream vertex
   * @param canTurnOnto true when a traveller may turn from the first edge segment onto the second, which leaves the
   *                    vertex the first one ends at
   * @param canContinueOn true for an edge segment reached that a traveller may continue on beyond; the start is always
   *                      continued on, and an edge segment that is not is reached but leads nowhere
   * @param segmentCost cost of passing an edge segment
   * @param maximumCost highest cost an edge segment may be reached at
   * @return the edge segments reached, the start included, with their costs and the connections to them
   */
  @SuppressWarnings("unchecked")
  public static <ES extends EdgeSegment> ShortestPathBreadthFirstResult<ES> executeAlongEdgeSegments(
      ES start, BiPredicate<ES, ES> canTurnOnto, Predicate<ES> canContinueOn, ToDoubleFunction<ES> segmentCost,
      double maximumCost) {
    return execute(start, (segment, next) -> {
      if (segment != start && !canContinueOn.test(segment)) {
        return;
      }
      double cost = segment == start ? 0 : segmentCost.applyAsDouble(segment);
      for (var onto : segment.getDownstreamVertex().getExitEdgeSegments()) {
        if (canTurnOnto.test(segment, (ES) onto)) {
          next.accept((ES) onto, cost);
        }
      }
    }, maximumCost);
  }
}

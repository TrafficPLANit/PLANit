package org.goplanit.algorithms.shortest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What a {@link ShortestPathBreadthFirst} search reached: each vertex or edge segment, the cost to reach it, and the
 * shortest connection to it from where the search started
 *
 * @param <T> what was searched over, vertices or edge segments
 * @author markr
 */
public final class ShortestPathBreadthFirstResult<T> {

  /** where the search started */
  private final T start;

  /** cost to reach each entity reached, in the order first reached */
  private final Map<T, Double> costToReach;

  /** the entity before each entity reached, on the shortest connection to it */
  private final Map<T, T> previous;

  /**
   * Constructor
   *
   * @param start where the search started
   * @param costToReach each entity reached with its cost
   * @param previous the entity before each entity reached, on the shortest connection to it
   */
  ShortestPathBreadthFirstResult(T start, Map<T, Double> costToReach, Map<T, T> previous) {
    this.start = start;
    this.costToReach = costToReach;
    this.previous = previous;
  }

  /**
   * The entities reached, the start included, in the order first reached
   *
   * @return entities reached, unmodifiable
   */
  public Set<T> getReached() {
    return Collections.unmodifiableSet(costToReach.keySet());
  }

  /**
   * Verify if an entity was reached
   *
   * @param entity to verify
   * @return true when reached, false otherwise
   */
  public boolean isReached(T entity) {
    return costToReach.containsKey(entity);
  }

  /**
   * The cost of the shortest connection to an entity
   *
   * @param entity to get the cost for
   * @return cost, {@link Double#MAX_VALUE} when not reached
   */
  public double getCostToReach(T entity) {
    return costToReach.getOrDefault(entity, Double.MAX_VALUE);
  }

  /**
   * The entities on the shortest connection from the start to an entity reached, both included
   *
   * @param entity reached
   * @return entities from the start to the entity, empty when not reached
   */
  public List<T> getEntitiesOnConnectionTo(T entity) {
    if (!isReached(entity)) {
      return List.of();
    }
    var onConnection = new ArrayList<T>();
    for (var current = entity; current != null; current = current == start ? null : previous.get(current)) {
      onConnection.add(current);
    }
    Collections.reverse(onConnection);
    return onConnection;
  }
}

package org.planit.graph;

import java.util.function.Function;

import org.planit.utils.graph.GraphEntities;
import org.planit.utils.graph.GraphEntity;
import org.planit.utils.id.ManagedIdEntitiesImpl;

/**
 * Base class for containers of entities on graph
 * 
 * @author markr
 *
 * @param <E> type of graph entity
 */
public abstract class GraphEntitiesImpl<E extends GraphEntity> extends ManagedIdEntitiesImpl<E> implements GraphEntities<E> {

  /**
   * Constructor
   * 
   * @param valueToKey         the mapping from key to value of the graph entity
   * @param graphEntityIdClass should reflect the base class signature used for generating the graph entities internal id of this class when creating it via the factory of this
   *                           container
   */
  protected GraphEntitiesImpl(Function<E, Long> valueToKey, final Class<? extends GraphEntity> graphEntityIdClass) {
    super(valueToKey, graphEntityIdClass);
  }

  /**
   * copy constructor
   * 
   * @param other to copy
   */
  protected GraphEntitiesImpl(GraphEntitiesImpl<E> other) {
    super(other);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract GraphEntitiesImpl<E> clone();

}

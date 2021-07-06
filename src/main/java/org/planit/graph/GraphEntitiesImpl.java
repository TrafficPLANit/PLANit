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
   * @param valueToKey the mapping from key to value of the graph entity
   */
  protected GraphEntitiesImpl(Function<E, Long> valueToKey) {
    super(valueToKey);
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

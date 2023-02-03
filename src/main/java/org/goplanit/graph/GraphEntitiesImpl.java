package org.goplanit.graph;

import java.util.TreeMap;
import java.util.function.Function;

import org.goplanit.utils.graph.GraphEntities;
import org.goplanit.utils.graph.GraphEntity;
import org.goplanit.utils.wrapper.LongMapWrapperImpl;

/**
 * Base class for containers of entities on graph
 * 
 * @author markr
 *
 * @param <E> type of graph entity
 */
public abstract class GraphEntitiesImpl<E extends GraphEntity> extends LongMapWrapperImpl<E> implements GraphEntities<E> {

  /**
   * Constructor
   * 
   * @param valueToKey the mapping from key to value of the graph entity
   */
  protected GraphEntitiesImpl(Function<E, Long> valueToKey) {
    super(new TreeMap<>(), valueToKey);
  }

  /**
   * copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected GraphEntitiesImpl(GraphEntitiesImpl<E> other, boolean deepCopy) {
    super(other);

    if(deepCopy){
      this.clear();
      other.forEach(v -> this.register((E) v.deepClone()));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract GraphEntitiesImpl<E> shallowClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract GraphEntitiesImpl<E> deepClone();

}

package org.goplanit.graph;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.goplanit.utils.graph.GraphEntities;
import org.goplanit.utils.graph.GraphEntity;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.misc.Pair;
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
   * @param biConsumer when deepCopy applied to each original and copy, may be null
   */
  protected GraphEntitiesImpl(GraphEntitiesImpl<E> other, boolean deepCopy, BiConsumer<E, E> biConsumer) {
    super(other);

    // super already shallow copied, so only redo it if we do a deep copy
    if(deepCopy){
      this.clear();
      other.forEach(v -> {
        var copy = (E) v.deepClone();
        this.register(copy);
        if(deepCopy && biConsumer != null) {
          biConsumer.accept(v, copy);
        }
      });
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

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract GraphEntitiesImpl<E> deepCloneWithMapping(BiConsumer<E,E> graphEntityMapper);

}

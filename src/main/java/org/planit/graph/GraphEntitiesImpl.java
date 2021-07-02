package org.planit.graph;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import org.planit.utils.graph.GraphEntities;
import org.planit.utils.graph.GraphEntity;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.wrapper.LongMapWrapperImpl;

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
    super(new TreeMap<Long, E>(), valueToKey);
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
   * updates the container keys based on currently presiding ids. Only to be used when an external force has changed already registered edges' their ids
   */
  protected void updateIdMapping() {
    /* redo mapping */
    Map<Long, E> updatedMap = new TreeMap<Long, E>();
    getMap().forEach((oldId, entity) -> updatedMap.put(getValueToKey().apply(entity), entity));
    getMap().clear();
    setMap(updatedMap);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateIds() {
    if (!isEmpty()) {
      /* remove gaps by simply resetting and recreating all entity ids */
      IdGenerator.reset(getFactory().getIdGroupingToken(), iterator().next().getIdClass() /* e.g. Edge.class, vertex.class etc. */);
      forEach(entity -> entity.recreateId(getFactory().getIdGroupingToken()));
      updateIdMapping();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract GraphEntitiesImpl<E> clone();

}

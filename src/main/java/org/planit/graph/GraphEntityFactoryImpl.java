package org.planit.graph;

import org.planit.utils.graph.GraphEntities;
import org.planit.utils.graph.GraphEntity;
import org.planit.utils.graph.GraphEntityFactory;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.ManagedIdEntityFactoryImpl;

/**
 * Base implementation for creating and registering graph entities on underlying container and conducting changes to ids based on the factory settings for egenrating ids.
 * 
 * @author markr
 *
 * @param <E> type of graph entity
 */
public abstract class GraphEntityFactoryImpl<E extends GraphEntity> extends ManagedIdEntityFactoryImpl<E> implements GraphEntityFactory<E> {

  /** container on which newly created entities are to be registered */
  private final GraphEntities<E> graphEntities;

  /**
   * The entities to register on
   * 
   * @return graphEntities
   */
  protected GraphEntities<E> getGraphEntities() {
    return graphEntities;
  }

  /**
   * Constructor
   * 
   * @param groupIdToken  to use for creating element ids
   * @param graphEntities to register the created instances on
   */
  protected GraphEntityFactoryImpl(IdGroupingToken groupIdToken, GraphEntities<E> graphEntities) {
    super(groupIdToken);
    this.graphEntities = graphEntities;
  }

}

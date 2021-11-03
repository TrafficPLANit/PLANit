package org.goplanit.graph;

import org.goplanit.utils.graph.GraphEntities;
import org.goplanit.utils.graph.GraphEntity;
import org.goplanit.utils.graph.GraphEntityFactory;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;

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

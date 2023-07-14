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
  private GraphEntities<E> graphEntities;

  /**
   * Use with caution as it replaces the managed id container in its entirety
   *
   * @param graphEntities to use as the managed id container
   */
  protected void setGraphEntities( GraphEntities<E> graphEntities){
    this.graphEntities = graphEntities;
  }

  /**
   * The entities to register on
   * 
   * @return graphEntities
   */
  protected GraphEntities<E> getGraphEntities() {
    return graphEntities;
  }

  /**
   * Constructor without a container. Only use when the graph entities are know to be set manually afterward by derived class
   *
   * @param groupIdToken  to use for creating element ids
   */
  protected GraphEntityFactoryImpl(IdGroupingToken groupIdToken) {
    super(groupIdToken);
    this.graphEntities = null;
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

  /**
   * Constructor. Do not use unless the entities do not rely on id generation but obtain their unique id from elsewhere
   * 
   * @param graphEntities to register the created instances on
   */
  protected GraphEntityFactoryImpl(GraphEntities<E> graphEntities) {
    super();
    this.graphEntities = graphEntities;
  }

}

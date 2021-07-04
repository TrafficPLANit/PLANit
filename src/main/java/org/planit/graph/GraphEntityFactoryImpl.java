package org.planit.graph;

import org.planit.utils.graph.GraphEntities;
import org.planit.utils.graph.GraphEntity;
import org.planit.utils.graph.GraphEntityFactory;
import org.planit.utils.id.IdGroupingToken;

/**
 * Base implementation for creating and registering graph entities on underlying container and conducting changes to ids based on the factory settings for egenrating ids.
 * 
 * @author markr
 *
 * @param <E> type of graph entity
 */
public abstract class GraphEntityFactoryImpl<E extends GraphEntity> implements GraphEntityFactory<E> {

  /** the id group token */
  protected IdGroupingToken groupIdToken;

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
    this.groupIdToken = groupIdToken;
    this.graphEntities = graphEntities;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setIdGroupingToken(IdGroupingToken groupIdToken) {
    this.groupIdToken = groupIdToken;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IdGroupingToken getIdGroupingToken() {
    return this.groupIdToken;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public E createUniqueCopyOf(GraphEntity entityToCopy) {
    /* shallow copy as is */
    @SuppressWarnings("unchecked")
    E copy = (E) entityToCopy.clone();
    /* recreate id and register */
    copy.recreateId(getIdGroupingToken());
    return copy;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public E registerUniqueCopyOf(GraphEntity entityToCopy) {
    E copy = createUniqueCopyOf(entityToCopy);
    graphEntities.register(copy);
    return copy;
  }
}

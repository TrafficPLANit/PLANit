package org.planit.graph;

import java.util.logging.Logger;

import org.planit.utils.graph.GraphEntities;
import org.planit.utils.graph.GraphEntityFactory;
import org.planit.utils.id.ExternalIdable;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;

public class GraphEntityFactoryImpl<E extends ExternalIdable> implements GraphEntityFactory<E> {

  /** the logger ot use */
  private static final Logger LOGGER = Logger.getLogger(GraphEntityFactoryImpl.class.getCanonicalName());

  /** the id group token */
  protected IdGroupingToken groupIdToken;

  /** the class to rgister the generated ids under */
  protected final Class<? extends ExternalIdable> groupIdClass;

  /**
   * Constructor
   * 
   * @param groupIdToken to use for creating element ids
   * @param groupIdClass to register the created ids on
   */
  public GraphEntityFactoryImpl(IdGroupingToken groupIdToken, final Class<? extends ExternalIdable> groupIdClass) {
    this.groupIdToken = groupIdToken;
    this.groupIdClass = groupIdClass;
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
  public void recreateIds(GraphEntities<E> entities) {
    /* remove gaps by simply resetting and recreating all entity ids */
    IdGenerator.reset(getIdGroupingToken(), groupIdClass /* e.g. Edge.class, vertex.class etc. */);

    for (E entity : entities) {
      ((EdgeImpl) entity).setId(entity.generateId(getIdGroupingToken(), groupIdClass));
    }

    ((EdgesImpl<?>) entities).updateIdMapping();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public E createUniqueCopyOf(E edgeToCopy) {
    /* shallow copy as is */
    E copy = (E) edgeToCopy.clone();
    /* make unique copy by updating id */
    copy.setId(copy.generateId(getIdGroupingToken()));
    return copy;
  }
}

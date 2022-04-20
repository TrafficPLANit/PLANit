package org.goplanit.graph;

import org.goplanit.utils.graph.GraphEntity;
import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdAble;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * Base implementation for graph entity (edge, vertex, etc.)
 * 
 * @author markr
 *
 */
public abstract class GraphEntityImpl extends ExternalIdAbleImpl implements GraphEntity {

  /**
   * generate id based on provided token and class and set it on this instance
   * 
   * @param tokenId to use for id generation
   * @return created and set id
   */
  protected long generateAndSetId(IdGroupingToken tokenId) {
    // TODO: likely best replaced by methods already present in IdAble(Impl)
    long newId = generateId(tokenId, getIdClass());
    setId(newId);
    return newId;
  }

  /**
   * Generate an id based on provided token and class
   * 
   * @param idGroupingToken to use
   * @param clazz           to register for
   * @return generated id
   */
  protected static long generateId(IdGroupingToken idGroupingToken, Class<? extends IdAble> clazz) {
    return IdGenerator.generateId(idGroupingToken, clazz);
  }

  /**
   * Constructor
   * 
   * @param tokenId to use
   * @param clazz   to register for
   */
  protected GraphEntityImpl(IdGroupingToken tokenId, Class<? extends GraphEntity> clazz) {
    super(generateId(tokenId, clazz));
  }

  /**
   * Constructor. Note only to be used when entity is not reliant on an id generation procedure
   * 
   * @param id to use
   */
  protected GraphEntityImpl(long id) {
    super(id);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy from
   */
  protected GraphEntityImpl(GraphEntityImpl other) {
    super(other);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    return generateAndSetId(tokenId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return String.format("%s [%d %s]", getClass().getSimpleName(), getId(), getXmlId() == null ? "" : getXmlId());
  }
}

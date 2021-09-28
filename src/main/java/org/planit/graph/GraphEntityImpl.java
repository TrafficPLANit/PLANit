package org.planit.graph;

import org.planit.utils.graph.GraphEntity;
import org.planit.utils.id.ExternalIdAbleImpl;
import org.planit.utils.id.IdAble;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;

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
   * @param clazz   to use for id generation
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
   * @return
   */
  protected static long generateId(IdGroupingToken idGroupingToken, Class<? extends IdAble> clazz) {
    return IdGenerator.generateId(idGroupingToken, clazz);
  }

  /**
   * Constructor
   * 
   * @param id to use
   */
  public GraphEntityImpl(IdGroupingToken tokenId, Class<? extends GraphEntity> clazz) {
    super(generateId(tokenId, clazz));
  }

  /**
   * Copy constructor
   * 
   * @param other to copy from
   */
  public GraphEntityImpl(GraphEntityImpl other) {
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
    return String.format("[ %d %s]", getId(), getXmlId() == null ? "" : getXmlId());
  }
}

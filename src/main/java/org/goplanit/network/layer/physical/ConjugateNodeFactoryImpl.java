package org.goplanit.network.layer.physical;

import org.goplanit.graph.GraphEntityFactoryImpl;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.physical.ConjugateNode;
import org.goplanit.utils.network.layer.physical.ConjugateNodeFactory;
import org.goplanit.utils.network.layer.physical.ConjugateNodes;
import org.goplanit.utils.network.layer.physical.Link;

/**
 * Factory for creating nodes on conjugate nodes container.
 * 
 * @author markr
 */
public class ConjugateNodeFactoryImpl extends GraphEntityFactoryImpl<ConjugateNode> implements ConjugateNodeFactory {

  /**
   * Constructor
   * 
   * @param groupId   to use
   * @param container to use
   */
  protected ConjugateNodeFactoryImpl(final IdGroupingToken groupId, final ConjugateNodes container) {
    super(groupId, container);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateNode createNew(final Link originalLink) {
    return new ConjugateNodeImpl(originalLink);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateNode registerNew(final Link originalLink) {
    final ConjugateNode newEntity = createNew(originalLink);
    getGraphEntities().register(newEntity);
    return newEntity;
  }

}

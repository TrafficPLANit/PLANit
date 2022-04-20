package org.goplanit.network.layer.physical;

import org.goplanit.graph.GraphEntityFactoryImpl;
import org.goplanit.utils.network.layer.physical.ConjugateNode;
import org.goplanit.utils.network.layer.physical.ConjugateNodeFactory;
import org.goplanit.utils.network.layer.physical.ConjugateNodes;
import org.goplanit.utils.network.layer.physical.Link;

/**
 * Factory for creating nodes on conjugate nodes container. Note that because conjugate nodes are 1:1 repalcement for original links we sync their ids by default so they can be
 * used interchangeably
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
  protected ConjugateNodeFactoryImpl(final ConjugateNodes container) {
    super(container);
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

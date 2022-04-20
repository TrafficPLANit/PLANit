package org.goplanit.network.virtual;

import org.goplanit.graph.GraphEntityFactoryImpl;
import org.goplanit.utils.network.virtual.ConjugateConnectoidNode;
import org.goplanit.utils.network.virtual.ConjugateConnectoidNodeFactory;
import org.goplanit.utils.network.virtual.ConjugateConnectoidNodes;
import org.goplanit.utils.network.virtual.ConnectoidEdge;

/**
 * Factory for creating conjugate connectoid nodes on container. Note that because conjugate connectoid nodes are 1:1 replacement for original connectoid links we sync their ids by
 * default so they can be used interchangeably
 * 
 * @author markr
 */
public class ConjugateConnectoidNodeFactoryImpl extends GraphEntityFactoryImpl<ConjugateConnectoidNode> implements ConjugateConnectoidNodeFactory {

  /**
   * Constructor
   * 
   * @param groupId   to use
   * @param container to use
   */
  protected ConjugateConnectoidNodeFactoryImpl(final ConjugateConnectoidNodes container) {
    super(container);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateConnectoidNode createNew(final ConnectoidEdge originalConnectoidEdge) {
    return new ConjugateConnectoidNodeImpl(originalConnectoidEdge);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateConnectoidNode registerNew(final ConnectoidEdge originalConnectoidEdge) {
    final ConjugateConnectoidNode newEntity = createNew(originalConnectoidEdge);
    getGraphEntities().register(newEntity);
    return newEntity;
  }

}

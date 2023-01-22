package org.goplanit.network.virtual;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.network.virtual.ConjugateConnectoidNode;
import org.goplanit.utils.network.virtual.ConjugateConnectoidNodeFactory;
import org.goplanit.utils.network.virtual.ConjugateConnectoidNodes;

/**
 * 
 * Conjugate connectoid nodes managed container implementation. Note that each conjugate connectoid node will be indexed by its original link's id to allow for easy matching. In
 * turn it is expected that the conjugate node's id is also synced to the original link's id anyway. However we explicit map to the original link's id as this ensures that when
 * recreating managed ids the correct id is used
 * 
 * @author markr
 *
 */
public class ConjugateConnectoidNodesImpl extends ManagedIdEntitiesImpl<ConjugateConnectoidNode> implements ConjugateConnectoidNodes {

  /** factory to use */
  private final ConjugateConnectoidNodeFactory factory;

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public ConjugateConnectoidNodesImpl(final IdGroupingToken groupId) {
    super(ConjugateConnectoidNode::getId);
    this.factory = new ConjugateConnectoidNodeFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param groupId                        to use for creating ids for instances
   * @param conjugateConnectoidNodeFactory the factory to use
   */
  public ConjugateConnectoidNodesImpl(final IdGroupingToken groupId, ConjugateConnectoidNodeFactory conjugateConnectoidNodeFactory) {
    super(n -> n.getOriginalEdge().getId());
    this.factory = conjugateConnectoidNodeFactory;
  }

  /**
   * Copy constructor, also creates new factory with this as its underlying container
   * 
   * @param nodesImpl to copy
   */
  public ConjugateConnectoidNodesImpl(ConjugateConnectoidNodesImpl nodesImpl) {
    super(nodesImpl);
    this.factory = new ConjugateConnectoidNodeFactoryImpl(nodesImpl.factory.getIdGroupingToken(), this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateConnectoidNodeFactory getFactory() {
    return factory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateConnectoidNodesImpl clone() {
    return new ConjugateConnectoidNodesImpl(this);
  }

}

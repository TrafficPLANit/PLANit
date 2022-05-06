package org.goplanit.network.layer.physical;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.network.layer.physical.ConjugateNode;
import org.goplanit.utils.network.layer.physical.ConjugateNodeFactory;
import org.goplanit.utils.network.layer.physical.ConjugateNodes;

/**
 * 
 * Conjugate nodes primary managed container implementation. 
 * 
 * @author markr
 *
 */
public class ConjugateNodesImpl extends ManagedIdEntitiesImpl<ConjugateNode> implements ConjugateNodes {

  /** factory to use */
  private final ConjugateNodeFactory factory;

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public ConjugateNodesImpl(final IdGroupingToken groupId) {
    super(ConjugateNode::getId);
    this.factory = new ConjugateNodeFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param groupId     to use for creating ids for instances
   * @param nodeFactory the factory to use
   */
  public ConjugateNodesImpl(final IdGroupingToken groupId, ConjugateNodeFactory nodeFactory) {
    super(n -> n.getOriginalEdge().getId());
    this.factory = nodeFactory;
  }

  /**
   * Copy constructor
   * 
   * @param nodesImpl to copy
   */
  public ConjugateNodesImpl(ConjugateNodesImpl nodesImpl) {
    super(nodesImpl);
    this.factory = nodesImpl.factory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateNodeFactory getFactory() {
    return factory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateNodesImpl clone() {
    return new ConjugateNodesImpl(this);
  }

}

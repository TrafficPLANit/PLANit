package org.planit.network.layer.physical;

import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.ManagedIdEntitiesImpl;
import org.planit.utils.network.layer.physical.Node;
import org.planit.utils.network.layer.physical.NodeFactory;
import org.planit.utils.network.layer.physical.Nodes;

/**
 * 
 * Nodes primary managed container implementation
 * 
 * @author markr
 *
 */
public class NodesImpl extends ManagedIdEntitiesImpl<Node> implements Nodes {

  /** factory to use */
  private final NodeFactory nodeFactory;

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public NodesImpl(final IdGroupingToken groupId) {
    super(Node::getId, Node.VERTEX_ID_CLASS);
    this.nodeFactory = new NodeFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param groupId     to use for creating ids for instances
   * @param nodeFactory the factory to use
   */
  public NodesImpl(final IdGroupingToken groupId, NodeFactory nodeFactory) {
    super(Node::getId, Node.VERTEX_ID_CLASS);
    this.nodeFactory = nodeFactory;
  }

  /**
   * Copy constructor
   * 
   * @param nodesImpl to copy
   */
  public NodesImpl(NodesImpl nodesImpl) {
    super(nodesImpl);
    this.nodeFactory = nodesImpl.nodeFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NodeFactory getFactory() {
    return nodeFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateIds(boolean resetManagedIdClass) {
    /* always reset the additional node id class */
    IdGenerator.reset(getFactory().getIdGroupingToken(), Node.NODE_ID_CLASS);

    super.recreateIds(resetManagedIdClass);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NodesImpl clone() {
    return new NodesImpl(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    IdGenerator.reset(getFactory().getIdGroupingToken(), Node.NODE_ID_CLASS);
    super.reset();
  }
}

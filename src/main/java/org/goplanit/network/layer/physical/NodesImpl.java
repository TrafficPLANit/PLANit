package org.goplanit.network.layer.physical;

import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.network.layer.physical.NodeFactory;
import org.goplanit.utils.network.layer.physical.Nodes;

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
   * Copy constructor, also creates new factory with this as its underlying container
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep cpy, shallow copy otherwise
   */
  public NodesImpl(NodesImpl other, boolean deepCopy) {
    super(other, deepCopy);
    this.nodeFactory = new NodeFactoryImpl(other.nodeFactory.getIdGroupingToken(), this);
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
    return new NodesImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NodesImpl deepClone() {
    return new NodesImpl(this, true);
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

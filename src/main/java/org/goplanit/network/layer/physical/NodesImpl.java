package org.goplanit.network.layer.physical;

import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.network.layer.physical.ConjugateNode;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.network.layer.physical.NodeFactory;
import org.goplanit.utils.network.layer.physical.Nodes;

import java.util.function.BiConsumer;

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
   * @param mapper apply to each mapping from original to copy
   */
  public NodesImpl(NodesImpl other, boolean deepCopy, BiConsumer<Node,Node> mapper) {
    super(other, deepCopy, mapper);
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
  public NodesImpl shallowClone() {
    return new NodesImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NodesImpl deepClone() {
    return new NodesImpl(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NodesImpl deepCloneWithMapping(BiConsumer<Node,Node> mapper) {
    return new NodesImpl(this, true, mapper);
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

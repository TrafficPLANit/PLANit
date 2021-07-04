package org.planit.network.layer.physical;

import org.planit.graph.GraphEntitiesImpl;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.physical.Node;
import org.planit.utils.network.layer.physical.NodeFactory;
import org.planit.utils.network.layer.physical.Nodes;

/**
 * 
 * Nodes implementation
 * 
 * @author markr
 *
 */
public class NodesImpl extends GraphEntitiesImpl<Node> implements Nodes {

  /** factory to use */
  private final NodeFactory nodeFactory;

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public NodesImpl(final IdGroupingToken groupId) {
    super(Node::getId);
    this.nodeFactory = new NodeFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param groupId     to use for creating ids for instances
   * @param nodeFactory the factory to use
   */
  public NodesImpl(final IdGroupingToken groupId, NodeFactory nodeFactory) {
    super(Node::getId);
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
  public NodesImpl clone() {
    return new NodesImpl(this);
  }
}

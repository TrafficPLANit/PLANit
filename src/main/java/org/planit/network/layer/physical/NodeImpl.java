/**
 *
 */
package org.planit.network.layer.physical;

import java.util.logging.Logger;

import org.planit.graph.directed.DirectedVertexImpl;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.physical.Node;

/**
 * Node representation connected to one or more entry and exit links
 *
 * @author markr
 *
 */
public class NodeImpl extends DirectedVertexImpl implements Node {

  // Protected

  /** generated UID */
  private static final long serialVersionUID = 8237965522827691852L;

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(NodeImpl.class.getCanonicalName());

  /**
   * Unique node identifier
   */
  protected long nodeId;

  /** name of the node */
  protected String name;

  /**
   * generate unique node id
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @return nodeId
   */
  protected static long generateNodeId(final IdGroupingToken groupId) {
    return IdGenerator.generateId(groupId, Node.class);
  }

  /**
   * set the node id on this node
   * 
   * @param nodeId to set
   */
  protected void setNodeId(long nodeId) {
    this.nodeId = nodeId;
  }

  // Public

  /**
   * Node constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  protected NodeImpl(final IdGroupingToken groupId) {
    super(groupId);
    this.nodeId = generateNodeId(groupId);
  }

  /**
   * Copy constructor, see also {@code VertexImpl}
   * 
   * @param nodeImpl to copy
   */
  protected NodeImpl(NodeImpl nodeImpl) {
    super(nodeImpl);
    setNodeId(nodeImpl.getNodeId());
    setName(nodeImpl.getName());
  }

  // Getters-Setters

  /**
   * {@inheritDoc}
   */
  @Override
  public long getNodeId() {
    return nodeId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NodeImpl clone() {
    return new NodeImpl(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setName(String name) {
    this.name = name;
  }

}

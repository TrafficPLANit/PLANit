/**
 *
 */
package org.goplanit.network.layer.physical;

import java.util.logging.Logger;

import org.goplanit.graph.directed.DirectedVertexImpl;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.physical.Node;

/**
 * Node representation connected to one or more entry and exit links
 *
 * @author markr
 *
 */
public class NodeImpl<LS extends EdgeSegment> extends DirectedVertexImpl<LS> implements Node {

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
  protected NodeImpl(NodeImpl<LS> nodeImpl) {
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
  public NodeImpl<LS> clone() {
    return new NodeImpl<LS>(this);
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

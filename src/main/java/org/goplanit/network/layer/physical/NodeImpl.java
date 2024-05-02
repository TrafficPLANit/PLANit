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

  /**
   * recreate the internal node id and set it
   *
   * @param tokenId to use
   * @return the created node id
   */
  protected long recreateNodeId(IdGroupingToken tokenId) {
    long newNodeId = generateNodeId(tokenId);
    setNodeId(newNodeId);
    return newNodeId;
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
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep cpy, shallow copy otherwise
   */
  protected NodeImpl(NodeImpl<LS> other, boolean deepCopy) {
    super(other, deepCopy);
    setNodeId(other.getNodeId());
    setName(other.getName());
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
  public NodeImpl<LS> shallowClone() {
    return new NodeImpl<>(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NodeImpl<LS> deepClone() {
    return new NodeImpl<>(this, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * Recreate id and link id
   *
   * @param tokenId to use
   * @return created id (updated link Id is not returned)
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    recreateNodeId(tokenId);
    return super.recreateManagedIds(tokenId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setName(String name) {
    this.name = name;
  }

}

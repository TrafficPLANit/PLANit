/**
 *
 */
package org.planit.network.physical;

import org.planit.graph.DirectedVertexImpl;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.physical.Node;

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

  /**
   * Unique node identifier
   */
  protected final long nodeId;

  /**
   * generate unique node id
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @return nodeId
   */
  protected static int generateNodeId(final IdGroupingToken groupId) {
    return IdGenerator.generateId(groupId, Node.class);
  }

  // Public

  /**
   * Node constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public NodeImpl(final IdGroupingToken groupId) {
    super(groupId);
    this.nodeId = generateNodeId(groupId);
  }

  // Getters-Setters

  /**
   * {@inheritDoc}
   */
  @Override
  public long getNodeId() {
    return nodeId;
  }

}

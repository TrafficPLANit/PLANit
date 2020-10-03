/**
 *
 */
package org.planit.network.physical;

import java.util.logging.Logger;

import org.planit.graph.DirectedVertexImpl;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.MultiIdSetter;
import org.planit.utils.network.physical.Node;

/**
 * Node representation connected to one or more entry and exit links
 *
 * @author markr
 *
 */
public class NodeImpl extends DirectedVertexImpl implements Node, MultiIdSetter<Long> {

  // Protected

  /** generated UID */
  private static final long serialVersionUID = 8237965522827691852L;
  
  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(NodeImpl.class.getCanonicalName());  

  /**
   * Unique node identifier
   */
  protected long nodeId;

  /**
   * generate unique node id
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @return nodeId
   */
  protected static long generateNodeId(final IdGroupingToken groupId) {
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

  /**
   * Allows one to overwrite the underlying vertex id (first id), and node id (second id) at the same time
   * 
   * @param ids (only supports two ids, first edge id, second link id)
   */
  @Override
  public void overwriteIds(Long... ids) {
    if(ids.length != 2) {
      LOGGER.warning(String.format("overwriting node ids requires exactly two ids, one for the vertex and one for the node, we found %d, ignored",ids.length));
    }
    this.overwriteId(ids[0]);
    this.nodeId = ids[1];
  }
     

}

package org.planit.network.physical;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Node;

/**
 * Create network entities for a physical network simulation model
 * 
 * @author markr
 *
 */
public class PhysicalNetworkBuilderImpl implements PhysicalNetworkBuilder<Node, Link, LinkSegment> {

  /**
   * Contiguous id generation within this group id token for all instances created with factory methods in this class
   */
  protected IdGroupingToken groupId;

  // Public methods

  /**
   * {@inheritDoc}
   */
  @Override
  public Node createVertex() {
    return new NodeImpl(groupId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Link createEdge(Node nodeA, Node nodeB, final double length) throws PlanItException {
    return new LinkImpl(groupId, (Node) nodeA, (Node) nodeB, length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LinkSegment createEdgeSegment(Link parentLink, boolean directionAB) throws PlanItException {
    return new LinkSegmentImpl(getIdGroupingToken(), (Link) parentLink, directionAB);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setIdGroupingToken(IdGroupingToken groupId) {
    this.groupId = groupId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IdGroupingToken getIdGroupingToken() {
    return this.groupId;
  }

}

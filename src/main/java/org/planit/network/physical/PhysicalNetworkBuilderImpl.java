package org.planit.network.physical;

import java.util.logging.Logger;

import org.planit.graph.DirectedGraphBuilderImpl;
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

  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(PhysicalNetworkBuilderImpl.class.getCanonicalName());

  /** hold an implementation of directed graph builder to use its overlapping functionality */
  protected DirectedGraphBuilderImpl directedGraphBuilderImpl = new DirectedGraphBuilderImpl();

  // Public methods

  /**
   * {@inheritDoc}
   */
  @Override
  public Node createVertex() {
    return new NodeImpl(getIdGroupingToken());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Link createEdge(Node nodeA, Node nodeB, final double length) throws PlanItException {
    return new LinkImpl(getIdGroupingToken(), nodeA, nodeB, length);
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
  public void setIdGroupingToken(IdGroupingToken groupToken) {
    directedGraphBuilderImpl.setIdGroupingToken(groupToken);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IdGroupingToken getIdGroupingToken() {
    return directedGraphBuilderImpl.getIdGroupingToken();
  }

}

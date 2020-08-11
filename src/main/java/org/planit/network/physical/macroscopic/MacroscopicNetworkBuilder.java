package org.planit.network.physical.macroscopic;

import java.util.Map;

import org.planit.network.physical.LinkImpl;
import org.planit.network.physical.ModeImpl;
import org.planit.network.physical.NodeImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.Vertex;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.Mode;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentType;
import org.planit.utils.network.physical.macroscopic.MacroscopicModeProperties;

/**
 * Create network entities for a macroscopic simulation model
 * 
 * @author markr
 *
 */
public class MacroscopicNetworkBuilder implements MacroscopicPhysicalNetworkBuilder {

  /**
   * Contiguous id generation within this group id token for all instances created with factory methods in this class
   */
  protected IdGroupingToken groupId;

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
  public Link createEdge(Vertex nodeA, Vertex nodeB, final double length) throws PlanItException {
    if (!(nodeA instanceof Node) || !(nodeB instanceof Node)) {
      throw new PlanItException(String.format("provided vertices (%s (id:%d), %s(id:%d)) are not of type Node when creating a new Link", nodeA.getExternalId(), nodeA.getId(),
          nodeB.getExternalId(), nodeB.getId()));
    }
    return new LinkImpl(groupId, (Node) nodeA, (Node) nodeB, length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegment createEdgeSegment(Edge parentLink, boolean directionAB) throws PlanItException {
    if (!(parentLink instanceof Link)) {
      throw new PlanItException(String.format("provided parent link (id:%d) is not of type Link when creating a new LinkSegment", parentLink.getId()));
    }
    return new MacroscopicLinkSegmentImpl(groupId, (Link) parentLink, directionAB);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mode createMode(long externalModeId, String name, double pcu) {
    return new ModeImpl(groupId, externalModeId, name, pcu);
  }

  /**
   * {@inheritDoc}
   */
  public MacroscopicLinkSegmentType createLinkSegmentType(String name, double capacity, double maximumDensity, Object externalId,
      Map<Mode, MacroscopicModeProperties> modeProperties) {
    return new MacroscopicLinkSegmentTypeImpl(groupId, name, capacity, maximumDensity, externalId, modeProperties);
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

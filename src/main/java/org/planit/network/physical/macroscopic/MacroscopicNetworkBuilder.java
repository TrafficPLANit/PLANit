package org.planit.network.physical.macroscopic;

import java.util.Map;

import org.planit.exceptions.PlanItException;
import org.planit.network.physical.LinkImpl;
import org.planit.network.physical.ModeImpl;
import org.planit.network.physical.NodeImpl;
import org.planit.network.physical.PhysicalNetworkBuilder;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Mode;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentType;
import org.planit.utils.network.physical.macroscopic.MacroscopicModeProperties;

/**
 * Create network entities for a macroscopic simulation model
 * 
 * @author markr
 *
 */
public class MacroscopicNetworkBuilder implements PhysicalNetworkBuilder {

  /**
   * Contiguous id generation within this group id token for all instances created with factory methods in this class
   */
  protected IdGroupingToken groupId;

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
  @Override
  public Node createNode() {
    return new NodeImpl(groupId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Link createLink(Node nodeA, Node nodeB, double length, String name) throws PlanItException {
    return new LinkImpl(groupId, nodeA, nodeB, length, name);
  }

  /**
   * Create a new MacroscopicLinkSegment
   * 
   * @param parentLink  the parent link of this link segment
   * @param directionAB the direction of this link
   * @return LinkSegment created
   */
  @Override
  public LinkSegment createLinkSegment(Link parentLink, boolean directionAB) {
    return new MacroscopicLinkSegmentImpl(groupId, parentLink, directionAB);
  }

  /**
   * Create a fully functional macroscopic link segment type instance
   * 
   * @param name           the name of this link type
   * @param capacity       the capacity of this link type
   * @param maximumDensity the maximum density of this link type
   * @param externalId     the external reference number of this link type
   * @param modeProperties the mode properties for each mode along this link
   * @return macroscopicLinkSegmentType the created link segment type
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

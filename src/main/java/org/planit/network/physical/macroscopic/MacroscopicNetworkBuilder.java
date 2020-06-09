package org.planit.network.physical.macroscopic;

import java.util.Map;



import org.planit.exceptions.PlanItException;
import org.planit.network.physical.LinkImpl;
import org.planit.network.physical.NodeImpl;
import org.planit.network.physical.PhysicalNetworkBuilder;
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
   * Create a new node
   * 
   * @see org.planit.network.physical.PhysicalNetworkBuilder#createNode()
   * @return Node object created
   */
  @Override
  public Node createNode() {
    return new NodeImpl();
  }

  /**
   * Create a new link, injecting link length directly
   * 
   * @param nodeA first node in the link
   * @param nodeB second node in the link
   * @param length length of the link
   * @param name the name of the link
   * @return Link object created
   * @throws PlanItException
   *           thrown if there is an error
   */
  @Override
  public Link createLink(Node nodeA, Node nodeB, double length, String name) throws PlanItException {
    return new LinkImpl(nodeA, nodeB, length, name);
  }

  /**
   * Create a new MacroscopicLinkSegment
   * 
   * @param parentLink
   *          the parent link of this link segment
   * @param directionAB
   *          the direction of this link
   * @return LinkSegment created
   */
  @Override
  public LinkSegment createLinkSegment(Link parentLink, boolean directionAB) {
    return new MacroscopicLinkSegmentImpl(parentLink, directionAB);
  }

  /**
   * Create a fully functional macroscopic link segment type instance
   * 
   * @param name the name of this link type
   * @param capacity the capacity of this link type
   * @param maximumDensity the maximum density of this link type
   * @param externalId the external reference number of this link type
   * @param modeProperties the mode properties for each mode along this link
   * @return macroscopicLinkSegmentType the created link segment type
   */
  public MacroscopicLinkSegmentType createLinkSegmentType( String name, double capacity, double maximumDensity,
      Object externalId, Map<Mode, MacroscopicModeProperties> modeProperties) {
    return new MacroscopicLinkSegmentTypeImpl(name, capacity, maximumDensity, externalId, modeProperties);
  }
}

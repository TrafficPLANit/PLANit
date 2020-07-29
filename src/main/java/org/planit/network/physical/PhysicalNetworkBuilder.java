package org.planit.network.physical;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Mode;
import org.planit.utils.network.physical.Node;

/**
 * Build network elements based on chosen network view. Implementations are registered on the network class which uses it to construct network elements
 * 
 * @author markr
 *
 */
public interface PhysicalNetworkBuilder {

  /**
   * Create a new node instance
   * 
   * @return created node
   */
  public Node createNode();

  /**
   * Create a new mode
   * 
   * @param pcu            pcu value of the mode
   * @param name           name of the mode
   * @param externalModeId external id of the mode
   * @return created mode
   */
  public Mode createMode(long externalModeId, String name, double pcu);

  /**
   * Create a new link instance
   * 
   * @param nodeA  the first node in this link
   * @param nodeB  the second node in this link
   * @param length the length of this link
   * @param name   the name of the link
   * @return created link
   * @throws PlanItException thrown if there is an error
   */
  public Link createLink(Node nodeA, Node nodeB, double length, String name) throws PlanItException;

  /**
   * Create a new physical link segment instance
   * 
   * @param parentLink  the parent link of the link segment
   * @param directionAB direction of travel
   * @return linkSegment the created link segment
   */
  public LinkSegment createLinkSegment(Link parentLink, boolean directionAB);

  /**
   * Each builder needs a group if token to allow all underlying factory methods to generated ids uniquely tied to the group the entities belong to
   * 
   * @param groupId, contiguous id generation within this group for instances created with the factory methods
   */
  public void setIdGroupingToken(IdGroupingToken groupId);

  /**
   * Collect the id grouping token used by this builder
   * 
   * @return idGroupingToken the id grouping token used by this builder
   */
  public IdGroupingToken getIdGroupingToken();

}

package org.planit.network.physical;

import org.planit.exceptions.PlanItException;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Mode;
import org.planit.utils.network.physical.Node;

/**
 * Build network elements based on chosen network view. Implementations are
 * registered on the network class which uses it to construct network elements
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
    Node createNode();
    
    /**
     * Create a new mode
     * @param pcu pcu value of the mode
     * @param name name of the mode
     * @param externalModeId external id of the mode
     * @return created mode
     */
	default Mode createMode(long externalModeId, String name, double pcu) {
		return new ModeImpl(externalModeId, name, pcu);
	}

    /**
     * Create a new link instance
     * 
     * @param nodeA the first node in this link
     * @param nodeB the second node in this link
     * @param length the length of this link
     * @param name the name of the link
     * @return created link
     * @throws PlanItException thrown if there is an error
     */
    Link createLink(Node nodeA, Node nodeB, double length, String name) throws PlanItException;

    /**
     * Create a new physical link segment instance
     * 
     * @param parentLink the parent link of the link segment
     * @param directionAB direction of travel
     * @return linkSegment the created link segment
     */
    LinkSegment createLinkSegment(Link parentLink, boolean directionAB);



}

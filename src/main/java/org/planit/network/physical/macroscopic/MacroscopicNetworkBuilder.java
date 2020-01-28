package org.planit.network.physical.macroscopic;

import javax.annotation.Nonnull;

import org.planit.exceptions.PlanItException;
import org.planit.network.physical.LinkImpl;
import org.planit.network.physical.NodeImpl;
import org.planit.network.physical.PhysicalNetworkBuilder;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentType;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentTypeModeProperties;

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
     * @param nodeA
     *            first node in the link
     * @param nodeB
     *            second node in the link
     * @param length
     *            length of the link
     * @return Link object created
     * @throws PlanItException
     *             thrown if there is an error
     */
    @Override
    public Link createLink(Node nodeA, Node nodeB, double length) throws PlanItException {
        return new LinkImpl(nodeA, nodeB, length);
    }

    /**
     * Create a new MacroscopicLinkSegment
     * 
     * @param parentLink
     *            the parent link of this link segment
     * @param directionAB
     *            the direction of this link
     * @return LinkSegment created
     */
    @Override
    public LinkSegment createLinkSegment(Link parentLink, boolean directionAB) {
        return new MacroscopicLinkSegmentImpl(parentLink, directionAB);
    }

/**
 * Create a fully functional macroscopic link segment type instance
 * 
 * @param name                                          the name of this link type
 * @param capacity                                      the capacity of this link type
 * @param maximumDensity                       the maximum density of this link type
 * @param linkType                                      the external reference number of this link type
 * @param modeProperties                          the mode properties of this link
 * @return macroscopicLinkSegmentType   the created link segment type
 */
    public MacroscopicLinkSegmentType createLinkSegmentType(@Nonnull String name, double capacity,  double maximumDensity, long linkType, MacroscopicLinkSegmentTypeModeProperties modeProperties) {
        return new MacroscopicLinkSegmentTypeImpl(name, capacity, maximumDensity, linkType, modeProperties);
    }

}

package org.planit.network.physical.macroscopic;

import javax.annotation.Nonnull;

import org.planit.network.physical.Node;
import org.planit.exceptions.PlanItException;
import org.planit.network.physical.Link;
import org.planit.network.physical.LinkSegment;
import org.planit.network.physical.PhysicalNetworkBuilder;

/**
 * Create network entities for a macroscopic simulation model
 * @author markr
 *
 */
public class MacroscopicNetworkBuilder implements PhysicalNetworkBuilder {
	
	/** Create a new node
	 * @see org.planit.network.physical.PhysicalNetworkBuilder#createNode()
	 * @return node
	 */
	@Override
	public Node createNode() {
		return new Node();
	}

	/** create a new link, injecting link length directly
	 * @see org.planit.network.physical.PhysicalNetworkBuilder#createLink()
	 * @return link
	 * @throws PlanItException 
	 */
	@Override
	public Link createLink(Node nodeA, Node nodeB, double length) throws PlanItException {
		return new Link(nodeA, nodeB, length);
	}

	/** create a new MacroscopicLinkSegment
	 * @see org.planit.network.physical.PhysicalNetworkBuilder#createLinkSegment()
	 * @return macroscopicLinkSegment
	 */
	@Override
	public LinkSegment createLinkSegment(Link parentLink, boolean directionAB) {
		return new MacroscopicLinkSegment(parentLink, directionAB);
	}
	
	/** Create a fully functional macroscopic link segment type instance
	 * @param name
	 * @param capacity
	 * @param maximumDensity
	 * @param modeProperties
	 * @return macroscopicLinkSegmentType
	 */
	public MacroscopicLinkSegmentType createLinkSegmentType(@Nonnull String name, double capacity, double maximumDensity, MacroscopicLinkSegmentTypeModeProperties modeProperties) {
		return new MacroscopicLinkSegmentType(name, capacity, maximumDensity, modeProperties);
	}
	
}

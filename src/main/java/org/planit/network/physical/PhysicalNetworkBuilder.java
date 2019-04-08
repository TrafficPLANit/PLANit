package org.planit.network.physical;

import org.planit.exceptions.PlanItException;
import org.planit.geo.utils.PlanitGeoUtils;

/**
 * Build network elements based on chosen network view. Implementations are registered on the network class which uses it
 * to construct network elements 
 * @author markr
 *
 */
public interface PhysicalNetworkBuilder {
	
	/** Create a new node instance
	 * @return node
	 */
	Node createNode();

	/** Create a new link instance
	 * @return link
	 */
	Link createLink(Node nodeA, Node nodeB, PlanitGeoUtils planitGeoUtils) throws PlanItException;
	
	Link createLink(Node nodeA, Node nodeB, double length) throws PlanItException ;

	/** Create a new physical link segment instance
	 * @param parentLink
	 * @param directionAB, when true the segment takes on direction from A to B, otherwise from B to A
	 * @return linkSegment
	 */
	LinkSegment createLinkSegment(Link parentLink, boolean directionAB);

}

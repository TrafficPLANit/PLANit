package org.planit.network.physical;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nonnull;

import org.opengis.referencing.operation.TransformException;
import org.planit.exceptions.PlanItException;
import org.planit.geo.utils.PlanitGeoUtils;
import org.planit.network.physical.LinkSegment;
import org.planit.trafficassignment.TrafficAssignmentComponent;
import org.planit.utils.IdGenerator;

/**
 * Model free Network consisting of nodes and links, each of which can be iterated over. This network does not
 * contain any transport specific information, hence the qualification "model free". 
 * 
 * @author markr
 */
public class PhysicalNetwork extends TrafficAssignmentComponent<PhysicalNetwork> {
	
	// INNER CLASSES
	
	/**
	 * Internal class for all Link specific code
	 *
	 */
	public class Links implements Iterable<Link> {
		
		/** Add link to the internal container
		 * @return link, in case it overrides an existing link, the removed link is returned
		 */
		protected Link registerLink(@Nonnull Link link) {
			return linkMap.put(link.getId(),link);
		}
		
		@Override
		public Iterator<Link> iterator() {
			return linkMap.values().iterator();
		}		
		
		/** create new link to network identified via its id, using GeoTools to generate link length
		 * @return link, new link
		 * @throws PlanItException 
		 * @throws TransformException 
		 */
		public Link registerNewLink(Node nodeA, Node nodeB, PlanitGeoUtils planitGeoUtils) throws TransformException, PlanItException {
			Link newLink = networkBuilder.createLink(nodeA, nodeB, planitGeoUtils);
			registerLink(newLink);
			return newLink;
		}			
		
		/** create new link to network identified via its id, injecting link length directly
		 * @return link, new link
		 * @throws PlanItException 
		 * @throws TransformException 
		 */
		public Link registerNewLink(Node nodeA, Node nodeB, double length) throws TransformException, PlanItException {
			Link newLink = networkBuilder.createLink(nodeA, nodeB, length);
			registerLink(newLink);
			return newLink;
		}			
		
		/**
		 * Collect link by id 
		 * @param id
		 * @return link
		 */
		public Link getLink(long id) {
			return linkMap.get(id);
		}		
		
		/** Collect number of links on the network
		 * @return
		 */
		public int getNumberOfLinks() {
			return linkMap.size();			
		}		
	}
	
	/**
	 * Internal class for LinkSegment specific code (non-physical link segments are placed in the zoning)
	 *
	 */
	public class LinkSegments implements Iterable<LinkSegment> {
		
		/** Register a link segment on the network
		 * @param linkSegment
		 */
		protected LinkSegment registerLinkSegment(@Nonnull LinkSegment linkSegment) {	
			return linkSegmentMap.put(linkSegment.getId(), linkSegment);
		}
		
		@Override
		public Iterator<LinkSegment> iterator() {
			return linkSegmentMap.values().iterator();
		}		
		
		public LinkSegment createDirectionalLinkSegment(@Nonnull Link parentLink, boolean directionAB) throws PlanItException {
			LinkSegment linkSegment = networkBuilder.createLinkSegment(parentLink, directionAB);
			return linkSegment;
		}			
		
		public void registerLinkSegment(@Nonnull Link parentLink, LinkSegment linkSegment, boolean directionAB) throws PlanItException {
			parentLink.registerLinkSegment(linkSegment, directionAB);
			registerLinkSegment(linkSegment);
		}
	
		/**
		 * Collect link segment by id 
		 * @param id
		 * @return linkSegment
		 */
		public LinkSegment getLinkSegment(long id) {
			return linkSegmentMap.get(id);
		}			
		
		public int getNumberOfLinkSegments() {
			return linkSegmentMap.size();			
		}		
	}	
	
		
	/**
	 * Internal class for all Node specific code
	 *
	 */
	public class Nodes implements Iterable<Node> {
		
		/** Add node to the internal container
		 * @return node, in case it overrides an existing node, the removed node is returned
		 */	
		public Node registerNode(@Nonnull Node node) {
			return nodeMap.put(node.getId(),node);
		}		
		
		@Override
		public Iterator<Node> iterator() {
			return nodeMap.values().iterator();
		}
		
		/** Add node to network identified via its id
		 * @param node to add
		 * @return node, new node 
		 */
		public Node registerNewNode(){
			Node newNode = networkBuilder.createNode();
			registerNode(newNode);
			return newNode;
		}		
		
		public int getNumberOfNodes() {
			return nodeMap.size();
		}
		
		public Node findNodeByGeometryId(long externalLinkId) {
			for (Node node: nodeMap.values()) {
				if  ((node.getExternalLinkIdSet().contains(externalLinkId)) && (node.getExternalId() != 0)) {
					return node;
				}
			}
			return null;
		}
		
		public Node findNodeByExternalIdentifier(long externalId) {
			for (Node node: nodeMap.values()) {
				if  (node.getExternalId() ==  externalId) {
					return node;
				}
			}
			return null;
		}
		
	}	
	
	// Private
	
	// Protected
	
	/**
	 * Unique id of the network
	 */
	protected final long id;
	

	/**
	 * Network builder responsible for constructing all network related (derived) instances
	 */
	protected final PhysicalNetworkBuilder networkBuilder;
	
	// for now use tree map to ensure non-duplicate keys until we add functionality to account for this (treemap is slower than hashmap)
	protected Map<Long, Link> linkMap = new TreeMap<Long, Link>();
	protected Map<Long, LinkSegment> linkSegmentMap = new TreeMap<Long, LinkSegment>();
	protected Map<Long, Node> nodeMap = new TreeMap<Long, Node>();
								
	// PUBLIC
	
	/**
	 * internal class instance containing all link specific functionality
	 */
	public final Links links = new Links();
	/**
	 * internal class instance containing all  link segment specific functionality
	 */	
	public final LinkSegments linkSegments = new LinkSegments();
	/**
	 * internal class instance containing all nodes specific functionality
	 */		
	public final Nodes nodes = new Nodes();	
	
	/**
	 * Network Constructor
	 * @param theNetworkBuilder 
	 */
	public PhysicalNetwork(@Nonnull PhysicalNetworkBuilder networkBuilder)
	{		
		this.id = IdGenerator.generateId(PhysicalNetwork.class);
		this.networkBuilder = networkBuilder;		
	}
	
	
	// Getters - Setters
	
	/** Collect network id
	 * @return id
	 */
	public long getId() {
		return this.id;
	}


}

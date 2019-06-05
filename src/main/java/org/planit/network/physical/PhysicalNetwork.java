package org.planit.network.physical;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.planit.exceptions.PlanItException;
import org.planit.network.physical.LinkSegment;
import org.planit.trafficassignment.TrafficAssignmentComponent;
import org.planit.utils.IdGenerator;

/**
 * Model free Network consisting of nodes and links, each of which can be
 * iterated over. This network does not contain any transport specific
 * information, hence the qualification "model free".
 * 
 * @author markr
 */
public class PhysicalNetwork extends TrafficAssignmentComponent<PhysicalNetwork> {

	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = Logger.getLogger(PhysicalNetwork.class.getName());

	// INNER CLASSES

	/**
	 * Internal class for all Link specific code
	 *
	 */
	public class Links implements Iterable<Link> {

		/**
		 * Add link to the internal container
		 * 
		 * @param link link to be registered in this network
		 * @return link, in case it overrides an existing link, the removed link is
		 *         returned
		 */
		protected Link registerLink(@Nonnull Link link) {
			return linkMap.put(link.getId(), link);
		}

		/**
		 * Iterator over registered links
		 * 
		 * @return iterator over registered links
		 */
		@Override
		public Iterator<Link> iterator() {
			return linkMap.values().iterator();
		}

		/**
		 * Create new link to network identified via its id, injecting link length
		 * directly
		 * 
		 * @param nodeA  the first node in this link
		 * @param nodeB  the second node in this link
		 * @param length the length of this link
		 * @return the created link
		 * @throws PlanItException thrown if there is an error
		 */
		public Link registerNewLink(Node nodeA, Node nodeB, double length) throws PlanItException {
			Link newLink = networkBuilder.createLink(nodeA, nodeB, length);
			registerLink(newLink);
			return newLink;
		}

		/**
		 * Get link by id
		 * 
		 * @param id the id of the link
		 * @return the retrieved link
		 */
		public Link getLink(long id) {
			return linkMap.get(id);
		}

		/**
		 * Get the number of links on the network
		 * 
		 * @return the number of links in the network
		 */
		public int getNumberOfLinks() {
			return linkMap.size();
		}
	}

	/**
	 * Internal class for LinkSegment specific code (non-physical link segments are
	 * placed in the zoning)
	 *
	 */
	public class LinkSegments implements Iterable<LinkSegment> {

		/**
		 * Register a link segment on the network
		 * 
		 * @param linkSegment the link segment to be registered
		 * @return the registered link segment
		 */
		protected LinkSegment registerLinkSegment(@Nonnull LinkSegment linkSegment) {
			return linkSegmentMap.put(linkSegment.getId(), linkSegment);
		}

		/**
		 * Iterator over registered link segments
		 * 
		 * @return Iterator over registered link segments
		 */
		@Override
		public Iterator<LinkSegment> iterator() {
			return linkSegmentMap.values().iterator();
		}

		/**
		 * Returns a List of LinkSegments in the network
		 * 
		 * @return List of registered link segments
		 * 
		 * @return
		 */
		public List<LinkSegment> toList() {
			return new ArrayList<LinkSegment>(linkSegmentMap.values());
		}

		/**
		 * Create directional link segment
		 * 
		 * @param parentLink  the parent link of this link segment
		 * @param directionAB direction of travel
		 * @return the created link segment
		 * @throws PlanItException thrown if there is an error
		 */
		public LinkSegment createDirectionalLinkSegment(@Nonnull Link parentLink, boolean directionAB)
				throws PlanItException {
			LinkSegment linkSegment = networkBuilder.createLinkSegment(parentLink, directionAB);
			return linkSegment;
		}

		/**
		 * Register a link segment
		 * 
		 * @param parentLink  the parent link which specified link segment will be
		 *                    registered on
		 * @param linkSegment link segment to be registered
		 * @param directionAB direction of travel
		 * @throws PlanItException thrown if there is an error
		 */
		public void registerLinkSegment(@Nonnull Link parentLink, LinkSegment linkSegment, boolean directionAB)
				throws PlanItException {
			parentLink.registerLinkSegment(linkSegment, directionAB);
			registerLinkSegment(linkSegment);
		}

		/**
		 * Get link segment by id
		 * 
		 * @param id id of the link segment
		 * @return retrieved linkSegment
		 */
		public LinkSegment getLinkSegment(long id) {
			return linkSegmentMap.get(id);
		}

		/**
		 * Return number of registered link segments
		 * 
		 * @return number of registered link segments
		 */
		public int getNumberOfLinkSegments() {
			return linkSegmentMap.size();
		}
	}

	/**
	 * Internal class for all Node specific code
	 */
	public class Nodes implements Iterable<Node> {

		/**
		 * Add node to the internal container
		 * 
		 * @param node node to be registered in this network
		 * @return node, in case it overrides an existing node, the removed node is
		 *         returned
		 */
		public Node registerNode(@Nonnull Node node) {
			return nodeMap.put(node.getId(), node);
		}

		/**
		 * Iterator through registered nodes
		 * 
		 * @return iterator through registered nodes
		 */
		@Override
		public Iterator<Node> iterator() {
			return nodeMap.values().iterator();
		}

		/**
		 * Create and register new node
		 * 
		 * @return new node created
		 */
		public Node registerNewNode() {
			Node newNode = networkBuilder.createNode();
			registerNode(newNode);
			return newNode;
		}

		/**
		 * Return number of registered nodes
		 * 
		 * @return number of registered nodes
		 */
		public int getNumberOfNodes() {
			return nodeMap.size();
		}

		/**
		 * Find a node by its external Id
		 * 
		 * @param externalId external Id of node
		 * @return retrieved node
		 */
		public Node findNodeByExternalIdentifier(long externalId) {
			for (Node node : nodeMap.values()) {
				if (node.getExternalId() == externalId) {
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
	 * Network builder responsible for constructing all network related (derived)
	 * instances
	 */
	protected final PhysicalNetworkBuilder networkBuilder;

	// for now use tree map to ensure non-duplicate keys until we add functionality
	// to account for this (treemap is slower than hashmap)
	protected Map<Long, Link> linkMap = new TreeMap<Long, Link>();
	protected Map<Long, LinkSegment> linkSegmentMap = new TreeMap<Long, LinkSegment>();
	protected Map<Long, Node> nodeMap = new TreeMap<Long, Node>();

	// PUBLIC

	/**
	 * internal class instance containing all link specific functionality
	 */
	public final Links links = new Links();
	/**
	 * internal class instance containing all link segment specific functionality
	 */
	public final LinkSegments linkSegments = new LinkSegments();
	/**
	 * internal class instance containing all nodes specific functionality
	 */
	public final Nodes nodes = new Nodes();

	/**
	 * Network Constructor
	 * 
	 * @param networkBuilder the builder to be used to create this network
	 */
	public PhysicalNetwork(@Nonnull PhysicalNetworkBuilder networkBuilder) {
		this.id = IdGenerator.generateId(PhysicalNetwork.class);
		this.networkBuilder = networkBuilder;
	}

	// Getters - Setters

	/**
	 * Collect network id
	 * 
	 * @return id
	 */
	public long getId() {
		return this.id;
	}

}

package org.planit.network.physical;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nonnull;

import org.planit.exceptions.PlanItException;
import org.planit.logging.PlanItLogger;
import org.planit.trafficassignment.TrafficAssignmentComponent;
import org.planit.utils.misc.IdGenerator;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Mode;
import org.planit.utils.network.physical.Node;

/**
 * Model free Network consisting of nodes and links, each of which can be
 * iterated over. This network does not contain any transport specific
 * information, hence the qualification "model free".
 *
 * @author markr
 */
public class PhysicalNetwork extends TrafficAssignmentComponent<PhysicalNetwork> implements Serializable {

	// INNER CLASSES

	/** generated UID */
	private static final long serialVersionUID = -2794450367185361960L;

	/**
	 * Internal class for all Link specific code
	 *
	 */
	public class Links {

		/**
		 * Add link to the internal container
		 *
		 * @param link link to be registered in this network
		 * @return link, in case it overrides an existing link, the removed link is
		 *         returned
		 */
		protected Link registerLink(@Nonnull final Link link) {
			return linkMap.put(link.getId(), link);
		}

		/**
		 * Returns a List of Links
		 *
		 * @return List of Links
		 */
		public List<Link> toList() {
			return new ArrayList<Link>(linkMap.values());
		}

		/**
		 * Create new link to network identified via its id, injecting link length
		 * directly
		 *
		 * @param nodeA  the first node in this link
		 * @param nodeB  the second node in this link
		 * @param length the length of this link
		 * @param name the name of the link
		 * @return the created link
		 * @throws PlanItException thrown if there is an error
		 */
		public Link registerNewLink(final Node nodeA, final Node nodeB, final double length, final String name) throws PlanItException {
			final Link newLink = networkBuilder.createLink(nodeA, nodeB, length, name);
			registerLink(newLink);
			return newLink;
		}

		/**
		 * Get link by id
		 *
		 * @param id the id of the link
		 * @return the retrieved link
		 */
		public Link getLink(final long id) {
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
	public class LinkSegments {

		/**
		 * Register a link segment on the network
		 *
		 * @param linkSegment the link segment to be registered
		 * @throws PlanItException thrown if the current link segment external Id has already been assigned
		 */
		protected void registerLinkSegment(@Nonnull final LinkSegment linkSegment) throws PlanItException {
			if (linkSegmentMapByExternalId.containsKey(linkSegment.getExternalId())) {
				throw new PlanItException("Link Segment External Id " + linkSegment.getExternalId()
						+ " has been assigned to more than one link segment in the input file.");
			}
			// do not store by external Id if external Id is zero, that value means external
			// Ids are not being used for this input method
			final long externalId = linkSegment.getExternalId();
			if (externalId > 0) {
				linkSegmentMapByExternalId.put(externalId, linkSegment);
			}
			linkSegmentMap.put(linkSegment.getId(), linkSegment);
			final Node startNode = (Node) linkSegment.getUpstreamVertex();
			final long startNodeExternalId = startNode.getExternalId();
			if (!linkSegmentMapByStartExternalId.containsKey(startNodeExternalId)) {
				linkSegmentMapByStartExternalId.put(startNodeExternalId, new ArrayList<LinkSegment>());
			}
			linkSegmentMapByStartExternalId.get(startNodeExternalId).add(linkSegment);
		}

		/**
		 * Returns a List of LinkSegments in the network
		 *
		 * @return List of registered link segments
		 */
		public List<LinkSegment> toList() {
			return new ArrayList<LinkSegment>(linkSegmentMap.values());
		}

		/**
		 * Find a LinkSegment by the external Ids of its start and end nodes
		 *
		 * @param startExternalId reference to start node
		 * @param endExternalId   reference to end node
		 * @return the linkSegment found
		 */
		public LinkSegment getLinkSegmentByStartAndEndNodeExternalId(final long startExternalId, final long endExternalId) {
			if (!linkSegmentMapByStartExternalId.containsKey(startExternalId)) {
				PlanItLogger.severe("No link segment with start node " + startExternalId + " has been registered in the network.");
				return null;
			}
			final List<LinkSegment> linkSegmentsForCurrentStartNode = linkSegmentMapByStartExternalId.get(startExternalId);
			for (final LinkSegment linkSegment : linkSegmentsForCurrentStartNode) {
				final Node endNode = (Node) linkSegment.getDownstreamVertex();
				if (endNode.getExternalId() == endExternalId) {
					return linkSegment;
				}
			}
			PlanItLogger.severe("No link segment with start node " + startExternalId + " and end node " + endExternalId + " has been registered in the network.");
			return null;
		}

		/**
		 * Create directional link segment
		 *
		 * @param parentLink  the parent link of this link segment
		 * @param directionAB direction of travel
		 * @return the created link segment
		 * @throws PlanItException thrown if there is an error
		 */
		public LinkSegment createDirectionalLinkSegment(@Nonnull final Link parentLink, final boolean directionAB)
				throws PlanItException {
			final LinkSegment linkSegment = networkBuilder.createLinkSegment(parentLink, directionAB);
			return linkSegment;
		}

		/**
		 * Register a link segment
		 *
		 * @param parentLink  the parent link which specified link segment will be registered on
		 * @param linkSegment link segment to be registered
		 * @param directionAB direction of travel
		 * @throws PlanItException thrown if there is an error
		 */
		public void registerLinkSegment(@Nonnull final Link parentLink, final LinkSegment linkSegment, final boolean directionAB)
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
		public LinkSegment getLinkSegment(final long id) {
			return linkSegmentMap.get(id);
		}

		/**
		 * Get link segment by External Id
		 *
		 * @param externalId external Id of the link segment
		 * @return retrieved link segment
		 */
		public LinkSegment getLinkSegmentByExternalId(final long externalId) {
			if (!linkSegmentMapByExternalId.containsKey(externalId) ) {
				PlanItLogger.severe("Link with External Id " + externalId + " has not been registered in the network.  Are the network supply and initial cost definitions consistent for link " + (externalId / 10) + "?");
				return null;
			}
			return linkSegmentMapByExternalId.get(externalId);
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
	public class Nodes {

		/**
		 * Add node to the internal container
		 *
		 * @param node node to be registered in this network
		 * @return node, in case it overrides an existing node, the removed node is
		 *         returned
		 */
		public Node registerNode(@Nonnull final Node node) {
			return nodeMap.put(node.getId(), node);
		}

		/**
		 * Returns a List of Nodes
		 *
		 * @return List of Nodes
		 */
		public List<Node> toList() {
			return new ArrayList<Node>(nodeMap.values());
		}

		/**
		 * Create and register new node
		 *
		 * @return new node created
		 */
		public Node registerNewNode() {
			final Node newNode = networkBuilder.createNode();
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
		public Node findNodeByExternalIdentifier(final long externalId) {
			for (final Node node : nodeMap.values()) {
				if (node.getExternalId() == externalId) {
					return node;
				}
			}
			return null;
		}

	}

	/**
	 * Internal class for all Mode specific code
	 */
	public class Modes {
		/**
		 * Add mode to the internal container
		 *
		 * @param mode to be registered in this network
		 * @return mode, in case it overrides an existing mode, the removed mode is
		 *         returned
		 */
		public Mode registerMode(@Nonnull final Mode mode) {
			return modeMap.put(mode.getId(), mode);
		}

		/**
		 * Returns a List of Modes
		 *
		 * @return List of Modes
		 */
		public List<Mode> toList() {
			return new ArrayList<Mode>(modeMap.values());
		}

		/**
		 * Create and register new mode
		 *
		 * @param externalModeId
		 * @param name
		 * @param pcu

		 *
		 * @return new mode created
		 */
		public Mode registerNewMode(final long externalModeId, final String name, final double pcu) {
			final Mode newMode = networkBuilder.createMode(externalModeId, name, pcu);
			registerMode(newMode);
			return newMode;
		}

		/**
		 * Return number of registered modes
		 *
		 * @return number of registered modes
		 */
		public int getNumberOfModes() {
			return modeMap.size();
		}

		/**
		 * Find a mode by its external Id (not fool proof as multiple modes can have this id)
		 * Also costly as external id is not indexed
		 *
		 * @param externalId external Id of mode
		 * @return retrieved node
		 */
		public Mode findModeByExternalIdentifier(final long externalId) {
			for (final Mode mode : modeMap.values()) {
				if (mode.getExternalId() == externalId) {
					return mode;
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

	/**
	 * Map to store Links by their Id
	 */
	protected Map<Long, Link> linkMap;

	/**
	 * Map to store link segments by their Id
	 */
	protected Map<Long, LinkSegment> linkSegmentMap;

	/**
	 * Map to store link segments by their external Id
	 */
	protected Map<Long, LinkSegment> linkSegmentMapByExternalId;

	/**
	 * Map to store all link segments for a given start node external Id
	 */
	protected Map<Long, List<LinkSegment>> linkSegmentMapByStartExternalId;

	/**
	 * Map to store nodes by their Id
	 */
	protected Map<Long, Node> nodeMap;

	/**
	 * Map to store modes by their Id
	 */
	protected Map<Long, Mode> modeMap;

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
	 * internal class instance containing all modes specific functionality
	 */
	public final Modes modes = new Modes();

	/**
	 * Network Constructor
	 *
	 * @param networkBuilder the builder to be used to create this network
	 */
	public PhysicalNetwork(@Nonnull final PhysicalNetworkBuilder networkBuilder) {
		this.id = IdGenerator.generateId(PhysicalNetwork.class);
		// for now use tree map to ensure non-duplicate keys until we add functionality
		// to account for this (treemap is slower than hashmap)
		linkMap = new TreeMap<Long, Link>();
		linkSegmentMap = new TreeMap<Long, LinkSegment>();
		linkSegmentMapByExternalId = new HashMap<Long, LinkSegment>();
		linkSegmentMapByStartExternalId = new HashMap<Long, List<LinkSegment>>();
		nodeMap = new TreeMap<Long, Node>();
		modeMap = new TreeMap<Long,Mode>();
		this.networkBuilder = networkBuilder;
	}

	// Getters - Setters

	/**
	 * #{@inheritDoc}
	 */
	@Override
	public long getId() {
		return this.id;
	}

}

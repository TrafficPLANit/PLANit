package org.planit.network.physical;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.planit.exceptions.PlanItException;
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

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PhysicalNetwork.class.getCanonicalName());

  /**
   * Internal class for all Link specific code
   *
   */
  public class Links {

    /**
     * Map to store Links by their Id
     */
    private Map<Long, Link> linkMap;

    public Links() {
      linkMap = new TreeMap<Long, Link>();
    }

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
     * @param nodeA the first node in this link
     * @param nodeB the second node in this link
     * @param length the length of this link
     * @param name the name of the link
     * @return the created link
     * @throws PlanItException thrown if there is an error
     */
    public Link registerNewLink(final Node nodeA, final Node nodeB, final double length, final String name)
        throws PlanItException {
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
     * Map to store link segments by their Id
     */
    private Map<Long, LinkSegment> linkSegmentMap;

    /**
     * Map to store all link segments for a given start node Id
     */
    private Map<Long, List<LinkSegment>> linkSegmentMapByStartNodeId;

    public LinkSegments() {
      linkSegmentMap = new TreeMap<Long, LinkSegment>();
      linkSegmentMapByStartNodeId = new HashMap<Long, List<LinkSegment>>();
    }

    /**
     * Register a link segment on the network
     *
     * @param linkSegment the link segment to be registered
     * @throws PlanItException thrown if the current link segment external Id has already been
     *           assigned
     */
    protected void registerLinkSegment(@Nonnull final LinkSegment linkSegment) throws PlanItException {
      linkSegmentMap.put(linkSegment.getId(), linkSegment);
      final Node startNode = (Node) linkSegment.getUpstreamVertex();
      if (!linkSegmentMapByStartNodeId.containsKey(startNode.getId())) {
        linkSegmentMapByStartNodeId.put(startNode.getId(), new ArrayList<LinkSegment>());
      }
      linkSegmentMapByStartNodeId.get(startNode.getId()).add(linkSegment);
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
     * @param endExternalId reference to end node
     * @return the linkSegment found
     */
    public LinkSegment getLinkSegmentByStartAndEndNodeId(final long startId, final long endId) {
      if (!linkSegmentMapByStartNodeId.containsKey(startId)) {
        LOGGER.severe("No link segment with start node " + startId + " has been registered in the network.");
        return null;
      }
      final List<LinkSegment> linkSegmentsForCurrentStartNode = linkSegmentMapByStartNodeId.get(startId);
      for (final LinkSegment linkSegment : linkSegmentsForCurrentStartNode) {
        final Node endNode = (Node) linkSegment.getDownstreamVertex();
        if (endNode.getId() == endId) {
          return linkSegment;
        }
      }
      LOGGER.severe("No link segment with start node " + startId + " and end node " + endId
          + " has been registered in the network.");
      return null;
    }

    /**
     * Create directional link segment
     *
     * @param parentLink the parent link of this link segment
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
     * @param parentLink the parent link which specified link segment will be registered on
     * @param linkSegment link segment to be registered
     * @param directionAB direction of travel
     * @throws PlanItException thrown if there is an error
     */
    public void registerLinkSegment(@Nonnull final Link parentLink, final LinkSegment linkSegment,
        final boolean directionAB)
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
     * Map to store nodes by their Id
     */
    private Map<Long, Node> nodeMap;

    public Nodes() {
      nodeMap = new TreeMap<Long, Node>();
    }

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
     * Find a node by its d
     *
     * @param id Id of node
     * @return retrieved node
     */
    public Node getNodeById(final long id) {
      return nodeMap.get(id);
    }

  }

  /**
   * Internal class for all Mode specific code
   */
  public class Modes {

    /**
     * Map to store modes by their Id
     */
    private Map<Long, Mode> modeMap;

    public Modes() {
      modeMap = new TreeMap<Long, Mode>();
    }

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
     * Return a Mode by its id
     * 
     * @param id the id of the Mode
     * @return the specified mode
     * 
     */
    public Mode getModeById(long id) {
      return modeMap.get(id);
    }

    /**
     * Retrieve a Mode by its external Id
     * 
     * This method has the option to convert the external Id parameter into a long value,
     * to find the mode when mode objects use long values for external ids.
     * 
     * @param externalId the external Id of the specified mode
     * @param convertToLong if true, the external Id is converted into a long before beginning the
     *          search
     * @return the retrieved mode, or null if no mode was found
     * @throws PlanItException thrown if the external Id cannot be cast into a long
     */
    public Mode getModeByExternalId(Object externalId, boolean convertToLong) throws PlanItException {
      if (convertToLong) {
        try {
          long value = Long.valueOf(externalId.toString());
          return getModeByExternalId(value);
        } catch (NumberFormatException e) {
          //String errorMessage = "getModeByExternalId was passed a " + externalId.getClass().getCanonicalName()
          //    + " which cannot be cast into a long.";
          //LOGGER.severe(errorMessage);
          //throw new PlanItException(errorMessage);
         }
      }
      return getModeByExternalId(externalId);
    }

    /**
     * Retrieve a Mode by its external Id
     * 
     * This method is not efficient, since it loops through all the registered modes in order
     * to find the required time period. The equivalent method in InputBuilderListener is more
     * efficient and should be used in preference to this in Java code.
     * 
     * @param externalId the external Id of the specified mode
     * @return the retrieved mode, or null if no mode was found
     */
    public Mode getModeByExternalId(Object externalId) {
      for (Mode mode : modeMap.values()) {
        if (mode.getExternalId().equals(externalId)) {
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
    this.networkBuilder = networkBuilder;
  }

  /**
   * Return the Modes internal class instance.
   * Method available to allow Py4J to access modes since public final member approach is not
   * supported
   * 
   * @return the Modes local class
   */
  public Modes getModes() {
    return modes;
  }

  /**
   * Return the LinkSegments internal class instance.
   * Method available to allow Py4J to access modes since public final member approach is not
   * supported
   * 
   * @return the LinkSegments inner class instance
   */
  public LinkSegments getLinkSegments() {
    return linkSegments;
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

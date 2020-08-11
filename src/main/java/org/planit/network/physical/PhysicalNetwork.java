package org.planit.network.physical;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.planit.assignment.TrafficAssignmentComponent;
import org.planit.graph.Graph;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.Vertex;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.misc.LoggingUtils;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Mode;
import org.planit.utils.network.physical.Node;

/**
 * Model free Network consisting of nodes and links, each of which can be iterated over. This network does not contain any transport specific information, hence the qualification
 * "model free".
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
   * The graph containing the edges and vertices
   */
  private final Graph graph;

  /**
   * Internal class for all Link specific code
   *
   */
  public class Links implements Iterable<Edge> {

    /**
     * Iterator over available links
     */
    @Override
    public Iterator<Edge> iterator() {
      return graph.edges.iterator();
    }

    /**
     * Create new link to network identified via its id, injecting link length directly
     *
     * @param nodeA  the first node in this link
     * @param nodeB  the second node in this link
     * @param length the length of this link
     * @return the created link
     * @throws PlanItException thrown if there is an error
     */
    public Link registerNewLink(final Node nodeA, final Node nodeB, final double length) throws PlanItException {
      final Link newLink = (Link) graph.edges.registerNewEdge(nodeA, nodeB, length);
      return newLink;
    }

    /**
     * Get link by id
     *
     * @param id the id of the link
     * @return the retrieved link
     */
    public Link getLink(final long id) {
      return (Link) graph.edges.getEdge(id);
    }

    /**
     * Get the number of links on the network
     *
     * @return the number of links in the network
     */
    public int getNumberOfLinks() {
      return graph.edges.getNumberOfEdges();
    }
  }

  /**
   * Internal class for LinkSegment specific code (non-physical link segments are placed in the zoning)
   *
   */
  public class LinkSegments implements Iterable<EdgeSegment> {

    /**
     * Map to store all link segments for a given start node Id
     */
    private Map<Long, List<LinkSegment>> linkSegmentMapByStartNodeId;

    /**
     * Register a link segment on the network
     *
     * @param linkSegment the link segment to be registered
     * @throws PlanItException thrown if the current link segment external Id has already been assigned
     */
    protected void registerLinkSegment(final LinkSegment linkSegment) throws PlanItException {
      final Node startNode = (Node) linkSegment.getUpstreamVertex();
      if (!linkSegmentMapByStartNodeId.containsKey(startNode.getId())) {
        linkSegmentMapByStartNodeId.put(startNode.getId(), new ArrayList<LinkSegment>());
      }
      linkSegmentMapByStartNodeId.get(startNode.getId()).add(linkSegment);
    }

    /**
     * Constructor
     */
    public LinkSegments() {
      linkSegmentMapByStartNodeId = new HashMap<Long, List<LinkSegment>>();
    }

    /**
     * Iterator over available nodes
     */
    @Override
    public Iterator<EdgeSegment> iterator() {
      return graph.edgeSegments.iterator();
    }

    /**
     * Find a LinkSegment by the external Ids of its start and end nodes
     *
     * @param startId reference to start node
     * @param endId   reference to end node
     * @return the linkSegment found
     */
    public LinkSegment getLinkSegmentByStartAndEndNodeId(final long startId, final long endId) {
      if (!linkSegmentMapByStartNodeId.containsKey(startId)) {
        LOGGER.warning(LoggingUtils.createNetworkPrefix(getId()) + String.format("no link segment with start node %d has been registered in the network", startId));
        return null;
      }
      final List<LinkSegment> linkSegmentsForCurrentStartNode = linkSegmentMapByStartNodeId.get(startId);
      for (final LinkSegment linkSegment : linkSegmentsForCurrentStartNode) {
        final Node endNode = (Node) linkSegment.getDownstreamVertex();
        if (endNode.getId() == endId) {
          return linkSegment;
        }
      }
      LOGGER.warning(
          LoggingUtils.createNetworkPrefix(getId()) + String.format("no link segment with start node %d and end node %d has been registered in the network", startId, endId));
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
    public LinkSegment createLinkSegment(final Link parentLink, final boolean directionAB) throws PlanItException {
      return (LinkSegment) graph.edgeSegments.createEdgeSegment(parentLink, directionAB);
    }

    /**
     * Register a link segment
     *
     * @param parentLink  the parent link which specified link segment will be registered on
     * @param linkSegment link segment to be registered
     * @param directionAB direction of travel
     * @throws PlanItException thrown if there is an error
     */
    public void registerLinkSegment(final Link parentLink, final LinkSegment linkSegment, final boolean directionAB) throws PlanItException {
      graph.edgeSegments.registerEdgeSegment(parentLink, linkSegment, directionAB);
      registerLinkSegment(linkSegment);
    }

    /**
     * Get link segment by id
     *
     * @param id id of the link segment
     * @return retrieved linkSegment
     */
    public LinkSegment getLinkSegment(final long id) {
      return (LinkSegment) graph.edgeSegments.getEdgeSegment(id);
    }

    /**
     * Return number of registered link segments
     *
     * @return number of registered link segments
     */
    public int getNumberOfLinkSegments() {
      return graph.edgeSegments.getNumberOfEdgeSegments();
    }

    /**
     * Retrieve a link segment by its external Id
     * 
     * This method has the option to convert the external Id parameter into a long value, to find the link segment type when link segment type objects use long values for external
     * ids.
     * 
     * @param externalId    the external Id of the specified link segment
     * @param convertToLong if true, the external Id is converted into a long before beginning the search
     * @return the retrieved link segment, or null if no mode was found
     */
    public LinkSegment getLinkSegmentByExternalId(Object externalId, boolean convertToLong) {
      try {
        if (convertToLong) {
          long value = Long.valueOf(externalId.toString());
          return getLinkSegmentByExternalId(value);
        }
        return getLinkSegmentByExternalId(externalId);
      } catch (NumberFormatException e) {
        // do nothing - if conversion to long is not possible, use the general method instead
      }
      return getLinkSegmentByExternalId(externalId);
    }

    /**
     * Retrieve a link segment by its external Id
     * 
     * This method is not efficient, since it loops through all the registered modes in order to find the required link segment. The equivalent method in InputBuilderListener is
     * more efficient and should be used in preference to this in Java code.
     * 
     * @param externalId the external Id of the specified link segment type
     * @return the retrieved link segment, or null if no link segment type was found
     */
    public LinkSegment getLinkSegmentByExternalId(Object externalId) {
      for (EdgeSegment linkSegment : graph.edgeSegments) {
        if (linkSegment.getExternalId().equals(externalId)) {
          return (LinkSegment) linkSegment;
        }
      }
      return null;
    }

  }

  /**
   * Internal class for all Node specific code
   */
  public class Nodes implements Iterable<Vertex> {

    /**
     * Iterator over available nodes
     */
    @Override
    public Iterator<Vertex> iterator() {
      return graph.vertices.iterator();
    }

    /**
     * Create and register new node
     *
     * @return new node created
     */
    public Node registerNewNode() {
      final Node newNode = (Node) graph.vertices.registerNewNode();
      return newNode;
    }

    /**
     * Create and register new node
     *
     * @param externalId the externalId of the node
     * @return new node created
     */
    public Node registerNewNode(Object externalId) {
      final Node newNode = (Node) graph.vertices.registerNewVertex(externalId);
      return newNode;
    }

    /**
     * Return number of registered nodes
     *
     * @return number of registered nodes
     */
    public int getNumberOfNodes() {
      return graph.vertices.getNumberOfVertices();
    }

    /**
     * Find a node by its d
     *
     * @param id Id of node
     * @return retrieved node
     */
    public Node getNodeById(final long id) {
      return (Node) graph.vertices.getVertexById(id);
    }

  }

  /**
   * Internal class for all Mode specific code
   */
  public class Modes implements Iterable<Mode> {

    /**
     * Map to store modes by their Id
     */
    private Map<Long, Mode> modeMap;

    /**
     * Add mode to the internal container
     *
     * @param mode to be registered in this network
     * @return mode, in case it overrides an existing mode, the removed mode is returned
     */
    protected Mode registerMode(final Mode mode) {
      return modeMap.put(mode.getId(), mode);
    }

    /**
     * constructor
     */
    public Modes() {
      modeMap = new TreeMap<Long, Mode>();
    }

    /**
     * Iterator over available modes
     */
    @Override
    public Iterator<Mode> iterator() {
      return modeMap.values().iterator();
    }

    /**
     * Create and register new mode
     *
     * @param externalModeId the external mode id for the mode
     * @param name           of the mode
     * @param pcu            value for the mode
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
     * Collect the first registered mode
     * 
     * @return first registered mode if any
     */
    public Mode getFirst() {
      return getModeById(0);
    }

    /**
     * Retrieve a Mode by its external Id
     * 
     * This method has the option to convert the external Id parameter into a long value, to find the mode when mode objects use long values for external ids.
     * 
     * @param externalId    the external Id of the specified mode
     * @param convertToLong if true, the external Id is converted into a long before beginning the search
     * @return the retrieved mode, or null if no mode was found
     */
    public Mode getModeByExternalId(Object externalId, boolean convertToLong) {
      if (convertToLong) {
        try {
          long value = Long.valueOf(externalId.toString());
          return getModeByExternalId(value);
        } catch (NumberFormatException e) {
          // do nothing - if conversion to long is not possible, use general method instead
        }
      }
      return getModeByExternalId(externalId);
    }

    /**
     * Retrieve a Mode by its external Id
     * 
     * This method is not efficient, since it loops through all the registered modes in order to find the required time period. The equivalent method in InputBuilderListener is
     * more efficient and should be used in preference to this in Java code.
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
   * Network builder responsible for constructing all network related (derived) instances
   */
  protected final PhysicalNetworkBuilder networkBuilder;

  // PUBLIC

  // shorthand for the macroscopic network canonical class name
  public static final String MACROSCOPICNETWORK = MacroscopicNetwork.class.getCanonicalName();

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
   * @param groupId        contiguous id generation within this group for instances of this class
   * @param networkBuilder the builder to be used to create this network
   */
  public PhysicalNetwork(final IdGroupingToken groupId, final PhysicalNetworkBuilder networkBuilder) {
    super(groupId, PhysicalNetwork.class);
    this.graph = new Graph(groupId, networkBuilder);
    this.networkBuilder = networkBuilder;
  }

  // Getters - Setters

  /**
   * Collect the id grouping token used for all entities registered on the network, i.e., this network's specific identifier for generating ids unique and contiguous within this
   * network and this network only
   * 
   * @return the network id grouping token
   */
  public IdGroupingToken getNetworkIdGroupingToken() {
    return graph.getGraphIdGroupingToken();
  }

}

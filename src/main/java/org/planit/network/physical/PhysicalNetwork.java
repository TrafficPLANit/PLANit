package org.planit.network.physical;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.assignment.TrafficAssignmentComponent;
import org.planit.geo.PlanitJtsUtils;
import org.planit.graph.DirectedGraphImpl;
import org.planit.graph.GraphModifier;
import org.planit.mode.ModesImpl;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.DirectedGraph;
import org.planit.utils.graph.DirectedVertex;
import org.planit.utils.graph.Vertex;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.misc.LoggingUtils;
import org.planit.utils.mode.Modes;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Node;

import com.vividsolutions.jts.geom.LineString;

/**
 * Model free Network consisting of nodes and links, each of which can be iterated over. This network does not contain any transport specific information, hence the qualification
 * "model free".
 *
 * @author markr
 */
public class PhysicalNetwork<N extends Node, L extends Link, LS extends LinkSegment> extends TrafficAssignmentComponent<PhysicalNetwork<N, L, LS>> implements Serializable {

  // INNER CLASSES

  /** generated UID */
  private static final long serialVersionUID = -2794450367185361960L;

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PhysicalNetwork.class.getCanonicalName());

  /** the coordinate reference system used for all entities in this network */
  private CoordinateReferenceSystem coordinateReferenceSystem;

  /** one can set the coordinate reference system only when the network is empty and only once at this point */
  private boolean coordinateReferenceSystemLocked = false;

  /**
   * Internal class for all Link specific code
   *
   */
  /**
   * @author markr
   *
   */
  public class Links implements Iterable<L> {

    /**
     * Iterator over available links
     */
    @Override
    public Iterator<L> iterator() {
      return graph.getEdges().iterator();
    }

    /**
     * Create new link to network identified via its id, injecting link length directly (link is not registered on nodes, this is left to the user)
     *
     * @param nodeA  the first node in this link
     * @param nodeB  the second node in this link
     * @param length the length of this link
     * @return the created link
     * @throws PlanItException thrown if there is an error
     */
    public L registerNew(final Node nodeA, final Node nodeB, final double length) throws PlanItException {
      return registerNew(nodeA, nodeB, length, false);
    }

    /**
     * Create new link to network identified via its id, injecting link length directly
     *
     * @param nodeA           the first node in this link
     * @param nodeB           the second node in this link
     * @param length          the length of this link
     * @param registerOnNodes choice to register new link on the nodes or not
     * @return the created link
     * @throws PlanItException thrown if there is an error
     */
    public L registerNew(final Node nodeA, final Node nodeB, final double length, boolean registerOnNodes) throws PlanItException {
      final L newLink = graph.getEdges().registerNew(nodeA, nodeB, length);
      if (registerOnNodes) {
        nodeA.addEdge(newLink);
        nodeB.addEdge(newLink);
      }
      return newLink;
    }

    /**
     * Get link by id
     *
     * @param id the id of the link
     * @return the retrieved link
     */
    public L get(final long id) {
      return graph.getEdges().get(id);
    }

    /**
     * Get the number of links on the network
     *
     * @return the number of links in the network
     */
    public int size() {
      return graph.getEdges().size();
    }

    /**
     * check if size is zero
     * 
     * @return true when empty false otherwise
     */
    public boolean isEmpty() {
      return graph.getEdges().isEmpty();
    }

  }

  /**
   * Internal class for LinkSegment specific code (non-physical link segments are placed in the zoning)
   *
   */
  public class LinkSegments implements Iterable<LS> {

    /**
     * Map to store all link segments for a given start node Id
     */
    private Map<Long, List<LS>> linkSegmentMapByStartNodeId = new HashMap<Long, List<LS>>();

    /**
     * Register a link segment on the network
     *
     * @param linkSegment the link segment to be registered
     * @throws PlanItException thrown if the current link segment external Id has already been assigned
     */
    protected void register(final LS linkSegment) throws PlanItException {
      final Vertex startNode = linkSegment.getUpstreamVertex();
      if (!linkSegmentMapByStartNodeId.containsKey(startNode.getId())) {
        linkSegmentMapByStartNodeId.put(startNode.getId(), new ArrayList<LS>());
      }
      linkSegmentMapByStartNodeId.get(startNode.getId()).add(linkSegment);
    }

    /**
     * Iterator over available edge segments
     */
    @Override
    public Iterator<LS> iterator() {
      return graph.getEdgeSegments().iterator();
    }

    /**
     * Find a LinkSegment by the external Ids of its start and end nodes
     *
     * @param startId reference to start node
     * @param endId   reference to end node
     * @return the linkSegment found
     */
    public LS getByStartAndEndNodeId(final long startId, final long endId) {
      if (!linkSegmentMapByStartNodeId.containsKey(startId)) {
        LOGGER.warning(LoggingUtils.createNetworkPrefix(getId()) + String.format("no link segment with start node %d has been registered in the network", startId));
        return null;
      }
      final List<LS> linkSegmentsForCurrentStartNode = linkSegmentMapByStartNodeId.get(startId);
      for (final LS linkSegment : linkSegmentsForCurrentStartNode) {
        final Vertex downstreamVertex = linkSegment.getDownstreamVertex();
        if (downstreamVertex.getId() == endId) {
          return linkSegment;
        }
      }
      LOGGER.warning(
          LoggingUtils.createNetworkPrefix(getId()) + String.format("no link segment with start node %d and end node %d has been registered in the network", startId, endId));
      return null;
    }

    /**
     * Create directional link segment (not registered on nodes or link)
     *
     * @param parentLink  the parent link of this link segment
     * @param directionAb direction of travel
     * @return the created link segment
     * @throws PlanItException thrown if there is an error
     */
    public LS createNew(final L parentLink, final boolean directionAb) throws PlanItException {
      return graph.getEdgeSegments().create(parentLink, directionAb);
    }

    /**
     * Create directional link segment and register it on the network (not registered on nodes or link)
     *
     * @param parentLink  the parent link of this link segment
     * @param directionAb direction of travel
     * @return the created link segment
     * @throws PlanItException thrown if there is an error
     */
    public LS registerNew(final L parentLink, final boolean directionAb) throws PlanItException {
      return registerNew(parentLink, directionAb, false /* do not register on node and link */);
    }

    /**
     * Create directional link segment and register it on the network (not registered on nodes or link)
     *
     * @param parentLink            the parent link of this link segment
     * @param directionAb           direction of travel
     * @param registerOnNodeAndLink option to register the new link segment on the underlying link and its nodes
     * @return the created link segment
     * @throws PlanItException thrown if there is an error
     */
    public LS registerNew(final L parentLink, final boolean directionAb, final boolean registerOnNodeAndLink) throws PlanItException {
      LS linkSegment = createNew(parentLink, directionAb);
      register(parentLink, linkSegment, directionAb);
      if (registerOnNodeAndLink) {
        parentLink.registerEdgeSegment(linkSegment, directionAb);
        if (parentLink.getVertexA() instanceof DirectedVertex) {
          ((DirectedVertex) parentLink.getVertexA()).addEdgeSegment(linkSegment);
          ((DirectedVertex) parentLink.getVertexB()).addEdgeSegment(linkSegment);
        }
      }
      return linkSegment;
    }

    /**
     * Register a link segment on network (not on nodes and link)
     *
     * @param parentLink  the parent link which specified link segment will be registered on
     * @param linkSegment link segment to be registered
     * @param directionAb direction of travel
     * @throws PlanItException thrown if there is an error
     */
    public void register(final L parentLink, final LS linkSegment, final boolean directionAb) throws PlanItException {
      graph.getEdgeSegments().registerNew(parentLink, linkSegment, directionAb);
      register(linkSegment);
    }

    /**
     * Get link segment by id
     *
     * @param id id of the link segment
     * @return retrieved linkSegment
     */
    public LS get(final long id) {
      return graph.getEdgeSegments().get(id);
    }

    /**
     * Return number of registered link segments
     *
     * @return number of registered link segments
     */
    public long size() {
      return graph.getEdgeSegments().size();
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
    public LinkSegment getByExternalId(Object externalId, boolean convertToLong) {
      try {
        if (convertToLong) {
          long value = Long.valueOf(externalId.toString());
          return getByExternalId(value);
        }
        return getByExternalId(externalId);
      } catch (NumberFormatException e) {
        // do nothing - if conversion to long is not possible, use the general method instead
      }
      return getByExternalId(externalId);
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
    public LinkSegment getByExternalId(Object externalId) {
      for (LinkSegment linkSegment : graph.getEdgeSegments()) {
        if (linkSegment.getExternalId().equals(externalId)) {
          return linkSegment;
        }
      }
      return null;
    }

    /**
     * check if size is zero
     * 
     * @return true if empty false otherwise
     */
    public boolean isEmpty() {
      return graph.getEdgeSegments().isEmpty();
    }

  }

  /**
   * Internal class for all Node specific code
   */
  public class Nodes implements Iterable<N> {

    /**
     * Iterator over available nodes
     */
    @Override
    public Iterator<N> iterator() {
      return graph.getVertices().iterator();
    }

    /**
     * Create and register new node
     *
     * @return new node created
     */
    public N registerNew() {
      final N newNode = graph.getVertices().registerNew();
      return newNode;
    }

    /**
     * Create and register new node
     *
     * @param externalId the externalId of the node
     * @return new node created
     */
    public N registerNew(Object externalId) {
      final N newNode = graph.getVertices().registerNew(externalId);
      return newNode;
    }

    /**
     * Return number of registered nodes
     *
     * @return number of registered nodes
     */
    public int size() {
      return graph.getVertices().size();
    }

    /**
     * Find a node by its d
     *
     * @param id Id of node
     * @return retrieved node
     */
    public N get(final long id) {
      return graph.getVertices().get(id);
    }

    /**
     * check if size is zero
     * 
     * @return true when empty false otherwise
     */
    public boolean isEmpty() {
      return graph.getVertices().isEmpty();
    }

  }

  /**
   * the network builder
   */
  private final PhysicalNetworkBuilder<N, L, LS> networkBuilder;

  /**
   * The graph containing the nodes, links, and link segments (or derived implementations)
   */
  private final DirectedGraph<N, L, LS> graph;

  // Protected

  /**
   * collect the graph
   * 
   * @return graph
   */
  protected DirectedGraph<N, L, LS> getGraph() {
    return graph;
  }

  /**
   * collect the registered network builder
   * 
   * @return networkBuilder the network builder registered
   */
  protected PhysicalNetworkBuilder<N, L, LS> getNetworkBuilder() {
    return networkBuilder;
  }

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
  public final Modes modes;

  /**
   * Network Constructor using default coordinate reference system
   *
   * @param groupId        contiguous id generation within this group for instances of this class
   * @param networkBuilder the builder to be used to create this network
   */
  public PhysicalNetwork(final IdGroupingToken groupId, final PhysicalNetworkBuilder<N, L, LS> networkBuilder) {
    super(groupId, PhysicalNetwork.class);
    this.coordinateReferenceSystem = PlanitJtsUtils.DEFAULT_GEOGRAPHIC_CRS;

    this.networkBuilder = networkBuilder; /* for derived classes building part */
    this.graph = new DirectedGraphImpl<N, L, LS>(groupId, networkBuilder /* for graph builder part */);
    this.modes = new ModesImpl(getNetworkIdGroupingToken()); /* for mode building added by this class */
  }

  /**
   * Network Constructor
   *
   * @param groupId                   contiguous id generation within this group for instances of this class
   * @param networkBuilder            the builder to be used to create this network
   * @param coordinateReferenceSystem the coordinate reference system to use
   */
  public PhysicalNetwork(final IdGroupingToken groupId, final PhysicalNetworkBuilder<N, L, LS> networkBuilder, final CoordinateReferenceSystem coordinateReferenceSystem) {
    super(groupId, PhysicalNetwork.class);
    this.coordinateReferenceSystem = coordinateReferenceSystem;

    this.networkBuilder = networkBuilder; /* for derived classes building part */
    this.graph = new DirectedGraphImpl<N, L, LS>(groupId, networkBuilder /* for graph builder part */);
    this.modes = new ModesImpl(getNetworkIdGroupingToken()); /* for mode building added by this class */
  }

  // Getters - Setters

  /**
   * Collect the id grouping token used for all entities registered on the network, i.e., this network's specific identifier for generating ids unique and contiguous within this
   * network and this network only
   * 
   * @return the network id grouping token
   */
  public IdGroupingToken getNetworkIdGroupingToken() {
    return ((DirectedGraphImpl<N, L, LS>) graph).getGraphIdGroupingToken();
  }

  /**
   * Collect the coordinate reference system used for this network
   * 
   * @return
   */
  public CoordinateReferenceSystem getCoordinateReferenceSystem() {
    return coordinateReferenceSystem;
  }

  /**
   * Collect the coordinate reference system used for this network
   * 
   * @return
   */
  public final void setCoordinateReferenceSystem(CoordinateReferenceSystem coordinateReferenceSystem) {
    if (!coordinateReferenceSystemLocked && isEmpty()) {
      this.coordinateReferenceSystem = coordinateReferenceSystem;
      this.coordinateReferenceSystemLocked = true;
    }
  }

  /**
   * check if network is empty, meaning not a single link, node, or link segment is registered yet
   * 
   * @return true if empty fals otherwise
   */
  public boolean isEmpty() {
    return nodes.isEmpty() && links.isEmpty() && linkSegments.isEmpty();
  }

  /**
   * remove any dangling subnetworks from the network if they exist and subsequently reorder the internal ids if needed
   * 
   */
  public void removeDanglingSubnetworks() {
    removeDanglingSubnetworks(Integer.MAX_VALUE);
  }

  /**
   * remove any dangling subnetworks below a given size from the network if they exist and subsequently reorder the internal ids if needed
   * 
   * @param belowSize only remove subnetworks below the given size
   */
  public void removeDanglingSubnetworks(Integer belowsize) {
    LOGGER.info(String.format("Removing dangling subnetworks with less than %s vertices", belowsize != Integer.MAX_VALUE ? String.valueOf(belowsize) : "infinite"));
    LOGGER.info(String.format("Original number of nodes %d, links %d, link segments %d", nodes.size(), links.size(), linkSegments.size()));

    if (getGraph() instanceof GraphModifier<?, ?>) {
      ((GraphModifier<?, ?>) getGraph()).removeDanglingSubGraphs(belowsize);
    } else {
      LOGGER.severe("Dangling subnetworks can only be removed when network supports graph modifications, this is not the case, call ignored");
    }
    LOGGER.info(String.format("remaining number of nodes %d, links %d, link segments %d", nodes.size(), links.size(), linkSegments.size()));
  }

  /**
   * Break the passed in links by inserting the passed in node in between. After completion the original links remain as NodeA->NodeToBreakAt, and new links as inserted for
   * NodeToBreakAt->NodeB.
   * 
   * Underlying link segments (if any) are also updated accordingly in the same manner
   * 
   * @param linksToBreak  the links to break
   * @param nodeToBreakAt the node to break at
   * @return the broken edges for each original edge's id
   * @throws PlanItException thrown if error
   */
  @SuppressWarnings("unchecked")
  public Map<Long, Set<L>> breakLinksAt(List<? extends L> linksToBreak, N nodeToBreakAt) throws PlanItException {
    if (getGraph() instanceof GraphModifier<?, ?>) {
      Map<Long, Set<L>> affectedLinks = ((GraphModifier<N, L>) getGraph()).breakEdgesAt(linksToBreak, nodeToBreakAt);

      /* broken links geometry must be updated since it links is truncated compared to its original */
      PlanitJtsUtils geoUtils = new PlanitJtsUtils(getCoordinateReferenceSystem());
      for (Entry<Long, Set<L>> brokenLinks : affectedLinks.entrySet()) {
        for (Link brokenLink : brokenLinks.getValue()) {
          LineString updatedGeometry = null;
          if (brokenLink.getNodeA().equals(nodeToBreakAt)) {
            updatedGeometry = geoUtils.createCopyWithoutCoordinatesBefore(nodeToBreakAt.getPosition(), brokenLink.getGeometry());
          } else if (brokenLink.getNodeB().equals(nodeToBreakAt)) {
            updatedGeometry = geoUtils.createCopyWithoutCoordinatesAfter(nodeToBreakAt.getPosition(), brokenLink.getGeometry());
          } else {
            LOGGER.warning(String.format("unable to locate node to break at for broken link %s (id:%d)", brokenLink.getExternalId(), brokenLink.getId()));
          }
          brokenLink.setGeometry(updatedGeometry);
        }
      }
      return affectedLinks;
    }
    LOGGER.severe("Dangling subnetworks can only be removed when network supports graph modifications, this is not the case, call ignored");
    return null;
  }

}

package org.planit.network.physical;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.planit.assignment.TrafficAssignmentComponent;
import org.planit.graph.DirectedGraphImpl;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.DirectedGraph;
import org.planit.utils.graph.Vertex;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.misc.LoggingUtils;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Modes;
import org.planit.utils.network.physical.Node;

/**
 * Model free Network consisting of nodes and links, each of which can be iterated over. This network does not contain any transport specific information, hence the qualification
 * "model free".
 *
 * @author markr
 */
public class PhysicalNetwork<N extends Node, L extends Link, LS extends LinkSegment> extends TrafficAssignmentComponent<PhysicalNetwork<N,L,LS>> implements Serializable {

  // INNER CLASSES

  /** generated UID */
  private static final long serialVersionUID = -2794450367185361960L;

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PhysicalNetwork.class.getCanonicalName());

  /**
   * Internal class for all Link specific code
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
     * Create new link to network identified via its id, injecting link length directly
     *
     * @param nodeA  the first node in this link
     * @param nodeB  the second node in this link
     * @param length the length of this link
     * @return the created link
     * @throws PlanItException thrown if there is an error
     */
    public L registerNewLink(final N nodeA, final N nodeB, final double length) throws PlanItException {
      final L newLink = graph.getEdges().registerNewEdge(nodeA, nodeB, length);
      return newLink;
    }

    /**
     * Get link by id
     *
     * @param id the id of the link
     * @return the retrieved link
     */
    public L getLink(final long id) {
      return graph.getEdges().getEdge(id);
    }

    /**
     * Get the number of links on the network
     *
     * @return the number of links in the network
     */
    public int getNumberOfLinks() {
      return graph.getEdges().getNumberOfEdges();
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
    protected void registerLinkSegment(final LS linkSegment) throws PlanItException {
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
    public LS getLinkSegmentByStartAndEndNodeId(final long startId, final long endId) {
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
     * Create directional link segment
     *
     * @param parentLink  the parent link of this link segment
     * @param directionAB direction of travel
     * @return the created link segment
     * @throws PlanItException thrown if there is an error
     */
    public LS createLinkSegment(final Link parentLink, final boolean directionAB) throws PlanItException {
      return graph.getEdgeSegments().createEdgeSegment(parentLink, directionAB);
    }

    /**
     * Register a link segment
     *
     * @param parentLink  the parent link which specified link segment will be registered on
     * @param linkSegment link segment to be registered
     * @param directionAB direction of travel
     * @throws PlanItException thrown if there is an error
     */
    public void registerLinkSegment(final Link parentLink, final LS linkSegment, final boolean directionAB) throws PlanItException {
      graph.getEdgeSegments().registerEdgeSegment(parentLink, linkSegment, directionAB);
      registerLinkSegment(linkSegment);
    }

    public void registerLinkSegmentTest(LS linkSegment) {
    }

    /**
     * Get link segment by id
     *
     * @param id id of the link segment
     * @return retrieved linkSegment
     */
    public LS getLinkSegment(final long id) {
      return graph.getEdgeSegments().getEdgeSegment(id);
    }

    /**
     * Return number of registered link segments
     *
     * @return number of registered link segments
     */
    public int getNumberOfLinkSegments() {
      return graph.getEdgeSegments().getNumberOfEdgeSegments();
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
      for (LinkSegment linkSegment : graph.getEdgeSegments()) {
        if (linkSegment.getExternalId().equals(externalId)) {
          return linkSegment;
        }
      }
      return null;
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
    public N registerNewNode() {
      final N newNode = graph.getVertices().registerNewVertex();
      return newNode;
    }

    /**
     * Create and register new node
     *
     * @param externalId the externalId of the node
     * @return new node created
     */
    public N registerNewNode(Object externalId) {
      final N newNode = graph.getVertices().registerNewVertex(externalId);
      return newNode;
    }

    /**
     * Return number of registered nodes
     *
     * @return number of registered nodes
     */
    public int getNumberOfNodes() {
      return graph.getVertices().getNumberOfVertices();
    }

    /**
     * Find a node by its d
     *
     * @param id Id of node
     * @return retrieved node
     */
    public N getNodeById(final long id) {
      return graph.getVertices().getVertexById(id);
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
   * collect the registered network builder 
   * @return networkBuilder the network builder registered
   */
  protected DirectedGraph<N,L,LS> getGraph(){
    return graph;
  }  

    
  /**
   * collect the registered network builder 
   * @return networkBuilder the network builder registered
   */
  protected PhysicalNetworkBuilder<N, L, LS> getNetworkBuilder(){
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
   * Network Constructor
   *
   * @param groupId        contiguous id generation within this group for instances of this class
   * @param networkBuilder the builder to be used to create this network
   */
  public PhysicalNetwork(final IdGroupingToken groupId, final PhysicalNetworkBuilder<N, L, LS> networkBuilder) {
    super(groupId, PhysicalNetwork.class);
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
    return ((DirectedGraphImpl<N,L,LS>)graph).getGraphIdGroupingToken();
  }

}

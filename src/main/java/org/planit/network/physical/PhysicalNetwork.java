package org.planit.network.physical;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.planit.assignment.TrafficAssignmentComponent;
import org.planit.graph.DirectedGraphImpl;
import org.planit.mode.ModesImpl;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.DirectedGraph;
import org.planit.utils.graph.DirectedVertex;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.Vertex;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.IdSetter;
import org.planit.utils.misc.LoggingUtils;
import org.planit.utils.mode.Modes;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Node;

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
     * Create new link to network identified via its id, injecting link length directly (link is not registered on nodes, this is left to the user)
     *
     * @param nodeA  the first node in this link
     * @param nodeB  the second node in this link
     * @param length the length of this link
     * @return the created link
     * @throws PlanItException thrown if there is an error
     */
    public L registerNewLink(final N nodeA, final N nodeB, final double length) throws PlanItException {
      return registerNew(nodeA, nodeB, length, false);
    }

    /**
     * Create new link to network identified via its id, injecting link length directly (link is not registered on nodes, this is left to the user)
     *
     * @param nodeA           the first node in this link
     * @param nodeB           the second node in this link
     * @param length          the length of this link
     * @param registerOnNodes choice to register new link on the nodes or not
     * @return the created link
     * @throws PlanItException thrown if there is an error
     */
    public L registerNew(final N nodeA, final N nodeB, final double length, boolean registerOnNodes) throws PlanItException {
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
     * update the internal ids of all links in case there are gaps due to removal of links after their initial creation
     */
    public void updateInternalIds() {
      if(!graph.getEdges().isEmpty()) {
        Edge anEdge = graph.getEdges().iterator().next();
        if(IdSetter.class.isAssignableFrom(anEdge.getClass())) {
         
          IdGenerator.reset(getNetworkIdGroupingToken(), Link.class); // use Link because that is how the ids are registered, not LinkImpl which is the runtime class of anEdge
          IdGenerator.reset(getNetworkIdGroupingToken(), Edge.class); // use edge because that is how the ids are registered, not LinkImpl which is the runtime class of anEdge
          
          ((EdgesImpl)graph.getEdges()).updateInternalIds();
          
          for(Edge edge : graph.getEdges()) {
            ((IdSetter)edge).overwriteId(id);
          }
        }
      }
      
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
    public LS createNew(final Link parentLink, final boolean directionAb) throws PlanItException {
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
    public LS createAndRegisterNew(final Link parentLink, final boolean directionAb) throws PlanItException {
      return createAndRegisterNew(parentLink, directionAb, false /* do not register on node and link */);
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
    public LS createAndRegisterNew(final Link parentLink, final boolean directionAb, final boolean registerOnNodeAndLink) throws PlanItException {
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
    public void register(final Link parentLink, final LS linkSegment, final boolean directionAb) throws PlanItException {
      graph.getEdgeSegments().createAndRegister(parentLink, linkSegment, directionAb);
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
    public int size() {
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
      final N newNode = graph.getVertices().registerNewVertex();
      return newNode;
    }

    /**
     * Create and register new node
     *
     * @param externalId the externalId of the node
     * @return new node created
     */
    public N registerNew(Object externalId) {
      final N newNode = graph.getVertices().registerNewVertex(externalId);
      return newNode;
    }

    /**
     * Return number of registered nodes
     *
     * @return number of registered nodes
     */
    public int size() {
      return graph.getVertices().getNumberOfVertices();
    }

    /**
     * Find a node by its d
     *
     * @param id Id of node
     * @return retrieved node
     */
    public N get(final long id) {
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

  /**
   * update internal ids for the network's internal structure (nodes, links, link segments) in case gaps are found.
   * It is expected that all implementations for nodes, links, link segments support the IdSetter interface. If not
   * a warning will be issued and the id's cannot be updated
   * 
   * @throws PlanItException
   */
  protected void updateNetworkInfrastructureInternalIds() throws PlanItException {    
    this.links.updateInternalIds();
  }

  /**
   * helper function for recursive subnetwork identification
   * 
   * @param referenceNode   to process
   * @param subNetworkNodes to add connected adjacent nodes to
   */
  protected void processSubNetworkNode(Node referenceNode, Set<Node> subNetworkNodes) {
    if(!subNetworkNodes.contains(referenceNode)) {
      subNetworkNodes.add(referenceNode);

      Set<Edge> edges = referenceNode.getEdges();
      for (Edge edge : edges) {
        if (!subNetworkNodes.contains(edge.getVertexA())) {
          processSubNetworkNode((Node) edge.getVertexA(), subNetworkNodes);
        }
        if (!subNetworkNodes.contains(edge.getVertexB())) {
          processSubNetworkNode((Node) edge.getVertexB(), subNetworkNodes);
        }
      }
    }
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
    return ((DirectedGraphImpl<N, L, LS>) graph).getGraphIdGroupingToken();
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
    Set<Integer> removedSubnetworksOfSize = new HashSet<Integer>();
    
    Set<Node> remainingNodes = new HashSet<Node>(nodes.size());
    nodes.forEach(node -> remainingNodes.add(node));
    Map<Node, Integer> identifiedSubNetworkSizes = new HashMap<Node, Integer>();

    while (remainingNodes.iterator().hasNext()) {
      /* recursively traverse the subnetwork */
      Node referenceNode = remainingNodes.iterator().next();
      Set<Node> subNetworkNodesToPopulate = new HashSet<Node>();
      processSubNetworkNode(referenceNode, subNetworkNodesToPopulate);

      /* register size and remove subnetwork from remaining nodes */
      identifiedSubNetworkSizes.put(referenceNode, subNetworkNodesToPopulate.size());
      remainingNodes.removeAll(subNetworkNodesToPopulate);
    }

    /* remove all non-dominating subnetworks */
    int maxSubNetworkSize = Collections.max(identifiedSubNetworkSizes.values());
    for (Entry<Node, Integer> entry : identifiedSubNetworkSizes.entrySet()) {
      int subNetworkSize = entry.getValue();
      if (maxSubNetworkSize > subNetworkSize) {        
        /* not the biggest subnetwork, remove from network if below threshold */
        if(subNetworkSize < belowsize) {
          removeSubnetworkOf(entry.getKey());
          removedSubnetworksOfSize.add(subNetworkSize);
        }
      }
    }
    
    if(belowsize != Integer.MAX_VALUE) {
      LOGGER.info(String.format("removed %d dangling networks",removedSubnetworksOfSize));  
    }else {
      LOGGER.info(String.format("removed %d dangling networks of size %d or less",removedSubnetworksOfSize, belowsize));
    }
    
  }

  /**
   * remove the (sub)network in which the passed in node resides. Apply reordering of internal ids of remaining network.
   * 
   * @param referenceNode to identify subnetwork by
   */
  @SuppressWarnings("unchecked")
  public void removeSubnetworkOf(Node referenceNode) {
    Set<Node> subNetworkNodesToPopulate = new HashSet<Node>();
    processSubNetworkNode(referenceNode, subNetworkNodesToPopulate);

    /* remove the subnetwork from the actual network */
    for (Node node : subNetworkNodesToPopulate) {
      Set<Edge> nodeEdges = node.getEdges();
      nodeEdges.forEach(edge -> this.graph.getEdges().remove((L) edge));

      /* remove from network if not already done */
      Set<EdgeSegment> entryEdgeSegments = node.getEntryEdgeSegments();
      entryEdgeSegments.forEach(edgeSegment -> this.graph.getEdgeSegments().remove((LS) edgeSegment));
      Set<EdgeSegment> exitEdgeSegments = node.getExitEdgeSegments();
      exitEdgeSegments.forEach(edgeSegment -> this.graph.getEdgeSegments().remove((LS) edgeSegment));

      /* remove edges and edge segments from node */
      nodeEdges.forEach(edge -> node.removeEdge(edge));
      entryEdgeSegments.forEach(edgeSegment -> node.removeEdgeSegment(edgeSegment));
      exitEdgeSegments.forEach(edgeSegment -> node.removeEdgeSegment(edgeSegment));

      /* remove node from edges and edge segments */
      entryEdgeSegments.forEach(edgeSegment -> edgeSegment.removeVertex(node));
      exitEdgeSegments.forEach(edgeSegment -> edgeSegment.removeVertex(node));
    }

    try {
      getNetworkBuilder().removeIdGaps(graphs); <-- CONTINUE HERE>
    } catch (PlanItException e) {
      LOGGER.severe(String.format("unable to update internal ids after removing dangling subnetwork (reference node id:%d)",referenceNode.getId()));
    }
  }

}

package org.planit.network.physical;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.geo.PlanitOpenGisUtils;
import org.planit.graph.DirectedGraphImpl;
import org.planit.graph.modifier.DirectedGraphModifierImpl;
import org.planit.network.InfrastructureLayer;
import org.planit.network.TopologicalLayer;
import org.planit.network.TopologicalLayerImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.DirectedGraph;
import org.planit.utils.graph.modifier.BreakEdgeListener;
import org.planit.utils.graph.modifier.DirectedGraphModifier;
import org.planit.utils.graph.modifier.RemoveSubGraphListener;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.LinkSegments;
import org.planit.utils.network.physical.Links;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.physical.Nodes;

/**
 * Model free Network consisting of nodes and links, each of which can be iterated over. This network does not contain any transport specific information, hence the qualification
 * "model free".
 *
 * @author markr
 */
public class PhysicalNetwork<N extends Node, L extends Link, LS extends LinkSegment> extends TopologicalLayerImpl implements TopologicalLayer {

  // INNER CLASSES

  /** generated UID */
  @SuppressWarnings("unused")
  private static final long serialVersionUID = -2794450367185361960L;

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PhysicalNetwork.class.getCanonicalName());

  /**
   * the network builder
   */
  private final PhysicalNetworkBuilder<N, L, LS> networkBuilder;

  /**
   * The graph containing the nodes, links, and link segments (or derived implementations)
   */
  private final DirectedGraph<N, L, LS> graph;

  /** the graph modifier to use to apply larger modifications */
  protected DirectedGraphModifier<N, L, LS> graphModifier;

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

  /**
   * class instance containing all link specific functionality
   */
  public final Links<L> links;

  /**
   * alternative to using the links public member
   * 
   * @return the links
   */
  public final Links<L> getLinks() {
    return links;
  }

  /**
   * class instance containing all link segment specific functionality
   */
  public final LinkSegments<LS> linkSegments;

  /**
   * alternative to using the linkSegments public member
   * 
   * @return the linkSegments
   */
  public final LinkSegments<LS> getLinkSegments() {
    return linkSegments;
  }

  /**
   * class instance containing all nodes specific functionality
   */
  public final Nodes<N> nodes;

  /**
   * alternative to using the nodes public member
   * 
   * @return the nodes
   */
  public final Nodes<N> getNodes() {
    return nodes;
  }

  /**
   * Network Constructor
   *
   * @param tokenId        contiguous id generation within this group for instances of this class
   * @param networkBuilder the builder to be used to create this network
   */
  public PhysicalNetwork(final IdGroupingToken tokenId, final PhysicalNetworkBuilder<N, L, LS> networkBuilder) {
    super(tokenId);

    this.networkBuilder = networkBuilder; /* for derived classes building part */
    this.graph = new DirectedGraphImpl<N, L, LS>(tokenId, networkBuilder /* for graph builder part */);
    this.graphModifier = new DirectedGraphModifierImpl<N, L, LS>((DirectedGraphImpl<N, L, LS>) graph, networkBuilder);

    this.nodes = new NodesImpl<N>(getGraph().getVertices());
    this.links = new LinksImpl<L>(getGraph().getEdges());
    this.linkSegments = new LinkSegmentsImpl<LS>(getGraph().getEdgeSegments());
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
   * {@inheritDoc}
   */
  @Override
  public void transform(CoordinateReferenceSystem fromCoordinateReferenceSystem, CoordinateReferenceSystem toCoordinateReferenceSystem) throws PlanItException {
    try {
      getGraph().transformGeometries(PlanitOpenGisUtils.findMathTransform(fromCoordinateReferenceSystem, toCoordinateReferenceSystem));
    } catch (Exception e) {
      PlanitOpenGisUtils.findMathTransform(fromCoordinateReferenceSystem, toCoordinateReferenceSystem);
      throw new PlanItException(String.format("%s error during transformation of network %s CRS", InfrastructureLayer.createLayerLogPrefix(this), getXmlId()), e);
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
   * remove any dangling subnetworks below a given size from the network if they exist and subsequently reorder the internal ids if needed. Also remove zoning entities that rely
   * solely on removed dangling network entities
   * 
   * @param belowSize         remove subnetworks below the given size
   * @param aboveSize         remove subnetworks above the given size (typically set to maximum value)
   * @param alwaysKeepLargest when true the largest of the subnetworks is always kept, otherwise not
   * @param listeners         listeners to be invoked during removal of subgraphs, may be null
   * @throws PlanItException thrown if error
   */
  @SuppressWarnings("unchecked")
  @Override
  public void removeDanglingSubnetworks(final Integer belowSize, Integer aboveSize, boolean alwaysKeepLargest, final Set<RemoveSubGraphListener<?, ?>> listeners)
      throws PlanItException {

    /* check validity */
    if (graphModifier == null) {
      LOGGER.severe(String.format("%s Dangling subnetworks can only be removed when network supports graph modifications, this is not the case, call ignored",
          InfrastructureLayer.createLayerLogPrefix(this)));
      return;
    }

    /* create callback for zoning */
    if (listeners != null) {
      listeners.forEach(listener -> graphModifier.registerRemoveSubGraphListener((RemoveSubGraphListener<N, L>) listener));
    }

    /* perform removal */
    graphModifier.removeDanglingSubGraphs(belowSize, aboveSize, alwaysKeepLargest);

    /* unregister call back for zoning */
    if (listeners != null) {
      listeners.forEach(listener -> graphModifier.unregisterRemoveSubGraphListener((RemoveSubGraphListener<N, L>) listener));
    }

  }

  /**
   * Break the passed in link by inserting the passed in node in between. After completion the original links remain as (NodeA,NodeToBreakAt), and new links as inserted for
   * (NodeToBreakAt,NodeB).
   * 
   * Underlying link segments (if any) are also updated accordingly in the same manner
   * 
   * @param linkToBreak        the link to break
   * @param nodeToBreakAt      the node to break at
   * @param crs                to use to recompute link lengths of broken links
   * @param breakEdgeListeners the listeners to register (temporarily) when we break edges so they get invoked for callbacks (may be nnull)
   * @return the broken edges for each original edge's id
   * @throws PlanItException thrown if error
   */
  public Map<Long, Set<L>> breakLinkAt(L linkToBreak, N nodeToBreakAt, CoordinateReferenceSystem crs, Set<BreakEdgeListener<N, L>> breakEdgeListeners) throws PlanItException {
    return breakLinksAt(List.of(linkToBreak), nodeToBreakAt, crs, breakEdgeListeners);
  }

  /**
   * Break the passed in links by inserting the passed in node in between. After completion the original links remain as (NodeA,NodeToBreakAt), and new links as inserted for
   * (NodeToBreakAt,NodeB). It is assumed no transfer zones exist in the network, otherwise one should use the same method yet provide the zoning as an additiona parameter to
   * ensure affected connectoids are updated to reflect the new situation
   * 
   * Underlying link segments (if any) are also updated accordingly in the same manner
   * 
   * @param linksToBreak  the links to break
   * @param nodeToBreakAt the node to break at
   * @param crs           to use to recompute link lengths of broken links
   * @return the broken edges for each original edge's id
   * @throws PlanItException thrown if error
   */
  public Map<Long, Set<L>> breakLinksAt(List<? extends L> linksToBreak, N nodeToBreakAt, CoordinateReferenceSystem crs) throws PlanItException {
    return breakLinksAt(linksToBreak, nodeToBreakAt, crs, null);
  }

  /**
   * Break the passed in links by inserting the passed in node in between. After completion the original links remain as (NodeA,NodeToBreakAt), and new links as inserted for
   * (NodeToBreakAt,NodeB).
   * 
   * Underlying link segments (if any) are also updated accordingly in the same manner
   * 
   * @param linksToBreak       the links to break
   * @param nodeToBreakAt      the node to break at
   * @param crs                to use to recompute link lengths of broken links
   * @param breakEdgeListeners the listeners to register (temporarily) when we break edges so they get invoked for callbacks
   * @return the broken edges for each original edge's id
   * @throws PlanItException thrown if error
   */
  public Map<Long, Set<L>> breakLinksAt(List<? extends L> linksToBreak, N nodeToBreakAt, CoordinateReferenceSystem crs, Set<BreakEdgeListener<N, L>> breakEdgeListeners)
      throws PlanItException {
    if (graphModifier == null) {
      LOGGER.severe(String.format("%s Dangling subnetworks can only be removed when network supports graph modifications, this is not the case, call ignored",
          InfrastructureLayer.createLayerLogPrefix(this)));
      return null;
    }

    if (breakEdgeListeners != null) {
      breakEdgeListeners.forEach(listener -> graphModifier.registerBreakEdgeListener(listener));
    }

    Map<Long, Set<L>> affectedLinks = graphModifier.breakEdgesAt(linksToBreak, nodeToBreakAt, crs);

    if (breakEdgeListeners != null) {
      breakEdgeListeners.forEach(listener -> graphModifier.unregisterBreakEdgeListener(listener));
    }

    return affectedLinks;
  }

  /**
   * Basic validation of the network verifying if all nodes and link s are properly set and connected
   */
  @Override
  public boolean validate() {
    boolean isValid = graph.validate();
    for (Link link : links) {
      isValid = isValid && link.validate();
    }
    for (LinkSegment linkSegment : linkSegments) {
      isValid = isValid && linkSegment.validate();
    }
    for (Node node : nodes) {
      isValid = isValid && node.validate();
    }
    return isValid;
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public void logInfo(String prefix) {
    /* log supported modes */
    LOGGER.info(String.format("%s#supported modes: %s", prefix, getSupportedModes().stream().map((mode) -> {
      return mode.getXmlId();
    }).collect(Collectors.joining(", "))));

    /* log infrastructure components */
    LOGGER.info(String.format("%s#links: %d", prefix, links.size()));
    LOGGER.info(String.format("%s#link segments: %d", prefix, linkSegments.size()));
    LOGGER.info(String.format("%s#nodes: %d", prefix, nodes.size()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getNumberOfNodes() {
    return this.nodes.size();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getNumberOfLinks() {
    return this.links.size();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getNumberOfLinkSegments() {
    return this.linkSegments.size();
  }

}

package org.planit.network.physical;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.LineString;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.geo.PlanitJtsUtils;
import org.planit.geo.PlanitOpenGisUtils;
import org.planit.graph.DirectedGraphImpl;
import org.planit.graph.GraphModifier;
import org.planit.network.InfrastructureLayer;
import org.planit.network.InfrastructureLayerImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.DirectedGraph;
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
public class PhysicalNetwork<N extends Node, L extends Link, LS extends LinkSegment> extends InfrastructureLayerImpl implements InfrastructureLayer {

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
   * class instance containing all link segment specific functionality
   */
  public final LinkSegments<LS> linkSegments;

  /**
   * class instance containing all nodes specific functionality
   */
  public final Nodes<N> nodes;

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
      throw new PlanItException(String.format("error during transformation of network %s CRS", getXmlId()), e);
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
   * @throws PlanItException thrown if error
   * 
   */
  public void removeDanglingSubnetworks() throws PlanItException {
    removeDanglingSubnetworks(Integer.MAX_VALUE, Integer.MAX_VALUE, true);
  }

  /**
   * remove any dangling subnetworks below a given size from the network if they exist and subsequently reorder the internal ids if needed
   * 
   * @param belowSize         remove subnetworks below the given size
   * @param aboveSize         remove subnetworks above the given size (typically set to maximum value)
   * @param alwaysKeepLargest when true the largest of the subnetwork sis always kept, otherwise not
   * @throws PlanItException thrown if error
   */
  public void removeDanglingSubnetworks(Integer belowsize, Integer aboveSize, boolean alwaysKeepLargest) throws PlanItException {
    LOGGER.info(String.format("Removing dangling subnetworks with less than %s vertices", belowsize != Integer.MAX_VALUE ? String.valueOf(belowsize) : "infinite"));
    if (aboveSize != Integer.MAX_VALUE) {
      LOGGER.info(String.format("Removing dangling subnetworks with more than %s vertices", String.valueOf(aboveSize)));
    }
    LOGGER.info(String.format("Original number of nodes %d, links %d, link segments %d", nodes.size(), links.size(), linkSegments.size()));

    if (getGraph() instanceof GraphModifier<?, ?>) {
      ((GraphModifier<?, ?>) getGraph()).removeDanglingSubGraphs(belowsize, aboveSize, alwaysKeepLargest);
    } else {
      LOGGER.severe("Dangling subnetworks can only be removed when network supports graph modifications, this is not the case, call ignored");
    }
    LOGGER.info(String.format("remaining number of nodes %d, links %d, link segments %d", nodes.size(), links.size(), linkSegments.size()));
  }

  /**
   * Break the passed in link by inserting the passed in node in between. After completion the original links remain as NodeA->NodeToBreakAt, and new links as inserted for
   * NodeToBreakAt->NodeB.
   * 
   * Underlying link segments (if any) are also updated accordingly in the same manner
   * 
   * @param linkToBreak   the link to break
   * @param nodeToBreakAt the node to break at
   * @param crs           to use to recompute link lengths of broken links
   * @return the broken edges for each original edge's id
   * @throws PlanItException thrown if error
   */
  public Map<Long, Set<L>> breakLinkAt(L linkToBreak, N nodeToBreakAt, CoordinateReferenceSystem crs) throws PlanItException {
    return breakLinksAt(List.of(linkToBreak), nodeToBreakAt, crs);
  }

  /**
   * Break the passed in links by inserting the passed in node in between. After completion the original links remain as NodeA->NodeToBreakAt, and new links as inserted for
   * NodeToBreakAt->NodeB.
   * 
   * Underlying link segments (if any) are also updated accordingly in the same manner
   * 
   * @param linksToBreak  the links to break
   * @param nodeToBreakAt the node to break at
   * @param crs           to use to recompute link lengths of broken links
   * @return the broken edges for each original edge's id
   * @throws PlanItException thrown if error
   */
  @SuppressWarnings("unchecked")
  public Map<Long, Set<L>> breakLinksAt(List<? extends L> linksToBreak, N nodeToBreakAt, CoordinateReferenceSystem crs) throws PlanItException {
    if (getGraph() instanceof GraphModifier<?, ?>) {

      Map<Long, Set<L>> affectedLinks = ((GraphModifier<N, L>) getGraph()).breakEdgesAt(linksToBreak, nodeToBreakAt);

      /* broken links geometry must be updated since it links is truncated compared to its original */
      PlanitJtsUtils geoUtils = new PlanitJtsUtils(crs);
      for (Entry<Long, Set<L>> brokenLinks : affectedLinks.entrySet()) {
        for (Link brokenLink : brokenLinks.getValue()) {
          LineString updatedGeometry = null;
          if (brokenLink.getNodeA().equals(nodeToBreakAt)) {
            updatedGeometry = PlanitJtsUtils.createCopyWithoutCoordinatesBefore(nodeToBreakAt.getPosition(), brokenLink.getGeometry());
          } else if (brokenLink.getNodeB().equals(nodeToBreakAt)) {
            updatedGeometry = PlanitJtsUtils.createCopyWithoutCoordinatesAfter(nodeToBreakAt.getPosition(), brokenLink.getGeometry());
          } else {
            LOGGER.warning(String.format("unable to locate node to break at (%s) for broken link %s (id:%d)", nodeToBreakAt.getPosition().toString(), brokenLink.getExternalId(),
                brokenLink.getId()));
          }
          brokenLink.setGeometry(updatedGeometry);
          brokenLink.setLengthKm(geoUtils.getDistanceInKilometres(updatedGeometry));
        }
      }
      return affectedLinks;
    }
    LOGGER.severe("Dangling subnetworks can only be removed when network supports graph modifications, this is not the case, call ignored");
    return null;
  }

  /**
   * Basic validation of the network verifying if all nodes and link s are properly set and connected
   */
  public void validate() {
    graph.validate();
    links.forEach(link -> link.validate());
    linkSegments.forEach(linkSegment -> linkSegment.validate());
    nodes.forEach(node -> node.validate());
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

}

package org.planit.network.physical;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.assignment.TrafficAssignmentComponent;
import org.planit.geo.PlanitJtsUtils;
import org.planit.geo.PlanitOpenGisUtils;
import org.planit.graph.DirectedGraphImpl;
import org.planit.graph.GraphModifier;
import org.planit.mode.ModesImpl;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.DirectedGraph;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Modes;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.LinkSegments;
import org.planit.utils.network.physical.Links;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.physical.Nodes;
import org.locationtech.jts.geom.LineString;

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
   * class instance containing all modes specific functionality
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
    
    this.nodes = new NodesImpl<N>(getGraph().getVertices());
    this.links = new LinksImpl<L>(getGraph().getEdges());
    this.linkSegments = new LinkSegmentsImpl<LS>(getGraph().getEdgeSegments());
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
    if (!coordinateReferenceSystemLocked || isEmpty()) {
      this.coordinateReferenceSystem = coordinateReferenceSystem;
      this.coordinateReferenceSystemLocked = true;
    } else {
      LOGGER.warning("Coordinate Reference System is already set. To change the CRS after instantiation, use transform() method");
    }
  }

  /**
   * change the coordinate system, which will result in an update of all geometries in the network from the original CRS to the new CRS. If the network is empty and no CRS is set
   * yet, then this is identical to calling setCoordinateReferenceSystem
   * 
   * @param newCoordinateReferenceSystem to transform the network to
   * @throws PlanItException
   * @throws FactoryException
   */
  public void transform(CoordinateReferenceSystem newCoordinateReferenceSystem) throws PlanItException {
    if (!coordinateReferenceSystemLocked || isEmpty()) {
      setCoordinateReferenceSystem(coordinateReferenceSystem);
    } else {
      try {
        getGraph().transformGeometries(PlanitOpenGisUtils.findMathTransform(getCoordinateReferenceSystem(), newCoordinateReferenceSystem));
      } catch (Exception e) {
        PlanitOpenGisUtils.findMathTransform(getCoordinateReferenceSystem(), newCoordinateReferenceSystem);
        throw new PlanItException(String.format("error during transformation of network's CRS", e));
      }
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
   * @return the broken edges for each original edge's id
   * @throws PlanItException thrown if error
   */
  public Map<Long, Set<L>> breakLinkAt(L linkToBreak, N nodeToBreakAt) throws PlanItException {
    return breakLinksAt(List.of(linkToBreak), nodeToBreakAt);
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

}

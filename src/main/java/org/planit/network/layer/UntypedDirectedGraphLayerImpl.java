package org.planit.network.layer;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.graph.UntypedDirectedGraphImpl;
import org.planit.graph.modifier.DirectedGraphModifierImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.geo.PlanitJtsUtils;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.GraphEntities;
import org.planit.utils.graph.UntypedDirectedGraph;
import org.planit.utils.graph.directed.DirectedEdge;
import org.planit.utils.graph.directed.DirectedVertex;
import org.planit.utils.graph.modifier.BreakEdgeListener;
import org.planit.utils.graph.modifier.DirectedGraphModifier;
import org.planit.utils.graph.modifier.RemoveSubGraphListener;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.TransportLayer;
import org.planit.utils.network.layer.UntypedDirectedGraphLayer;

/**
 * Model free network layer consisting of containers for vertices, edges, and edge segments each of which can be typed separately. This network does not contain any transport
 * specific information, hence the qualification "model free".
 *
 * @author markr
 */
public abstract class UntypedDirectedGraphLayerImpl<V extends DirectedVertex, VE extends GraphEntities<V>, E extends DirectedEdge, EE extends GraphEntities<E>, S extends EdgeSegment, SE extends GraphEntities<S>>
    extends TopologicalLayerImpl implements UntypedDirectedGraphLayer<V, VE, E, EE, S, SE> {

  // INNER CLASSES

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(UntypedDirectedGraphLayerImpl.class.getCanonicalName());

  /**
   * The graph containing the vertices, edges, and edge segments (or derived implementations)
   */
  private final UntypedDirectedGraphImpl<VE, EE, SE> graph;

  /** the graph modifier to use to apply larger modifications */
  protected DirectedGraphModifier graphModifier;

  // Protected

  /**
   * collect the graph
   * 
   * @return graph
   */
  protected UntypedDirectedGraph<VE, EE, SE> getGraph() {
    return graph;
  }

  // PUBLIC

  /**
   * Network Constructor
   *
   * @param tokenId      contiguous id generation within this group for instances of this class
   * @param vertices     vertices container to use
   * @param edges        edges container to use
   * @param edgeSegments edge Segments container to use
   */
  public UntypedDirectedGraphLayerImpl(final IdGroupingToken tokenId, final VE vertices, final EE edges, final SE edgeSegments) {
    super(tokenId);
    this.graph = new UntypedDirectedGraphImpl<VE, EE, SE>(tokenId, vertices, edges, edgeSegments);
    this.graphModifier = new DirectedGraphModifierImpl(graph);
  }

  /**
   * Copy constructor
   * 
   * @param untypedDirectedGraphLayerImpl to copy
   */
  public UntypedDirectedGraphLayerImpl(UntypedDirectedGraphLayerImpl<V, VE, E, EE, S, SE> untypedDirectedGraphLayerImpl) {
    super(untypedDirectedGraphLayerImpl);
    this.graph = untypedDirectedGraphLayerImpl.graph.clone();
    this.graphModifier = new DirectedGraphModifierImpl(graph);
  }

  // Getters - Setters

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
  @Override
  public Map<Long, Set<E>> breakAt(List<E> linksToBreak, V nodeToBreakAt, CoordinateReferenceSystem crs, Set<BreakEdgeListener> breakEdgeListeners) throws PlanItException {
    if (graphModifier == null) {
      LOGGER.severe(String.format("%s Dangling subnetworks can only be removed when network supports graph modifications, this is not the case, call ignored",
          TransportLayer.createLayerLogPrefix(this)));
      return null;
    }

    if (breakEdgeListeners != null) {
      breakEdgeListeners.forEach(listener -> graphModifier.registerBreakEdgeListener(listener));
    }

    Map<Long, Set<E>> affectedLinks = graphModifier.breakEdgesAt(linksToBreak, nodeToBreakAt, crs);

    if (breakEdgeListeners != null) {
      breakEdgeListeners.forEach(listener -> graphModifier.unregisterBreakEdgeListener(listener));
    }

    return affectedLinks;
  }

  // Getters - Setters

  /**
   * {@inheritDoc}
   */
  @Override
  public IdGroupingToken getLayerIdGroupingToken() {
    return graph.getGraphIdGroupingToken();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void transform(CoordinateReferenceSystem fromCoordinateReferenceSystem, CoordinateReferenceSystem toCoordinateReferenceSystem) throws PlanItException {
    try {
      getGraph().transformGeometries(PlanitJtsUtils.findMathTransform(fromCoordinateReferenceSystem, toCoordinateReferenceSystem));
    } catch (Exception e) {
      PlanitJtsUtils.findMathTransform(fromCoordinateReferenceSystem, toCoordinateReferenceSystem);
      throw new PlanItException(String.format("%s error during transformation of physical network %s CRS", TransportLayer.createLayerLogPrefix(this), getXmlId()), e);
    }
  }

  /**
   * check if network is empty, meaning not a single link, node, or link segment is registered yet
   * 
   * @return true if empty false otherwise
   */
  public boolean isEmpty() {
    return graph.isEmpty();
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
  @Override
  public void removeDanglingSubnetworks(final Integer belowSize, Integer aboveSize, boolean alwaysKeepLargest, final Set<RemoveSubGraphListener> listeners) throws PlanItException {

    /* check validity */
    if (graphModifier == null) {
      LOGGER.severe(String.format("%s Dangling subnetworks can only be removed when network supports graph modifications, this is not the case, call ignored",
          TransportLayer.createLayerLogPrefix(this)));
      return;
    }

    /* create callback for zoning */
    if (listeners != null) {
      listeners.forEach(listener -> graphModifier.registerRemoveSubGraphListener(listener));
    }

    /* perform removal */
    graphModifier.removeDanglingSubGraphs(belowSize, aboveSize, alwaysKeepLargest);

    /* unregister call back for zoning */
    if (listeners != null) {
      listeners.forEach(listener -> graphModifier.unregisterRemoveSubGraphListener(listener));
    }

  }

  /**
   * Basic validation of the network verifying if all nodes and link s are properly set and connected
   */
  @Override
  public boolean validate() {
    return graph.validate();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract UntypedDirectedGraphLayerImpl<V, VE, E, EE, S, SE> clone();

}

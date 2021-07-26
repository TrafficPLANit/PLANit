package org.planit.network.layer;

import java.util.logging.Logger;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.graph.UntypedDirectedGraphImpl;
import org.planit.network.layer.modifier.UntypedNetworkLayerModifierImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.geo.PlanitJtsUtils;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.GraphEntities;
import org.planit.utils.graph.UntypedDirectedGraph;
import org.planit.utils.graph.directed.DirectedEdge;
import org.planit.utils.graph.directed.DirectedVertex;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.TransportLayer;
import org.planit.utils.network.layer.UntypedDirectedGraphLayer;
import org.planit.utils.network.layer.modifier.UntypedDirectedGraphLayerModifier;

/**
 * Model free network layer consisting of containers for vertices, edges, and edge segments each of which can be typed separately. This network does not contain any transport
 * specific information, hence the qualification "model free".
 *
 * @author markr
 */
public abstract class UntypedNetworkLayerImpl<V extends DirectedVertex, VE extends GraphEntities<V>, E extends DirectedEdge, EE extends GraphEntities<E>, S extends EdgeSegment, SE extends GraphEntities<S>>
    extends TopologicalLayerImpl implements UntypedDirectedGraphLayer<V, VE, E, EE, S, SE> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(UntypedNetworkLayerImpl.class.getCanonicalName());

  /**
   * The graph containing the vertices, edges, and edge segments (or derived implementations)
   */
  private final UntypedDirectedGraphImpl<VE, EE, SE> graph;

  /** the modifier to use to apply larger modifications */
  protected UntypedDirectedGraphLayerModifier<V, VE, E, EE, S, SE> layerModifier;

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
  public UntypedNetworkLayerImpl(final IdGroupingToken tokenId, final VE vertices, final EE edges, final SE edgeSegments) {
    super(tokenId);
    this.graph = new UntypedDirectedGraphImpl<VE, EE, SE>(tokenId, vertices, edges, edgeSegments);
    this.layerModifier = new UntypedNetworkLayerModifierImpl<V, VE, E, EE, S, SE>(this.graph);
  }

  /**
   * Network Constructor
   *
   * @param tokenId       contiguous id generation within this group for instances of this class
   * @param vertices      vertices container to use
   * @param edges         edges container to use
   * @param edgeSegments  edge Segments container to use
   * @param layerModifier to use for applying modifications to the graph
   */
  public UntypedNetworkLayerImpl(final IdGroupingToken tokenId, final VE vertices, final EE edges, final SE edgeSegments,
      UntypedDirectedGraphLayerModifier<V, VE, E, EE, S, SE> layerModifier) {
    super(tokenId);
    this.graph = new UntypedDirectedGraphImpl<VE, EE, SE>(tokenId, vertices, edges, edgeSegments);
    this.layerModifier = layerModifier;
  }

  /**
   * Copy constructor
   * 
   * @param untypedDirectedGraphLayerImpl to copy
   */
  public UntypedNetworkLayerImpl(UntypedNetworkLayerImpl<V, VE, E, EE, S, SE> untypedDirectedGraphLayerImpl) {
    super(untypedDirectedGraphLayerImpl);
    this.graph = untypedDirectedGraphLayerImpl.graph.clone();
    this.layerModifier = new UntypedNetworkLayerModifierImpl<V, VE, E, EE, S, SE>(graph);
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
  public UntypedDirectedGraphLayerModifier<V, VE, E, EE, S, SE> getLayerModifier() {
    return layerModifier;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract UntypedNetworkLayerImpl<V, VE, E, EE, S, SE> clone();

}

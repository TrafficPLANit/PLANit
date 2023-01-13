package org.goplanit.network.layer;

import java.util.logging.Logger;

import org.goplanit.graph.directed.UntypedDirectedGraphImpl;
import org.goplanit.network.layer.modifier.UntypedNetworkLayerModifierImpl;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.graph.ManagedGraphEntities;
import org.goplanit.utils.graph.UntypedDirectedGraph;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntities;
import org.goplanit.utils.network.layer.NetworkLayer;
import org.goplanit.utils.network.layer.UntypedDirectedGraphLayer;
import org.goplanit.utils.network.layer.modifier.UntypedDirectedGraphLayerModifier;
import org.locationtech.jts.geom.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Model free network layer consisting of containers for vertices, edges, and edge segments each of which can be typed separately. This network does not contain any transport
 * specific information, hence the qualification "model free".
 *
 * @author markr
 */
public abstract class UntypedNetworkLayerImpl<V extends DirectedVertex, E extends DirectedEdge, S extends EdgeSegment> extends TopologicalLayerImpl
    implements UntypedDirectedGraphLayer<V, E, S> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(UntypedNetworkLayerImpl.class.getCanonicalName());

  /**
   * The graph containing the vertices, edges, and edge segments (or derived implementations)
   */
  protected final UntypedDirectedGraphImpl<V, E, S> graph;

  /** the modifier to use to apply larger modifications */
  protected UntypedDirectedGraphLayerModifier<V, E, S> layerModifier;

  // Protected

  /**
   * collect the graph
   * 
   * @return graph
   */
  protected UntypedDirectedGraph<V, E, S> getGraph() {
    return graph;
  }

  // PUBLIC

  // @formatter:off
  /**
   * Network Constructor
   *
   * @param <Vx> container type for vertices
   * @param <Ex> container type for vertices
   * @param <Sx> container type for vertices
   * @param tokenId      contiguous id generation within this group for instances of this class
   * @param vertices     managed vertices container to use
   * @param edges        managed edges container to use
   * @param edgeSegments managed edge Segments container to use
   */
  public <Vx extends ManagedGraphEntities<V>, Ex extends ManagedGraphEntities<E>, Sx extends ManagedGraphEntities<S>> 
      UntypedNetworkLayerImpl(final IdGroupingToken tokenId, final Vx vertices, final Ex edges, final Sx edgeSegments) {
    super(tokenId);
    this.graph = new UntypedDirectedGraphImpl<>(tokenId, vertices, edges, edgeSegments);
    this.layerModifier = new UntypedNetworkLayerModifierImpl<>(this.graph);
  }

  /**
   * Copy constructor
   * 
   * @param untypedDirectedGraphLayerImpl to copy
   */
  public UntypedNetworkLayerImpl(UntypedNetworkLayerImpl<V, E, S> untypedDirectedGraphLayerImpl) {
    super(untypedDirectedGraphLayerImpl);
    this.graph = untypedDirectedGraphLayerImpl.graph.clone();
    this.layerModifier = new UntypedNetworkLayerModifierImpl<>(graph);
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
      throw new PlanItException(String.format("%s error during transformation of physical network %s CRS", NetworkLayer.createLayerLogPrefix(this), getXmlId()), e);
    }
  }

  /**
   * Create bounding box based on underlying nodes, this means that any geometries of links tht are internal may cross the boundary of the bounding box
   *
   * @return bounding box for this layer based on its nodes' locations, if no vertices are present null is returned
   */
  @Override
  public Envelope createBoundingBox() {
    if(getGraph().getVertices().isEmpty()){
      return null;
    }

    Envelope envelope = new Envelope(getGraph().getVertices().iterator().next().getPosition().getCoordinate());
    getGraph().getVertices().forEach(v -> envelope.expandToInclude(v.getPosition().getCoordinate()));
    return envelope;
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
  public UntypedDirectedGraphLayerModifier<V, E, S> getLayerModifier() {
    return layerModifier;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract UntypedNetworkLayerImpl<V, E, S> clone();

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public void reset() {
    super.reset();
    ((ManagedIdEntities<V>) this.graph.getVertices()).reset();
    ((ManagedIdEntities<E>) this.graph.getEdges()).reset();
    ((ManagedIdEntities<S>) this.graph.getEdgeSegments()).reset();
  }
}

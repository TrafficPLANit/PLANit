package org.goplanit.network.layer;

import java.util.logging.Logger;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.goplanit.graph.directed.UntypedDirectedGraphImpl;
import org.goplanit.network.layer.modifier.UntypedNetworkLayerModifierImpl;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.graph.GraphEntityDeepCopyMapper;
import org.goplanit.utils.graph.ManagedGraphEntities;
import org.goplanit.utils.graph.UntypedDirectedGraph;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdDeepCopyMapper;
import org.goplanit.utils.id.ManagedIdEntities;
import org.goplanit.utils.network.layer.NetworkLayer;
import org.goplanit.utils.network.layer.UntypedDirectedGraphLayer;
import org.goplanit.utils.network.layer.modifier.UntypedDirectedGraphLayerModifier;
import org.goplanit.utils.network.layer.physical.BannedMovement;
import org.goplanit.utils.network.layer.physical.BannedMovements;
import org.locationtech.jts.geom.Envelope;

/**
 * Model free network layer consisting of containers for vertices, edges, and edge segments each of which can be
 * typed separately. This network does not contain any transport specific information, hence the qualification
 * "model free".
 *
 * @author markr
 * @param <V> type of vertex
 * @param <E> type of edge
 * @param <S> type of segment
 */
public abstract class UntypedNetworkLayerImpl<V extends DirectedVertex, E extends DirectedEdge, S extends EdgeSegment>
    extends TopologicalLayerImpl
    implements UntypedDirectedGraphLayer<V, E, S> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(UntypedNetworkLayerImpl.class.getCanonicalName());

  /**
   * The graph containing the vertices, edges, and edge segments (or derived implementations)
   */
  protected final UntypedDirectedGraphImpl<V, E, S> directedGraph;

  /** the modifier to use to apply larger modifications */
  protected UntypedDirectedGraphLayerModifier<V, E, S> layerModifier;

  // Protected

  /**
   * collect the graph
   * 
   * @return graph
   */
  protected UntypedDirectedGraph<V, E, S> getDirectedGraph() {
    return directedGraph;
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
   * @param bannedMovements    managed movements container to use
   */
  public <Vx extends ManagedGraphEntities<V>, Ex extends ManagedGraphEntities<E>, Sx extends ManagedGraphEntities<S>> 
      UntypedNetworkLayerImpl(
          final IdGroupingToken tokenId,
          final Vx vertices,
          final Ex edges,
          final Sx edgeSegments,
          final BannedMovements bannedMovements) {
    super(tokenId);
    this.directedGraph = new UntypedDirectedGraphImpl<>(tokenId, vertices, edges, edgeSegments, bannedMovements);
    this.layerModifier = new UntypedNetworkLayerModifierImpl<>(this.directedGraph);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param vertexMapper to apply in case of deep copy to each original to copy combination
   *                     (when provided, may be null)
   * @param edgeMapper to apply in case of deep copy to each original to copy combination
   *                   (when provided, may be null)
   * @param edgeSegmentMapper to apply in case of deep copy to each original to copy combination
   *                         (when provided, may be null)
   * @param movementMapper to apply in case of deep copy to each original to copy combination
   *                       (when provided, may be null)
   *
   */
  public UntypedNetworkLayerImpl(
          UntypedNetworkLayerImpl<V, E, S> other,
          boolean deepCopy,
          GraphEntityDeepCopyMapper<V> vertexMapper,
          GraphEntityDeepCopyMapper<E> edgeMapper,
          GraphEntityDeepCopyMapper<S> edgeSegmentMapper,
          ManagedIdDeepCopyMapper<BannedMovement> movementMapper) {
    super(other, deepCopy);

    /* network layer is in fact a graph, so requiring cloning even for shallow copy */
    this.directedGraph =
            deepCopy ? other.directedGraph.smartDeepClone(
                vertexMapper, edgeMapper, edgeSegmentMapper, movementMapper) : other.directedGraph.shallowClone();

    this.layerModifier = new UntypedNetworkLayerModifierImpl<>(directedGraph);
  }

  // Getters - Setters

  /**
   * {@inheritDoc}
   */
  @Override
  public IdGroupingToken getLayerIdGroupingToken() {
    return directedGraph.getGraphIdGroupingToken();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void transform(
          CoordinateReferenceSystem fromCoordinateReferenceSystem,
          CoordinateReferenceSystem toCoordinateReferenceSystem){
    try {
      getDirectedGraph().transformGeometries(
              PlanitJtsUtils.findMathTransform(fromCoordinateReferenceSystem, toCoordinateReferenceSystem));
    } catch (Exception e) {
      PlanitJtsUtils.findMathTransform(fromCoordinateReferenceSystem, toCoordinateReferenceSystem);
      throw new PlanItRunTimeException(String.format("%s error during transformation of physical network %s CRS",
              NetworkLayer.createLayerLogPrefix(this), getXmlId()), e);
    }
  }

  /**
   * Create bounding box based on underlying nodes, this means that any geometries of links tht are internal
   * may cross the boundary of the bounding box
   *
   * @return bounding box for this layer based on its nodes' locations, if no vertices are present null is returned
   */
  @Override
  public Envelope createBoundingBox() {
    if(getDirectedGraph().getVertices().isEmpty()){
      return null;
    }

    Envelope envelope = new Envelope(getDirectedGraph().getVertices().iterator().next().getPosition().getCoordinate());
    getDirectedGraph().getVertices().forEach(v -> envelope.expandToInclude(v.getPosition().getCoordinate()));
    return envelope;
  }

  /**
   * check if network is empty, meaning not a single link, node, or link segment is registered yet
   * 
   * @return true if empty false otherwise
   */
  public boolean isEmpty() {
    return directedGraph.isEmpty();
  }

  /**
   * Basic validation of the network verifying if all nodes and link s are properly set and connected
   */
  @Override
  public boolean validate() {
    return directedGraph.validate();
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
  public abstract UntypedNetworkLayerImpl<V, E, S> shallowClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract UntypedNetworkLayerImpl<V, E, S> deepClone();

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public void reset(boolean resetManagedIdToken) {
    super.reset(resetManagedIdToken);
    ((ManagedIdEntities<V>) this.directedGraph.getVertices()).reset(resetManagedIdToken);
    ((ManagedIdEntities<E>) this.directedGraph.getEdges()).reset(resetManagedIdToken);
    ((ManagedIdEntities<S>) this.directedGraph.getEdgeSegments()).reset(resetManagedIdToken);
    this.directedGraph.getMovements().reset(resetManagedIdToken);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public long recreateManagedIds(IdGroupingToken tokenId) {
    var newId = super.recreateManagedIds(tokenId);
    ((ManagedIdEntities<V>) this.directedGraph.getVertices()).recreateIds(true);
    ((ManagedIdEntities<E>) this.directedGraph.getEdges()).recreateIds(true);
    ((ManagedIdEntities<S>) this.directedGraph.getEdgeSegments()).recreateIds(true);
    this.directedGraph.getMovements().recreateIds(true);
    return newId;
  }
}

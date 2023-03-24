package org.goplanit.network.virtual;

import org.goplanit.network.Network;
import org.goplanit.utils.graph.EdgeUtils;
import org.goplanit.utils.graph.GraphEntityDeepCopyMapper;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.graph.VertexUtils;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegmentUtils;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.virtual.*;
import org.goplanit.utils.zoning.Centroid;

import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Model free virtual network which is part of the zoning and holds all the virtual infrastructure connecting the zones to the physical road network.
 * 
 * @author markr
 */
public class VirtualNetworkImpl extends Network implements VirtualNetwork {

  // INNER CLASSES

  /** generated id */
  private static final long serialVersionUID = -4088201905917614130L;

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger((VirtualNetworkImpl.class.getCanonicalName()));

  // Protected

  // PUBLIC

  /**
   * Container for connectoid edges
   */
  protected final ConnectoidEdgesImpl connectoidEdges;

  /**
   * Container for connectoid edge segments
   */
  protected final ConnectoidSegmentsImpl connectoidSegments;

  /**
   * Container for centroid vertices
   */
  protected final CentroidVerticesImpl centroidVertices;

  /**
   * Constructor
   * 
   * @param tokenId contiguous id generation for instances of this class
   */
  public VirtualNetworkImpl(final IdGroupingToken tokenId) {
    super(tokenId);
    this.connectoidSegments = new ConnectoidSegmentsImpl(getIdGroupingToken());
    this.connectoidEdges = new ConnectoidEdgesImpl(getIdGroupingToken());
    this.centroidVertices = new CentroidVerticesImpl(getIdGroupingToken());
  }

  /**
   * Copy constructor
   *
   * @param other to clone
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param connectoidEdgeMapper to use for tracking mapping between original and copied entity (may be null)
   * @param connectoidSegmentMapper to use for tracking mapping between original and copied entity (may be null)
   * @param centroidVertexMapper to use for tracking mapping between original and copied entity (may be null)
   */
  protected VirtualNetworkImpl(
      final VirtualNetworkImpl other,
      boolean deepCopy,
      GraphEntityDeepCopyMapper<ConnectoidEdge> connectoidEdgeMapper,
      GraphEntityDeepCopyMapper<ConnectoidSegment> connectoidSegmentMapper,
      GraphEntityDeepCopyMapper<CentroidVertex> centroidVertexMapper
      ) {
    super(other, deepCopy);

    // container wrappers so requires clone also for shallow copy
    if(deepCopy){
      this.connectoidSegments = other.connectoidSegments.deepCloneWithMapping(connectoidSegmentMapper);
      this.connectoidEdges    = other.connectoidEdges.deepCloneWithMapping(connectoidEdgeMapper);
      this.centroidVertices   = other.centroidVertices.deepCloneWithMapping(centroidVertexMapper);

      // update edges connected to all centroid vertices as these have been copied and existing references are outdated
      VertexUtils.updateVertexEdges(centroidVertices, (ConnectoidEdge edge) -> connectoidEdgeMapper.getMapping(edge), true);
      // connectoid edges partly reside in physical network, so we keep those mappings as is, but we do update the centroid vertex mappings
      EdgeUtils.updateEdgeVertices(
          connectoidEdges,
          (DirectedVertex vertex) -> {
              if( !(vertex instanceof  CentroidVertex)){
                return null;
              }
              return centroidVertexMapper.getMapping((CentroidVertex) vertex);
          },
          false);
      // update connectoid segment parent connectoid edges
      EdgeSegmentUtils.updateEdgeSegmentParentEdges(connectoidSegments, (ConnectoidEdge originalEdge) -> connectoidEdgeMapper.getMapping(originalEdge), true);
    }else{
      this.connectoidSegments = other.connectoidSegments.shallowClone();
      this.connectoidEdges    = other.connectoidEdges.shallowClone();
      this.centroidVertices   = other.centroidVertices.shallowClone();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectoidSegments getConnectoidSegments() {
    return connectoidSegments;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectoidEdges getConnectoidEdges() {
    return connectoidEdges;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CentroidVertices getCentroidVertices() {
    return centroidVertices;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clear() {
    connectoidEdges.clear();
    connectoidSegments.clear();
    centroidVertices.clear();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    connectoidEdges.reset();
    connectoidSegments.reset();
    centroidVertices.reset();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void logInfo(String prefix) {
    LOGGER.info(String.format("%s#connectoid edges: %d", prefix, getConnectoidEdges().size()));
    LOGGER.info(String.format("%s#connectoid segments: %d", prefix, getConnectoidSegments().size()));
    LOGGER.info(String.format("%s#centroid vertices: %d", prefix, getCentroidVertices().size()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateVirtualNetworkImpl createConjugate(IdGroupingToken idToken) {
    var conjugateVirtualNetwork = new ConjugateVirtualNetworkImpl(idToken, this);
    conjugateVirtualNetwork.update();
    return conjugateVirtualNetwork;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public VirtualNetworkImpl shallowClone() {
    return new VirtualNetworkImpl(this, false, null, null, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public VirtualNetworkImpl deepClone() {
    return deepCloneWithMapping(new GraphEntityDeepCopyMapper<>(), new GraphEntityDeepCopyMapper<>(), new GraphEntityDeepCopyMapper<>());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public VirtualNetworkImpl deepCloneWithMapping(GraphEntityDeepCopyMapper<ConnectoidEdge> connectoidEdgeMapper,
                                                 GraphEntityDeepCopyMapper<ConnectoidSegment> connectoidSegmentMapper,
                                                 GraphEntityDeepCopyMapper<CentroidVertex> centroidVertexMapper) {
    return new VirtualNetworkImpl(this, true, connectoidEdgeMapper, connectoidSegmentMapper, centroidVertexMapper);
  }

}

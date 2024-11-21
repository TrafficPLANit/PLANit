package org.goplanit.network.virtual;

import org.goplanit.network.Network;
import org.goplanit.utils.graph.GraphEntityDeepCopyMapper;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.network.virtual.*;
import org.goplanit.utils.network.virtual.graph.CentroidVertex;
import org.goplanit.utils.network.virtual.graph.ConnectoidDirectedEdge;
import org.goplanit.utils.network.virtual.physical.ConnectoidLink;
import org.goplanit.utils.network.virtual.physical.ConnectoidSegment;

import java.util.logging.Logger;

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

  protected VirtualNetworkLayerImpl virtualLayer;

  /**
   * Constructor
   * 
   * @param tokenId contiguous id generation for instances of this class
   */
  public VirtualNetworkImpl(final IdGroupingToken tokenId) {
    super(tokenId);
    virtualLayer = new VirtualNetworkLayerImpl(tokenId);
  }

  /**
   * Copy constructor
   *
   * @param other to clone
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param connectoidLinkMapper to use for tracking mapping between original and copied entity (may be null)
   * @param connectoidSegmentMapper to use for tracking mapping between original and copied entity (may be null)
   * @param centroidVertexMapper to use for tracking mapping between original and copied entity (may be null)
   */
  @SuppressWarnings("unchecked")
  protected VirtualNetworkImpl(
      final VirtualNetworkImpl other,
      boolean deepCopy,
      GraphEntityDeepCopyMapper<? extends ConnectoidDirectedEdge> connectoidLinkMapper,
      GraphEntityDeepCopyMapper<? extends ConnectoidSegment> connectoidSegmentMapper,
      GraphEntityDeepCopyMapper<? extends CentroidVertex> centroidVertexMapper) {
    super(other, deepCopy);

    this.virtualLayer = deepCopy ?
            getLayer().deepCloneWithMapping(
                    (GraphEntityDeepCopyMapper<ConnectoidLink>) connectoidLinkMapper,
                    (GraphEntityDeepCopyMapper<ConnectoidSegment>) connectoidSegmentMapper,
                    (GraphEntityDeepCopyMapper<CentroidVertex>) centroidVertexMapper) :
            getLayer().shallowClone();
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public void logInfo(String prefix){
    LOGGER.info(String.format(
        "%sVirtual network (%s) has %d layer", prefix, getIdsAsString(), 1));
    getLayer().logInfo(prefix + LoggingUtils.virtualNetworkLayerPrefix(getLayer().getId()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public VirtualNetworkLayerImpl getLayer() {
    return virtualLayer;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clear() {
    getLayer().clear();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    getLayer().reset();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEmpty() {
    return getLayer().isEmpty();
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
    return new VirtualNetworkImpl(
            this, false, null, null, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public VirtualNetworkImpl deepClone() {
    return deepCloneWithMapping(
            new GraphEntityDeepCopyMapper<>(), new GraphEntityDeepCopyMapper<>(), new GraphEntityDeepCopyMapper<>());
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("Unchecked")
  @Override
  public VirtualNetworkImpl deepCloneWithMapping(GraphEntityDeepCopyMapper<? extends ConnectoidDirectedEdge> connectoidEdgeMapper,
                                                 GraphEntityDeepCopyMapper<? extends ConnectoidSegment> connectoidSegmentMapper,
                                                 GraphEntityDeepCopyMapper<? extends DirectedVertex> centroidVertexMapper) {
    return new VirtualNetworkImpl(
            this,
        true,
        connectoidEdgeMapper,
        connectoidSegmentMapper,
        (GraphEntityDeepCopyMapper<CentroidVertex>) centroidVertexMapper);
  }

}

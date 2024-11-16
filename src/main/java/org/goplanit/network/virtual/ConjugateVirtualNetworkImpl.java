package org.goplanit.network.virtual;

import java.util.logging.Logger;

import org.goplanit.utils.graph.GraphEntityDeepCopyMapper;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.virtual.*;

/**
 * Conjugate version (edge-to-vertex-dual) of regular virtual network.
 * 
 * @author markr
 *
 */
public class ConjugateVirtualNetworkImpl implements ConjugateVirtualNetwork {

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(ConjugateVirtualNetworkImpl.class.getCanonicalName());

  protected ConjugateVirtualNetworkLayerImpl conjugateVirtualLayer;

  /** original virtual network this conjugate is based on */
  protected final VirtualNetwork originalVirtualNetwork;

  /**
   * Reset and re-populate entire conjugate virtual network based on current state of original virtual network
   * this is the conjugate of
   */
  protected void update() {
    getLayer().update();
  }

  /**
   * Constructor
   * 
   * @param idToken contiguous id generation for instances of this class
   * @param originalVirtualNetwork to use
   */
  public ConjugateVirtualNetworkImpl(IdGroupingToken idToken, final VirtualNetworkImpl originalVirtualNetwork) {
    this.conjugateVirtualLayer = new ConjugateVirtualNetworkLayerImpl(idToken, originalVirtualNetwork.getLayer());
    this.originalVirtualNetwork = originalVirtualNetwork;
  }

  /**
   * Copy constructor
   *
   * @param other to clone
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param connectoidEdgeMapper to use for tracking mapping between original and copied entity (may be null)
   * @param connectoidSegmentMapper to use for tracking mapping between original and copied entity (may be null)
   * @param conjugateNodeMapper to use for tracking mapping between original and copied entity (may be null)
   */
  @SuppressWarnings("unchecked")
  protected ConjugateVirtualNetworkImpl(
          final ConjugateVirtualNetworkImpl other,
          boolean deepCopy,
          GraphEntityDeepCopyMapper<? extends ConjugateConnectoidEdge> connectoidEdgeMapper,
          GraphEntityDeepCopyMapper<? extends ConjugateConnectoidSegment> connectoidSegmentMapper,
          GraphEntityDeepCopyMapper<? extends ConjugateConnectoidNode> conjugateNodeMapper) {
    this.conjugateVirtualLayer = deepCopy ?
            getLayer().deepCloneWithMapping(
                    (GraphEntityDeepCopyMapper<ConjugateConnectoidEdge>) connectoidEdgeMapper,
                    (GraphEntityDeepCopyMapper<ConjugateConnectoidSegment>) connectoidSegmentMapper,
                    (GraphEntityDeepCopyMapper<ConjugateConnectoidNode>) conjugateNodeMapper) :
            getLayer().shallowClone();

    this.originalVirtualNetwork = other.originalVirtualNetwork;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public VirtualNetwork getOriginalVirtualNetwork() {
    return originalVirtualNetwork;
  }


  @Override
  public ConjugateVirtualNetworkLayerImpl getLayer() {
    return conjugateVirtualLayer;
  }

  @Override
  public void clear() {
    conjugateVirtualLayer.clear();
  }

  @Override
  public void reset() {
    conjugateVirtualLayer.reset();
  }

  @Override
  public ConjugateVirtualNetworkImpl shallowClone() {
    return new ConjugateVirtualNetworkImpl(this, false, null, null, null );
  }

  @Override
  public ConjugateVirtualNetworkImpl deepClone() {
    return deepCloneWithMapping(
            new GraphEntityDeepCopyMapper<>(),
            new GraphEntityDeepCopyMapper<>(),
            new GraphEntityDeepCopyMapper<>());
  }

  @Override
  public ConjugateVirtualNetworkImpl deepCloneWithMapping(
      GraphEntityDeepCopyMapper<? extends ConnectoidEdge> connectoidEdgeMapper,
      GraphEntityDeepCopyMapper<? extends ConnectoidSegment> connectoidSegmentMapper,
      GraphEntityDeepCopyMapper<? extends DirectedVertex> conjugateNodeMapper) {
    return new ConjugateVirtualNetworkImpl(
        this,
        true,
        (GraphEntityDeepCopyMapper<? extends ConjugateConnectoidEdge>) connectoidEdgeMapper,
        (GraphEntityDeepCopyMapper<? extends ConjugateConnectoidSegment>) connectoidSegmentMapper,
        (GraphEntityDeepCopyMapper<ConjugateConnectoidNode>)conjugateNodeMapper);
  }

}
